import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;

public class TCPackage implements Serializable {

	private String header; 
	private String content; 
	private String user; 
	
	public TCPackage() {}
	
	public TCPackage(String header) {
		this.header = header;
		this.content = ""; 
	}
	
	public TCPackage(String header, String content) {
		this.header = header; 
		this.content = content; 
	}
	
	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String receiver) {
		this.user = receiver;
	}

//	public Socket getSocket() {
//		return socket;
//	}
//
//	public void setSocket(Socket socket) {
//		this.socket = socket;
//	}
	
}
