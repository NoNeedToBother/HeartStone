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

public class GameServer {
    private ServerSocket serverSocket;
    private List<Client> clients = new ArrayList<>();

    private List<Client> clientsToConnect = new ArrayList<>();

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

    public static void main(String[] args) {
        GameServer gameServer = new GameServer();
        gameServer.start();
    }

    static class Client implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameServer server;

        private boolean connected = false;

        public Client(BufferedReader input, BufferedWriter output, GameServer gameServer) {
            this.input = input;
            this.output = output;
            this.server = gameServer;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = input.readLine();
                    JSONObject json = new JSONObject(message);
                    String response;
                    if (checkEntityIsServer(json)) {
                        response = handleServerMessage(json);
                    }
                    else {
                        //stub
                        response = "response";
                    }

                    server.sendResponse(response, this);
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
                    while (!connected) {
                        Thread.sleep(50);
                        for (Client otherClient : server.clientsToConnect) {
                            if (!this.equals(otherClient)) {
                                otherClient.notifyConnected();
                                GameRoom room = new GameRoom(this, otherClient);
                                connected = true;
                                server.clientsToConnect.remove(this);
                                server.clientsToConnect.remove(otherClient);
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
                    case CONNECT -> handleConnection(jsonServerMessage, response);
                    case LOGIN -> handleLogin(jsonServerMessage, response);
                    case REGISTER -> handleRegistration(jsonServerMessage, response);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return response.toString();
        }

        private void handleLogin(JSONObject jsonServerMessage, JSONObject response) {
            response.put("server_action", ServerMessage.ServerAction.LOGIN.toString());
            UserService service = new UserService();
            User user = service.getWithLoginAndPassword(
                    jsonServerMessage.getString("login"), jsonServerMessage.getString("password"));
            if (user != null) {
                response.put("status", "OK");
                response.put("login", user.getLogin());
                response.put("deck", user.getDeck());
                response.put("cards", user.getCards());
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
                response.put("login", user.getLogin());
                response.put("deck", user.getDeck());
                response.put("cards", user.getCards());
            } catch (SQLException e) {
                response.put("status", "NOT_OK");
            }
        }

        private void handleConnection(JSONObject jsonServerMessage, JSONObject response) {
            response.put("server_action", ServerMessage.ServerAction.CONNECT.toString());
            Thread connectionThread = new Thread(getConnectionLogic());
            connectionThread.start();
            try {
                connectionThread.join();
                response.put("status", "OK");
            } catch (InterruptedException e) {}
        }

        public BufferedWriter getOutput() {
            return output;
        }

        public BufferedReader getInput() {
            return input;
        }

        public GameServer getServer() {
            return server;
        }
    }
}
