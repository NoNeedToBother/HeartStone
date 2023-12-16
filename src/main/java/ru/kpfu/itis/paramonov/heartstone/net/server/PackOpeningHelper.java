package ru.kpfu.itis.paramonov.heartstone.net.server;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PackOpeningHelper {
    private static Random random = new Random();

    private static int ONE_PACK_COST = 100;

    private static int FIVE_PACK_COST = 5 * ONE_PACK_COST;

    private static User getUser(JSONObject msg) {
        String login = msg.getString("login");
        UserService userService = new UserService();
        return userService.get(login);
    }

    public static void openOnePack(JSONObject msg, JSONObject response) {
        response.put("server_action", ServerMessage.ServerAction.OPEN_1_PACK.toString());
        User user = getUser(msg);

        if (user.getMoney() < ONE_PACK_COST) {
            response.put("status", "not_ok");
            response.put("reason", "Not enough gold");
            return;
        }

        UserService userService = new UserService();
        try {
            userService.updateMoney(user.getLogin(), user.getMoney() - ONE_PACK_COST);
        }  catch (SQLException e) {
            response.put("status", "not_ok");
            response.put("reason", "Failed to connect database, please try again later.");
            return;
        }
        Integer cardId = getRandomCard();
        response.put("card_id", cardId);
        updateCards(response, user.getLogin(), List.of(cardId));
    }

    public static void openFivePacks(JSONObject msg, JSONObject response) {
        response.put("server_action", ServerMessage.ServerAction.OPEN_5_PACKS.toString());
        User user = getUser(msg);

        if (user.getMoney() < FIVE_PACK_COST) {
            response.put("status", "not_ok");
            response.put("reason", "Not enough gold");
            return;
        }

        UserService userService = new UserService();
        try {
            userService.updateMoney(user.getLogin(), user.getMoney() - FIVE_PACK_COST);
        }  catch (SQLException e) {
            response.put("status", "not_ok");
            response.put("reason", "Failed to connect database, please try again later.");
            return;
        }
        JSONArray cardIds = new JSONArray();
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Integer cardId = getRandomCard();
            cardIds.put(cardId);
            ids.add(cardId);
        }
        response.put("card_ids", cardIds);

        updateCards(response, user.getLogin(), ids);
    }

    private static CardRepository.Rarity calculateRarity() {
        double randomNum = random.nextDouble();
        if (randomNum <= 0.5) return CardRepository.Rarity.COMMON;
        else {
            randomNum = random.nextDouble();
            if (randomNum <= 0.6) return CardRepository.Rarity.RARE;
            else {
                randomNum = random.nextDouble();
                if (randomNum <= 0.2) return CardRepository.Rarity.LEGENDARY;
                else return CardRepository.Rarity.EPIC;
            }
        }
    }

    private static Integer getRandomId(List<Integer> ids) {
        int randomNum = random.nextInt(ids.size());
        return ids.get(randomNum);
    }

    private static Integer getRandomCard() {
        CardRepository.Rarity rarity = calculateRarity();
        Integer cardId = getRandomId(CardRepository.getCardsByRarity(rarity));
        return cardId;
    }

    private static void updateCards(JSONObject response, String login, List<Integer> cardIds) {
        UserService service = new UserService();
        User user = service.get(login);
        String cards = user.getCards();
        List<CardRepository.CardTemplate> userCards = CardRepository.getCardsById(cards);
        List<CardRepository.CardTemplate> gottenCards = CardRepository.getCardsById(cardIds);

        for (CardRepository.CardTemplate gottenCard : gottenCards) {
            checkIfNotEnoughCopies(userCards, gottenCard, user);
        }
        try {
            service.updateMoney(login, user.getMoney());
        } catch (SQLException e) {
            response.put("status", "not_ok");
            response.put("reason", "Failed to connect database, please try again later.");
        }

        StringBuilder cardIdsString = new StringBuilder("[");
        for (int i = 0; i < userCards.size(); i++) {
            cardIdsString.append(userCards.get(i).getId());
            if (i != userCards.size() - 1) cardIdsString.append(",");
        }
        cardIdsString.append("]");

        try {
            service.updateCards(login, cardIdsString.toString());
        } catch (SQLException e) {
            response.put("status", "not_ok");
            response.put("reason", "Failed to connect database, please try again later.");
            return;
        }
        response.put("cards", cardIdsString.toString());
        response.put("money", service.get(login).getMoney());
        response.put("status", "ok");
    }

    private static void checkIfNotEnoughCopies(List<CardRepository.CardTemplate> userCards,
            CardRepository.CardTemplate card, User user) {
        int maxCardAmount;
        if (card.getRarity().equals(CardRepository.Rarity.LEGENDARY)) maxCardAmount = 1;
        else maxCardAmount = 2;

        int cardAmount = 0;

        for (CardRepository.CardTemplate userCard : userCards) {
            if (userCard.equals(card)) cardAmount++;
        }
        System.out.println(card.getName() + cardAmount);

        if (cardAmount >= maxCardAmount) addGoldForCopy(card, user);
        else userCards.add(card);

    }

    private static int GOLD_COMMON = 25;
    private static int GOLD_RARE = 50;
    private static int GOLD_EPIC = 150;
    private static int GOLD_LEGENDARY = 500;

    private static void addGoldForCopy(CardRepository.CardTemplate card, User user) {
        int userMoney = user.getMoney();
        switch (card.getRarity()) {
            case COMMON -> user.setMoney(userMoney + GOLD_COMMON);
            case RARE -> user.setMoney(userMoney + GOLD_RARE);
            case EPIC -> user.setMoney(userMoney + GOLD_EPIC);
            case LEGENDARY -> user.setMoney(userMoney + GOLD_LEGENDARY);
        }
    }
}
