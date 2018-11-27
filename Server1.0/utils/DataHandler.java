package utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import comm.ClientNode;
import comm.Connection;
import comm.ServerNode;
import comm.ServerRecord;
import main.Server;

/**
 * Functions
 * 1.Provide respond to all kinds of messages that are sent to the server.
 * 
 * Usage:
 * In the initiate function, the input should be an Object rather than a JSON string.
 * Always use the run() function to deal with data.
 * 
 * COMMAND APIS (Server interactive):
 * 
 * AUTHENTICATE:
 * receive from: SERVER
 * reply to: SERVER
 * reply body (Target) : AUTHENTICATION_FAIL(RESOURCE), AUTHENTICATE_SUCCESS(NOTHING)
 * 
 * REGISTER:
 * receive from: CLIENT
 * reply to: SERVER / RESOURCE
 * reply body (Target) : REGISTER_FAILED(RESOURCE), LOCK_REQUEST(SERVER)
 * 
 * LOGIN:
 * receive from: CLIENT
 * reply to: ALL / RESOURCE
 * reply body (Target): LOGIN_SUCCESS(ALL), LOGIN_FAILED(RESOURCE)
 * 
 * SERVER_ANNOUNCE:
 * receive from: SERVER
 * reply to: NOTHING
 * 
 * LOCK_REQUEST:
 * receive from: SERVER
 * reply to: SERVER
 * reply body (Target): LOCK_DENIED(SERVER), LOCK_ALLOWED(SERVER)
 * 
 * LOCK_ALLOWED:
 * receive from: SERVER
 * reply to: SERVER
 * reply body (Target): REGISTER_SUCCESS(SERVER)
 * 
 * LOGOUT:
 * receive from: CLIENT
 * reply to: SERVER
 * reply body (Target): LOGOUT(SERVER)
 * 
 * 
 * COMMAND APIS (Server send only):
 * 
 * REDIRECT:
 * send to: RESOURCE
 * 
 * INVALID_MESSAGE:
 * send to: RESOURCE
 * 
 * AUTHENTICATION_FAIL:
 * send to: RESOURCE
 * 
 * SERVER_ANNOUNCE:
 * send to: SERVER
 * */
public class DataHandler {
	Map<String, Object> data = null;
	List<Map<String, Object>> result_maps = new ArrayList<Map<String, Object>>();
	Connection connection = null;
	
	/*Initiate the instance*/
	public DataHandler() {
		this.data = new HashMap<String, Object>();
	}
	
	public DataHandler(Map<String, Object> datamap, Connection currConnection) {
		this.data = datamap;
		this.connection = currConnection;
	}
	
	public void setData(Map<String, Object> datamap) {
		this.data = datamap;
	}
	
	public void setCurrConnection(Connection curr) {
		this.connection = curr;
	}

	// update the Client 
	public void updateClientTimeStamp() {
		Map<String, Object> connInfo = connection.getConnectionInfo();
		ClientNode client = Server.servernodes.findClient((String)connInfo.get("address"), (int)connInfo.get("port"));
		Date updateTimeStamp = new Date();
		client.setTimeStamp(new Date());
	}
	
	/*Decide where this message should be sent.
	 * RESOURCE: return the message to where it comes from.
	 * TARGET: return the message to a specific node within the value of key "node".
	 * CLIENTS: send the message to all clients of current server.
	 * CLIENT_EX: send the message to all clients except source.
	 * SERVERS: send the message to all servers except the source.
	 * ALL: send the message to all existing nodes, including clients and servers.
	 * ALL_SERVERS: send to all servers including the source.
	 * NOTHING: don't send anything.*/
	private void setSendTarget(String target, Map<String, Object> map) {
		map.put("target", target);
	}

