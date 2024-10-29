package network.torrent.tracker.torrentcontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
public class TorrentController {
    @CrossOrigin(origins = "*")
    @PostMapping("/torrent")
    public ResponseEntity<Object> TorrentFileController(@RequestBody String data) {
        //TODO: process POST request
        
        return ResponseEntity.ok().build();
    }
    
}
