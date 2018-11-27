package utils;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Message {
    String text;
    long time;
    Date timeStamp;


    public Message (String msg, Date timeStamp){
        this.text = msg;
        this.timeStamp = timeStamp;
        this.time = this.timeStamp.getTime();
    }

    public Message (String msg, long time){
        this.text = msg;
        this.time = time;

        SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd " +
                "HH:mm:ss" );
        String d = format.format(time);

        try {
            timeStamp = format.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String read(){
        return text;
    }



    public String dateToTime(Date date){
        return date.toString();
    }
    public Date timeToDate(String time) {
        try {
            timeStamp = DateFormat.getDateInstance().parse(time);
            return timeStamp;
        } catch (ParseException e) {
            System.err.println("trans form failed");
            e.printStackTrace();
        }
        return null;
    }

    public boolean after(Message msg){
        if(this.timeStamp.after(msg.timeStamp)){
            return true;
        }
        return false;
    }
    public boolean before(Message msg){
        if(this.timeStamp.before(msg.timeStamp)){
            return true;
        }
        return false;
    }

    public String getDate(){
        return timeStamp.toString();
    }
    public String getTime(){
        return Long.toString(time);
    }
}
