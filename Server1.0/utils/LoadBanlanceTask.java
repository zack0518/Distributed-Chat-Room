package utils;

import java.util.concurrent.Callable;

import comm.ClientNode;
import comm.Connection;
import comm.ServerRecord;
import main.Server;

public class LoadBanlanceTask implements Callable<String>{

	@Override
	public String call() throws Exception {
		while(true) {
			String currID = Server.server_id;
			int currLoad = Server.servernodes.getLoad();
			boolean throwFlag = true;
			//if current node has largest load, load 2 bigger than smallest, has biggest hash string, 
			//	then throw one client to the least loaded server
			for(ServerRecord record : Server.servernodes.getGlobalServerList()) {
				if(record.getLoad() > currLoad) {
					throwFlag = false;
					break;
				}
				if(record.getLoad() == currLoad && compareHashString(currID, record.getSecret()) > 0) {
					throwFlag = false;
					break;
				}
			}
			Logger.error("Load: "+ currLoad);
			ServerRecord minLoadServer = this.leastLoadServer();
			if(minLoadServer != null && throwFlag) {
				int minload = minLoadServer.getLoad();
				Logger.error(currLoad + ", " + minload);
				if(currLoad - minload >= 2) {
					ClientNode client = Server.servernodes.getClients().get(0);
					Connection connection = Server.findConnection(client.getAddress(), client.getPort());
					connection.doRedirect(minLoadServer);
					Logger.info("Dynamic balancing: Thrown one client " + client.toString() + "to " + minLoadServer.toString());
				}
			}
			Thread.sleep(3500);
		}
	}
	
	private int compareHashString(String hash1, String hash2) {
		return hash1.compareTo(hash2);
	}
	
	private ServerRecord leastLoadServer() {
		int min_load = Integer.MAX_VALUE;
		ServerRecord result = null;
		for(ServerRecord record : Server.servernodes.getGlobalServerList()) {
			if(record.getLoad() < min_load) {
				min_load = record.getLoad();
				result = record;
			}
		}
		return result;
	}

}
