/*
 *
 *  TCPClient from Kurose and Ross
 *  * Compile: java TCPClient.java
 *  * Run: java TCPClient
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;


public class Client extends Thread{
	
	private static String user; 
	private static Socket serverConnect; 
	private static ReentrantLock syncLock = new ReentrantLock(); 
	private static ObjectOutputStream toServer; 
	private static ObjectInputStream fromServer; 
	private static BufferedReader userInput; 
	private static ServerSocket peerListener; 
	
	//Username and their respective output sockets
	private static HashMap<String, ObjectOutputStream> privateUsers = new HashMap<>();
	
	//Username and the opened listener thread  
	private static HashMap<String,Thread> openThreads = new HashMap<>(); 
	
	//Error checking to determine if String is an integer 
	public static boolean isInteger(String s) {
	    try { 
	        Integer.parseInt(s); 
	    } catch(Exception e) { 
	        return false; 
	    }
	    return true;
	}
	
	//Prompt user to enter username
	private static void checkUsername() {
		System.out.println("Please enter username: ");
		try {
			user = userInput.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		} 		
	}
	
	//Prompt user to enter password 
	private static void checkPassword()  {
		try {			

			System.out.println("Please enter password: "); 
			String password = userInput.readLine();
	 
			TCPackage content = new TCPackage("user/authenticate"); 
			content.setUser(user);
			content.setContent(password);
			content.setPort(peerListener.getLocalPort());
			//Returns address 127.0.0.1 
			content.setIpAddress(InetAddress.getLoopbackAddress().getHostAddress());
			toServer.writeObject(content);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	//Filter message from terminal and then send to server
	private static TCPackage sendMessage(String[] message, int index) {

		TCPackage msg = new TCPackage(); 

		String body = user + ": "; 
		for(int i = index; i < message.length; i++) {
			body += (message[i] + " "); 
		}
		msg.setContent(body);
		return msg; 
	}
	
	//Opens thread to listen to new peer connection
	private static void startPeerThread(TCPackage data, Socket client) {
		try {
			String peer = data.getUser();
			ObjectOutputStream peerOut = new ObjectOutputStream(client.getOutputStream()); 
			ObjectInputStream peerIn = new ObjectInputStream(client.getInputStream()); 
			
			privateUsers.put(peer, peerOut);
			
			Thread listen = new ClientListener(privateUsers, openThreads, peerIn, syncLock);
			listen.start();
			openThreads.put(peer, listen); 			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//User logouts 
	private static void logOut() {
		//Closes and messages all p2p sockets and threads
		try {
		
			TCPackage msg = new TCPackage("private/close");
			msg.setContent(user + " is logging out. Private connection has been closed");
			msg.setUser(user);
			for(String username : privateUsers.keySet()) {
				ObjectOutputStream output = privateUsers.get(username); 
				output.writeObject(msg);
				output.close();
			}

			//Closes STDIN thread
			if(openThreads.containsKey(user)) openThreads.get(user).interrupt();
			serverConnect.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return; 
	}
	
	//Private messaging in P2P
	private static void peerMessage(TCPackage packet, String peer) {
		if(privateUsers.containsKey(peer)) {
			ObjectOutputStream peerConnect = privateUsers.get(peer); 
			try {
				peerConnect.writeObject(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else if(peer.equals(user)) {
			syncLock.lock();
			System.out.println("You cannot private message yourself");
			syncLock.unlock();
		}else {
			syncLock.lock();
			System.out.println("You do not have a private connection with this user: " + peer); 
			syncLock.unlock();
		}
		return;
	}
	
	//Closing a private P2P Socket
	private static void closePeer(String peer) {
		ObjectOutputStream peerConnect = privateUsers.get(peer);
		if(privateUsers.containsKey(peer)) {
			TCPackage packet = new TCPackage("private/close");
			packet.setContent(user + " has closed the private connection with you");
			packet.setUser(user);
			try {
				peerConnect.writeObject(packet);
				peerConnect.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			privateUsers.remove(peer); 
			openThreads.get(peer).interrupt();
			openThreads.remove(peer);
		}else {
			syncLock.lock();
			System.out.println("You do not have a private connection with this user: " + peer);
			syncLock.unlock();
		}
	}
	
	public static void main(String[] args) throws Exception {
		
		// Determine correct number of args
		if(args.length != 2) {
			System.out.println("Missing arguments: server_IP server_port"); 
			return; 
		}
		
		// Get socket parameters, address and Port No
		InetAddress serverIP = InetAddress.getByName(args[0]);
		int serverPort = Integer.parseInt(args[1]); 
		
		//Create socket which connects to server
		serverConnect = new Socket(serverIP, serverPort); 
				
		toServer = new ObjectOutputStream(serverConnect.getOutputStream()); 
		fromServer = new ObjectInputStream(serverConnect.getInputStream()); 

		userInput = new BufferedReader(new InputStreamReader(System.in));
		
		//Create ServerSocket to listen to incoming connections
		peerListener = new ServerSocket(0);
		
		//Ask user to enter username and password 
		checkUsername(); 
		checkPassword(); 
		
		//Receives messages from server
		for(TCPackage data = (TCPackage) fromServer.readObject(); data != null; data = (TCPackage) fromServer.readObject()) {
			syncLock.lock();
			System.out.println(">> " + data.getContent()); 
			String header = data.getHeader(); 
			switch(header) {
			case "login/pass":
				Thread reader = new Client(); 
				reader.start(); 
				openThreads.put(user, reader);
				break; 
			case "login/fail/retry":
				checkPassword(); 
				break;
			case "login/fail/user":
				checkUsername(); 
				checkPassword(); 
				break; 
			case "logout/user":
				logOut(); 
				return; 
			case "private/start":
				int portNum = data.getPort();
				InetAddress ip = InetAddress.getByName(data.getIpAddress());
				Socket privateChat = new Socket(ip, portNum); 
				startPeerThread(data, privateChat); 
				break; 
			case "private/connect":
				Socket peer = peerListener.accept();
				startPeerThread(data, peer); 
				break;
			case "msg/user":
				break; 
			default:
				System.out.println("Invalid header"); 
				break; 
			}	
			syncLock.unlock();
		}	
	} 
	
	//Reads STDIN for commands
	public void run() { 
		try {
			for(String line = userInput.readLine(); line != null && !Thread.currentThread().isInterrupted(); line = userInput.readLine()) {
				String[] message = line.trim().split(" ");
			
				TCPackage packet = null; 
				
				switch (message[0]) {
				case "message":
					if(message.length < 3) {
						System.out.println("Missing arguments for type \"message\": message <user> <message>");
						continue; 
					}
					packet = sendMessage(message,2);
					packet.setUser(message[1]);
					packet.setHeader("user/msg"); 
					break;
				case "broadcast":
					if(message.length < 2) {
						System.out.println("Missing arguments for type \"broadcast\": broadcast <message>"); 
						continue; 
					}
					packet = sendMessage(message, 1);
					packet.setHeader("user/broadcast");
					break;
				case "whoelse":
					if(message.length !=  1) {
						System.out.println("No arguments"); 
						continue; 
					}
					packet = new TCPackage("user/whoelse"); 
					break; 
				case "whoelsesince":
					if(message.length != 2 && isInteger(message[1])) {
						System.out.println("Missing arguments for type \"whoelsesince\": whoelsesince <time>");
						continue; 
					}
					packet = new TCPackage("user/whoelse/since"); 
					packet.setContent(message[1]); 
					break; 
				case "block":
					if(message.length != 2) {
						System.out.println("Missing arguments for type\"block\": block <user>"); 
						continue; 
					}
					packet = new TCPackage("user/block");
					packet.setUser(message[1]);
					break; 
				case "unblock":
					if(message.length != 2) {
						System.out.println("Missing arguments for type \"unblock\": unblock <user>"); 
						continue; 
					}
					packet = new TCPackage("user/unblock"); 
					packet.setUser(message[1]);
					break; 
				case "logout":
					if(message.length != 1) {
						System.out.println("No arguments needed for type \"logout\": logout"); 
						continue; 
					}
					packet = new TCPackage("user/logout"); 
					break; 
				case "startprivate": 
					if(message.length != 2) {
						System.out.println("Missing arguments for type \"startprivate\": startprivate <user>"); 
						continue; 
					}
					if(privateUsers.containsKey(message[1])) {
						System.out.println("Error. Already have a private connection with " + message[1]);
						continue; 
					}
					packet = new TCPackage("user/startprivate");
					packet.setUser(message[1]);
					break; 
				case "private": 
					if(message.length < 3) {
						System.out.println("Missing arguments for type \"private\": private <user> <message>"); 
						continue; 
					}
					packet = sendMessage(message,2);
					packet.setContent("(private) " + packet.getContent());
					String client = message[1]; 
					peerMessage(packet, client); 
					continue; 
				case "stopprivate":
					if(message.length != 2) {
						System.out.println("Missing arguments for type \"stopprivate\": stopprivate <user>"); 
						continue; 
					}
					closePeer(message[1]);
					continue; 
				default:
					System.out.println("Error. Invalid command");
					continue; 
				}
				toServer.writeObject(packet);
			}			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return; 
	}
}

 // end of class TCPClient