package reto1.cliente;

import java.io.ObjectInputStream;
import reto1.modelo.Mensaje;
import javafx.application.Platform;

public class HiloReceptorCliente extends Thread {
    private final ObjectInputStream entrada;
    private final PrimaryController controller;
    private boolean activo = true;

    public HiloReceptorCliente(ObjectInputStream entrada, PrimaryController controller) {
        this.entrada = entrada;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            while (activo) {
                Mensaje msg = (Mensaje) entrada.readObject();
                // Platform.runLater actualiza la vista de JavaFX de forma segura
                Platform.runLater(() -> controller.procesarMensajeEntrante(msg));
            }
        } catch (Exception e) {
            if (activo) {
                Platform.runLater(() -> controller.mostrarErrorDesconexion("Conexión perdida con el servidor."));
            }
        }
    }

    public void detener() {
        this.activo = false;
    }
}