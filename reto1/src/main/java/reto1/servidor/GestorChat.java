package reto1.servidor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import reto1.modelo.Mensaje;

public class GestorChat implements AutoCloseable {

    private static final int MAX_CLIENTES = 5; // Límite máximo de usuarios
    private final List<HiloCliente> clientesConectados = new ArrayList<>();
    private final long tiempoInicio = System.currentTimeMillis();
    private final RegistroChat registro;
    private String ultimoMensaje = "Sin mensajes aún";

    public GestorChat() throws IOException {
        registro = new RegistroChat();
    }

    public synchronized boolean conectarCliente(HiloCliente cliente) {
        if (clientesConectados.size() >= MAX_CLIENTES) {
            cliente.enviarMensaje(new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", "Límite de conexiones alcanzado."));
            return false;
        }

        for (HiloCliente h : clientesConectados) {
            if (h.getNombreUsuario().equalsIgnoreCase(cliente.getNombreUsuario())) {
                cliente.enviarMensaje(new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", "El usuario ya está conectado."));
                return false;
            }
        }

        clientesConectados.add(cliente);
        System.out.println("[ENTRADA] Usuario conectado: " + cliente.getNombreUsuario());
        registro.registrar("ENTRADA", "Usuario conectado: " + cliente.getNombreUsuario());

        difundirPublico("SISTEMA", cliente.getNombreUsuario() + " se ha unido al chat.");
        return true;
    }

    public synchronized void desconectarCliente(HiloCliente cliente) {
        if (clientesConectados.remove(cliente)) {
            System.out.println("[SALIDA] Usuario desconectado: " + cliente.getNombreUsuario());
            registro.registrar("SALIDA", "Usuario desconectado: " + cliente.getNombreUsuario());
            difundirPublico("SISTEMA", cliente.getNombreUsuario() + " ha salido del chat.");
        }
    }

    public synchronized void difundirPublico(String emisor, String texto) {
        this.ultimoMensaje = "[" + emisor + " (PÚBLICO)]: " + texto;
        System.out.println("[LOG] " + emisor + ": " + texto);
        registro.registrar("PUBLICO", emisor + ": " + texto);

        Mensaje msg = new Mensaje(Mensaje.Tipo.PUBLICO, emisor, texto);
        for (HiloCliente h : clientesConectados) {
            h.enviarMensaje(msg);
        }
    }

    public synchronized void enviarPrivado(String emisor, String destinatario, String texto) {
        this.ultimoMensaje = "[" + emisor + " -> " + destinatario + " (PRIVADO)]: " + texto;
        System.out.println("[LOG] " + emisor + " a " + destinatario + ": " + texto);
        registro.registrar("PRIVADO", emisor + " -> " + destinatario + ": " + texto);

        Mensaje msg = new Mensaje(Mensaje.Tipo.PRIVADO, emisor, destinatario, texto);
        boolean encontrado = false;

        for (HiloCliente h : clientesConectados) {
            if (h.getNombreUsuario().equalsIgnoreCase(destinatario)) {
                h.enviarMensaje(msg);
                encontrado = true;
                break;
            }
        }

        for (HiloCliente h : clientesConectados) {
            if (h.getNombreUsuario().equalsIgnoreCase(emisor)) {
                if (encontrado) {
                    h.enviarMensaje(msg);
                } else {
                    h.enviarMensaje(new Mensaje(Mensaje.Tipo.SISTEMA, "SERVIDOR", "El usuario '" + destinatario + "' no existe."));
                }
                break;
            }
        }
    }

    public synchronized void mostrarEstadisticas() {
        long uptime = (System.currentTimeMillis() - tiempoInicio) / 1000;
        System.out.println("\n--- ESTADO DEL SERVIDOR ---");
        System.out.println("Usuarios conectados: " + clientesConectados.size() + " / " + MAX_CLIENTES);
        System.out.println("Tiempo activo       : " + uptime + " segundos");
        System.out.println("Último mensaje      : " + ultimoMensaje);
        System.out.println("---------------------------\n");
    }

    @Override
    public void close() throws IOException {
        registro.close();
    }
}