/*
 *
 *  TCPClient from Kurose and Ross
 *  * Compile: java TCPClient.java
 *  * Run: java TCPClient
 */
import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class Client {
	
	private static boolean logStatus = false; 
	
	//Prompt user to enter username and password 
	private static boolean checkLogin(ObjectOutputStream postObject, ObjectInputStream inObject)  {
		try {
			BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please enter username: ");
			String username = userInput.readLine(); 

			System.out.println("Please enter password: "); 
			String password = userInput.readLine();
			
//			DataOutputStream postServer = new DataOutputStream(connectionSocket.getOutputStream());
//			postServer.writeBytes("Header: user/authenticate" + "\n"); 
//			postServer.writeBytes(username + "\n"); 
//			postServer.writeBytes(password + "\n");	
//			postServer.writeBytes("Connection: close\n"); 
			
			ArrayList<String> tmp = new ArrayList<>(); 
			tmp.add(username); 
			tmp.add(password); 
			TCPackage content = new TCPackage("user/authenticate", tmp); 
			
			postObject.writeObject(content);
					
			TCPackage confirm = (TCPackage) inObject.readObject(); 
			if(confirm.getHeader().equals("login/pass")) return true; 
			return false; 
			
//			BufferedReader getServer = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream())); 
			//return the boolean value returned by the server 
//			return Boolean.parseBoolean(getServer.readLine());
			
		} catch (IOException e) {
			e.printStackTrace();
			return false; 
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false; 
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
		Socket serverConnect = new Socket(serverIP, serverPort); 
		ObjectOutputStream postObject = new ObjectOutputStream(serverConnect.getOutputStream()); 
		ObjectInputStream inObject = new ObjectInputStream(serverConnect.getInputStream()); 
		
		while(!logStatus) {
			if(checkLogin(postObject, inObject)) {
				logStatus = true;
				System.out.println("logged in"); 
				break; 
			}
			System.out.println("try again"); 
		}	
		
		
		// close client socket
		serverConnect.close();
	} // end of main

} // end of class TCPClient