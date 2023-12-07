package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.model.user.User;
import ru.kpfu.itis.paramonov.heartstone.net.client.GameClient;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {
    @FXML
    private Button btnEndTurn;

    @FXML
    private HBox hBoxCards;

    @FXML
    private AnchorPane root;

    private List<Card> hand = new ArrayList<>();

    private static BattlefieldController controller = null;

    public static BattlefieldController getInstance() {
        return controller;
    }

    @FXML
    private void initialize() {
        controller = this;
        setCards();
        makeCardsDraggable();
    }

    private EventHandler<MouseEvent> getDragEventHandler(ImageView iv, Card card) {
        return mouseEvent -> {
            Dragboard db = iv.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(iv.getImage());
            db.setContent(content);
            mouseEvent.consume();
        };
    }

    private void onBattleStart() {

    }

    private void drawInitialCards() {
        CardRepository.CardTemplate stone = CardRepository.CardTemplate.Stone;
        List<CardRepository.CardTemplate> stubDeck = List.of(stone, stone, stone, stone, stone);
        User.setDeck(stubDeck);

        List<CardRepository.CardTemplate> deck = User.getDeck();

        /*for (int i = 0; i < 5; i++) {
            hand.add()
        }*/

    }

    private void setCards() {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();


        for (int i = 0; i < 5; i++) {
            CardRepository.CardTemplate cardInfo = CardRepository.CardTemplate.Stone;
            Image sprite = Card.SpriteBuilder()
                    .addImage(cardInfo.getPortraitUrl())
                    .addRarity(cardInfo.getRarity())
                    .setBase()
                    .scale(2)
                    .build();

            ImageView img = new ImageView();
            img.setImage(sprite);
            img.hoverProperty().addListener((observable, oldValue, isHovered) -> {
                if (isHovered) {

                }
            });
            hBoxCardsChildren.add(img);

            Card card = new Card(cardInfo);
            card.associateImageView(img);
            hand.add(card);
        }
    }

    private void makeCardsDraggable() {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();
        int counter = 0;

        for (Node element : hBoxCardsChildren) {
            if (element instanceof ImageView) {
                ImageView iv = (ImageView) element;
                iv.setOnDragDetected(getDragEventHandler(iv, hand.get(counter)));
                counter++;
            }
        }
    }
}
