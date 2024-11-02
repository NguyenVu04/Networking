package torrent.network.client.peerconnection.leecher;

import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.PeerEntity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class LeecherSocket {
    private Socket socket;
    private Thread aliveThread;
    private boolean[] bitfield;

    public LeecherSocket(PeerEntity peer, byte[] infoHash, LeecherSocketController controller, int numberOfPieces)
            throws Exception {

        this.bitfield = new boolean[numberOfPieces];

        try (Socket socket = new Socket(peer.getIp(), peer.getPort())) {
            this.socket = socket;

            try (BufferedOutputStream output = new BufferedOutputStream(this.socket.getOutputStream())) {
                output.write(PeerMessage.createHandshakeMessage(infoHash, peer.getPeerId().getBytes()));
                output.flush();
            }

            try (BufferedInputStream input = new BufferedInputStream(this.socket.getInputStream())) {
                byte[] message = input.readAllBytes();

                if (!PeerMessage.getMessageType(message).equals(MessageType.BITFIELD)) {
                    throw new Exception("Expected bitfield message");
                }

                byte[] payload = PeerMessage.getPayload(message);

                boolean[] bitfield = new boolean[payload.length];
                for (int i = 0; i < payload.length; i++) {
                    bitfield[i] = payload[i] == 1;
                }

                controller.updatePieceMap(this, bitfield);
            }

            this.aliveThread = new Thread(() -> {
                while (!this.socket.isClosed()) {
                    this.socketTask(controller);
                }
            });

            this.aliveThread.start();
        }
    }

    public void socketTask(LeecherSocketController controller) {

        int index = controller.getNextBlockIndex(this.bitfield);
        if (index == -1) {
            try (BufferedOutputStream output = new BufferedOutputStream(this.socket.getOutputStream())) {
                output.write(PeerMessage.createKeepAliveMessage());
                output.flush();
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
                try {
                    this.socket.close();
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
            }

            try (BufferedInputStream input = new BufferedInputStream(this.socket.getInputStream())) {
                byte[] message = input.readAllBytes();

                if (!PeerMessage.getMessageType(message).equals(MessageType.KEEP_ALIVE)) {
                    throw new Exception("Expected keepalive message");
                }
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
                try {
                    this.socket.close();
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
            }
        } else {
            try (BufferedOutputStream output = new BufferedOutputStream(this.socket.getOutputStream())) {
                output.write(PeerMessage.createRequestMessage(index, 0, TorrentBuilder.pieceSize));
                output.flush();
            } catch (Exception e) {
                ExceptionHandler.handleException(e);

                controller.returnIndex(index);

                try {
                    this.socket.close();
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
            }

            try (BufferedInputStream input = new BufferedInputStream(this.socket.getInputStream())) {
                byte[] message = input.readAllBytes();

                if (!PeerMessage.getMessageType(message).equals(MessageType.PIECE)) {
                    throw new Exception("Expected piece message");
                }

                byte[] payload = PeerMessage.getPayload(message);
                int pieceIndex = ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).getInt();
                byte[] piece = Arrays.copyOfRange(payload, 8, payload.length);

                if (!controller.verifyPiece(piece, pieceIndex))
                    throw new Exception("Invalid piece");

            } catch (Exception e) {
                ExceptionHandler.handleException(e);

                controller.returnIndex(index);

                try {
                    this.socket.close();
                } catch (Exception ex) {
                    ExceptionHandler.handleException(ex);
                }
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isClosed() {
        return this.socket.isClosed();
    }
}
