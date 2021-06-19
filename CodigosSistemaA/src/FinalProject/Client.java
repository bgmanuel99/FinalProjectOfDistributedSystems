package FinalProject;

import protocol.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.net.Socket;

import Global.GlobalFunctions;

public class Client {
    public final  String version = "1.0";

    private Socket socket;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private Console console;
    private String nick;
    private long start, end;
    private boolean done;

    public static void main(String[] args) {
    	GlobalFunctions.initFile("ClientLogin");
    	GlobalFunctions.initFile("ClientRegister");
    	GlobalFunctions.initFile("ClientFilter");
    	GlobalFunctions.initFile("Node1Token");
    	GlobalFunctions.initFile("Node2Token");
    	GlobalFunctions.initFile("Node3Token");
    	try {
			GlobalFunctions.initBBDD("Client");
			GlobalFunctions.initBBDD("CorbaServer");
		} catch (Exception e) {
			System.out.println("Exception main client: " + e.getMessage());
		}
        new Client();
    }

    public void init() {
        try {
            this.console = new Console(this.version);
            this.start = 0;
            this.start = 0;
            this.nick = "";
            this.done = false;
        } catch (Exception e) {
            System.out.println("Exception init client: " + e.getMessage());
        }
    }

    public Client() {
        this.init();

        String cmd = this.console.getCommand();

        while (!cmd.equals("close")) {
            try {
                if (cmd.equals("login")) {
                	GlobalFunctions.writeToBBDD("Client", "> New login request");
                    if (this.nick == "") {
                    	this.doConnectCentralServerToken();
                    	GlobalFunctions.writeToBBDD("Client", "> Sending a request to the central server to get the token");
                    	DataRequest dr = new DataRequest("OP_GET_TOKEN");
                    	this.os.writeObject(dr);
                    	ControlResponse crs = (ControlResponse) this.is.readObject();
                    	if(!crs.getSubtype().equals("OP_GET_TOKEN_OK")) throw new Exception("There was an unexpected error");
                    	GlobalFunctions.writeToBBDD("Client", "> Response received from the central server, so this request have now the token");
                    	this.doDisconnect();
                    	
                    	GlobalFunctions.writeToBBDD("Client", "> Requesting the authentication time");
                    	long t1 = System.currentTimeMillis();
                    	this.doConnectAuth(1);
                    	this.os.writeObject(new DataRequest("OP_GET_TIME"));
                    	ControlResponse crsTime = (ControlResponse) this.is.readObject();
                    	long t = (long) crsTime.getArgs().get(0);
                    	GlobalFunctions.writeToBBDD("Client", "> The time of the authentication node has arraived, calculating the correction time...");
                    	long t2 = System.currentTimeMillis();
                    	long Tround = t2 - t1;
                    	long correction = t + (Tround/2);
                    	GlobalFunctions.setLatency("ClientLogin", correction);
                    	this.doDisconnect();
                    	
                        this.doConnectAuth(1);
                        String[] credentials = this.console.getCommandLogin();
                        GlobalFunctions.writeToBBDD("Client", "> This are the credentials -> Email: " + credentials[0] + ", passwd: " + credentials[1]);
                        this.doLogin(credentials);
                        this.doDisconnect();
                        
                        this.doConnectCentralServerToken();
                        DataRequest dr1 = new DataRequest("OP_RETURN_TOKEN");
                        GlobalFunctions.writeToBBDD("Client", "> Returning the token to the central server to release it for another request");
                        this.os.writeObject(dr1);
                        ControlResponse crs1 = (ControlResponse) this.is.readObject();
                        if(!crs1.getSubtype().equals("OP_RETURN_TOKEN_OK")) throw new Exception("There was an unexpected error");
                        GlobalFunctions.writeToBBDD("Client", "> Token returned successfully");
                        this.doDisconnect();
                    } else {
                    	GlobalFunctions.writeToBBDD("Client", "> There is another user connected, login failed");
                    	this.console.writeMessage("There is another user connected");
                    }
                } else if(cmd.equals("register")) {
                	GlobalFunctions.writeToBBDD("Client", "> New register request");
                    if(this.nick == "") {
                    	this.doConnectCentralServerToken();
                    	GlobalFunctions.writeToBBDD("Client", "> Sending a request to the central server to get the token");
                    	DataRequest dr = new DataRequest("OP_GET_TOKEN");
                    	this.os.writeObject(dr);
                    	ControlResponse crs = (ControlResponse) this.is.readObject();
                    	if(!crs.getSubtype().equals("OP_GET_TOKEN_OK")) throw new Exception("There was an unexpected error");
                    	GlobalFunctions.writeToBBDD("Client", "> Response received from the central server, so this request have now the token");
                    	this.doDisconnect();
                    	
                        this.doConnectAuth(1);
                        String [] credentials = this.console.getCommandRegister();
                        GlobalFunctions.writeToBBDD("Client", "> This are the credentials -> Name: " + credentials[0] + ", email: " + credentials[1] + ", passwd: " + credentials[2]);
                        this.doRegister(credentials);
                        this.doDisconnect();
                        
                        this.doConnectCentralServerToken();
                        DataRequest dr1 = new DataRequest("OP_RETURN_TOKEN");
                        GlobalFunctions.writeToBBDD("Client", "> Returning the token to the central server to release it for another request");
                        this.os.writeObject(dr1);
                        ControlResponse crs1 = (ControlResponse) this.is.readObject();
                        if(!crs1.getSubtype().equals("OP_RETURN_TOKEN_OK")) throw new Exception("There was an unexpected error");
                        GlobalFunctions.writeToBBDD("Client", "> Token returned successfully");
                        this.doDisconnect();
                    }else {
                    	GlobalFunctions.writeToBBDD("Client", "> You can only register when there are no users online, register failed");
                    	this.console.writeMessage("You can only register when there are no users online");
                    }
                } else if (cmd.equals("filter")) {
                	GlobalFunctions.writeToBBDD("Client", "> New filter request");
                    if (this.nick != "") {
                        this.doConnectCentral();
                        String params = this.console.getCommandFilter();
                        String whichFilter = "";
                        if(params == "1") whichFilter = "RemoveGreenChannel filter";
                        else if(params == "2") whichFilter = "RemoveBlueChannel filter";
                        else if(params == "3") whichFilter = "RemoveRedChannel filter";
                        else if(params == "4") whichFilter = "BlackWhite filter";
                        else if(params == "5") whichFilter = "Gray filter";
                        GlobalFunctions.writeToBBDD("Client", "> The filter to be applied is: " + whichFilter);
                        this.doFilter(params);
                        this.doDisconnect();
                    } else {
                    	GlobalFunctions.writeToBBDD("Client", "> You need to be connected to use the filter command, filter request failed");
                    	this.console.writeMessage("You need to be connected to use the filter command");
                    }
                } else if (cmd.equals("logout")) {
                    if (this.nick != "") {
                        this.nick = "";
                        this.console.setPrompt("v", this.version);
                        this.console.writeMessage("Disconnecting from user account...");
                    } else this.console.writeMessage("You were already logout");
                }
                GlobalFunctions.writeToBBDD("Client", "----------------------------------------------------------");
            } catch (Exception e) {
                System.out.println("Exception client constructor: " + e.getMessage());
                e.printStackTrace();
            }
            
            cmd = this.console.getCommand();
        }
    }

