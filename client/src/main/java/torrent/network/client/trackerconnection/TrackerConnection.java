package torrent.network.client.trackerconnection;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import torrent.network.client.peerconnection.leecher.LeecherController;
import torrent.network.client.peerconnection.uploader.SingleFileApp;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrententity.SingleFileInfo;
import torrent.network.client.torrententity.TorrentEntity;
import torrent.network.client.torrentexception.ExceptionHandler;

import java.time.Duration;

public class TrackerConnection {
    private byte[] peerId;
    private long downloaded;
    private long left;
    private long uploaded;
    private int port;
    private byte[] infoHash;
    private byte[] info;
    private String trackerUrl;
    private List<PeerEntity> peers;
    public static final String torrentPath = "torrent";
    private Thread aliveThread;
    private boolean alive;
    private int interval;
    private SingleFileInfo singleFileInfo;

    public TrackerConnection(String magnetText, String tracker_url, int port, String path) throws Exception {
        Bencode bencode = new Bencode();
        this.peerId = null;
        this.peers = new CopyOnWriteArrayList<>();
        this.downloaded = 0;
        this.uploaded = 0;
        this.port = port;
        byte[] torrentFile = getTorrentFile(magnetText, tracker_url);

        if (torrentFile == null)
            throw new Exception("Failed to get torrent file");

        Map<String, Object> torrent = bencode.decode(torrentFile, Type.DICTIONARY);

        TorrentEntity torrentEntity = TorrentEntity.from(torrent);

        singleFileInfo = torrentEntity.getSingleFileInfo();

        this.left = singleFileInfo.getLength();

        this.trackerUrl = torrentEntity.getAnnounce();
        this.info = torrentEntity.getInfo();
        this.infoHash = TorrentEntity.getInfoHash(this.info);

        String encodedInfoHash = TrackerConnection.getEncodedInfoHash(this.infoHash);
        String ip = InetAddress.getLocalHost().getHostAddress();
        MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
        Date now = new Date();
        this.peerId = md.digest((now.toString() + ip + encodedInfoHash).getBytes(StandardCharsets.UTF_8));

        TrackerResponse response = this.getTrackerResponse("started");

        List<PeerEntity> peerList = response.getPeers();
        synchronized (this) {
            this.peers.addAll(peerList);
        }

        SingleFileApp.serveFile(path, encodedInfoHash, this.getNumberOfPieces(), this.getPieces());
        List<Integer> indexLeft = SingleFileApp.getIndexOfPiecesLeft(encodedInfoHash);
        this.left = indexLeft.size() * TorrentBuilder.pieceSize;

        this.interval = response.getInterval();
        this.alive = true;
        this.aliveThread = new Thread(() -> {
            while (this.alive) {
                try {
                    Thread.sleep(Duration.ofSeconds(this.interval));
                    if (SingleFileApp.isDone(encodedInfoHash)) {
                        this.sendCompletedEvent();
                    } else {
                        this.sendAliveEvent();
                        LeecherController.createLeecherController(
                                this.getPieces(),
                                this.infoHash,
                                this.peerId,
                                this.peers,
                                indexLeft);
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                    this.alive = false;
                }
            }
        });
        this.aliveThread.start();
    }

    public TrackerConnection(int port, String path) throws Exception {
        Bencode bencode = new Bencode();
        this.peerId = null;
        this.peers = new CopyOnWriteArrayList<>();
        this.downloaded = 0;
        this.uploaded = 0;
        this.port = port;

        BufferedInputStream stream = new BufferedInputStream(new FileInputStream(path));
        byte[]torrentFile = stream.readAllBytes();
        stream.close();
        
        Map<String, Object> torrent = bencode.decode(torrentFile, Type.DICTIONARY);

        TorrentEntity torrentEntity = TorrentEntity.from(torrent);

        singleFileInfo = torrentEntity.getSingleFileInfo();

        this.left = singleFileInfo.getLength();

        this.trackerUrl = torrentEntity.getAnnounce();
        this.info = torrentEntity.getInfo();
        this.infoHash = TorrentEntity.getInfoHash(this.getInfo());

        String encodedInfoHash = TrackerConnection.getEncodedInfoHash(this.infoHash);
        String ip = InetAddress.getLocalHost().getHostAddress();
        MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
        Date now = new Date();
        this.peerId = md.digest((now.toString() + ip + encodedInfoHash).getBytes(StandardCharsets.UTF_8));

        TrackerResponse response = this.getTrackerResponse("started");

        List<PeerEntity> peerList = response.getPeers();
        synchronized (this) {
            this.peers.addAll(peerList);
        }

        SingleFileApp.serveFile(path, encodedInfoHash, this.getNumberOfPieces(), this.getPieces());
        List<Integer> indexLeft = SingleFileApp.getIndexOfPiecesLeft(encodedInfoHash);
        this.left = indexLeft.size() * TorrentBuilder.pieceSize;

        this.interval = response.getInterval();
        this.alive = true;
        this.aliveThread = new Thread(() -> {
            while (this.alive) {
                try {
                    Thread.sleep(Duration.ofSeconds(this.interval));
                    if (SingleFileApp.isDone(encodedInfoHash)) {
                        this.sendCompletedEvent();
                    } else {
                        this.sendAliveEvent();
                        LeecherController.createLeecherController(
                                this.getPieces(),
                                this.infoHash,
                                this.peerId,
                                this.peers,
                                indexLeft);
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                    this.alive = false;
                }
            }
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

    public byte[] getPeerId() {
        return this.peerId;
    }

    private byte[] getTorrentFile(String magnetText, String trackerUrl) {
        try {
            RestClient response = RestClient.builder()
                    .baseUrl(trackerUrl)
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
                    .baseUrl(this.trackerUrl)
                    .build();

            String ip = InetAddress.getLocalHost().getHostAddress();
            String encodedInfoHash = HexFormat.of().formatHex(this.infoHash);
            String encodedPeerId = HexFormat.of().formatHex(this.peerId);

            ResponseEntity<byte[]> response = request.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("info_hash", encodedInfoHash)
                            .queryParam("peer_id", encodedPeerId)
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

            this.getTrackerResponse("completed");

        } catch (Exception e) {
            alive = false;
            ExceptionHandler.handleException(e);
            this.peers.clear();
        }
    }

    public void sendStoppedEvent() {
        alive = false;
        this.peers.clear();
        try {
            this.getTrackerResponse("stopped");
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendAliveEvent() {
        try {
            TrackerResponse res = this.getTrackerResponse("alive");

            this.peers.clear();
            this.peers.addAll(res.getPeers());

        } catch (Exception e) {
            alive = false;
            this.peers.clear();
            ExceptionHandler.handleException(e);
        }
    }

    public List<PeerEntity> getPeers() {
        return this.peers;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public static String sendTorrentFile(String path, String trackerUrl) {
        try {
            Path filePath = Path.of(path);

            byte[] data;

            if (filePath.toFile().isFile()) {
                data = new TorrentBuilder(trackerUrl)
                        .generateSingleFileTorrent(path);
            } else {
                data = new TorrentBuilder(trackerUrl)
                        .generateMultiFileTorrent(path);
            }

            if (data == null)
                throw new Exception("Failed to generate torrent file");

            RestClient request = RestClient.builder()
                    .baseUrl(trackerUrl)
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

    public byte[] getInfoHash() {
        return this.infoHash;
    }

    public byte[] getInfo() {
        return this.info;
    }

    public int getNumberOfPieces() {
        return this.singleFileInfo.getNumberOfPieces();
    }

    public boolean peerInSwarm(byte[] infoHash, byte[] peerId) {
        String encodedInfoHash = HexFormat.of().formatHex(infoHash);
        String encodedPeerId = HexFormat.of().formatHex(peerId);

        RestClient client = RestClient.create(this.trackerUrl);

        ResponseEntity<Boolean> response = client.get()
                .uri(uriBuilder -> uriBuilder.path("peer")
                        .queryParam("info_hash", encodedInfoHash)
                        .queryParam("peer_id", encodedPeerId)
                        .build())
                .retrieve()
                .toEntity(Boolean.class);

        return response.getBody();
    }

    public String getTrackerUrl() {
        return this.trackerUrl;
    }

    public void increaseDownloaded() {
        this.downloaded += TorrentBuilder.pieceSize;
    }

    public void increaseUploaded() {
        this.uploaded += TorrentBuilder.pieceSize;
    }

    public byte[] getPieces() {
        return this.singleFileInfo.getPieces();
    }

    public static String getEncodedInfoHash(byte[] infoHash) {
        return HexFormat.of().formatHex(infoHash);
    }
}
