import java.io.Serializable;
import java.util.ArrayList;

public class TCPackage implements Serializable {

	private String header; 
	private ArrayList<String> content; 
	
	public TCPackage(String header) {
		this.header = header;
		this.content = null; 
	}
	
	public TCPackage(String header, ArrayList<String> content){
		this.header = header; 
		this.content = content; 
		this.content.add("Content: end\n"); 
	}

	public String getHeader() {
		return header;
	}

	public ArrayList<String> getContent() {
		return content;
	}
}