    private void doLogin(String[] credentials) {
        try {
            ControlRequest cr = new ControlRequest("OP_LOGIN");
            cr.getArgs().add(GlobalFunctions.encryptMessage(credentials[0]));
            cr.getArgs().add(GlobalFunctions.encryptMessage(credentials[1]));

            GlobalFunctions.writeToBBDD("Client", "> Sending request to the authentication node");
            this.os.writeObject(cr);

            Thread inactiveAuth = new Thread(new InactiveNode(this, "ClientLogin"));
            inactiveAuth.start();

            ControlResponse crs = (ControlResponse) this.is.readObject();
            this.done = true;
            if(crs.getSubtype().equals("OP_LOGIN_OK")){
            	GlobalFunctions.writeToBBDD("Client", "> The login was successful and this is your username: " + crs.getArgs().get(0).toString());
                this.nick = crs.getArgs().get(0).toString();
                this.console.setPrompt(this.nick, this.version);
                this.console.writeMessage("Login successfuly!!");
            }else if(crs.getSubtype().equals("OP_LOGIN_NOK")){
            	GlobalFunctions.writeToBBDD("Client", "> The login request failed");
            	this.console.writeMessage("Login failed...");
            }
        } catch (Exception e) {
            System.out.println("DoLogin (Client): " + e.getMessage());
        }
    }

    private void doFilter(String params) {
        try {
            this.start = System.currentTimeMillis();
            ControlRequest cr = new ControlRequest("OP_FILTER");
            cr.getArgs().add(params);

            GlobalFunctions.writeToBBDD("Client", "> Sending request to the central node");
            this.os.writeObject(cr);
            Thread inactiveCentral = new Thread(new InactiveNode(this, "ClientFilter"));
            inactiveCentral.start();

            ControlResponse crs = (ControlResponse) this.is.readObject();
            this.done=true;
            if(crs.getSubtype().equals("OP_FILTER_OK")){
            	GlobalFunctions.writeToBBDD("Client", "> The filter operation was successful and the path to the new image is: " + crs.getArgs().get(0).toString());
                this.console.writeMessage("This is the path to the filtered image: " + crs.getArgs().get(0).toString());
            }else if(crs.getSubtype().equals("OP_FILTER_NOK")){
            	GlobalFunctions.writeToBBDD("Client", "> The filter operation failed due to unexpected errors");
                this.console.writeMessage(crs.getArgs().get(0).toString());
            }
            this.end = System.currentTimeMillis();
            GlobalFunctions.setLatency("ClientFilter", (this.end-this.start));
        } catch (Exception e) {
            System.out.println("doFilter (Client): " + e.getMessage());
        }
    }

