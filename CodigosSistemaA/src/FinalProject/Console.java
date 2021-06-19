package FinalProject;

import java.io.*;
import java.util.Scanner;

import Global.GlobalFunctions;

public class Console {
    public static String prompt;
    private InputStreamReader isr;
    private BufferedReader br;
    private String version, nick = "v";
    
    public Console(String version){
        this.isr = new InputStreamReader(System.in);
        this.br = new BufferedReader(this.isr);
        this.version = version;
        Console.prompt = "Cliente v " + this.version + "> ";
    }

    public void writeMessage(String msg) {
        System.out.println("> " + msg);
    }
    
    public String getCommand() {
        String line = "";

        try {
            System.out.print(Console.prompt);
            line = this.br.readLine();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        
        return line;
    }

    public String[] getCommandLogin() {
        String [] credentials = new String[2];

        try {
            System.out.print("Choose an email: ");
            credentials[0] = this.br.readLine();

            System.out.print("Choose a password: ");
            credentials[1] = this.br.readLine();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        
        return credentials;
    }

    public String[] getCommandRegister() {
        String [] credentials = new String[3];

        try {
            System.out.print("User name: ");
            credentials[0] = this.br.readLine();

            System.out.print("Email: ");
            credentials[1] = this.br.readLine();

            System.out.print("Password: ");
            credentials[2] = this.br.readLine();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return credentials;
    }

    public String getCommandFilter() throws Exception {
    	String params = "";
    	
    	boolean one = false, two = false, three = false, four = false, five = false;
    	
    	File file = new File("//MANUEL-VIRTUALB/possibleFilters/filtersToUse.txt");
    	if(file.exists()) {
    		Scanner scanner = new Scanner(file);
    		while(scanner.hasNext()) {
    			String next = scanner.nextLine();
    			if(next.equals("filterRemoveGreenChannel")) one = true;
    			else if(next.equals("filterRemoveBlueChannel")) two = true;
    			else if(next.equals("filterRemoveRedChannel")) three = true;
    			else if(next.equals("filterBlackWhite")) four = true;
    			else if(next.equals("filterGray")) five = true;
    		}
    	}else throw new Exception("The file " + file.getName() + " does not exists");
    	
        try {
            System.out.println("Types of filter");
            System.out.println("---------------");
            System.out.println("1. RemoveGreenChannel" + (one ? "(available)" : "(not available)"));
            System.out.println("2. RemoveBlueChannel" + (two ? "(available)" : "(not available)"));
            System.out.println("3. RemoveRedChannel" + (three ? "(available)" : "(not available)"));
            System.out.println("4. BlackWhiteFilter" + (four ? "(available)" : "(not available)"));
            System.out.println("5. GrayFilter" + (five ? "(available)" : "(not available)"));
            System.out.println("(Press the number of the filter to choose it)");
            System.out.print("Choose the filter: ");
            boolean end = true;
            while(end) {
            	params = this.br.readLine();
            	GlobalFunctions.writeToBBDD("Client", "" + params + ", " + one + ", " + two + ", " + three + ", " + four + ", " + five);
            	if(params.equals("1") && !one) {
            		GlobalFunctions.writeToBBDD("Client", "> The filter RemoveGreenChannel was selected but it is not available");
            		System.out.println("This filter is not available, please choose an available filter to use");
            	}else if(params.equals("2") && !two) {
            		GlobalFunctions.writeToBBDD("Client", "> The filter RemoveBlueChannel was selected but it is not available");
            		System.out.println("This filter is not available, please choose an available filter to use");
            	}else if(params.equals("3") && !three) {
            		GlobalFunctions.writeToBBDD("Client", "> The filter RemoveRedChannel was selected but it is not available");
            		System.out.println("This filter is not available, please choose an available filter to use");
            	}else if(params.equals("4") && !four) {
            		GlobalFunctions.writeToBBDD("Client", "> The filter BlackWhite was selected but it is not available");
            		System.out.println("This filter is not available, please choose an available filter to use");
            	}else if(params.equals("5") && !five) {
            		GlobalFunctions.writeToBBDD("Client", "> The filter GrayFilter was selected but it is not available");
            		System.out.println("This filter is not available, please choose an available filter to use");
            	}else {
            		end = false;
            	}
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return params;
    }

    public String getEmail(){
        String email = "";

        try {
            System.out.print("Email: ");
            email = this.br.readLine();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return email;
    }



    public void setPrompt(String nick, String version) {
		if(nick == null) nick = "v";
		else if(nick != "v"){
			if(nick.length() == 1) nick = nick.toUpperCase();
			else nick = String.valueOf(nick.charAt(0)).toUpperCase() + nick.substring(1, nick.length());
		}
		if(version == null) version = "1.0";
				
		Console.prompt = "Client " + nick + " " + version + "> ";
	}
}