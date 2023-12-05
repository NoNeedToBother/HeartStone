package ru.kpfu.itis.paramonov.heartstone.controller;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {
    @FXML
    private Button btnEndTurn;

    @FXML
    private HBox hBoxCards;

    private List<Card> cards = new ArrayList<>();

    @FXML
    private void initialize() {
        setCards();
    }

    private EventHandler<MouseEvent> getDragEventHandler(ImageView iv) {
        return mouseEvent -> {
            Dragboard db = iv.startDragAndDrop(TransferMode.ANY);
            ClipboardContent content = new ClipboardContent();
            content.putImage(iv.getImage());
            db.setContent(content);
            mouseEvent.consume();

        };
    }

    private void setCards() {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();

        for (int i = 0; i < 5; i++) {
            CardRepository.CardTemplate card = CardRepository.CardTemplate.Stone;
            Image sprite = Card.SpriteBuilder()
                    .addImage(card.getImageUrl())
                    .addRarity(card.getRarity())
                    .setBase()
                    .scale(2)
                    .build();

            ImageView img = new ImageView();
            img.setImage(sprite);
            hBoxCardsChildren.add(img);
        }
    }

    private void makeCardsDraggable() {
        ObservableList<Node> hBoxCardsChildren = hBoxCards.getChildren();

        for (Node element : hBoxCardsChildren) {
            if (element instanceof ImageView) {
                ImageView iv = (ImageView) element;
                iv.setOnDragDetected(getDragEventHandler(iv));
            }
        }
    }
}
