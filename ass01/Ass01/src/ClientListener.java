import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.locks.ReentrantLock;

public class ClientListener extends Thread {
	
//	private Socket peerSocket; 
//	private ObjectOutputStream peerOut; 
	private ObjectInputStream peerIn; 
	private ReentrantLock syncLock; 
	
	public ClientListener(ObjectInputStream peerIn, ReentrantLock lock) {
//		this.peerSocket = socket; 
		this.peerIn = peerIn; 
		this.syncLock = lock; 
	}
	
	public void run() {
		try {
			for(TCPackage data = (TCPackage) peerIn.readObject(); data != null && !Thread.currentThread().isInterrupted(); data = (TCPackage) peerIn.readObject()) {
				syncLock.lock();
				System.out.println(">> " + data.getContent()); 
				String header = data.getHeader(); 
				switch(header) {
				case "msg/user":
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
