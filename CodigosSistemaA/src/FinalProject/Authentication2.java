package FinalProject;

import protocol.*;
import java.net.*;
import Global.GlobalFunctions;
import java.io.*;

public class Authentication2 {
    public static void main(String[] args) {
        try {
        	GlobalFunctions.initFile("AuthLogin");
        	GlobalFunctions.initFile("AuthRegister");
        	try {
    			GlobalFunctions.initBBDD("Authentication2");
    		} catch (Exception e) {
    			System.out.println("Exception main client: " + e.getMessage());
    		}
            ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("AUTH2"));

            while (true) {
                System.out.println("Waiting auth2 node...");
                Socket socket = listenSocket.accept();
                try {
                	GlobalFunctions.writeToBBDD("Authentication2", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main authentication2: " + e.getMessage());
                }
                System.out.println("Accepted connection from: " + socket.getInetAddress().toString());

                new Connection(socket);
            }
        } catch (Exception e) {
            System.out.println("Main (Auth2): " + e.getMessage());
        }
    }
}

class Connection2 extends Thread {

    private Socket sClient, sCentral;
    private ObjectInputStream isClient, isCentral;
    private ObjectOutputStream osClient, osCentral;
    private boolean done;
    private long start, end;

    public Connection2(Socket client) {
        try {
            this.sClient = client;
            this.osClient = new ObjectOutputStream(this.sClient.getOutputStream());
            this.isClient = new ObjectInputStream(this.sClient.getInputStream());
            this.done = false;
            this.start = 0L;
            this.end = 0L;
            this.start();
        } catch (Exception e) {
            System.out.println("Connection (Auth2): " + e.getMessage());
        }
    }

    public void run() {
        try {
            Request r = (Request) this.isClient.readObject();
            if (r.getType().equals("CONTROL_REQUEST")) {
                ControlRequest cr = (ControlRequest) r;
                if (cr.getSubtype().equals("OP_LOGIN")) {
                	GlobalFunctions.writeToBBDD("Authentication2", "> A login request has arrived to the authentication2 node");
                    this.doConnect();
                    this.doLogin((byte[]) cr.getArgs().get(0), (byte[]) cr.getArgs().get(1));
                    this.doDisconnect();
                }else if(cr.getSubtype().equals("OP_REGISTER")) {
                	GlobalFunctions.writeToBBDD("Authentication2", "> A register request has arrived to the authentication2 node");
                    if(!GlobalFunctions.isUser(GlobalFunctions.decrypt((byte []) cr.getArgs().get(1)))) {
                    	GlobalFunctions.writeToBBDD("Authentication2", "> Registering the user in the BBDD");
                        GlobalFunctions.addUser((byte[]) cr.getArgs().get(0), (byte []) cr.getArgs().get(1), (byte []) cr.getArgs().get(2));
                        this.osClient.writeObject(new ControlResponse("OP_REGISTER_OK"));
                    }else {
                    	GlobalFunctions.writeToBBDD("Authentication2", "> There is already a user with that email in the BBDD");
                        this.osClient.writeObject(new ControlResponse("OP_REGISTER_NOK"));
                    }
                }
            }else if(r.getType().equals("DATA_REQUEST")) {
            	DataRequest dr = (DataRequest) r;
            	if(dr.getSubtype().equals("OP_GET_TIME")) {
            		GlobalFunctions.writeToBBDD("Authentication", "> A time request has arrived to the authentication node");
            		ControlResponse crs = new ControlResponse("OP_GET_TIME_OK");
            		crs.getArgs().add(System.currentTimeMillis());
            		GlobalFunctions.writeToBBDD("Authentication", "> The actual time of the server is going to be send back to the client");
            		this.osClient.writeObject(crs);
            	}
            }
            GlobalFunctions.writeToBBDD("Authentication2", "----------------------------------------------------------");
        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException connection (Auth2): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Readline connection (Auth2): " + e.getMessage());
        }catch (Exception e){
        	try {
				this.osClient.writeObject(new ControlResponse("OP_REGISTER_NOK"));
			} catch (IOException e1) {
				System.out.println("IoException in Exception run (Auth2): " + e.getMessage());
			}
        }
    }

