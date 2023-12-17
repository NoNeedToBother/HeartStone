package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GameRoom {

    public enum RoomAction {
        GET_BACKGROUND, GET_INITIAL_INFO, DRAW_CARD, BEGIN_TURN, END_TURN, PLAY_CARD, PLAY_CARD_OPPONENT, CARD_CARD_ATTACK,
        GET_OPPONENT_MANA, CHECK_CARD_PLAYED, CARD_HERO_ATTACK, CHECK_CARD_TO_ATTACK
    }

    private GameServer.Client player1;

    private GameServer.Client player2;

    private Hero player1Hero;

    private Hero player2Hero;

    private GameServer.Client activePlayer;

    private GameServer server;

    HashMap<String, List<Card>> player1AllCards = setPlayerCardMap();

    HashMap<String, List<Card>> player2AllCards = setPlayerCardMap();

    private UserService service = new UserService();

    Random random = new Random();

    public GameRoom(GameServer.Client player1, GameServer.Client player2, GameServer server) {
        this.player1 = player1;
        this.player2 = player2;
        this.server = server;
    }

    public void onStart() {
        setBackground();
        sendInitialInfo();
        setActivePlayer();
    }

    private HashMap<String, List<Card>> setPlayerCardMap() {
        HashMap<String, List<Card>> res = new HashMap<>();
        res.put("field", Collections.synchronizedList(new ArrayList<>()));
        res.put("hand", Collections.synchronizedList(new ArrayList<>()));
        res.put("deck", Collections.synchronizedList(new ArrayList<>()));
        return res;
    }

    public GameServer.Client getNonActivePlayer() {
        if (activePlayer.equals(player1)) return player2;
        else return player1;
    }
    public GameServer.Client getOtherPlayer(GameServer.Client player) {
        if (player.equals(player1)) return player2;
        else return player1;
    }
    private HashMap<String, List<Card>> getAllCards(GameServer.Client player) {
        if (player.equals(player1)) return player1AllCards;
        else return player2AllCards;
    }
    private Hero getHero(GameServer.Client player) {
        if (player.equals(player1)) return player1Hero;
        else return player2Hero;
    }

    public void handleMessage(JSONObject msg, GameServer.Client client) {
        switch (RoomAction.valueOf(msg.getString("room_action"))) {
            case END_TURN -> {
                if (client.equals(player1)) {
                    activePlayer = player2;
                    CardHelper.makeCardsAbleToAttack(player1AllCards.get("field"));
                }
                else {
                    activePlayer = player1;
                    CardHelper.makeCardsAbleToAttack(player2AllCards.get("field"));
                }
                JSONObject responseEnd = new JSONObject();
                responseEnd.put("room_action", RoomAction.END_TURN.toString());
                responseEnd.put("status", "ok");
                sendResponse(responseEnd.toString(), client);

                JSONObject responseBegin = new JSONObject();
                responseBegin.put("room_action", RoomAction.BEGIN_TURN.toString());
                responseBegin.put("status", "ok");

                Hero resHero =
                        ManaHelper.increaseMana(responseBegin, activePlayer, player1, player1Hero, player2Hero);
                sendResponse(responseBegin.toString(), activePlayer);

                JSONObject responseOpponentMana = new JSONObject();
                ManaHelper.getOpponentMana(responseOpponentMana, resHero);
                sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
                drawCard(activePlayer);
            }

            case CHECK_CARD_PLAYED -> {
                JSONObject response = new JSONObject();
                response.put("room_action", RoomAction.CHECK_CARD_PLAYED.toString());
                int mana = getHero(client).getMana();

                CardHelper.checkCardPlayed(response, client, player1, Integer.parseInt(msg.getString("hand_pos")),
                        player1AllCards, player2AllCards, mana);

                sendResponse(response.toString(), client);
            }

            case PLAY_CARD -> {
                Card playedCard = CardHelper.onCardPlayed(msg, client, player1, player1AllCards, player2AllCards);
                Hero hero = getHero(client);
                int newMana = hero.getMana() - playedCard.getCost();
                hero.setMana(newMana);

                JSONObject response = new JSONObject();
                CardHelper.putPlayedCardForOpponent(response, playedCard);
                response.put("opponent_mana", newMana);
                sendResponse(response.toString(), getOtherPlayer(client));
            }

            case CHECK_CARD_TO_ATTACK -> {
                JSONObject response = new JSONObject();
                HashMap<String, List<Card>> allCards = getAllCards(client);
                String target = msg.getString("target");
                int pos = Integer.parseInt(msg.getString("pos"));
                if (target.equals("hero")) {
                    CardHelper.checkCardToAttack(response, allCards.get("field").get(pos), pos, target);
                } else {
                    int opponentPos = Integer.parseInt(msg.getString("opponent_pos"));
                    CardHelper.checkCardToAttack(response, allCards.get("field").get(pos), pos, opponentPos, target);
                }
                sendResponse(response.toString(), client);
            }

            case CARD_HERO_ATTACK -> {
                HashMap<String, List<Card>> allCards = getAllCards(client);
                Card attacker = allCards.get("field").get(Integer.parseInt(msg.getString("pos")));
                Hero attackedHero;
                if (client.equals(player1)) attackedHero = player2Hero;
                else attackedHero = player1Hero;
                JSONObject responseAttacker = new JSONObject();
                JSONObject responseAttacked = new JSONObject();
                HeroHelper.onHeroAttacked(responseAttacker, responseAttacked, attackedHero, attacker, Integer.parseInt(msg.getString("pos")));

                sendResponse(responseAttacker.toString(), client);
                sendResponse(responseAttacked.toString(), getOtherPlayer(client));
            }

            case CARD_CARD_ATTACK -> {
                List<Card> attackerField;
                List<Card> attackedField;
                if (client.equals(player1)) {
                    attackerField = player1AllCards.get("field");
                    attackedField = player2AllCards.get("field");
                }
                else {
                    attackerField = player2AllCards.get("field");
                    attackedField = player1AllCards.get("field");
                }

                Card attacker = attackerField.get(Integer.parseInt(msg.getString("attacker_pos")));
                Card attacked = attackedField.get(Integer.parseInt(msg.getString("attacked_pos")));
                CardHelper.decreaseHpOnDirectAttack(attacker, attacked);

                JSONObject attackerResponse = new JSONObject();
                attackerResponse.put("room_action", RoomAction.CARD_CARD_ATTACK.toString());
                attackerResponse.put("status", "ok");
                putFieldChanges(attackerResponse, attackerField, Integer.parseInt(msg.getString("attacker_pos")));
                putOpponentChanges(attackerResponse, attackedField, Integer.parseInt(msg.getString("attacked_pos")));
                sendResponse(attackerResponse.toString(), client);

                JSONObject attackedResponse = new JSONObject();
                attackedResponse.put("room_action", RoomAction.CARD_CARD_ATTACK.toString());
                attackedResponse.put("status", "ok");
                putFieldChanges(attackedResponse, attackedField, Integer.parseInt(msg.getString("attacked_pos")));
                putOpponentChanges(attackedResponse, attackerField, Integer.parseInt(msg.getString("attacker_pos")));
                sendResponse(attackedResponse.toString(), getOtherPlayer(client));

                CardHelper.removeDefeatedCards(attackerField);
                CardHelper.removeDefeatedCards(attackedField);
            }
        }
    }

    private void putFieldChanges(JSONObject response, List<Card> field, int... positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("stat_changes", changes);
    }

    private JSONArray getFieldChanges(List<Card> field, int... positions) {
        JSONArray changes = new JSONArray();
        for (int pos = 0; pos < positions.length; pos++) {
            JSONObject changedCard = new JSONObject();
            changedCard.put("pos", positions[pos]);
            changedCard.put("hp", field.get(positions[pos] ).getHp());
            changedCard.put("atk", field.get(positions[pos]).getAtk());
            changes.put(changedCard);
        }
        return changes;
    }

    private void putOpponentChanges(JSONObject response, List<Card> field, int... positions) {
        JSONArray changes = getFieldChanges(field, positions);
        response.put("opponent_stat_changes", changes);
    }

    private void drawCard(GameServer.Client client) {
        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.DRAW_CARD.toString());
        JSONArray deck = new JSONArray();
        Card cardToDraw;

        Map<String, List<Card>> allCards = getAllCards(client);
        try {
            cardToDraw = allCards.get("deck").remove(0);
            putDeckInfo(allCards.get("deck"), deck);
            if (allCards.get("hand").size() == HAND_SIZE) {
                response.put("card_status", "burned");
            } else {
                allCards.get("hand").add(cardToDraw);
                response.put("card_status", "drawn");
            }
            putCardInfo(cardToDraw, response);
        } catch (IndexOutOfBoundsException e) {
            response.put("card_status", "no_card");
        }
        response.put("deck", deck);
        response.put("status", "ok");

        sendResponse(response.toString(), client);
    }


    private void putDeckInfo(List<Card> deck, JSONArray deckArray) {
        for (Card card : deck) {
            putCardInfo(card, deckArray);
        }
    }

    private void sendResponse(String response, GameServer.Client client) {
        try {
            client.getOutput().write(response);
            client.getOutput().newLine();
            client.getOutput().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setActivePlayer() {
        if (random.nextBoolean()) activePlayer = player1;
        else activePlayer = player2;

        drawCard(activePlayer);

        JSONObject response = new JSONObject();
        response.put("room_action", RoomAction.BEGIN_TURN);
        response.put("status", "ok");

        Hero resHero =
                ManaHelper.increaseMana(response, activePlayer, player1, player1Hero, player2Hero);
        sendResponse(response.toString(), activePlayer);

        JSONObject responseOpponentMana = new JSONObject();
        ManaHelper.getOpponentMana(responseOpponentMana, resHero);
        sendResponse(responseOpponentMana.toString(), getNonActivePlayer());
    }

    private void sendInitialInfo() {
        player1AllCards.put("deck", getShuffledDeck(player1));
        player2AllCards.put("deck", getShuffledDeck(player2));
        CardHelper.makeCardsUnableToAttackOnStart(player1AllCards.get("deck"));
        CardHelper.makeCardsUnableToAttackOnStart(player2AllCards.get("deck"));

        int player1Hp = HeroHelper.getInitialHp(player1AllCards.get("deck"));
        int player2Hp = HeroHelper.getInitialHp(player2AllCards.get("deck"));
        player1Hero = new Hero(player1Hp, player1Hp, 0, 0);
        player2Hero = new Hero(player2Hp, player2Hp, 0, 0);

        JSONObject player1Response = new JSONObject();
        JSONObject player2Response = new JSONObject();
        HeroHelper.putHpInfo(player1Response, player1Hp, player2Hp);
        HeroHelper.putHpInfo(player2Response, player2Hp, player1Hp);

        putInitialInfo(player1Response, player1AllCards.get("deck"), player1);
        putInitialInfo(player2Response, player2AllCards.get("deck"), player2);

        sendResponse(player1Response.toString(), player1);
        sendResponse(player2Response.toString(), player2);
    }

    private final int INITIAL_HAND_SIZE = 4;

    private final int HAND_SIZE = 6;

    private void putInitialInfo(JSONObject response, List<Card> deck, GameServer.Client client) {
        response.put("room_action", RoomAction.GET_INITIAL_INFO.toString());
        JSONArray arrayHand = new JSONArray();
        JSONArray arrayDeck = new JSONArray();

        int counter = 1;

        List<Card> handCards = new ArrayList<>();

        for (Card card : deck) {
            if (counter <= INITIAL_HAND_SIZE) {
                handCards.add(card);
                putCardInfo(card, arrayHand);
            }
            else putCardInfo(card, arrayDeck);
            counter++;
        }
        deck.removeAll(handCards);
        if (client.equals(player1)) player1AllCards.put("hand", handCards);
        else player2AllCards.put("hand", handCards);

        response.put("hand", arrayHand);
        response.put("deck", arrayDeck);
    }

    private void putCardInfo(Card card, JSONArray responseCards) {
        JSONObject jsonCard = getJsonCard(card);

        responseCards.put(jsonCard);
    }

    private void putCardInfo(Card card, JSONObject response) {
        JSONObject jsonCard = getJsonCard(card);

        response.put("card", jsonCard);
    }

    private JSONObject getJsonCard(Card card) {
        JSONObject jsonCard = new JSONObject();
        jsonCard.put("atk", card.getAtk());
        jsonCard.put("hp", card.getHp());
        jsonCard.put("cost", card.getCost());
        CardRepository.CardTemplate cardInfo = card.getCardInfo();
        jsonCard.put("id", cardInfo.getId());
        return jsonCard;
    }

    private List<Card> getShuffledDeck(GameServer.Client client) {
        String clientDeck = getClientDeckCardIds(client);
        List<CardRepository.CardTemplate> clientDeckList = CardRepository.getCardsById(clientDeck);
        List<Card> res = clientDeckList.stream()
                .map(cardTemplate -> new Card(cardTemplate))
                .collect(Collectors.toList());

        Collections.shuffle(res);
        return res;
    }

    private String getClientDeckCardIds(GameServer.Client client) {
        String clientLogin = client.getUserLogin();
        String clientCardIdsString = service.get(clientLogin).getDeck();

        return clientCardIdsString;
    }

    private final int BACKGROUND_AMOUNT = 4;

    private void setBackground() {
        JSONObject json = new JSONObject();
        json.put("room_action", RoomAction.GET_BACKGROUND.toString());
        int randomBg = random.nextInt(1, BACKGROUND_AMOUNT + 1);
        json.put("status", "OK");
        json.put("background", "bg_" + randomBg + ".png");
        server.sendResponse(json.toString(), player1);
        server.sendResponse(json.toString(), player2);
    }
}
