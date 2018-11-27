package utils;


import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import comm.Connection;
import main.Server;

/**
 * usage：when establish connection, add connectionrecord and in stream
 * to the Server,pipe use add(conR,in)
 * when read message use readMessage(connectionRecord conR) or readMessage
 * (String address, int port)
 * when remove a connection，Server.pipe.remove(conR)
 */

public class MessagePipeline {
    private HashMap<ConnectionRecord, BlockingQueue<Message>> messages;
    private ExecutorService pool;


    public MessagePipeline(){
        messages = new HashMap<>();
        pool = Executors.newCachedThreadPool();
    }

    public void add(ConnectionRecord con, BufferedReader in){
        if(messages.containsKey(con)){
            BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(100);
            queue.addAll(messages.get(con));
            messages.remove(con);
            bind(in, con,queue);
            messages.put(con,queue);
        }
        else {
            BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(100);
            bind(in, con, queue);
            messages.put(con, queue);
        }
    }

    public void remove(ConnectionRecord con){
//        if(messages.containsKey(localport)){
//            messages.remove(localport);
//        }
        messages.remove(con);
    }

    private void bind(BufferedReader in,ConnectionRecord con, BlockingQueue<Message> queue) {
        //            BufferedReader reader;
//            reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));

        class Task implements Runnable {
            @Override
            public void run() {
                try {
                    while (true) {
                        Message msg = new Message(in.readLine(),
                                System.currentTimeMillis());
                        queue.put(msg);
                    }
                } 
                catch (Exception e) {
                	Logger.error("A connection is detected droped, info: " + e.getMessage() + ", " + con.toString());
				} 
                finally {
                    try {
                    	Connection c = Server.findConnection(con.address, con.port);
                    	if(c != null) c.selfKill();
                        in.close();
                    } catch (IOException e) {
                        Logger.error("IO Exception: " + e.getMessage());
                    }
                }
            }
        }
        pool.submit(new Task());
    }

    //Note that if the return is null, that's means some error happens.
    public String readLine(ConnectionRecord con){
        try {
            return messages.get(con).take().read();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
    public String readLine(String C_address, int port){
        return readLine(new ConnectionRecord(C_address,port));
    }

    public Message readMessage(ConnectionRecord con){
        try {
            return messages.get(con).take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Message readMessage(String C_address, int port){
        return readMessage(new ConnectionRecord(C_address,port));
    }
}


