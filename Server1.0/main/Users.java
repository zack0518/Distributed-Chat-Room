package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import utils.Logger;

/**
 * Functions:
 * 1.To maintain the user list, to add users, to change their status.
 * 
 * Usage:
 * Initiate:
 * Users users = new Users();
 * add(username, secret) -> used when REGISTERing
 * login(username, secret) -> used when LOGINing
 * logout(username, secret) -> used when LOGOUTing
 * */
public class Users {
	List<Map<String, String>> userlist = new ArrayList<Map<String, String>>();
	List<Map<String, Object>> register_req = new ArrayList<Map<String, Object>>();
	
	public String add(String name, String secret, long timestamp, int lockcount) {
		if(userExists(name)) return "EXISTED";
		Map<String, String> user = new HashMap<String, String>();
		user.put("name", name);
		user.put("secret", secret);
		user.put("timestamp", String.valueOf(timestamp));
		user.put("status", "locked");
		user.put("lockcount", String.valueOf(lockcount));
		//one server only, will never receive LOCK_ALLOWED
		if(Server.servernodes.getGlobalServerList().size() == 0) {
			user.put("status", "logout");
			user.put("lockcount", "-1");
		}
		userlist.add(user);
		return "SUCCESS";
	}
	
	public String add(Map<String, String> map) {
		if(userExists(map.get("name"))) return "EXISTED";
		userlist.add(map);
		return "SUCCESS";
	}
	
	public int getLockCount(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) return Integer.parseInt(user.get("lockcount"));
		}
		return -1;
	}
	
	/*update user info if target is set earlier than local storage*/
	public boolean updateUserInfo(String name, String secret, String timestamp, String lockcnt, String status) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				long currTimestamp = Long.valueOf(user.get("timestamp"));
				String curr_secret = user.get("secret");
				if(curr_secret.equals(secret)) return true;
				if(currTimestamp > Long.valueOf(timestamp)) {
					user.put("secret", secret);
					user.put("timestamp", timestamp);
					user.put("lockcount", lockcnt);
					user.put("status", status);
					return true;
				}
			}
		}	
		return false;
	}
	
	public String incLockCount(String name, String secret, long timestamp) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				//user exists, same secret, lock count += 1
				if(user.get("secret").equals(secret)) {
					int lockcount = Integer.parseInt(user.get("lockcount")) + 1;
					if(lockcount == Server.servernodes.getGlobalServerList().size()) {
						user.put("status", "logout");
						user.put("lockcount", "-1");
						this.showUserInfo(name);
						return "REGISTER_SUCCESS";
					}
					else {
						user.put("lockcount", String.valueOf(lockcount));
						this.showUserInfo(name);
						return "LOCK_COUNT_UPDATE";
					}
				}
				// user exists, secret different.
				// local older, dismiss; local newer, delete local.
				else {
					long local_timestamp = Long.valueOf(user.get("timestamp"));
					if (local_timestamp < timestamp) return "EARLIER_FOUND";
					this.deleteUser(name);
					break;
				}
			}
		}
		
		//user not exist, create it
		this.add(name, secret, timestamp, 1);
		return "LOCK_COUNT_INIT";
	}
	
	public boolean userExists(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) return true;
		}
		return false;
	}
	
	public boolean userHasDiffSecret(String name, String secret) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				if(!(user.get("secret").equals(secret)))return true;
			}
		}
		return false;
	}
	
	public boolean userRegistered(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				if(!(user.get("status").equals("locked")))return true;
			}
		}
		return false;
	}
	
	public boolean checkSameUser(String name, String secret){
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name) && user.get("secret").equals(secret)) return true;
		}
		return false;
	}
	
	public void DeleteUser(String name, String secret) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name) && user.get("secret").equals(secret)) userlist.remove(user);
		}
	}
	
	public String login(String name, String secret) {
		if(name.toLowerCase().equals("anonymous")) return "SUCCESS";
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				if(!user.get("secret").equals(secret)) return "AUTHENTICATION_FAIL";
				if(user.get("status").equals("login")) return "DUPLICATE_LOGIN";
				user.put("status", "login");
				return "SUCCESS";
			}
		}
		return "INVALID_USER";
	}
	
	public String logout(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				user.put("status", "logout");
				return "SUCCESS";
			}
		}
		return "INVALID_USER";
	}
	
	/*Check if a user is valid.*/
	public String checkStatus(String name, String secret) {
		if(name.equals("anonymous")) return "SUCCESS";
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				if(!user.get("secret").equals(secret)) return "AUTHENTICATION_FAIL";
				if(!user.get("status").equals("login")) return "AUTHENTICATION_FAIL";
				else return "SUCCESS";
			}
		}
		return "INVALID_USER";
	}
	
	/*Receives a register request from the client, bind the register request to the client.*/
	public void putRegisterRequestInfo(String name, String secret, String ip, int port) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("name", name);
		map.put("secret", secret);
		map.put("ip", ip);
		map.put("port", port);
		this.register_req.add(map);
	}
	
	/*Get the requesting client from a username*/
	public Map<String, Object> getRegisterRequestInfo(String name){
		for(Map<String, Object> request : register_req) {
			if(request.get("name").equals(name)) {
				return request;
			}
		}
		return null;
	}
	
	/*Show a user, for testing*/
	public void showUserInfo(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name))
				Logger.info(user.toString());
		}
	}

	/*Set status of a user.*/
	public void setStatus(String name, String status) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name))
				user.put("status", status);
		}	
	}
	
	/*Get status of a user*/
	public String getStatus(String name) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name))
				return user.get("status");
		}
		return "";
	}
	
	/*Pack the user list to server message sent through user synchronize message*/
	public Map<String, Object> pack() {
		List<Map<String, String>> list = new ArrayList<Map<String, String>>(userlist);
		for(Map<String, String> user : list) {
			if(user.get("status").equals("login")) user.put("status", "logout");
		}
		Map<String, Object> resultmap = new HashMap<String, Object>();
		resultmap.put("command", "USER_SYNC");
		resultmap.put("users", list);
		return resultmap;
	}
	
	/*delete a user from the list.*/
	public synchronized void deleteUser(String name) {
		Iterator<Map<String, String>> iterator = this.userlist.iterator();
		while(iterator.hasNext()){
			Map<String, String> user = iterator.next();
			if(user.get("name").equals(name)) iterator.remove();
		}
	}
	
	/*detects if local record is older*/
	public boolean hasOlderRecord(String name, long timestamp) {
		for(Map<String, String> user : userlist) {
			if(user.get("name").equals(name)) {
				if(Long.valueOf(user.get("timestamp")) < timestamp) return true;
			}
		}
		return false;
	}
}
