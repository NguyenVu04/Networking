package torrent.network.client.connectionmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import torrent.network.client.trackerconnection.TrackerConnection;

public class ConnectionManager {
    public static Map<String, TrackerConnection> trackerMap = new ConcurrentHashMap<>();
    public static Map<String, String> magnetMap = new ConcurrentHashMap<>();

    public static TrackerConnection createTrackerConnetion(
            String magnetText,
            String trackerUrl,
            int port,
            String path) throws Exception {

        TrackerConnection trackerConnection = new TrackerConnection(magnetText, trackerUrl, port, path);
        trackerMap.put(TrackerConnection.getEncodedInfoHash(trackerConnection.getInfoHash()), trackerConnection);
        magnetMap.put(magnetText, TrackerConnection.getEncodedInfoHash(trackerConnection.getInfoHash()));

        return trackerConnection;
    }

    public static TrackerConnection getTrackerConnection(byte[] infoHash) {
        return trackerMap.get(TrackerConnection.getEncodedInfoHash(infoHash));
    }

    public static TrackerConnection getTrackerConnection(String encodedInfoHash) {
        return trackerMap.get(encodedInfoHash);
    }

    public static String getInfoHash(String magnetText) {
        return magnetMap.get(magnetText);
    }
}
