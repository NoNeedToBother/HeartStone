module ru.kpfu.itis.paramonov.heartstone {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens ru.kpfu.itis.paramonov.heartstone to javafx.fxml;
    exports ru.kpfu.itis.paramonov.heartstone;
}