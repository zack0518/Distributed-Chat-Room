package comm;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import main.Server;
import utils.*;

public class Connection extends Thread{
	DataInputStream in;
	DataOutputStream out;
	OutputStreamWriter writer;
	BufferedReader reader;
	Socket clientSocket;
	int type;// 0 is passive, 1 is initiative
	int cPort;
	String cAddress;
	int localport;
	long starttime = 0;
	long endtime = 0;
	boolean timeout = false;
	boolean first_received = false;
	ConnectionRecord conR;

	
	/*Initiate the connection object.
	 * 0 for passive, 1 for initiative mode*/
	public Connection(Socket aClientSocket, int type) {
		try {
			starttime = System.currentTimeMillis();
			this.type = type;
			clientSocket = aClientSocket;
			cAddress = clientSocket.getInetAddress().getHostAddress();
			cPort = clientSocket.getPort();
			conR = new ConnectionRecord(cAddress, cPort);
			localport = clientSocket.getLocalPort();
			in = new DataInputStream(clientSocket.getInputStream());
			out = new DataOutputStream(clientSocket.getOutputStream());
			writer = new OutputStreamWriter(out,"UTF-8");
			reader = new BufferedReader(new InputStreamReader(in,"UTF-8"));
			Server.pipe.add(conR,reader);
			if(this.type == 1) { // send an authentication message
				this.serverAuth();
			}
			this.start();
		}
		catch(IOException e) {
			System.err.println("(ERROR) Connection: " + e.getMessage());
		}
	}
	
