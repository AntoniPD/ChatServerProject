package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;


public class ChatClient implements Runnable {
    private Socket socket;
    private Thread thread;
    private Scanner console;
    private PrintWriter streamOut;
    private ChatClientThread client;

    public ChatClient() {
        System.out.println("Establishing connection. Please wait ...");
        try {
            Scanner in = new Scanner(System.in);
            String input = in.nextLine();
            String[] inputArr = input.split(" ");
            if (inputArr[0].equals("connect")) {
                String serverName = inputArr[1];
                int serverPort = Integer.parseInt(inputArr[2]);

                socket = new Socket(serverName, serverPort);
                System.out.println("Connected: " + socket.getLocalAddress().getHostName());
                start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() throws IOException {
        console = new Scanner(System.in);
        streamOut = new PrintWriter(socket.getOutputStream());
        if (thread == null) {
            client = new ChatClientThread(this, socket);
            thread = new Thread(this);
            thread.start();
        }
    }

    public void run() {
        while (thread != null) {
            try {
                //sends the message to the server
                streamOut.println(console.nextLine());
                streamOut.flush();
            } catch (Exception e) {
                System.out.println("Sending error: " + e.getMessage());
                stop();
            }
        }
    }

    public void handle(String msg) {
        if (msg == null) {
            System.out.println("Good bye. Press ENTER to exit ...");
            stop();
        } else
            System.out.println(msg);
    }



    public void stop() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
        try {
            if (console != null) console.close();
            if (streamOut != null) streamOut.close();
            if (socket != null) socket.close();
        } catch (IOException ioe) {
            System.out.println("Error closing ...");
        }
        client.close();
        client.stop();
    }

    public static void main(String args[]) {
        ChatClient client = new ChatClient();
    }
}

