package network.torrent.tracker.torrentrepository.filesystem;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;

import network.torrent.tracker.torrentexception.ExceptionHandler;

@Repository
public class TorrentFileRepository {
    @Autowired
    private GridFsTemplate gridFsTemplate;

    public boolean saveTorrentFile(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            String filename = new String(digest.digest(data));

            gridFsTemplate.store(new ByteArrayInputStream(data), filename, "text/plain");

            return true;

        } catch (Exception e) {
            ExceptionHandler.exceptionLog(e.toString());
        }

        return false;
    }
    
    public byte[] getTorrentFile(byte[] magnetText) {
        //TODO: implement
        return null;
    }
}
