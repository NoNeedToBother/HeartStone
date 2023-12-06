package ru.kpfu.itis.paramonov.heartstone.model.user;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.List;

public class User {

    private static User user;

    private User() {}

    public User instance() {
        if (user == null) {
            user = new User();
        }
        return user;
    }

    private static List<CardRepository.CardTemplate> deck;

    public static List<CardRepository.CardTemplate> getDeck() {
        return deck;
    }

    public static void setDeck(List<CardRepository.CardTemplate> deck) {
        User.deck = deck;
    }
}
