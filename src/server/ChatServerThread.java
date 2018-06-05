package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;


public class ChatServerThread extends Thread {
    private ChatServer server;
    private Socket socket;
    private int ID = -1;
    private String clientUsername;
    private boolean isLogged;
    private BufferedReader streamIn;
    private PrintWriter streamOut;
    private ArrayList<ChatRoom> clientChatRooms;

    public ChatServerThread(ChatServer server, Socket socket) {
        super();
        this.server = server;
        this.socket = socket;
        ID = socket.getPort();
        clientChatRooms = new ArrayList<>();
    }

    public void send(String msg) {
        try {
            streamOut.println(msg);
            streamOut.flush();
        } catch (Exception e) {
            System.out.println(ID + " ERROR sending: " + e.getMessage());
            server.remove(ID);
            interrupt();
        }
    }

    public int getID() {
        return ID;
    }

    public void run() {
        System.out.println("Server Thread " + ID + " running.");
        while (!currentThread().isInterrupted()) {
            while (true) {
                try {
                    //receives the message from the client
                    server.handle(ID, streamIn.readLine());
                } catch (IOException ioe) {
                    System.out.println(ID + " ERROR reading: " + ioe.getMessage());
                    server.remove(ID);
                    stop();
                }
            }
        }
    }

    public void open() throws IOException {
        streamIn = new BufferedReader(new
                InputStreamReader(socket.getInputStream()));
        streamOut = new PrintWriter(socket.getOutputStream());
    }

    public void close() throws IOException {
        if (socket != null) socket.close();
        if (streamIn != null) streamIn.close();
        if (streamOut != null) streamOut.close();
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public boolean isLogged() {
        return isLogged;
    }

    public void logClient() {
        isLogged = true;
    }

    public void createChatRoom(String roomName) {
        clientChatRooms.add(new ChatRoom(roomName, this, roomName + ".txt"));
    }

    public void joinRoom(ChatRoom room) {
        clientChatRooms.add(room);
        room.addUserToRoom(this);
    }

    public void leaveRoom(ChatRoom room) {
        clientChatRooms.remove(room);
    }

    public ChatRoom getChatRoomByName(String name) {
        for (int i = 0; i < clientChatRooms.size(); ++i) {
            if (clientChatRooms.get(i).getRoomName().equals(name)) {
                return clientChatRooms.get(i);
            }
        }
        return null;
    }

    public ArrayList<ChatRoom> getClientChatRooms() {
        return clientChatRooms;
    }
}

