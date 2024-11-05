package torrent.network.client.peerconnection.uploader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import torrent.network.client.torrentdigest.TorrentDigest;

public class SingleFileApp {
    private static Map<String, ServedFileEntity> servingFiles = new ConcurrentHashMap<>();

    protected SingleFileApp() {
    }

    public synchronized static void serveFile(String filePath, String encodedInfoHash, int numberOfPieces, byte[] pieces) throws Exception {
        ServedFileEntity servedFileEntity = new ServedFileEntity(filePath, numberOfPieces, pieces);

        servingFiles.put(encodedInfoHash, servedFileEntity);
    }

    public synchronized static ServedFileEntity getServedFile(String encodedInfoHash) throws Exception {
        return servingFiles.get(encodedInfoHash);
    }

    public static boolean isServingFile(String encodeInfoHash) throws Exception {
        return servingFiles.containsKey(encodeInfoHash);
    }

    public static byte[] getBitField(String encodedInfoHash) throws Exception {
        return SingleFileApp.getServedFile(encodedInfoHash).getBitfield();
    }

    public static boolean isDone(String encodedInfoHash) throws Exception {
        return SingleFileApp.getServedFile(encodedInfoHash).isDone();
    }

    public static byte[] getPiece(String encodedInfoHash, int index, byte[] pieces) throws Exception {
        byte[] piece = SingleFileApp
            .getServedFile(encodedInfoHash)
            .getPiece(index);
        TorrentDigest td = new TorrentDigest(pieces);

        if (!td.verify(piece, index)) {
            throw new IOException("Invalid Piece");
        }
        
        return piece;
    }

    public static boolean hasPiece(String encodedInfoHash, int index) throws Exception {
        return SingleFileApp.getServedFile(encodedInfoHash).hasPiece(index);
    }

    public synchronized static boolean savePiece(String encodedInfoHash, int index, byte[] piece, byte[] pieces)
            throws Exception {
        return SingleFileApp
                .getServedFile(encodedInfoHash)
                .savePiece(index, piece, pieces);
    }

    public synchronized static List<Integer> getIndexOfPiecesLeft(String encodedInfoHash) throws Exception {
        return SingleFileApp.servingFiles.get(encodedInfoHash).getIndexLeft();
    }
}
