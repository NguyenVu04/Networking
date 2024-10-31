package torrent.network.client.requestcontroller;

import org.springframework.web.bind.annotation.RestController;

import torrent.network.client.ClientApplication;
import torrent.network.client.connectionmanager.ConnectionManager;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class MagnetTextController {

    @GetMapping("/magnet_text")
    public ResponseEntity<Object> getFileByMagnetText(
            @RequestParam(name = "magnet_text") String magnetText,
            @RequestParam(name = "tracker_url") String tracker_url) {
        
        try {
            ConnectionManager manager = new ConnectionManager(magnetText, tracker_url, 8080);
            ClientApplication.connections.add(manager);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }              

        return ResponseEntity.ok(magnetText);
    }

}