    private void doLogin(byte[] email, byte[] password) {
        try {
        	GlobalFunctions.writeToBBDD("Authentication2", "> Decrypting the email and the password");
            String emailDecrypt = GlobalFunctions.decrypt(email);
            String passDecrypt = GlobalFunctions.decrypt(password);
            if (GlobalFunctions.isUser(emailDecrypt) && GlobalFunctions.getPassword(emailDecrypt).equals(passDecrypt)) {
            	GlobalFunctions.writeToBBDD("Authentication2", "> The email and password where correct, login successful");
                ControlRequest cr = new ControlRequest("OP_LOGIN");
                GlobalFunctions.writeToBBDD("Authentication2", "> Sending request to the central node for approval");
                this.osCentral.writeObject(cr);

                Thread inactiveCentral2 = new Thread(new InactiveCentral2(this, "AuthLogin"));
                inactiveCentral2.start();

                ControlResponse crs = (ControlResponse) this.isCentral.readObject();
                this.done = true;
                
                GlobalFunctions.writeToBBDD("Authentication2", "> Response received from the central node, sending it back to the client node");
                crs.getArgs().add(GlobalFunctions.getUserName(emailDecrypt));
                this.osClient.writeObject(crs);
                GlobalFunctions.setLatency("AuthLogin", (this.end - this.start));
                this.resetCurrentTime();
            }else {
            	GlobalFunctions.writeToBBDD("Authentication2", "> Email or password not correct, login failed");
                this.osClient.writeObject(new ControlResponse("OP_LOGIN_NOK"));
            }
        } catch (Exception e) {
            System.out.println("doLogin (Auth2): " + e.getMessage());
            try {
				GlobalFunctions.writeToBBDD("Authentication2", "> There was an unexpected error");
				this.osClient.writeObject(new ControlResponse("OP_LOGIN_NOK"));
			} catch (IOException e1) {
				System.out.println("doLogin IoException (Auth2): " + e1.getMessage());
			}catch(Exception e1) {
				System.out.println("doLogin Exception (Auth2): " + e1.getMessage());
			}
        }
    }

    private void doConnect() {
        try {
            if (this.sCentral == null) {
                this.sCentral = new Socket(GlobalFunctions.getIP("CENTRAL"), GlobalFunctions.getPort("CENTRAL"));
                this.osCentral = new ObjectOutputStream(this.sCentral.getOutputStream());
                this.isCentral = new ObjectInputStream(this.sCentral.getInputStream());
            }
        } catch (Exception e) {
            System.out.println("doConnect (Auth2): " + e.getMessage());
        }
    }

    public void doDisconnect() {
        try {
            if (this.sCentral != null) {
                this.osCentral.close();
                this.osCentral = null;
                this.isCentral.close();
                this.isCentral = null;
                this.sCentral.close();
                this.sCentral = null;
            }
        } catch (Exception e) {
            System.out.println("doDisconnect (Auth2): " + e.getMessage());
        }
    }

    private void resetCurrentTime() {
        this.start = 0L;
        this.end = 0L;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }
    
    public void setEnd(long end) {
        this.end = end;
    }
}

class InactiveCentral2 implements Runnable {
    private Connection2 connection;
    private String type;

    public InactiveCentral2(Connection2 connection, String type) {
        this.connection = connection;
        this.type = type;
    }

    @Override
    public void run() {
        long sleep = 1000;

        try {
            sleep = GlobalFunctions.getLatency(this.type);
        } catch (Exception e) {
            System.out.println("InactiveCentral2 run: " + e.getMessage());
        }

        try {
            Thread.sleep(sleep);
            if (!this.connection.isDone()) {
                this.connection.doDisconnect();
                GlobalFunctions.writeToBBDD("Authentication2", "> Masking has been applied");
                GlobalFunctions.setLatency(this.type, GlobalFunctions.getLatency(this.type) * 2);
            }
            this.connection.setDone(false);
        } catch (InterruptedException e) {
            System.out.println("InterruptedException run InactiveCentral2: " + e.getMessage());
        }catch(Exception e) {
        	System.out.println("Exception run InactiveCentral2: " + e.getMessage());
        }
    }
}