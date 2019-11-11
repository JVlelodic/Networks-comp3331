import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;

public class Server extends Thread {
	
	private Socket socket; 
	private ObjectOutputStream outToClient;
   	private ObjectInputStream inFromClient;
   	
	private static int serverPort; 
	private static int blockDuration; 
	private static int timeout; 
	private static ReentrantLock synLock = new ReentrantLock(); 

	//Usernames and passwords from Credentials.txt
	private static HashMap<String,String> users; 
	//Usernames and their respective sockets 
	private static HashMap<String, Socket> loggedUsers = new HashMap<>();
	// Offline username, User that send the message, message 
	private static HashMap<String, OfflineMessage> offlineMsgs = new HashMap<>(); 
	// Username and number of login attempts
	private static HashMap<String, Integer> loginAttempts = new HashMap<>();
	

	public Server(Socket connectionSocket) {
		socket = connectionSocket; 
	}
	//Loads "Credentials.txt" file in a Hash stored by server  
	private static void loadUsers() {
		users = new HashMap<>();
		String currDir = System.getProperty("user.dir") + "/"; 
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
	
	//Checks package contents to confirm whether user/pass is correct 
	private void userAuthenticate(TCPackage data) {
		String username = data.getUser();
		String password = data.getContent(); 
		System.out.println(loggedUsers);
		TCPackage packet; 
		if(users.containsKey(username) && users.get(username).equals(password)) {
			loggedUsers.put(username,socket); 
			System.out.println("This user " + username + "is using this port " + socket.getPort());
			packet = new TCPackage("login/pass");
		}else {
			int count = loginAttempts.containsKey(username) ? loginAttempts.get(username) : 0;
			if(count == 3) {
				packet = new TCPackage("login/fail/blocked"); 
			}else {
				loginAttempts.put(username, count+1);
				packet = new TCPackage("login/fail/retry"); 
			}	
		}
		
		try {
//			ObjectOutputStream outOfServer = new ObjectOutputStream(socket.getOutputStream());
//			outOfServer.writeObject(packet);
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private void receiveMsg(TCPackage packet) {
//		//User is offline
//		if(!loggedUsers.containsKey(packet.getReceiver())) {
//			OfflineMessage message = new OfflineMessage(packet.getUser(),packet.getContent()); 
//			offlineMsgs.put(packet.getReceiver(), message); 
//		}else {
//			TCPackage payload = new TCPackage("server/message", );
//		}
//	}
	
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

		    // accept connection from connection queue and starts a new thread for each new client
		    Socket connectionSocket = welcomeSocket.accept();
            new Server(connectionSocket).start(); 
		}
	} 
	
	public void run() {
		try {
			outToClient = new ObjectOutputStream(socket.getOutputStream());
		   	inFromClient = new ObjectInputStream(socket.getInputStream());
		   	
		   	
		    for(TCPackage data = (TCPackage) inFromClient.readObject(); data != null; data = (TCPackage) inFromClient.readObject()) {
		    	synLock.lock();
		    	String header = data.getHeader(); 
		    	System.out.println(header);
		    	switch(header){
		    	case "user/authenticate":
		    		userAuthenticate(data); 
	    			break; 
//		    	case "user/message": 
//		    		receiveMsg(data); 
		    	default:
		    		System.out.println("invalid header"); 
		    		socket.close();  
		    	}
		    	synLock.unlock();
		    }  
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}

} 