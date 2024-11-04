package torrent.network.client.peerconnection.uploader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleFileApp {
    private static Map<String, ServedFileEntity> servingFiles = new ConcurrentHashMap<>();

    protected SingleFileApp() {}

    public synchronized static void serveFile(String filePath, String infoHash, int numberOfPieces) throws Exception {
        ServedFileEntity servedFileEntity = new ServedFileEntity(filePath, numberOfPieces);

        servingFiles.put(infoHash, servedFileEntity);
    }

    public synchronized static ServedFileEntity getServedFile(String infoHash) throws Exception {
        return servingFiles.get(infoHash);
    }

    public synchronized static boolean isServingFile(String infoHash) throws Exception {
        return servingFiles.containsKey(infoHash);
    }

    public synchronized static byte[] getBitField(String infoHash) throws Exception {
        return SingleFileApp.getServedFile(infoHash).getBitfield();
    }

    public static boolean isDone(String infoHash) throws Exception {
        return SingleFileApp.getServedFile(infoHash).isDone();
    }

    public synchronized static byte[] getPiece(String infoHash, int index) throws Exception {
        return SingleFileApp.getServedFile(infoHash).getPiece(index);
    }

    public synchronized static boolean hasPiece(String infoHash, int index) throws Exception {
        return SingleFileApp.getServedFile(infoHash).hasPiece(index);
    }

    public synchronized static void savePiece(String infoHash, int index, byte[] piece) throws Exception {
        SingleFileApp.getServedFile(infoHash).savePiece(index, piece);
    }

    public synchronized static List<Integer> getIndexOfPiecesLeft(String infoHash) throws Exception {
        return SingleFileApp.servingFiles.get(infoHash).getIndexLeft();
    }
}
