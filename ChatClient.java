// By Ajay and Gary

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static Socket socket;
    private static BufferedReader socketIn;
    private static PrintWriter out;

    public static void main(String[] args) throws Exception {
        Scanner userInput = new Scanner(System.in);

        System.out.println("What's the server IP? ");
        String serverip = userInput.nextLine();
        System.out.println("What's the server port? ");
        int port = userInput.nextInt();
        userInput.nextLine();

        socket = new Socket(serverip, port);
        socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true); // this true makes newlines autoflush

        // start a thread to listen for server messages
        ServerListener listener = new ServerListener();
        Thread t = new Thread(listener);
        t.start();

        // handle input from the user, send to server
        System.out.print("Chat has started: enter a user name: ");
        String name = userInput.nextLine().trim();
        out.println(name); //out.flush();

        //send any messages read in, until /quit
        String line = userInput.nextLine().trim();
        while(!line.toLowerCase().startsWith("/quit")) {
            if (line.toLowerCase().startsWith("/vote")) {
                out.println(String.format("VOTE %s", line.substring(6)));
                line = userInput.nextLine().trim();
            } else if (line.toLowerCase().startsWith("/yes")) {
                out.println("YES");
                line = userInput.nextLine().trim();
            } else if (line.toLowerCase().startsWith("/no")) {
                out.println("NO");
                line = userInput.nextLine().trim();
            } else if (line.toLowerCase().startsWith("/payday")) {
                out.println("PAYDAY");
                line = userInput.nextLine().trim();
            } else {
                String msg = String.format("CHAT %s", line);
                out.println(msg);
                line = userInput.nextLine().trim();
            }

        }
        out.println("QUIT");
        out.close();
        userInput.close();
        socketIn.close();
        socket.close();
    }

    // incoming from server
    static class ServerListener implements Runnable {

        @Override
        public void run() {
            try {
                String incoming = "";
                while ((incoming = socketIn.readLine()) != null) {
                    // System.out.println(incoming);
                    // Three kinds of message headers
                    //WELCOME
                    if (incoming.startsWith("WELCOME")){
                        String newUser = incoming.substring(7).trim();
                        System.out.println("A new user has entered: " + newUser);
                    }
                    //CHAT
                    if (incoming.startsWith("CHAT")){
                        String body = incoming.substring(4).trim();
                        String[] user_msg = body.split(":");
                        String user = user_msg[0].trim();
                        String msg = user_msg[1].trim();
                        System.out.printf("\"%s\": %s\n", user, msg);
                    }
                    if (incoming.startsWith("MONEY")) {
                        System.out.println("Collect your money: [̲̅$̲̅( ͡❛ ͜ʖ ͡❛)̲̅$̲̅]");
                    }
                    //EXIT
                    if (incoming.startsWith("EXIT")){
                        String newUser = incoming.substring(4).trim();
                        System.out.println("A user has left: " + newUser);
                    }

                }
            } catch (Exception ex) {
                System.out.println("Exception caught in listener - " + ex);
            } finally{
                System.out.println("Client Listener exiting");
            }
        }
    }
}