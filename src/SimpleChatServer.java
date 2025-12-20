import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleChatServer {

    private final List<PrintWriter> clientWriters = new ArrayList<>();
    private final Gson gson = new Gson();

    public static void main(String[] args) {
        new SimpleChatServer().go();
    }

    public void go() {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        try {
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(5000));

            System.out.println("Server listening at port 5000");

            while (serverSocketChannel.isOpen()) {
                SocketChannel clientSocket = serverSocketChannel.accept();
                PrintWriter writer = new PrintWriter(Channels.newWriter(clientSocket, UTF_8));
                clientWriters.add(writer);
                threadPool.submit(new ClientHandler(clientSocket));
                System.out.println("Got a connection");;
            }

        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }

    private void tellEveryone(ServerMessage message) {
        for (PrintWriter writer: clientWriters) {
            String json = gson.toJson(message);
            writer.println(json);
            writer.flush();
        }
    }


    public class ClientHandler implements Runnable{

        BufferedReader reader;
        SocketChannel socket;

        public ClientHandler(SocketChannel clientSocket) {
            socket = clientSocket;
            reader = new BufferedReader(Channels.newReader(socket, UTF_8));
        }

        public void run() {
            String jsonString;
            try {
                while((jsonString = reader.readLine()) != null) {
                    ClientMessage message = gson.fromJson(jsonString, ClientMessage.class);
                    ServerMessage messageWithTimestamp = new ServerMessage(message.Type(), message.from(), Instant.now().toEpochMilli(), message.data());
                    tellEveryone(messageWithTimestamp);
                }

            } catch(SocketException ex) {
                System.out.println("Client disconnected");
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    
     }

}

