import java.io.Serializable;
import java.util.ArrayList;

public class TCPackage implements Serializable {

	private String header; 
	private String content; 
	private String user; 
	private String receiver; 
	
	public TCPackage(String header) {
		this.header = header;
		this.content = null; 
	}
	
	public TCPackage(String header, String content){
		this.header = header; 
		this.content = content; 
//		this.content.add("Content: end\n"); 
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

	public void setUser(String user) {
		this.user = user;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}
	
}
