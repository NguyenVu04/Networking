import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.security.MessageDigest;

import org.junit.Test;

import network.torrent.torrentbuilder.TorrentBuilder;
import network.torrent.torrentdigest.TorrentDigest;

public class TorrentBuilderTest {
    @Test
    public void testGenerateSingleFileTorrent() throws Exception {
        TorrentBuilder torrentBuilder = new TorrentBuilder("http://example.com/announce");
        byte[] torrent = torrentBuilder.generateSingleFileTorrent("./test.txt");

        MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
        md.update("This is a test file".getBytes());
        byte[] digest = md.digest();

        System.err.println(new String(digest));

        System.err.println(new String(torrent));
        assertNotNull(torrent);
    }

    @Test
    public void testGenerateMultiFileTorrent() throws Exception {
        TorrentBuilder torrentBuilder = new TorrentBuilder("http://example.com/announce");
        byte[] torrent = torrentBuilder.generateMultiFileTorrent("./test_dir");

        byte[] testfile = torrentBuilder.generateSingleFileTorrent("./test_dir/test_2/text2.txt");

        System.err.println(new String(testfile));

        System.err.println(new String(torrent));
        assertNotNull(torrent);
    }

    @Test
    public void testTorrentDigest() throws Exception {
        String s1 = "This is test 1";
        String s2 = "This is test 2";

        MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);

        md.update(s1.getBytes());
        byte[] digest1 = md.digest();

        md.reset();

        md.update(s2.getBytes());
        byte[] digest2 = md.digest();

        ByteBuffer byteBuffer = ByteBuffer.allocate(digest1.length + digest2.length);
        byteBuffer.put(digest1);
        byteBuffer.put(digest2);

        byte[] digest3 = byteBuffer.array();

        TorrentDigest td1 = new TorrentDigest(digest3);
        boolean result = td1.verify(s1.getBytes(), 0);
        assertTrue(result);
    }
}
