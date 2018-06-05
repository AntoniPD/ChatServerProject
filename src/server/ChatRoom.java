package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ChatRoom {
    private String roomName;
    private ChatServerThread[] users;
    private PrintWriter history;
    private String fileName;
    private int usersCount;
    private int capacity;

    ChatRoom(String roomName, ChatServerThread owner, String fileName) {
        this.roomName = roomName;
        this.usersCount = 1;
        this.fileName = fileName;
        try {
            history = new PrintWriter(new FileWriter(fileName, true));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        this.capacity = 10;
        users = new ChatServerThread[capacity];
        users[0] = owner;
    }

    public void addUserToRoom(ChatServerThread user) {
        users[usersCount] = user;
        usersCount++;
    }

    public void removeUserFromRoom(ChatServerThread user) {
        int index = -1;
        for (int i = 0; i < usersCount; ++i) {
            if (users[i].getClientUsername().equals(user.getClientUsername())) {
                index = i;
                break;
            }
        }
        if (index < usersCount - 1) {
            for (int i = index + 1; i < usersCount; ++i) {
                users[i - 1] = users[i];
            }
        }
        usersCount--;
        for (int i = 0; i < usersCount; ++i) {
            users[i].send(user.getClientUsername() + " has left the room!");
        }
    }

    public void deleteRoom(ChatServerThread user) {
        if (user == users[0]) {
            for (int i = 1; i < usersCount; ++i) {
                removeUserFromRoom(users[i]);
            }
            if (usersCount == 1) {
                user.send("Room " + roomName + " deleted!");
                removeUserFromRoom(user);
            }
        } else {
            user.send("You are not the room maker!");
        }
    }

    public boolean isActive() {
        return (usersCount > 0);
    }

    public void sendMessage(ChatServerThread user, String message) {
        //LocalDateTime ldt = LocalDateTime.now();
        history.println("(" + roomName + ") " + user.getClientUsername() + ":" + message);
        history.flush();
        for (int i = 0; i < usersCount; ++i) {
            if (!users[i].getClientUsername().equals(user.getClientUsername())) {
                users[i].send("(" + roomName + ") " + user.getClientUsername() + ":" + message);
            }
        }
    }

    public String getRoomName() {
        return roomName;
    }


    public String getFileName() {
        return fileName;
    }

    public ChatServerThread[] getUsers() {
        return users;
    }

    public int getUsersCount() {
        return usersCount;
    }
}
