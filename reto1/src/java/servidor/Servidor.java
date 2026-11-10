package servidor;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import comun.Mensaje;
import comun.Hilo;

public class Servidor implements AutoCloseable {
    private static final int MAX_CLIENTES = 5;
    // Incluye conexiones que todavía están enviando su nombre.
    private final ArrayList<SesionCliente> conexiones = new ArrayList<SesionCliente>();
    private final ArrayList<SesionCliente> usuarios = new ArrayList<SesionCliente>();
    private final ArrayList<SesionCliente> listos = new ArrayList<SesionCliente>();
    private final BufferedWriter escritor;
    private final long inicio = System.currentTimeMillis();
    private String ultimoMensaje = "Sin mensajes";
    private boolean cerrado;

    public Servidor() throws IOException {
        Files.createDirectories(Paths.get("logs"));
        escritor = Files.newBufferedWriter(Paths.get("logs/chat.log"), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public synchronized boolean reservar(SesionCliente cliente) {
        if (cerrado || conexiones.size() >= MAX_CLIENTES) return false;
        conexiones.add(cliente);
        return true;
    }
    public synchronized String registrarUsuario(SesionCliente cliente, String nombre) {
        if (cerrado) return "El servidor se está cerrando.";
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 30
                || nombre.indexOf('\n') >= 0 || nombre.indexOf('\r') >= 0) return "Nombre no válido (1 a 30 caracteres).";
        nombre = nombre.trim();
        if (nombre.equalsIgnoreCase("SERVIDOR") || nombre.equalsIgnoreCase("SISTEMA")) return "Nombre reservado.";
        for (SesionCliente otro : usuarios) {
            if (otro.getNombreUsuario().equalsIgnoreCase(nombre)) return "El usuario ya está conectado.";
        }
        cliente.setNombreUsuario(nombre);
        usuarios.add(cliente);
        anotar("ENTRADA", nombre);
        return null;
    }
    public synchronized void activar(SesionCliente cliente) {
        if (!cerrado && usuarios.contains(cliente)) listos.add(cliente);
    }
    private synchronized ArrayList<SesionCliente> copiarUsuarios() {
        return new ArrayList<SesionCliente>(listos);
    }
    public void aviso(String texto) {
        Mensaje mensaje = new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", texto);
        for (SesionCliente cliente : copiarUsuarios()) cliente.enviarMensaje(mensaje);
    }
    public void publico(String emisor, String texto) {
        synchronized (this) {
            ultimoMensaje = "[PÚBLICO] " + emisor + ": " + texto;
            anotar("PUBLICO", emisor + ": " + texto);
        }
        Mensaje mensaje = new Mensaje(Mensaje.Tipo.PUBLICO, emisor, texto);
        // Las escrituras de red no mantienen bloqueado el gestor.
        for (SesionCliente cliente : copiarUsuarios()) cliente.enviarMensaje(mensaje);
    }
    public void privado(SesionCliente emisor, String nombre, String texto) {
        SesionCliente destino = null;
        for (SesionCliente cliente : copiarUsuarios()) {
            if (nombre != null && cliente.getNombreUsuario().equalsIgnoreCase(nombre.trim())) {
                destino = cliente;
                break;
            }
        }
        if (destino == null) {
            emisor.enviarMensaje(new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", "El destinatario no está conectado."));
            anotar("ERROR", "Privado de " + emisor.getNombreUsuario() + ": destinatario inexistente");
            return;
        }
        synchronized (this) {
            ultimoMensaje = "[PRIVADO] " + emisor.getNombreUsuario() + " a " + destino.getNombreUsuario() + ": " + texto;
            anotar("PRIVADO", ultimoMensaje);
        }
        Mensaje mensaje = new Mensaje(Mensaje.Tipo.PRIVADO, emisor.getNombreUsuario(), destino.getNombreUsuario(), texto);
        destino.enviarMensaje(mensaje);
        if (destino != emisor) emisor.enviarMensaje(mensaje);
    }
    public void retirar(SesionCliente cliente) {
        boolean estaba;
        synchronized (this) {
            conexiones.remove(cliente);
            estaba = usuarios.remove(cliente);
            listos.remove(cliente);
            if (estaba) anotar("SALIDA", cliente.getNombreUsuario());
        }
        if (estaba) aviso(cliente.getNombreUsuario() + " ha salido del chat.");
    }
    public synchronized void anotar(String tipo, String texto) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        String fecha = LocalDateTime.now().format(formato);

        String linea = fecha + " [" + tipo + "] " + texto.replace("\n", " ").replace("\r", " ");
        
        try {
            escritor.write(linea);
            escritor.newLine();
            escritor.flush();
        } catch (IOException e) {
            System.err.println("Error al guardar el log: " + e.getMessage());
        }
    }
    public synchronized void mostrarEstadisticas() {
        System.out.println("");
        System.out.println("--- ESTADO DEL SERVIDOR ---");
        System.out.println("Usuarios conectados: " + usuarios.size() + " / " + MAX_CLIENTES);
        System.out.println("Conexiones ocupadas (incluye pendientes): " + conexiones.size());
        System.out.println("Tiempo activo: " + (System.currentTimeMillis() - inicio) / 1000 + " segundos");
        System.out.println("Último mensaje: " + ultimoMensaje);
    }
    public void close() throws IOException {
        ArrayList<SesionCliente> copia;
        synchronized (this) {
            if (cerrado) return;
            cerrado = true;
            copia = new ArrayList<SesionCliente>(conexiones);
        }
        for (SesionCliente cliente : copia) cliente.cerrar();
        // Esperar a los finally de los trabajadores antes de cerrar el log.
        for (SesionCliente cliente : copia) {
            cliente.esperarFin();
        }
        escritor.close();
    }

    private void monitorizar() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10000);
                mostrarEstadisticas();
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void rechazar(final Socket socket) {
        anotar("RECHAZO", "Límite de conexiones alcanzado: " + socket.getRemoteSocketAddress());
        Hilo hilo = new Hilo("servidor-rechazo", new Runnable() {
            public void run() {
                try {
                    socket.setSoTimeout(2000);
                    ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
                    salida.flush();
                    ObjectInputStream entrada = new ObjectInputStream(socket.getInputStream());
                    entrada.readObject();
                    salida.writeObject(new Mensaje(Mensaje.Tipo.RECHAZADO, "SERVIDOR", "Límite de conexiones alcanzado."));
                    salida.flush();
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("No se pudo comunicar el rechazo: " + e.getMessage());
                } finally {
                    try { socket.close(); }
                    catch (IOException e) { System.err.println(e.getMessage()); }
                }
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    public static void main(String[] args) {
        try {
            final Servidor chat = new Servidor();
            final ServerSocket escucha;
            try { escucha = new ServerSocket(5000); }
            catch (IOException e) { chat.close(); throw e; }
            final Hilo monitor = new Hilo("servidor-monitor", new Runnable() {
                public void run() { chat.monitorizar(); }
            });
            // También el cierre utiliza la misma clase Hilo.
            Runtime.getRuntime().addShutdownHook(new Hilo("servidor-cierre", new Runnable() {
                public void run() {
                    monitor.interrupt();
                    try { escucha.close(); chat.close(); }
                    catch (IOException e) { System.err.println(e.getMessage()); }
                }
            }));
            monitor.setDaemon(true);
            monitor.start();
            System.out.println("Servidor escuchando en el puerto 5000. Ctrl+C para terminar.");
            try {
                while (!escucha.isClosed()) {
                    Socket socket = escucha.accept();
                    SesionCliente cliente = new SesionCliente(socket, chat);
                    if (chat.reservar(cliente)) cliente.iniciar();
                    else chat.rechazar(socket);
                }
            } finally {
                monitor.interrupt();
                escucha.close();
                chat.close();
            }
        } catch (IOException e) { System.err.println("Servidor: " + e.getMessage()); }
    }
}
