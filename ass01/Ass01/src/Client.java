/*
 *
 *  TCPClient from Kurose and Ross
 *  * Compile: java TCPClient.java
 *  * Run: java TCPClient
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;


public class Client extends Thread{
	
	private static String user; 
	private static Socket serverConnect; 
	private static ReentrantLock syncLock = new ReentrantLock(); 
	private static ObjectOutputStream toServer; 
	private static ObjectInputStream fromServer; 
	private static BufferedReader userInput; 
	private static Thread readCommand = null; 
	
//	private static HashMap
	
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
			toServer.writeObject(content);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
		
	//Filter message from terminal and then send to server
	private static TCPackage sendMessage(String[] message, int index) {

		TCPackage msg = new TCPackage(); 

		String body = ""; 
		for(int i = index; i < message.length; i++) {
			body += (message[i] + " "); 
		}
		msg.setContent(body);
		return msg; 
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
		
		//Ask user to enter username and password 
		checkUsername(); 
		checkPassword(); 
		
		ServerSocket welcomeSocket = new ServerSocket(serverConnect.getLocalPort()); 
//		while(true) {
//			
//		}
		
		for(TCPackage data = (TCPackage) fromServer.readObject(); data != null; data = (TCPackage) fromServer.readObject()) {
//			TCPackage data = (TCPackage) fromServer.readObject(); 
			syncLock.lock();
			System.out.println(">> " + data.getContent()); 
			String header = data.getHeader(); 
			switch(header) {
			//Login accepted and opens new thread to accept userInput
			case "login/pass":
				readCommand = new Client();
				readCommand.start();
				break; 
			case "login/fail/retry":
				checkPassword(); 
				break;
			case "login/fail/user":
				checkUsername(); 
				checkPassword(); 
				break; 
			case "logout/user":
				if(readCommand != null) readCommand.interrupt();
				serverConnect.close();
				return; 
			case "msg/user":
				break; 
			case "startprivate/user":
				System.out.println(data.getPort());
				System.out.println(data.getIpAddress().toString());
				break; 
			default:
				System.out.println("Invalid header"); 
				break; 
			}	
			syncLock.unlock();
		}	
	} 
	
	public void run() { 
//		//read data do something

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
					if(message.length != 2) {
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
					packet = new TCPackage("user/startprivate"); 
					break; 
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