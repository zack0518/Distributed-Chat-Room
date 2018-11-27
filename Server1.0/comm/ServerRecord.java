package comm;

import java.util.Date;

/**
 * @description
 * This class is an abstraction of a server.
 * 
 * @usage
 * This class is only used to record informations and loads after one node receives a server announcement.
 * Every server nodes should have a list of server records.
 *
 */
public class ServerRecord {
	private String address;
	private int port;
	private int load;
	// this secret is server id hash
	private String secret;
	private Date TimeStamp = new Date();
	
	public ServerRecord(String address, int port, int load, String secret) {
		this.address = address;
		this.port = port;
		this.setLoad(load);
		this.setSecret(secret);
	}
		
	public String getAddress() {return this.address;}
	public void setAddress(String address) {this.address = address;}
	public int getPort() {return this.port;}
	public void setPort(int port) {this.port = port;}
	public int getLoad() {return load;}
	public void setLoad(int load) {this.load = load;}
	public String getSecret() {return secret;}
	public void setSecret(String secret) {this.secret = secret;}
	public void setTimeStamp(Date TimeStamp) {this.TimeStamp = TimeStamp;}
	public Date getTimeStamp() {return TimeStamp;}
	
	public boolean equal(String address, int port) {
		return (address.equals(this.address) && port == this.port);
	}
	
	public boolean equal(String id) {
		return  (this.secret.equals(id));
	}

	public boolean equal(ServerNode node) {
		return (this.address.equals(node.getAddress()) && this.port == node.getPort());
	}

	public String toString() {
		return this.address + ":" + this.port;
	}
}
