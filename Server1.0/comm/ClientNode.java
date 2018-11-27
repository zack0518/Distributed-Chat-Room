package comm;

import java.util.Date;

public class ClientNode {
	String address = "";
	int port = 0;
	String status = "logout";
	String username = "anonymous";
	private Date TimeStamp = null;
	
	public ClientNode(String address, int port) {
		this.address = address;
		this.port = port;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	public boolean equals(String address, int port){
		if(address.equals(this.address) && port == this.port){
			return true;
		}
		return false;
	}
	
	public String getAddress() {
		return address;
	}
	
	public int getPort() {
		return port;
	}
	
	public void login(String username) {
		this.username = username;
		this.status = "login";
	}
	
	public void setTimeStamp(Date TimeStamp) {
		this.TimeStamp = TimeStamp;
	}
	
	public Date getTimeStamp() {
		return this.TimeStamp;
	}
	
	public String getStatus() {
		return this.status;
	}
	
	public String getUser() {
		return this.username;
	}
	
	public String toString() {
		return this.address + ":" + this.port;
	}
}
