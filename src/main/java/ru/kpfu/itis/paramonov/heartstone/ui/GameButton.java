package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.util.BufferedImageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class GameButton extends Button {
    public enum GameButtonText {
        LOGIN, REGISTER, GO_LOGIN, GO_REGISTER, PLAY, QUIT, DECK, END_TURN, OPEN_PACKS
    }

    public enum GameButtonStyle {
        BASE, GREEN, RED, GOLD_100, GOLD_500
    }

    private boolean clickable = false;

    public boolean isClickable() {
        return clickable;
    }

    public static class ButtonBuilder {

        GameButton btn = new GameButton();

        private BufferedImage img;

        private int imgWidth = 46;
        private int imgHeight = 14;

        private final String DEFAULT_PATH = "/assets/images/buttons";

        public ButtonBuilder() {
            setBlankImg();
        }

        private void setBlankImg() {
            btn = new GameButton();
            imgWidth = 46;
            imgHeight = 14;
            img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        }

        private ButtonBuilder addImageToBufferedImage(String imgUrl) {
            img = BufferedImageUtil.addImage(img, imgUrl);
            return this;
        }

        public ButtonBuilder setStyle(GameButtonStyle style) {
            switch (style) {
                case BASE -> {
                    btn.clickable = true;
                    return addImageToBufferedImage(DEFAULT_PATH + "/base_button.png");
                }
                case RED -> {
                    btn.clickable = false;
                    return addImageToBufferedImage(DEFAULT_PATH + "/red_button.png");
                }
                case GREEN -> {
                    btn.clickable = true;
                    return addImageToBufferedImage(DEFAULT_PATH + "/green_button.png");
                }
                case GOLD_100 -> {
                    btn.clickable = true;
                    return addImageToBufferedImage(DEFAULT_PATH + "/100g_button.png");
                }
                case GOLD_500 -> {
                    btn.clickable = true;
                    return addImageToBufferedImage(DEFAULT_PATH + "/500g_button.png");
                }
                default -> throw new RuntimeException("Impossible");
            }
        }

        public ButtonBuilder setText(GameButtonText text) {
            switch (text) {
                case LOGIN -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/login.png");
                }
                case REGISTER -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/register.png");
                }
                case GO_LOGIN -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/go_login.png");
                }
                case GO_REGISTER -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/go_register.png");
                }
                case PLAY -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/play.png");
                }
                case QUIT -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/quit.png");
                }
                case DECK -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/deck.png");
                }
                case END_TURN -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/end_turn.png");
                }
                case OPEN_PACKS -> {
                    return addImageToBufferedImage(DEFAULT_PATH + "/labels/open_packs.png");
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
            Image btnImg;
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(img, "PNG", out);
                InputStream in = new ByteArrayInputStream(out.toByteArray());
                btnImg = new Image(in);
                btn.setPrefHeight(imgHeight);
                btn.setPrefWidth(imgWidth);
                btn.setGraphic(new ImageView(btnImg));
                btn.setPadding(Insets.EMPTY);
                return btn;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static ButtonBuilder builder() {
        return new ButtonBuilder();
    }
}
