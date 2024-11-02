package torrent.network.client.peerconnection.uploader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//! YOU ARE A FUCKING IDIOT WHO CAN'T HANDLE THIS SHIT.
//! TRY TO IMPLEMENT MULTIFILE MODE SOMEDAY.
public class SingleFileApp {
    private static Map<String, ServedFileEntity> servingFiles = new ConcurrentHashMap<>();

    protected SingleFileApp() {}

    public static void serveFile(String filePath, byte[] infoHash, int numberOfPieces) throws Exception {
        ServedFileEntity servedFileEntity = new ServedFileEntity(filePath, numberOfPieces);

        servingFiles.put(new String(infoHash), servedFileEntity);
    }

    public static ServedFileEntity getServedFile(byte[] infoHash) throws Exception {
        return servingFiles.get(new String(infoHash));
    }

    public static boolean isServingFile(byte[] infoHash) throws Exception {
        return servingFiles.containsKey(new String(infoHash));
    }

    public static byte[] getBitField(byte[] infoHash) throws Exception {
        return SingleFileApp.getServedFile(infoHash).getBitfield();
    }

    public static boolean isDone(byte[] infoHash) throws Exception {
        return SingleFileApp.getServedFile(infoHash).isDone();
    }

    public static byte[] getPiece(byte[] infoHash, int index) throws Exception {
        return SingleFileApp.getServedFile(infoHash).getPiece(index);
    }

    public static boolean hasPiece(byte[] infoHash, int index) throws Exception {
        return SingleFileApp.getServedFile(infoHash).hasPiece(index);
    }

    public static void savePiece(byte[] infoHash, int index, byte[] piece) throws Exception {
        SingleFileApp.getServedFile(infoHash).savePiece(index, piece);
    }
}
