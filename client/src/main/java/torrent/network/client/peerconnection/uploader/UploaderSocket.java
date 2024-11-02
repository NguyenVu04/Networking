package torrent.network.client.peerconnection.uploader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.torrentexception.ExceptionHandler;

public class UploaderSocket {
    private Socket socket;
    private Thread aliveThread;
    private byte[] infoHash;

    public UploaderSocket(Socket socket, UploaderSocketController controller, int threadId)
            throws Exception {
        this.socket = socket;
        this.aliveThread = new Thread(() -> {
            try (BufferedInputStream input = new BufferedInputStream(this.socket.getInputStream())) {
                byte[] message = input.readAllBytes();

                MessageType messageType = PeerMessage.getMessageType(message);

                if (!messageType.equals(MessageType.HANDSHAKE)) {
                    throw new Exception("Expected handshake message");
                }

                this.infoHash = PeerMessage.getInfoHash(message);

                if (!SingleFileApp.isServingFile(this.infoHash)) {
                    throw new Exception("Serving file not found");
                }

                try (BufferedOutputStream output = new BufferedOutputStream(this.socket.getOutputStream())) {
                    output.write(PeerMessage.createBitfieldMessage(infoHash));
                    output.flush();
                }

                
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
                return;
            }

            while (!this.socket.isClosed()) {
                try (BufferedInputStream input = new BufferedInputStream(socket.getInputStream())) {
                    byte[] message = input.readAllBytes();

                    MessageType messageType = PeerMessage.getMessageType(message);

                    switch (messageType) {
                        case KEEP_ALIVE:
                            try (BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
                                output.write(PeerMessage.createKeepAliveMessage());
                                output.flush();
                            }
                        case CHOKE:
                            // TODO: Add choke message
                            break;
                        case UNCHOKE:
                            // TODO: Add unchoke message
                            break;
                        case INTERESTED:
                            // TODO: Add interested message
                            break;
                        case NOT_INTERESTED:
                            // TODO: Add not interested message
                            break;
                        case HAVE:
                            break;
                        case BITFIELD:
                            // TODO: Add bitfield message
                            break;
                        case REQUEST:
                            byte[] requestPayload = PeerMessage.getPayload(message);
                            try (BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
                                int index = ByteBuffer.wrap(Arrays.copyOfRange(requestPayload, 0, 4)).getInt();
                                int begin = 0;// Always start at byte 0
                                // int length = TorrentBuilder.pieceSize;

                                if (!SingleFileApp.hasPiece(this.infoHash, index))
                                    throw new Exception("Piece not found");

                                output.write(PeerMessage.createPieceMessage(index,
                                        begin,
                                        SingleFileApp.getPiece(this.infoHash, index)));
                                output.flush();
                            } catch (Exception e) {
                                ExceptionHandler.handleException(e);
                                try (BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {
                                    output.write(PeerMessage.createKeepAliveMessage());
                                    output.flush();
                                }
                            }
                            break;
                        case PIECE:
                            // TODO: Add piece message
                            break;
                        case CANCEL:
                            // TODO: Add cancel message
                            break;
                        case PORT:
                            // TODO: Add port message
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                    try {
                        this.socket.close();
                    } catch (Exception ex) {
                        ExceptionHandler.handleException(ex);
                        break;
                    }
                }
            }

            controller.removeThread(threadId);
        });
        this.aliveThread.start();
    }

    public byte[] getInfoHash() {
        return this.infoHash;
    }
}
