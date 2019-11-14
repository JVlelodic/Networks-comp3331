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
   	private String clientUsername; 
   	
	private static int serverPort; 
	private static long blockDuration; 
	private static int timeout; 
	private static ReentrantLock synLock = new ReentrantLock(); 

	//Usernames and passwords from Credentials.txt
	private static HashMap<String,String> users; 
	//Usernames and their respective sockets 
	private static HashMap<String, ObjectOutputStream> loggedUsers = new HashMap<>();
	// Offline username, User that sent the message, message 
	private static HashMap<String, ArrayList<TCPackage>> offlineMsgs = new HashMap<>(); 
	// Username and number of login attempts
	private static HashMap<String, Integer> loginAttempts = new HashMap<>();
	//Username and time logged in
	private static HashMap<String, LocalDateTime> logInTime = new HashMap<>(); 
	//Username and time blocked
	private static HashMap<String, LocalDateTime> logInBlocked = new HashMap<>(); 
	//Username and list of users who have blocked them 
	private static HashMap<String,ArrayList<String>> blockedUsers = new HashMap<>();  
	
	
	public Server(Socket connectionSocket) {
		socket = connectionSocket; 
	}
	
	private void unblockUser(TCPackage data) {
		String blocked = data.getUser(); 
		TCPackage packet = new TCPackage("msg/user"); 
		
		if(!users.containsKey(blocked)) {
			packet.setContent("Error. User does not exist");
		}else if(clientUsername.equals(blocked)) {
			packet.setContent("Error. Cannot unblock self");
		}else if(!blockedUsers.containsKey(blocked) || !blockedUsers.get(blocked).contains(clientUsername)) {
			packet.setContent("Error. " + blocked + " was not blocked");
		}else {
			blockedUsers.get(blocked).remove(clientUsername); 
			packet.setContent(blocked + " is unblocked");
		}
		
		try { 
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void blockUser(TCPackage data) {
		String blocked = data.getUser(); 
		
		TCPackage packet;
		packet = new TCPackage("msg/user"); 

		
		//No such user
		if(!users.containsKey(blocked)) {
			packet.setContent("Error. User does not exist");
		}else if(clientUsername.equals(blocked)) {
			packet.setContent("Error. Cannot block Self"); 
		}else {
			if(!blockedUsers.containsKey(blocked)) blockedUsers.put(blocked, new ArrayList<String>()); 
			blockedUsers.get(blocked).add(clientUsername); 
			packet.setContent(blocked + " is blocked");
		}
	}

	private void whoelse() {
		String allUsers = ""; 
		for(String username : loggedUsers.keySet()) {	
			if(!username.equals(clientUsername)) allUsers.concat(username + "\n"); 
		}
		TCPackage packet = new TCPackage("msg/user"); 
		packet.setContent(allUsers);
		try {
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void whoelseSince(String seconds) {
		Long time = Long.parseLong(seconds); 
		LocalDateTime withinTime = LocalDateTime.now().minusSeconds(time); 
		String allUsers = ""; 

		for(String username : logInTime.keySet()) {	
			if(!username.equals(clientUsername) && logInTime.get(username).isBefore(withinTime)) allUsers.concat(username + "\n"); 
		}
		TCPackage packet = new TCPackage("msg/user"); 
		packet.setContent(allUsers);
		try {
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	/**
	 * Broadcast a message to all users currently online
	 * @param message- message to be broadcasted
	 * @param user- user broadcasting the message 
	 * @param presence- if its a presence broadcast e.g. log in or log out 
	 */
	//If user A blocks user B, user B is not informed when A logs in or logs out
	//If user A blocks user B, when user B broadcasts a msg, user 
	private void broadcast(String message, boolean presence) {
		
		
		TCPackage broadcast = new TCPackage("msg/user"); 
		broadcast.setContent(message);
		boolean delivered = true; 
		
		try {
			for(String currUser : loggedUsers.keySet()) {
				ObjectOutputStream send = loggedUsers.get(currUser);
				if(!currUser.equals(clientUsername)) {
					//Presence broadcast && current user in the loop is not blocked by user logging on 
					if(presence){
						if(!blockedUsers.containsKey(currUser) || !blockedUsers.get(currUser).contains(clientUsername)) send.writeObject(broadcast); 
					}else {
						//user has been blocked by current user in loop and will be informed that message has not been delivered
						if(blockedUsers.get(clientUsername).contains(currUser)) {
							delivered = false; 
						}else {
							send.writeObject(broadcast);
						}
					}
				}
			}
			broadcast.setContent("Your message could not be delivered to some recipients");
			if(!delivered) outToClient.writeObject(broadcast); 
		} catch (IOException e) {
				e.printStackTrace();
		}
	}
	
	private void logout() {
		loggedUsers.remove(clientUsername); 
		broadcast(clientUsername + " logged out", true); 
		TCPackage logout = new TCPackage("logout/user"); 
		try {
			outToClient.writeObject(logout);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return; 
	}
	
	//Send all offline messages to user 
	private void sendOfflineMessages() {
		if(offlineMsgs.containsKey(clientUsername)) {
			for(TCPackage packet : offlineMsgs.get(clientUsername)) {
				String fromUser = om.getUser();
				TCPackage packet = new TCPackage() 
			}
		}
		return; 
	}
	
	
	//Checks package contents to confirm whether user/pass is correct 
	private void userAuthenticate(TCPackage data) {
		try {
			String password = data.getContent();
			TCPackage packet; 
		
			//Account exists 
			if(users.containsKey(clientUsername)) {
				
				// Clear existing lock outs
				if(logInBlocked.containsKey(clientUsername) 
					&& logInBlocked.get(clientUsername).isBefore(LocalDateTime.now())) {
					logInBlocked.remove(clientUsername);	
				}
				
				//Account is still locked out
				if(logInBlocked.containsKey(clientUsername)) {
					packet = new TCPackage("logout/user"); 
					packet.setContent("Your account is blocked due to multiple login failures. Please try again later");
					
				//Account clientUsername and password is correct 
				}else if(users.get(clientUsername).equals(password)) {
					//Someone's already logged in under this account 
					if(loggedUsers.containsKey(clientUsername)) {
						packet = new TCPackage("login/fail/user"); 
						packet.setContent("Account is already logged in. Please try another account");
					}else {
//						clientUsername = data.getUser(); 
						loggedUsers.put(clientUsername, outToClient);
						logInTime.put(clientUsername, LocalDateTime.now());
						broadcast(clientUsername + " logged in", true); 
						packet = new TCPackage("login/pass"); 
						packet.setContent("You are logged in, you can now enter messages:"); 
					}
				//Account password is wrong 
				}else{
					int count = loginAttempts.containsKey(clientUsername)? loginAttempts.get(clientUsername) : 0; 
					count++; 
					
					// 3 login attempts were made 
					if(count == 3) {
						logInBlocked.put(clientUsername, LocalDateTime.now().plusSeconds(blockDuration));
						packet = new TCPackage("logout/user");
						packet.setContent("Invalid Password. Your account has been blocked. Please try again later"); 
						//Reset counter
						count = 0; 
					// Login attempts before 3 
					}else {
						loginAttempts.put(clientUsername, count);
						packet = new TCPackage("login/fail/retry"); 
						packet.setContent("Invalid Password. Please try again");
					}
					loginAttempts.put(clientUsername, count); 
				}
			//Account does not exist
			}else {
				packet = new TCPackage("login/fail/user");
				packet.setContent("Username does not exist"); 
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
			String sendTo = packet.getUser();
			TCPackage msg = new TCPackage("msg/user");
			//If user exists 
			if(users.containsKey(sendTo)) {
				//WHAT IF THEY ARE BLOCKED AND LOGGED OFF
				if(blockedUsers.get(clientUsername).contains(sendTo)) {
					msg.setContent("Your message could not be delivered as the recipient has blocked you");
					//send back to client
					outToClient.writeObject(msg);
				// user is logged in 
				}else if(loggedUsers.containsKey(sendTo)) {
					msg.setContent(clientUsername + ": " + packet.getContent());
					loggedUsers.get(sendTo).writeObject(msg); 
				// user is offline 
				}else {
					packet.setUser(clientUsername);
					if(!offlineMsgs.containsKey(sendTo)) offlineMsgs.put(sendTo, new ArrayList<TCPackage>()); 
					offlineMsgs.get(sendTo).add(packet); 			
				}
			//user doesn't exist
			}else {	
				loggedUsers.get(clientUsername).writeObject(msg); 
				msg.setContent("Error. Invalid user"); 
				outToClient.writeObject(msg);  
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
		timeout = Integer.parseInt(args[2]); 
		
		loadUsers();
		
		/*create server socket that is assigned the serverPort (6789)
        We will listen on this port for connection request from clients */
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

		while (true){
		    // accept connection from connection queue and starts a new thread for each new client
		    Socket connectionSocket = welcomeSocket.accept();
//		    connectionSocket.setSoTimeout(timeout);
		    connectionSocket.setSoTimeout(timeout*1000);
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
		    		clientUsername = data.getUser(); 
		    		userAuthenticate(data); 
	    			break; 
		    	case "user/broadcast":
		    		broadcast(clientUsername + ": " + data.getContent(),false); 
		    		break; 
		    	case "user/msg": 
		    		sendMsg(data); 
		    		break; 
		    	case "user/whoelse":
		    		whoelse(); 
		    		break; 
		    	case "user/whoelse/since": 
		    		whoelseSince(data.getContent()); 
		    	case "user/block":
		    		blockUser(data); 
		    		break; 
		    	case "user/unblock":
		    		unblockUser(data); 
		    		break; 
		    	case "user/logout":
		    		logout();
		    		break; 
		    	default:
		    		System.out.println("invalid header"); 
		    		socket.close();  
		    	}
		    	synLock.unlock();
		    }  
		} catch (SocketTimeoutException e) {
			//Send message to client that timeout has occured 
			
			//Have to remove client from logout 
			logout(); 
			try {
				socket.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return; 
		} catch (SocketException | EOFException  e) {
			System.out.println("Client has closed the connection");
			return; 
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

} 