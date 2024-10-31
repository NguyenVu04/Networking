package torrent.network.client.trackerconnection;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrententity.MultiFileInfo;
import torrent.network.client.torrententity.SingleFileInfo;
import torrent.network.client.torrententity.TorrentEntity;
import torrent.network.client.torrentexception.ExceptionHandler;

import java.time.Duration;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
public class TrackerConnection {
    private String peer_id;
    private long downloaded;
    private long left;
    private long uploaded;
    private int port;
    private byte[] infoHash;
    private String tracker_url;
    private List<PeerEntity> peers;
    public static final String torrentPath = "torrent";
    private Thread aliveThread;
    private boolean alive;
    private int interval;
    private MultiFileInfo multiFileInfo;
    private SingleFileInfo singleFileInfo;

    public TrackerConnection(String magnetText, String tracker_url, int port) throws Exception {
        Bencode bencode = new Bencode();
        this.peer_id = null;
        this.peers = new CopyOnWriteArrayList<>();
        this.downloaded = 0;
        this.uploaded = 0;
        this.port = port;
        byte[] torrentFile = getTorrentFile(magnetText, tracker_url);

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("D:\\Project\\torrent.torrent"));
        out.write(torrentFile);
        out.flush();
        out.close();

        if (torrentFile == null)
            throw new Exception("Failed to get torrent file");

        Map<String, Object> torrent = bencode.decode(torrentFile, Type.DICTIONARY);

        TorrentEntity torrentEntity = TorrentEntity.from(torrent);

        multiFileInfo = torrentEntity.getMultiFileInfo();
        singleFileInfo = torrentEntity.getSingleFileInfo();
        if (multiFileInfo == null && singleFileInfo == null)
            throw new Exception("Invalid torrent file");
        
        this.left = multiFileInfo != null ? multiFileInfo.getSize() : singleFileInfo.getSize();

        this.tracker_url = torrentEntity.getAnnounce();

        this.infoHash = torrentEntity.getInfoHash();

        TrackerResponse response = this.getTrackerResponse("started");

        List<PeerEntity> peerList = response.getPeers();
        synchronized (this) {
            this.peers.clear();
            this.peers.addAll(peerList);
        }

        this.interval = response.getInterval();
        this.alive = true;
        this.aliveThread = new Thread(() -> {
            while (this.alive) {
                this.sendAliveEvent();
            }

            this.sendStoppedEvent();
        });
        this.aliveThread.start();
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public long getLeft() {
        return left;
    }

    public void setLeft(long left) {
        this.left = left;
    }

    public long getUploaded() {
        return uploaded;
    }

    public void setUploaded(long uploaded) {
        this.uploaded = uploaded;
    }

    public String getPeerId() {
        return this.peer_id;
    }

    private byte[] getTorrentFile(String magnetText, String tracker_url) {
        try {
            RestClient response = RestClient.builder()
                    .baseUrl(tracker_url)
                    .build();

            ResponseEntity<byte[]> result = response.get()
                    .uri(uriBuilder -> uriBuilder.path(TrackerConnection.torrentPath)
                            .queryParam("magnet_text", magnetText)
                            .build())
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .toEntity(byte[].class);

            return result.getBody();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    private TrackerResponse getTrackerResponse(String event) {
        try {
            RestClient request = RestClient.builder()
                    .baseUrl(this.tracker_url)
                    .build();

            String ip = InetAddress.getLocalHost().getHostAddress();

            if (peer_id == null) {
                MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
                Date now = new Date();

                this.peer_id = new String(md.digest((now.toString() + ip + infoHash).getBytes()));

            }

            String encodedInfoHash = URLEncoder.encode(new String(this.infoHash), "UTF-8");

            ResponseEntity<byte[]> response = request.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("info_hash", encodedInfoHash)
                            .queryParam("peer_id", this.peer_id)
                            .queryParam("port", this.port)
                            .queryParam("downloaded", this.downloaded)
                            .queryParam("left", this.left)
                            .queryParam("uploaded", this.uploaded)
                            .queryParam("event", event)
                            .queryParam("compact", 0)
                            .queryParam("no_peer_id", 0)
                            .queryParam("ip", ip)
                            .build())
                    .header("Content-Type", "text/plain")
                    .retrieve()
                    .toEntity(byte[].class);

            Bencode bencode = new Bencode();
            Map<String, Object> responseMap = bencode.decode(
                    response.getBody(), Type.DICTIONARY);

            return TrackerResponse.from(responseMap);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    public void sendCompletedEvent() {
        try {
            TrackerResponse res = this.getTrackerResponse("completed");

            if (res != null) {
                synchronized (this) {
                    this.peers.clear();
                    this.peers.addAll(res.getPeers());
                }
            } else {
                throw new Exception("Failed to send completed event");
            }
        } catch (Exception e) {
            this.aliveThread.interrupt();
            alive = false;
            ExceptionHandler.handleException(e);

            synchronized (this) {
                this.peers.clear();
            }
        }
    }

    public void sendStoppedEvent() {
        synchronized (this) {
            this.peers.clear();
        }
        this.aliveThread.interrupt();
        alive = false;

        try {
            this.getTrackerResponse("stopped");
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendAliveEvent() {
        try {
            Thread.sleep(Duration.ofSeconds(this.interval));

            TrackerResponse res = this.getTrackerResponse("alive");

            if (res != null) {

                synchronized (this) {
                    this.peers.clear();
                    this.peers.addAll(res.getPeers());
                }

            } else {
                throw new Exception("Failed to send alive event");
            }
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
            alive = false;
            synchronized (this) {
                this.peers.clear();
            }
        }
    }

    public List<PeerEntity> getPeers() {
        return this.peers;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public static String sendTorrentFile(String path, String tracker_url) {
        try {
            Path filePath = Path.of(path);

            byte[] data;

            if (filePath.toFile().isFile()) {
                data = new TorrentBuilder(tracker_url)
                        .generateSingleFileTorrent(path);
            } else {
                data = new TorrentBuilder(tracker_url)
                        .generateMultiFileTorrent(path);
            }

            if (data == null)
                throw new Exception("Failed to generate torrent file");

            RestClient request = RestClient.builder()
                    .baseUrl(tracker_url)
                    .build();

            ResponseEntity<String> res = request.post()
                    .uri(uriBuilder -> uriBuilder.path(TrackerConnection.torrentPath)
                            .build())
                    .body(data)
                    .contentType(MediaType.TEXT_PLAIN)
                    .accept(MediaType.TEXT_PLAIN)
                    .retrieve()
                    .toEntity(String.class);

            return res.getBody();

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public boolean verifyPiece(byte[] piece, int index) throws Exception {
        boolean result;
        if (this.singleFileInfo != null) {
            result = this.singleFileInfo.verifyPiece(piece, index);

        } else if (this.multiFileInfo != null) {
            result = this.multiFileInfo.verifyPiece(piece, index);

        } else {
            throw new Exception("No torrent file found");

        }
        if (result) {
            this.downloaded += TorrentBuilder.pieceSize;
            
            if (this.left > 0) {
                this.left -= TorrentBuilder.pieceSize;

                if (left == 0)
                    this.sendCompletedEvent();
            }
        }

        return result;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }
}
