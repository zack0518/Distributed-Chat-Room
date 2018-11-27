package main;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import org.apache.commons.codec.digest.DigestUtils;

import comm.ClientNode;
import comm.Connection;
import comm.ServerNode;
import comm.ServerRecord;
import utils.*;

/**
 * Usage:
 * java -jar ActivityStreamerServer.jar (-p <local port>) (-t <target server address>) (-tp <target server port)
 * 					 (-s <secret>) 
 * 
 * */

public class Server {
	public static Users user_list; // a list of users that have ever registered to the server.
	public static ServerNode servernodes; // object reflects to current server.
	public static List<Connection> connections = new ArrayList<Connection>(); //saves a list of connections
	public static String server_id = "";
	public static boolean announce_flag = false;
	public static MessagePipeline pipe = new MessagePipeline();
	public static Date lastCheckTime = new Date();  // The Last time to check the server failure
	public static long checkInterval = 3000; // The checking interval. 1000 indicates 1s
	public static long serverTimeOut = 10000;
	public static long clientMsgTimeOut = 10000;
	public static boolean serverPause = false;
 
	public static void main(String[] args) {
		ServerSocket listenSocket = null;
		String currAddress = null;
		int defaultPort = 3877;
		int mode = 0;//I am the root Server.
		String defaultSecret = "asdfghjkl";
		user_list = new Users();
		try {
			ArgumentsHandler arg_hdl = new ArgumentsHandler(args);
			Map<String, Object> arg_map = arg_hdl.getArgs();

			try {
				if(arg_map.get("laddress") != null) currAddress = (String) arg_map.get("laddress");
				else currAddress = InetAddress.getLocalHost().getHostAddress();
			} 
			catch (UnknownHostException e) {
				e.printStackTrace();
			}
			//set secret.
			if(arg_map.get("secret" )!= null) {
				String secret = (String) arg_map.get("secret");
				Logger.info("Server secret: " + secret);
				servernodes = new ServerNode(secret);
				servernodes.setAddress(currAddress);
				server_id = DigestUtils.md5Hex(secret + System.currentTimeMillis());
			}
			else{
				Logger.warning("Server secret not set, the server will initialize with a default secret");
				Logger.info("Server secret: " + defaultSecret);
				servernodes = new ServerNode(defaultSecret);
				servernodes.setAddress(currAddress);
				server_id = DigestUtils.md5Hex(defaultSecret + System.currentTimeMillis());
			}
			
			//set local port.
			if(arg_map.get("port")!= null) {
				servernodes.setPort((int) arg_map.get("port"));
			}
			else{
				Logger.info("Local port not set, the server will use port 3877 as default");
				servernodes.setPort(defaultPort);
			}
			
			//set target address.
			String t_add = null;
			if(arg_map.get("taddress") != null) {
				t_add = (String) arg_map.get("taddress");
			}
			int t_port = 0;
			if(arg_map.get("taddress") != null) {
				t_port = (int) arg_map.get("tport");
			}
			//connect to another Server
			if(t_add != null && t_port!= 0){
				mode = 1;// a passive Server, need to link to another server
				Logger.info("Connecting to an existing server....");
				Logger.info("Target server: " + t_add + ":" + t_port);
				Socket socket = new Socket(t_add, t_port);
				ServerNode parent = new ServerNode(t_add, t_port, servernodes.getSecret());
				servernodes.setParent(parent);
				Connection sConn = new Connection(socket, mode);// initiative: I am connecting to other nodes
				connections.add(sConn);
			}

			listenSocket = new ServerSocket(servernodes.getPort());

			Logger.info("Initialization finished");
			if(mode == 1){
				Logger.info("This server has connected to: " + t_add + ":" + t_port);
				// Connected to the parent, open the 
			}
			else{
				Logger.info("This is the root Server");
				setAnnounceTask();
			}
			setHeatBeats();
			//checkOfflineClient();
			setBalanceTask();
			Logger.info("Local port: " + servernodes.getPort());
			
			while(true) {
				Logger.info("Server listening for a connection.");
				Socket clientSocket = listenSocket.accept();
				Logger.info("Received connection: " +
						connections.size() + 1);
				if(!Server.connectionExist(clientSocket.getInetAddress().getHostAddress(),
						clientSocket.getPort())) {
					Connection c = new Connection(clientSocket, 0); //passive mode: other nodes connects to me
					connections.add(c);
					Logger.info(connections.size() + " Connected");
				}
				else {
					Logger.error("Connection already exists.");
				}
			}
		}
		catch(IOException e){
			Logger.error("Listen socket: " + e.getMessage());
		}
		catch(NumberFormatException e) {
			Logger.error("Parse error: " + e.getMessage());
		}
		finally {
			try {
				listenSocket.close();
			} 
			catch (IOException e) {
				Logger.error("Closing connection: " + e.getMessage());
			}
			catch (NullPointerException e) {
				Logger.error("Session quit when connection is not established yet.");
			}
		}
	}
	
