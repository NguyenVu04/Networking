package torrent.network.client.requestcontroller;

import org.springframework.web.bind.annotation.RestController;

import torrent.network.client.connectionmanager.ConnectionManager;
import torrent.network.client.peerconnection.uploader.UploaderSocketController;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.TrackerConnection;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
public class MagnetTextController {

    @GetMapping("/magnet_text")
    public ResponseEntity<Object> getFileByMagnetText(
            @RequestParam(name = "magnet_text") String magnetText,
            @RequestParam(name = "tracker_url") String tracker_url) {

        try {
            ConnectionManager.createTrackerConnection(magnetText,
                    tracker_url,
                    UploaderSocketController.getPort());
            return ResponseEntity.ok(magnetText);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return ResponseEntity.internalServerError().build();
    }

    @PostMapping("/magnet_text")
    public ResponseEntity<Object> postMethodName(@RequestParam(name = "path") String path) {
        String result = TrackerConnection.sendTorrentFile(path, "http://127.0.0.1:8080");
        
        return ResponseEntity.ok(result);
    }
    

}
