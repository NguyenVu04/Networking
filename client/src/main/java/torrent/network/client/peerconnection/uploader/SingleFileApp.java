package torrent.network.client.peerconnection.uploader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

//! YOU ARE A FUCKING IDIOT WHO CAN'T HANDLE THIS SHIT.
//! TRY TO IMPLEMENT MULTIFILE MODE SOMEDAY.
public class SingleFileApp {
    private static Map<String, Map.Entry<File, List<Boolean>>> servingFiles = new ConcurrentHashMap<>();

    protected SingleFileApp() {
    }

    public static void addBlankFile(byte[] infoHash, String path, int size) throws IOException {
        Path file = Paths.get(path);
        File newFile = Files.createFile(file).toFile();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(newFile))) {
            out.write(byteBuffer.array());
            out.flush();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        servingFiles.putIfAbsent(new String(infoHash), Map.entry(newFile, new CopyOnWriteArrayList<>()));

        for (int i = 0; i < newFile.length() / TorrentBuilder.pieceSize; i++) {
            servingFiles.get(new String(infoHash)).getValue().add(false);
        }
    }

    public static void addPiece(byte[] infoHash, int index) {
        Map.Entry<File, List<Boolean>> entry = servingFiles.get(new String(infoHash));
        entry.getValue().set(index, true);
    }

    public static boolean serveFile(byte[] infoHash, String filePath) {
        Path path = Path.of(filePath);
        File file = path.toFile();
        if (file.exists()) {
            List<Boolean> pieces = new CopyOnWriteArrayList<>();

            for (int i = 0; i < file.length() / TorrentBuilder.pieceSize; i++) {
                pieces.add(true);
            }
            servingFiles.putIfAbsent(new String(infoHash), Map.entry(file, pieces));
            return true;
        }

        return false;
    }

    public static File getFile(byte[] infoHash) {
        return servingFiles.get(new String(infoHash)).getKey();
    }

    public static boolean isServingFile(byte[] infoHash) {
        return servingFiles.containsKey(new String(infoHash));
    }

    public static boolean isServingPiece(byte[] infoHash, int index) {
        Map.Entry<File, List<Boolean>> entry = servingFiles.get(new String(infoHash));

        if (entry != null && entry.getKey().length() > index) {
            List<Boolean> pieces = entry.getValue();
            return pieces.get(index).booleanValue();
        }

        return false;
    }

    public static byte[] getPiece(byte[] infoHash, int index) {
        if (isServingPiece(infoHash, index)) {
            File file = servingFiles.get(new String(infoHash)).getKey();

            try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
                byte[] piece = new byte[TorrentBuilder.pieceSize];

                int byteRead = inputStream.read(piece, index * TorrentBuilder.pieceSize, TorrentBuilder.pieceSize);

                if (byteRead == TorrentBuilder.pieceSize) {
                    return piece;
                } else {
                    return Arrays.copyOf(piece, byteRead);
                }
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
            }
        }

        return null;
    }

    public static byte[] getBitField(byte[] infoHash) {
        if (isServingFile(infoHash)) {
            List<Boolean> pieces = servingFiles.get(new String(infoHash)).getValue();
            byte[] bitfield = new byte[pieces.size()];
            for (int i = 0; i < pieces.size(); i++) {
                bitfield[i] = pieces.get(i) ? (byte) 1 : (byte) 0;
            }
            return bitfield;
        }

        return null;
    }
}
