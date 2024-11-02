package torrent.network.client.connectionmanager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import torrent.network.client.peerconnection.leecher.LeecherSocketController;
import torrent.network.client.trackerconnection.PeerEntity;
import torrent.network.client.trackerconnection.TrackerConnection;

public class ConnectionManager {
    public static Map<String, TrackerConnection> trackerConnectionMap = new ConcurrentHashMap<>();
    public static Map<String, LeecherSocketController> leecherSocketControllerMap = new ConcurrentHashMap<>();

    public static void createLeecherSocket(byte[] infoHash, String peerId, String ip, int port) throws Exception {
        PeerEntity peer = new PeerEntity(peerId, port, ip);
        LeecherSocketController leecherSocketController = leecherSocketControllerMap.get(new String(infoHash));
        leecherSocketController.createLeecherSocket(peer);
    }

    public static void createTrackerConnection(String magnetText, String tracker_url, int port) throws Exception {
        TrackerConnection trackerConnection = new TrackerConnection(magnetText, tracker_url, port);
        trackerConnectionMap.put(new String(trackerConnection.getInfoHash()), trackerConnection);

        LeecherSocketController leecherSocketController = new LeecherSocketController(trackerConnection.getInfo(),
                trackerConnection.getInfoHash(),
                trackerConnection.getNumberOfPieces(),
                trackerConnection.getPeers());

        leecherSocketControllerMap.put(new String(trackerConnection.getInfoHash()), leecherSocketController);
    }
}
