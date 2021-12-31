package examples.queue;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import javax.jms.*;
import javax.naming.InitialContext;
import java.util.Hashtable;

class QueueLookup {
    private static final String LOCATION = "./target/artemis";

    private static EmbeddedActiveMQ server;

    public static void main(String[] args) throws Exception {
        try{
            Configuration configuration = new ConfigurationImpl()
                    .setPersistenceEnabled(true)
                    .setBindingsDirectory(LOCATION + "/bindings")
                    .setJournalDirectory(LOCATION + "/journal")
                    .setLargeMessagesDirectory(LOCATION + "/largemessages")
                    .setPagingDirectory(LOCATION + "/paging")
                    .setSecurityEnabled(false)
                    .addAcceptorConfiguration("invm", "vm://0")
                    .setJournalBufferTimeout_AIO(100)
                    .setJournalBufferTimeout_NIO(100)
                    .setJournalType(JournalType.NIO)
                    .setMaxDiskUsage(90);


            //the following three lines have no effect
            CoreQueueConfiguration coreQueueConfiguration = new CoreQueueConfiguration();
            coreQueueConfiguration.setName("Queue123").setDurable(true);
            configuration.addQueueConfiguration(coreQueueConfiguration);


            server = new EmbeddedActiveMQ();
            server.setConfiguration(configuration);
            server.start();


            TransportConfiguration transportConfiguration = new TransportConfiguration(InVMConnectorFactory.class.getName());
            ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

            Hashtable<String, String> jndi = new Hashtable<>();
            jndi.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
            jndi.put("connectionFactory.ConnectionFactory", "vm://0");
            //# queue.[jndiName] = [physicalName]
            //jndi.put("queue.queue/Queue123", "Queue123");

            InitialContext initialContext = new InitialContext(jndi);
            Queue jmsQueue = (Queue) initialContext.lookup("queue/Queue123");

            try (Connection connection = connectionFactory.createConnection()) {
                try(Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)){
                    MessageProducer producer = jmsSession.createProducer(jmsQueue);
                    connection.start();
                    TextMessage message = jmsSession.createTextMessage("Hello, Artemis!");
                    producer.send(message);
                    System.out.println("Message sent: " + message.getText());
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }

        }finally {
            server.stop();
        }
    }
}
