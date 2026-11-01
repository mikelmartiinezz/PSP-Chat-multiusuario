package reto1.servidor;

import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import reto1.modelo.Mensaje;

public class HiloCliente extends Thread {

    private final Socket socketCliente;
    private final GestorChat gestor;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String nombreUsuario;

    public HiloCliente(Socket socketCliente, GestorChat gestor) {
        this.socketCliente = socketCliente;
        this.gestor = gestor;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    @Override
    public void run() {
        try {
            salida = new ObjectOutputStream(socketCliente.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socketCliente.getInputStream());

            while (true) {
                Mensaje msg = (Mensaje) entrada.readObject();

                if (msg.getTipo() == Mensaje.Tipo.CONECTAR) {
                    this.nombreUsuario = msg.getEmisor();
                    boolean exito = gestor.conectarCliente(this);
                    if (!exito) break;
                } else if (msg.getTipo() == Mensaje.Tipo.DESCONECTAR) {
                    break;
                } else if (msg.getTipo() == Mensaje.Tipo.PUBLICO) {
                    gestor.difundirPublico(this.nombreUsuario, msg.getContenido());
                } else if (msg.getTipo() == Mensaje.Tipo.PRIVADO) {
                    gestor.enviarPrivado(this.nombreUsuario, msg.getDestinatario(), msg.getContenido());
                }
            }
        } catch (EOFException e) {
            // El cierre de la conexión es una salida normal del bucle de lectura.
        } catch (Exception e) {
            System.err.println("Error de comunicación con " + nombreUsuario + ": " + e.getMessage());
        } finally {
            try {
                gestor.desconectarCliente(this);
            } finally {
                try {
                    if (socketCliente != null && !socketCliente.isClosed()) {
                        socketCliente.close();
                    }
                } catch (Exception e) {
                    System.err.println("Error al cerrar la conexión: " + e.getMessage());
                }
            }
        }
    }

    public void enviarMensaje(Mensaje msg) {
        try {
            salida.writeObject(msg);
            salida.flush();
        } catch (Exception e) {
            System.out.println("Error al enviar mensaje a " + nombreUsuario + ": " + e.getMessage());
        }
    }
}