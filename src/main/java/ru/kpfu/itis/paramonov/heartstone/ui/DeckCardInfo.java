package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public class DeckCardInfo extends StackPane {
    public DeckCardInfo(Card card) {
        init(card);
    }

    private void init(Card card) {
        String src;
        if (card.getCardInfo().getRarity().equals(CardRepository.Rarity.LEGENDARY)) {
            src = GameApplication.class.getResource("/assets/images/deck_card_name_legendary.png").toString();
        } else {
            src = GameApplication.class.getResource("/assets/images/deck_card_name.png").toString();
        }
        ImageView iv = new ImageView(new Image(src));
        Text text = new Text();
        text.setStyle("-fx-font-size: 16;");
        text.setFill(Color.WHITE);
        text.setText(card.getCardInfo().getName());
        getChildren().addAll(iv, text);
    }
}
