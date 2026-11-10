# Chat multiusuario · PSP

## Estructura
Un proyecto llamado reto, una carpeta src y una única clase propia que extiende Thread: comun/Hilo.java.

| Carpeta | Archivos |
| --- | --- |
| src/java/cliente | App.java, PrimaryController.java, Conexion.java |
| src/java/servidor | Servidor.java, SesionCliente.java |
| src/java/comun | Mensaje.java, Hilo.java |
| src/java | module-info.java (descriptor del módulo, no es una clase) |
| src/resources/cliente | chat.fxml |
| raíz | pom.xml, README.MD, ESTRUCTURA.png |

Son siete clases en total. Mensaje.java existe una sola vez y lo utilizan cliente y servidor.
El pom.xml configura src/java y src/resources expresamente. No hay dos proyectos ni dos carpetas src.

## Requisitos y ejecución
JDK 11 y Maven. La primera compilación descarga JavaFX 13 y las dependencias de Maven.
Abre la carpeta reto (la que contiene pom.xml) como un único proyecto Maven.
Todos los comandos se ejecutan desde reto.

Compilar:

```shell
mvn clean package
```

Iniciar el servidor en una terminal:

```shell
java -cp target/classes servidor.Servidor
```

Iniciar el cliente en otra terminal:

```shell
mvn javafx:run
```

Repite el comando del cliente en varias terminales para abrir varias ventanas.
El servidor escucha en el puerto 5000; Ctrl+C lo cierra.
El log se crea en logs/chat.log, relativo a la carpeta desde la que se ejecuta el servidor, y se conserva al reiniciar.

## Uso
1. Arranca el servidor.
2. Abre un cliente, introduce IP, puerto y usuario y pulsa Conectar.
3. En el mismo equipo: IP 127.0.0.1 y puerto 5000. En otro equipo: IP local del servidor y el puerto permitido en su cortafuegos.
4. Para un público, escribe y pulsa Enviar. Lo reciben todos, incluido el emisor.
5. Para un privado, marca Privado, escribe el destinatario y envía. Lo reciben el destinatario y el emisor, los demás no. Un privado a ti mismo aparece una vez.
6. Pulsa Desconectar para salir. Puedes volver a conectar. Cerrar la ventana también cierra la conexión.

Los nombres tienen entre 1 y 30 caracteres, no se pueden repetir y SERVIDOR/SISTEMA están reservados.
Los mensajes tienen hasta 2000 caracteres. Se informa de destinatarios inexistentes, rechazos y errores de comunicación.
El estado pasa a Conectado cuando el servidor acepta el nombre, no antes.

## Qué hace cada clase
- App: carga el FXML y cierra la conexión al terminar la aplicación.
- PrimaryController: botones, campos, mensajes y estado de la pantalla.
- Conexion: socket, streams, conexión, envío y recepción del cliente.
- Servidor: accept(), listas de usuarios, públicos, privados, sincronización, límite, monitor y log.
- SesionCliente: guarda el socket y el nombre de una sesión del servidor y atiende sus peticiones. Es una clase normal, no extiende Thread.
- Mensaje: objeto Serializable con tipo, emisor, destinatario y contenido; declara serialVersionUID.
- Hilo: única clase que extiende Thread. Su run() ejecuta la tarea Runnable recibida en el constructor.

## Una clase de hilo, varias instancias
Solo Hilo.java hereda de Thread. No existen HiloCliente, HiloMonitor, HiloEnvio ni otras subclases de Thread, tampoco subclases anónimas de Thread.
Un Runnable describe una tarea con su método run(); no inicia un hilo por sí mismo.
Se utiliza new Hilo(nombre, new Runnable() { public void run() { ... } }) y después start().
No se utilizan lambdas ni referencias a métodos.

La misma clase Hilo se reutiliza para:
- Atender a cada cliente en el servidor (una instancia por conexión).
- Mostrar estadísticas cada 10 segundos.
- Conectar y recibir mensajes en el cliente.
- Enviar y cerrar la conexión fuera del hilo gráfico.
- Comunicar rechazos y cerrar el servidor.

Hay varias ejecuciones concurrentes porque el enunciado exige un hilo por cliente y un monitor. Todas utilizan la misma clase Hilo.
Platform.runLater(new Runnable() { ... }) cambia la pantalla en el hilo de JavaFX; no crea un nuevo hilo propio para cada actualización.

## Sincronización y recursos
- synchronized protege las listas, las estadísticas, el log y las escrituras a cada stream.
- Para difundir, se copia la lista bajo synchronized y se escribe a la red fuera del bloqueo global del servidor.
- Ambos extremos crean primero ObjectOutputStream y hacen flush antes de crear ObjectInputStream.
- Se usa readObject(), se comprueba el tipo y se convierte a Mensaje.
- El servidor usa el nombre de la sesión registrada como emisor, no el que declare cada mensaje.
- El cierre del socket cierra sus streams y desbloquea lecturas/escrituras pendientes.
- El servidor espera con join(2000) a los trabajadores al cerrar; nunca se hace join en un evento de la pantalla.

## Límite, registro y monitor
Máximo cinco conexiones, incluidas las que están identificándose. Al superar el límite se comunica un rechazo y se cierra la conexión.
La identificación tiene cinco segundos de plazo. Una conexión que no se identifica libera su plaza al vencer ese tiempo.
Se registran fecha y hora, entradas, salidas, públicos, privados y errores. El log global contiene también el contenido de los privados.
Cada diez segundos el monitor muestra usuarios conectados, conexiones ocupadas, tiempo activo y último mensaje de usuario.
Las entradas/salidas no sustituyen ese último mensaje.

## Prueba antes de la presentación
1. Abre tres clientes y envía un público: debe aparecer en los tres.
2. Envía un privado entre dos: no debe aparecer en el tercero.
3. Envíate un privado: debe aparecer una vez.
4. Prueba nombre duplicado y destinatario inexistente.
5. Conecta cinco usuarios y prueba un sexto: debe ser rechazado.
6. Desconecta uno: otro usuario debe poder entrar.
7. Espera diez segundos y comprueba estadísticas y logs/chat.log.
8. Cierra una ventana: los demás deben ver su salida.
9. Para el servidor: los clientes deben indicar que se ha cerrado o perdido la conexión.
10. Prueba un servidor que no responda: la ventana debe seguir respondiendo mientras intenta conectar.

## Verificación y alcance
Compilado directamente con JDK 21 usando sintaxis y bytecode objetivo Java 11, y JavaFX 13.
La compilación completa mediante Maven requiere comprobarse en vuestro equipo: la conectividad de Maven del entorno de revisión no permitió descargar sus dependencias.
Probado el protocolo del servidor con varios clientes y el FXML/controlador con dos interfaces JavaFX en modo sin pantalla.
Antes del vídeo, conviene repetir la prueba manual en vuestro JDK 11 y comprobar la presentación visual.
Un cliente que deja de leer puede retrasar una escritura dirigida a él; la escritura no mantiene el bloqueo global del servidor.
El vídeo y la defensa del equipo deben prepararse aparte.
