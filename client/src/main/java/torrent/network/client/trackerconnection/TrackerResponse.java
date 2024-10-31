package torrent.network.client.trackerconnection;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentexception.ExceptionHandler;

public class TrackerResponse {
    private int interval;
    private int complete;
    private int incomplete;
    private Object peers;
    private String tracker_id;

    protected TrackerResponse() {}
    
    public int getInterval() {
        return interval;
    }
    public int getComplete() {
        return complete;
    }
    public int getIncomplete() {
        return incomplete;
    }
    public List<PeerEntity> getPeers() {
        try {
            Gson gson = new Gson();
            JsonElement jsonElement = gson.toJsonTree(this.peers);
            Object[] array = gson.fromJson(jsonElement, Object[].class);

            List<PeerEntity> peers = new ArrayList<>();
            for (Object peer : array) {
                peers.add(PeerEntity.from(peer));
            }
            
            return peers;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }
    public String getTrackerId() {
        return tracker_id;
    }

    public static TrackerResponse from(Object response) {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(response);
            return gson.fromJson(jsonElement, TrackerResponse.class);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }   
}