	/*examines if the server has already connected to somewhere*/
	public static boolean connectionExist(String address, int port) {
		for(Connection c : connections) {
			if(c.equal(address, port)) return true;
		}
		return false;
	}
	
	/*find a connection with specific ip and port*/
	public static Connection findConnection(String address, int port) {
		for(Connection c : connections) {
			if(c.equal(address, port)) return c;
		}
		return null;
	}
	
	/*send server announce every 5 seconds*/
	public static void setAnnounceTask() {
		if(announce_flag) return;
		announce_flag = true;
		ExecutorService executor = Executors.newCachedThreadPool();
		class AnnounceTask implements Callable<String> {
			public String call() throws Exception {
				while(true) {
					String command = "SERVER_ANNOUNCE";
					String id = server_id;
					String hostname = Server.servernodes.getAddress();
					int load = Server.servernodes.getLoad();
					int port = Server.servernodes.getPort();
					Map<String, Object> announce = new HashMap<String, Object>();
					announce.put("command", command);
					announce.put("id", id);
					announce.put("load", load);
					announce.put("hostname", hostname);
					announce.put("port", port);
					String an_str = JSONHandler.Map2Json(announce);
					Logger.info("Server connection pool size:" + connections.size() + ", " +"Server connected with " 
							+ servernodes.getCurrAllServer().size() + " servers, Server has " + 
							servernodes.getClients().size() + " clients, server tree size: " + servernodes.getGlobalServerList().size() + ".");
					Logger.info("Server announcing: " + an_str);
					List<ServerNode> servers = servernodes.getCurrAllServer();
					for(ServerNode server : servers) {
						Connection c = server.findConnection();
						c.doAnnounce(announce);
					}
					if(serverPause) {
						Thread.sleep(10000);
					}else {
						Thread.sleep(5000);
					}
				}
			}
		}
		AnnounceTask task = new AnnounceTask();
		FutureTask<String> futureTask = new FutureTask<String>(task);
		executor.submit(futureTask);
	}
	
	
	/*send heartbeat to check offline server and client every 1 sec*/
	public static void setHeatBeats() {
		ExecutorService executor = Executors.newCachedThreadPool();
		class HeartBeatTask implements Callable<String> {
		public String call() throws Exception {
		while(true) {
			Date currentTime = new Date();
			String command = "HEART_BEATS";
			String id = server_id;
			String hostname = Server.servernodes.getAddress();
			int port = Server.servernodes.getPort();
			Map<String, Object> heartBeats = new HashMap<String, Object>();
			heartBeats.put("command", command);
			heartBeats.put("id", id);
			heartBeats.put("hostname", hostname);
			heartBeats.put("port", port);
			heartBeats.put("timestamp", currentTime);
			
			List<ServerNode> servers = servernodes.getCurrAllServer();
			String heartbeat_str = JSONHandler.Map2Json(heartBeats);
			
			for(ServerNode server : servers) {
				Connection c = server.findConnection();
				c.doHeartBeat(heartBeats);
			}
			
			/* Check the offline server*/
			List<ServerRecord> serverlist = Server.servernodes.getGlobalServerList();
			/* Time to check the offline server */
			if(currentTime.getTime() - Server.lastCheckTime.getTime() >= Server.checkInterval) {
				/*Find the corrected serverNode*/
				Logger.info("Checking Offline Servers"+ heartbeat_str);
				for(ServerRecord server : serverlist) {
					
					if(server.getTimeStamp() != null) {
						if(currentTime.getTime() - server.getTimeStamp().getTime()  >= Server.serverTimeOut) { 
							//If the timestamp of a server is larger the defined interval, the server may be offline
							System.out.println();
							Logger.warning("Offline Server detected! : " + server.getAddress()+"  "+server.getPort());
							/*First remove the childrens of the server*/
							
							for(Connection c : connections) {
								Map<String, Object> connInfo = c.getConnectionInfo();	
								if(connInfo.get("address").equals(server.getAddress()) && (int) connInfo.get("port")==server.getPort()) {
								     c.removeConnection(server.getAddress(), server.getPort());
								     break;
								}
							}
							/*remove the server*/
							servernodes.removeServerFromServerList(server.getAddress(),server.getPort());
							servernodes.removeChild(server.getAddress(), server.getPort());
							break;
						}
					}
				}
				Server.lastCheckTime = currentTime;
			 }
				try {
					if(serverPause) {
						Thread.sleep(10000);
					}else {
						Thread.sleep(1000);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		  }
		}
		HeartBeatTask task = new HeartBeatTask();
		FutureTask<String> futureTask = new FutureTask<String>(task);
		executor.submit(futureTask);
	}
	
	//If the client is back, the client with old ip and port will be removed
	//If the client is found in the connection list, it's offline
	private static boolean isClientBack(ClientNode client) {
		List<ClientNode> currClientNodes = servernodes.getClients();
		for(ClientNode c : currClientNodes) {
			if(c.getAddress() == client.getAddress() && c.getPort() == client.getPort()) {
				return false;
			}
		}
		return true;
	}
	
	public static void checkOfflineClient() {
		ExecutorService executor = Executors.newCachedThreadPool();
		class CheckClientConnectionTask implements Callable<String> {
		public String call() throws Exception {
		while(true) {
			Map<String, Object> redirectSelf= new HashMap<String, Object>();
			
			Date currentTime = new Date();
			
			String hostname = Server.servernodes.getAddress();
			int port = Server.servernodes.getPort();
			redirectSelf.put("command", "REDIRECT");
			redirectSelf.put("hostname", hostname);
			redirectSelf.put("port",  port);
			String redirectSelf_str = JSONHandler.Map2Json(redirectSelf);			
			List<ClientNode> currClientNodes = servernodes.getClients();

			for(ClientNode client : currClientNodes) {
				if(currentTime.getTime() - client.getTimeStamp().getTime() >=  clientMsgTimeOut) {
					Logger.info("Checking Offline Client  " );
					for(Connection c : connections) {
						Map<String, Object> connInfo = c.getConnectionInfo();
						
						if(connInfo.get("address") == client.getAddress() && (int)connInfo.get("port") == client.getPort()) {
							c.doRedirectCheck(client, redirectSelf);
						}
						// wait 1s for client reconnection, client is online if client connection still exists,
						Thread.sleep(1000);
						if(isClientBack(client)) {
							client.setTimeStamp(new Date());
						}else {
							//client offline detected
						//	Logger. ("Offline Client Detected: " + client.getAddress()+"  "+client.getPort());
							
							servernodes.removeClient(client.getAddress(), client.getPort());
							connections.remove(client);
						}
					}
				}
			}
			try {
				if(serverPause) {
					Thread.sleep(10000);
				}else {
					Thread.sleep(1000);
				} 
			}catch (InterruptedException e) {
				e.printStackTrace();
			}
		  }
		
		 }
		}
		CheckClientConnectionTask task = new CheckClientConnectionTask();
		FutureTask<String> futureTask = new FutureTask<String>(task);
		executor.submit(futureTask);
	}
	
	public static void setBalanceTask() {
		ExecutorService executor = Executors.newCachedThreadPool();
		LoadBanlanceTask task = new LoadBanlanceTask();
		FutureTask<String> futureTask = new FutureTask<String>(task);
		executor.submit(futureTask);
	}
	
	/*close the server*/
	public static void close() {
		System.exit(0);
	}
	
}
