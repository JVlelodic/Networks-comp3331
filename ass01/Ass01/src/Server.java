import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Date;

public class Server extends Thread {
	
	private Socket socket; 
	
	private static int serverPort; 
	private static int blockDuration; 
	private static int timeout; 
	private static HashMap<String,String> users; 
	private static ArrayList<String> loggedUsers = new ArrayList<>(); 
	private static ReentrantLock synLock = new ReentrantLock(); 
	private static HashMap<String,ArrayList<String>> offlineMsgs = new HashMap<>(); 
	
	public Server(Socket connectionSocket) {
		socket = connectionSocket; 
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
	
	//Checks package contents to confirm whether user/pass is correct 
	private static TCPackage userAuthenticate(String data) {
		String[] user = data.split(" "); 
		String username = user[0]; 
		String password = user[1]; 
		System.out.println(loggedUsers);
		if(users.containsKey(username) && users.get(username).equals(password)) {
			loggedUsers.add(username); 
			return new TCPackage("login/pass"); 
		}else {
			return new TCPackage("login/fail"); 
		}
		
	}
	
	private static void receiveMsg() {
		
	}
	
	public static void main(String[] args)throws Exception {
		
		if(args.length != 3) {
			System.out.println("Missing arguments: server_port block_duration timeout"); 
			return; 
		}
		
		serverPort = Integer.parseInt(args[0]); 
		blockDuration = Integer.parseInt(args[1]); 
		timeout = Integer.parseInt(args[2]); 
		
		loadUsers();
		
		/*create server socket that is assigned the serverPort (6789)
        We will listen on this port for connection request from clients */
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
        System.out.println("Server is ready :");

		while (true){

		    // accept connection from connection queue and starts a new thread for each new client
		    Socket connectionSocket = welcomeSocket.accept();
            new Thread(new Server(connectionSocket)).start(); 
		} 
	} 
	
	public void run() {
		try {
			ObjectOutputStream outToClient = new ObjectOutputStream(socket.getOutputStream());
		   	ObjectInputStream inFromClient = new ObjectInputStream(socket.getInputStream());
		   	
		   	
		    for(TCPackage data = (TCPackage) inFromClient.readObject(); data != null; data = (TCPackage) inFromClient.readObject()) {
		    	synLock.lock();
		    	String header = data.getHeader(); 
		    	System.out.println(header);
		    	switch(header){
		    	case "user/authenticate":
		    		TCPackage confirm = userAuthenticate(data.getContent()); 
	    			outToClient.writeObject(confirm);
	    			break; 
		    	case "user/message": 
		    		receivemessage
		    	default:
		    		System.out.println("invalid header"); 
		    		socket.close();  
		    	}
		    	synLock.unlock();
		    }  
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

} 