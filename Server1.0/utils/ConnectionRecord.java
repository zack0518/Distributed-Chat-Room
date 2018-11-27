package utils;


import comm.Connection;

import java.util.Map;

public class ConnectionRecord {
    String address;
    int port;

    public ConnectionRecord(String address, int port){
        this.address = address;
        this.port = port;
    }

    public boolean equals(String C_address, int C_port){
        if(C_address.equals(address) && C_port == port){
            return true;
        }
        return false;
    }

    @Override
    public boolean equals (Object obj){
        if(obj == null && this != null){
            return false;
        }
        if(obj instanceof ConnectionRecord){
            return equals((ConnectionRecord)obj);
        }
        else if (obj instanceof Connection){
            Map<String, Object> map = ((Connection) obj).getConnectionInfo();
            if(map.get("address") == this.address && (long)(map.get("port")) ==
                    this.port){
                return true;
            }
        }
        return false;
    }

    public String toString() {
    	return this.address + ":" + this.port;
    }


    private boolean equals(ConnectionRecord connection) {
        if(connection.address == this.address && connection.port == this.port){
            return true;
        }
        return false;
    }


}