	/*The main function in the separate connection thread.*/
	public void run() {
		DataHandler datahandler = null;
		try {
			Logger.info("Connection established with " + cAddress + ":" + cPort);
			while(!Thread.currentThread().isInterrupted()) {
				
				if(Server.serverPause) {
					Logger.warning("OFFLINE STARTED");
					Thread.sleep(10000);
					Server.serverPause = false;
					Logger.info("OFFLINE FINISHED");
				}
				
				String data;
				Map<String, Object> datamap;
				String req_command;
				

				//judges if it is the first message received.
				if(!first_received) {
					class Task implements Callable<String> {
						public String call() throws Exception {
							Thread.sleep(1500);
							//still not received anything after sleep for 1500ms
							if(!first_received) {
								if(type == 0) {//passive, treat incoming connection as client
									if (!clientIsExist()) {
										Server.servernodes.addClient(new ClientNode(cAddress, cPort));
										Logger.info("TIMEOUT treat this connection as a client");
									}
								}
								else{//Initiative, treat the authentication as succeed
									if(setParentConnection()) Logger.info("Set parent node, " + cAddress + ":" + cPort);
									Server.setAnnounceTask();
								}
							}
							return "";
						}
					}
					ExecutorService executor = Executors.newCachedThreadPool();
					Task task = new Task();
					FutureTask<String> futureTask = new FutureTask<String>(task);
					executor.submit(futureTask);
					//data = reader.readLine();
					Message msg = Server.pipe.readMessage(conR);
//					Logger.info(msg.getDate());
//					Logger.info(msg.getTime());
					//data = Server.pipe.readLine(conR);
					data = msg.read();

					try {
						//passive node, wait 1500ms to get the AUTHENTICATE message.
						if(this.type == 0){
							first_received = true;
							if(data == null) {
								Logger.warning("The target has been disconnected., info: " + this.toString());
								break;
							}
							Logger.info("Server first received data: " + data);
							datamap = JSONHandler.Json2Map(data);
							req_command = (String) datamap.get("command");

							if(req_command.equals("AUTHENTICATE")){
								//another server want connect to me.
								endtime = System.currentTimeMillis();
								Logger.info("Received authentication request in " + (endtime - starttime) + " ms.");
								datahandler = new DataHandler(datamap, this);
								List<Map<String, Object>> exe_results = datahandler.run();
								String rst = (String) exe_results.get(0).get("command");
								if (rst.equals("AUTHENTICATE_SUCCESS")) {
									if(!childIsExist()) {
										Server.servernodes.addChild(new ServerNode(cAddress, cPort, (String) datamap.get("secret")));
										Server.servernodes.removeClient(cAddress, cPort);;
										Logger.info("Added child node, " + this.toString());
									}
									else {
										Logger.warning("Child " + "existed.");
									}
									Map<String, Object> userinfo = Server.user_list.pack();
									this.send(userinfo);
									Logger.info("User synchronize request sent: " + JSONHandler.Map2Json(userinfo));
								} 
								else {
									futureTask.cancel(true);
									this.send(exe_results.get(0));
									Logger.info("Server sent: " + JSONHandler.Map2Json(exe_results.get(0)));
									Logger.warning("Authentication failed, connection closed.");
									break;
								}
							}
							else{
								//a Client connect to me
								if (!clientIsExist()) { //received data in 1500ms
									Server.servernodes.addClient(new ClientNode(cAddress, cPort));
									Logger.info("Client connection detected, client info: " + this.toString());
								}
								try {
									this.handleData(datamap);
								} 
								catch (Exception e) {
									Logger.error("Server handling data error: " + e.getMessage() + " data: " + 
											datamap.toString() + ", " + this.toString());
									break;
								}
							}
						}
						else{ //this.type == 1, initiative mode
							first_received = true;
							Logger.info("Server first received data: " +	data);
							datamap = JSONHandler.Json2Map(data);
							req_command = (String) datamap.get("command");

							if(req_command.equals("AUTHENTICATION_FAIL") || req_command.equals("INVALID_MESSAGE")){
								//the first message received is auth_fail or inv_msg, so we are connecting to an existing server,
								// and we are refused.
								Logger.warning("Authentication failed, connection closed.");
								Server.close();
								break;
							}
							else{
								Server.setAnnounceTask();
								if(setParentConnection()) Logger.info("Parent of current node is " + cAddress + ":" + cPort);
								handleData(datamap);
								Map<String, Object> userinfo = Server.user_list.pack();
								this.send(userinfo);
								Logger.info("User synchronize request sent: " + JSONHandler.Map2Json(userinfo));
							}
						}
					} 
					catch (InterruptedException e) {
						Logger.error(e.getMessage());
					} 
					catch (ExecutionException e) {
						Logger.error(e.getMessage());
					} 
					catch (TimeoutException e) {
						Logger.error(e.getMessage());

					} catch (Exception e) {
						Logger.warning("Connection closed, " + this.toString());
						break;
					}
				}

				//NOT the first message.
				data = null;
				try{
					Message msg = Server.pipe.readMessage(conR);
					data = msg.read();
				}
				catch(Exception e) {
					Logger.error("read error");
				}
				//data = Server.pipe.readLine(conR);
				if(data == null) {
					Logger.warning("A client / server has disconnected., info: " + this.toString());
					break;
				}
				Logger.info("Server received data: " + data);
				//convert the JSON string received to an Object.
				datamap = JSONHandler.Json2Map(data);
				try {
					handleData(datamap);
				} catch (Exception e) {
					Logger.error("Connection closed, " + this.toString());
					break;
				}

			}
		}
		catch (Exception e) {
			Logger.warning("Connection closed, " + this.toString());
		}
		finally {
			try {
				clientSocket.close();
				this.selfKill();
				Logger.info("killed connection");
			}
			catch(Exception e) {
				Logger.error("Connection close failed: " + e.getMessage());
			}
		}
	}
	
	/*When a client connects, try to find another server with lower load.*/
	private void doRedirect() {
		List<ServerRecord> server_info = Server.servernodes.getGlobalServerList();
		//no other servers
		if(server_info.size() == 0) return;
		//find a server with 2 less load than current.
		int load = Server.servernodes.getLoad();
		if(load < 2) return;
		for(ServerRecord record : server_info) {
			if(load - record.getLoad() > 1) {
				Map<String, Object> returnmap = new HashMap<String, Object>();
				returnmap.put("command", "REDIRECT");
				returnmap.put("hostname", record.getAddress());
				returnmap.put("port", record.getPort());
				this.send(returnmap);
				this.selfKill();
				break;
			}
		}
	}
	
	public void doRedirect(ServerRecord targetServer) {
		Map<String, Object> returnmap = new HashMap<String, Object>();
		returnmap.put("command", "REDIRECT");
		returnmap.put("hostname", targetServer.getAddress());
		returnmap.put("port", targetServer.getPort());
		this.send(returnmap);
		this.selfKill();
	}

	/*server announcement sending*/
	public void doAnnounce(Map<String, Object> announce) {
		this.send(announce);
	}
	
	public void doHeartBeat(Map<String, Object> heartBeat) {
		this.send(heartBeat);
	}

