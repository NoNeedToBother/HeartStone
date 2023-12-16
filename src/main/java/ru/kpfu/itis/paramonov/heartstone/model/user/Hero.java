package ru.kpfu.itis.paramonov.heartstone.model.user;

import javafx.scene.image.Image;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import java.awt.image.BufferedImage;

public class Hero implements Sprite {
    public enum HeroStyle {
        BASE
    }

    private int hp;

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public static class HeroSpriteBuilder implements SpriteBuilder<Image> {

        private BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        private final String DEFAULT_PATH = "/assets/images/heroes";

        private SpriteBuilder<Image> addImageToBufferedImage(String imgUrl) {
            img = BufferedImageUtil.addImage(img, imgUrl);
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
        public SpriteBuilder<Image> scale(int scale) {
            img = BufferedImageUtil.scale(img, scale);
            return this;
        }

        @Override
        public Image build() {
            return BufferedImageUtil.toImage(img);
        }
    }
}
