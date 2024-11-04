package torrent.network.client.peerconnection.leecher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import torrent.network.client.connectionmanager.ConnectionManager;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.peerconnection.uploader.SingleFileApp;
// import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.PeerEntity;
import torrent.network.client.trackerconnection.TrackerConnection;

public class LeecherController {
    private static final String handshakePath = "handshake";
    private static final String requestPath = "request";
    private static final String protocol = "http";

    public static void createLeecherController(
            byte[] pieces,
            byte[] infoHash,
            byte[] peerId,
            List<PeerEntity> peers,
            List<Integer> indexLeft) throws Exception {

        Map<Integer, List<PeerEntity>> peerMap = new HashMap<>();
        for (Integer index : indexLeft) {
            peerMap.put(index, new LinkedList<>());
        }

        List<Integer> requestQueue = new LinkedList<>();

        for (PeerEntity peer : peers) {
            byte[] message = PeerMessage.createHandshakeMessage(infoHash, peerId);

            RestClient client = RestClient
                    .create(LeecherController.protocol + "://" + peer.getIp() + ":" + peer.getPort());
            try {
                ResponseEntity<byte[]> response = client.post()
                        .uri(uriBuilder -> uriBuilder.path(LeecherController.handshakePath).build())
                        .body(message)
                        .retrieve().toEntity(byte[].class);

                byte[] bitfield = response.getBody();

                for (int i = 0; i < bitfield.length; i++) {
                    if (bitfield[i] == 1 && peerMap.containsKey(i)) {
                        peerMap.get(i).add(peer);
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
                continue;
            }
        }

        TreeMap<Integer, Integer> sortedMap = new TreeMap<>();
        for (Integer index : indexLeft) {
            sortedMap.put(peerMap.get(index).size(), index);
        }

        for (Map.Entry<Integer, Integer> entry : sortedMap.entrySet()) {
            requestQueue.add(entry.getValue());
        }

        while (!requestQueue.isEmpty()) {
            int index = requestQueue.removeFirst();
            byte[] message = PeerMessage.createRequestMessage(infoHash, peerId, index);
            while (!peerMap.get(index).isEmpty()) {
                PeerEntity peer = peerMap.get(index).removeFirst();

                RestClient client = RestClient
                        .create(LeecherController.protocol + "://" + peer.getIp() + ":" + peer.getPort());
                try {
                    ResponseEntity<byte[]> response = client.post()
                            .uri(uriBuilder -> uriBuilder.path(LeecherController.requestPath).build())
                            .body(message)
                            .retrieve().toEntity(byte[].class);

                    byte[] data = response.getBody();

                    // TorrentDigest td = new TorrentDigest(pieces);
                    // if (!td.verify(data, index)) {
                    //     System.out.println("Failed to verify piece " + index);
                    //     continue;
                    // }

                    SingleFileApp.savePiece(TrackerConnection.getEncodedInfoHash(infoHash), index, data);

                    ConnectionManager.getTrackerConnection(infoHash).increaseDownloaded();
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                    System.out.println("Failed to send request to " + peer.getIp() + ":" + peer.getPort());
                }
            }
        }
    }
}
