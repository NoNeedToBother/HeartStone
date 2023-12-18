package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.ServerMessage;
import ru.kpfu.itis.paramonov.heartstone.ui.DeckCardInfo;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeckController {
    @FXML
    private FlowPane fpCards;

    @FXML
    private ScrollPane spDeckCards;

    @FXML
    private VBox vBoxDeckCards;

    private List<Card> deckCards = new ArrayList<>();

    private List<Card> cards = new ArrayList<>();
    @FXML
    private VBox deck;

    @FXML
    private void initialize() {
        setCards();
        setDeck();
        setButtons();
    }

    private void setButtons() {
        GameButton btnBack = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.BACK)
                .scale(5)
                .build();

        btnBack.setOnMouseClicked(mouseEvent -> {
            GameApplication.getApplication().loadScene("/main_menu.fxml");
            mouseEvent.consume();
        });

        GameButton btnSave = GameButton.builder()
                .setStyle(GameButton.GameButtonStyle.GREEN)
                .setText(GameButton.GameButtonText.SAVE)
                .scale(4)
                .build();

        btnSave.setOnMouseClicked(mouseEvent -> {
            StringBuilder deck = new StringBuilder("[");
            for (Card card : deckCards) {
                deck.append(card.getCardInfo().getId()).append(",");
            }
            String stringDeck = deck.substring(0, deck.length() - 1) + "]";
            String msg = ServerMessage.builder()
                    .setEntityToConnect(ServerMessage.Entity.SERVER)
                    .setServerAction(ServerMessage.ServerAction.UPDATE_DECK)
                    .setParameter("deck", stringDeck)
                    .build();
            GameApplication.getApplication().getClient().sendMessage(msg);

            List<CardRepository.CardTemplate> cardInfos = new ArrayList<>();
            for (Card card : deckCards) {
                cardInfos.add(card.getCardInfo());
            }
            User.getInstance().setDeck(cardInfos);
        });
        deck.getChildren().add(0, btnBack);
        deck.getChildren().add(1, btnSave);
    }

    private final int MAX_DECK_CARDS_AMOUNT = 20;

    private void setCards() {
        fpCards.setHgap(10);
        fpCards.setVgap(10);
        List<CardRepository.CardTemplate> cards = User.getInstance().getCards();
        for (CardRepository.CardTemplate card : cards) {
            Image img = getImage(card);
            ImageView cardIv = new ImageView(img);
            Card deckCard = new Card(card);
            deckCard.associateImageView(cardIv);
            this.cards.add(deckCard);
            fpCards.getChildren().add(cardIv);

            cardIv.setOnMouseClicked(mouseEvent -> {
                Card ivCard = getByImageView(cardIv);
                if (!checkCards(ivCard)) {
                    mouseEvent.consume();
                    return;
                }
                if (deckCards.size() >= MAX_DECK_CARDS_AMOUNT) {
                    mouseEvent.consume();
                    return;
                }
                deckCards.add(ivCard);
                DeckCardInfo deckCardInfo = new DeckCardInfo(ivCard);
                deckCardInfo.setCard(ivCard);
                deckCardInfo.setOnMouseClicked(cardInfoMouseEvent -> {
                    deckCards.remove(deckCardInfo.getCard());
                    vBoxDeckCards.getChildren().remove(deckCardInfo);
                });
                vBoxDeckCards.getChildren().add(deckCardInfo);
            });
        }
    }

    private void setDeck() {
        List<CardRepository.CardTemplate> deck = User.getInstance().getDeck();
        for (CardRepository.CardTemplate cardTemplate : deck) {
            Card card = new Card(cardTemplate);
            deckCards.add(card);
            DeckCardInfo deckCardInfo = new DeckCardInfo(card);
            vBoxDeckCards.getChildren().add(deckCardInfo);
            deckCardInfo.setCard(card);
            deckCardInfo.setOnMouseClicked(cardInfoMouseEvent -> {
                deckCards.remove(deckCardInfo.getCard());
                vBoxDeckCards.getChildren().remove(deckCardInfo);
            });
        }
    }

    private boolean checkCards(Card card) {
        for (Card deckCard : deckCards) {
            if (deckCard.equals(card)) {
                return false;
            }
        }
        return true;
    }

    private Card getByImageView(ImageView iv) {
        for (Card card : cards) {
            if (card.getAssociatedImageView().equals(iv)) return card;
        }
        return null;
    }

    private Image getImage(CardRepository.CardTemplate card) {
        return Card.spriteBuilder()
                .addImage(card.getPortraitUrl())
                .setStyle(Card.CardStyle.BASE.toString())
                .addRarity(card.getRarity())
                .scale(4)
                .build();
    }
}