package cliente;
import java.io.*;
import java.net.*;
import javafx.application.Platform;
import comun.Mensaje;
import comun.Hilo;

// Conecta y después recibe. Todo el trabajo de red se hace fuera de JavaFX.
public class Conexion {
    private final PrimaryController controlador;
    private final String ip;
    private final int puerto;
    private final String usuario;
    private Socket socket;
    private ObjectOutputStream salida;
    private boolean cerrado;

    public Conexion(PrimaryController controlador, String ip, int puerto, String usuario) {
        this.controlador = controlador;
        this.ip = ip;
        this.puerto = puerto;
        this.usuario = usuario;
    }
    public void iniciar() {
        Hilo hilo = new Hilo("cliente-recepcion", new Runnable() {
            public void run() { conectarYRecibir(); }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    public void enviarEnSegundoPlano(final Mensaje mensaje) {
        Hilo hilo = new Hilo("cliente-envio", new Runnable() {
            public void run() {
                String resultado = null;
                try { enviar(mensaje); }
                catch (IOException e) {
                    resultado = "No se pudo enviar: " + e.getMessage();
                    cerrar();
                }
                final String error = resultado;
                Platform.runLater(new Runnable() {
                    public void run() { controlador.envioTerminado(Conexion.this, error); }
                });
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    public void desconectar() {
        Hilo hilo = new Hilo("cliente-cierre", new Runnable() {
            public void run() { cerrar(); }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void conectarYRecibir() {
        String motivo = "Conexión cerrada por el servidor.";
        try {
            Socket nuevoSocket = new Socket();
            synchronized (this) {
                if (cerrado) { nuevoSocket.close(); return; }
                socket = nuevoSocket;
            }
            nuevoSocket.connect(new InetSocketAddress(ip, puerto), 5000);
            nuevoSocket.setSoTimeout(7000);
            ObjectOutputStream nuevaSalida = new ObjectOutputStream(nuevoSocket.getOutputStream());
            nuevaSalida.flush();
            synchronized (this) { salida = nuevaSalida; }
            ObjectInputStream entrada = new ObjectInputStream(nuevoSocket.getInputStream());
            enviar(new Mensaje(Mensaje.Tipo.CONECTAR, usuario, null));
            Object objeto = entrada.readObject();
            if (!(objeto instanceof Mensaje)) throw new IOException("Respuesta no válida.");
            Mensaje respuesta = (Mensaje) objeto;
            if (respuesta.getTipo() == Mensaje.Tipo.RECHAZADO) {
                motivo = respuesta.getContenido();
                return;
            }
            if (respuesta.getTipo() != Mensaje.Tipo.ACEPTADO) throw new IOException("El servidor no ha aceptado la sesión.");
            nuevoSocket.setSoTimeout(0);
            Platform.runLater(new Runnable() {
                public void run() { controlador.conexionAceptada(Conexion.this); }
            });
            while (!estaCerrado()) {
                objeto = entrada.readObject();
                if (!(objeto instanceof Mensaje)) throw new IOException("Mensaje no válido.");
                final Mensaje recibido = (Mensaje) objeto;
                Platform.runLater(new Runnable() {
                    public void run() { controlador.recibir(Conexion.this, recibido); }
                });
            }
        } catch (IOException | ClassNotFoundException e) {
            motivo = "Error de conexión: " + e.getMessage();
        } finally {
            cerrar();
            final String aviso = motivo;
            Platform.runLater(new Runnable() {
                public void run() { controlador.conexionTerminada(Conexion.this, aviso); }
            });
        }
    }
    public void enviar(Mensaje mensaje) throws IOException {
        ObjectOutputStream flujo;
        synchronized (this) {
            if (cerrado || salida == null) throw new IOException("No hay conexión.");
            flujo = salida;
        }
        // Solo un escritor usa el stream cada vez; cerrar() no espera este bloqueo.
        synchronized (flujo) {
            flujo.writeObject(mensaje);
            flujo.flush();
            flujo.reset();
        }
    }
    public synchronized boolean estaCerrado() { return cerrado; }
    public void cerrar() {
        Socket copia;
        synchronized (this) { cerrado = true; copia = socket; }
        if (copia != null) {
            try { copia.close(); }
            catch (IOException e) { System.err.println("Cierre: " + e.getMessage()); }
        }
    }
}
