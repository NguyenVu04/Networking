package torrent.network.client.torrententity;

import java.util.List;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;

public class MultiFileInfo {
    private String name;
    private Object files;
    private int piece_length;
    private byte[] pieces;

    protected MultiFileInfo() {}

    public String getName() {
        return name;
    }

    public List<FileEntity> getFiles() {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(files);
            Object[] array = gson.fromJson(jsonElement, Object[].class);

            List<FileEntity> files = new ArrayList<>();

            for (Object file : array) {
                files.add(FileEntity.from(file));
            }
            return files;
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        return null;
    }

    public int getPieceLength() {
        return piece_length;
    }

    public byte[] getPieces() {
        return this.pieces;
    }

    public static MultiFileInfo from(Object info) {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(info);
            return gson.fromJson(jsonElement, MultiFileInfo.class);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    public boolean verifyPiece(byte[] piece, int index) {
        TorrentDigest td = new TorrentDigest(this.pieces);
        return td.verify(piece, index);
    }

    public long getSize() {
        return this.pieces.length / TorrentBuilder.hashedPieceLength * TorrentBuilder.pieceSize;
    }
}
