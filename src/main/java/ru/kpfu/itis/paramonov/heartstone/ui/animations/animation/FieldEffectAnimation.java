package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class FieldEffectAnimation extends Animation {
    public enum Type {
        FIRE_CIRCLE
    }

    private final Type type;

    private ImageView fieldEffects;

    public FieldEffectAnimation(Type type, ImageView fieldEffects) {
        this.type = type;
        this.fieldEffects = fieldEffects;
    }

    private final Runnable fieldFireAnimation = () -> {
        int FRAME_AMOUNT = 5;
        Image base = fieldEffects.getImage();
        for (int i = 1; i <= FRAME_AMOUNT; i++) {
            BufferedImage img = SwingFXUtils.fromFXImage(base, null);
            try {
                Thread.sleep(75);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ImageUtil.addImage(img, new Image(GameApplication.class.getResource(DEFAULT_PATH + "fire_effect/field_fire_effect_" + i + ".png").toString()));
            fieldEffects.setImage(ImageUtil.toImage(img));
        }
        fieldEffects.setImage(base);
    };

    @Override
    public void play() {
        Thread thread = null;
        switch (type) {
            case FIRE_CIRCLE -> thread = new Thread(fieldFireAnimation);
        }
        thread.start();
    }
}
