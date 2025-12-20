import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import java.time.Instant;


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

        ClientMessage message = new ClientMessage(MessageType.CHAT, userName, Map.of("content", outgoing.getText()));
        String json = gson.toJson(message);
        writer.println(json);
        writer.flush();
        outgoing.setText("");
        outgoing.requestFocus();

    }

    private void setUserName() {
        ClientMessage message = new ClientMessage(MessageType.UPDATE_NAME, userName, Map.of("old", userName, "new", userNameField.getText()));
        String json = gson.toJson(message);
        writer.println(json);
        writer.flush();
        userName = userNameField.getText();
        userNameField.setText("");
        userNameField.requestFocus();
    }

    public class IncomingReader implements Runnable {

        public void run() {
            String jsonString;
            try {

                while((jsonString = reader.readLine()) != null) {
                    ServerMessage messageObj = gson.fromJson(jsonString, ServerMessage.class);
                    String message = handleMessage(messageObj);
                    incoming.append(message + "\n");
                }

            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String handleMessage(ServerMessage message) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
        String time = formatter.format(Date.from(Instant.ofEpochMilli(message.createdAt())));
        switch (message.Type()) {
            case CHAT: return time + " " + message.from().toUpperCase() + ": " + message.data().get("content");
            case UPDATE_NAME: return time + " " + message.data().get("old").toUpperCase() + " changed his name to: " + message.data().get("new").toUpperCase();
            default: return "Error";      
        }
    }

    public static void main(String[] args) {
        new SimpleChatClient().go();
    }

}
