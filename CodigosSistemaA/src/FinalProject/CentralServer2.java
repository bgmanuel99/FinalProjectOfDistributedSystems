package FinalProject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

import Global.GlobalFunctions;
import protocol.*;

public class CentralServer2 {
	public static void main(String [] args) {
		try {
			try {
				GlobalFunctions.initBBDD("CentralServerII");
			} catch (Exception e) {
				System.out.println("Exception main client: " + e.getMessage());
			}
			ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("SERVER2"));
			
			while(true) {
				System.out.println("Waiting CentralServer2...");
				Socket socket = listenSocket.accept();
				try {
                	GlobalFunctions.writeToBBDD("CentralServerII", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main authentication: " + e.getMessage());
                }
				System.out.println("Accepted connection from: " + socket.getInetAddress().toString());
				
				new ConnectionCentral2(socket);
			}
		}catch(IOException e) {
			System.out.println("IOException main CentralServer2: " + e.getMessage());
		}catch(Exception e) {
			System.out.println("Exception main CentralServer2: " + e.getMessage());
		}
	}
}

class ConnectionCentral2 extends Thread{
	private Socket socket, nodeSocket;
	private ObjectInputStream isSocket, isNode;
	private ObjectOutputStream osSocket, osNode;
	
	public ConnectionCentral2(Socket socket) {
		try {
			this.socket = socket;
			this.isSocket = new ObjectInputStream(this.socket.getInputStream());
			this.osSocket = new ObjectOutputStream(this.socket.getOutputStream());
			this.start();
		} catch (IOException e) {
			System.out.println("IOException constructor ConnectionCentral2: " + e.getMessage());
		}
	}
	
	@Override
	public void run() {
		try {
			Request r = (Request) this.isSocket.readObject();
			if(r.getType().equals("CONTROL_REQUEST")) {
				ControlRequest cr = (ControlRequest) r;
				if(cr.getSubtype().equals("OP_FILTER")) {
					GlobalFunctions.writeToBBDD("CentralServerII", "> A filter request has arrived from the central node");
					this.doConnectNode();
					this.doFilter(cr);
					this.doDisconnectNode();
				}
			}else if(r.getType().equals("DATA_REQUEST")) {
				DataRequest dr = (DataRequest) r;
				if(dr.getSubtype().equals("OP_CPU")) {
					GlobalFunctions.writeToBBDD("CentralServerII", "> A CPU data request has arrived from the central node");
					ControlResponse crs = new ControlResponse("OP_CPU_OK");
					Random random = new Random();
					int cpu = random.nextInt(101);
					crs.getArgs().add(cpu);
					GlobalFunctions.writeToBBDD("CentralServerII", "The CPU data for this server in this moment is: " + cpu + ", sending it back to the central node");
					this.osSocket.writeObject(crs);
					if(this.socket != null) {
						this.isSocket.close();
						this.isSocket = null;
						this.osSocket.close();
						this.osSocket = null;
						this.socket.close();
						this.socket = null;
					}
				}
			}
			GlobalFunctions.writeToBBDD("CentralServerII", "----------------------------------------------------------");
		}catch(ClassNotFoundException e) {
			System.out.println("ClassNotFoundException run ConnectionCentral2: " + e.getMessage());
		}catch(IOException e) {
			System.out.println("IOException run ConnectionCentral2: " + e.getMessage());
		}catch(Exception e) {
			System.out.println("Exception run ConnectionCentral2: " + e.getMessage());
		}
	}
	
	public void doFilter(ControlRequest cr) {
		try {
			GlobalFunctions.writeToBBDD("CentralServerII", "> Getting the tokens from the processing nodes");
			int [] tokens = new int[] {GlobalFunctions.getNodeToken("Node1Token"), GlobalFunctions.getNodeToken("Node2Token"), GlobalFunctions.getNodeToken("Node3Token")};
			
			GlobalFunctions.writeToBBDD("CentralServerII", "> The token values of the processing nodes are: ");
			for(int i = 0; i < tokens.length; i++) {
				GlobalFunctions.writeToBBDD("CentralServerII", "> Node " + (i+1) + " -> " + tokens[i]);
			}
			
			GlobalFunctions.writeToBBDD("CentralServerII", "> Choosing the processing node with the least token number");
			
			int maxToken = 5, max = 5;
			int node = 0;
			for(int i = 0; i < tokens.length; i++) {
				if(tokens[i] == maxToken) continue;
				else if(tokens[i] < max) {
					max = tokens[i];
					node = i + 1;
				}
			}
			
			if(node != 0) {
				GlobalFunctions.writeToBBDD("CentralServerII", "> The processing node choosen is Node " + node + ", sending the filter request");
				cr.getArgs().add(node);
				this.osNode.writeObject(cr);
			}else {
				GlobalFunctions.writeToBBDD("CentralServerII", "> There are no available processing nodes");
				ControlResponse crs = new ControlResponse("OP_FILTER_NOK");
				crs.getArgs().add("There are no available nodes to process the image");
				this.osSocket.writeObject(crs);
				return;
			}
			
			ControlResponse crs = (ControlResponse) this.isNode.readObject();
			GlobalFunctions.writeToBBDD("CentralServerII", "> Response received from the processing node, sending response back to the central node");
			this.osSocket.writeObject(crs);
		} catch (Exception e) {
			System.out.println("Exception doFilter ConnectionCentral1: " + e.getMessage());
		}
	}
	
	public void doConnectNode() {
		try {
			if(this.nodeSocket == null) {
				this.nodeSocket = new Socket(GlobalFunctions.getIP("NODE1"), GlobalFunctions.getPort("NODE1"));
				this.isNode = new ObjectInputStream(this.nodeSocket.getInputStream());
				this.osNode = new ObjectOutputStream(this.nodeSocket.getOutputStream());
			}
		}catch(IOException e) {
			System.out.println("IOException doConnectNode ConnectionCentral2: " + e.getMessage());
		}catch(Exception e) {
			System.out.println("Exceptoin doConnectNode ConnectionCentral2: " + e.getMessage());
		}
	}
	
	public void doDisconnectNode() {
		try {
			if(this.nodeSocket != null) {
				this.isNode.close();
				this.isNode = null;
				this.osNode.close();
				this.osNode = null;
				this.nodeSocket.close();
				this.nodeSocket = null;
			}
		}catch(IOException e) {
			System.out.println("IOException doDisconnectNode ConnectionCentral2: " + e.getMessage());
		}catch(Exception e) {
			System.out.println("Exceptoin doDisconnectNode ConnectionCentral2: " + e.getMessage());
		}
	}
}