package ru.kpfu.itis.paramonov.heartstone.net;

import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.net.server.room.GameRoom;

import java.util.HashMap;
import java.util.Map;

public class ServerMessage {

    public enum Entity {
        SERVER, ROOM
    }

    public enum ServerAction {
        CONNECT, REGISTER, LOGIN, UPDATE_DECK, DISCONNECT, OPEN_1_PACK, OPEN_5_PACKS //etc
    }

    private ServerMessage() {}

    private Entity entityToConnect = null;

    private ServerAction serverAction = null;

    private GameRoom.RoomAction roomAction = null;

    private Map<String, String> params = new HashMap<>();

    public static class ServerMessageBuilder {
        ServerMessage message = new ServerMessage();

        public ServerMessageBuilder setEntityToConnect(Entity entityToConnect) {
            message.entityToConnect = entityToConnect;
            return this;
        }

        public ServerMessageBuilder setServerAction(ServerAction action) {
            if (Entity.SERVER.equals(message.entityToConnect)) {
                message.serverAction = action;
                return this;
            } else {
                throw new RuntimeException("Attempt to send message to server with wrong or no entity to connect");
            }
        }

        public ServerMessageBuilder setRoomAction(GameRoom.RoomAction action) {
            if (Entity.ROOM.equals(message.entityToConnect)) {
                message.roomAction = action;
                return this;
            } else {
                throw new RuntimeException("Attempt to send message to server with wrong or no entity to connect");
            }
        }

        public ServerMessageBuilder setParameter(String parameter, String value) {
            message.params.put(parameter, value);
            return this;
        }

        public String build() {
            JSONObject json = new JSONObject();
            json.put("entity", message.entityToConnect.toString());
            if (message.entityToConnect.equals(Entity.SERVER)) json.put("server_action", message.serverAction.toString());
            if (message.entityToConnect.equals(Entity.ROOM)) json.put("room_action", message.roomAction.toString());
            for (String key : message.params.keySet()) {
                json.put(key, message.params.get(key));
            }
            return json.toString();
        }
    }

    /**
     * Builds a message to the server (or server room) and returns it in JSON format. <p>
     * User must firstly set entity to connect and then use only methods appropriate for the specified entity.
     */

    public static ServerMessageBuilder builder() {
        return new ServerMessageBuilder();
    }
}
