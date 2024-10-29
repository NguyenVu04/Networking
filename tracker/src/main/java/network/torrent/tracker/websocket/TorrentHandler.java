package network.torrent.tracker.websocket;

import java.util.List;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class TorrentHandler implements WebSocketHandler {
    private static WebSocketSession session = null;
    private static WebSocketSession clientSession = null;
    private static boolean done = false;
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        if (TorrentHandler.session == null)
            return;

        if (TorrentHandler.session.getId().equals(session.getId()))
            TorrentHandler.session = null;

        if (TorrentHandler.clientSession == null)
            return;

        if (TorrentHandler.clientSession.getId().equals(session.getId()))
            TorrentHandler.clientSession = null;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (TorrentHandler.session == null) {
            synchronized (this) {
                TorrentHandler.session = session;
            }
            return;
        }

        synchronized (this) {
            TorrentHandler.clientSession = session;
        }

        List<String> clientHeaders = session.getHandshakeHeaders().get("X-Forwarded-For");
        String client = clientHeaders != null ? clientHeaders.get(0) : session.getRemoteAddress().getHostString();

        List<String> serverHeaders = TorrentHandler.session.getHandshakeHeaders().get("X-Forwarded-For");
        String server = serverHeaders != null ? serverHeaders.get(0)
                : TorrentHandler.session.getRemoteAddress().getHostString();

        TorrentHandler.session.sendMessage(new TextMessage("connect:" + client));
        session.sendMessage(new TextMessage("connect:" + server));
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> data) throws Exception {
        String message = data.getPayload().toString();

        List<String> clientHeaders = session.getHandshakeHeaders().get("X-Forwarded-For");
        String client = clientHeaders != null ? clientHeaders.get(0) : session.getRemoteAddress().getHostString();

        String server = null;
        if (TorrentHandler.session != null) {
            List<String> serverHeaders = TorrentHandler.session.getHandshakeHeaders().get("X-Forwarded-For");
            server = serverHeaders != null ? serverHeaders.get(0)
                    : TorrentHandler.session.getRemoteAddress().getHostString();
        }
        switch (message) {
            case "ping":
                session.sendMessage(new TextMessage("ping:" + client));
                break;
            case "sent":
                clientSession.sendMessage(new TextMessage("connect:" + server));
                break;

            case "done":
                TorrentHandler.done = true;
                System.out.println("CONNECTED");
                System.out.println("_____________________________________________");
                break;

            case "failed":
                if (TorrentHandler.done)
                    break;

                TorrentHandler.session.sendMessage(new TextMessage("connect:" + client));
                session.sendMessage(new TextMessage("connect:" + server));

                break;
            default:
                break;
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
