package ru.kpfu.itis.paramonov.heartstone.net.server;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GameServer {
    private ServerSocket serverSocket;
    private List<Client> clients = new ArrayList<>();

    private InetAddress host = setHost();

    private int port = 5555;

    private GameServer() {}

    private static GameServer gameServer = null;

    public static GameServer getInstance() {
        if (gameServer == null) {
            gameServer = new GameServer();
        }
        return gameServer;
    }

    private InetAddress setHost() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(5555);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));

                Client client = new Client(input, output, this);
                clients.add(client);

                new Thread(client).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendMessage(String message, Client client) {
        for (Client c : clients) {
            if (c.equals(client)) {
                continue;
            }

            try {
                c.getOutput().write(message + "\n");
                c.getOutput().flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {
        GameServer chatServer = new GameServer();
        chatServer.start();
    }

    static class Client implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameServer server;

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
                    server.sendMessage(message, this);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public BufferedWriter getOutput() {
            return output;
        }
    }
}
