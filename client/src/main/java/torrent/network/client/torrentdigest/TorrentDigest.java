package torrent.network.client.torrentdigest;

import java.security.MessageDigest;
import java.util.Arrays;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

public class TorrentDigest {
    byte[] hashedBuffer;

    public TorrentDigest(byte[] buffer) {
        this.hashedBuffer = buffer;
    }

    public boolean verify(byte[] input, int index) {
        try {
            MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
            byte[] hashedInput = md.digest(input);

            byte[] piece = Arrays.copyOfRange(this.hashedBuffer,
                    index * TorrentBuilder.hashedPieceLength,
                    Math.min((index + 1) * TorrentBuilder.hashedPieceLength, hashedBuffer.length));
            return Arrays.equals(hashedInput, piece);
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }

        return false;
    }
}
