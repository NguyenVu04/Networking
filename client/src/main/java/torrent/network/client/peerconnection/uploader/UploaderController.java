package torrent.network.client.peerconnection.uploader;

import org.springframework.web.bind.annotation.RestController;

import torrent.network.client.connectionmanager.ConnectionManager;
import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
// import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.TrackerConnection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
public class UploaderController {
    @PostMapping(path = "/handshake")
    public ResponseEntity<Object> handshakeController(@RequestBody byte[] body) {
        if (!PeerMessage.getMessageType(body).equals(MessageType.HANDSHAKE)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] infoHash = PeerMessage.getInfoHash(body);
            byte[] peerId = PeerMessage.getPeerId(body);

            TrackerConnection connection = ConnectionManager.getTrackerConnection(infoHash);

            if (connection == null || !connection.peerInSwarm(infoHash, peerId)) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(
                SingleFileApp.getBitField(
                    TrackerConnection.getEncodedInfoHash(infoHash)));
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/request")
    public ResponseEntity<Object> requestController(@RequestBody byte[] body) {
        if (PeerMessage.getMessageType(body) != MessageType.REQUEST) {
            return ResponseEntity.badRequest().build();
        }

        try {
            int index = PeerMessage.getRequestIndex(body);

            byte[] infoHash = PeerMessage.getInfoHash(body);
            byte[] peerId = PeerMessage.getPeerId(body);

            TrackerConnection connection = ConnectionManager.getTrackerConnection(infoHash);

            if (connection == null || !connection.peerInSwarm(infoHash, peerId)) {
                return ResponseEntity.badRequest().build();
            }
            
            byte[] piece = SingleFileApp.getPiece(
                TrackerConnection.getEncodedInfoHash(infoHash), 
                index, 
                connection.getPieces());

            connection.increaseUploaded();

            return ResponseEntity.ok(piece);

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return ResponseEntity.badRequest().build();
    }

}
