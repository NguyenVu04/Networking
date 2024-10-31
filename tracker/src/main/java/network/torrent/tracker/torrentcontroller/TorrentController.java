package network.torrent.tracker.torrentcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;

import network.torrent.tracker.torrentrepository.filesystem.TorrentFileRepository;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
public class TorrentController {
    @Autowired
    private TorrentFileRepository torrentRepo;

    @CrossOrigin(origins = "*")
    @PostMapping(value = "/torrent")
    public ResponseEntity<Object> postTorrentFile(@RequestBody byte[] data) {
        String filename = torrentRepo.saveTorrentFile(data);

        if (filename == null)
            return ResponseEntity.internalServerError()
                    .build();

        return ResponseEntity.ok()
                .body(filename);
    }

    @CrossOrigin(origins = "*")
    @GetMapping(value = "/torrent")
    public ResponseEntity<Object> getTorrentFile(@RequestParam(name = "magnet_text") String magnetText) {
        byte[] data = torrentRepo.getTorrentFile(magnetText);

        if (data == null)
            return ResponseEntity.notFound()
                    .build();
        return ResponseEntity.ok()
                .body(data);
    }
    
}
