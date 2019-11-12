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
//	private static boolean logStatus = false;
	private static Socket serverConnect; 
	private static ReentrantLock syncLock = new ReentrantLock(); 
	private static ObjectOutputStream toServer; 
	private static ObjectInputStream fromServer; 
	private static BufferedReader userInput; 
	
	//Prompt user to enter username and password 
	private static void checkLogin()  {
		try {			

			System.out.println("Please enter password: "); 
			String password = userInput.readLine();
			
//			String login = username + " " + password; 
//			TCPackage content = new TCPackage("user/authenticate", login); 
			TCPackage content = new TCPackage("user/authenticate"); 
			content.setUser(user);
			content.setContent(password);
//			content.setSocket(serverConnect);
			
			toServer.writeObject(content);
					
//			TCPackage confirm = (TCPackage) input.readObject(); 
//			if(confirm.getHeader().equals("login/pass")) {
//				user = username; 
//				logStatus = true; 
//				userInput.close();
//				return; 
//			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Create packet to send to user 
	private static void sendMessage(String[] message) {
		if(message.length >= 3) {
			System.out.println("Missing arguments for type \"message\": message <user> <message>");
			return; 
		}
		
		TCPackage forward = new TCPackage("user/message"); 
		forward.setReceiver(message[1]);
		forward.setUser(user);
		String body = ""; 
		for(int i = 2; i < message.length; i++) {
			body.concat(message[i] + " "); 
		}
		forward.setContent(body);
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
		
		System.out.println("Please enter username: ");
		userInput = new BufferedReader(new InputStreamReader(System.in));
		user = userInput.readLine(); 
		
//		Client client = new Client();
//		client.start();
		
//		while(!logStatus) {
////			syncLock.lock();
//			System.out.println(logStatus);
//			checkLogin(user, userInput); 
////			syncLock.unlock();
//		}
		
		//Checks password for first time 
		checkLogin(); 
		
		for(TCPackage data = (TCPackage) fromServer.readObject(); data != null; data = (TCPackage) fromServer.readObject()) {
			String header = data.getHeader(); 
			syncLock.lock();
			switch(header) {
			//Login accepted and opens new thread to accept userInput
			case "login/pass":
//				logStatus = true;
				System.out.println("You are logged in, you can now enter messages:"); 
				new Client().start();
				break; 
			case "login/fail/retry":
				System.out.println("Invalid Password. Please try again");
				checkLogin(); 
				break;
			case "login/fail/blocked":
				System.out.println("Your account is blocked due to multiple login failures. Please try again later"); 
				toServer.close();
				fromServer.close();
				serverConnect.close();
				return; 
			case "broadcast/msg":
				System.out.println(data.getUser() + data.getContent());
				break; 
			default:
				System.out.println("invalid header"); 
				break; 
			}	
//			System.out.println("The header is " + data.getHeader());
//			System.out.println("The content is " + data.getContent()); 
			syncLock.unlock();
		
		//Create buffer to read terminal input
//		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		
		//take input
		
		}
		
		// close client socket
		
	} // end of main
	
	public void run() {
		//read data do something
		try {
			for(String line = userInput.readLine(); line != null; line = userInput.readLine()) {
				syncLock.lock();	
				String[] message = line.split(" ");
				switch (message[0]) {
				case "message":
					sendMessage(message);
					break;
				case "broadcast":
					break;
				case "whoelse":
					break; 
				case "whoelsesince":
					break; 
				case "block":
					break; 
				case "unblock":
					break; 
				case "logout":
					break; 
				default:
					System.out.println("Error. Invalid command");
				}
				syncLock.unlock();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
} // end of class TCPClient