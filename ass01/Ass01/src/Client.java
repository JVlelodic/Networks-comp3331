/*
 *
 *  TCPClient from Kurose and Ross
 *  * Compile: java TCPClient.java
 *  * Run: java TCPClient
 */
import java.io.*;
import java.net.*;

public class Client {

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
				


		// create read stream and receive from server
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(serverConnect.getInputStream()));
		String sentenceFromServer;
		sentenceFromServer = inFromServer.readLine();	

		// print output
		System.out.println(sentenceFromServer);
		
		// get input from keyboard
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		String sentence = inFromUser.readLine();
		
//		 write to server
		DataOutputStream outToServer = new DataOutputStream(serverConnect.getOutputStream());
		outToServer.writeBytes(sentence);
		
		sentenceFromServer = inFromServer.readLine(); 
		System.out.println(sentenceFromServer); 
		
		sentence = inFromUser.readLine();
		outToServer.writeBytes(sentence);

		// close client socket
		serverConnect.close();

	} // end of main

} // end of class TCPClient