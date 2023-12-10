package ru.kpfu.itis.paramonov.heartstone.model.user;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

import java.util.List;

public class User {

    private static User user;

    private String login;

    private List<CardRepository.CardTemplate> deck;

    private List<CardRepository.CardTemplate> cards;

    private User() {}

    public static User getInstance() {
        if (user == null) {
            user = new User();
        }
        return user;
    }

    public List<CardRepository.CardTemplate> getDeck() {
        return deck;
    }

    public void setDeck(List<CardRepository.CardTemplate> deck) {
        this.deck = deck;
    }

    public List<CardRepository.CardTemplate> getCards() {
        return cards;
    }

    public void setCards(List<CardRepository.CardTemplate> cards) {
        this.cards = cards;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
