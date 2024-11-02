package torrent.network.client.peerconnection.leecher;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import torrent.network.client.peerconnection.uploader.SingleFileApp;
import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.PeerEntity;

public class LeecherSocketController {
    private byte[] infoPieces;
    private byte[] infoHash;
    private Map<String, LeecherSocket> leecherSocketMap;
    private List<ConcurrentLinkedDeque<LeecherSocket>> pieceMap;
    private int numberOfPieces;
    private ConcurrentLinkedDeque<Integer> blockQueue;
    private List<Integer> numberOfPiecesList;
    private Thread aliveThread;

    public LeecherSocketController(byte[] infoPieces, byte[] infoHash, int numberOfPieces, List<PeerEntity> peers)
            throws Exception {
        this.infoPieces = infoPieces;
        this.infoHash = infoHash;
        pieceMap = new CopyOnWriteArrayList<>();
        this.numberOfPieces = numberOfPieces;
        blockQueue = new ConcurrentLinkedDeque<>();
        this.numberOfPiecesList = new CopyOnWriteArrayList<>();

        for (int i = 0; i < numberOfPieces; i++) {
            this.pieceMap.add(new ConcurrentLinkedDeque<>());
            this.numberOfPiecesList.add(Integer.valueOf(0));
        }

        leecherSocketMap = new ConcurrentHashMap<>();

        for (PeerEntity peer : peers) {
            createLeecherSocket(peer);
        }

        this.aliveThread = new Thread(() -> {
            while (true) {
                try {
                    int max = 0;
                    int index = -1;

                    for (int i = 0; i < numberOfPieces; i++) {
                        if (numberOfPiecesList.get(i) > max) {
                            max = numberOfPiecesList.get(i);
                            index = i;
                        }
                    }

                    if (index != -1) {
                        numberOfPiecesList.set(index, 0);
                        blockQueue.add(index);
                    } else {
                        Thread.sleep(3000);// ! ARE YOU SURE THIS WORKS?
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                }
            }

        });
        this.aliveThread.start();

    }

    public void createLeecherSocket(PeerEntity peer) throws Exception {
        if (this.leecherSocketMap.containsKey(peer.getPeerId())) {
            return;
        }

        if (!this.leecherSocketMap.get(peer.getPeerId()).isClosed())
            return;

        LeecherSocket socket = new LeecherSocket(peer, this.infoHash, this, this.numberOfPieces);
        this.leecherSocketMap.put(peer.getPeerId(), socket);
    }

    public void updatePieceMap(LeecherSocket socket, boolean[] bitfield) throws Exception {
        if (bitfield.length != numberOfPieces) {
            throw new IllegalArgumentException("Invalid bitfield length");
        }

        for (int i = 0; i < bitfield.length; i++) {
            if (bitfield[i]) {
                pieceMap.get(i).add(socket);
                numberOfPiecesList.set(i, pieceMap.get(i).size());
            }
        }

    }

    public void updatePieceMap(LeecherSocket socket, int pieceIndex) throws Exception {
        if (pieceIndex < 0 || pieceIndex >= numberOfPieces) {
            throw new IllegalArgumentException("Invalid piece index");
        }

        pieceMap.get(pieceIndex).add(socket);
        numberOfPiecesList.set(pieceIndex, pieceMap.get(pieceIndex).size());

    }

    public int getNextBlockIndex(boolean[] bitfield) {
        synchronized (this) {
            if (blockQueue.isEmpty() || !bitfield[blockQueue.peek().intValue()]) {
                return -1;
            }

            return blockQueue.poll().intValue();
        }
    }

    public boolean verifyPiece(byte[] piece, int index) throws Exception {
        TorrentDigest td = new TorrentDigest(this.infoPieces);
        if (td.verify(piece, index)) {
            SingleFileApp.savePiece(this.infoHash, index, piece);

            return true;
        }

        return false;
    }

    public void returnIndex(int index) {
        this.numberOfPiecesList.set(
                index,
                this.pieceMap.get(index).size());
    }
}
