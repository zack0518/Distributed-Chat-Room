package client;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.LineBorder;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *The class implements a simple GUI to allow user interact with Client module
 *The GUI includes:
 *Display the editing message, sent message and received message. 
 *Receive IP and port inputs
 *Pass user input to Client model
 * @author ziangChen
 */
public class ClientGUI extends javax.swing.JFrame {

    /**
     * Variables which need to be initialized in constructor
     */
	Client currentClient;
	String ReceivedDisplay;
    String displaySent;
	
    public ClientGUI(Client newClient) {
    		currentClient = newClient;
    		displaySent = "";
    		ReceivedDisplay = "";
    		userSelect = "";
        initComponents();
        this.setLocationRelativeTo(null);
        setResizable(false);
    }

    @SuppressWarnings("unchecked")        
    
    /**
     * Layout of GUI
     */
    private void initComponents() {

        recivedMsg = new javax.swing.JScrollPane();
        displayRecivedMsg = new javax.swing.JTextPane();
        sentMsg = new javax.swing.JScrollPane();
        sendingMsg = new javax.swing.JScrollPane();
        displaySentMsg = new javax.swing.JTextPane();
        receviedLable = new java.awt.Label();
        sendingLable = new java.awt.Label();
        ipField = new javax.swing.JTextField();
        disconnect = new javax.swing.JButton();
        send = new javax.swing.JButton();
        ipIntro = new javax.swing.JLabel();
        portField = new javax.swing.JTextField();
        portIntro = new javax.swing.JLabel();
        connect = new javax.swing.JButton();
        sentLable = new java.awt.Label();
        selectCommand = new javax.swing.JComboBox<>();
        commandPanel = new javax.swing.JPanel();
        secretField = new javax.swing.JTextField();
        usernameField = new javax.swing.JTextField();
        usernameIntro = new javax.swing.JLabel(); 
        secretIntro = new  javax.swing.JLabel();
        activityIntro = new javax.swing.JLabel();
        activityField =  new javax.swing.JTextField();
        CustomIntro = new javax.swing.JLabel();
        CustomField = new javax.swing.JTextField();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(null);
        setPreferredSize(new Dimension(950,600));

        recivedMsg.setViewportView(displayRecivedMsg);

        getContentPane().add(recivedMsg);
        recivedMsg.setBounds(490, 100, 450, 470);

        receviedLable.setText("Received");
        getContentPane().add(receviedLable);
        receviedLable.setBounds(490, 70, 80, 30);
        sendingLable.setText("Sending");
        getContentPane().add(sendingLable);
        sendingLable.setBounds(10, 70, 80, 30);
        getContentPane().add(ipField);
        ipField.setBounds(30, 10, 160, 30);

        disconnect.setText("Disconnected");
        disconnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectActionPerformed(evt);
            }
        });
        getContentPane().add(disconnect);
        disconnect.setBounds(590, 10, 110, 29);

        send.setLabel("Send");
        send.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                try {
					sendActionPerformed(evt);
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });
        getContentPane().add(send);
        send.setBounds(320, 70, 140, 30);
        send.setEnabled(false);

        ipIntro.setText("IP");
        getContentPane().add(ipIntro);
        ipIntro.setBounds(10, 10, 50, 30);

        getContentPane().add(portField);
        portField.setBounds(300, 10, 160, 30);

        portIntro.setText("Port");
        getContentPane().add(portIntro);
        portIntro.setBounds(260, 10, 50, 30);

        connect.setText("Connect");
        getContentPane().add(connect);
        connect.setBounds(490, 10, 97, 29);

        sentMsg.setViewportView(displaySentMsg);

        getContentPane().add(sentMsg);
        sentMsg.setBounds(10, 350, 450, 220);

        sentLable.setText("Sent");
        getContentPane().add(sentLable);
        sentLable.setBounds(10, 330, 27, 16);
        
       
        selectCommand.addItem("Select Command");
        selectCommand.addItem("REGISTER");
        selectCommand.addItem("LOGIN");
        selectCommand.addItem("ACTIVITY_MESSAGE");
        selectCommand.addItem("LOGOUT");
        selectCommand.addItem("CUSTOM");
        
       
        selectCommand.setBounds(130, 70, 160, 30);
        getContentPane().add(selectCommand);
             
	  	commandPanel.add(usernameField);
        usernameField.setBounds(130, 40, 280, 30);
        
        commandPanel.add(usernameIntro);
        usernameIntro.setBounds(30, 40, 80, 30);
        
        commandPanel.add(secretIntro);
        secretIntro.setBounds(30, 80, 60, 30);
        CustomIntro.setBounds(30, 80, 100, 30);
        secretField.setBounds(130, 80, 280, 30);
        CustomField.setBounds(130, 80, 280, 30);
        commandPanel.add(CustomField);
        
        commandPanel.add(secretField);
        commandPanel.add(activityIntro);
        CustomIntro.setText("Command: ");
        commandPanel.add(CustomIntro);
        
        activityIntro.setBounds(30, 120, 70, 30);
        commandPanel.add(activityField);
        activityField.setBounds(130, 120, 280, 30);
        
        usernameIntro.setText("Username : ");
        secretIntro.setText("Secret : ");
        activityIntro.setText("Activity : ");
        initVisible();
		
        getContentPane().add(commandPanel);
        commandPanel.setBounds(20, 110, 440, 200);
        
        commandPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        commandPanel.setLayout(null);
        commandPanel.setVisible(true);
        disconnect.setEnabled(false);
        
        selectCommand.addActionListener(new ActionListener(){
    		public void actionPerformed(ActionEvent e){
                JComboBox combo = (JComboBox)e.getSource();
                String choice = combo.getSelectedItem().toString();
                userSelect = choice;
                parseCmdType(choice);
            }});
        
        connect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            		connectActionPerformed(evt);
            }
        });
        /**
         * Listening for exit
         */
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
            	    if(Client.isConnected == true) {
            	  	JSONObject logoutCmd = new JSONObject();
            	  	logoutCmd.put("command", "LOGOUT");
            	    try {
						currentClient.sendJsonObject(logoutCmd, null);
					} catch (IOException e) {
						
					}
            	    }
                setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            }
          });

        pack();
    }                        
    
    public void setDefaultIP(String host, String port) {
        ipField.setText(host);
        portField.setText(port);
    }
    
    public void setDefaultUser(String username, String secret) {
    		usernameField.setText(username);
    		secretField.setText(secret);
    }
    
    private void disconnectActionPerformed(java.awt.event.ActionEvent evt)  {     
  	  	  JSONObject logout = new JSONObject();
  	      logout.put("command", "LOGOUT");
  	      try {
			currentClient.sendJsonObject(logout, null);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	  disconnect();
    }    
    private void constructActivityMsg() throws UnknownHostException, IOException {
		  jsonCmd.put("username", usernameField.getText());
		  jsonCmd.put("secret", secretField.getText());
		  JSONObject activity;
		  try {
			  activity = new JSONObject();
			  activity = currentClient.jsonParser(activityField.getText());
			  jsonCmd.put("activity", activity);
		  }catch (ParseException e) {
			  jsonCmd.put("activity", activityField.getText());
		  }
    }
    
    private void sendActionPerformed(java.awt.event.ActionEvent evt) throws UnknownHostException, IOException { 
    		
    		if(userSelect.equals("Select Command") || userSelect == "") {
    		    return;
    		}
    		if(userSelect.equals("CUSTOM")) {
			try {
				jsonCmd = currentClient.jsonParser(CustomField.getText());
				if(jsonCmd.get("command").equals("ACTIVITY_MESSAGE)")) {
					constructActivityMsg();
				}
				currentClient.sendJsonObject(jsonCmd, null);
				displaySent += jsonCmd.toJSONString().replaceAll("\\\\", "")+"\n";
				displaySentMsg.setText(displaySent);
				
			} catch (ParseException e) {
				currentClient.sendJsonObject(null, CustomField.getText());
				displaySent += CustomField.getText()+"\n";
				displaySentMsg.setText(displaySent);
			}
			return;
    		}
    		switch (jsonCmd.get("command").toString()){
    			  case "LOGIN": case "REGISTER":
    				  jsonCmd.put("username", usernameField.getText());
    				  jsonCmd.put("secret", secretField.getText());
    				  break;
    			  case "ACTIVITY_MESSAGE":
    				  constructActivityMsg();
    				  break;
    		}
    		if(Client.isConnected) {
				if(jsonCmd != null) {
					currentClient.sendJsonObject(jsonCmd,null);
					displaySent += jsonCmd.toJSONString().replaceAll("\\\\", "")+"\n";
					displaySentMsg.setText(displaySent);
				}else {
					currentClient.sendJsonObject(null,null);
				}
    		}
    }   
    private void initVisible() {
        usernameField.setVisible(false);
        usernameIntro.setVisible(false);
        secretIntro.setVisible(false);
        secretField.setVisible(false);
        activityIntro.setVisible(false);
        activityField.setVisible(false);
        CustomField.setVisible(false);
        CustomIntro.setVisible(false);
    }
    
    public void parseCmdType(String cmdType) {
    		jsonCmd = new JSONObject();
    		
    		switch (cmdType) { 
    		   case "Select Command":
    			   initVisible();
    			   break;
    		   case "LOGIN": case "REGISTER":
    			   initVisible();
    		       usernameIntro.setVisible(true);
    			   usernameField.setVisible(true);
    		       secretIntro.setVisible(true);
    		       secretField.setVisible(true);
    			   break;
    		  case "ACTIVITY_MESSAGE":
    			   initVisible();
    			   usernameIntro.setVisible(true);
    			   usernameField.setVisible(true);
       		 	   secretIntro.setVisible(true);
       		 	   secretField.setVisible(true);
       		   	   activityIntro.setVisible(true);
       		   	   activityField.setVisible(true);
    		       jsonCmd.put("command", "ACTIVITY_MESSAGE");
    		       usernameField.setText(currentClient.getUsername());
    		       secretField.setText(currentClient.getSecret());
    		       break;
    		  case "LOGOUT":
    			  	initVisible();
    			    jsonCmd.put("command", "LOGOUT");
    			    break;
    		  case "CUSTOM":
    			  	initVisible();
    			  	CustomField.setVisible(true);
    			  	CustomIntro.setVisible(true);
    			  	break;
    		}
		if(cmdType.equals("LOGIN")) {
		    jsonCmd.put("command", "LOGIN");
		}else if(cmdType.equals("REGISTER")){
			jsonCmd.put("command", "REGISTER");
		}
    }
    
    public void setSentDisplay(String sent) {
    	if(sent!=null) {
    		displaySent += sent+"\n";
			displaySentMsg.setText(displaySent);
		}
    }
    public void setRecievedText(String received) {
    		if(received!=null) {
    			ReceivedDisplay += received +"\n";
    			displayRecivedMsg.setText(ReceivedDisplay);
    		}
    }
    @SuppressWarnings("deprecation")
	private void connectActionPerformed(java.awt.event.ActionEvent evt)   {                                          
        
    	try {	
    			if(!Client.isConnected) {
    				currentClient.connect(ipField.getText(),Integer.parseInt(portField.getText()));
    				connectionSuccessWindow();
    			}else {
    				 connectionAlreadyWindow();
    			}
		} catch (NumberFormatException e) {
			connectionFailureWindow();
			e.printStackTrace();
		}
    }   
    
    /**
     * Message to user
     * 
     */
    public void connectionSuccessWindow(){
    		JOptionPane optionPane = new JOptionPane("Conncetion Successful", JOptionPane.INFORMATION_MESSAGE);    
    		JDialog dialog = optionPane.createDialog("Connected");
    		dialog.setAlwaysOnTop(true);
    		dialog.setVisible(true);
    		connect.setEnabled(false);
    		disconnect.setEnabled(true);
    		send.setEnabled(true);
    }
    
    public void connectionFailureWindow(){
		JOptionPane optionPane = new JOptionPane("Connection Failed!", JOptionPane.OK_OPTION);    
		JDialog dialog = optionPane.createDialog("Failure");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
    }

    public void connectionAlreadyWindow() {
    		JOptionPane optionPane = new JOptionPane("Connection already existed, Disconnect first to make other connection", JOptionPane.OK_OPTION);    
		JDialog dialog = optionPane.createDialog("Connection Found");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
    }
    
    public void parseErrorWindow() {
    		JOptionPane optionPane = new JOptionPane("Invalid input, the command is in JSON format", JOptionPane.OK_OPTION);    
		JDialog dialog = optionPane.createDialog("Invaild input");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
    }
    public void disconnectionWindow(){
		JOptionPane optionPane = new JOptionPane("Disconnected!", JOptionPane.INFORMATION_MESSAGE);    
		JDialog dialog = optionPane.createDialog("Disconnected");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);	
		connect.setEnabled(true);
		disconnect.setEnabled(false);
		send.setEnabled(false);
    }

    
    public void redirectWindow(String ip, int port) {
    		JOptionPane optionPane = new JOptionPane("Conncetion has been redirected to "+ip+" "+port, JOptionPane.INFORMATION_MESSAGE);    
		JDialog dialog = optionPane.createDialog("Redirected");
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
		portField.setText(Integer.toString(port));
		ipField.setText(ip);
    }
    
    public void disconnect() {
    		if(Client.isConnected) {
    		try {
				currentClient.disconnect();
				disconnectionWindow();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		}
    }
    
    // Variables declaration - do not modify   
    private JSONObject jsonCmd;
    private javax.swing.JComboBox<String> selectCommand;
    private javax.swing.JButton connect;
    private javax.swing.JTextPane displayRecivedMsg;
    private javax.swing.JTextPane displaySentMsg;
    private javax.swing.JTextField ipField;
    private javax.swing.JLabel ipIntro;
    private javax.swing.JButton disconnect;
    private javax.swing.JButton send;
    private javax.swing.JTextField portField;
    private javax.swing.JLabel portIntro;
    private java.awt.Label receviedLable;
    private javax.swing.JScrollPane recivedMsg;
    private java.awt.Label sendingLable;
    private java.awt.Label sentLable;
    private javax.swing.JScrollPane sentMsg;
    private javax.swing.JScrollPane sendingMsg;
    private javax.swing.JPanel commandPanel;
    private javax.swing.JTextField secretField;
    private javax.swing.JTextField usernameField;
    private javax.swing.JTextField activityField;
    private javax.swing.JTextField CustomField;
    private javax.swing.JLabel usernameIntro;
    private javax.swing.JLabel secretIntro;
    private javax.swing.JLabel activityIntro;
    private javax.swing.JLabel CustomIntro;
    private String userSelect;
    // End of variables declaration                   
}
