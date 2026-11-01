# Chat multiusuario

Aplicación Java cliente-servidor con el servidor en consola y el cliente gráfico en JavaFX.

## Estructura

- `src/main/java/reto1/servidor/`: servidor, gestión de conexiones, hilos de cliente y registro persistente.
- `src/main/java/reto1/cliente/`: aplicación JavaFX, controlador e hilo receptor.
- `src/main/java/reto1/modelo/`: mensajes compartidos por cliente y servidor.
- `src/main/resources/reto1/cliente/`: interfaz JavaFX (`chat.fxml`).
- `logs/chat.log`: registro del servidor, creado automáticamente al iniciarlo.

## Requisitos

- JDK 11.
- Maven.

## Compilar

Desde esta carpeta (`reto1`):

```shell
mvn clean package
```

## Ejecutar el servidor

Desde esta carpeta, en una terminal:

```shell
java -cp target/classes reto1.servidor.ServidorApp
```

El servidor escucha en el puerto `5000`. Al arrancar, crea `logs/chat.log` si no existe y añade los eventos al final del archivo. No se borra al reiniciar el servidor. Cada línea incluye fecha, hora, zona horaria y tipo de evento: entradas, salidas, mensajes públicos y mensajes privados.

El registro incluye también el contenido de los mensajes privados, tal como pide el requisito de log global; protege el archivo frente a accesos no autorizados.

## Ejecutar el cliente

Desde esta carpeta, en otra terminal:

```shell
mvn javafx:run
```

## Uso

1. Inicia primero el servidor.
2. En el cliente, configura la IP (por defecto `127.0.0.1`), el puerto (`5000`) y el nombre de usuario; pulsa **Conectar**.
3. Escribe un mensaje y pulsa **Enviar** para enviarlo a todos.
4. Para un mensaje privado, activa **Privado**, escribe el destinatario y envía el mensaje.
5. Pulsa **Desconectar** para salir. El límite es de cinco usuarios conectados simultáneamente.
