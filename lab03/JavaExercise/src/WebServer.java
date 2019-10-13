import java.io.*;
import java.net.*;

public class WebServer {
	
	public static void main(String[] args)throws Exception {
        /* define socket parameters, Address + PortNo, Address will default to localhost */
		
		if(args.length != 1){
			System.out.println("Missing argument: portNumber"); 
			return; 
		}
		
		int serverPort = Integer.parseInt(args[0]); 
		
		/*create server socket that is assigned the serverPort (6789)
        We will listen on this port for connection request from clients */
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");     

		while (true){

		    // accept connection from connection queue
		    Socket connectionSocket = welcomeSocket.accept();
            /*When a client knocks on this door, the program invokes the accept( ) method for welcomeSocket, which creates a new socket in the server, called connectionSocket, dedicated to this particular client. 
             * The client and server then complete the handshaking, creating a TCP connection 
             * between the client’s clientSocket and the server’s connectionSocket. 
             * With the TCP connection established, the client and server can now 
             * send bytes to each other over the connection. With TCP, all bytes sent from
             *  one side not are not only guaranteed to arrive at the 
             *  other side but also guaranteed to arrive in order*/
            

		    // create read stream to get input
		    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		    
		    String clientSentence = inFromClient.readLine();
		    String[] getRequest = clientSentence.split(" ");
		    String filePath = getRequest[1]; 
		    System.out.println(filePath);
		    
		    PrintWriter response = new PrintWriter(connectionSocket.getOutputStream(),true); 
		    File file = new File(filePath); 
		    System.out.println(file.exists()); 
		    if(!file.exists()) {
		    	response.write("404 Error");
		    	System.out.println("this has run"); 
		    	
		    }
		   
		    BufferedReader fileReader = new Buffered
		    
		    // process input, change the case
//		    String capitalizedSentence;
//		    capitalizedSentence = clientSentence.toUpperCase() + '\n';
		    
		    // send reply
//		    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
//		    outToClient.writeBytes(capitalizedSentence);
            
            connectionSocket.close();
            /*In this program, after sending the capitalized sentence to the client, we close the connection socket. But since welcomeSocket remains open, another client can now knock on the door and send the server a sentence to modify.
             */
		} // end of while (true)

	} // end of main()

} // end of class TCPServer

