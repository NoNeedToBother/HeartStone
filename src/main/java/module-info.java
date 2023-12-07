module ru.kpfu.itis.paramonov.heartstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.json;

    requires org.controlsfx.controls;

    opens ru.kpfu.itis.paramonov.heartstone to javafx.fxml;
    opens ru.kpfu.itis.paramonov.heartstone.controller to javafx.fxml;
    exports ru.kpfu.itis.paramonov.heartstone;
    exports ru.kpfu.itis.paramonov.heartstone.controller to javafx.fxml;
}