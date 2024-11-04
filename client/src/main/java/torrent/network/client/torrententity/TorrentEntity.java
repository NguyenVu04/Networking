package torrent.network.client.torrententity;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

import java.security.MessageDigest;
import java.util.Map;
public class TorrentEntity {
    private Map<String, Object> info;
    private String announce;

    protected TorrentEntity() {}

    public byte[] getInfo() {
        Bencode bencode = new Bencode();

        return bencode.encode(info);
    }

    public static byte[] getInfoHash(byte[] info) {
        try {
            MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);

            return md.digest(info);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public SingleFileInfo getSingleFileInfo() {
        Bencode bencode = new Bencode();

        Map<String, Object> map = bencode.decode(this.getInfo(), Type.DICTIONARY);

        return SingleFileInfo.from(map);
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
