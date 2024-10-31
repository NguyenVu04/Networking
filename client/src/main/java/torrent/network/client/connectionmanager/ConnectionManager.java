package torrent.network.client.connectionmanager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import torrent.network.client.peerconnection.leecher.LeecherSocket;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.PeerEntity;
import torrent.network.client.trackerconnection.TrackerConnection;

//TODO: ADD ERROR HANDLING LATER
//! WHAT IF SOCKET IS CLOSED
public class ConnectionManager {
    public class havingPiece {
        private boolean peerHaving;
        private ConcurrentLinkedDeque<LeecherSocket> sockets;

        public havingPiece(boolean requested, ConcurrentLinkedDeque<LeecherSocket> sockets) {
            this.peerHaving = requested;
            this.sockets = sockets;
        }

        public boolean isPeerHaving() {
            return this.peerHaving;
        }

        public LeecherSocket getSocket() {
            return this.sockets.poll();
        }

        public void setHaving(boolean peerHaving) {
            this.peerHaving = peerHaving;
        }

        public void addSocket(LeecherSocket socket) {
            this.sockets.add(socket);
        }

        public boolean isHavingPeerExist() {
            return !this.sockets.isEmpty();
        }
    }

    private TrackerConnection trackerConnection;
    private List<LeecherSocket> leecherSockets;
    private static Map<String, String> sessionMap = new ConcurrentHashMap<>();
    private static Map<String, ConnectionManager> infoMap = new ConcurrentHashMap<>();
    private List<Boolean> requestedPieces;
    // ! False means not having or requesting
    private List<havingPiece> havingPieces = new CopyOnWriteArrayList<>();
    private Thread requestThread;
    private Thread aliveThread;

    public boolean verifyPiece(byte[] piece, int index) {
        try {
            return this.trackerConnection.verifyPiece(piece, index);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public byte[] getInfoHash() {
        return trackerConnection.getInfoHash();
    }
    public String getPeerId() {
        return trackerConnection.getPeerId();
    }
    public static ConnectionManager getConnectionManager(String sessionId) {
        return infoMap.get(sessionMap.get(sessionId));
    }

    public ConnectionManager(String magnetText, String tracker_url, int port) throws Exception {
        this.trackerConnection = new TrackerConnection(magnetText, tracker_url, port);

        int numberOfPieces = (int) trackerConnection.getLeft() / TorrentBuilder.pieceSize;// ! THIS MAYBE WRONG
        for (int i = 0; i < numberOfPieces; i++) {
            this.requestedPieces.add(false);
            this.havingPieces.add(new havingPiece(false, new ConcurrentLinkedDeque<>()));
        }

        String tracker_ip = tracker_url.split("://")[1];

        System.err.println(tracker_ip);// TODO: remove this line later

        this.leecherSockets = new CopyOnWriteArrayList<>();

        List<PeerEntity> peers = trackerConnection.getPeers();
        for (PeerEntity peer : peers) {
            try {
                LeecherSocket socket = new LeecherSocket(this,
                        peer.getIp(),
                        false,
                        trackerConnection.getInfoHash(),
                        peer.getPeerId().getBytes(),
                        true);

                this.leecherSockets.add(socket);
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
            }
        }

        if (this.leecherSockets.size() == 0) {
            throw new Exception("No peers found");
        }

        this.requestThread = new Thread(() -> {
            while (this.trackerConnection.getLeft() > 0) {
                try {
                    Thread.sleep(Duration.ofSeconds(3));

                    synchronized (this) {
                        for (int i = 0; i < this.havingPieces.size(); i++) {
                            if ((!this.havingPieces.get(i).isPeerHaving()) &&
                                    this.havingPieces.get(i).isHavingPeerExist()) {

                                LeecherSocket socket = this.havingPieces.get(i).getSocket();
                                this.havingPieces.get(i).setHaving(true);
                                socket.sendRequest(i, 0, TorrentBuilder.pieceSize);

                                this.requestedPieces.set(i, true);
                            }
                        }
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                }
            }
        });
        this.requestThread.start();

        this.aliveThread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(Duration.ofSeconds(3));
                    synchronized (this) {
                        for (int i = 0; i < this.leecherSockets.size(); i++) {
                            if (!this.leecherSockets.get(i).isAlive())
                                this.leecherSockets.remove(i);
                        }
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
            }
        });
        this.aliveThread.start();

        infoMap.put(new String(trackerConnection.getInfoHash()), this);
    }

    public static void addSession(String infoHash, String session) {
        ConnectionManager.sessionMap.put(session, infoHash);
    }

    public static void removeSession(String session) {
        ConnectionManager.sessionMap.remove(session);
    }

    public void addLeecherSocket(LeecherSocket socket) {
        this.leecherSockets.add(socket);
    }

    public void addHavingPiece(int index, LeecherSocket socket) throws Exception {
        this.havingPieces.get(index).addSocket(socket);
    }
}
