package torrent.network.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import torrent.network.client.peerconnection.MessageType;
import torrent.network.client.peerconnection.PeerMessage;
import torrent.network.client.peerconnection.uploader.SingleFileApp;
import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentdigest.TorrentDigest;
import torrent.network.client.torrentexception.ExceptionHandler;
import torrent.network.client.trackerconnection.TrackerConnection;

import java.security.MessageDigest;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
@SpringBootTest
class ClientApplicationTests {
	@Test
	public void testGenerateSingleFileTorrent() throws Exception {
		TorrentBuilder torrentBuilder = new TorrentBuilder("http://example.com/announce");
		byte[] torrent = torrentBuilder.generateSingleFileTorrent("./test.txt");

		MessageDigest md = MessageDigest.getInstance(TorrentBuilder.HASH_ALGORITHM);
		md.update("This is a test".getBytes());
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

		byte[] digest1 = md.digest(s1.getBytes());

		md.reset();

		byte[] digest2 = md.digest(s2.getBytes());

		ByteBuffer byteBuffer = ByteBuffer.allocate(digest1.length + digest2.length);
		byteBuffer.put(digest1);
		byteBuffer.put(digest2);

		byte[] digest3 = byteBuffer.array();

		TorrentDigest td1 = new TorrentDigest(digest3);
		boolean result = td1.verify(s2.getBytes(), 1);
		assertTrue(result);
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

	@Test
	public void testGetFile() throws Exception {
		File file = new File("D:\\Project\\symbols.pdf");
		assertEquals(file.getAbsolutePath() + "." + 1, "D:\\Project\\symbols.pdf.1");
	}

	@Test
	public void getMessageType() throws Exception {
		byte[] infoHash = new byte[20];
		byte[] message = PeerMessage.createHandshakeMessage(infoHash, new byte[20]);
		MessageType messageType = PeerMessage.getMessageType(message);
		assertEquals(MessageType.HANDSHAKE, messageType);
		assertTrue(Arrays.equals(infoHash, PeerMessage.getInfoHash(message)));
		

		byte[] message2 = PeerMessage.createKeepAliveMessage();
		MessageType messageType2 = PeerMessage.getMessageType(message2);
		assertEquals(MessageType.KEEP_ALIVE, messageType2);

		byte[] message3 = PeerMessage.createChokeMessage();
		MessageType messageType3 = PeerMessage.getMessageType(message3);
		assertEquals(MessageType.CHOKE, messageType3);

		byte[] message4 = PeerMessage.createUnchokeMessage();
		MessageType messageType4 = PeerMessage.getMessageType(message4);
		assertEquals(MessageType.UNCHOKE, messageType4);

		byte[] message5 = PeerMessage.createInterestedMessage();
		MessageType messageType5 = PeerMessage.getMessageType(message5);
		assertEquals(MessageType.INTERESTED, messageType5);

		byte[] message6 = PeerMessage.createNotInterestedMessage();
		MessageType messageType6 = PeerMessage.getMessageType(message6);
		assertEquals(MessageType.NOT_INTERESTED, messageType6);

		byte[] message7 = PeerMessage.createHaveMessage(1);
		MessageType messageType7 = PeerMessage.getMessageType(message7);
		assertEquals(MessageType.HAVE, messageType7);

		byte[] message8 = PeerMessage.createRequestMessage(1, 1, 1);
		MessageType messageType8 = PeerMessage.getMessageType(message8);
		assertEquals(MessageType.REQUEST, messageType8);

		byte[] message9 = PeerMessage.createPieceMessage(1, 1, new byte[1]);
		MessageType messageType9 = PeerMessage.getMessageType(message9);
		assertEquals(MessageType.PIECE, messageType9);

		byte[] message10 = PeerMessage.createCancelMessage(1, 1, 1);
		MessageType messageType10 = PeerMessage.getMessageType(message10);
		assertEquals(MessageType.CANCEL, messageType10);

		byte[] message11 = PeerMessage.createPortMessage((short) 1);
		MessageType messageType11 = PeerMessage.getMessageType(message11);
		assertEquals(MessageType.PORT, messageType11);
	}

	@Test
	public void testFile() throws Exception {
		try (BufferedInputStream input = new BufferedInputStream(new FileInputStream("D:\\Project\\symbols.pdf"))) {
			int byteRead = -1;
			byte[] buffer = new byte[1024];
			int i = 0;
			File file = new File("D:\\symbols.pdf");
			file.mkdirs();
			while ((byteRead = input.read(buffer, 0, buffer.length)) != -1) {
				try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("D:\\symbols.pdf\\symbols.pdf." + i, false))) {
					output.write(buffer, 0, byteRead);
					output.flush();
					i++;
				}
			}
		} catch (Exception e) {
			ExceptionHandler.handleException(e);
		}
	}

	@Test
	public void testMergeFile() throws Exception {
		File src = new File("D:\\symbols.pdf");

		for (int i = 0; i < 256; i++) {
			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream("D:\\symbols.pdf\\symbols.pdf." + i))) {
				try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream("D:\\symbols1.pdf", true))) {
					byte[] buffer = input.readAllBytes();
					output.write(buffer);
				}
				
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	@Test
	public void testSingleFile() throws Exception {
		byte[] infoHash1 = new byte[20];
		Arrays.fill(infoHash1, (byte) 1);
		byte[] infoHash2 = new byte[20];
		Arrays.fill(infoHash2, (byte) 2);
		SingleFileApp.serveFile("D:\\symbols.pdf", infoHash1, 256);
		SingleFileApp.serveFile("D:\\test\\symbols.pdf", infoHash2, 256);

		for (int i = 0; i < 256; i++) {
			byte[] input = SingleFileApp.getPiece(infoHash1, i);

			SingleFileApp.savePiece(infoHash2, i, input);
		}
	}
}
