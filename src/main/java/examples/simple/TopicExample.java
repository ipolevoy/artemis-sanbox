package examples.simple;

import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreAddressConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;
import java.util.Hashtable;

import static examples.EmbeddedConfig.createServerConfiguration;


public class TopicExample {

    static EmbeddedActiveMQ server;
    static String TOPIC_NAME = "TOPIC_123";
    static ActiveMQConnectionFactory connectionFactory;
    static Topic topic;

    public static void main(String[] args) throws Exception {

        configureServer();
        configureClient();
        registerListener(topic);

        sendMessage(topic);

    }

    private static void configureServer() throws Exception {

        Configuration configuration = createServerConfiguration();


        CoreAddressConfiguration coreAddressConfiguration = new CoreAddressConfiguration();
        coreAddressConfiguration
                .setName(TOPIC_NAME)
                .addRoutingType(RoutingType.MULTICAST);
        configuration.addAddressConfiguration(coreAddressConfiguration);

        server = new EmbeddedActiveMQ();
        server.setConfiguration(configuration);
        server.start();

        server.getActiveMQServer().getAddressSettingsRepository().addMatch("#", new AddressSettings()
                .setAutoCreateQueues(false)
                .setAutoCreateAddresses(false)
                .setAutoDeleteQueues(false)
                .setAutoDeleteAddresses(false));
    }

    static void configureClient() throws NamingException {
        connectionFactory = new ActiveMQConnectionFactory("vm://0");

        Hashtable<String, String> jndi = new Hashtable<>();
        jndi.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        jndi.put("topic.topic/" + TOPIC_NAME, TOPIC_NAME);
        InitialContext initialContext = new InitialContext(jndi);
        topic = (Topic) initialContext.lookup("topic/" + TOPIC_NAME);
    }

    static void sendMessage(Topic topic) {
        try (Connection connection = connectionFactory.createConnection()) {
            try (Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                MessageProducer producer = jmsSession.createProducer(topic);
                connection.start();
                TextMessage message = jmsSession.createTextMessage("Hello -> " + new Date());
                producer.send(message);
                System.out.println("Message sent: " + message.getText());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    static void registerListener(Topic topic) throws JMSException {
        Connection connection = connectionFactory.createConnection();

        Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = jmsSession.createConsumer(topic);
        connection.start();
        consumer.setMessageListener(message -> {
            try {
                System.out.println("Received message by consumer: \"" + ((TextMessage) message).getText() + "\" on thread: " + Thread.currentThread());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("Starting to listen...");
    }
}
