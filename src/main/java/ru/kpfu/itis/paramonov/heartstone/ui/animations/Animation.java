package ru.kpfu.itis.paramonov.heartstone.ui.animations;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.util.ImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Animation {

    @FunctionalInterface
    public interface OnAnimationEndedListener {
        void onAnimationEnded(Animation animation);
    }

    private final List<OnAnimationEndedListener> onAnimationEndedListeners = new ArrayList<>();

    public void addOnAnimationEndedListener(OnAnimationEndedListener listener) {
        onAnimationEndedListeners.add(listener);
    }

    public abstract void play();

    protected void invokeOnAnimationEndedListeners() {
        Platform.runLater(() -> {
            for (OnAnimationEndedListener listener : onAnimationEndedListeners) {
                listener.onAnimationEnded(this);
            }
        });
    }

    protected final String DEFAULT_PATH = "/assets/animations/";

    protected void drawFrame(int pos, String src, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = ImageUtil.addImage(img, DEFAULT_PATH + src + pos + ".png");
        draw(img, iv);
    }

    protected void drawFrame(String src, ImageView iv) {
        BufferedImage img = SwingFXUtils.fromFXImage(iv.getImage(), null);
        img = ImageUtil.addImage(img, DEFAULT_PATH + src);
        draw(img, iv);
    }

    protected static void draw(BufferedImage img, ImageView iv) {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "PNG", out);
            InputStream in = new ByteArrayInputStream(out.toByteArray());
            iv.setImage(new Image(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
