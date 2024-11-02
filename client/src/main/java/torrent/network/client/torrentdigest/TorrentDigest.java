package torrent.network.client.torrentdigest;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;
public class TorrentDigest {
    byte[] hashedBuffer;
    
    public TorrentDigest(byte[] buffer) {
        this.hashedBuffer= buffer;  
    }

    public boolean verify(byte[] input, int index) {
        try {
            MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
            byte[] hashedInput = md.digest(input);

            ByteBuffer byteBuffer = ByteBuffer.wrap(hashedBuffer, index * md.getDigestLength(), md.getDigestLength());
            return byteBuffer.equals(ByteBuffer.wrap(hashedInput));
        } catch (Exception e) {
            ExceptionHandler.handleException(e);
        }
        
        return false;
    }
}
