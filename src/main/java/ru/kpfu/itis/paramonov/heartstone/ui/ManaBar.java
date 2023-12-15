package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class ManaBar extends VBox {
    private ImageView iv = new ImageView();

    private StackPane spText = new StackPane();
    private Text text = new Text();

    public ManaBar() {
        init();
    }

    private void init() {
        String src = GameApplication.class.getResource("/assets/images/mana_bar/text_bg.png").toString();
        spText = new StackPane(new ImageView(new Image(src)), text);
        this.getChildren().addAll(iv, spText);
        text.setStyle("-fx-font-family: Impact; -fx-font-size: 32px; -fx-text-alignment: center;");
    }

    public void setMana(int manaAmount, int maxAmount) {
        setImageView(manaAmount);
        setText(manaAmount, maxAmount);
    }

    private void setImageView(int manaAmount) {
        String src = "/assets/images/mana_bar/mana_" + manaAmount + ".png";
        iv.setImage(new Image(GameApplication.class.getResource(src).toString()));
    }

    private void setText(int manaAmount, int maxAmount) {
        spText.getChildren().remove(text);
        text.setText("" + manaAmount + "/" + maxAmount);
        spText.getChildren().add(text);
    }

}
