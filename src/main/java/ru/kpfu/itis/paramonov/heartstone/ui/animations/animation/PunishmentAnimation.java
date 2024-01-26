package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import java.awt.image.BufferedImage;

public class PunishmentAnimation extends Animation {

    private final int id;

    private final ImageView hero;

    public PunishmentAnimation(ImageView hero, int id) {
        this.id = id;
        this.hero = hero;
    }

    private Runnable getHeroOverlayAnimation(String path) {
        return () -> {
            Image before = hero.getImage();
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(hero.getImage(), null);
            ImageUtil.addImage(bufferedImage, DEFAULT_PATH + path);
            hero.setImage(ImageUtil.toImage(bufferedImage));
            delay(250);
            hero.setImage(before);
        };
    }

    @Override
    public void play() {
        Thread thread = null;
        if (id == CardRepository.CardTemplate.TheRock.getId()) {
            Runnable runnable = getHeroOverlayAnimation("card_effects/rock_punishment.png");
            thread = new Thread(runnable);
        }
        thread.start();
    }
}
