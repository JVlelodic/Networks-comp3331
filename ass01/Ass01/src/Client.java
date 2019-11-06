/*
 *
 *  TCPClient from Kurose and Ross
 *  * Compile: java TCPClient.java
 *  * Run: java TCPClient
 */
import java.io.*;
import java.net.*;

public class Client {
	
	private static boolean logStatus = false; 
	
	//Prompt user to enter username and password 
	private static boolean checkLogin(Socket connectionSocket) {
		try {
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please enter username: ");
			String username = userInput.readLine(); 

			System.out.println("Please enter password: "); 
			String password = userInput.readLine();
			
			DataOutputStream postServer = new DataOutputStream(connectionSocket.getOutputStream());
			postServer.writeBytes(username); 
			postServer.writeBytes(password);
			
			System.out.println("this has been done"); 
			
			BufferedReader getServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
			//return the boolean value returned by the server 
			return Boolean.parseBoolean(getServer.readLine());
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return false; 
	}

	public static void main(String[] args) throws Exception {
		
		// Determine correct number of args
		if(args.length != 2) {
			System.out.println("Missing arguments: server_IP server_port"); 
			return; 
		}
		
		// Get socket parameters, address and Port No
		InetAddress serverIP = InetAddress.getByName(args[0]);
		System.out.println(args[0]); 	
		int serverPort = Integer.parseInt(args[1]); 
		System.out.println(args[1]); 
		
		//create socket which connects to server
		Socket serverConnect = new Socket(serverIP, serverPort); 
			
		while(!logStatus) {
			if(checkLogin(serverConnect)) {
				logStatus = true;
				System.out.println("logged in"); 
				break; 
			}
		}	
		
		
		// close client socket
//		serverConnect.close();

	} // end of main

} // end of class TCPClient