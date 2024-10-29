package network.torrent.tracker.torrentexception;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class ExceptionHandler {
    private static final String fileName = "./exception.log"; 

    protected ExceptionHandler() {}
    
    public static void exceptionLog(String error) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
            writer.write(error);
            writer.newLine();
            writer.close();            
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
