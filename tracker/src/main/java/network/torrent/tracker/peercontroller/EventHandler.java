package network.torrent.tracker.peercontroller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.stereotype.Service;

import network.torrent.tracker.torrentrepository.session.SessionDocument;
import network.torrent.tracker.torrentrepository.session.SessionRepository;

@Service
public class EventHandler {
    @Autowired
    private SessionRepository sessionRepository;

    private List<SessionDocument> getPeerIds(String infoHash) {
        Example<SessionDocument> example = Example.of(
                new SessionDocument(infoHash, null, 0, false, null),
                ExampleMatcher.matching()
                        .withMatcher("infoHash",
                                ExampleMatcher.GenericPropertyMatchers
                                        .exact()));

        List<SessionDocument> sessions = sessionRepository.findAll(example);

        return sessions;
    }

    public List<SessionDocument> handleStartedEvent(String infoHash, String peerId, int port, String ip) {
        List<SessionDocument> sessions = getPeerIds(infoHash);

        sessionRepository.save(
                new SessionDocument(infoHash, peerId, port, false, ip));

        return sessions;
    }

    public List<SessionDocument> handleStoppedEvent(String infoHash, String peerId) {
        sessionRepository.deleteById(peerId);

        return getPeerIds(infoHash);
    }

    public List<SessionDocument> handleCompletedEvent(String infoHash, String peerId, int port, String ip) {
        Optional<SessionDocument> session = sessionRepository.findById(peerId);
        SessionDocument newSession;

        if (session.isPresent()) {
            sessionRepository.deleteById(peerId);

            newSession = session.get();
            newSession.setCompleted(true);
            newSession.resetCreatedAt();
        } else {
            newSession = new SessionDocument(infoHash, peerId, port, true, ip   );
        }

        List<SessionDocument> sessions = getPeerIds(infoHash);

        sessionRepository.save(newSession);

        return sessions;
    }

    public List<SessionDocument> handleAliveEvent(String infoHash, String peerId, int port, boolean completed, String ip) {
        Optional<SessionDocument> session = sessionRepository.findById(peerId);
        SessionDocument newSession;

        if (session.isPresent()) {
            sessionRepository.deleteById(peerId);

            newSession = session.get();
            newSession.resetCreatedAt();
        } else {
            newSession = new SessionDocument(infoHash, peerId, port, completed, ip);
        }
        List<SessionDocument> sessions = getPeerIds(infoHash);

        sessionRepository.save(newSession);

        return sessions;
    }
}