	/*Handle JSON parsing error*/
	private void doInvalidMessageJ() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", "INVALID_MESSAGE");
		map.put("info", "JSON parse error while parsing message");
		result_maps.add(map);
	}
	
	/*Handle Invalid_message error*/
	private void doInvalidMessageI() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", "INVALID_MESSAGE");
		map.put("info", "the received message did not contain a command");
		result_maps.add(map);
	}
	
	private void doInvalidMessageI(String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", "INVALID_MESSAGE");
		map.put("info", message);
		result_maps.add(map);
	}
	
	private void doAuthenticate() {
		Map<String, Object> map = new HashMap<String, Object>();
		String secret = (String) data.get("secret");
		if(secret == null || !secret.equals(Server.servernodes.getSecret())){
			map.put("command", "AUTHENTICATION_FAIL");
			map.put("info", "the supplied secret is incrrect: " + secret);
		}
		else{
			map.put("command","AUTHENTICATE_SUCCESS");
			map.put("secret",secret);
		}
		result_maps.add(map);
	}

	private void doLogin() {
		updateClientTimeStamp();
		
		Map<String, Object> map = new HashMap<String, Object>();
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		String op_res = Server.user_list.login(username, secret);
		
		if(username.equals("anonymous")) return;
		if(op_res.equals("SUCCESS")) {
			Map<String, Object> connInfo = connection.getConnectionInfo();
			System.out.println("CLIENT INFO : "+(String)connInfo.get("address")+"     "+ (int)connInfo.get("port"));
			ClientNode client = Server.servernodes.findClient((String)connInfo.get("address"), (int)connInfo.get("port"));
			client.login(username);
			//send success to client;
			map.put("command", "LOGIN_SUCCESS");
			this.setSendTarget("RESOURCE", map);
			result_maps.add(map);
			//do a redirection check;
			map = new HashMap<String, Object>();
			map.put("command", "REDIRECT_CHECK");
			this.setSendTarget("NOTHING", map);
			result_maps.add(map);
		}
		else if(op_res.equals("INVALID_USER")) {
			map.put("command", "LOGIN_FAILED");
			map.put("info", "attempt to login with wrong username");
			if(this.isValidServerConn()) this.setSendTarget("NOTHING", map);
			else this.setSendTarget("RESOURCE", map);
			result_maps.add(map);
		}
		else if(op_res.equals("AUTHENTICATION_FAIL")) {
			map.put("command", "AUTHENTICATION_FAIL");
			map.put("info", "the supplied secret is incrrect: " + secret);
			if(this.isValidServerConn()) this.setSendTarget("NOTHING", map);
			else this.setSendTarget("RESOURCE", map);
			result_maps.add(map);
		}
		else if(op_res.equals("DUPLICATE_LOGIN")) {
			map.put("command", "LOGIN_FAILED");
			map.put("info", "Same user has logged in at this server.");
			if(this.isValidServerConn()) this.setSendTarget("NOTHING", map);
			else this.setSendTarget("RESOURCE", map);
			result_maps.add(map);
		}
	}
	
	private void doLogout() {
		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> connInfo = connection.getConnectionInfo();
		ClientNode client = Server.servernodes.findClient((String)connInfo.get("address"), (int)connInfo.get("port"));
		String username = client.getUser();
		String op_res = Server.user_list.logout(username);
		if(op_res.equals("SUCCESS")) {
			this.setSendTarget("NOTHING", map);
			map.put("timestamp", data.get("timestamp"));
			map.put("command", "LOGOUT");
		}
		result_maps.add(map);
	}
	
	/*Deal with a register message. If only one server, register it. If multiple servers, send LOCK_REQUEST.*/
	private void doRegister() {
		updateClientTimeStamp();
		
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		if(username == null || secret == null) {
			doInvalidMessageI();
			return;
		}

		Map<String, Object> map = new HashMap<String, Object>();
		//user exist on current server, return fail
		if(Server.user_list.userExists(username)) {
			map.put("command", "REGISTER_FAILED");
			map.put("info", username + " is already registered with the system.");
			this.setSendTarget("RESOURCE", map);
		}
		//user not exist on current server, create it then send request
		else {
			int serversize = Server.servernodes.getGlobalServerList().size();
			long timestamp = (long) data.get("timestamp");
			//the only server
			if(serversize == 0) {
				map = new HashMap<String, Object>();
				this.setSendTarget("RESOURCE", map);
				Server.user_list.add(username, secret, timestamp, -1);
				Server.user_list.setStatus(username, "logout");
				map.put("command", "REGISTER_SUCCESS");
				map.put("timestamp", data.get("timestamp"));
				result_maps.add(map);
				return;
			}
			Map<String, Object> connInfo = connection.getConnectionInfo();
			Server.user_list.putRegisterRequestInfo(username, secret, (String) connInfo.get("address"), (int) connInfo.get("port"));
			Server.user_list.add(username, secret, timestamp, 0);
			map.put("command", "LOCK_REQUEST"); 
			map.put("timestamp", data.get("timestamp"));
			map.put("username", username);
			map.put("secret",secret);
			this.setSendTarget("ALL_SERVERS", map);
		}
		result_maps.add(map);
	}

	private void doLock() {
		if(!this.isValidServerConn()) {
			this.doInvalidMessageI("received LOCK_REQUEST from an unauthenticated machine.");
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		long timestamp = (long) data.get("timestamp");
		Logger.info("Received Lock Request, User Info : "+ username+"  "+secret);

		//check current node
		if(username.equals(null) || secret.equals(null)) {
			this.doInvalidMessageI();
			return;
		}
		// user not exist, add this user and send allowed
		if (!Server.user_list.userRegistered(username)) {
			Server.user_list.add(username, secret, timestamp, 0);
			map.put("command", "LOCK_ALLOWED");
			map.put("timestamp", data.get("timestamp"));
			map.put("username", username);
			map.put("secret", secret);
			this.setSendTarget("ALL_SERVERS", map);
		}
		// exists a record with different secret
		else if (Server.user_list.userHasDiffSecret(username, secret)){
			boolean hasOlder = Server.user_list.hasOlderRecord(username, timestamp);
			// an older record exists, deny
			if(hasOlder) {
				map.put("command", "LOCK_DENIED");
				map.put("timestamp", timestamp);
				map.put("username", username);
				map.put("secret", secret);
				this.setSendTarget("SERVERS", map);
			}
			// local is newer, delete it and add the older one as if never exists.
			else {
				Server.user_list.deleteUser(username);
				Server.user_list.add(username, secret, timestamp, 0);
				map.put("command", "LOCK_ALLOWED");
				map.put("timestamp", data.get("timestamp"));
				map.put("username", username);
				map.put("secret", secret);
				this.setSendTarget("ALL_SERVERS", map);
			}
		}
		result_maps.add(map);
		
		//transfer the lock request to other nodes
		map = new HashMap<String, Object>();
		map.put("command", "LOCK_REQUEST");
		map.put("timestamp", timestamp);
		map.put("username", username);
		map.put("secret", secret);
		this.setSendTarget("SERVERS", map);
		result_maps.add(map);
	}

	private void doLockDeny() {
		if(!this.isValidServerConn()) {
			this.doInvalidMessageI("received LOCK_DENIED from an unauthenticated machine.");
			return;
		}
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		
		if(username.equals(null) || secret.equals(null)) {
			doInvalidMessageI();
			return;
		}
		if(Server.user_list.checkSameUser(username, secret)) {
			Server.user_list.DeleteUser(username, secret);
		}
		//transfer the message
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", "LOCK_DENIED");
		map.put("username", username);
		map.put("secret", secret);
		this.setSendTarget("SERVERS", map);
		map.put("timestamp", data.get("timestamp"));
		result_maps.add(map);
	}

	private void doLockAllowed() {
		if(!this.isValidServerConn()) {
			this.doInvalidMessageI("received LOCK_ALLOWED from an unauthenticated machine.");
			return;
		}
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		long timestamp = (long) data.get("timestamp");

		//increase the lock_allowed count of the user
		String op_res = Server.user_list.incLockCount(username, secret, timestamp);
		
		//transfer the lock allowed to other nodes
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", "LOCK_ALLOWED");
		map.put("username", username);
		map.put("secret", secret);
		this.setSendTarget("SERVERS", map);
		map.put("timestamp", data.get("timestamp"));
		result_maps.add(map);
		
		//received enough lock_allowed messages, then send register success message
		if(op_res.equals("REGISTER_SUCCESS")) {
			Map<String, Object> reg_req = Server.user_list.getRegisterRequestInfo(username);
			// if the client sending request is connected to current machine
			if(reg_req != null) {
				Map<String, Object> succ_map = new HashMap<String, Object>();
				succ_map.put("target", "TARGET");
				succ_map.put("timestamp", timestamp);
				succ_map.put("address", reg_req.get("ip"));
				succ_map.put("port", reg_req.get("port"));
				succ_map.put("command", "REGISTER_SUCCESS");
				result_maps.add(succ_map);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doActivityMessage() {
		updateClientTimeStamp();

		Map<String, Object> map = new HashMap<String, Object>();
		String username = (String) data.get("username");
		String secret = (String) data.get("secret");
		if(username == null || username.equals("")) username = "anonymous";
		Map<String, Object> activity = null;
		try{
			activity = (Map<String, Object>) data.get("activity");
			//not json format
			if(activity == null) {
				this.doInvalidMessageI("Activity message should be in json format.");
				return;
			}
			else {
				activity.put("authenticated_user", username);
			}
		}
		catch(Exception e) {
			this.doInvalidMessageI("Activity message should be in json format.");
			return;
		}
		String user_status = Server.user_list.checkStatus(username, secret);
		if(user_status.equals("AUTHENTICATION_FAIL")) {
			setSendTarget("RESOURCE", map);
			map.put("command", "AUTHENTICATION_FAIL");
			map.put("info", "The user info is either incorrect or not logged in");
			result_maps.add(map);
			return;
		}
		else if(user_status.equals("INVALID_USER")) {
			setSendTarget("RESOURCE", map);
			map.put("command", "AUTHENTICATION_FAIL");
			map.put("info", "The user info is either incorrect or not logged in");
			result_maps.add(map);
			return;
		}
		setSendTarget("ALL", map);
		map.put("timestamp", data.get("timestamp"));
		map.put("command", "ACTIVITY_BROADCAST");
		map.put("activity", activity);
		result_maps.add(map);
	}
	
	private void doActivityBroadcast() {
		Map<String, Object> map = new HashMap<String, Object>();
		@SuppressWarnings("unchecked")
		Map<String, Object> activity = (Map<String, Object>) data.get("activity");
		setSendTarget("ALL", map);
		map.put("timestamp", data.get("timestamp"));
		map.put("command", "ACTIVITY_BROADCAST");
		map.put("activity", activity);
		result_maps.add(map);
	}
	
	private void doServerAnnounce() {
		doUpdateTimeStamp();
		String secret = (String) data.get("id");
		int load = (int) data.get("load");
		String hostname = (String) data.get("hostname");
		int port = (int) data.get("port");
		
		
		Map<String, Object> map = new HashMap<String, Object>(data);
		List<ServerRecord> serverlist = Server.servernodes.getGlobalServerList();
		for(ServerRecord server : serverlist) {
			if(server.equal( (String) data.get("id"))) {
				//secret not correct, INVALID_MESSAGE
				if(!server.getSecret().equals(secret)) {
					this.doInvalidMessageI();
					return;
				}
				server.setLoad(load);
				this.setSendTarget("SERVERS", map);
				map.put("timestamp", data.get("timestamp"));
				this.result_maps.add(map);
				return;
			}
		}
		ServerRecord server = new ServerRecord(hostname, port, load, secret);
		Date newDate = new Date();
		server.setTimeStamp(new Date());
		serverlist.add(server);
		this.setSendTarget("SERVERS", map);
		map.put("timestamp", data.get("timestamp"));
		this.result_maps.add(map);
	}
	
	private void doUpdateUserList() {
		@SuppressWarnings("unchecked")
		List<Map<String, String>> userSyncList = (List<Map<String, String>>) data.get("users");
		for(Map<String, String> user: userSyncList) {
			if(Server.user_list.userExists((String) user.get("username"))) {
				String name = user.get("username");
				String secret = user.get("secret");
				String timestamp = user.get("timestamp");
				String lockcnt = user.get("lockcount");
				String status = user.get("status");
				Server.user_list.updateUserInfo(name, secret, timestamp, lockcnt, status);
			}
			else {
				Server.user_list.add(user);
			}
		}
		Logger.info(JSONHandler.Map2Json(data));
	}
	/**
	 * Update TimeStamp for each server when receiving the heartbeat from it
	 */
	private void doUpdateTimeStamp() {
		Date currentTime = new Date();
		String hostname = (String) data.get("hostname");
		int port = (int) data.get("port");
		for(ServerRecord server : Server.servernodes.getGlobalServerList()) {
			if(server.equal((String) data.get("id"))) {
				server.setTimeStamp(currentTime);
			}
		}
	}
	
	private void doClientACK() {
		Date receivedTime = (Date) data.get("timestamp");
	}
	
	
	public void testOffline() {
		Server.serverPause = true;
		Map<String, Object> map = new HashMap<String, Object>(data);
		this.setSendTarget("NOTHING", map);
		this.result_maps.add(map);
	}
	
	public List<Map<String, Object>> run() {
		this.result_maps = new ArrayList<Map<String, Object>>();
		if(this.data == null) {
			this.doInvalidMessageJ();
			return this.result_maps;
		}
		if(data.get("timestamp") == null) data.put("timestamp", this.getTimeStamp());
		String command = (String) data.get("command");
		if(command == null || command.equals("")) {
			this.doInvalidMessageI();
			return this.result_maps;
		}
		switch(command) {
		case "AUTHENTICATE":
			this.doAuthenticate();
			break;
			
		case "LOGIN":
			this.doLogin();
			break;
			
		case "LOGOUT":
			this.doLogout();
			break;
			
		case "ACTIVITY_MESSAGE":
			this.doActivityMessage();
			break;
			
		case "ACTIVITY_BROADCAST":
			this.doActivityBroadcast();
			break;
			
		case "REGISTER":
			this.doRegister();
			break;
			
		case "LOCK_REQUEST":
			this.doLock();
			break;
			
		case "LOCK_ALLOWED":
			this.doLockAllowed();
			break;
		
		case "LOCK_DENIED":
			this.doLockDeny();
			break;
			
		case "SERVER_ANNOUNCE":
			this.doServerAnnounce();
			break;
			
		case "HEART_BEATS":
			this.doUpdateTimeStamp();
			break;
			
		case "REDIRECT":
			//do nothing.
			break;
			
		case "USER_SYNC":
			this.doUpdateUserList();
			break;
		
		case "OFFLINE":
			testOffline();
			break;
		
		case "CLIENT_ACK":
			this.doClientACK();
			break;
			
		default:
			this.doInvalidMessageI();
			break;
		}
		return this.result_maps;
	}

	/*Checks if current connection is a valid connection with a server*/
	private boolean isValidServerConn() {
		Map<String, Object> connInfo = connection.getConnectionInfo();
		return Server.servernodes.isValidServer((String) connInfo.get("address"), (int)connInfo.get("port"));
	}
	
	private long getTimeStamp() {
		return new Date().getTime();
	}
}
