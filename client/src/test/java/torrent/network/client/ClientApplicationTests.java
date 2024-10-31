package torrent.network.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.TrackerConnection;

import java.security.MessageDigest;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
@SpringBootTest
class ClientApplicationTests {
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
		assertTrue(!result);
	}

	@Test
	public void testObjectParse() {
		// SingleFileInfo info = new SingleFileInfo("aaaa", 10, 10, "aaaa");
		// Map<String, Object> infoMap = new HashMap<>();
		// infoMap.put("name", info.getName());
		// infoMap.put("length", info.getLength());
		// infoMap.put("piece length", info.getPieceLength());
		// infoMap.put("pieces", info.getPieces());
		
		// TorrentEntity entity = new TorrentEntity(infoMap, "ggggg");

		// Map<String, Object> map = new HashMap<>();
		// map.put("announce", entity.getAnnounce());
		// map.put("info", entity.getInfo());

		// Bencode bencode = new Bencode();
		// byte[] torrent = bencode.encode(map);

		// Map<String, Object> torrentMap = bencode.decode(torrent, Type.DICTIONARY);

		// TorrentEntity torrentEntity = TorrentEntity.from(torrentMap);
		// System.err.println(torrentEntity.getAnnounce());
		// assertNotNull(torrentEntity);

		List<List<String>> paths = new ArrayList<>();
		paths.add(new ArrayList<String>());
		paths.add(new ArrayList<String>());
		paths.add(new ArrayList<String>());
		paths.get(0).add("test");
		paths.get(1).add("test");
		paths.get(2).add("test");

		Bencode bencode = new Bencode();
		byte[] torrent = bencode.encode(paths);
		
		List<Object> decoded = bencode.decode(torrent, Type.LIST);

		Gson gson = new Gson();
		JsonElement jsonElement = gson.toJsonTree(decoded);

		Object[][] array = gson.fromJson(jsonElement, Object[][].class);

		for (Object[] objects : array) {
			for (Object object : objects) {
				System.err.println(object);
			}
		}
	}

	@Test
	public void testGetTorrentFile() throws Exception {
		TrackerConnection connection = new TrackerConnection("symbols.pdf", "127.0.0.1:8080", 9000);
		assertTrue(connection.isAlive());
	}

	@Test
	public void testSendTorrentFile() throws Exception {
		String result = TrackerConnection.sendTorrentFile("D:\\Project\\symbols.pdf", "http://127.0.0.1:8080");
		System.err.println(result);
		assertNotNull(result);
	}

	@Test
	public void getSendTorrentFile() throws Exception {
		TrackerConnection result = new TrackerConnection("84a2771d52b9269690be3217a35b5b3c9b1aaa4bf35143ba47ecde26948d07b5", "http://127.0.0.1:8080", 9000);
		//result.aliveThread.join();
		assertTrue(result.isAlive());		
	}
}
