package reto1.cliente;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import reto1.modelo.Mensaje;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

public class PrimaryController {

    // Elementos visuales declarados en chat.fxml
    @FXML private TextField txtIp;
    @FXML private TextField txtPuerto;
    @FXML private TextField txtUsuario;
    @FXML private Button btnConectar;
    @FXML private Button btnDesconectar;
    @FXML private Label lblEstado;

    @FXML private TextArea txtAreaChat;

    @FXML private CheckBox chkPrivado;
    @FXML private TextField txtDestinatario;
    @FXML private TextField txtMensaje;
    @FXML private Button btnEnviar;

    // Sockets y Flujos de Objetos Serializados (estilo visto en clase)
    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private HiloReceptorCliente hiloReceptor;

    @FXML
    private void accionTogglePrivado() {
        // Habilita o deshabilita la caja de texto del destinatario
        txtDestinatario.setDisable(!chkPrivado.isSelected());
    }

    @FXML
    private void accionConectar() {
        String ip = txtIp.getText().trim();
        String puertoStr = txtPuerto.getText().trim();
        String usuario = txtUsuario.getText().trim();

        if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
            txtAreaChat.appendText("[SISTEMA] Rellena IP, Puerto y Usuario.\n");
            return;
        }

        try {
            int puerto = Integer.parseInt(puertoStr);
            socket = new Socket(ip, puerto);

            // Importante: Crear salida antes que entrada
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            // Petición inicial de conexión enviando objeto Mensaje
            salida.writeObject(new Mensaje(Mensaje.Tipo.CONECTAR, usuario, null));
            salida.flush();

            // Iniciar hilo receptor independiente para no congelar JavaFX
            hiloReceptor = new HiloReceptorCliente(entrada, this);
            hiloReceptor.start();

            // Cambiar estados de botones e interfaz
            btnConectar.setDisable(true);
            btnDesconectar.setDisable(false);
            txtIp.setDisable(true);
            txtPuerto.setDisable(true);
            txtUsuario.setDisable(true);

            lblEstado.setText("Conectado");
            lblEstado.setTextFill(Color.GREEN);

            txtAreaChat.appendText("[SISTEMA] Conectado al servidor como " + usuario + "\n");

        } catch (Exception e) {
            txtAreaChat.appendText("[ERROR] No se pudo conectar: " + e.getMessage() + "\n");
        }
    }

    @FXML
    private void accionDesconectar() {
        limpiarConexion();
        txtAreaChat.appendText("[SISTEMA] Te has desconectado.\n");
    }

    @FXML
    private void accionEnviar() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty() || salida == null) return;

        try {
            if (chkPrivado.isSelected()) {
                String destinatario = txtDestinatario.getText().trim();
                if (destinatario.isEmpty()) {
                    txtAreaChat.appendText("[SISTEMA] Escribe el nombre del destinatario para el mensaje privado.\n");
                    return;
                }
                salida.writeObject(new Mensaje(Mensaje.Tipo.PRIVADO, txtUsuario.getText(), destinatario, texto));
            } else {
                salida.writeObject(new Mensaje(Mensaje.Tipo.PUBLICO, txtUsuario.getText(), texto));
            }
            salida.flush();
            txtMensaje.clear();
        } catch (Exception e) {
            txtAreaChat.appendText("[ERROR] Error al enviar mensaje: " + e.getMessage() + "\n");
        }
    }

    public void procesarMensajeEntrante(Mensaje msg) {
        switch (msg.getTipo()) {
            case PUBLICO:
                txtAreaChat.appendText("[PÚBLICO] " + msg.getEmisor() + ": " + msg.getContenido() + "\n");
                break;
            case PRIVADO:
                txtAreaChat.appendText("[PRIVADO] " + msg.getEmisor() + " -> " + msg.getDestinatario() + ": " + msg.getContenido() + "\n");
                break;
            case SISTEMA:
                txtAreaChat.appendText("[SISTEMA] " + msg.getContenido() + "\n");
                break;
            default:
                break;
        }
    }

    public void mostrarErrorDesconexion(String motivo) {
        txtAreaChat.appendText("[SISTEMA] " + motivo + "\n");
        limpiarConexion();
    }

    private void limpiarConexion() {
        try {
            if (salida != null) {
                salida.writeObject(new Mensaje(Mensaje.Tipo.DESCONECTAR, txtUsuario.getText(), null));
                salida.flush();
            }
            if (hiloReceptor != null) hiloReceptor.detener();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}

        btnConectar.setDisable(false);
        btnDesconectar.setDisable(true);
        txtIp.setDisable(false);
        txtPuerto.setDisable(false);
        txtUsuario.setDisable(false);

        lblEstado.setText("No conectado");
        lblEstado.setTextFill(Color.RED);
    }
}