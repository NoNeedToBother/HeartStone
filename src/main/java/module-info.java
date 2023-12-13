module ru.kpfu.itis.paramonov.heartstone {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires java.sql;
    requires org.json;

    requires org.controlsfx.controls;
    requires jakarta.xml.bind;
    requires org.postgresql.jdbc;

    opens ru.kpfu.itis.paramonov.heartstone to javafx.fxml;
    opens ru.kpfu.itis.paramonov.heartstone.controller to javafx.fxml;
    exports ru.kpfu.itis.paramonov.heartstone;
    exports ru.kpfu.itis.paramonov.heartstone.controller to javafx.fxml;
    exports ru.kpfu.itis.paramonov.heartstone.ui to javafx.fxml;
}