package examples;

import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.javalite.common.Util;

import javax.jms.Queue;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

public class JMSExampleJNDI_GUI extends JFrame {


    static String QUEUE_NAME = "QUEUE_123";
    private EmbeddedActiveMQ server;
    private ConnectionFactory connectionFactory;
    private Queue jmsQueue;

    private java.util.List<Session> listenerSessions = new ArrayList();
    private java.util.List<MessageConsumer> messageConsumers = new ArrayList<>();
    private Connection consumerConnection;

    private JMSExampleJNDI_GUI(String title) throws Exception {
        super(title);

        JPanel panel = new JPanel(new FlowLayout());
        JButton sendB = new JButton("Send message");
        JButton receiveB = new JButton("Receive 1 message");
        JButton registerListenerB = new JButton("Register message listener");
        JButton browseB = new JButton("Browse message");

        JTextField messageField = new JTextField("enter your message");
        messageField.setColumns(20);
        JButton queueControlCountBcountB = new JButton("Get count from QueueControl (read on console)");


        panel.add(new JLabel("Send a message: "));
        panel.add(messageField);
        panel.add(sendB);
        panel.add(receiveB);
        panel.add(queueControlCountBcountB);
        panel.add(registerListenerB);
        panel.add(browseB);
        add(panel);
        pack();
        setSize(600, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        addActionListeners(sendB, receiveB, messageField, queueControlCountBcountB, registerListenerB, browseB);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    JMSExampleJNDI_GUI.this.listenerSessions.forEach(Util::closeQuietly);
                    JMSExampleJNDI_GUI.this.messageConsumers.forEach(Util::closeQuietly);
                    Util.closeQuietly(JMSExampleJNDI_GUI.this.consumerConnection);
                    server.stop();
                } catch (Exception ignore) {
                }
            }
        });

        //Artemis code:
        Configuration configuration = EmbeddedConfig.createServerConfiguration();

//        //the following three lines have no effect
        CoreQueueConfiguration coreQueueConfiguration = new CoreQueueConfiguration();
        coreQueueConfiguration.setName(QUEUE_NAME).setDurable(true);
        configuration.addQueueConfiguration(coreQueueConfiguration);

        server = new EmbeddedActiveMQ();


        server.setConfiguration(configuration);
        server.start();

        server.getActiveMQServer().getAddressSettingsRepository().addMatch("#", new AddressSettings()
                .setAutoCreateQueues(true)
                .setAutoCreateAddresses(true)
                .setAutoDeleteQueues(false)
                .setAutoDeleteAddresses(false));

        connectionFactory = new ActiveMQConnectionFactory("vm://0");


        Hashtable<String, String> jndi = new Hashtable<>();
        jndi.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        jndi.put("connectionFactory.ConnectionFactory", "vm://0");
        //# queue.[jndiName] = [physicalName]
        jndi.put("queue.queue/" + QUEUE_NAME, QUEUE_NAME);

        InitialContext initialContext = new InitialContext(jndi);
        jmsQueue = (Queue) initialContext.lookup("queue.queue/" + QUEUE_NAME);
    }

    private void addActionListeners(JButton sendB, JButton receiveB, JTextField messageField, JButton queueControlCountB, JButton registerListenerB, JButton browseB) {
        sendB.addActionListener(e -> {
            try (Connection connection = connectionFactory.createConnection()) {
                try (Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                    MessageProducer producer = jmsSession.createProducer(jmsQueue);
                    connection.start();
                    TextMessage message = jmsSession.createTextMessage(messageField.getText());
                    producer.send(message);
                    System.out.println("Message sent: " + message.getText());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        receiveB.addActionListener(e -> {

            Runnable r = () -> {
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
            };
            new Thread(r).start();
        });

        queueControlCountB.addActionListener(e -> {

            Object[] resources = server.getActiveMQServer().getManagementService().getResources(QueueControl.class);
            if (resources.length == 0) {
                System.err.println("QueueControl ERROR: no queue controls found!!!");
            } else {
                QueueControl qc = (QueueControl) resources[0];

                System.out.println("Found QueueControl: " + qc.getName());
                System.out.println("QueueControl: Messages in queue: " + qc.getMessageCount());
            }

        });


        registerListenerB.addActionListener(e -> {

            try  {
                consumerConnection = connectionFactory.createConnection();
                Session jmsSession = consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                this.listenerSessions.add(jmsSession);

                MessageConsumer consumer = jmsSession.createConsumer(jmsQueue);
                messageConsumers.add(consumer);
                consumer.setMessageListener(message -> {

                    try {
                        System.out.println("MessageListener: got message:  " + ((TextMessage) message).getText());
                    } catch (JMSException e1) {
                        e1.printStackTrace();
                    }
                });

                consumerConnection.start();

            } catch (InvalidDestinationException e1) {
                System.err.println("Browser ERROR: did not find this queue!!!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.out.println("Registered the listener ");

        });

        browseB.addActionListener(e -> {

            try (Connection connection = connectionFactory.createConnection()) {
                try (Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                    QueueBrowser queueBrowser = jmsSession.createBrowser(jmsQueue);
                    Enumeration enumeration = queueBrowser.getEnumeration();
                    int count = 0;

                    while (enumeration.hasMoreElements()) {
                        TextMessage tm = (TextMessage) enumeration.nextElement();
                        System.out.println("Browser found a message: " + tm.getText());
                        count++;
                    }

                    System.out.println("Browsed total messages: " + count);
                }
            } catch (InvalidDestinationException e1) {
                System.err.println("Browser ERROR: did not find this queue!!!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }


    public static void main(String[] args) throws Exception {
        JMSExampleJNDI_GUI frame = new JMSExampleJNDI_GUI("Artemis Sandbox");
        frame.setVisible(true);
    }
}

