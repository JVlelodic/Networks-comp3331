import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Server extends Thread {
	
	private static int serverPort; 
	private static int blockDuration; 
	private static int timeout; 
	private static HashMap<String,String> users; 
	private static ArrayList<String> loggedUsers = new ArrayList<>(); 
	
	private static void loadUsers() {
		users = new HashMap<>();
		String currDir = System.getProperty("user.dir") + "\\"; 
		String path = currDir + "credentials.txt"; 
		File file = new File(path); 
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(file));
			String line; 
			while((line = br.readLine()) != null){
				String[] userDetails = line.split(" "); 
				users.put(userDetails[0], userDetails[1]); 
			}
		} catch (FileNotFoundException e) { 
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)throws Exception {
		
		if(args.length != 3) {
			System.out.println("Missing arguments: server_port block_duration timeout"); 
			return; 
		}
		
		serverPort = Integer.parseInt(args[0]); 
		blockDuration = Integer.parseInt(args[1]); 
		timeout = Integer.parseInt(args[2]); 
		
		loadUsers();
		
		/*create server socket that is assigned the serverPort (6789)
        We will listen on this port for connection request from clients */
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

		while (true){

		    // accept connection from connection queue
		    Socket connectionSocket = welcomeSocket.accept();
            /*When a client knocks on this door, the program invokes the accept( ) method for welcomeSocket, which creates a new socket in the server, called connectionSocket, dedicated to this particular client. The client and server then complete the handshaking, creating a TCP connection between the client’s clientSocket and the server’s connectionSocket. With the TCP connection established, the client and server can now send bytes to each other over the connection. With TCP, all bytes sent from one side not are not only guaranteed to arrive at the other side but also guaranteed to arrive in order*/
            
		    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
		    String clientInput; 
		    ArrayList<String> tmp = new ArrayList<>(); 
		    
		    while((clientInput = inFromClient.readLine()) != null) {
		    	tmp.add(clientInput); 
		    }
		    
		   
		    System.out.println(tmp);
		    
		    DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream()); 
		    outToClient.writeBytes("true");
		    
		    
		   
//            connectionSocket.close();
            /*In this program, after sending the capitalized sentence to the client, we close the connection socket. But since welcomeSocket remains open, another client can now knock on the door and send the server a sentence to modify.
             */
		} 

	} 

} 