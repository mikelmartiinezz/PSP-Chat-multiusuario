package comun;

// Única clase de hilo de todo el proyecto.
// Cada objeto Hilo recibe la tarea que debe ejecutar.
public class Hilo extends Thread {
    private final Runnable tarea;

    public Hilo(String nombre, Runnable tarea) {
        super(nombre);
        this.tarea = tarea;
    }

    @Override
    public void run() {
        tarea.run();
    }
}
