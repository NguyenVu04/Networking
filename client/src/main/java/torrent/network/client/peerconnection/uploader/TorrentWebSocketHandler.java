package torrent.network.client.peerconnection.uploader;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import torrent.network.client.connectionmanager.ConnectionManager;
import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.peerconnection.leecher.LeecherSocket;
import torrent.network.client.torrentexception.ExceptionHandler;

public class TorrentWebSocketHandler implements WebSocketHandler {
    private static Map<String, LeecherSocket> leecherSockets = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        ConnectionManager.removeSession(session.getId());

        TorrentWebSocketHandler.leecherSockets.remove(session.getId()).close();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(message.getPayload());
            oos.flush();

            byte[] bytes = bos.toByteArray();

            MessageType type = PeerMessage.getMessageType(bytes);
            switch (type) {
                case HANDSHAKE:
                    byte[] infoHash = Arrays.copyOfRange(bytes, 28, 48);

                    if (!SingleFileApp.isServingFile(infoHash))
                        session.close();

                    ConnectionManager.addSession(new String(infoHash), session.getId());
                    ConnectionManager manager = ConnectionManager.getConnectionManager(session.getId());
                    LeecherSocket socket = new LeecherSocket(manager,
                            session.getRemoteAddress().getAddress().toString(),
                            false,
                            infoHash,
                            manager.getPeerId().getBytes(),
                            false);

                    TorrentWebSocketHandler.leecherSockets.put(session.getId(), socket);

                    socket.sendBitfield(SingleFileApp.getBitField(infoHash));
                    break;
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
                    int haveIndex = ByteBuffer.wrap(PeerMessage.getPayload(bytes)).getInt();
                    ConnectionManager.getConnectionManager(session.getId())
                            .addHavingPiece(haveIndex,
                                    TorrentWebSocketHandler.leecherSockets
                                            .get(session.getId()));
                    break;
                case BITFIELD:
                    break;
                case REQUEST:
                    // TODO: implement request message
                    byte[] payload = PeerMessage.getPayload(bytes);

                    int requestIndex = ByteBuffer.wrap(payload, 0, 4).getInt();
                    // ! begin and length are 0 for single file
                    byte[] buffer = SingleFileApp.getPiece(
                            ConnectionManager.getConnectionManager(
                                    session.getId())
                                    .getInfoHash(),
                            requestIndex);

                    session.sendMessage(new BinaryMessage(buffer));
                    break;
                case PIECE:
                    break;
                case CANCEL:
                    break;
                case PORT:
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        ExceptionHandler.handleException(new Exception("Transport error: ", exception));
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
