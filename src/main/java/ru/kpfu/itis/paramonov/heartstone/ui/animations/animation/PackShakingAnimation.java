package ru.kpfu.itis.paramonov.heartstone.ui.animations.animation;

import javafx.animation.TranslateTransition;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.ui.animations.Animation;

import java.util.concurrent.atomic.AtomicReference;

public class PackShakingAnimation extends Animation {

    private ImageView pack;

    public PackShakingAnimation(ImageView pack) {
        this.pack = pack;
    }

    @Override
    public void play() {
        AtomicReference<TranslateTransition> transition = new AtomicReference<>();
        playTransition(transition, pack, 25, 0, 150);
        transition.get().setOnFinished(actionEvent -> {
            playTransition(transition, pack, -50, 0, 150);
            transition.get().setOnFinished(actionEvent1 -> {
                playTransition(transition, pack, 75, 0, 100);
                transition.get().setOnFinished(actionEvent2 -> {
                    playTransition(transition, pack, -100, 0, 100);
                    transition.get().setOnFinished(actionEvent3 -> {
                        playTransition(transition, pack, 50, 0, 75);
                        transition.get().setOnFinished(actionEvent4 -> invokeOnAnimationEndedListeners());
                    });
                });
            });
        });
    }
}
