package network.torrent.tracker.viewcontroller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.gridfs.model.GridFSFile;

import network.torrent.tracker.torrentrepository.session.SessionRepository;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class ViewController {
    @Autowired
    private SessionRepository peerRepo;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @CrossOrigin(origins = "*")
    @GetMapping("/stat/peers")
    public ResponseEntity<Object> statPeers() {
        return ResponseEntity.ok(peerRepo.findAll());
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/stat/torrents")
    public ResponseEntity<Object> statTorrent() {
        return ResponseEntity.ok()
                .body(gridFsTemplate
                        .find(new Query(Criteria.where("filename").regex(".*"))));
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/stat/torrents/magnet")
    public ResponseEntity<Object> getTorrentByMagnet(@RequestParam(name = "magnet_text") String magnetText) {

        GridFsResource resource = gridFsTemplate.getResource(magnetText);
        return ResponseEntity.ok().body(resource);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/stat/count")
    public ResponseEntity<Object> getTorrentCount() {
        long peerCount = peerRepo.count();
        long torrentCount = 0;
        MongoCursor<GridFSFile> iterable = gridFsTemplate.find(
                new Query(
                        Criteria.where("filename")
                                .regex(".*")))
                .iterator();
        while (iterable.hasNext()) {
            torrentCount++;
            iterable.next();
        }

        Map<String, Long> map = Map.of("peer_count", peerCount, "torrent_count", torrentCount);
        return ResponseEntity.ok().body(map);
    }
}
