package ru.kpfu.itis.paramonov.heartstone.net.client;

import javafx.application.Platform;
import org.json.JSONException;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.controller.BattlefieldController;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameRoom;
import ru.kpfu.itis.paramonov.heartstone.net.server.GameServer;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class GameClient {
    private GameApplication application;

    private Socket socket;

    private ClientThread thread;

    public GameClient(GameApplication application) {
        this.application = application;
    }

    public void sendMessage(String message) {
        try {
            thread.getOutput().write(message);
            thread.getOutput().newLine();
            thread.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        GameServer server = application.getServer();
        String host = server.getHost();
        int port = server.getPort();

        BufferedReader input;
        BufferedWriter output;
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        thread = new ClientThread(input, output, this);
        (new Thread(thread)).start();
    }

    public GameApplication getApplication() {
        return application;
    }

    static class ClientThread implements Runnable {

        private BufferedReader input;
        private BufferedWriter output;
        private GameClient gameClient;

        public ClientThread(BufferedReader input, BufferedWriter output, GameClient gameClient) {
            this.input = input;
            this.output = output;
            this.gameClient = gameClient;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String response = input.readLine();
                    System.out.println(response);
                    Platform.runLater(() -> {
                        JSONObject json = new JSONObject(response);
                        try {
                            switch (ServerMessage.ServerAction.valueOf(json.getString("server_action"))) {
                                case CONNECT -> {
                                    if (checkStatus(json))
                                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                                }
                                case LOGIN, REGISTER -> {
                                    if (checkStatus(json)) {
                                        setGameUser(json);
                                        GameApplication.getApplication().loadScene("/fxml/main_menu.fxml");
                                    }
                                }
                            }
                        } catch (JSONException ignored) {}
                        try {
                            switch (GameRoom.RoomAction.valueOf(json.getString("room_action"))) {
                                case GET_BACKGROUND -> {
                                    String bg = json.getString("background");
                                    if (BattlefieldController.getController() == null) {
                                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                                    }
                                    BattlefieldController.getController().setBackground(bg);
                                }
                                case GET_BATTLE_DECK -> {
                                    if (BattlefieldController.getController() == null) {
                                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                                    }
                                    BattlefieldController.getController().setHand(json.getJSONArray("hand"));
                                    BattlefieldController.getController().setDeck(json.getJSONArray("deck"));
                                }

                                case END_TURN -> {
                                    BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.RED);
                                }
                                case BEGIN_TURN -> {
                                    if (BattlefieldController.getController() == null) {
                                        GameApplication.getApplication().loadScene("/fxml/battlefield.fxml");
                                    }
                                    BattlefieldController.getController().changeEndTurnButton(GameButton.GameButtonStyle.GREEN);
                                }
                            }
                        } catch (JSONException ignored) {}
                    });
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private boolean checkStatus(JSONObject json) {
            return json.getString("status").equals("OK");
        }

        private void setGameUser(JSONObject json) {
            User user = User.getInstance();
            user.setLogin(json.getString("login"));
            user.setDeck(CardRepository.getCardsById(json.getString("deck")));
            user.setCards(CardRepository.getCardsById(json.getString("cards")));
            user.setMoney(json.getInt("money"));
        }

        public BufferedWriter getOutput() {
            return output;
        }
    }
}
