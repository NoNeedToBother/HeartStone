package ru.kpfu.itis.paramonov.heartstone.net.server.util;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.service.UserService;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.net.ServerResponse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PackOpeningUtil {
    private final static Random random = new Random();

    private final static int ONE_PACK_COST = 100;

    private final static int FIVE_PACK_COST = 5 * ONE_PACK_COST;

    private static User getUser(JSONObject msg) {
        String login = msg.getString("login");
        UserService userService = new UserService();
        return userService.get(login);
    }

    public static void openOnePack(JSONObject msg, ServerResponse response) {
        response.putAction(ServerMessage.ServerAction.OPEN_ONE_PACK);
        User user = getUser(msg);

        if (!buyPack(response, user, ONE_PACK_COST)) return;
        Integer cardId = getRandomCard();
        response.putParameter("card_id", cardId);
        updateCards(response, user.login(), List.of(cardId));
    }

    public static void openFivePacks(JSONObject msg, ServerResponse response) {
        response.putAction(ServerMessage.ServerAction.OPEN_FIVE_PACKS);
        User user = getUser(msg);

        if (!buyPack(response, user, FIVE_PACK_COST)) return;
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Integer cardId = getRandomCard();
            ids.add(cardId);
        }
        response.putIntegers("card_ids", ids);

        updateCards(response, user.login(), ids);
    }

    private static boolean buyPack(ServerResponse response, User user, int packCost) {
        if (user.money() < packCost) {
            response.putStatus("not_ok");
            response.putReason("Not enough gold");
            return false;
        }

        UserService userService = new UserService();
        try {
            userService.updateMoney(user.login(), user.money() - packCost);
        }  catch (SQLException e) {
            response.putStatus("not_ok");
            response.putReason("Failed to connect database, please try again later");
            return false;
        }
        return true;
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

    private static void updateCards(ServerResponse response, String login, List<Integer> cardIds) {
        UserService service = new UserService();
        User user = service.get(login);
        String cards = user.cards();
        List<CardRepository.CardTemplate> userCards = CardRepository.getCardsById(cards);
        List<CardRepository.CardTemplate> gottenCards = CardRepository.getCardsById(cardIds);

        int newUserMoney = user.money();
        for (CardRepository.CardTemplate gottenCard : gottenCards) {
            newUserMoney += checkCopiesAndGetMoney(userCards, gottenCard, user);
        }
        try {
            service.updateMoney(login, newUserMoney);
        } catch (SQLException e) {
            response.putStatus("not_ok");
            response.putReason("Failed to connect database, please try again later");
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
            response.putStatus("not_ok");
            response.putReason("Failed to connect database, please try again later");
            return;
        }
        response.putParameter("cards", cardIdsString);
        response.putParameter("money", service.get(login).money());
        response.putStatus("ok");
    }

    private static int checkCopiesAndGetMoney(List<CardRepository.CardTemplate> userCards,
                                              CardRepository.CardTemplate card, User user) {
        int maxCardAmount;
        if (card.getRarity().equals(CardRepository.Rarity.LEGENDARY)) maxCardAmount = 1;
        else maxCardAmount = 2;

        int cardAmount = 0;

        for (CardRepository.CardTemplate userCard : userCards) {
            if (userCard.equals(card)) cardAmount++;
        }

        int res = 0;
        if (cardAmount >= maxCardAmount) res += addGoldForCopy(card);
        else userCards.add(card);
        return res;
    }

    private final static int GOLD_COMMON = 20;
    private final static int GOLD_RARE = 40;
    private final static int GOLD_EPIC = 100;
    private final static int GOLD_LEGENDARY = 300;

    private static int addGoldForCopy(CardRepository.CardTemplate card) {
        int res = 0;
        switch (card.getRarity()) {
            case COMMON -> res = GOLD_COMMON;
            case RARE -> res = GOLD_RARE;
            case EPIC -> res = GOLD_EPIC;
            case LEGENDARY -> res = GOLD_LEGENDARY;
        }
        return res;
    }
}
