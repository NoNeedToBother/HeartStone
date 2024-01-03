package ru.kpfu.itis.paramonov.heartstone.model.user;

import javafx.scene.image.Image;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class Hero implements Sprite {
    public enum HeroStyle {
        BASE
    }

    private int hp;

    private int maxHp;

    private int mana;

    private int maxMana;

    public Hero(int hp, int maxHp, int mana, int maxMana) {
        this.hp = hp;
        this.maxHp = maxHp;
        this.mana = mana;
        this.maxMana = maxMana;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        this.mana = mana;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
    }

    public static class HeroSpriteBuilder implements SpriteBuilder<Image> {

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
}
