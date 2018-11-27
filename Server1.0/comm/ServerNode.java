package comm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import main.Server;
import utils.Logger;
/**
 * Author: Zijian Wang
 * Mail Address: zijianw2@student.unimelb.edu.au
 * Version: 1.0.0
 * Functions:
 * 1.Find all servers and clients from current server node.
 * 2.Broadcast a message to all selected nodes.
 * 3.Push a free server with currently minimum load.
 * 
 * Usage:
 * 
 * 1.Initiate a ServerNode Object:
 * ServerNode server_node = new ServerNode("some secret key");
 * if(server_node has a server it connects to){
 * 		server_node.setParent(some existing server);
 * }
 * */

public class ServerNode {
	private ServerNode parent = null;
	private List<ServerNode> children = new ArrayList<ServerNode>(); // a list of servers that connect to the current server.
	private List<ClientNode> clients = new ArrayList<ClientNode>(); // a list of clients that connect to the current server.
	private List<ServerRecord> global_servers = new ArrayList<ServerRecord>(); // a list of informations of servers in the whole system.
	private String secret = "asdfghjkl"; // default secret
	private int port = 0;
	private String addr = "";
	private String id = "";
	
	//initiate a server node
	public ServerNode(String secret) {
		if(secret != null && !secret.equals("")) this.secret = secret;
	}
	//initiate a server node
	public ServerNode(String address, int port, String secret) {
		this.setAddress(address);
		this.setPort(port);
		if(secret != null && !secret.equals("")) this.secret = secret;
	}
	
	
	//set the parent node of current node.
	public void setParent(ServerNode parent) {
		this.parent = parent;
	}
	
	//get the parent node of current node.
	public ServerNode getParent() {
		return this.parent;
	}
	
	//get load of current server.
	public int getLoad() {
		int size = 0;
		for(ClientNode client : clients) {
			if(client.getStatus().equals("login")) size++;
		}
		return size;
	}
	
	//add a new child to the children list
	public void addChild(ServerNode child) {
		this.children.add(child);
	}
	
	//add a new client to current node
	public void addClient(ClientNode clientNode) {
		this.clients.add(clientNode);
	}

	public List<ServerNode> getCurrAllServer(){//
		List<ServerNode> result = new ArrayList<ServerNode>();
		ServerNode p = this.getParent();
		if(p != null ){
			result.add(p);
		}
		if(this.getAllChildren() != null) {
			result.addAll(this.getAllChildren());
		}
		return result;
	}
	
	//get a list of all clients of this server
	public List<ClientNode> getClients() {
		return clients;
	}
	
	//get the secret of current node
	public String getSecret() {
		return secret;
	}

	//address of current node
	public void setAddress(String address) {
		this.addr = address;
	}

	public String getAddress() {
		return addr;
	}

	public int getPort() {
		return this.port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public List<ServerNode> getChildren(){
		return this.children;
	}


	//get all children nodes of current node.
	private List<ServerNode> getAllChildren(){
		List<ServerNode> result = new ArrayList<ServerNode>();
		if(children.size() == 0) return null;
		for(ServerNode child : children) {
			result.add(child);
		}
		return result;
	}
	
	public boolean validSecret() {
		return !(secret == null || secret.equals(""));
	}

	public boolean equals(String Address, int port){
//		System.out.println("This Add  "+this.addr+"    "+this.port);
//		System.out.println("Checked Add  "+Address+"    "+port);
		if(this.addr.equals(Address) && this.port == port){
			return true;
		}
		return false;
	}

	//find the connection corresponding to information stored in current object
	public Connection findConnection() {
		for(Connection c : Server.connections) {
			Map<String, Object> connection_info = c.getConnectionInfo();
			if(this.addr.equals(connection_info.get("address")) && this.port == (int) connection_info.get("port")) return c;
		}
		return null;
	}
	
	public ClientNode findClient(String address, int port) {
		for(ClientNode c : clients) {
			if(c.equals(address, port)) return c;
		}
		return null;
	}

	public boolean isDuplicate(int port) {
		for(ServerRecord serverNode : this.global_servers) {
			if(serverNode.getPort() == port) {
				return true;
			}
		}
		return false;
	}

	/*Get server information acquired from server announcement*/
	public List<ServerRecord> getGlobalServerList() {
		return this.global_servers;
	}
	
	
	public synchronized void removeServerFromServerList(String address, int port) {
		Logger.info("Removing server "+ address +":"+port + " from current node.");
		Iterator<ServerRecord> iterator = this.global_servers.iterator();
		while(iterator.hasNext()){
			ServerRecord currServer = iterator.next();
			if(currServer.getAddress().equals(address) && currServer.getPort() == port) iterator.remove(); 
		}
	}

	
	/*Remove a child*/
	public synchronized void removeChild(String address, int port) {
		Iterator<ServerNode> iterator = this.children.iterator();
		while(iterator.hasNext()){
			ServerNode child = iterator.next();
			if(child.equals(address, port)) iterator.remove(); 
		}
	}
	
	/*Remove a client*/
	public synchronized void removeClient(String address, int port) {
		Iterator<ClientNode> iterator = this.clients.iterator();
		while(iterator.hasNext()){
			ClientNode client = iterator.next();
			if(client.equals(address, port)) iterator.remove();
		}
	}
	
	/*Tests if a node is a valid server*/
	public boolean isValidServer(String address, int port) {
		for(ServerNode server : this.getCurrAllServer()) {
			if(address.equals(server.getAddress()) && port == server.getPort()) return true;
		}
		return false;
	}
	
	/*Tests if a node is a valid client*/
	public boolean isValidClient(String address, int port) {
		for(ClientNode client : this.getClients()) {
			if(client.equals(address, port)) return true;
		}
		return false;
	}
}
