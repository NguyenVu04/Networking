package torrent.network.client.trackerconnection;

import java.util.Base64;
import java.util.HexFormat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentexception.ExceptionHandler;

public class PeerEntity {
    private String peer_id;
    private int port;
    private String ip;

    public PeerEntity(String peerId, int port, String ip) {
        this.peer_id = peerId;
        this.port = port;
        this.ip = ip;
    }
    public String getPeerId() {
        return peer_id;
    }

    public byte[] getDecodedPeerId() {
        return HexFormat.of().parseHex(this.peer_id);//TODO:
    }
    public int getPort() {
        return port;
    }
    public String getIp() {
        return ip;
    }

    public static PeerEntity from(Object peer) {
        try {
            Gson gson = new Gson();
            JsonElement jsonElement = gson.toJsonTree(peer);
            return gson.fromJson(jsonElement, PeerEntity.class);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }
}
