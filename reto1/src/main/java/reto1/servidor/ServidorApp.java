package reto1.servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorApp {

    public static void main(String[] args) {
        int puerto = 5000;
        try (GestorChat gestor = new GestorChat();
             ServerSocket servidor = new ServerSocket(puerto)) {
            Thread hiloStats = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(10000);
                        gestor.mostrarEstadisticas();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            hiloStats.setDaemon(true);
            hiloStats.start();

            System.out.println("Servidor de Chat iniciado en el puerto " + puerto);

            while (true) {
                Socket socketCliente = servidor.accept();
                System.out.println("Nuevo cliente intentando conectar...");

                HiloCliente hilo = new HiloCliente(socketCliente, gestor);
                hilo.start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }
}