package FinalProject;

import protocol.*;
import java.net.*;
import Global.GlobalFunctions;
import java.io.*;

public class CentralNode {
    public static void main(String [] args) {
        try{
        	GlobalFunctions.initFile("CentralNodeCPU");
        	try {
    			GlobalFunctions.initBBDD("CentralNode");
    		} catch (Exception e) {
    			System.out.println("Exception main client: " + e.getMessage());
    		}
            ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("CENTRAL"));
            
            while(true) {
                System.out.println("Waiting central node...");
                Socket socket = listenSocket.accept();
                try {
                	GlobalFunctions.writeToBBDD("CentralNode", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main authentication: " + e.getMessage());
                }
                System.out.println("Accepted connection from: " + socket.getInetAddress().toString());
            
                new ConnectionCentral(socket);
            }
        }catch(Exception e){
            System.out.println("Main (Auth): "+e.getMessage());
        }
    }
}

class ConnectionCentral extends Thread{
    private Socket socketIn;
	private Socket [] socketservers;
    private ObjectOutputStream osIn;
    private ObjectOutputStream [] osServers;
    private ObjectInputStream isIn;
    private ObjectInputStream [] isServers;
    private int [] dataCpu;

    public ConnectionCentral(Socket socket){
        try {
            this.socketIn = socket;
            this.isIn = new ObjectInputStream(this.socketIn.getInputStream());
            this.osIn =  new ObjectOutputStream(this.socketIn.getOutputStream());
            this.socketservers = new Socket[GlobalFunctions.getExternalVariables("MAXSERVERS")];
            this.osServers = new ObjectOutputStream[GlobalFunctions.getExternalVariables("MAXSERVERS")];
            this.isServers = new ObjectInputStream[GlobalFunctions.getExternalVariables("MAXSERVERS")];
            this.dataCpu = new int[GlobalFunctions.getExternalVariables("MAXSERVERS")];
            for(int i = 0; i < GlobalFunctions.getExternalVariables("MAXSERVERS"); i++) this.dataCpu[i] = 0;
            this.start();
        } catch (Exception e) {
            System.out.println("ConnectionCentral: "+e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try{
            Request r = (Request) this.isIn.readObject();
            if(r.getType().equals("CONTROL_REQUEST")) {
                ControlRequest cr = (ControlRequest) r;
                if(cr.getSubtype().equals("OP_LOGIN")) {
                	GlobalFunctions.writeToBBDD("CentralNode", "> A login request has arrived from the authentication node");
                    ControlResponse crs = new ControlResponse("OP_LOGIN_OK");
                    GlobalFunctions.writeToBBDD("CentralNode", "> Sending the response back to the authentication node");
                    this.osIn.writeObject(crs);
                }else if(cr.getSubtype().equals("OP_FILTER")) {
                	GlobalFunctions.writeToBBDD("CentralNode", "> A filter request has arrived from the client node");
                	this.doConnect();
                	this.doFilter(cr);
                }
            }
            GlobalFunctions.writeToBBDD("CentralNode", "----------------------------------------------------------");
        }catch(ClassNotFoundException e){
            System.out.println("ClassNotFoundException run connectionCental: " + e.getMessage());
        }catch(IOException e) {
        	System.out.println("IOException run connectionCentral: " + e.getMessage());
        }catch(Exception e) {
        	System.out.println("Exception run connectionCentral: " + e.getMessage());
        }
    }

    private void doFilter(ControlRequest cr) {
        try{
        	GlobalFunctions.writeToBBDD("CentralNode", "> Getting CPU data from the servers");
        	DataRequest dr = new DataRequest("OP_CPU");
        	for(int i = 0; i < this.osServers.length; i++) {
        		new DataCPU(this, i, dr);
        	}
        	
        	while(true) {
        		int done = 0;
                for(int data : this.dataCpu) if(data!=0) done++;
                if(done == GlobalFunctions.getExternalVariables("MAXSERVERS")) break;
        	}
        	
        	GlobalFunctions.writeToBBDD("CentralNode", "> CPU data has arrived from the servers it's values are:");
        	for(int i = 0; i < this.dataCpu.length; i++) {
        		GlobalFunctions.writeToBBDD("CentralNode", "> Server " + (i+1) + " -> " + this.dataCpu[i] + "%");
        	}
        	
        	this.doDisconnect();
        	this.doConnect();
        	
        	GlobalFunctions.writeToBBDD("CentralNode", "> Choosing the server with less % of used CPU");
        	
        	int indexServer = -1;
            int minCPU = 100;
            for(int i = 0; i < this.dataCpu.length; i++){
                if(this.dataCpu[i] < minCPU){
                    minCPU = this.dataCpu[i];
                    indexServer = i;
                }
            }
            
            if(indexServer != -1) {
            	GlobalFunctions.writeToBBDD("CentralNode", "> The elected server is: " + (indexServer+1) + ", sending the filter request");
            	this.osServers[indexServer].writeObject(cr);
            }else {
            	GlobalFunctions.writeToBBDD("CentralNode", "> There are no available servers to process the request");
            	ControlResponse crs = new ControlResponse("OP_FILTER_NOK");
            	crs.getArgs().add("There are no available servers to process the request");
            	this.osIn.writeObject(crs);
            	return;
            }
            
            ControlResponse crs = (ControlResponse) this.isServers[indexServer].readObject();
            GlobalFunctions.writeToBBDD("CentralNode", "> Response received from the server, sending response back to the client");
            this.osIn.writeObject(crs);
            this.doDisconnect();
        }catch(Exception e){
        	try {
				GlobalFunctions.writeToBBDD("CentralNode", "> There was an unexpected error");
			} catch (IOException e1) {
				System.out.println("IOException doFilter connectionCentral: " + e1.getMessage());
			} catch (Exception e1) {
				System.out.println("Exception doFilter connectionCentral: " + e1.getMessage());
			}
            System.out.println("Exception doFilter connectionCentral: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void doConnect(){
        try {
            for(int i = 0; i < GlobalFunctions.getExternalVariables("MAXSERVERS");i++){
                if(this.socketservers[i] == null){
                    this.socketservers[i] = new Socket(GlobalFunctions.getIP("SERVER"+(i+1)),GlobalFunctions.getPort("SERVER"+(i+1)));
                    this.osServers[i] = new ObjectOutputStream(this.socketservers[i].getOutputStream());
                    this.isServers[i] = new ObjectInputStream(this.socketservers[i].getInputStream());
                }
            }
        } catch (Exception e) {
            System.out.println("ConnectionCentral (doConnect): "+e.getMessage());
        }
    }

    private void doDisconnect(){
        try {
            for(int i = 0; i< GlobalFunctions.getExternalVariables("MAXSERVERS");i++){
                if(this.socketservers[i] != null){
                    this.osServers[i].close();
                    this.osServers[i] = null;
                    this.isServers[i].close();
                    this.isServers[i] = null;
                    this.socketservers[i].close();
                    this.socketservers[i] = null;
                }
            }
        } catch (Exception e) {
            System.out.println("ConnectionCentral (doDisconnect): "+e.getMessage());
        }
    }
    
    public void doDisconnect(int indexServer) {
    	try {
    		if(this.socketservers[indexServer] != null && indexServer != 0){
                this.osServers[indexServer].close();
                this.osServers[indexServer] = null;
                this.isServers[indexServer].close();
                this.isServers[indexServer] = null;
                this.socketservers[indexServer].close();
                this.socketservers[indexServer] = null;
            }
    	}catch(Exception e) {
    		System.out.println("ConnectionCentral (doDisconnect(indexServer)): " + e.getMessage());
    	}
    }
    
    public Socket getSocketIn() {
		return socketIn;
	}

	public void setSocketIn(Socket socketIn) {
		this.socketIn = socketIn;
	}

	public Socket[] getSocketservers() {
		return socketservers;
	}

	public void setSocketservers(Socket[] socketservers) {
		this.socketservers = socketservers;
	}

	public ObjectOutputStream getOsIn() {
		return osIn;
	}

	public void setOsIn(ObjectOutputStream osIn) {
		this.osIn = osIn;
	}

	public ObjectOutputStream[] getOsServers() {
		return osServers;
	}

	public void setOsServers(ObjectOutputStream[] osServers) {
		this.osServers = osServers;
	}

	public ObjectInputStream getIsIn() {
		return isIn;
	}

	public void setIsIn(ObjectInputStream isIn) {
		this.isIn = isIn;
	}

	public ObjectInputStream[] getIsServers() {
		return isServers;
	}

	public void setIsServers(ObjectInputStream[] isServers) {
		this.isServers = isServers;
	}

	public int[] getDataCpu() {
		return dataCpu;
	}

	public void setDataCpu(int[] dataCpu) {
		this.dataCpu = dataCpu;
	}
	
	class DataCPU extends Thread {
		private ConnectionCentral connectionCentral;
		private int indexServer;
		private DataRequest dataRequest;
		private boolean done;
		
		public DataCPU(ConnectionCentral connectionCentral, int indexServer, DataRequest dataRequest) {
			this.connectionCentral = connectionCentral;
			this.indexServer = indexServer;
			this.dataRequest = dataRequest;
			this.done = false;
			this.start();
		}
		
		@Override
		public void run() {
			Masking m = new Masking(this.indexServer, this);
			long start = System.currentTimeMillis();
			
			try {
				GlobalFunctions.writeToBBDD("CentralNode", "> Sending CPU request to the server " + (this.indexServer+1));
				this.connectionCentral.osServers[this.indexServer].writeObject(this.dataRequest);
				m.start();
				ControlResponse crs = (ControlResponse) this.connectionCentral.isServers[this.indexServer].readObject();
				this.done = true;
				GlobalFunctions.writeToBBDD("CentralNode", "> Receiving CPU data from server: " + (this.indexServer+1));
				this.connectionCentral.getDataCpu()[this.indexServer] = Integer.valueOf(crs.getArgs().get(0).toString());
			}catch(IOException e) {
				try {
					GlobalFunctions.writeToBBDD("CentralNode", "> There was a problem with the data, so the masking is been applied");
				} catch (IOException e1) {
					System.out.println("IOExeception run (DataCPU): " + e.getMessage());
				} catch (Exception e1) {
					System.out.println("Exeception run (DataCPU): " + e.getMessage());
				}
				System.out.println("IOException run (DataCPU): " + e.getMessage());
				this.connectionCentral.getDataCpu()[this.indexServer] = 100;
			}catch(ClassNotFoundException e) {
				try {
					GlobalFunctions.writeToBBDD("CentralNode", "> There was a problem with the data, so the masking is been applied");
				} catch (IOException e1) {
					System.out.println("IOExeception run (DataCPU): " + e.getMessage());
				} catch (Exception e1) {
					System.out.println("Exeception run (DataCPU): " + e.getMessage());
				}
				System.out.println("ClassNotFoundException run (DataCPU): " + e.getMessage());
				this.connectionCentral.getDataCpu()[this.indexServer] = 100;
			}catch(Exception e) {
				try {
					GlobalFunctions.writeToBBDD("CentralNode", "> There was a problem with the data, so the masking is been applied");
				} catch (IOException e1) {
					System.out.println("IOExeception run (DataCPU): " + e.getMessage());
				} catch (Exception e1) {
					System.out.println("Exeception run (DataCPU): " + e.getMessage());
				}
				System.out.println("Exception run (DataCPU)");
				this.connectionCentral.getDataCpu()[this.indexServer] = 100;
			}
			
			long end = System.currentTimeMillis();
			
		    try {
		    	GlobalFunctions.setLatency("CentralNodeCPU", (end-start));
		    }catch(Exception e) {
		    	System.out.println("Exception run (DataCPU): " + e.getMessage());
		    }
		}
	}
	
	class Masking extends Thread {
		private int indexServer;
		private DataCPU dataCpu;
		
		public Masking(int indexServer, DataCPU dataCpu) {
			this.indexServer = indexServer;
			this.dataCpu = dataCpu;
		}
		
		@Override
		public void run() {
			long sleep = 1000;
			
			try {
				sleep = GlobalFunctions.getLatency("CentralNodeCPU");
			}catch(Exception e) {
				System.out.println("Exception run (masking): " + e.getMessage());
			}
			
			try {
				Thread.sleep(sleep);
				if(!this.dataCpu.done) {
					GlobalFunctions.writeToBBDD("CentralNode", "> Masking has been applied");
					this.dataCpu.connectionCentral.doDisconnect(this.indexServer);
				}
			}catch(InterruptedException e) {
				System.out.println("InterruptedException run (masking): " + e.getMessage());
			}catch(Exception e) {
				System.out.println("Exception run (masking) 2: " + e.getMessage());
			}
		}
	}
}