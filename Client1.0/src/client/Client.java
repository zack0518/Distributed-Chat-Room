package client;
import java.io.*;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.google.gson.JsonSyntaxException;


/**
 * TCP Client module
 * Communicate with server by JSON commands
 * The following received commands will result the connection closed:
 * There are two modes of the client - command line Client and Client with GUI
 * To run GUI version, simply do not enter any arguments
 * To run the command line version, set the first arguments ip address followed by port
 * @author ziang chen
 * email: ziang.chen@student.unimelb.edu.au
 * Version: 1.0.0
 */

public class Client extends Thread{
	
	
	private DataInputStream input;
	private DataOutputStream output;
	private Socket socket;
	private ClientGUI clientGUI;
	private OutputStreamWriter writer;
	private static Listener newListener;
	private volatile BufferedReader reader;
	static volatile boolean isConnected;
	private String currentUsername,currentSecret;
	
	Client(){
		isConnected = false;
	}
	public void connectGUI(ClientGUI clientGUI) {
		this.clientGUI = clientGUI;
	}
	/**
	 * Receive parsed Json ojbect
	 * Send Json object in UTF-8
	 * Check if a command contains "LOGOUT", disconnect if there is,
	 * @param json
	 * @return
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws ParseException 
	 */
	public void sendJsonObject(JSONObject json,String s) throws UnknownHostException, IOException {
			if(json != null) {
				writer.write(json.toJSONString()+"\n");
				try {
					clientActionByCommand(json.toJSONString());
				} catch (ParseException e) {
					System.err.println("(ERROR) :" + e.getMessage());
				}
				writer.flush();
				if(json.get("command").equals("LOGOUT")) {
					disconnect();
					if(clientGUI!=null) {
						clientGUI.disconnectionWindow();
					}else {
						System.out.println("Disconnected!");
					}
				}
			}else {
				writer.write(s + "\n");
				writer.flush();
			}
	}
	/**
	 * Close I/O
	 * Disconnect the connection by closing the socket
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
			isConnected = false;
			writer.close();
			reader.close();
			socket.close();
	}
	/**
	 * build connection via socket with user inputs
	 * @param ip
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void connect(String ip, int port) {
			Socket socket = null;
			try {
				socket = new Socket(ip, port);
				isConnected = true;
			} catch (UnknownHostException e) {
				System.err.println("(ERROR) :" + "Unknown Host");
			} catch (IOException e) {
				System.err.println("(ERROR) :" + "Invalid IP and Port");
			}
			this.socket = socket;
			try {
				writer = new OutputStreamWriter(socket.getOutputStream(),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				System.err.println("(ERROR) :" + "Writer Unsupported Encoding Exception");
			} catch (IOException e) {
				System.err.println("(ERROR) :" + "Writer IO error");
			}
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),"UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.err.println("(ERROR) :" + "Reader Unsupported Encoding Exception");
			} catch (IOException e) {
				System.err.println("(ERROR) :" + "Reader IO error");
			}
	}
	
	public void autoLogin() {
		JSONObject login = new JSONObject();
		login.put("command", "LOGIN");
		login.put("username", currentUsername);
		login.put("secret", currentSecret);
		try {
			sendJsonObject(login,null);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientGUI.setSentDisplay((String) login.toJSONString());
	}
	/**
	 * Parser the user input string to Json object
	 * @param Receivedstring
	 * @return
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * @throws ParseException
	 */
	public JSONObject jsonParser(String Receivedstring) throws UnknownHostException, IOException, ParseException  {
			JSONParser parser = new JSONParser();
			JSONObject json = (JSONObject) parser.parse(Receivedstring);
			return  json;
	}
	/**
	 * Client listener to the port 
	 * Receive message from server
	 * The wait loop is optimized by Thread.sleep(500) to avoid increasing CPU load
	 */
	private class Listener extends Thread{
		public void run() {
			while (true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						System.err.println("(ERROR) : Listener interrupted");
					}
					try {
						if(isConnected && reader!=null) {
							String received = reader.readLine();
								if(clientGUI != null) {
									clientGUI.setRecievedText(received);
								}
								if(clientGUI == null) {
									System.out.println("(RECEIVED): "+received);
								}
								clientActionByCommand(received);
						}
					} catch (IOException e) {
						
					} 
					catch (ParseException e) {
						System.err.println("(ERROR) : Parsing error" );
					}
			}
		}
	}
	
	/**
	 * Client will make actions with received command
	 * The actions include disconnect from server, redirect to other server
	 * @param cmd
	 * @throws ParseException
	 * @throws IOException 
	 */
	public void clientActionByCommand(String cmd) throws ParseException, IOException {
		JSONObject parseCmd = jsonParser(cmd);
		/**
		 * commands to disconnect the connection
		 */
		String cmdType = parseCmd.get("command").toString();
		if(cmdType.equals("AUTHENTICATION_FAIL") || cmdType.equals("INVALID_MESSAGE") ||
	       cmdType.equals("REGISTER_FAILED") || cmdType.equals("LOGIN_FAILED")) {
    			isConnected = false;	
		    	if(clientGUI == null) {
					System.out.println("Connection Closed!");
					disconnect();
					System.exit(0);
		    		}
				if(clientGUI != null) {
					disconnect();
					clientGUI.disconnectionWindow();
				}
		}
		if(cmdType.equals("LOGIN")) {
			currentUsername = (String) parseCmd.get("username");
			currentSecret = (String) parseCmd.get("secret");
		}
		
		/**
		 * Redirect to other server
		 */
		if(cmdType.equals("REDIRECT")) {
			String ip = parseCmd.get("hostname").toString();
			int port = Integer.parseInt(parseCmd.get("port").toString());
			disconnect();
			/**
			 *  reconnect to the new server with provided port and ip
			 */
			connect(ip, port);
			
			autoLogin();
			if(clientGUI!=null) {
				clientGUI.redirectWindow(ip, port);
			}
		}
		
	}
	public String getUsername() {
		return currentUsername;
	}
	public String getSecret() {
		return currentSecret;
	}
	
	public void setUsername(String s) {
		currentUsername = s;
	}
	
	public void setSecret(String s) {
		currentSecret = s;
	}
	/**
	 * @param args
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException {
		Client newClient = new Client();
		newListener = newClient.new Listener();
		newListener.start();
		
		ArgumentsHandler arg_hdl = new ArgumentsHandler(args);
		Map<String, Object> parsedArgs = arg_hdl.getArgs();
		
		if((int) parsedArgs.get("GUI") == 1) {
		final ClientGUI clientGUI = new ClientGUI(newClient);
		newClient.connectGUI(clientGUI);
		java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            		clientGUI.setVisible(true);
            }
        });
		

		newClient.connect((String) parsedArgs.get("thost"),(int) parsedArgs.get("tport"));
		newClient.setUsername((String) parsedArgs.get("username"));
		newClient.setSecret((String) parsedArgs.get("secret"));
		newClient.autoLogin();
		
		clientGUI.connectionSuccessWindow();
		
		clientGUI.setDefaultIP((String) parsedArgs.get("thost"), Integer.toString( (int) parsedArgs.get("tport")));
		clientGUI.setDefaultUser(newClient.currentUsername ,newClient.currentSecret);
		
		}else if((int) parsedArgs.get("GUI") == 0){
			
			newClient.connect((String) parsedArgs.get("thost"),(int) parsedArgs.get("tport"));
			System.out.println("Connected to : "+parsedArgs.get("thost")+", "+parsedArgs.get("tport"));
			System.out.println("(Enter Command)");
			
			while(true) {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				String cmd = br.readLine();
				try {
					JSONObject jsonCmd = newClient.jsonParser(cmd);
					newClient.sendJsonObject(newClient.jsonParser(cmd),cmd);
				} catch (ParseException e) {
					newClient.sendJsonObject(null,cmd);
				}
			}
		}
	}
}
