package cliente;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import comun.Mensaje;

public class PrimaryController {
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
    // Esta referencia se consulta y cambia solo desde el hilo de JavaFX.
    private Conexion conexion;
    private boolean conectado;

    @FXML private void initialize() { btnEnviar.setDisable(true); }
    @FXML private void accionTogglePrivado() { txtDestinatario.setDisable(!chkPrivado.isSelected()); }
    @FXML private void accionConectar() {
        String ip = txtIp.getText().trim();
        String usuario = txtUsuario.getText().trim();
        int puerto;
        try {
            puerto = Integer.parseInt(txtPuerto.getText().trim());
            if (puerto < 1 || puerto > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) { escribir("[ERROR] Puerto no válido (1 a 65535)."); return; }
        if (ip.isEmpty() || usuario.isEmpty() || usuario.length() > 30) {
            escribir("[ERROR] Indica IP y usuario (1 a 30 caracteres)."); return;
        }
        txtUsuario.setText(usuario);
        bloquearDatos(true);
        btnDesconectar.setDisable(false);
        btnEnviar.setDisable(true);
        lblEstado.setText("Conectando...");
        lblEstado.setTextFill(Color.DARKORANGE);
        conexion = new Conexion(this, ip, puerto, usuario);
        conexion.iniciar();
    }
    @FXML private void accionDesconectar() {
        cerrarVentana();
        escribir("[SISTEMA] Te has desconectado.");
    }
    public void cerrarVentana() {
        if (conexion != null) {
            conexion.desconectar();
            conexion = null;
        }
        estadoDesconectado();
    }
    @FXML private void accionEnviar() {
        if (!conectado || conexion == null || btnEnviar.isDisabled()) return;
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty() || texto.length() > 2000) {
            escribir("[ERROR] El mensaje debe tener entre 1 y 2000 caracteres."); return;
        }
        Mensaje mensaje;
        if (chkPrivado.isSelected()) {
            String destino = txtDestinatario.getText().trim();
            if (destino.isEmpty()) { escribir("[ERROR] Indica el destinatario."); return; }
            mensaje = new Mensaje(Mensaje.Tipo.PRIVADO, txtUsuario.getText(), destino, texto);
        } else {
            mensaje = new Mensaje(Mensaje.Tipo.PUBLICO, txtUsuario.getText(), texto);
        }
        btnEnviar.setDisable(true);
        txtMensaje.setDisable(true);
        conexion.enviarEnSegundoPlano(mensaje);
    }
    public void conexionAceptada(Conexion origen) {
        if (conexion != origen) return;
        conectado = true;
        lblEstado.setText("Conectado");
        lblEstado.setTextFill(Color.GREEN);
        btnEnviar.setDisable(false);
        escribir("[SISTEMA] Conectado como " + txtUsuario.getText());
    }
    public void conexionTerminada(Conexion origen, String motivo) {
        if (conexion != origen) return;
        conexion = null;
        estadoDesconectado();
        escribir("[SISTEMA] " + motivo);
    }
    public void envioTerminado(Conexion origen, String error) {
        if (conexion != origen) return;
        txtMensaje.setDisable(false);
        if (error == null) {
            txtMensaje.clear();
            btnEnviar.setDisable(!conectado);
            txtMensaje.requestFocus();
        } else {
            btnEnviar.setDisable(true);
            escribir("[ERROR] " + error);
        }
    }
    public void recibir(Conexion origen, Mensaje mensaje) {
        if (conexion != origen) return;
        if (mensaje.getTipo() == Mensaje.Tipo.PUBLICO) {
            escribir("[PÚBLICO] " + mensaje.getEmisor() + ": " + mensaje.getContenido());
        } else if (mensaje.getTipo() == Mensaje.Tipo.PRIVADO) {
            escribir("[PRIVADO] " + mensaje.getEmisor() + " a " + mensaje.getDestinatario() + ": " + mensaje.getContenido());
        } else {
            escribir("[SISTEMA] " + mensaje.getContenido());
        }
    }
    private void bloquearDatos(boolean bloquear) {
        txtIp.setDisable(bloquear);
        txtPuerto.setDisable(bloquear);
        txtUsuario.setDisable(bloquear);
        btnConectar.setDisable(bloquear);
    }
    private void estadoDesconectado() {
        conectado = false;
        bloquearDatos(false);
        btnDesconectar.setDisable(true);
        btnEnviar.setDisable(true);
        txtMensaje.setDisable(false);
        lblEstado.setText("No conectado");
        lblEstado.setTextFill(Color.RED);
    }
    private void escribir(String texto) { txtAreaChat.appendText(texto + "\n"); }
}
