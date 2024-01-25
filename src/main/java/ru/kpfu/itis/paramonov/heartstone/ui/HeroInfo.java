package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.user.Hero;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class HeroInfo extends HBox implements Sprite {

    public enum HeroStyle {
        BASE
    }

    private Hero hero;

    private final ImageView portrait = new ImageView();

    private final StackPane healthSp = new StackPane();

    private final Text text = new Text();

    public HeroInfo() {
        init();
    }

    private void init() {
        setStackPane();
        this.getChildren().addAll(portrait, healthSp);
    }

    private void setStackPane() {
        String src = GameApplication.class.getResource("/assets/images/heroes/health.png").toString();
        ImageView health = new ImageView(src);
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/ThaleahFat.ttf").toString(), 32);
        text.setFont(font);
        healthSp.getChildren().addAll(health, text);
    }

    public void changeHealth(Integer health) {
        if (health != null) text.setText(String.valueOf(health));
    }

    public void setPortrait(Image portrait) {
        this.portrait.setImage(portrait);
    }

    public ImageView getPortrait() {
        return portrait;
    }

    public static class HeroSpriteBuilder implements Sprite.SpriteBuilder<Image> {

        private BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        private final String DEFAULT_PATH = "/assets/images/heroes";

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            img = ImageUtil.addImage(img, imgUrl);
            return this;
        }

        @Override
        public SpriteBuilder<Image> setStyle(String style) {
            switch (HeroStyle.valueOf(style)) {
                case BASE -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/base_hero.png");
                }
                default -> throw new RuntimeException("Not supported style");
            }
        }

        @Override
        public SpriteBuilder<Image> addImage(String imgUrl) {
            return addImageToBufferedImage(DEFAULT_PATH + imgUrl);
        }

        @Override
        public SpriteBuilder<Image> scale(double scale) {
            img = ImageUtil.scale(img, scale);
            return this;
        }

        @Override
        public Image build() {
            return ImageUtil.toImage(img);
        }
    }

    public static HeroSpriteBuilder spriteBuilder() {
        return new HeroSpriteBuilder();
    }

    public Hero getHero() {
        return hero;
    }

    public void setHero(Hero hero) {
        this.hero = hero;
    }
}
