package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameServer {
    private ServerSocket serverSocket;
    private ConcurrentLinkedDeque<Client> clients = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<Client> clientsToConnect = new ConcurrentLinkedDeque<>();

    private List<GameRoom> rooms = new ArrayList<>();

    private final String host = "127.0.0.1";

    private final int port = 5555;

    private GameServer() {}

    private static GameServer gameServer = null;

    public static GameServer getInstance() {
        if (gameServer == null) {
            gameServer = new GameServer();
        }
        return gameServer;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

                Client client = new Client(input, output, this);
                clients.add(client);

                (new Thread(client)).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendResponse(String message, Client client) {
        try {
            client.getOutput().write(message);
            client.getOutput().newLine();
            client.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void startRoom(Client client1, Client client2) {
        GameRoom room = new GameRoom(client1, client2, this);
        client1.setCurrentRoom(room);
        client2.setCurrentRoom(room);
        this.rooms.add(room);
        room.onStart();
    }

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }

    static class Client implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameServer server;

        private GameRoom currentRoom = null;

        private boolean isDisconnected = false;

        private boolean connected = false;

        private Thread connectionThread = null;

        private String login = null;

        public String getUserLogin() {
            return login;
        }

        public Client(BufferedReader input, BufferedWriter output, GameServer gameServer) {
            this.input = input;
            this.output = output;
            this.server = gameServer;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (!isDisconnected) {
                        String message = input.readLine();
                        JSONObject json = new JSONObject(message);
                        String response;
                        if (checkEntityIsServer(json)) {
                            response = handleServerMessage(json);
                        }
                        else {
                            currentRoom.handleMessage(json, this);
                            response = null;
                        }

                        if (response != null) server.sendResponse(response, this);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void notifyConnected() {
            connected = true;
        }

        private Runnable getConnectionLogic() {
            Runnable runnable = () -> {
                try {
                    server.clientsToConnect.add(this);
                    while (!connected && !isDisconnected) {
                        Thread.sleep(1);
                        for (Client otherClient : server.clientsToConnect) {
                            if (connected) break;
                            if (!this.equals(otherClient)) {
                                otherClient.notifyConnected();
                                connected = true;
                                synchronized (server.clientsToConnect) {
                                    if (server.clientsToConnect.remove(this) && server.clientsToConnect.remove(otherClient)) {
                                        server.startRoom(this, otherClient);
                                    }
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
            return runnable;
        }

        private boolean checkEntityIsServer(JSONObject jsonMessage) {
            return (ServerMessage.Entity.SERVER.toString().equals(jsonMessage.getString("entity")));
        }

        private String handleServerMessage(JSONObject jsonServerMessage) {
            JSONObject response = new JSONObject();
            try {
                String serverAction = jsonServerMessage.getString("server_action");
                switch (ServerMessage.ServerAction.valueOf(serverAction)) {
                    case CONNECT -> {
                        handleConnection();
                        return null;
                    }
                    case LOGIN -> handleLogin(jsonServerMessage, response);
                    case REGISTER -> handleRegistration(jsonServerMessage, response);
                    case DISCONNECT -> {
                        handleDisconnection();
                        return null;
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return response.toString();
        }

        private void handleDisconnection() {
            server.clientsToConnect.remove(this);
            server.clients.remove(this);
            isDisconnected = true;
            if (connectionThread != null) connectionThread.interrupt();
        }

        private void handleLogin(JSONObject jsonServerMessage, JSONObject response) {
            response.put("server_action", ServerMessage.ServerAction.LOGIN.toString());
            UserService service = new UserService();
            User user = service.getWithLoginAndPassword(
                    jsonServerMessage.getString("login"), jsonServerMessage.getString("password"));
            if (user != null) {
                login = user.getLogin();
                response.put("status", "OK");
                putUserInfo(user, response);
            } else {
                response.put("status", "NOT_OK");
            }
        }

        private void handleRegistration(JSONObject jsonServerMessage, JSONObject response) {
            response.put("server_action", ServerMessage.ServerAction.REGISTER.toString());
            UserService service = new UserService();
            try {
                User user = service.save(jsonServerMessage.getString("login"), jsonServerMessage.getString("password"));
                response.put("status", "OK");
                login = user.getLogin();
                putUserInfo(user, response);
            } catch (SQLException e) {
                response.put("status", "NOT_OK");
            }
        }

        private void putUserInfo(User user, JSONObject response) {
            response.put("login", user.getLogin());
            response.put("deck", user.getDeck());
            response.put("cards", user.getCards());
            response.put("money", user.getMoney());
        }

        private void handleConnection() {
            Thread connectionThread = new Thread(getConnectionLogic());
            this.connectionThread = connectionThread;
            connectionThread.start();
        }

        public BufferedWriter getOutput() {
            return output;
        }

        public BufferedReader getInput() {
            return input;
        }

        public GameRoom getCurrentRoom() {
            return currentRoom;
        }

        public void setCurrentRoom(GameRoom currentRoom) {
            this.currentRoom = currentRoom;
        }
    }
}
