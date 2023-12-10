package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import ru.kpfu.itis.paramonov.heartstone.model.Sprite;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

public class GameButton extends Button {
    public enum GameButtonText {
        LOGIN, REGISTER,
    }
    public static class ButtonBuilder {
        private BufferedImage img;

        private int imgWidth = 46;
        private int imgHeight = 14;

        private final String DEFAULT_PATH = "D:/projects/HeartStone/src/main/resources/assets/images/buttons";

        public ButtonBuilder() {
            setBlankImg();
        }

        private void setBlankImg() {
            imgWidth = 46;
            imgHeight = 14;
            img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        }

        private ButtonBuilder addImageToBufferedImage(String imgUrl) {
            img = BufferedImageUtil.addImage(img, imgUrl);
            return this;
        }

        public ButtonBuilder setBase() {
            return addImageToBufferedImage(DEFAULT_PATH + "/base_button.png");
        }

        public ButtonBuilder setText(GameButtonText text) {
            switch (text) {
                case LOGIN -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/login.png");
                }
                case REGISTER -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/register.png");
                }
                default -> throw new RuntimeException("Impossible");
            }
        }

        public ButtonBuilder scale(int scale) {
            img = BufferedImageUtil.scale(img, scale);
            imgWidth *= scale;
            imgHeight *= scale;
            return this;
        }

        public GameButton build() {
            GameButton button = new GameButton();
            Image btnImg;
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                setBlankImg();
                btnImg = new Image(in);
                button.setPrefHeight(imgHeight);
                button.setPrefWidth(imgWidth);
                button.setGraphic(new ImageView(btnImg));
                button.setPadding(Insets.EMPTY);
                return button;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static final ButtonBuilder builder = new ButtonBuilder();

    public static ButtonBuilder builder() {
        return builder;
    }
}
