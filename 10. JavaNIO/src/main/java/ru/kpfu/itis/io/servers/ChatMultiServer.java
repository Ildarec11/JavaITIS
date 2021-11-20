package ru.kpfu.itis.io.servers;

import ru.kpfu.itis.io.protocols.Message;
import ru.kpfu.itis.io.protocols.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatMultiServer {
    // список клиентов
    private List<ClientHandler> clients;
    private Map<String, List<ClientHandler>> rooms;
    private Map<String, List<String>> history;
    private Map<ClientHandler, String> clientInRoom;

    public ChatMultiServer() {
        // Список для работы с многопоточностью
        clients = new CopyOnWriteArrayList<>();
        history = new HashMap<>();
        rooms = new HashMap<>();
        clientInRoom = new HashMap<>();
    }

    public void start(int port) {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        // запускаем бесконечный цикл
        while (true) {
            try {
                // запускаем обработчик сообщений для каждого подключаемого клиента
                new ClientHandler(serverSocket.accept()).start();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private class ClientHandler extends Thread {
        // связь с одним клиентом
        private Socket clientSocket;
        // информация, поступающая от клиента
        private BufferedReader in;
        private String nameClient;

        ClientHandler(Socket socket) {
            this.clientSocket = socket;
            // добавляем текущее подключение в список
            clients.add(this);
            System.out.println("New client " + socket.getPort());
        }

        public void run() {
            try {
                // получем входной поток для конкретного клиента
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ( true ) {
                    inputLine = in.readLine();
                    if (inputLine != null) {
                        Message inputMsg = MessageConverter.messageFromString(inputLine);
                        switch (inputMsg.getMessageType()) {
                            case NAME_RESPONSE: {
                                System.out.println("receive name req");
                                this.nameClient = inputMsg.getText();
                                System.out.println("Client " + clientSocket.getPort() + "set name " + inputMsg.getText());
                                break;
                            }
                            case CONNECT_RESPONSE: {
                                System.out.println("receive connect response");
                                System.out.println("room name:" + inputMsg.getText());
                                clientInRoom.put(this, inputMsg.getText());
                                List<ClientHandler> roommates = rooms.get(inputMsg.getText());
                                if (roommates == null) {
                                    roommates = new ArrayList<>();
                                }
                                roommates.add(this);
                                List<String> historyMsg = history.get(inputMsg.getText());
                                if (historyMsg == null) {
                                    historyMsg = new ArrayList<>();
                                }
                                for (String msg: historyMsg) {
                                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                                 out.println(msg);
                                }
                                for (ClientHandler client : roommates) {
                                    PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                                    out.println("New roommate joined:" + this.nameClient);
                                }
                                rooms.put(inputMsg.getText(), roommates);
                                break;
                            }

                            case DEFAULT_MESSAGE: {
                                System.out.println("receive msg from " + nameClient);
                                String roomName = clientInRoom.get(this);
                                System.out.println("room: " + roomName);
                                List<ClientHandler> roommates = rooms.get(roomName);
                                if (roommates == null) {
                                    roommates = new ArrayList<>();
                                }
                                System.out.println("size" + roommates.size());
                                List<String> historyMsg = history.get(roomName);
                                if (historyMsg == null) {
                                    historyMsg = new ArrayList<>();
                                }
                                historyMsg.add(this.nameClient + ": " + inputMsg.getText());
                                for (ClientHandler client : roommates) {
                                    PrintWriter out = new PrintWriter(client.clientSocket.getOutputStream(), true);
                                    out.println(this.nameClient + ": " + inputMsg.getText());
                                }
                                break;
                            }
                        }
                    }
                }
                //in.close();
                //clientSocket.close();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        public String getNameClient() {
            return nameClient;
        }

        public void setNameClient(String nameClient) {
            this.nameClient = nameClient;
        }
    }


    private static class MessageConverter {
        static Message messageFromString(String msg) {
            Message message = new Message();
            if (msg.startsWith("/name")) {
                message.setMessageType(MessageType.NAME_RESPONSE);
                message.setText(msg.replace("/name ", ""));
            } else if (msg.startsWith("/room")) {
                message.setMessageType(MessageType.CONNECT_RESPONSE);
                message.setText(msg.replace("/room ", ""));
            } else {
                message.setMessageType(MessageType.DEFAULT_MESSAGE);
                message.setText(msg);
            }
            return message;
        }
    }
}
