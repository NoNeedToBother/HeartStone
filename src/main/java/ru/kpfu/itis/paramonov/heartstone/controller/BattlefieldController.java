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
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    @FXML
    private ImageView background;

    private static BattlefieldController controller = null;

    public static BattlefieldController getController() {
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

    private String DEFAULT_IMAGE_PATH = "D:\\projects\\HeartStone\\src\\main\\resources\\assets\\images";

    public void setBackground(String bg) {
        File file = new File(DEFAULT_IMAGE_PATH + "\\background\\" + bg);
        try {
            BufferedImage img = ImageIO.read(file);
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                background.setImage(new Image(in));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
