package network.torrent.torrentdigest;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import network.torrent.torrentbuilder.TorrentBuilder;
import network.torrent.torrentexception.ExceptionHandler;
public class TorrentDigest {
    byte[] hashedBuffer;
    
    public TorrentDigest(byte[] buffer) {
        this.hashedBuffer= buffer;  
    }

    public boolean verify(byte[] input, int index) {
        try {
            MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
            md.update(input, 0, TorrentBuilder.pieceSize);
            byte[] hashedInput = md.digest();

            ByteBuffer byteBuffer = ByteBuffer.wrap(hashedBuffer, index * md.getDigestLength(), md.getDigestLength());
            return byteBuffer.equals(ByteBuffer.wrap(hashedInput));
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        
        return false;
    }
}
