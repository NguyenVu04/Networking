package torrent.network.client.peerconnection.leecher;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.peerconnection.uploader.SingleFileApp;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

public class LeecherListener implements WebSocket.Listener {
    private LeecherSocket socket;
    List<ByteBuffer> list = new ArrayList<>();
    CompletableFuture<?> accumulatedMessage = new CompletableFuture<>();

    public LeecherListener(LeecherSocket socket) {
        this.socket = socket;
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket,
            ByteBuffer data,
            boolean last) {
        // TODO: implement onBinary method
        list.add(data);
        webSocket.request(1);
        if (last) {
            for (ByteBuffer byteBuffer : list) {
                MessageType type = PeerMessage.getMessageType(byteBuffer.array());
                byte[] payload = PeerMessage.getPayload(byteBuffer.array());
                switch (type) {
                    case KEEP_ALIVE:
                        break;
                    case CHOKE:
                        break;
                    case UNCHOKE:
                        break;
                    case INTERESTED:
                        break;
                    case NOT_INTERESTED:
                        break;
                    case HAVE:
                        int index = ByteBuffer.wrap(payload).getInt();
                        try {
                            this.socket.addHavingPiece(index);
                        } catch (Exception e) {
                            ExceptionHandler.handleException(e);
                        }
                        break;
                    case BITFIELD:
                        for (int i = 0; i < payload.length; i++) {
                            if (payload[i] == 1) {
                                try {
                                    this.socket.addHavingPiece(i);
                                } catch (Exception e) {
                                    ExceptionHandler.handleException(e);
                                }
                            }
                        }

                        break;
                    case REQUEST:
                        break;
                    case PIECE:
                        int pieceIndex = ByteBuffer.wrap(payload, 0, 4).getInt();
                        byte[] block = ByteBuffer.wrap(payload, 8, payload.length - 8).array();
                        this.socket.verifyPiece(block, pieceIndex);

                        try (BufferedInputStream input = new BufferedInputStream(
                                new FileInputStream(SingleFileApp.getFile(this.socket.getInfoHash())))) {
                            ByteBuffer file = ByteBuffer.wrap(input.readAllBytes());

                            file.position(pieceIndex * TorrentBuilder.pieceSize);
                            file.put(block);

                            input.close();

                            BufferedOutputStream output = new BufferedOutputStream(
                                    new FileOutputStream(SingleFileApp.getFile(this.socket.getInfoHash()), false));

                            output.write(file.array());

                            output.close();

                        } catch (Exception e) {
                            ExceptionHandler.handleException(e);
                        }
                        break;
                    case PORT:
                        break;
                    default:
                        break;
                }
            }

            list = new ArrayList<>();
            accumulatedMessage.complete(null);
            CompletionStage<?> cf = accumulatedMessage;
            accumulatedMessage = new CompletableFuture<>();
            return cf;
        }
        return accumulatedMessage;
    }
}
