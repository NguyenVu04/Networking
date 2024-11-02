package torrent.network.client.peerconnection.uploader;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import torrent.network.client.torrentexception.ExceptionHandler;

@Configuration
public class UploaderSocketController {
    private ServerSocket serverSocket;
    private static int port = 9999;
    List<UploaderSocket> threads;
    ConcurrentLinkedDeque<Integer> freeThreadId;
    private static final int maxThreads = 20;
    private Thread aliveThread;

    public UploaderSocketController() throws Exception {
        this.serverSocket = new ServerSocket(0);
        UploaderSocketController.port = serverSocket.getLocalPort();
        freeThreadId = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < maxThreads; i++) {
            freeThreadId.add(Integer.valueOf(i));
        }

        threads = new CopyOnWriteArrayList<>(new UploaderSocket[maxThreads]);

        this.aliveThread = new Thread(() -> {
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    synchronized (this) {
                        if (freeThreadId.isEmpty()) {
                            socket.close();
                        } else {
                            int threadId = freeThreadId.pop().intValue();

                            try {
                                UploaderSocket newSocket = new UploaderSocket(socket, this, threadId);
                                threads.set(threadId, newSocket);
                            } catch (Exception e) {
                                ExceptionHandler.handleException(e);
                                freeThreadId.add(threadId);
                            }

                        }
                    }
                } catch (Exception e) {
                    ExceptionHandler.handleException(e);
                }
            }
        });
        this.aliveThread.start();
    }

    @Bean
    public UploaderSocketController getController() {
        try {
            return new UploaderSocketController();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public void removeThread(int threadId) {
        this.threads.set(threadId, null);
        freeThreadId.add(threadId);
    }

    public boolean isAlive() {
        return !serverSocket.isClosed();
    }

    public static int getPort() {
        return UploaderSocketController.port;
    }
}
