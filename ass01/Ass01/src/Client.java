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
	private static boolean logStatus = false;
	private static Socket serverConnect; 
	private static ReentrantLock syncLock = new ReentrantLock(); 
	private static ObjectOutputStream output; 
	private static ObjectInputStream input; 
	
	//Prompt user to enter username and password 
	private static void checkLogin()  {
		try {
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please enter username: ");
			String username = userInput.readLine(); 

			System.out.println("Please enter password: "); 
			String password = userInput.readLine();
			
			String login = username + " " + password; 
			TCPackage content = new TCPackage("user/authenticate", login); 
			
			output.writeObject(content);
					
			TCPackage confirm = (TCPackage) input.readObject(); 
			if(confirm.getHeader().equals("login/pass")) {
				user = username; 
				logStatus = true; 
				userInput.close();
				return; 
			}
			System.out.println("try again"); 

		} catch (IOException | ClassNotFoundException e) {
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
		output = new ObjectOutputStream(serverConnect.getOutputStream()); 
		input = new ObjectInputStream(serverConnect.getInputStream()); 
		
		while(!logStatus) {
			checkLogin(); 
		}
		
		System.out.println("You are logged in, you can now enter messages:"); 

		Client client = new Client();
		client.start();
		
		//Create buffer to read terminal input
		BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
		
		//take input
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
				System.out.println("invlaid message type, please try again");
			}
			syncLock.unlock();
		}
		
		// close client socket
		
	} // end of main
	
	public void run() {
		//read data do something
		try {
			for(TCPackage data = (TCPackage) input.readObject(); data != null; data = (TCPackage) input.readObject()) {
				String header = data.getHeader(); 
				syncLock.lock();

				switch(header) {
				default:
					System.out.println("invalid header"); 
					break; 
				}	
				System.out.println("The header is " + data.getHeader());
				System.out.println("The content is " + data.getContent()); 
				syncLock.unlock();
			}
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
} // end of class TCPClient