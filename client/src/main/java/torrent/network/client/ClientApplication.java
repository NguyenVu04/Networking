package torrent.network.client;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import torrent.network.client.connectionmanager.ConnectionManager;

@SpringBootApplication
public class ClientApplication {
	public static List<ConnectionManager> connections = new CopyOnWriteArrayList<>();

	public static void main(String[] args) {
		SpringApplication.run(ClientApplication.class, args);
	}

}
