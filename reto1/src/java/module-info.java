module reto {
    requires javafx.controls;
    requires javafx.fxml;
    opens cliente to javafx.fxml;
    exports cliente;
    exports servidor;
}
