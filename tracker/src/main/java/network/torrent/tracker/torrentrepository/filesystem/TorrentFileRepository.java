package network.torrent.tracker.torrentrepository.filesystem;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Repository;

import network.torrent.tracker.torrentexception.ExceptionHandler;

import java.io.BufferedInputStream;

@Repository
public class TorrentFileRepository {
    @Autowired
    private GridFsTemplate gridFsTemplate;

    public String saveTorrentFile(byte[] data) {
        try {
            HexFormat hexFormat = HexFormat.of();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            String filename = hexFormat.formatHex(digest.digest(data));

            GridFsResource resource = gridFsTemplate.getResource(filename);
            if (resource.exists())
                return filename;

            gridFsTemplate.store(new ByteArrayInputStream(data), filename, "text/plain");

            return filename;

        } catch (Exception e) {
            ExceptionHandler.exceptionLog(e.toString());
        }

        return null;
    }

    public byte[] getTorrentFile(String magnetText) {
        try {
            GridFsResource[] resources = gridFsTemplate.getResources(magnetText);

            if (resources.length == 0)
                return null;

            BufferedInputStream bs = new BufferedInputStream(resources[0].getInputStream());

            return bs.readAllBytes();

        } catch (Exception e) {
            ExceptionHandler.exceptionLog(e.toString());
        }

        return null;
    }

    public boolean deleteTorrentFile(String magnetText) {//! NEED IT SOMEDAY
        try {
            gridFsTemplate.delete(new Query(
                    Criteria.where("filename")
                            .is(magnetText)));//! ARE YOU SURE ABOUT THIS?
            return true;

        } catch (Exception e) {
            ExceptionHandler.exceptionLog(e.toString());
        }

        return false;
    }
}