		public void doRedirectCheck(ClientNode targetClient, Map<String, Object> redirectCmd) {
		for(ClientNode client : Server.servernodes.getClients()){
			for(Connection con : Server.connections){
				if(client.equals(con.cAddress, con.cPort) 
						&& client.getAddress() == targetClient.getAddress()
						&& client.getPort() == targetClient.getPort()){
					
					con.send(redirectCmd);
					Logger.info("Redirect message has been sent to the message timeout client.");
				}
			}
		}
	}

	//send a message to all nodes.
	//assumption target == "ALL"
	//All means send this message to all other Servers and Clients
	private void sendtoAll(Map<String, Object> exe_result) {
		this.sendtoAllClients(exe_result);
		this.sendtoAllServerExceptSource(exe_result);
		Logger.info("A message has been sent to all nodes.");
	}

	//send a message to all client nodes.
	//assumption target == "CLIENTS"
	//CLIENTS means send this message to Clients of this Server.
	private void sendtoAllClients(Map<String, Object> exe_result) {
		for(ClientNode client : Server.servernodes.getClients()){
			for(Connection con : Server.connections){
				if(client.equals(con.cAddress, con.cPort)){
					//there, the broadcast message will be sent to all
					// clients of THIS NODE.
					//if the source is client, it will receive the broadcast
					// as well. if the source is a server, it will not
					// receive any message.
					con.send(exe_result);
					Logger.info("A message has been sent to all clients.");
				}
			}
		}
	}
	
	//send a message to all clients except resource
	private void sendtoAllClientsExSource(Map<String, Object> exe_result) {
		for(ClientNode client : Server.servernodes.getClients()){
			if(client.equals(this.cAddress, this.cPort)) continue;
			for(Connection con : Server.connections){
				if(client.equals(con.cAddress, con.cPort)){
					//there, the broadcast message will be sent to all
					// clients of THIS NODE.
					//if the source is client, it will receive the broadcast
					// as well. if the source is a server, it will not
					// receive any message.
					con.send(exe_result);
					Logger.info("A message has been sent to all clients.");
				}
			}
		}
	}

	//send a message to all server nodes.
	//assumption target == "ALL_SERVERS"
	private void sendtoAllServer(Map<String, Object> exe_result) {
		for(ServerNode server : Server.servernodes.getCurrAllServer()){
			for(Connection con : Server.connections){
				//System.out.println(con.toString());
				if(con.equal(server.getAddress(),server.getPort())) {
					//match the server node and the connection
					if(!con.equal(Server.servernodes.getAddress(),Server.servernodes.getPort())) {
						//send to all servers except current node
						con.send(exe_result);
						Logger.info("A message has been sent to all servers.");
					}
				}
			}
		}
	}
	
	//send a message to all sever nodes except the source.
		//assumption target == "SERVERS"
		private void sendtoAllServerExceptSource(Map<String, Object> exe_result) {
			for(ServerNode server : Server.servernodes.getCurrAllServer()){
				for(Connection con : Server.connections){
					if(con.equal(server.getAddress(),server.getPort())) {
						//match the server node and the connection
						if(!con.equal(this.cAddress,this.cPort)) {
							//send to all servers except the source node
							con.send(exe_result);
							Logger.info("A message has been sent to all servers except its source.");
						}
					}
				}
			}
		}

