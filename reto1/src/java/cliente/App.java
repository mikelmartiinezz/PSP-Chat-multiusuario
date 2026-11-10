package cliente;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class App extends Application {
    private PrimaryController controlador;
    public void start(Stage stage) throws Exception {
        FXMLLoader cargador = new FXMLLoader(getClass().getResource("chat.fxml"));
        Parent vista = cargador.load();
        controlador = cargador.getController();
        stage.setScene(new Scene(vista, 650, 450));
        stage.setTitle("Chat Multiusuario - Cliente");
        stage.show();
    }
    public void stop() {
        if (controlador != null) controlador.cerrarVentana();
    }
    public static void main(String[] args) { launch(args); }
}
