package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class CrackingAnimation extends Animation {

    public enum Type{
        CARD, HERO
    }

    private ImageView target;

    private Type type;

    public CrackingAnimation(Type type, ImageView target) {
        this.type = type;
        this.target = target;
    }

    private final int CARD_FRAME_AMOUNT = 5;

    private final Runnable cardCrackingAnimation = () -> {
        for (int i = 1; i <= CARD_FRAME_AMOUNT; i++) {
            delay(100);
            drawFrame(i, "card_cracking/card_cracking_", target);
        }
        invokeOnAnimationEndedListeners();
    };

    private final int HERO_FRAME_AMOUNT = 3;

    private final Runnable heroCrackingCardAnimation = () -> {
        for (int i = 1; i <= HERO_FRAME_AMOUNT; i++) {
            delay(100);
            drawHeroCrackingFrame(i, target);
        }
        invokeOnAnimationEndedListeners();
    };

    private static void drawHeroCrackingFrame(int pos, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = ImageUtil.addImage(img, "/assets/animations/hero_cracking/hero_cracking_" + pos + ".png");
        draw(img, iv);
    }

    @Override
    public void play() {
        Thread thread = null;
        switch (type) {
            case CARD -> thread = new Thread(cardCrackingAnimation);
            case HERO -> thread = new Thread(heroCrackingCardAnimation);
        }
        thread.start();
    }
}
