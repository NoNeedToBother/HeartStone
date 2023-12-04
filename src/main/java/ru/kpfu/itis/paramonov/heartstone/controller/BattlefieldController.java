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
import javafx.scene.layout.StackPane;
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

        CardRepository.CardTemplate card = CardRepository.CardTemplate.Stone;
        Image sprite = Card.Builder()
                .addImage(card.getImageUrl())
                .addRarity(card.getRarity())
                .setBase()
                .build();

        ImageView img = new ImageView();
        img.setImage(sprite);
        hBoxCardsChildren.add(img);
        /*
        for (Node element : hBoxCardsChildren) {
            if (element.getClass().equals(ImageView.class)) {
                ImageView iv = (ImageView) element;
                iv.setImage(new Image("/base_card.png"));
                iv.setOnDragDetected(getDragEventHandler(iv));
            }
        }*/

    }
}
