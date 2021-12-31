package examples.simple;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Date;
import java.util.Hashtable;

import static examples.EmbeddedConfig.createServerConfiguration;

/**
 * Created this class as the most simplistic example of embedded server in otrder to use the same code structure to write a Pub/Sub example called TopicExample
 */
public class QueueExample {

    static EmbeddedActiveMQ server;
    static String QUEUE_NAME = "QUEUE_123";
    static ActiveMQConnectionFactory connectionFactory;
    static Queue queue;

    public static void main(String[] args) throws Exception {

        configureServer();
        configureClient();
        sendMessage(queue);
        receiveMessage(queue);
    }

    private static void configureServer() throws Exception {

        Configuration configuration = createServerConfiguration();

        QueueConfiguration coreQueueConfiguration = new QueueConfiguration(QUEUE_NAME);
        coreQueueConfiguration.setAddress(QUEUE_NAME)
                .setName(QUEUE_NAME)
                .setDurable(true)
                .setRoutingType(RoutingType.ANYCAST);
        configuration.addQueueConfiguration(coreQueueConfiguration);

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
        jndi.put("queue.queue/" + QUEUE_NAME, QUEUE_NAME);
        InitialContext initialContext = new InitialContext(jndi);
        queue = (Queue) initialContext.lookup("queue/" + QUEUE_NAME);
    }

     static void  sendMessage(Queue jmsQueue){
         try (Connection connection = connectionFactory.createConnection()) {
             try (Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                 MessageProducer producer = jmsSession.createProducer(jmsQueue);
                 connection.start();
                 TextMessage message = jmsSession.createTextMessage("Hello -> " + new Date());
                 producer.send(message);
                 System.out.println("Message sent: " + message.getText());
             }
         } catch (Exception ex) {
             ex.printStackTrace();
         }
     }


     static void receiveMessage(Queue jmsQueue){
         try (Connection connection = connectionFactory.createConnection()) {
             try (Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                 MessageConsumer consumer = jmsSession.createConsumer(jmsQueue);
                 connection.start();

                 System.out.println("Starting to receive on: " + Thread.currentThread());
                 TextMessage message = (TextMessage) consumer.receive();
                 System.out.println("Received message by consumer: \"" + message.getText() + "\" on thread: " + Thread.currentThread());
             }
         } catch (Exception ex) {
             ex.printStackTrace();
         }
     }

     static void closeAll() throws Exception {

        server.stop();
     }


}
