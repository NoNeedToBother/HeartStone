package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.util.PackOpeningUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameServer {
    private ServerSocket serverSocket;
    private final ConcurrentLinkedDeque<Client> clients = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<Client> clientsToConnect = new ConcurrentLinkedDeque<>();

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
        room.onStart();
    }

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }

    public static class Client implements Runnable {

        private final BufferedReader input;
        private final BufferedWriter output;
        private final GameServer server;

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

        public void notifyGameEnd() {
            currentRoom = null;
            connected = false;
        }

        private Runnable getConnectionLogic() {
            Runnable runnable = () -> {
                try {
                    server.clientsToConnect.add(this);
                    while (!connected && !isDisconnected) {
                        Thread.sleep(50);
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
            ServerResponse response = new ServerResponse();
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
                    case OPEN_ONE_PACK -> PackOpeningUtil.openOnePack(jsonServerMessage, response);
                    case OPEN_FIVE_PACKS -> PackOpeningUtil.openFivePacks(jsonServerMessage, response);
                    case UPDATE_DECK -> handleDeckUpdating(jsonServerMessage, response);
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
            if (currentRoom != null) currentRoom.notifyDisconnected(this);
            if (connectionThread != null) connectionThread.interrupt();
        }

        private void handleLogin(JSONObject jsonServerMessage, ServerResponse response) {
            response.putAction(ServerMessage.ServerAction.LOGIN);
            UserService service = new UserService();
            User user = service.getWithLoginAndPassword(
                    jsonServerMessage.getString("login"), jsonServerMessage.getString("password"));
            if (user != null) {
                login = user.login();
                response.putStatus("ok");
                response.putUserInfo(user);
            } else {
                response.putStatus("not_ok");
            }
        }

        private void handleRegistration(JSONObject jsonServerMessage, ServerResponse response) {
            response.putAction(ServerMessage.ServerAction.REGISTER);
            UserService service = new UserService();
            try {
                User user = service.save(jsonServerMessage.getString("login"), jsonServerMessage.getString("password"));
                response.putStatus("ok");
                login = user.login();
                response.putUserInfo(user);
            } catch (SQLException e) {
                response.putStatus("not_ok");
            }
        }

        private void handleDeckUpdating(JSONObject json, ServerResponse response) {
            response.putAction(ServerMessage.ServerAction.UPDATE_DECK);
            UserService service = new UserService();
            try {
                service.updateDeck(login, json.getString("deck"));
                response.putStatus("ok");
            } catch (SQLException e) {
                response.putStatus("not_ok");
            }
        }

        private void handleConnection() {
            Thread connectionThread = new Thread(getConnectionLogic());
            this.connectionThread = connectionThread;
            connectionThread.start();
        }

        public BufferedWriter getOutput() {
            return output;
        }

        public void setCurrentRoom(GameRoom currentRoom) {
            this.currentRoom = currentRoom;
        }
    }
}
