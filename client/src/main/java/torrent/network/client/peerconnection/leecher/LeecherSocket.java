package torrent.network.client.peerconnection.leecher;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.torrentexception.ExceptionHandler;

import java.net.URI;

public class LeecherSocket {
    private WebSocket socket;
    private boolean amChoking;
    private boolean amInterested;
    private boolean peerChoking;
    private boolean peerInterested;
    public WebSocket getSocket() {
        return socket;
    }

    public boolean isAmChoking() {
        return amChoking;
    }

    public boolean isAmInterested() {
        return amInterested;
    }

    public boolean isPeerChoking() {
        return peerChoking;
    }

    public boolean isPeerInterested() {
        return peerInterested;
    }

    public static final String pstr = "BitTorrent protocol";

    protected LeecherSocket() {
    }

    public static LeecherSocket create(String ip, boolean ssl, byte[] infoHash, byte[] peerId) {
        HttpClient client = HttpClient.newHttpClient();

        CompletableFuture<WebSocket> future = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws" + (ssl ? "s" : "") + "://" + ip), new LeecherListener());

        LeecherSocket socket = new LeecherSocket();
        try {
            socket.socket = future.get();

            socket.sendHandShake(infoHash, peerId);

            socket.amChoking = true;
            socket.amInterested = false;
            socket.peerChoking = true;
            socket.peerInterested = false;

            return socket;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    private void sendHandShake(byte[] infoHash, byte[] peerId) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createHandshakeMessage(infoHash, peerId)), true);

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendKeepAlive() {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createKeepAliveMessage()), true);
        } catch (Exception e) {
            socket.abort();
            ExceptionHandler.handleException(e);
        }
    }

    public void sendChoke() {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createChokeMessage()), true);

            this.amChoking = true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendUnchoke() {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createUnchokeMessage()), true);

            this.amChoking = false;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendInterested() {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createInterestedMessage()), true);

            this.amInterested = true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendNotInterested() {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createNotInterestedMessage()), true);

            this.amInterested = false;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendHave(int index) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createHaveMessage(index)), true);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendBitfield(byte[] bitfield) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createBitfieldMessage(bitfield)), true);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendRequest(int index, int begin, int length) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createRequestMessage(index, begin, length)), true);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendPiece(int index, int begin, byte[] piece) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createPieceMessage(index, begin, piece)), true);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendCancel(int index, int begin, int length) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createCancelMessage(index, begin, length)), false);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }

    public void sendPort(short port) {
        try {
            socket.sendBinary(ByteBuffer.wrap(PeerMessage.createPortMessage(port)), true);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
    }
}
