package examples;

import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;


class EmbeddedConfig {
    private static final String LOCATION = "./target/artemis";

    static Configuration createServerConfiguration() throws Exception {

       Configuration configuration = new ConfigurationImpl()
                .setPersistenceEnabled(true)
                .setBindingsDirectory(LOCATION + "/bindings")
                .setJournalDirectory(LOCATION + "/journal")
                .setLargeMessagesDirectory(LOCATION+ "/largemessages")
                .setPagingDirectory(LOCATION + "/paging")
                .setSecurityEnabled(false)
                .addAcceptorConfiguration("invm", "vm://0")
                .setJournalBufferTimeout_AIO(100)
                .setJournalBufferTimeout_NIO(100)
                .setJournalType(JournalType.NIO)
                .setMaxDiskUsage(90);


        return configuration;
    }
}
