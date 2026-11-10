package comun;

import java.io.Serializable;

public class Mensaje implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Tipo {
        CONECTAR,
        ACEPTADO,
        RECHAZADO,
        DESCONECTAR,
        PUBLICO,
        PRIVADO,
        SISTEMA
    }

    private final Tipo tipo;
    private final String emisor;
    private final String destinatario;
    private final String contenido;

    public Mensaje(Tipo tipo, String emisor, String contenido) {
        this(tipo, emisor, null, contenido);
    }

    public Mensaje(Tipo tipo, String emisor, String destinatario, String contenido) {
        this.tipo = tipo;
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.contenido = contenido;
    }

    public Tipo getTipo() { return tipo; }
    public String getEmisor() { return emisor; }
    public String getDestinatario() { return destinatario; }
    public String getContenido() { return contenido; }
}
