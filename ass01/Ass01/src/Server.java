import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;
import java.time.*;

public class Server extends Thread {
	
	private Socket socket; 
	private ObjectOutputStream outToClient;
   	private ObjectInputStream inFromClient;
   	
	private static int serverPort; 
	private static long blockDuration; 
	private static long timeout; 
	private static ReentrantLock synLock = new ReentrantLock(); 

	//Usernames and passwords from Credentials.txt
	private static HashMap<String,String> users; 
	//Usernames and their respective sockets 
	private static HashMap<String, ObjectOutputStream> loggedUsers = new HashMap<>();
	// Offline username, User that send the message, message 
	private static HashMap<String, OfflineMessage> offlineMsgs = new HashMap<>(); 
	// Username and number of login attempts
	private static HashMap<String, Integer> loginAttempts = new HashMap<>();
	//Username and time logged in
	private static HashMap<String, LocalDateTime> logInTime = new HashMap<>(); 
	//Username and time of last action
	private static HashMap<String, Integer> lastAction = new HashMap<>(); 
	
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
	
	//Broadcast to all logged in users of log in/out updates
	private void broadcast(String username, String content) {
		TCPackage broadcast = new TCPackage("broadcast/msg"); 
		broadcast.setContent(content);
		broadcast.setUser(username);
		
		for(String currUser : loggedUsers.keySet()) {
			ObjectOutputStream send = loggedUsers.get(currUser);
			try {
				send.writeObject(broadcast);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//Checks package contents to confirm whether user/pass is correct 
	private void userAuthenticate(TCPackage data) {
		try {
			String username = data.getUser();
			String password = data.getContent(); 
			TCPackage packet; 
			
			//If username does not exist
			
			// Correct username and password was entered
			if(users.containsKey(username) && users.get(username).equals(password)) {
				
				broadcast(username, " logged in"); 
				
				//Add user to the hash maps 
				loggedUsers.put(username,outToClient);
				logInTime.put(username, LocalDateTime.now());
				System.out.println(loggedUsers);
				
				packet = new TCPackage("login/pass");
			}else if()))
//			}else if(){			
//				int count = loginAttempts.containsKey(username) ? loginAttempts.get(username) : 0;
//				if(count == 3) {
//					packet = new TCPackage("login/fail/blocked"); 
//				}else {
//					count++; 
//					if(count == 3) {
//					
//					}
//					loginAttempts.put(username, count);
//					packet = new TCPackage("login/fail/retry"); 
//				}	
//			}
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
		blockDuration = Long.parseLong(args[1]); 
		timeout = Long.parseLong(args[2]); 
		
		loadUsers();
		
		/*create server socket that is assigned the serverPort (6789)
        We will listen on this port for connection request from clients */
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

		while (true){
		    // accept connection from connection queue and starts a new thread for each new client
		    Socket connectionSocket = welcomeSocket.accept();
//		    connectionSocket.setSoTimeout();
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