    private void doRegister(String [] credentials) {
        try{
            this.start = System.currentTimeMillis();
            ControlRequest cr = new ControlRequest("OP_REGISTER");
            cr.getArgs().add(GlobalFunctions.encryptMessage(credentials[0]));
            cr.getArgs().add(GlobalFunctions.encryptMessage(credentials[1]));
            cr.getArgs().add(GlobalFunctions.encryptMessage(credentials[2]));

            GlobalFunctions.writeToBBDD("Client", "> Sending request to the authentication node");
            this.os.writeObject(cr);
            Thread inactiveAuth = new Thread(new InactiveNode(this,"ClientRegister"));
            inactiveAuth.start();

            ControlResponse crs = (ControlResponse) this.is.readObject();
            this.done = true;
            if(crs.getSubtype().equals("OP_REGISTER_OK")) {
            	GlobalFunctions.writeToBBDD("Client", "> The register was successful");
                this.console.writeMessage("You have register successfuly");
            }else if(crs.getSubtype().equals("OP_REGISTER_NOK")) {
            	GlobalFunctions.writeToBBDD("Client", "> There was a problem while registering the new client");
                this.console.writeMessage("There was a problem during the registration or the user is already registered");
            }
            this.end = System.currentTimeMillis();
            GlobalFunctions.setLatency("ClientRegister", (this.end-this.start));
        }catch(Exception e) {
            System.out.println("Exception doRegister (Client): " + e.getMessage());
        }
    }

    private void doConnectAuth(int count) {
        try {
            if(count > GlobalFunctions.getExternalVariables("MAXAUTH")) {
            	GlobalFunctions.writeToBBDD("Client", "> All the authentication nodes are disconnected");
            	throw new Exception("All the authentification nodes are disconnected");
            }
            if (this.socket == null) {
                this.socket = new Socket(GlobalFunctions.getIP("AUTH" + count), GlobalFunctions.getPort("AUTH" + count));
                this.os = new ObjectOutputStream(this.socket.getOutputStream());
                this.is = new ObjectInputStream(this.socket.getInputStream());
            }
        } catch (UncheckedIOException e) {
            System.out.println("Client (Auth)" + e.getMessage());
        } catch (IOException e) {
        	try {
				GlobalFunctions.writeToBBDD("Client", "> There was a problem while connecting to the first authentication node. Connecting with the second authentication node...");
			} catch (IOException e1) {
				System.out.println("Client (Auth) 1: " + e1.getMessage());
			} catch (Exception e1) {
				System.out.println("Client (Auth) 1: " + e1.getMessage());
			}
            System.out.println("Client (Auth) 2" + e.getMessage());
            this.doConnectAuth(count++);
        } catch (Exception e) {
            System.out.println("Client (Auth) 2" + e.getMessage());
        }
    }

    private void doConnectCentral() {
        try {
            if (this.socket == null) {
                this.socket = new Socket(GlobalFunctions.getIP("CENTRAL"), GlobalFunctions.getPort("CENTRAL")); 
                this.os = new ObjectOutputStream(this.socket.getOutputStream());
                this.is = new ObjectInputStream(this.socket.getInputStream());
            }
        } catch (UncheckedIOException e) {
            System.out.println("Client (Central)" + e.getMessage());

        } catch (IOException e) {
            System.out.println("Client (Central)" + e.getMessage());

        } catch (Exception e) {
            System.out.println("Client (Central)" + e.getMessage());
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
            System.out.println("Client (CentralServerToken)" + e.getMessage());

        } catch (IOException e) {
            System.out.println("Client (CentralServerToken)" + e.getMessage());

        } catch (Exception e) {
            System.out.println("Client (CentralServerToken)" + e.getMessage());
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
            System.out.println("Disconnect (Client): " + e.getMessage());
        }
    }

    private void resetCurrentTime() {
        this.start = 0L;
        this.end = 0L;
    }

    public String getVersion() {
        return this.version;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ObjectOutputStream getOs() {
        return this.os;
    }

    public void setOs(ObjectOutputStream os) {
        this.os = os;
    }

    public ObjectInputStream getIs() {
        return this.is;
    }

    public void setIs(ObjectInputStream is) {
        this.is = is;
    }

    public Console getConsole() {
        return this.console;
    }

    public void setConsole(Console console) {
        this.console = console;
    }

    public String getNick() {
        return this.nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public long getStart() {
        return this.start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return this.end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}

class InactiveNode implements Runnable {
    private Client client;
    private String type;

    public InactiveNode(Client client, String type) {
        this.client = client;
        this.type = type;
    }

    @Override
    public void run() {
        long sleep = 1000;
        
        try{
            sleep = GlobalFunctions.getLatency(this.type);
        }catch(Exception e) {
            System.out.println("InactiveNode run: " + e.getMessage());
        }

        try {
            Thread.sleep(sleep);
            if(!this.client.isDone()) {
                this.client.doDisconnect();
                GlobalFunctions.writeToBBDD("Client", "> Masking has been applied");
                GlobalFunctions.setLatency(this.type, GlobalFunctions.getLatency(this.type)*2);
            }
            this.client.setDone(false);
        }catch(InterruptedException e) {
            System.out.println("InterruptedException run InactiveNode: " + e.getMessage());
        }catch(Exception e) {
        	System.out.println("Exception run InactiveNode: " + e.getMessage());
        }
    }
}