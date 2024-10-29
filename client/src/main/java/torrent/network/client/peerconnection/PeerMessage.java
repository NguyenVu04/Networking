package torrent.network.client.peerconnection;

import java.nio.ByteBuffer;

import torrent.network.client.peerconnection.leecher.LeecherSocket;

public class PeerMessage {
    protected PeerMessage() {}

    public static byte[] createHandshakeMessage(byte[] infoHash, byte[] peerId) {
        ByteBuffer buffer = ByteBuffer.allocate(49 + LeecherSocket.pstr.length());

        buffer.put((byte) LeecherSocket.pstr.length());
        buffer.put(LeecherSocket.pstr.getBytes());
        buffer.put(infoHash);
        buffer.put(peerId);

        return buffer.array();
    }

    public static byte[] createKeepAliveMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(4);

        buffer.put(new byte[] { 0, 0, 0, 0 });

        return buffer.array();
    }

    public static byte[] createChokeMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 0 });

        return buffer.array();
    }

    public static byte[] createUnchokeMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 1 });

        return buffer.array();
    }

    public static byte[] createInterestedMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 2 });

        return buffer.array();
    }

    public static byte[] createNotInterestedMessage() {
        ByteBuffer buffer = ByteBuffer.allocate(5);

        buffer.put(new byte[] { 0, 0, 0, 1, 3 });

        return buffer.array();
    }

    public static byte[] createHaveMessage(int index) {
        ByteBuffer buffer = ByteBuffer.allocate(9);

        buffer.put(new byte[] { 0, 0, 0, 5, 4 });

        return buffer.array();
    }

    public static byte[] createBitfieldMessage(byte[] bitfield) {
        ByteBuffer buffer = ByteBuffer.allocate(5 + bitfield.length);

        int len = bitfield.length + 1;
        buffer.putInt(len);

        buffer.put(new byte[] { 5 });

        buffer.put(bitfield);

        return buffer.array();
    }

    public static byte[] createRequestMessage(int index, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(17);

        buffer.put(new byte[] { 0, 0, 0, 13, 6 });

        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);

        return buffer.array();
    }

    public static byte[] createPieceMessage(int index, int begin, byte[] piece) {
        ByteBuffer bufer = ByteBuffer.allocate(13 + piece.length - begin);
        bufer.putInt(9 + piece.length - begin);

        bufer.put(new byte[] { 7 });

        bufer.putInt(index);
        bufer.putInt(begin);

        bufer.put(ByteBuffer.wrap(piece, begin, piece.length - begin));

        return bufer.array();
    }

    public static byte[] createCancelMessage(int index, int begin, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(17);

        buffer.put(new byte[] { 0, 0, 0, 13, 8 });

        buffer.putInt(index);
        buffer.putInt(begin);
        buffer.putInt(length);

        return buffer.array();
    }

    public static byte[] createPortMessage(short port) {
        ByteBuffer buffer = ByteBuffer.allocate(7);

        buffer.put(new byte[] { 0, 0, 0, 3, 9 });

        buffer.putShort(port);

        return buffer.array();
    }
}
