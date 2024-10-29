package network.torrent.tracker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

@SpringBootTest
class TrackerApplicationTests {
	@Autowired
	private GridFsTemplate gridFsTemplate;

	@Test
	void testGridFS() throws Exception {
		DBObject metaData = new BasicDBObject();
		metaData.put("user", "alex");
		InputStream inputStream = new FileInputStream("./exception.log");
		String id = gridFsTemplate.store(inputStream, "exception.log", "text/plain", metaData).toString();
		GridFsResource[] resources = gridFsTemplate.getResources("exception.log");
		BufferedInputStream bis = new BufferedInputStream(resources[0].getInputStream());
		assertNotNull(id);
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("./exception123.log"));
		byte[] bytes = new byte[1024];
		int len = 0;
		while ((len = bis.read(bytes)) != -1) {
			bos.write(bytes, 0, len);
		}
		bos.flush();
		bos.close();
		bis.close();
	}

}
