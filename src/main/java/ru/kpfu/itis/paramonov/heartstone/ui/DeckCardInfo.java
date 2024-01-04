package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class DeckCardInfo extends StackPane {

    private Card card;

    public DeckCardInfo(Card card) {
        init(card);
    }

    private void init(Card card) {
        String src = GameApplication.class.getResource("/assets/images/deck_card_name.png").toString();
        ImageView iv = new ImageView(new Image(src));
        if (card.getCardInfo().getRarity().equals(CardRepository.Rarity.LEGENDARY)) {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(iv.getImage(), null);
            ImageUtil.addImage(bufferedImage, "/assets/images/deck_card_legendary.png");
            iv.setImage(ImageUtil.toImage(bufferedImage));
        }
        Text text = new Text();
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/m3x6.ttf").toString(), 28);
        text.setFont(font);
        text.setFill(Color.WHITE);
        text.setText(card.getCardInfo().getName());
        getChildren().addAll(iv, text);
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }
}
