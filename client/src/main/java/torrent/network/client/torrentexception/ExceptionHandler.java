package torrent.network.client.torrentexception;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class ExceptionHandler {
    private static final String filePath = "./exception.log";

    public static void handleException(Exception e) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.write(e.toString());
            writer.newLine();
            writer.close();
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
