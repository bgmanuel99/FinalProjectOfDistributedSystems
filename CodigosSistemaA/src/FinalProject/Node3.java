package FinalProject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import FilterApp.Filter;
import FilterApp.FilterHelper;
import Global.GlobalFunctions;
import protocol.ControlRequest;
import protocol.ControlResponse;
import protocol.Request;

public class Node3 {
	
	static Filter filterImpl;
	
	public static void main(String [] args) {
		try {
			try {
				GlobalFunctions.initBBDD("NodeIII");
			} catch (Exception e) {
				System.out.println("Exception main client: " + e.getMessage());
			}
			
			GlobalFunctions.writeToBBDD("NodeIII", "> Initializing the ORB");
			
			ORB orb = ORB.init(args, null);
			
	        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
	        
	        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	        
	        String name = "Filter";
	        filterImpl = FilterHelper.narrow(ncRef.resolve_str(name));
	        System.out.println("Obtained a handle on server object: " + filterImpl);
	        System.out.println("Filter ready and waiting in node 3...");
	        
	        GlobalFunctions.writeToBBDD("NodeIII", "> Obtained a handle on server object: " + filterImpl);
	        GlobalFunctions.writeToBBDD("NodeIII", "> Filter ready and waiting in node 1...");
	        GlobalFunctions.writeToBBDD("NodeIII", "----------------------------------------------------------");
	        
			ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("NODE3"));
			int index = GlobalFunctions.getPortNode("NODE3");
			while(true) {
				System.out.println("Waiting node " + index + "...");
				Socket socket = listenSocket.accept();
				try {
                	GlobalFunctions.writeToBBDD("NodeIII", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main authentication: " + e.getMessage());
                }
				System.out.println("Accepted connection from: " + socket.getLocalAddress().toString());
				
				new Node3Class(socket, index, filterImpl);
			}
		} catch (IOException e) {
			System.out.println("IOException main Node3: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception main Node3: " + e.getMessage());
		}
	}
}

class Node3Class extends Thread{
	private Socket socket, socketRight;
	private ObjectInputStream is, isRight;
	private ObjectOutputStream os, osRight;
	private int nodeIndex;
	private Filter filterImpl;
	
	public Node3Class(Socket socket, int nodeIndex, Filter filterImpl) {
		try {
			this.socket = socket;
			this.os = new ObjectOutputStream(this.socket.getOutputStream());
			this.is = new ObjectInputStream(this.socket.getInputStream());
			this.nodeIndex = nodeIndex;
			this.filterImpl = filterImpl;
			this.start();
		} catch (IOException e) {
			System.out.println("IOException Node3Class: " + e.getMessage());
		}
	}
	
	@Override
	public void run() {
		try {
			Request r = (Request) this.is.readObject();
			if(r.getType().equals("CONTROL_REQUEST")) {
				ControlRequest cr = (ControlRequest) r;
				if(cr.getSubtype().equals("OP_FILTER")) {
					GlobalFunctions.writeToBBDD("NodeIII", "> A filter request has arrived from NodeII");
					if(this.nodeIndex == (int) cr.getArgs().get(1)) {
						GlobalFunctions.writeToBBDD("NodeIII", "> This is the choosen node to process the filter request. Calling to the corba server...");
						GlobalFunctions.setNodeToken("Node3Token", 1);
						this.doConnectRight();
						String pathToFilterImage = this.filterImpl.getFilterImage(Integer.valueOf(cr.getArgs().get(0).toString()));
						ControlResponse crs = new ControlResponse("OP_FILTER_OK");
						GlobalFunctions.writeToBBDD("NodeIII", "> The path to the filtered image is: " + pathToFilterImage + ", sending it the response to NodeI");
						crs.getArgs().add(pathToFilterImage);
						this.osRight.writeObject(crs);
						this.doDisconnectRight();
						GlobalFunctions.setNodeToken("Node3Token", -1);
						GlobalFunctions.writeToBBDD("NodeIII", "----------------------------------------------------------");
					}else {
						GlobalFunctions.writeToBBDD("NodeIII", "> There was an internal problem that result in an unexpected error");
						this.doConnectRight();
						ControlResponse crs = new ControlResponse("OP_FILTER_NOK");
						crs.getArgs().add("There was an unexpected error");
						this.osRight.writeObject(crs);
						this.doDisconnectRight();
					}
				}else if(cr.getSubtype().equals("OP_FILTER_OK")) {
					GlobalFunctions.writeToBBDD("NodeIII", "> Sending the response that other node did correctly the filter request to NodeI");
					this.doConnectRight();
					this.osRight.writeObject(cr);
					this.doDisconnectRight();
				}
			}
		}catch(ClassNotFoundException e) {
			System.out.println("ClassNotFoundException Node3 (run): " + e.getMessage());
		}catch(IOException e) {
			System.out.println("IOException Node3 (run): " + e.getMessage());
		}catch(Exception e) {
            System.out.println("Exception Node3 (run): " + e.getMessage());
            e.printStackTrace();
        }
	}
	
	public void doConnectRight() {
		try {
			if(this.socketRight == null) {
				this.socketRight = new Socket(GlobalFunctions.getIP("NODE1MESSAGE"), GlobalFunctions.getPort("NODE1MESSAGE"));
				this.isRight = new ObjectInputStream(this.socketRight.getInputStream());
				this.osRight = new ObjectOutputStream(this.socketRight.getOutputStream());
			}
		}catch(Exception e) {
			System.out.println("Exception doConnectRigth node3: " + e.getMessage());
		}
	}
	
	public void doDisconnectRight() {
		try {
			if(this.socketRight != null) {
				this.isRight.close();
				this.isRight = null;
				this.osRight.close();
				this.osRight = null;
				this.socketRight.close();
				this.socketRight = null;
			}
		}catch(Exception e) {
			System.out.println("Exception doDisconnectRight node3: " + e.getMessage());
		}
	}
}