import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

//Thread that listens to peer to peer connections
public class ClientListener extends Thread {
	
	private HashMap<String, ObjectOutputStream> privateUsers;
	private HashMap<String, Thread> openThreads; 
	private ObjectInputStream peerIn; 
	private ReentrantLock syncLock; 
	
	public ClientListener(HashMap<String, ObjectOutputStream> privateUsers, HashMap<String,Thread> openThreads , ObjectInputStream peerIn, ReentrantLock lock) {
		this.privateUsers = privateUsers;
		this.openThreads = openThreads;
		this.peerIn = peerIn; 
		this.syncLock = lock; 
	}
	
	//User has closed the connection so remove from HashMaps 
	public void removeThread(String user) {
		openThreads.remove(user);
		privateUsers.remove(user);
		return; 
	}
	
	public void run() {
		try {
			for(TCPackage data = (TCPackage) peerIn.readObject(); data != null; data = (TCPackage) peerIn.readObject()) {
				syncLock.lock();
				System.out.println(">> " + data.getContent()); 
				String header = data.getHeader(); 
				switch(header) {
				case "msg/user":
					break; 
				case "private/close":
					removeThread(data.getUser());
					break;
				default:
					System.out.println("Invalid header"); 
					break; 
				}	
				syncLock.unlock();
			}
		} catch (SocketException | EOFException e) {
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
		return; 
	}
}
