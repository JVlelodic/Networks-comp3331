import java.io.*;
import java.net.*;
import java.nio.file.Files;

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

		while (true){

		    // accept connection from connection queue
		    Socket connectionSocket = welcomeSocket.accept();

		    // create read stream to get input
		    BufferedReader input = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		    String clientSentence = input.readLine();
		    
		    // read and filter request
		    String[] getRequest = clientSentence.split(" ");
		    String requestString = getRequest[1];
		    StringBuilder builder = new StringBuilder(requestString);
		    builder.deleteCharAt(0);
		    String path = CURRDIR + builder.toString();

		    //create output connection
		    File file = new File(path);

	    	DataOutputStream response = new DataOutputStream(connectionSocket.getOutputStream());
	    	
	    	if(!file.isFile()) {
		    	response.writeBytes("HTTP/1.1 404\r\n");
		    	response.writeBytes("Content-Type: text/html\r\n");
		    	response.writeBytes("Connection: close\r\n");
		    	response.writeBytes("\r\n");
		    	response.writeBytes("<H1>404 Not Found</H1>" + "\r\n");
		    }else{
				byte[] fileContent = Files.readAllBytes(file.toPath());
				
			    String[] convert = requestString.split("\\.");
			    String fileType = convert[1];
			    response.writeBytes("HTTP/1.1 200\r\n");
			    if(fileType.equals("html")) {
			    	response.writeBytes("Content-Type: text/html\r\n");
			    }else if(fileType.equals("png")) {
			    	response.writeBytes("Content-Type: image/png\r\n"); 
			    }
			    response.writeBytes("Connection: close\r\n");
		    	response.writeBytes("\r\n");
		    	response.write(fileContent);
		    }
            connectionSocket.close();
		}
	}
}
