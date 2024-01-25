package ru.kpfu.itis.paramonov.heartstone.ui.battle;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class ManaBar extends VBox {
    private final ImageView iv = new ImageView();

    private StackPane spText = new StackPane();
    private final Text text = new Text();

    public ManaBar() {
        init();
    }

    private final String DEFAULT_PATH = "/assets/images/mana_bar/";

    private void init() {
        String src = GameApplication.class.getResource(DEFAULT_PATH + "text_bg.png").toString();
        spText = new StackPane(new ImageView(new Image(src)), text);
        this.getChildren().addAll(iv, spText);
        text.setStyle("-fx-font-family: Impact; -fx-font-size: 32px; -fx-text-alignment: center;");
    }

    public void setMana(int manaAmount, int maxAmount) {
        setImageView(manaAmount);
        setText(manaAmount, maxAmount);
    }

    private void setImageView(int manaAmount) {
        String src = DEFAULT_PATH + "mana_" + manaAmount + ".png";
        iv.setImage(new Image(GameApplication.class.getResource(src).toString()));
    }

    private void setText(int manaAmount, int maxAmount) {
        spText.getChildren().remove(text);
        text.setText("" + manaAmount + "/" + maxAmount);
        spText.getChildren().add(text);
    }

}
