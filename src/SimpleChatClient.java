import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleChatClient {
    private JTextArea incoming;
    private JTextField outgoing;
    private JTextField userNameField;
    private BufferedReader reader;
    private PrintWriter writer;
    private String userName;
    private Gson gson = new Gson();

    public SimpleChatClient() {
        this.userName = "Guest" + ThreadLocalRandom.current().nextInt(1000);
    }


    public void go() {
        setUpNetworking();

        JScrollPane scroller = createScrollableTextArea();

        outgoing = new JTextField(20);
        userNameField = new JTextField(20);
        JButton sendButton = new JButton("Send");
        JButton setUserNameButton = new JButton("Set user name");
        sendButton.addActionListener(e -> sendMessage());
        setUserNameButton.addActionListener(e -> setUserName());

        JPanel mainPanel = new JPanel();
        mainPanel.add(scroller);
        mainPanel.add(outgoing);
        mainPanel.add(sendButton);
        mainPanel.add(userNameField);
        mainPanel.add(setUserNameButton);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new IncomingReader());

        JFrame frame = new JFrame("Simple Chat Client ");
        frame.getContentPane().add(BorderLayout.CENTER, mainPanel);
        frame.setSize(450, 450);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }


    public void setUpNetworking() {

        try {
            InetSocketAddress serverAdress = new InetSocketAddress("localhost", 5000);
            SocketChannel socketChannel = SocketChannel.open(serverAdress);

            reader = new BufferedReader(Channels.newReader(socketChannel, UTF_8));
            writer = new PrintWriter(Channels.newWriter(socketChannel, UTF_8));

            System.out.println("Network established");

        } catch(IOException ex) {
            ex.printStackTrace();
        }

    }

    public JScrollPane createScrollableTextArea() {

        incoming = new JTextArea(15, 30);
        incoming.setLineWrap(true);
        incoming.setWrapStyleWord(true);
        incoming.setEditable(false);
        JScrollPane scroller = new JScrollPane(incoming);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroller;

    }

    private void sendMessage() {

        Message message = new Message(MessageType.CHAT, userName, 0L, Map.of("content", outgoing.getText()));
        String json = gson.toJson(message);
        writer.println(json);
        writer.flush();
        outgoing.setText("");
        outgoing.requestFocus();

    }

    private void setUserName() {
        Message message = new Message(MessageType.UPDATE_NAME, userName, 0L, Map.of("old", userName, "new", userNameField.getText()));
        writer.println(message);
        writer.flush();
        userName = userNameField.getText();
        userNameField.setText("");
        userNameField.requestFocus();
    }

    public class IncomingReader implements Runnable {

        public void run() {
            String message;
            try {

                while((message = reader.readLine()) != null) {
                    System.out.println("read " + message);
                    incoming.append(message + "\n");
                }

            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new SimpleChatClient().go();
    }

}