	/*authenticate current server to an existing server.*/
	private void serverAuth() {
		Map<String, Object> auth_req = new HashMap<String, Object>();
		Logger.info("Autherithing with " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
		auth_req.put("command", "AUTHENTICATE");
		auth_req.put("secret", Server.servernodes.getSecret());
		Logger.info("Server sending data: " + JSONHandler.Map2Json(auth_req));
		this.send(auth_req);
		setParentConnection();
	}

	/*Send a message to other nodes used information stored inside the variable map*/
	public void send(Map<String, Object> map) {
		Map<String, Object> res = new HashMap<String, Object>(map);
		//res.remove("target"); do not remove the "target" KV pair,so that
		// the receiver could know deliver this message to who
		String s = JSONHandler.Map2Json(res);
		try {
			writer.write(s + "\n");
			writer.flush();
		} catch (IOException e) {
			Logger.error("Send error: " + e.getMessage());
		}
	}
	
	/*Send a message to a specific node, when target == "TARGET"*/
	private void sendTo(Map<String, Object> map,  String address, int port) {
		map.remove("address");
		map.remove("port");
		for(Connection c : Server.connections) {
			if(c.equal(address, port)) c.send(map);
		}
	}
	
	/*Decide if the server should close the connection*/
	private boolean isFail(String command) {
		switch (command){
			case "INVALID_MESSAGE":
			case "AUTHENTICATION_FAIL":
			case "LOGIN_FAILED":
			case "REGISTER_FAILED":
				return true;
			case "LOGOUT":
			case "LOCK_DENIED":
				Map<String, Object> info = this.getConnectionInfo();
				if(Server.servernodes.isValidServer((String) info.get("address"), (int) info.get("port"))) return false;
				else return true;
		}
		return false;
	}
	
	/*Judges if a port and an address equals the information of current connection*/
	public boolean equal(String addr, int port) {
		if(addr.equals(this.cAddress) && port == this.cPort) return true;
		return false;
	}
	
	/*Get information of current connection*/
	public Map<String, Object> getConnectionInfo(){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("address", this.cAddress);
		result.put("port", this.cPort);
		result.put("type", this.type);
		return result;
	}

	private boolean clientIsExist(){
		for(ClientNode client : Server.servernodes.getClients()){
			if(client.equals(cAddress,cPort)){
				return true;
			}
		}
		return false;
	}

	private boolean childIsExist(){
		for(ServerNode serverNode : Server.servernodes.getChildren()){
			if(serverNode.equals(cAddress,cPort)){
				return true;
			}
		}
		return false;
	}

	private boolean setParentConnection(){
		if(Server.servernodes.getParent() != null){
			ServerNode parent = new ServerNode(Server.servernodes.getSecret());
			parent.setAddress(cAddress);
			parent.setPort(cPort);
			Server.servernodes.setParent(parent);
			return true;
		}
		return false;
	}

	private void handleData(Map<String, Object> datamap) throws Exception{
		DataHandler datahandler = new DataHandler(datamap, this);
		boolean fail = false;
		//get results with the input data.
		List<Map<String, Object>> exe_results = datahandler.run();
		if(exe_results.size() > 0) {
			//decide the target the response should be sent to.
			for(Map<String, Object> exe_result : exe_results) {
				String target = (String) exe_result.get("target");
				if(target != null) {
					exe_result.remove("target");
					if (target.equals("SERVERS")) this.sendtoAllServerExceptSource(exe_result);
					if(target.equals("ALL_SERVERS")) this.sendtoAllServer(exe_result);
					if (target.equals("CLIENTS")) this.sendtoAllClients(exe_result);
					if (target.equals("CLIENT_EX")) this.sendtoAllClientsExSource(exe_result);
					if (target.equals("ALL")) this.sendtoAll(exe_result);
					if (target.equals("NOTHING")) {
						String command = (String) exe_result.get("command");
						if(command.equals("REDIRECT_CHECK")) {
							this.doRedirect();
						}
						continue;
					}
					if (target.equals("RESOURCE")) this.send(exe_result);
					if (target.equals("TARGET")) this.sendTo(exe_result, (String) exe_result.get("address"), (int) exe_result.get("port"));
					Logger.info("Server replying data: " + JSONHandler.Map2Json(exe_result) + ", to: " + target);
				}
				String res_command = (String) exe_result.get("command");
				if(this.isFail(res_command)) {
					this.send(exe_result);
					Logger.info("Server sent failue message: " + JSONHandler.Map2Json(exe_result));
					fail = true;
					Logger.warning("Connection closed.");
				}
			}
			if(fail) throw new Exception("Server considers this connection should be closed.");
		}
	}

	public String toString() {
		return "Connection Info: {address: " + cAddress + ", port: " + cPort + "}";
	}
	
	/*close connection and remove it from connection list.*/
	public void selfKill(){
		Server.pipe.remove(conR);
		//remove from connection list.
		this.removeConnection(cAddress, cPort);
		//if connected with parent, make it null
		if(Server.servernodes.getParent() != null && Server.servernodes.getParent().equals(this.cAddress, this.cPort)) 
			Server.servernodes.setParent(null);
		//if connected with server, delete from children
		Server.servernodes.removeChild(cAddress, cPort);
		//if connected with client, delete from client list
		Server.servernodes.removeClient(cAddress, cPort);
		//close connection
		return;
	}
	
	public synchronized void removeConnection(String address, int port) {
		Iterator<Connection> iterator = Server.connections.iterator();
		while(iterator.hasNext()){
			Connection c = iterator.next();
			if(c.equal(address, port)) iterator.remove();
		}
	}
}
