import java.io.IOException;
import java.net.ServerSocket;

public class ConnectionListener extends Thread {
	private int port; 
	
	public ConnectionListener(int portNum) {
		this.port = portNum;
	}
	public void run() {
		try {
			ServerSocket welcomeSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
