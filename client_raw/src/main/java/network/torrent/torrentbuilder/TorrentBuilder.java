package network.torrent.torrentbuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.dampcake.bencode.Bencode;

import network.torrent.torrentexception.ExceptionHandler;

/**
 * Class for generating .torrent files from directories or single files.
 * Uses SHA-1 hashing for piece hashing.
 * 
 * @author DampCake
 */
public class TorrentBuilder {
    /**
     * Size of each piece in bytes.
     */
    public static final int pieceSize = 1024 * 512;
    /**
     * Algorithm for hashing the pieces. Currently only SHA-1 is supported.
     */
    public static final String HASH_ALGORITHM = "SHA-1";
    /**
     * URL of the tracker. This is the url where the tracker will be listening for
     * connections.
     */
    private String trackerURL;

    /**
     * Constructor for TorrentBuilder.
     * 
     * @param pieceSize Size of each piece in bytes.
     * @param trackerURL URL of the tracker. This is the url where the tracker will be
     *        listening for connections.
     */
    public TorrentBuilder(String trackerURL) {
        this.trackerURL = trackerURL;
    }

    /**
     * Generates the piece string for a single file.
     * @param filePath Path of the file to generate the piece string for.
     * @return The piece string for the file.
     */
    private byte[] generateFilePieces(String filePath) {
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(filePath))) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(0);
            byte[] buffer = new byte[TorrentBuilder.pieceSize];
            int bytesRead = 0;
            while ((bytesRead = stream.read(buffer, 0, buffer.length)) != -1) {

                MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
                md.update(buffer, 0, bytesRead);
                byte[] hashedBuffer = md.digest();

                ByteBuffer tmpBuffer = ByteBuffer.allocate(byteBuffer.capacity() + hashedBuffer.length);
                tmpBuffer.put(byteBuffer.array());
                byteBuffer.clear();
                tmpBuffer.put(hashedBuffer);

                byteBuffer = tmpBuffer;
            }

            return byteBuffer.array();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    /**
     * Generates the piece string for a directory.
     * @param rootDirPath Path of the directory to generate the piece string for.
     * @return The piece string for the directory.
     */
    private byte[] generateDirPieces(String rootDirPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                (Path.of(rootDirPath)))) {
            ByteBuffer filesBuilder = ByteBuffer.allocate(0);

            for (Path path : stream) {
                File file = path.toFile();
                if (file.isFile()) {
                    byte[] pieces = generateFilePieces(file.getPath());

                    if (pieces == null)
                        return null;

                    ByteBuffer tmpBuffer = ByteBuffer.allocate(filesBuilder.capacity() + pieces.length);
                    tmpBuffer.put(filesBuilder.array());
                    filesBuilder.clear();
                    tmpBuffer.put(pieces);

                    filesBuilder = tmpBuffer;
                } else {
                    byte[] pieces = generateDirPieces(file.getPath());

                    if (pieces == null)
                        return null;

                    ByteBuffer tmpBuffer = ByteBuffer.allocate(filesBuilder.capacity() + pieces.length);
                    tmpBuffer.put(filesBuilder.array());
                    filesBuilder.clear();
                    tmpBuffer.put(pieces);

                    filesBuilder = tmpBuffer;
                }
            }

            return filesBuilder.array();
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    /**
     * Gets a list of all the files in a directory and their sizes.
     * @param rootDirPath Path of the directory to get the files from.
     * @return A list of all the files in the directory and their sizes.
     */
    private List<Map.Entry<String, Long>> getFiles(String rootDirPath) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(rootDirPath))) {
            List<Map.Entry<String, Long>> files = new ArrayList<>();

            for (Path path : stream) {
                File file = path.toFile();

                if (file.isFile()) {
                    files.add(Map.entry(file.getPath(), Long.valueOf(file.length())));
                } else {
                    List<Map.Entry<String, Long>> subFiles = getFiles(file.getPath());
                    
                    if (subFiles == null)
                        return null;

                    files.addAll(subFiles);
                }   
            }
            return files;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    /**
     * Generates a .torrent file for a directory.
     * @param rootDirPath Path of the directory to generate the .torrent file for.
     * @return The .torrent file for the directory.
     */
    public byte[] generateMultiFileTorrent(String rootDirPath) {
        try {

            Path rootPath = Path.of(rootDirPath);

            String name = rootPath.getFileName().toString();

            if (rootPath.toFile().isFile())
                return null;

            HashMap<Object, Object> torrent = new HashMap<>();
            HashMap<Object, Object> info = new HashMap<>();

            info.put("name", name);

            List<Map<Object, Object>> files = new ArrayList<>();

            byte[] pieces = generateDirPieces(rootDirPath);

            if (pieces == null)
                return null;

            info.put("pieces", pieces);

            List<Map.Entry<String, Long>> filesMap = getFiles(rootDirPath);

            if (filesMap == null)
                return null;

            for (Map.Entry<String, Long> entry : filesMap) {
                List<String> path = List.of(entry.getKey().split("/"));

                files.add(Map.of("length", entry.getValue(), "path", path));
            }

            torrent.put("info", info);
            torrent.put("announce", trackerURL);

            info.put("files", files);

            Bencode bencode = new Bencode();
            return bencode.encode(torrent);

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    /**
     * Generates a .torrent file for a single file.
     * @param filePath Path of the file to generate the .torrent file for.
     * @return The .torrent file for the file.
     */
    public byte[] generateSingleFileTorrent(String filePath) {
        byte[] pieces = generateFilePieces(filePath);
        if (pieces == null)
            return null;

        HashMap<Object, Object> torrent = new HashMap<>();

        HashMap<Object, Object> info = new HashMap<>();
        info.put("piece length", TorrentBuilder.pieceSize);
        info.put("pieces", pieces);

        File file = new File(filePath);
        info.put("name", file.getName());
        info.put("length", file.length());

        torrent.put("info", info);
        torrent.put("announce", trackerURL);

        Bencode bencode = new Bencode();
        return bencode.encode(torrent);
    }
}
