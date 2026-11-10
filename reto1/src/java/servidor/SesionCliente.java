package servidor;
import java.io.*;
import java.net.Socket;
import comun.Mensaje;
import comun.Hilo;

public class SesionCliente {
    private final Socket socket;
    private final Servidor gestor;
    private ObjectOutputStream salida;
    private String nombreUsuario;
    private Hilo hilo;
    public SesionCliente(Socket socket, Servidor gestor) {
        this.socket = socket;
        this.gestor = gestor;
    }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombre) { nombreUsuario = nombre; }
    public void iniciar() {
        hilo = new Hilo("servidor-cliente", new Runnable() {
            public void run() { atender(); }
        });
        hilo.start();
    }
    public void esperarFin() {
        if (hilo != null) {
            try { hilo.join(2000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    private void atender() {
        try {
            socket.setSoTimeout(5000); // Tiempo máximo para identificarse.
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
            Object objeto = entrada.readObject();
            if (!(objeto instanceof Mensaje)) throw new IOException("Objeto no válido.");
            Mensaje mensaje = (Mensaje) objeto;
            if (mensaje.getTipo() != Mensaje.Tipo.CONECTAR) throw new IOException("Primero debes identificarte.");
            String error = gestor.registrarUsuario(this, mensaje.getEmisor());
            if (error != null) {
                gestor.anotar("RECHAZO", error);
                enviarMensaje(new Mensaje(Mensaje.Tipo.RECHAZADO, "SERVIDOR", error));
                return;
            }
            enviarMensaje(new Mensaje(Mensaje.Tipo.ACEPTADO, "SERVIDOR", "Conectado como " + nombreUsuario));
            socket.setSoTimeout(0);
            gestor.activar(this);
            gestor.aviso(nombreUsuario + " se ha unido al chat.");
            while (!socket.isClosed()) {
                objeto = entrada.readObject();
                if (!(objeto instanceof Mensaje)) throw new IOException("Objeto no válido.");
                mensaje = (Mensaje) objeto;
                if (mensaje.getTipo() == Mensaje.Tipo.DESCONECTAR) break;
                if (mensaje.getTipo() != Mensaje.Tipo.PUBLICO && mensaje.getTipo() != Mensaje.Tipo.PRIVADO) {
                    throw new IOException("Petición no permitida en una sesión identificada.");
                }
                String texto = mensaje.getContenido();
                if (texto == null || texto.trim().isEmpty() || texto.length() > 2000) {
                    enviarMensaje(new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", "Mensaje no válido (1 a 2000 caracteres)."));
                } else if (mensaje.getTipo() == Mensaje.Tipo.PUBLICO) {
                    gestor.publico(nombreUsuario, texto);
                } else {
                    gestor.privado(this, mensaje.getDestinatario(), texto);
                }
            }
        } catch (EOFException e) {
            // El cliente ha cerrado su conexión.
        } catch (IOException | ClassNotFoundException e) {
            if (!socket.isClosed()) gestor.anotar("ERROR", "Comunicación con " + nombreUsuario + ": " + e.getMessage());
        } finally {
            cerrar();
            gestor.retirar(this);
        }
    }
    public synchronized void enviarMensaje(Mensaje mensaje) {
        try {
            salida.writeObject(mensaje);
            salida.flush();
            salida.reset(); // No conservar indefinidamente los objetos enviados.
        } catch (IOException e) {
            gestor.anotar("ERROR", "Envío a " + nombreUsuario + ": " + e.getMessage());
            cerrar();
        }
    }
    public void cerrar() {
        // Cerrar el socket también cierra sus streams y desbloquea readObject.
        try { socket.close(); }
        catch (IOException e) { System.err.println("Error al cerrar socket: " + e.getMessage()); }
    }
}
