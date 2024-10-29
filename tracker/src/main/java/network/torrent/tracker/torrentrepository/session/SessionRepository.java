package network.torrent.tracker.torrentrepository.session;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<SessionDocument, String> {

}
