package torrent.network.client.connectionmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import torrent.network.client.trackerconnection.TrackerConnection;

public class ConnectionManager {
    public static Map<String, TrackerConnection> trackerMap = new ConcurrentHashMap<>();

    public static TrackerConnection createTrackerConnetion(
            String magnetText,
            String trackerUrl,
            int port,
            String path) throws Exception {

        TrackerConnection trackerConnection = new TrackerConnection(magnetText, trackerUrl, port, path);
        trackerMap.put(TrackerConnection.getEncodedInfoHash(trackerConnection.getInfoHash()), trackerConnection);
        return trackerConnection;
    }

    public static TrackerConnection createTrackerConnetion(
            int port,
            String path) throws Exception {

        TrackerConnection trackerConnection = new TrackerConnection(port, path);
        trackerMap.put(TrackerConnection.getEncodedInfoHash(trackerConnection.getInfoHash()), trackerConnection);
        return trackerConnection;
    }

    public static TrackerConnection getTrackerConnection(byte[] infoHash) {
        return trackerMap.get(TrackerConnection.getEncodedInfoHash(infoHash));
    }
}
