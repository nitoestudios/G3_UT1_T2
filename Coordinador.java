import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
Programa principal que coordina los siete procesos de la fábrica.
Lanza los componentes con ProcessBuilder, lee sus salidas.
 @author Equipo 3 DAM2
 @version 1.0
 */
public class Coordinador {
    /** Identificadores de los siete módulos industriales. */
    private static final String[] MODULOS = {
        "ENSAMBLAJE",
        "PINTURA",
        "CALIDAD",
        "EMPAQUETADO",
        "ETIQUETADO",
        "LOGISTICA",
        "SUPERVISION"
    };
    /** Archivo cuya existencia activa la parada de emergencia. */
    private static final Path ARCHIVO_PARADA = Path.of("PARADA.txt");
    /** Formato de la hora mostrada en consola. */
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Método principal de la aplicación.
    @param args argumentos de ejecución*/

    public static void main(String[] args) {
        List<Process> procesos = new ArrayList<>();
        List<BufferedReader> lectores = new ArrayList<>();
        boolean paradaEmergencia = false;
        try {
            // Eliminamos una posible señal de parada anterior.
            Files.deleteIfExists(ARCHIVO_PARADA);
            System.out.println("======================================");
            System.out.println(" SISTEMA DE CONTROL DE PRODUCCION");
            System.out.println("======================================");
            System.out.println("Procesos que se van a iniciar: "+ MODULOS.length);
            System.out.println();
            // Iniciamos los siete procesos independientes.
            for (String modulo : MODULOS) {
                try {
                    ProcessBuilder constructor = new ProcessBuilder( "java","-cp",System.getProperty("java.class.path"),"Componente",modulo);
                    // Unimos la salida de error con la salida normal.
                    constructor.redirectErrorStream(true);
                    Process proceso = constructor.start();
                    procesos.add(proceso);
                    InputStream entrada = proceso.getInputStream();
                    BufferedReader lector = new BufferedReader(new InputStreamReader(entrada));
                    lectores.add(lector);
                    mostrar(modulo, "INFO","Proceso iniciado correctamente.");
                } catch (IOException e) {
                    mostrar(modulo, "ERROR","No se pudo iniciar el proceso: " + e.getMessage());
                    // Mantenemos las listas alineadas.
                    procesos.add(null);
                    lectores.add(null);
                }
            }
            mostrar("COORDINADOR", "INFO","Comienza la supervision de la fabrica.");
            // Supervisamos hasta que terminen todos los procesos.
            boolean quedanProcesos = true;
            while (quedanProcesos) {
                quedanProcesos = false;
                // Comprobamos si se ha solicitado una emergencia.
                if (Files.exists(ARCHIVO_PARADA)) {
                    paradaEmergencia = true;
                    mostrar("COORDINADOR", "ALERTA","Parada de emergencia solicitada.");
                    break;
                }
                // Revisamos los siete procesos sin esperar
                // bloqueados a que uno concreto genere un mensaje.
                for (int i = 0; i < procesos.size(); i++) {
                    Process proceso = procesos.get(i);
                    BufferedReader lector = lectores.get(i);
                    if (proceso == null) {continue;}
                    // Si el proceso sigue activo, queda trabajo.
                    if (proceso.isAlive()) {quedanProcesos = true;}
                    // Leemos solo cuando hay datos disponibles.
                    if (lector != null) {
                        while (lector.ready()) {
                            String mensaje = lector.readLine();
                            if (mensaje == null) {break;}
                            // El módulo puede indicar su estado
                            // con el formato: [ESTADO] mensaje.
                            mostrarMensajeModulo(MODULOS[i], mensaje);
                        }
                    }
                    // Si el proceso terminó, comprobamos su resultado.
                    if (!proceso.isAlive()) {
                        int codigoSalida = proceso.exitValue();
                        if (codigoSalida != 0) {mostrar(MODULOS[i], "ERROR","Proceso terminado con codigo " + codigoSalida + ".");}
                        // Evitamos seguir comprobando un proceso
                        // que ya ha finalizado.
                        procesos.set(i, null);
                        if (lector != null) {
                            try {
                                lector.close();
                            } catch (IOException e) {mostrar(MODULOS[i], "ALERTA","No se pudo cerrar el lector.");
                            }
                            lectores.set(i, null);
                        }
                    }
                }
                // Pequeña pausa para evitar consumir CPU
                // continuamente mientras se supervisan los procesos.
                if (quedanProcesos) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        paradaEmergencia = true;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            mostrar("COORDINADOR", "ERROR","Error al comprobar la parada: " + e.getMessage());
        } finally {
            if (paradaEmergencia) {detenerProcesos(procesos);}
            // Cerramos los lectores que sigan abiertos.
            for (BufferedReader lector : lectores) {
                if (lector != null) {
                    try {
                        lector.close();
                    } catch (IOException e) {mostrar("COORDINADOR", "ALERTA","Error al cerrar un lector.");}
                }
            }
            // Intentamos cerrar cualquier proceso que siga activo.
            detenerProcesos(procesos);
            try {
                Files.deleteIfExists(ARCHIVO_PARADA);
            } catch (IOException e) {mostrar("COORDINADOR", "ALERTA","No se pudo eliminar el archivo de parada.");
            }
            mostrar("COORDINADOR", "INFO","Supervision finalizada.");
        }
    }

    /** Muestra un mensaje con el formato establecido.
    @param origen identificador del emisor
    @param estado categoría del mensaje
    @param mensaje contenido que se va a mostrar*/

    private static void mostrar(String origen, String estado, String mensaje) {
        String hora = LocalTime.now().format(FORMATO_HORA);
        System.out.println("[" + hora + "] " + "[" + origen + "] " + "[" + estado + "] " + mensaje);
    }

    /**
    Interpreta y presenta el estado recibido de un componente.
    Si el mensaje no contiene una categoría reconocida,
    se muestra como información.
    @param origen identificador del componente
    @param mensaje línea recibida por su salida estándar
     */

    private static void mostrarMensajeModulo(
            String origen, String mensaje) {
        if (mensaje.startsWith("[") && mensaje.indexOf(']') > 1) {
            int finEstado = mensaje.indexOf(']');
            String estado = mensaje.substring(1, finEstado);
            String contenido = mensaje.substring(finEstado + 1).trim();
            if (estado.equals("OK")
                    || estado.equals("INFO")
                    || estado.equals("ALERTA")
                    || estado.equals("ERROR")) {
                mostrar(origen, estado, contenido);
                return;
            }
        }
        mostrar(origen, "INFO", mensaje);
    }

    /**
     * Detiene los procesos que continúen activos.
     * Primero solicita una terminación normal y después
     * fuerza el cierre si el proceso no responde.
     * @param procesos lista de procesos que se van a detener
     */
    private static void detenerProcesos(
            List<Process> procesos) {
        for (Process proceso : procesos) {
            if (proceso == null || !proceso.isAlive()) {continue;}
            proceso.destroy();
        }
        for (Process proceso : procesos) {
            if (proceso == null || !proceso.isAlive()) {continue;}
            try {
                if (!proceso.waitFor(2, TimeUnit.SECONDS)) {
                    proceso.destroyForcibly();
                    proceso.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                proceso.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }
}
