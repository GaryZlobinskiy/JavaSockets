
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ChatServer {
    public static final int PORT = 59000;
    private static final ArrayList<ClientConnectionData> clientList = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(100);

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Chat Server started.");
            System.out.println("Local IP: "
                    + Inet4Address.getLocalHost().getHostAddress());
            System.out.println("Local Port: " + serverSocket.getLocalPort());

            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.printf("Connected to %s:%d on local port %d\n",
                            socket.getInetAddress(), socket.getPort(), socket.getLocalPort());

                    //handle client business in another thread
                    pool.execute(new ClientHandler(socket));
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

            }
        }
    }

    // Inner class
    static class ClientHandler implements Runnable {
        // Maintain data about the client serviced by this thread
        Socket socket;
        ClientConnectionData client;

        public ClientHandler(Socket socket) {
            //Note: This constructor runs on the MAIN thread.
            // This code should really be done in the separate thread
            this.socket = socket;
        }

        public void setup() throws Exception {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            String name = socket.getInetAddress().getHostName();

            client = new ClientConnectionData(socket, in, out, name);
            synchronized(clientList){
                clientList.add(client);
            }
            System.out.println("added client " + name);
        }

        /**
         * Broadcasts a message to all clients connected to the server.
         */
        public void broadcast(String msg) {
            try {
                System.out.println("Broadcasting -- " + msg);
                synchronized (clientList) {
                    for (ClientConnectionData c : clientList){
                        c.getOut().println(msg);
                    }
                }
            } catch (Exception ex) {
                System.out.println("broadcast caught exception: " + ex);
                ex.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                setup();
                BufferedReader in = client.getInput();
                //Step 1 of protocol: Get user name, and broadcast Welcome!
                String userName = in.readLine().trim();
                client.setUserName(userName);
                broadcast(String.format("WELCOME %s", client.getUserName()));

                String incoming = "";
                while ( (incoming = in.readLine()) != null) {
                    if (incoming.startsWith("CHAT")) {
                        String chat = incoming.substring(4).trim();
                        if (chat.length() > 0) {
                            broadcast(String.format("CHAT %s: %s",
                                    client.getUserName(), chat));
                        }
                    } else if (incoming.startsWith("QUIT")){
                        break;
                    }
                }

            } catch (Exception ex) {
                if (ex instanceof SocketException) {
                    System.out.println("Caught socket ex for " +
                            client.getName());
                } else {
                    System.out.println(ex);
                    ex.printStackTrace();
                }
            } finally {

                synchronized (clientList) {
                    clientList.remove(client);
                }
                System.out.println(client.getName() + " has left.");
                broadcast(String.format("EXIT %s", client.getUserName()));
                try {
                    client.getSocket().close();
                } catch (IOException ex) {}

            }
        }

    }

}