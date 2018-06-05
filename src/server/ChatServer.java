package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class ChatServer implements Runnable {
    private ChatServerThread clients[];
    private int capacity;
    private ServerSocket server;
    private Thread thread;
    private int clientCount;
    private ArrayList<ChatRoom> chatRooms;
    private ConcurrentHashMap<ChatRoom, String> mapRooms;

    public ChatServer(int port) {
        try {
            server = new ServerSocket(port);
            System.out.println("Server started: " + server);
            start();
        } catch (IOException ioe) {
            System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());
        }
        capacity = 10;
        clients = new ChatServerThread[capacity];
        chatRooms = new ArrayList<>();
        mapRooms = new ConcurrentHashMap<>();
    }

    public void run() {
        //while the server isn't closed
        while (thread != null) {
            try {
                System.out.println("Waiting for a client ...");
                addThread(server.accept());
            } catch (IOException ioe) {
                System.out.println("Server accept error: " + ioe);
                stop();
            }
        }
    }

    //open the server in a thread
    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            thread.stop();
            thread = null;
        }
    }


    private void addThread(Socket socket) {
        System.out.println("Client accepted: " + socket);
        clients[clientCount] = new ChatServerThread(this, socket);
        try {
            clients[clientCount].open();
            clients[clientCount].start();
            clientCount++;
            capacity++;
        } catch (IOException ioe) {
            System.out.println("Error opening thread: " + ioe);
        }

    }

    private int findClient(int ID) {
        for (int i = 0; i < clientCount; i++)
            if (clients[i].getID() == ID)
                return i;
        return -1;
    }

    public synchronized void handle(int ID, String input) {
        String[] inputToArr = input.split(" ");
        String command = inputToArr[0];
        ChatServerThread currentClient = clients[findClient(ID)];
        if (command.equals("disconnect")) {
            disconnect(ID);
            return;
        }
        if (command.equals("list-users") && (inputToArr.length == 1)) {
            showUsers(ID);
            return;
        }

        if (command.equals("register")) {
            if (inputToArr.length != 3) {
                currentClient.send("Unsuccessful register!");
                return;
            }
            String username = inputToArr[1];
            String password = inputToArr[2];
            register(username, password, ID);
            return;
        }
        if (command.equals("login")) {
            if (inputToArr.length != 3) {
                currentClient.send("Unsuccessful login!");
                return;
            }
            String username = inputToArr[1];
            String password = inputToArr[2];
            login(username, password, ID);
            return;
        }
        if (command.equals("send")) {
            if (inputToArr.length == 1) {
                currentClient.send("Unsuccessful sending a message!");
                return;
            }
            String username = inputToArr[1];
            String message = input.substring(command.length() + username.length() + 1);
            sendMessage(ID, username, message);
            return;
        }

        if (command.equals("create-room") && currentClient.isLogged()) {
            ChatServerThread owner = clients[findClient(ID)];
            owner.createChatRoom(inputToArr[1]);
            mapRooms.put(owner.getChatRoomByName(inputToArr[1]), inputToArr[1]);
            chatRooms.add(owner.getChatRoomByName(inputToArr[1]));
            return;
        }
        if (command.equals("join-room") && currentClient.isLogged()) {
            joinRoomServer(ID, inputToArr[1]);
            return;
        }
        if (command.equals("list-users")) {
            String chatRoomName = inputToArr[1];
            listUsersChatRoom(chatRoomName, currentClient);
            return;
        }
        if (command.equals("leave-room")) {
            String chatRoomName = inputToArr[1];
            leaveChatRoom(chatRoomName, currentClient);
            return;
        }
        if (command.equals("list-rooms")) {
            listChatRooms(currentClient);
            return;
        }
        if (command.equals("send-room")) {
            String chatRoomName = inputToArr[1];
            String message = input.substring(command.length() + chatRoomName.length() + 1);
            sendInChatRoom(chatRoomName, currentClient, message);
            return;
        }
        if (command.equals("delete-room")) {
            String chatRoomName = inputToArr[1];
            deleteChatRoom(chatRoomName, currentClient);
            return;
        }

        currentClient.send("Wrong command!");
    }

    private void deleteChatRoom(String chatRoomName, ChatServerThread currentClient) {
        if (!isChatRoomCreated(chatRoomName)) {
            currentClient.send("No such room!");
            return;
        }
        int indexOfChatRoom = -1;
        boolean found = false;
        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).getRoomName().equals(chatRoomName)) {
                indexOfChatRoom = i;
                found = true;
                break;
            }
        }
        if (found) {
            chatRooms.get(indexOfChatRoom).deleteRoom(currentClient);
            chatRooms.remove(indexOfChatRoom);
        } else {
            currentClient.send("No such room!");
        }
    }

    private void sendInChatRoom(String chatRoomName, ChatServerThread currentClient, String message) {
        boolean found = false;
        int indexOfChatRoom = -1;


        if (!isChatRoomCreated(chatRoomName)) {
            currentClient.send("No such room!");
            return;
        }

        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).getRoomName().equals(chatRoomName)) {
                indexOfChatRoom = i;
                break;
            }
        }

        for (int i = 0; i < currentClient.getClientChatRooms().size(); ++i) {
            String currentRoomName = currentClient.getClientChatRooms().get(i).getRoomName();
            if (currentRoomName.equals(chatRooms.get(indexOfChatRoom).getRoomName())) {
                found = true;
                break;
            }
        }

        if (found) {
            chatRooms.get(indexOfChatRoom).sendMessage(currentClient, message);
        } else {
            currentClient.send("No message was send to room: " + chatRoomName);
        }
    }


    private void listChatRooms(ChatServerThread currentClient) {
        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).isActive()) {
                currentClient.send(chatRooms.get(i).getRoomName());
            }
        }
    }

    private void leaveChatRoom(String chatRoomName, ChatServerThread currentClient) {
        boolean found = false;
        if (!isChatRoomCreated(chatRoomName)) {
            currentClient.send("No such room!");
            return;
        }
        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).getRoomName().equals(chatRoomName)) {
                found = true;
                currentClient.leaveRoom(chatRooms.get(i));
                chatRooms.get(i).removeUserFromRoom(currentClient);
                break;
            }
        }

        if (found) {
            currentClient.send("You left room " + chatRoomName);
        } else {
            currentClient.send("Room not found!");
        }
    }

    private void listUsersChatRoom(String chatRoomName, ChatServerThread currentClient) {
        boolean found = false;
        if (!isChatRoomCreated(chatRoomName)) {
            currentClient.send("No such room!");
            return;
        }
        int indexOfChatRoom = -1;
        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).getRoomName().equals(chatRoomName)) {
                found = true;
                indexOfChatRoom = i;
                break;
            }
        }
        if (found) {
            ChatServerThread[] usersInRoom = chatRooms.get(indexOfChatRoom).getUsers();
            int activeUsers = chatRooms.get(indexOfChatRoom).getUsersCount();
            for (int i = 0; i < activeUsers; ++i) {
                currentClient.send(usersInRoom[i].getClientUsername());
            }
        } else {
            currentClient.send("No such room!");
        }
    }

    private void joinRoomServer(int id, String name) {
        ChatServerThread currentClient = clients[findClient(id)];
        if (!isChatRoomCreated(name)) {
            currentClient.send("No such room!");
            return;
        }
        boolean joined = false;
        for (ChatRoom r : mapRooms.keySet()) {
            if (name.equals(r.getRoomName())) {
                currentClient.joinRoom(r);
                joined = true;
                break;
            }
        }

        if (joined) {
            currentClient.send("You joined room: " + currentClient.getChatRoomByName(name).getRoomName());
            try {
                String fileName = currentClient.getChatRoomByName(name).getFileName();
                BufferedReader reader = new BufferedReader(new FileReader(fileName));
                String line;
                while ((line = reader.readLine()) != null) {
                    currentClient.send(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            currentClient.send("There is no such active room");
        }
    }

    private void sendMessage(int id, String username, String message) {
        ChatServerThread currentClient = clients[findClient(id)];
        for (int i = 0; i < clientCount; ++i) {
            ChatServerThread target = clients[i];
            if (target.getClientUsername().equals(username)) {
                target.send(currentClient.getClientUsername() + ":" + message);
                break;
            }
        }
    }

    private void showUsers(int id) {
        ChatServerThread currentClient = clients[findClient(id)];
        for (int i = 0; i < clientCount; ++i) {
            if (!currentClient.getClientUsername().equals(clients[i].getClientUsername())) {
                currentClient.send(clients[i].getClientUsername());
            }
        }
    }

    private void register(String username, String password, int id) {
        ChatServerThread currentClient = clients[findClient(id)];
        if (currentClient.isLogged()) {
            currentClient.send("You are logged in as: " + currentClient.getClientUsername());
            return;
        }
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("usersInfo.txt", true));
            BufferedReader reader = new BufferedReader(new FileReader("usersInfo.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fileData = line.split(" ");
                if (fileData[0].equals(username)) {
                    currentClient.send("Username already taken!");
                    return;
                }
            }
            writer.print(username);
            writer.print(" ");
            writer.print(password);
            writer.println();
            writer.flush();
            currentClient.send("Your registration was successful!");
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void login(String username, String password, int id) {
        boolean logged = false;
        ChatServerThread currentClient = clients[findClient(id)];
        for (int i = 0; i < clientCount; ++i) {
            if (username.equals(clients[i].getClientUsername())) {
                currentClient.send("User already logged in from other client!");
                return;
            }
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader("usersInfo.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fileData = line.split(" ");
                // fileUsername       input username    filePassword       input password
                if (fileData[0].equals(username) && fileData[1].equals(password)) {
                    logged = true;
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (currentClient.isLogged()) {
            currentClient.send("You have already logged in as: " + currentClient.getClientUsername());
            return;
        }
        if (logged) {
            currentClient.setClientUsername(username);
            currentClient.send("You logged in successfully as: " + currentClient.getClientUsername());
            currentClient.logClient();
            System.out.println(currentClient.getClientUsername() + " logged in!");
        } else {
            currentClient.send("Login was unsuccessful!");
        }
    }

    private boolean isChatRoomCreated(String chatRoomName) {
        for (int i = 0; i < chatRooms.size(); ++i) {
            if (chatRooms.get(i).getRoomName().equals(chatRoomName)) {
                return true;
            }
        }
        return false;
    }

    private void disconnect(int ID) {
        remove(ID);
    }

    public synchronized void remove(int ID) {
        int pos = findClient(ID);
        if (pos >= 0) {
            ChatServerThread toTerminate = clients[pos];
            System.out.println("Removing client thread " + ID + " at " + pos);
            if (pos < clientCount - 1) {
                for (int i = pos + 1; i < clientCount; i++) {
                    clients[i - 1] = clients[i];
                }
            }
            clientCount--;
            try {
                toTerminate.send("You are disconnecting!");
                toTerminate.close();
            } catch (IOException ioe) {
                System.out.println("Error closing thread: " + ioe);
            }
            toTerminate.interrupt();
        }
    }


    public static void main(String args[]) {
        ChatServer server = new ChatServer(1337);
    }
}
