package reto1.servidor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

final class RegistroChat implements AutoCloseable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final BufferedWriter escritor;

    RegistroChat() throws IOException {
        Path ruta = Paths.get("logs", "chat.log");
        Files.createDirectories(ruta.getParent());
        escritor = Files.newBufferedWriter(
                ruta,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    synchronized void registrar(String tipo, String detalle) {
        try {
            escritor.write(OffsetDateTime.now().format(FORMATO_FECHA));
            escritor.write(" [");
            escritor.write(tipo);
            escritor.write("] ");
            escritor.write(detalle.replace("\r", "\\r").replace("\n", "\\n"));
            escritor.newLine();
            escritor.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir en logs/chat.log", e);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        escritor.close();
    }
}
