package network.torrent.tracker.torrentrepository.session;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "session")
public class SessionDocument {
    @Id
    private String peerId;
    private String infoHash;
    private int port;
    private boolean completed;
    private Date createdAt;
    private String ip;

    public SessionDocument(String infoHash, String peerId, int port, boolean completed, String ip) {
        this.infoHash = infoHash;
        this.peerId = peerId;
        this.port = port;
        this.completed = completed;
        this.createdAt = new Date();
        this.ip = ip;
    }

    
    public void resetCreatedAt() {
        this.createdAt = new Date();
    }

    public String getIp() {
        return ip;
    }

    public String getInfoHash() {
        return infoHash;
    }

    public String getPeerId() {
        return peerId;
    }

    public int getPort() {
        return port;
    }

    public boolean isCompleted() {
        return completed;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}
