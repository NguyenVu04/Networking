package torrent.network.client.peerconnection;

import java.nio.ByteBuffer;
import java.util.Arrays;
import torrent.network.client.peerconnection.leecher.LeecherSocket;

/**
 * Class used to create the messages sent to the peer.
 */
public class PeerMessage {

    protected PeerMessage() {}

    /**
     * Creates the handshake message sent to the peer.
     * @param infoHash The information hash of the torrent.
     * @param peerId The id of the peer.
     * @return The handshake message as a byte array.
     */
    public static byte[] createHandshakeMessage(byte[] infoHash, byte[] peerId) {
        ByteBuffer buffer = ByteBuffer.allocate(49 + LeecherSocket.pstr.length());

        buffer.put((byte) LeecherSocket.pstr.length());
        buffer.put(LeecherSocket.pstr.getBytes());
        buffer.put(infoHash);
        buffer.put(peerId);

        return buffer.array();
    }

    /**
     * Creates the keep alive message sent to the peer.
     * @return The keep alive message as a byte array.
     */
    public static byte[] createKeepAliveMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put(new byte[] { 0, 0, 0, 0 });

        return buffer.array();
    }

    /**
     * Creates the choke message sent to the peer.
     * @return The choke message as a byte array.
     */
    public static byte[] createChokeMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 0 });

        return buffer.array();
    }

    /**
     * Creates the unchoke message sent to the peer.
     * @return The unchoke message as a byte array.
     */
    public static byte[] createUnchokeMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 1 });

        return buffer.array();
    }

    /**
     * Creates the interested message sent to the peer.
     * @return The interested message as a byte array.
     */
    public static byte[] createInterestedMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 2 });

        return buffer.array();
    }

    /**
     * Creates the not interested message sent to the peer.
     * @return The not interested message as a byte array.
     */
    public static byte[] createNotInterestedMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 3 });

        return buffer.array();
    }

    /**
     * Creates the have message sent to the peer.
     * @param index The index of the piece in the torrent.
     * @return The have message as a byte array.
     */
    public static byte[] createHaveMessage(int index) {
        ByteBuffer buffer = ByteBuffer.allocate(9);

        buffer.put(new byte[] { 0, 0, 0, 5, 4 });

        buffer.putInt(index);

        return buffer.array();
    }

    /**
     * Creates the bitfield message sent to the peer.
     * @param bitfield The bitfield of the pieces the peer has.
     * @return The bitfield message as a byte array.
     */
    public static byte[] createBitfieldMessage(byte[] bitfield) {
        ByteBuffer buffer = ByteBuffer.allocate(5 + bitfield.length);

        int len = bitfield.length + 1;
        buffer.putInt(len);

        buffer.put(new byte[] { 5 });

        buffer.put(bitfield);

        return buffer.array();
    }

    /**
     * Creates the request message sent to the peer.
     * @param index The index of the piece in the torrent.
     * @param begin The beginning of the piece to request.
     * @param length The length of the piece to request.
     * @return The request message as a byte array.
     */
    public static byte[] createRequestMessage(int index, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(17);

        buffer.put(new byte[] { 0, 0, 0, 13, 6 });

        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);

        return buffer.array();
    }

    /**
     * Creates the piece message sent to the peer.
     * @param index The index of the piece in the torrent.
     * @param begin The beginning of the piece to send.
     * @param piece The piece to send.
     * @return The piece message as a byte array.
     */
    public static byte[] createPieceMessage(int index, int begin, byte[] piece) {
        ByteBuffer bufer = ByteBuffer.allocate(13 + piece.length - begin);
        bufer.putInt(9 + piece.length - begin);

        bufer.put(new byte[] { 7 });

        bufer.putInt(index);
        bufer.putInt(begin);

        bufer.put(ByteBuffer.wrap(piece, begin, piece.length - begin));

        return bufer.array();
    }

    /**
     * Creates the cancel message sent to the peer.
     * @param index The index of the piece in the torrent.
     * @param begin The beginning of the piece to cancel.
     * @param length The length of the piece to cancel.
     * @return The cancel message as a byte array.
     */
    public static byte[] createCancelMessage(int index, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(17);

        buffer.put(new byte[] { 0, 0, 0, 13, 8 });

        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);

        return buffer.array();
    }

    /**
     * Creates the port message sent to the peer.
     * @param port The port to use for the connection.
     * @return The port message as a byte array.
     */
    public static byte[] createPortMessage(short port) {
        ByteBuffer buffer = ByteBuffer.allocate(7);

        buffer.put(new byte[] { 0, 0, 0, 3, 9 });

        buffer.putShort(port);

        return buffer.array();
    }

    public static MessageType getMessageType(byte[] message) {
        if (message[0] == LeecherSocket.pstr.length()) return MessageType.HANDSHAKE;
        if (message.length < 5) return MessageType.KEEP_ALIVE;
        return MessageType.values()[message[4] + 1];
    }

    public static byte[] getPayload(byte[] message) {
        if (message.length < 5) return null;
        return Arrays.copyOfRange(message, 5, message.length);
    }
}
