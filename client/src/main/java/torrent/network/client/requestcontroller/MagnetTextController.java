package torrent.network.client.requestcontroller;

import org.springframework.web.bind.annotation.RestController;

import torrent.network.client.connectionmanager.ConnectionManager;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.TrackerConnection;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
public class MagnetTextController {
    // @Autowired
    // private ServletWebServerApplicationContext webServerAppCtxt;
    public static final String trackerUrl = "http://localhost:8080";// ! CHANGE LATER

    @CrossOrigin(origins = "*")
    @GetMapping("/magnet_text")
    public ResponseEntity<Object> getFileByMagnetText(
            @RequestParam(name = "magnet_text") String magnetText,
            @RequestParam(name = "path") String path) {

        try {
            TrackerConnection connection = ConnectionManager.createTrackerConnetion(
                    magnetText,
                    trackerUrl, 9999,
                    //webServerAppCtxt.getWebServer().getPort(),
                    path);

            Map<String, Object> map = new HashMap<>();
            map.put("magnet_text", magnetText);
            map.put("peers", connection.getPeers());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return ResponseEntity.notFound().build();
    }
    @CrossOrigin(origins = "*")
    @PostMapping("/magnet_text")
    public ResponseEntity<Object> connectTracker(
            @RequestParam(name = "path") String path) {
        try {
            String result = TrackerConnection.sendTorrentFile(path, trackerUrl);

            if (result == null) {
                return ResponseEntity.internalServerError().build();
            }

            TrackerConnection connection = ConnectionManager.createTrackerConnetion(
                    result,
                    trackerUrl, 9999,
                    //webServerAppCtxt.getWebServer().getPort(),
                    path);

            Map<String, Object> map = new HashMap<>();
            map.put("magnet_text", result);
            map.put("peers", connection.getPeers());
            return ResponseEntity.ok(map);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return ResponseEntity.internalServerError().build();
    }
    @CrossOrigin(origins = "*")
    @PostMapping("/torrent_file")
    public ResponseEntity<Object> postTorrentFile(
            @RequestParam(name = "downloadPath") String downloadPath,
            @RequestParam(name = "torrentPath") String torrentPath) {
        try {
            TorrentBuilder builder = new TorrentBuilder(trackerUrl);
            byte[] data = builder.generateSingleFileTorrent(downloadPath);

            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(torrentPath));
            out.write(data);
            out.flush();
            out.close();

            if (data == null) {
                return ResponseEntity.internalServerError().build();
            }
            HexFormat hexFormat = HexFormat.of();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String magnetText = hexFormat.formatHex(digest.digest(data));

            TrackerConnection connection = ConnectionManager.createTrackerConnetion(
                magnetText, 
                trackerUrl, 9999,
                //webServerAppCtxt.getWebServer().getPort(), 
                downloadPath);

            Map<String, Object> map = new HashMap<>();
            map.put("peers", connection.getPeers());
            map.put("magnet_text", magnetText);
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return ResponseEntity.internalServerError().build();
    }
    @CrossOrigin(origins = "*")
    @GetMapping("/info")
    public ResponseEntity<Object> getInfo(@RequestParam(name = "magnet_text") String magnetText) {
        try {
            String encodedInfoHash = ConnectionManager.getInfoHash(magnetText);

            if (encodedInfoHash == null) {
                return ResponseEntity.internalServerError().build();
            }

            TrackerConnection connection = ConnectionManager.getTrackerConnection(encodedInfoHash);

            Map<String, Object> map = new HashMap<>();
            map.put("downloaded", connection.getDownloaded());
            map.put("uploaded", connection.getUploaded());
            map.put("length", connection.getNumberOfPieces());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return ResponseEntity.notFound().build();
    }

}
