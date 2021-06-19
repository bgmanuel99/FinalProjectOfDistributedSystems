package FinalProject;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import protocol.*;
import Global.GlobalFunctions;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import FilterApp.Filter;
import FilterApp.FilterHelper;

public class Node1 {
	
	static Filter filterImpl;
	
	public static void main(String [] args) {
		try {
			try {
				GlobalFunctions.initBBDD("NodeI");
			} catch (Exception e) {
				System.out.println("Exception main client: " + e.getMessage());
			}
			
			GlobalFunctions.writeToBBDD("NodeI", "> Initializing the ORB");
			
			ORB orb = ORB.init(args, null);
			
	        org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
	        
	        NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	        
	        String name = "Filter";
	        filterImpl = FilterHelper.narrow(ncRef.resolve_str(name));
	        System.out.println("Obtained a handle on server object: " + filterImpl);
	        System.out.println("Filter ready and waiting in node 1...");
	        
	        GlobalFunctions.writeToBBDD("NodeI", "> Obtained a handle on server object: " + filterImpl);
	        GlobalFunctions.writeToBBDD("NodeI", "> Filter ready and waiting in node 1...");
	        GlobalFunctions.writeToBBDD("NodeI", "----------------------------------------------------------");
	        
			ServerSocket listenSocket = new ServerSocket(GlobalFunctions.getPort("NODE1"));
			int index = GlobalFunctions.getPortNode("NODE1");
			while(true) {
				System.out.println("Waiting node " + index + "...");
				Socket socket = listenSocket.accept();
				try {
                	GlobalFunctions.writeToBBDD("NodeI", "> Accepted connection from: " + socket.getInetAddress().toString());
                }catch(Exception e) {
                	System.out.println("Exception main authentication: " + e.getMessage());
                }
				System.out.println("Accepted connection from: " + socket.getLocalAddress().toString());
				new Node1Class(socket, index, filterImpl);
			}
		} catch (IOException e) {
			System.out.println("IOException main Node1: " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception main Node1: " + e.getMessage());
		}
	}
}

class Node1Class extends Thread{
	private Socket socket, socketRight;
	private ObjectInputStream is, isRight;
	private ObjectOutputStream os, osRight;
	private int nodeIndex;
	private Filter filterImpl;
	
	public Node1Class(Socket socket, int nodeIndex, Filter filterImpl) {
		try {
			this.socket = socket;
			this.os = new ObjectOutputStream(this.socket.getOutputStream());
			this.is = new ObjectInputStream(this.socket.getInputStream());
			this.filterImpl = filterImpl;
			this.nodeIndex = nodeIndex;
			this.start();
		} catch (IOException e) {
			System.out.println("IOException Node1Class: " + e.getMessage());
		}
	}
	
	@Override
	public void run() {
		try {
			Request r = (Request) this.is.readObject();
			if(r.getType().equals("CONTROL_REQUEST")) {
				ControlRequest cr = (ControlRequest) r;
				if(cr.getSubtype().equals("OP_FILTER")) {
					GlobalFunctions.writeToBBDD("NodeI", "> A filter request has arrived from the servers");
					if(this.nodeIndex == (int) cr.getArgs().get(1)) {
						GlobalFunctions.writeToBBDD("NodeI", "> This is the choosen node to process the filter request. Calling to the corba server...");
						GlobalFunctions.setNodeToken("Node1Token", 1);
						String pathToFilterImage = this.filterImpl.getFilterImage(Integer.valueOf(cr.getArgs().get(0).toString()));
						ControlResponse crs = new ControlResponse("OP_FILTER_OK");
						crs.getArgs().add(pathToFilterImage);
						GlobalFunctions.writeToBBDD("NodeI", "> The path to the filtered image is: " + pathToFilterImage + ", sending it back to the central servers");
						this.os.writeObject(crs);
						GlobalFunctions.setNodeToken("Node1Token", -1);
						GlobalFunctions.writeToBBDD("NodeI", "----------------------------------------------------------");
					}else {
						GlobalFunctions.writeToBBDD("NodeI", "> This is not the choosen node to process the filter request. Sending request to the next node");
						new Node1InterceptMessage(this);
						this.doConnectRight();
						this.osRight.writeObject(cr);
						this.doDisconnectRight();
					}
				}
			}
		}catch(ClassNotFoundException e) {
			System.out.println("ClassNotFoundException (run): " + e.getMessage());
		}catch(IOException e) {
			System.out.println("IOException (run): " + e.getMessage());
		}catch(Exception e) {
            System.out.println("Exception (run): " + e.getMessage());
        }
	}
	
	public void doConnectRight() {
		try {
			if(this.socketRight == null) {
				this.socketRight = new Socket(GlobalFunctions.getIP("NODE2"), GlobalFunctions.getPort("NODE2"));
				this.isRight = new ObjectInputStream(this.socketRight.getInputStream());
				this.osRight = new ObjectOutputStream(this.socketRight.getOutputStream());
			}
		}catch(Exception e) {
			System.out.println("Exception doConnectRigth node1: " + e.getMessage());
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
			System.out.println("Exception doDisconnectRight node1: " + e.getMessage());
		}
	}
	
	public Socket getSocket() {
		return this.socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	
	public ObjectInputStream getIs() {
		return this.is;
	}

	public void setIs(ObjectInputStream is) {
		this.is = is;
	}

	public ObjectOutputStream getOs() {
		return this.os;
	}

	public void setOs(ObjectOutputStream os) {
		this.os = os;
	}

}

class Node1InterceptMessage extends Thread {
	private Node1Class node;
	private Socket leftSocket;
	private ObjectInputStream isLeft;
	private ObjectOutputStream osLeft;
	
	public Node1InterceptMessage(Node1Class node) {
		this.node = node;
	}
	
	@Override
	public void run() {
		try {
			ServerSocket listen = new ServerSocket(GlobalFunctions.getPort("NODE1MESSAGE"));
			System.out.println("Waiting special node...");
			
			GlobalFunctions.writeToBBDD("NodeI", "----------------------------------------------------------");
			GlobalFunctions.writeToBBDD("NodeI", "> Waiting the response in the thread created for NodeI");
			GlobalFunctions.writeToBBDD("NodeI", "----------------------------------------------------------");
			
			this.leftSocket = listen.accept();
			GlobalFunctions.writeToBBDD("NodeI", "> Accepted connection from: " + leftSocket.getInetAddress().toString());
			this.isLeft = new ObjectInputStream(this.leftSocket.getInputStream());
			this.osLeft = new ObjectOutputStream(this.leftSocket.getOutputStream());
			
			ControlResponse crs = (ControlResponse) this.isLeft.readObject();
			GlobalFunctions.writeToBBDD("NodeI", "> Sending response to the central servers");
			this.node.getOs().writeObject(crs);
			
			if(this.leftSocket != null) {
				this.isLeft.close();
				this.isLeft = null;
				this.osLeft.close();
				this.osLeft = null;
				this.leftSocket.close();
				this.leftSocket = null;
			}else {
				GlobalFunctions.writeToBBDD("NodeI", "> There was an unexpected error");
				throw new Exception("This should not happen");
			}
			GlobalFunctions.writeToBBDD("NodeI", "----------------------------------------------------------");
		}catch(Exception e) {
			System.out.println("Exception run Node1InterceptMessage: " + e.getMessage());
		}
	}
}
