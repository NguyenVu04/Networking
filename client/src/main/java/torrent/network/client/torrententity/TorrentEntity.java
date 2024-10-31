package torrent.network.client.torrententity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

import java.io.ObjectOutputStream;
import java.security.MessageDigest;

import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
public class TorrentEntity {
    private Object info;
    private String announce;

    protected TorrentEntity() {}

    public Object getInfo() {
        return info;
    }

    public byte[] getInfoHash() {
        try {
            MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(info);
            oos.flush(); 

            byte[] infoBytes = bos.toByteArray();
            oos.close();
            bos.close();//! MAYBE THIS IS WRONG

            md.update(infoBytes);

            return md.digest();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public SingleFileInfo getSingleFileInfo() {
        return SingleFileInfo.from(info);
    }

    public MultiFileInfo getMultiFileInfo() {
        return MultiFileInfo.from(info);
    }


    public String getAnnounce() {
        return announce;
    }    

    public static TorrentEntity from(Object torrent) {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(torrent);
            return gson.fromJson(jsonElement, TorrentEntity.class); 

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }
}
