package client;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ArgumentsHandler {
	@Option(name = "-p",usage = "target server port")
    private int tport = -1;
	
	@Option(name = "-h",usage = "target server host")
    private String thost = null;
	
	@Option(name = "-u",usage = "username")
    private String username = "anonymous";
	
	@Option(name = "-s", usage = "secret")
	private String secret = "";
	
	@Option(name = "-g", usage = "GUI switch")
	private int guiSwitch = 1;
	
	public ArgumentsHandler(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		}
		catch(CmdLineException e) {
			
		}
	}
	
	public Map<String, Object> getArgs(){
		if(thost == null || tport == -1) this.doParseError();
		if(guiSwitch != 1 && guiSwitch != 0) this.doParseError();
		Map<String, Object> resultmap = new HashMap<String, Object>();
		resultmap.put("tport", tport);
		resultmap.put("thost", thost);
		resultmap.put("username", username);
		resultmap.put("secret", secret);
		resultmap.put("GUI", guiSwitch);
		return resultmap;
	}
	
	private void PrintUsage() {
		System.out.println("Usage: \n" + 
			"java -jar Client.jar (-p <target server port>) (-h <target server host) "
			+ "(-u <default user>) (-s <default secret>) "+("-g <GUI switch>"));
		System.out.println("Note: 0 and 1 are the only valid value to GUI switch");
	}
	private void doParseError() {
		System.err.println("Invalid usage, see usage below");
		this.PrintUsage();
		System.exit(0);
	}
}
