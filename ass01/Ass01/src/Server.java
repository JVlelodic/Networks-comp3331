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
	private static HashMap<String, ArrayList<OfflineMessage>> offlineMsgs = new HashMap<>(); 
	// Username and number of login attempts
	private static HashMap<String, Integer> loginAttempts = new HashMap<>();
	//Username and time logged in
	private static HashMap<String, LocalDateTime> logInTime = new HashMap<>(); 
	//Username and time blocked
	private static HashMap<String, LocalDateTime> logInBlocked = new HashMap<>(); 
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
		TCPackage broadcast = new TCPackage("msg/broadcast"); 
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
		
			//Account exists 
			if(users.containsKey(username)) {
				
				// Clear lock out
				if(logInBlocked.containsKey(username) 
					&& logInBlocked.get(username).isBefore(LocalDateTime.now())) {
					logInBlocked.remove(username);	
				}
				
				//Account is still locked out
				if(logInBlocked.containsKey(username)) {
					packet = new TCPackage("login/fail/lockout"); 
					
				//Account username and password is correct 
				}else if(users.get(username).equals(password)) {
					broadcast(username, " logged in"); 
					loggedUsers.put(username, outToClient);
					logInTime.put(username, LocalDateTime.now());
					packet = new TCPackage("login/pass"); 
					
				//Account password is wrong 
				}else{
					int count = loginAttempts.containsKey(username)? loginAttempts.get(username) : 0; 
					count++; 
					
					// 3 login attempts were made 
					if(count == 3) {
						logInBlocked.put(username, LocalDateTime.now().plusSeconds(blockDuration));
						count = 0; 
						packet = new TCPackage("login/fail/block");
					}else {
						loginAttempts.put(username, count);
						packet = new TCPackage("login/fail/retry"); 
					}
					loginAttempts.put(username, count); 
				}
			//Account does not exist
			}else {
				packet = new TCPackage("login/fail/user");				
			}
			
			System.out.println(loggedUsers);
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Transferring messages between users 
	private void sendMsg(TCPackage packet) {
		try {
			String fromUser = packet.getUser(); 
			String sendTo = packet.getReceiver();
			TCPackage msg; 
			//If user exists 
			if(users.containsKey(sendTo)) {
				// user is logged in 
				if(loggedUsers.containsKey(sendTo)) {
					msg = new TCPackage("msg/user"); 
					msg.setContent(packet.getUser() + ": " + packet.getContent());
					loggedUsers.get(sendTo).writeObject(msg); 
				// user is offline 
				}else {
					OfflineMessage om = new OfflineMessage(fromUser,packet.getContent());
					if(!offlineMsgs.containsKey(sendTo)) offlineMsgs.put(sendTo, new ArrayList<OfflineMessage>()); 
					offlineMsgs.get(sendTo).add(om); 				}
			}else {	
				msg = new TCPackage("msg/user/invalid");
				loggedUsers.get(fromUser).writeObject(msg); 
			}
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
		    	case "user/msg": 
		    		sendMsg(data); 
		    		break; 
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