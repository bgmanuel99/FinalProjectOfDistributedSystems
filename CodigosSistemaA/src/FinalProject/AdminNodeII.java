package FinalProject;

import java.io.*;
import java.net.Socket;

import Global.*;
import protocol.*;

public class AdminNodeII {
    public final String version = "1.0";
    private Socket socket;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private Console console;

    public static void main(String[] args) {
    	try {
			GlobalFunctions.initBBDD("AdminII");
		} catch (Exception e) {
			System.out.println("Exception main client: " + e.getMessage());
		}
        new AdminNodeII();
    }

    public void init() {
        this.console = new Console(this.version);
        this.console.setPrompt("AdminII", this.version);
    }

    public AdminNodeII(){
        this.init();

        String cmd = this.console.getCommand();

        while(!cmd.equals("close")){
            try {
                if(cmd.equals("register")) {
                	GlobalFunctions.writeToBBDD("AdminII", "> New register request");
                	
                	this.doConnectCentralServerToken();
                	GlobalFunctions.writeToBBDD("AdminII", "> Sending a request to the central server to get the token");
                	DataRequest dr = new DataRequest("OP_GET_TOKEN");
                	this.os.writeObject(dr);
                	ControlResponse crs = (ControlResponse) this.is.readObject();
                	if(!crs.getSubtype().equals("OP_GET_TOKEN_OK")) throw new Exception("There was an unexpected error");
                	GlobalFunctions.writeToBBDD("AdminII", "> Response received from the central server, so this request have now the token");
                	this.doDisconnect();
                	
                    String [] credentials = this.console.getCommandRegister();
                    GlobalFunctions.writeToBBDD("AdminII", "> This are the credentials -> User: " + credentials[0] + ", email: " + credentials[1]+ ", passwd: " + credentials[2]);
                    if(!GlobalFunctions.isUser(credentials[1])) {
                    	GlobalFunctions.writeToBBDD("AdminII", "> Registering the new client");
                        GlobalFunctions.addUser(GlobalFunctions.encryptMessage(credentials[0]), GlobalFunctions.encryptMessage(credentials[1]), GlobalFunctions.encryptMessage(credentials[2]));
                        GlobalFunctions.writeToBBDD("AdminII", "> Register was successful");
                        this.console.writeMessage("You have register successfuly");
                    }else {
                    	GlobalFunctions.writeToBBDD("AdminII", "> Register operation failed, there is already a user with that email in the BBDD");
                        this.console.writeMessage("There is already a user with that email in the BBDD");
                    }
                    
                    this.doConnectCentralServerToken();
                    DataRequest dr1 = new DataRequest("OP_RETURN_TOKEN");
                    GlobalFunctions.writeToBBDD("AdminII", "> Returning the token to the central server to release it for another request");
                    this.os.writeObject(dr1);
                    ControlResponse crs1 = (ControlResponse) this.is.readObject();
                    if(!crs1.getSubtype().equals("OP_RETURN_TOKEN_OK")) throw new Exception("There was an unexpected error");
                    GlobalFunctions.writeToBBDD("AdminII", "> Token returned successfully");
                    this.doDisconnect();
                }else if(cmd.equals("delete")) {
                	GlobalFunctions.writeToBBDD("AdminII", "> New delete user request");
                	
                	this.doConnectCentralServerToken();
                	GlobalFunctions.writeToBBDD("AdminII", "> Sending a request to the central server to get the token");
                	DataRequest dr = new DataRequest("OP_GET_TOKEN");
                	this.os.writeObject(dr);
                	ControlResponse crs = (ControlResponse) this.is.readObject();
                	if(!crs.getSubtype().equals("OP_GET_TOKEN_OK")) throw new Exception("There was an unexpected error");
                	GlobalFunctions.writeToBBDD("AdminII", "> Response received from the central server, so this request have now the token");
                	this.doDisconnect();
                	
                    String email = this.console.getEmail();
                    GlobalFunctions.writeToBBDD("AdminII", "> This is the email of the user to delete: " + email);
                    if(GlobalFunctions.isUser(email)){
                        GlobalFunctions.writeToBBDD("AdminII", "> Deleting the user from the BBDD");
                    	if(!GlobalFunctions.deleteUser(email)){
                            GlobalFunctions.writeToBBDD("AdminII", "> There was a problem deleting the user from the BBDD");
                        	this.console.writeMessage("There was a probleam deleting the user or the user doenst exists");
                        }else {
                        	GlobalFunctions.writeToBBDD("AdminII", "> Thre user was deleted successfully");
                        	this.console.writeMessage("User deleted successfully");
                        }
                    }else{
                    	GlobalFunctions.writeToBBDD("AdminII", "> The especified email user was not found in the BBDD");
                        this.console.writeMessage("User not found...");
                    }
                    
                    this.doConnectCentralServerToken();
                    DataRequest dr1 = new DataRequest("OP_RETURN_TOKEN");
                    GlobalFunctions.writeToBBDD("AdminII", "> Returning the token to the central server to release it for another request");
                    this.os.writeObject(dr1);
                    ControlResponse crs1 = (ControlResponse) this.is.readObject();
                    if(!crs1.getSubtype().equals("OP_RETURN_TOKEN_OK")) throw new Exception("There was an unexpected error");
                    GlobalFunctions.writeToBBDD("AdminII", "> Token returned successfully");
                    this.doDisconnect();
                }
            } catch (Exception e) {
            	try {
					GlobalFunctions.writeToBBDD("AdminII", "> There was an unexpected error");
				} catch (IOException e1) {
					System.out.println("IOException constructor AdminII: " + e.getMessage());
				} catch (Exception e1) {
					System.out.println("Exception constructor AdminII: " + e.getMessage());
				}
                System.out.println("Exception constructor AdminII(2): "+e.getMessage());
            }

            cmd = this.console.getCommand();
        }
    }
    
    private void doConnectCentralServerToken() {
    	try {
    		if(this.socket == null) {
    			this.socket = new Socket(GlobalFunctions.getIP("CENTRALSERVERTOKEN"), GlobalFunctions.getPort("CENTRALSERVERTOKEN"));
    			this.os = new ObjectOutputStream(this.socket.getOutputStream());
    			this.is = new ObjectInputStream(this.socket.getInputStream());
    		}
    	} catch (UncheckedIOException e) {
            System.out.println("AdminII (doConnectCentralServerToken)" + e.getMessage());

        } catch (IOException e) {
            System.out.println("AdminII (doConnectCentralServerToken)" + e.getMessage());

        } catch (Exception e) {
            System.out.println("AdminII (doConnectCentralServerToken)" + e.getMessage());
        }
    }
    
    public void doDisconnect() {
        try {
            if (this.socket != null) {
                this.os.close();
                this.os = null;
                this.is.close();
                this.is = null;
                this.socket.close();
                this.socket = null;
            }
        } catch (Exception e) {
            System.out.println("Disconnect (AdminII): " + e.getMessage());
        }
    }
}