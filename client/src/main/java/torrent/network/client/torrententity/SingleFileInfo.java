package torrent.network.client.torrententity;

import java.util.HexFormat;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

public class SingleFileInfo {
    private String name;
    private long length;
    private int piece_length;
    private String pieces;

    protected SingleFileInfo() {
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public int getPieceLength() {
        return piece_length;
    }

    public byte[] getPieces() {
        return HexFormat.of().parseHex(this.pieces);
    }

    public static SingleFileInfo from(Map<String, Object> info) {
        try {
            Gson gson = new Gson();

            JsonElement jsonElement = gson.toJsonTree(info);
            return gson.fromJson(jsonElement, SingleFileInfo.class);

        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return null;
    }

    public int getNumberOfPieces() {
        return (int) this.length / TorrentBuilder.pieceSize + 1;
    }
}
