package examples;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;
import org.apache.activemq.artemis.api.jms.JMSFactoryType;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import javax.jms.Queue;
import javax.jms.*;
import javax.naming.InitialContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Hashtable;

public class JMSExampleGUI extends JFrame{

    private EmbeddedActiveMQ server;
    private ConnectionFactory connectionFactory;
    private Queue jmsQueue;

    private JMSExampleGUI(String title) throws Exception {
        super(title);

        JPanel panel = new JPanel(new FlowLayout());
        JButton sendB = new JButton("Send message");
        JButton receiveB = new JButton("Receive 1 message");
        JButton registerListenerB = new JButton("Register message listener");
        JButton browseB = new JButton("Browse message");

        JTextField messageField = new JTextField("enter your message");
        messageField.setColumns(20);
        JButton countB =  new JButton("Get count from QueueControl (read on console)");



        panel.add(new JLabel("Send a message: "));
        panel.add(messageField);
        panel.add(sendB);
        panel.add(receiveB);
        panel.add(countB);
        panel.add(registerListenerB);
        panel.add(browseB);
        add(panel);
        pack();
        setSize(600, 200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        addActionListeners(sendB, receiveB, messageField, countB, registerListenerB, browseB);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    server.stop();
                } catch (Exception ignore) {}
            }
        });

        //Artemis code:
        server = EmbeddedConfig.configureServer();
        server.start();

        TransportConfiguration transportConfiguration = new TransportConfiguration(InVMConnectorFactory.class.getName());
        connectionFactory = ActiveMQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);

        Hashtable<String, String> jndi = new Hashtable<>();
        jndi.put("java.naming.factory.initial", "org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory");
        jndi.put("connectionFactory.ConnectionFactory", "vm://0");
        jndi.put("queue.queue/exampleQueue", "exampleQueue");

        InitialContext initialContext = new InitialContext(jndi);
        jmsQueue = (Queue) initialContext.lookup("queue/exampleQueue");
    }

    private void addActionListeners(JButton sendB, JButton receiveB, JTextField messageField, JButton countB, JButton registerListenerB, JButton browseB) {
        sendB.addActionListener(e -> {
            try (Connection connection = connectionFactory.createConnection()) {
                try(Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)){
                    MessageProducer producer = jmsSession.createProducer(jmsQueue);
                    connection.start();
                    TextMessage message = jmsSession.createTextMessage(messageField.getText());
                    producer.send(message);
                    System.out.println("Message sent: " + message.getText());
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        });

        receiveB.addActionListener(e -> {

            Runnable r = () -> {
                try (Connection connection = connectionFactory.createConnection()) {
                    try(Session jmsSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)){
                        MessageConsumer consumer = jmsSession.createConsumer(jmsQueue);
                        connection.start();

                        System.out.println("Starting to receive on: " + Thread.currentThread());
                        TextMessage message = (TextMessage)consumer.receive();
                        System.out.println("Received message by consumer: \"" + message.getText() + "\" on thread: " + Thread.currentThread());
                    }
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            };
            new Thread(r).start();
        });

        countB.addActionListener(e -> {

            Object[]  resources = server.getActiveMQServer().getManagementService().getResources(QueueControl.class);
            if(resources.length == 0){
                System.err.println("QueueControl ERROR: no queue controls found!!!" );
            }else{
                QueueControl qc = (QueueControl)resources[0];
                System.out.println("Messages in queue: " + qc.getMessageCount());
            }

        });

        registerListenerB.addActionListener(e -> {
            System.out.println("TBD");
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
            }catch (InvalidDestinationException e1){
                System.err.println("Browser ERROR: did not find this queue!!!");
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
        });

    }

    public static void main(String[] args) throws Exception {
        JMSExampleGUI frame= new JMSExampleGUI("Artemis Sandbox");
        frame.setVisible(true);
    }
}

