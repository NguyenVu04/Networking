package network.torrent.tracker.peercontroller;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dampcake.bencode.Bencode;

import network.torrent.tracker.torrentexception.ExceptionHandler;
import network.torrent.tracker.torrentrepository.session.SessionDocument;

@RestController
public class HttpController {

    /**
     * Interval between requests in seconds.
     */
    public static final int interval = 3;

    /**
     * {@link EventHandler} for handling events.
     */
    @Autowired
    private EventHandler eventHandler;

    /**
     * Handles a request to the tracker server.
     * 
     * @param infoHash
     *                   the info hash of the torrent.
     * @param peerId
     *                   the peer id of the peer.
     * @param port
     *                   the port of the peer.
     * @param downloaded
     *                   the number of bytes that the peer has downloaded from this
     *                   tracker.
     * @param left
     *                   the number of bytes that the peer is missing from this
     *                   tracker.
     * @param uploaded
     *                   the number of bytes that the peer has uploaded to this
     *                   tracker.
     * @param event
     *                   the event that this request is in response to.
     * @param compact
     *                   1 if the response should be in compact form, 0 if it should
     *                   be
     *                   in regular form.
     * @param noPeerId
     *                   1 if the response should not include the peer id, 0 if it
     *                   should.
     * @param trackerId
     *                   the tracker id of the tracker.
     * @param ip
     *                   the IP address of the peer.
     * @return a response to the request.
     */
    @CrossOrigin(origins = "*")
    @GetMapping("/")
    public ResponseEntity<Object> PeerController(
            @RequestParam(name = "info_hash") String infoHash,
            @RequestParam(name = "peer_id") String peerId,
            @RequestParam(name = "port") Integer port,
            @RequestParam(name = "downloaded") Integer downloaded,
            @RequestParam(name = "left") Integer left,
            @RequestParam(name = "uploaded") Integer uploaded,
            @RequestParam(name = "event") String event,
            @RequestParam(name = "compact") Integer compact,
            @RequestParam(name = "no_peer_id") Integer noPeerId,
            @RequestParam(name = "tracker_id", required = false) String trackerId,
            @RequestParam(name = "ip") String ip) {

        // Create a new instance of Bencode for encoding the response
        Bencode bencode = new Bencode();
        
        try {
            List<SessionDocument> sessions = null;
            
            // Handle events based on the event type
            switch (event) {
                case "started":
                    sessions = eventHandler.handleStartedEvent(infoHash, peerId, port, ip);
                    break;
                case "stopped":
                    sessions = eventHandler.handleStoppedEvent(infoHash, peerId);
                    break;
                case "completed":
                    sessions = eventHandler.handleCompletedEvent(infoHash, peerId, port, ip);
                    break;
                case "alive":
                    sessions = eventHandler.handleAliveEvent(infoHash, peerId, interval, left == 0, ip);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid event type");
            }

            // Prepare the response map
            HashMap<Object, Object> map = new HashMap<>();
            map.put("interval", HttpController.interval);

            // Count the number of completed and incomplete sessions
            int complete = 0, incomplete = 0;
            for (SessionDocument session : sessions) {
                if (session.isCompleted())
                    complete++;
                else
                    incomplete++;
            }
            map.put("complete", complete);
            map.put("incomplete", incomplete);
            // Add peer information based on compact flag
            if (compact.intValue() == 1) {
                // Compact mode: Use a ByteBuffer to store peer information
                ByteBuffer buffer = ByteBuffer.allocate(6 * sessions.size()); // ! WHAT IF THIS IS IPv6?

                for (SessionDocument session : sessions) {
                    try {
                        buffer.put(InetAddress.getByName(session.getIp()).getAddress());
                        buffer.putShort((short) session.getPort());
                    } catch (Exception e) {
                        throw e;
                    }
                }
                map.put("peers", buffer);
            } else {
                // Non-compact mode: Use a list of maps to store peer information
                List<Object> peers = new ArrayList<>();

                for (SessionDocument session : sessions) {
                    Map<Object, Object> peer = new HashMap<>();
                    if (noPeerId.intValue() != 1)
                        peer.put("peer_id", session.getPeerId());
                    peer.put("ip", session.getIp());
                    peer.put("port", session.getPort());
                    
                    peers.add(peer);
                }
                map.put("peers", peers);
            }

            if (trackerId == null) {
                // Generate a unique tracker ID using SHA-1 hashing
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                String id = InetAddress.getLocalHost().getHostAddress() + (new Date()).toString();
                md.update(id.getBytes());
                String hashedId = new String(md.digest());

                map.put("tracker_id", hashedId);
            } else {
                map.put("tracker_id", trackerId);
            }

            // Encode the response map using Bencode and return it
            byte[] encoded = bencode.encode(map);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(encoded);
        } catch (Exception e) {
            // Log the exception and prepare an error response
            ExceptionHandler.exceptionLog(e.toString());
            HashMap<Object, Object> map = new HashMap<>();
            map.put("failure reason", e.toString());

            byte[] encoded = bencode.encode(map);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(encoded);
        }
    }
}
