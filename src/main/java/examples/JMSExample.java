package examples;


import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import javax.jms.*;
import javax.naming.InitialContext;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

public class JMSExample {

    public static void main(String[] args) throws Exception {
        EmbeddedActiveMQ server = EmbeddedConfig.configureServer();

        server.start();

        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMConnectorFactory.class.getName());
        ConnectionFactory connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        Hashtable<String, String> jndi = new Hashtable<>();
        jndi.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        jndi.put("connectionFactory.ConnectionFactory", "vm://0");
        jndi.put("queue.queue/exampleQueue", "exampleQueue");

        InitialContext initialContext = new InitialContext(jndi);
        Queue jmsQueue = (Queue) initialContext.lookup("queue/exampleQueue");

        try (Connection connection = connectionFactory.createConnection()) {


            Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer producer = jmsSession.createProducer(jmsQueue);


            //QUEUE CONTROLS ARE AVAILABLE ONLY AFTER CREATION OF A PRODUCER!
            System.out.println("Queue Controls: " + server.getActiveMQServer().getManagementService().getResources(QueueControl.class).length);
            MessageConsumer messageConsumer = jmsSession.createConsumer(jmsQueue);

            connection.start(); // must have for delivery

            //SENDING 2 MESSAGES
            TextMessage message = jmsSession.createTextMessage("Hello 1");
            System.out.println("Sending message: " + message.getText());
            producer.send(message, 2, 5, 0);

            TextMessage message2 = jmsSession.createTextMessage("Hello 2");
            System.out.println("Sending message: " + message2.getText());
            producer.send(message2, 2, 5, 0);

            producer.close();

            //QueueControl usage
            QueueControl qc = (QueueControl) server.getActiveMQServer().getManagementService().getResources(QueueControl.class)[0];
            System.out.println("Messages in queue after sending 2 messages: " + qc.getMessageCount());


            browseJMSMessages(jmsSession, jmsQueue);


            //RECEIVING A MESSAGE
            Message messageReceived = messageConsumer.receive();
            message.acknowledge();
            System.out.println("Received message: " + ((TextMessage) messageReceived).getText());

            QueueControl qc1 = (QueueControl) server.getActiveMQServer().getManagementService().getResources(QueueControl.class)[0];
            System.out.println("Messages in queue: " + qc1.getMessageCount());


            //RECEIVING A MESSAGE
            messageReceived = messageConsumer.receive();
            message.acknowledge();
            System.out.println("Received message: " + ((TextMessage) messageReceived).getText());


            messageConsumer.close();

            qc1 = (QueueControl) server.getActiveMQServer().getManagementService().getResources(QueueControl.class)[0];
            System.out.println("Messages in queue: " + qc1.getMessageCount());

        } finally {

            // Step 6. Stop the broker
            System.out.println("Stopping the JMS Server: " + new Date());
            server.stop();
            System.out.println("Stopped the JMS Server: " + new Date());
        }
    }

    private static void browseJMSMessages(Session jmsSession, javax.jms.Queue jmsQueue) throws JMSException {
        int count = 0;
        //Browsing messages
        QueueBrowser queueBrowser = jmsSession.createBrowser(jmsQueue);
        Enumeration enumeration = queueBrowser.getEnumeration();
        while (enumeration.hasMoreElements()) {
            enumeration.nextElement();
            count++;

        }
        System.out.println("Browser found: " + count + " messages");
    }
}
