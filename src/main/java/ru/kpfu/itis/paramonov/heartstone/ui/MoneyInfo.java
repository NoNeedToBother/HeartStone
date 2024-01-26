package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class MoneyInfo extends StackPane {
    private final ImageView iv = new ImageView();

    private final Text money = new Text();

    public MoneyInfo() {
        init();
    }

    private void init() {
        setImageView();
        setTextStyle();
        this.getChildren().addAll(iv, money);
    }

    private void setImageView() {
        String src = GameApplication.class.getResource("/assets/images/money.png").toString();
        iv.setImage(new Image(src));
    }

    private void setTextStyle() {
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/m3x6.ttf").toString(), 48);
        money.setFont(font);
        setAlignment(money, Pos.CENTER);
    }

    public void setMoney(int money) {
        this.money.setText(String.valueOf(money));
    }
}
