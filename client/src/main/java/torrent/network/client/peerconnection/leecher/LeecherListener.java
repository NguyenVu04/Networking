package torrent.network.client.peerconnection.leecher;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

public class LeecherListener implements WebSocket.Listener {
    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket,
            ByteBuffer data,
            boolean last) {
        //TODO: implement onBinary method
        webSocket.request(1);
        return null;
    }
}
