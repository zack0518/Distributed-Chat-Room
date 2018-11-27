package utils;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ArgumentsHandler {
	@Option(name = "-h", usage = "host address")
	private String laddress = null;
	
	@Option(name = "-p",usage = "local port")
    private int port = 3877;
	
	@Option(name = "-t",usage = "target server address")
    private String taddress = null;
	
	@Option(name = "-tp",usage = "target server port")
    private int tport = -1;
	
	@Option(name = "-s", usage = "secret")
	private String secret = null;
	
	public ArgumentsHandler(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
		}
		catch(CmdLineException e) {
			this.doParseError();
		}
	}
	
	public Map<String, Object> getArgs(){
		if(taddress != null && tport == -1) this.doParseError();
		if(tport != -1 && taddress == null) this.doParseError();
		Map<String, Object> resultmap = new HashMap<String, Object>();
		resultmap.put("laddress", laddress);
		resultmap.put("port", port);
		resultmap.put("taddress", taddress);
		resultmap.put("tport", tport);
		resultmap.put("secret", secret);
		return resultmap;
	}

	private void PrintUsage() {
		System.out.println("Usage: \n" + 
			"java -jar ActivityStreamerServer.jar (-h <local address>) (-p <local port>) (-t <target server address>) (-tp <target server port) (-s <secret>) ");
		System.out.println("Note: <-t> and <-tp> should be used, or not used at the same time.");
	}
	
	private void doParseError() {
		Logger.error("Invalid arguments, see usages below: ");
		this.PrintUsage();
		System.exit(0);
	}
}
