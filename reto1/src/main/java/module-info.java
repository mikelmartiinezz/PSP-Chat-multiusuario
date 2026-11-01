module reto1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens reto1.cliente to javafx.fxml;
    exports reto1.cliente;
    exports reto1.modelo;
    exports reto1.servidor;
}
