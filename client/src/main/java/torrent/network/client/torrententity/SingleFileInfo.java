package torrent.network.client.torrententity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;

public class SingleFileInfo {
    private String name;
    private int length;
    private int piece_length;
    private byte[] pieces;

    protected SingleFileInfo() {}

    public String getName() {
        return name;
    }

    public int getLength() {
        return length;
    }

    public int getPieceLength() {
        return piece_length;
    }

    public byte[] getPieces() {
        return this.pieces;
    }

    public static SingleFileInfo from(Object info) {
        try {
            Gson gson = new Gson();
    
            JsonElement jsonElement = gson.toJsonTree(info);
            return gson.fromJson(jsonElement, SingleFileInfo.class);
            
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
