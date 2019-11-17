import java.io.Serializable;
import java.net.InetAddress;

public class TCPackage implements Serializable {

	private String header; 
	private String content; 
	private String user; 
	private InetAddress ipAddress; 
	private int port; 
	
	public TCPackage() {
		this.header = "msg/user";
		this.content = ""; 
	}
	
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

	public InetAddress getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
}
