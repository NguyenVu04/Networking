package network.torrent.peerconnection.leecher;

import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import network.torrent.torrentexception.ExceptionHandler;

public class LeecherSocket {
    private Socket socket;
    private boolean amChoking;
    private boolean amInterested;
    private boolean peerChoking;
    private boolean peerInterested;

    protected LeecherSocket() {}

    public static LeecherSocket create(String host, int port, byte[] infoHash, byte[] peerId) {
        LeecherSocket leecherSocket = new LeecherSocket();
        try (Socket socket = new Socket(host, port)) {
            leecherSocket.socket = socket;
            leecherSocket.amChoking = true;
            leecherSocket.amInterested = false;
            leecherSocket.peerChoking = true;
            leecherSocket.peerInterested = false;

            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            
            byte[] pstr = "BitTorrent protocol".getBytes();
            ByteBuffer buffer = ByteBuffer.allocate(49 + pstr.length);

            buffer.put((byte) pstr.length);
            buffer.put(pstr);
            buffer.put(new byte[] {0, 0, 0, 0, 0, 0, 0, 0});
            buffer.put(infoHash);
            buffer.put(peerId);

            outputStream.write(buffer.array());
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
            return null;
        }
        return leecherSocket;
    }

    public boolean sendKeepAlive() {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            byte[] buffer = new byte[] {0, 0, 0, 0};
            outputStream.write(buffer);
            outputStream.flush();

            return true;
        } catch (Exception e) {
            try {
                socket.close();
            } catch(Exception ex) {
                ExceptionHandler.handleException(ex);
            }
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public boolean sendChoke() {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            byte[] buffer = new byte[] {0, 0, 0, 1, 0};
            outputStream.write(buffer);
            outputStream.flush();

            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public boolean sendUnchoke() {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            byte[] buffer = new byte[] {0, 0, 0, 1, 1};
            outputStream.write(buffer);
            outputStream.flush();

            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public boolean sendInterested() {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            byte[] buffer = new byte[] {0, 0, 0, 1, 2};
            outputStream.write(buffer);
            outputStream.flush();

            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public boolean sendNotInterested() {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            byte[] buffer = new byte[] {0, 0, 0, 1, 3};
            outputStream.write(buffer);
            outputStream.flush();            

            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }

    public boolean sendHave(int pieceIndex) {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream())) {
            //TODO: Fix this
            byte[] buffer = new byte[] {0, 0, 0, 5, 4, (byte) pieceIndex, 0, 0, 0, 0};
            outputStream.write(buffer);
            outputStream.flush();

            return true;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return false;
    }
}
