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
import javafx.scene.layout.VBox;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.GameButton;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import java.util.ArrayList;
import java.util.List;

public class BattlefieldController {

    private Button btnEndTurn;

    @FXML
    private VBox vBoxBtnEndTurn;

    @FXML
    private HBox hBoxCards;

    @FXML
    private AnchorPane root;

    private List<Card> hand = new ArrayList<>();

    @FXML
    private ImageView background;

    @FXML
    private ImageView handBg;

    private static BattlefieldController controller = null;

    public static BattlefieldController getController() {
        return controller;
    }

    @FXML
    private void initialize() {
        controller = this;
        setCards();
        setHandBackground();
        addEndTurnBtn();
        makeCardsDraggable();
    }

    private void addEndTurnBtn() {
        GameButton btnEndTurn = GameButton.builder()
                .setBase()
                .setText(GameButton.GameButtonText.END_TURN)
                .scale(3)
                .build();

        vBoxBtnEndTurn.getChildren().add(btnEndTurn);
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
            CardRepository.CardTemplate cardInfo;
            if (i % 2 == 0) cardInfo = CardRepository.CardTemplate.Stone;
            else cardInfo = CardRepository.CardTemplate.KnightStone;
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
        BufferedImageUtil.getFromSrcAndSetImage("\\background\\" + bg, background);
        /*
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
        }*/
    }

    public void setHandBackground() {
        BufferedImageUtil.getFromSrcAndSetImage("\\hand_bg.png", handBg);
        /*
        File file = new File(DEFAULT_IMAGE_PATH + "hand_bg");
        try {
            BufferedImage img = ImageIO.read(file);
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                handBg.setImage(new Image(in));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
