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
	private static void sendMessage(String[] message) {
		
		if(message.length >= 3) {
			System.out.println("Missing arguments for type \"message\": message <user> <message>");
			return; 
		}
		
		TCPackage msg = new TCPackage("msg/user");
		msg.setUser(user);
		msg.setReceiver(message[1]);
		
		String body = ""; 
		for(int i = 2; i < message.length; i++) {
			body.concat(message[i] + " "); 
		}
		msg.setContent(body);
		try {
			toServer.writeObject(msg);
		} catch (IOException e) {
			e.printStackTrace();
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
		
		//Ask user to enter username and password 
		checkUsername(); 
		checkPassword(); 
		
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
				checkPassword(); 
				break;
			case "login/fail/block":
				System.out.println("Invalid Password. Your account has been blocked. Please try again later"); 
				toServer.close();
				fromServer.close();
				serverConnect.close();
				return; 
			case "login/fail/user":
				System.out.println("Username does not exist"); 
				checkUsername(); 
				checkPassword(); 
			case "login/fail/lockout":
				System.out.println("Your account is blocked due to multiple login failures");
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
		}		
	} 
	
	public void run() {
		//read data do something
		try {
			for(String line = userInput.readLine(); line != null; line = userInput.readLine()) {
				syncLock.lock();	
				String[] message = line.trim().split(" ");
				System.out.println(message); 
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