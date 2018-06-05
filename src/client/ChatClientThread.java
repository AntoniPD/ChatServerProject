package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


public class ChatClientThread extends Thread {
    private Socket socket = null;
    private ChatClient client = null;
    private BufferedReader streamIn = null;

    public ChatClientThread(ChatClient _client, Socket _socket) {
        client = _client;
        socket = _socket;
        open();
        start();
    }

    public void open() {
        try {
            streamIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ioe) {
            System.out.println("Error getting input stream: " + ioe);
            client.stop();
        }
    }

    public void close() {
        try {
            if (streamIn != null) streamIn.close();
        } catch (IOException ioe) {
            System.out.println("Error closing input stream: " + ioe);
        }
    }

    public void run() {
        while (!currentThread().isInterrupted()) {
            while (true) {
                try {
                    //receives the message from the server
                    client.handle(streamIn.readLine());
                } catch (IOException ioe) {
                    client.stop();
                }
            }
        }
    }
}