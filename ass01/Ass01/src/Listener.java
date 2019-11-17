import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

public class Listener extends Thread {
	
	private static ReentrantLock syncLock; 
	private static Socket socket;
	
	public void run() { 
		ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream()); 
		ObjectInputStream input = new ObjectInputStream(socket.getInputStream()); 
	
		for(TCPackage data = (TCPackage) input.readObject(); data != null; data = (TCPackage) input.readObject()) {
			syncLock.lock();
			System.out.println(">> " + data.getContent()); 
			String header = data.getHeader(); 
			switch(header) {
			//Login accepted and opens new thread to accept userInput
//			case "login/pass":
//				new Client().start();
//				break; 
//			case "login/fail/retry":
//				checkPassword(); 
//				break;
//			case "login/fail/user":
//				checkUsername(); 
//				checkPassword(); 
//				break; 
//			case "logout/user":
//				if(readCommand != null) readCommand.interrupt();
//				serverConnect.close();
//				return; 
			case "msg/user":
				break; 
//			case "startprivate/user":
//				System.out.println(data.getPort());
//				System.out.println(data.getIpAddress().toString());
//				break; 
			default:
				System.out.println("Invalid header"); 
				break; 
			}	
			syncLock.unlock();
	}
}
