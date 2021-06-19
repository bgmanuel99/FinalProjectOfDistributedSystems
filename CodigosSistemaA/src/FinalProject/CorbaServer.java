package FinalProject;

import FilterApp.*;
import Global.GlobalFunctions;

import org.omg.CosNaming.*;

import java.io.IOException;

import org.omg.CORBA.*;
import org.omg.PortableServer.*;
import org.omg.PortableServer.POA;

class FilterImpl extends FilterPOA{

    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val; 
      }

    @Override
    public String getFilterImage(int filter) {
    	String path = "";
    	try {    		
    		GlobalFunctions.writeToBBDD("CorbaServer", "> Received new filter request");
    		if(filter == 1) {
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The choosed filter operation was: RemoveGreenChannel");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> Executing the python script which is in the distributed file system, its path is: //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveGreenChannel.py");
    			Process process = Runtime.getRuntime().exec("python //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveGreenChannel.py");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The script run correctly, the path to the new filtered image is: //MANUELIMAGES-VI/Images/filterRemoveGreenChannelImage.jpg");
    			path = "//MANUELIMAGES-VI/Images/filterRemoveGreenChannelImage.jpg";
    		}else if(filter == 2){
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The choosed filter operation was: RemoveBlueChannel");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> Executing the python script which is in the distributed file system, its path is: //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveBlueChannel.py");
    			Process process = Runtime.getRuntime().exec("python //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveBlueChannel.py");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The script run correctly, the path to the new filtered image is: //MANUELIMAGES-VI/Images/filterRemoveBlueChannelImage.jpg");
    			path = "//MANUELIMAGES-VI/Images/filterRemoveBlueChannelImage.jpg";
    		}else if(filter == 3) {
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The choosed filter operation was: RemoveRedChannel");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> Executing the python script which is in the distributed file system, its path is: //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveRedChannel.py");
    			Process process = Runtime.getRuntime().exec("python //MANUEL-VIRTUALB/pythonFilterScripts/filterRemoveRedChannel.py");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The script run correctly, the path to the new filtered image is: //MANUELIMAGES-VI/Images/filterRemoveRedChannelImage.jpg");
    			path = "//MANUELIMAGES-VI/Images/filterRemoveRedChannelImage.jpg";
    		}else if(filter == 4) {
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The choosed filter operation was: BlackWhiteFilter");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> Executing the python script which is in the distributed file system, its path is: //MANUEL-VIRTUALB/pythonFilterScripts/filterBlackWhite.py");
    			Process process = Runtime.getRuntime().exec("python //MANUEL-VIRTUALB/pythonFilterScripts/filterBlackWhite.py");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The script run correctly, the path to the new filtered image is: //MANUELIMAGES-VI/Images/filterBlackWhiteImage.jpg");
    			path = "//MANUELIMAGES-VI/Images/filterBlackWhiteImage.jpg";
    		}else if(filter == 5) {
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The choosed filter operation was: GrayFilter");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> Executing the python script which is in the distributed file system, its path is: //MANUEL-VIRTUALB/pythonFilterScripts/filterGray.py");
    			Process process = Runtime.getRuntime().exec("python //MANUEL-VIRTUALB/pythonFilterScripts/filterGray.py");
    			GlobalFunctions.writeToBBDD("CorbaServer", "> The script run correctly, the path to the new filtered image is: //MANUELIMAGES-VI/Images/filterGrayImage.jpg");
    			path = "//MANUELIMAGES-VI/Images/filterGrayImage.jpg";
    		}
    		GlobalFunctions.writeToBBDD("CorbaServer", "----------------------------------------------------------");
    	}catch(Exception e) {
    		try {
				GlobalFunctions.writeToBBDD("CorbaServer", "> There was an unexpected error");
				GlobalFunctions.writeToBBDD("CorbaServer", "----------------------------------------------------------");
			} catch (IOException e1) {
				System.out.println("IOException getFilterImage FilterImpl 1: " + e1.getMessage());
			} catch (Exception e1) {
				System.out.println("Exception getFilterImage FilterImpl: " + e1.getMessage());
			}
    		System.out.println("IOException getFilterImage FilterImpl 2: " + e.getMessage());
    	}
    	
    	return path;
    }

    @Override
    public void shutdown() {
        orb.shutdown(false);
    }
    
}

public class CorbaServer {
	public static void main(String [] args) {
		try {
			ORB orb = ORB.init(args, null);
			
			POA rootpoa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
			rootpoa.the_POAManager().activate();
			
			FilterImpl filterImpl = new FilterImpl();
			filterImpl.setORB(orb);
			
			org.omg.CORBA.Object ref = rootpoa.servant_to_reference(filterImpl);
	        Filter href = FilterHelper.narrow(ref);
			
			org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
			
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			String name = "Filter";
			NameComponent path[] = ncRef.to_name(name);
			ncRef.rebind(path, href);
			
			System.out.println("Filter Server ready and waiting ...");
        	orb.run();
		}catch(Exception e) {
			System.out.println("ERROR: " + e.getMessage());
		}
		
		System.out.println("Filter Server Exiting ...");
	}
}
