package FinalProject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import Global.GlobalFunctions;
import protocol.*;

public class CentralServerToken {
	public static void main(String [] args) {
		try {
        	try {
    			GlobalFunctions.initBBDD("CentralServerToken");
    		} catch (Exception e) {
    			System.out.println("Exception main CentralServerToken: " + e.getMessage());
    		}
            ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("CENTRALSERVERTOKEN"));
            
            CentralServerTokenConnection centralServerToken = new CentralServerTokenConnection();

            while (true) {
                System.out.println("Waiting CentralServerToken node...");
                Socket socket = listenSocket.accept();
                try {
                	GlobalFunctions.writeToBBDD("CentralServerToken", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main CentralServerToken: " + e.getMessage());
                }
                System.out.println("Accepted connection from: " + socket.getInetAddress().toString());

                centralServerToken.addSocket(socket);
            }
        } catch (Exception e) {
            System.out.println("Main (CentralServerToken): " + e.getMessage());
        }
	}
}

class CentralServerTokenConnection extends Thread{
	private Vector<Socket> sockets;
	private Vector<ObjectOutputStream> os;
	private Vector<ObjectInputStream> is;
	private boolean token;
	
	public CentralServerTokenConnection() {
		this.sockets = new Vector<Socket>();
		this.os = new Vector<ObjectOutputStream>();
		this.is = new Vector<ObjectInputStream>();
		this.token = true;
		this.start();
	}
	
	@Override
	public void run() {
		while(true) {			
			try {
				if(this.token && this.sockets.size() != 0) {
					GlobalFunctions.writeToBBDD("CentralServerToken", "> The token is free and there is a node in the queue");
					this.token = false;
					GlobalFunctions.writeToBBDD("CentralServerToken", "> Sending the token to the node");
					this.os.elementAt(0).writeObject(new ControlResponse("OP_GET_TOKEN_OK"));
					
					if(this.sockets.elementAt(0) != null) {						
						this.os.elementAt(0).close();
						this.os.set(0, null);
						this.os.remove(0);
						this.is.elementAt(0).close();
						this.is.set(0, null);
						this.is.remove(0);
						this.sockets.elementAt(0).close();
						this.sockets.set(0, null);
						this.sockets.remove(0);
					}
				}
			}catch(Exception e) {
				System.out.println("Exception run CentralServerTokenConnection: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public void addSocket(Socket socket) {
		try {
			ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
			Request r = (Request) is.readObject();
			if(r.getType().equals("DATA_REQUEST")) {
				DataRequest dr = (DataRequest) r;
				if(dr.getSubtype().equals("OP_RETURN_TOKEN")) {
					GlobalFunctions.writeToBBDD("CentralServerToken", "> Returning the token from a node");
					this.token = true;
					os.writeObject(new ControlResponse("OP_RETURN_TOKEN_OK"));
					GlobalFunctions.writeToBBDD("CentralServerToken", "> Token returned, sending response back to the node");
					GlobalFunctions.writeToBBDD("CentralServerToken", "----------------------------------------------------------");
					if(socket != null) {
						os.close();
						os = null;
						is.close();
						is = null;
						socket.close();
						socket = null;
					}
				}else if(dr.getSubtype().equals("OP_GET_TOKEN")) {
					GlobalFunctions.writeToBBDD("CentralServerToken", "> New request to get the token, adding the node to the queue");
					this.sockets.add(socket);
					this.os.add(os);
					this.is.add(is);
				}
			}
		} catch (IOException e) {
			System.out.println("IOException addSocket CentralServerTokenConnection: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.out.println("ClassNotFoundException addSocket CentralServerTokenConnection: " + e.getMessage());
		}catch(Exception e) {
			System.out.println("Exception addSocket CentralServerTokenConnection: " + e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public Vector<Socket> getSockets() {
		return sockets;
	}

	public void setSockets(Vector<Socket> sockets) {
		this.sockets = sockets;
	}

	public boolean isToken() {
		return token;
	}

	public void setToken(boolean token) {
		this.token = token;
	}
}