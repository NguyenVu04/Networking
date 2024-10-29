package network.torrent.tracker.torrentrepository.filesystem;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
public class TorrentFileSystemConfig extends AbstractMongoClientConfiguration  {

    @Override
    protected String getDatabaseName() {
        return "Torrent";
    }

    @Bean
    public GridFsTemplate gridFsTemplate(@Lazy MappingMongoConverter mappingMongoConverter) throws Exception {
        return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter);
    }
}
