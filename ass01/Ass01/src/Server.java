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
	
	//Usernames and their respective output sockets 
	private static HashMap<String, ObjectOutputStream> loggedUsers = new HashMap<>();
	
	// Offline username, User that sent the message, message 
	private static HashMap<String, ArrayList<TCPackage>> offlineMsgs = new HashMap<>(); 
	
	// Username and number of login attempts
	private static HashMap<String, Integer> loginAttempts = new HashMap<>();
	
	//Username and time logged in
	private static HashMap<String, LocalDateTime> logInTime = new HashMap<>(); 
	
	//Username and time logged out
	private static HashMap<String, LocalDateTime> logOutTime = new HashMap<>(); 
	
	//Username and time blocked
	private static HashMap<String, LocalDateTime> logInBlocked = new HashMap<>(); 
	
	//Username and list of users who have blocked them 
	private static HashMap<String, ArrayList<String>> blockedUsers = new HashMap<>();  
	
	//Username and respective serversocket ip address and port num in string format -> "<ipAddress>:<port>"
	private static HashMap<String, String> listenSockets = new HashMap<>();
	
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
	
	private void startPrivate(TCPackage data) {
		try {
			String user = data.getUser(); 
			TCPackage packet = new TCPackage(); 
			if(!users.containsKey(user)) {
				packet.setContent("Error. " + user + " does not exist");
			}else if(user.equals(clientUsername)) {
				packet.setContent("Error. Cannot start private messaging with self");
			}else if(blockedUsers.containsKey(clientUsername) && blockedUsers.get(clientUsername).contains(user)) {
				packet.setContent("Error. " + user + " has blocked you");
			}else if(blockedUsers.containsKey(user) && blockedUsers.get(user).contains(clientUsername)) {
				packet.setContent("Error. You cannot start private messaging as you have blocked this user");
			}else if(!listenSockets.containsKey(user)) {
				packet.setContent("Error. " + user + " is not online");
			}else {
				
					TCPackage inform = new TCPackage("private/connect");
					inform.setUser(clientUsername);
					inform.setContent(clientUsername + " started a private connection with you");
					loggedUsers.get(user).writeObject(inform);
					
					packet.setHeader("private/start");
					packet.setUser(user);
					String ipPort = listenSockets.get(user);
					String[] values = ipPort.split(":");
					packet.setIpAddress(values[0]);
					packet.setPort(Integer.parseInt(values[1]));
	
					packet.setContent("Starting private connection with " + user);
			}
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
			//To check if user has already been blocked by current user 
			if(!blockedUsers.get(blocked).contains(clientUsername)) {
				blockedUsers.get(blocked).add(clientUsername); 
			}
			packet.setContent(blocked + " is blocked");
		}
		try {
			outToClient.writeObject(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void whoelse() {
		TCPackage packet = new TCPackage("msg/user"); 
		//No other users online
		if(loggedUsers.containsKey(clientUsername) && loggedUsers.size() == 1) {
			packet.setContent("No other users are online");
		}else {
			String allUsers = ""; 
			for(String username : loggedUsers.keySet()) {	
				if(!username.equals(clientUsername)) {
					//Formatting string 
					if(allUsers.equals("")) {
						allUsers += username; 
					}else {
						allUsers += ("\n" + username);
					}
				}
			}
			packet.setContent(allUsers); 
		}
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
			boolean loggedOut = logOutTime.containsKey(username) && logOutTime.get(username).isAfter(withinTime);
			if(!username.equals(clientUsername) && (loggedOut|| loggedUsers.containsKey(username))) {
				//Formatting string
				if(allUsers.equals("")) {
					allUsers += username;
				}else {
					allUsers += ("\n" + username); 
				}
			}
		}
		TCPackage packet = new TCPackage("msg/user"); 
		if(allUsers.equals("")){
			packet.setContent("No other users were online");
		}else {
			packet.setContent(allUsers);
		}
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
						if(blockedUsers.containsKey(clientUsername) && blockedUsers.get(clientUsername).contains(currUser)) {
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
	
	private void logout(String message) {
		logOutTime.put(clientUsername, LocalDateTime.now()); 
		loggedUsers.remove(clientUsername); 
		listenSockets.remove(clientUsername);
		broadcast(clientUsername + " logged out", true); 
		TCPackage logout = new TCPackage("logout/user"); 
		logout.setContent(message);
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
				try {
					outToClient.writeObject(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			offlineMsgs.get(clientUsername).clear();
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
					//Achieved proper login 
					}else {
						loggedUsers.put(clientUsername, outToClient);
						logInTime.put(clientUsername, LocalDateTime.now());
						//Add client serversockets' ip address and port number 
						String ipPort = data.getIpAddress() + ":" + Integer.toString(data.getPort()); 
						listenSockets.put(clientUsername, ipPort);
						broadcast(clientUsername + " logged in", true); 
						packet = new TCPackage("login/pass"); 
						packet.setContent("You are logged in, you can now enter messages:"); 
						sendOfflineMessages();
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
			//If sending to yourself
			if(clientUsername.equals(sendTo)) {
				msg.setContent("Cannot send message to yourself");
				outToClient.writeObject(msg);
			//If user exists 
			}else if(users.containsKey(sendTo)) {
				//WHAT IF THEY ARE BLOCKED AND LOGGED OFF
				if(blockedUsers.containsKey(clientUsername) && blockedUsers.get(clientUsername).contains(sendTo)) {
					msg.setContent("Your message could not be delivered as the recipient has blocked you");
					//send back to client
					outToClient.writeObject(msg);
				}else {
					msg.setContent(packet.getContent());
					//user is logged in 
					if(loggedUsers.containsKey(sendTo)) {
						loggedUsers.get(sendTo).writeObject(msg);
					//user is offline
					}else {
						if(!offlineMsgs.containsKey(sendTo)) offlineMsgs.put(sendTo, new ArrayList<TCPackage>()); 
						offlineMsgs.get(sendTo).add(msg); 		
					}			
				}
			//user doesn't exist
			}else {
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
		    	switch(header){
		    	case "user/authenticate":
		    		clientUsername = data.getUser(); 
		    		userAuthenticate(data); 
	    			break; 
		    	case "user/broadcast":
		    		broadcast("(all) " + data.getContent(),false); 
		    		break; 
		    	case "user/msg": 
		    		sendMsg(data); 
		    		break; 
		    	case "user/whoelse":
		    		whoelse(); 
		    		break; 
		    	case "user/whoelse/since": 
		    		whoelseSince(data.getContent()); 
		    		break; 
		    	case "user/block":
		    		blockUser(data); 
		    		break; 
		    	case "user/unblock":
		    		unblockUser(data); 
		    		break; 
		    	case "user/logout":
		    		logout("You have logged out");
		    		break; 
		    	case "user/startprivate":
		    		startPrivate(data);
		    		break; 
		    	default:
		    		System.out.println("invalid header"); 
		    		socket.close();  
		    	}
		    	synLock.unlock();
		    }  
		//Timeout occurred, logout client  
		} catch (SocketTimeoutException e) {
			logout("You have timed out");
		} catch (SocketException | EOFException  e) {
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return; 
	}

} 