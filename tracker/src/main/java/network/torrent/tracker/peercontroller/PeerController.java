package network.torrent.tracker.peercontroller;

import org.springframework.web.bind.annotation.RestController;

import network.torrent.tracker.torrentrepository.session.SessionDocument;
import network.torrent.tracker.torrentrepository.session.SessionRepository;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class PeerController {
    @Autowired
    private SessionRepository repo;

    @GetMapping("/peer")
    public ResponseEntity<Object> peerInSwarm(
        @RequestParam(name = "info_hash") String infoHash,
        @RequestParam(name = "peer_id") String peerId) {


        Optional<SessionDocument> session = repo.findById(peerId);

        if (!session.isPresent() || !session.get().getInfoHash().equals(infoHash)) {
            return ResponseEntity.ok(false);
        }    

        return ResponseEntity.ok(true);
    }
    
}
