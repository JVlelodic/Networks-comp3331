import java.io.*;
import java.net.*;

public class WebServer {
	
	//Path to current directory 
	private static final String CURRDIR = System.getProperty("user.dir") + "\\";
	
	public static void main(String[] args)throws Exception {
		
		if(args.length != 1){
			System.out.println("Missing argument: portNumber"); 
			return; 
		}
		
		int serverPort = Integer.parseInt(args[0]); 
		
        //We will listen on this port for connection request from clients
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
//        System.out.println("Server is ready :");     

		while (true){

		    // accept connection from connection queue
		    Socket connectionSocket = welcomeSocket.accept();
           
		    // create read stream to get input
		    BufferedReader input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		    String clientSentence = input.readLine();
//		    System.out.println(clientSentence);
		    
		    // read and filter request
		    String[] getRequest = clientSentence.split(" ");
		    String requestString = getRequest[1]; 
		    System.out.println(requestString);
		    StringBuilder builder = new StringBuilder(requestString); 
		    builder.deleteCharAt(0); 
		    String path = CURRDIR + builder.toString(); 
		    
		    //create output connection 
		    PrintWriter response = new PrintWriter(connectionSocket.getOutputStream()); 
		    File file = new File(path); 
		    System.out.println(file.exists());
		    
		    if(!file.isFile()) {
		    	response.print("HTTP/1.1 404\r\n"); 
		    	response.print("Content-Type: text/html\r\n");
		    	response.print("Connection: close\r\n"); 
		    	response.print("\r\n"); 
		    	response.print("<H1>404 Not Found</H1>" + "\r\n");
		    	response.flush(); 
		    }else{
		    	System.out.println(requestString);
			    String[] convert = requestString.split("\\.");
			    String fileType = convert[1];
		    	//For html files		    	
			    if(fileType.equals("html")) {
			    	response.print("HTTP/1.1 200\r\n"); 
			    	response.print("Content-Type: text/html\r\n");
			    	response.print("Connection: close\r\n"); 
			    	response.print("\r\n"); 
			   		    	
			    	FileReader fr = new FileReader(file); 
			    	BufferedReader br = new BufferedReader(fr); 
			    	String line; 
			    	while((line = br.readLine()) != null) {
			    		response.write(line + "\r\n" );
			    	}
			    	response.flush();
			    	br.close();
		    	}else if(fileType.equals("png")) {
		    		System.out.println("This is an image");
		    		response.print("HTTP/1.1 200\r\n"); 
			    	response.print("Content-Type: image/png\r\n");
			    	response.print("Connection: close\r\n"); 
			    	response.print("\r\n"); 
		
			    	BufferedReader imageBR = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			    	String line; 
			    	while((line = imageBR.readLine()) != null) {
			    		response.write(line + "\r\n" );
			    	}
			    	response.flush(); 
			    	imageBR.close(); 
		    	}
			    
		    }	   
		    input.close();
            connectionSocket.close();
		}
	}
}

