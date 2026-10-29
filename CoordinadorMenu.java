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
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Programa principal que coordina los procesos de la fábrica.
 * Permite seleccionar los módulos que se quieren ejecutar.
 *
 * @author Equipo 3 DAM2
 * @version 1.1
 */

public class CoordinadorMenu {
    /** Identificadores de los módulos industriales. */
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
    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    /**
     * Método principal. Muestra el menú y ejecuta la opción seleccionada.
     * @param args argumentos de ejecución
     */
    public static void main(String[] args) {
        try (Scanner teclado = new Scanner(System.in)) {
            int opcion;
            do {
                mostrarMenu();
                System.out.print("Selecciona una opción: ");
                while (!teclado.hasNextInt()) {
                    System.out.print("Introduce un número válido: ");
                    teclado.next();
                }
                opcion = teclado.nextInt();
                if (opcion >= 1 && opcion <= MODULOS.length) {
                    // Ejecutamos solamente el módulo seleccionado.
                    ejecutarModulos(List.of(MODULOS[opcion - 1]));
                } else if (opcion == 8) {
                    // Ejecutamos todos los módulos.
                    ejecutarModulos(List.of(MODULOS));
                } else if (opcion == 0) {System.out.println("Cerrando el coordinador...");
                } else {System.out.println("Opción no válida.");}
            } while (opcion != 0);
        }
    }
    /**
     * Muestra las opciones disponibles.
     */
    private static void mostrarMenu() {
        System.out.println();
        System.out.println("======================================");
        System.out.println("   SISTEMA DE CONTROL DE PRODUCCION");
        System.out.println("======================================");
        System.out.println("1. Lanzar módulo de ensamblaje");
        System.out.println("2. Lanzar módulo de pintura");
        System.out.println("3. Lanzar módulo de calidad");
        System.out.println("4. Lanzar módulo de empaquetado");
        System.out.println("5. Lanzar módulo de etiquetado");
        System.out.println("6. Lanzar módulo de logística");
        System.out.println("7. Lanzar módulo de supervisión");
        System.out.println("8. Lanzar todos los módulos");
        System.out.println("0. Salir");
        System.out.println("======================================");
    }
    /**
     * Inicia y supervisa los módulos seleccionados.
     *
     * @param modulos lista de módulos que se van a ejecutar
     */
    private static void ejecutarModulos(List<String> modulos) {
        List<Process> procesos = new ArrayList<>();
        List<BufferedReader> lectores = new ArrayList<>();
        boolean paradaEmergencia = false;
        try {
            // Eliminamos una posible señal de parada anterior.
            Files.deleteIfExists(ARCHIVO_PARADA);
            mostrar("COORDINADOR", "INFO","Procesos que se van a iniciar: " + modulos.size());
            // Iniciamos únicamente los módulos seleccionados.
            for (String modulo : modulos) {
                try {
                    String clase = obtenerClase(modulo);
                    ProcessBuilder constructor = new ProcessBuilder("java","-cp",System.getProperty("java.class.path"),clase);
                    // Unimos la salida de error con la salida normal.
                    constructor.redirectErrorStream(true);
                    Process proceso = constructor.start();
                    procesos.add(proceso);
                    InputStream entrada = proceso.getInputStream();
                    BufferedReader lector = new BufferedReader(new InputStreamReader(entrada));
                    lectores.add(lector);
                    mostrar(modulo, "INFO",
                            "Proceso iniciado correctamente.");
                } catch (IOException | IllegalArgumentException e) {
                    mostrar(modulo, "ERROR","No se pudo iniciar el proceso: " + e.getMessage());
                    // Mantenemos las listas alineadas.
                    procesos.add(null);
                    lectores.add(null);
                }
            }
            mostrar("COORDINADOR", "INFO",
                    "Comienza la supervision de la fabrica.");
            boolean quedanProcesos = true;
            // Supervisamos hasta que terminen todos los procesos.
            while (quedanProcesos) {
                quedanProcesos = false;
                // Comprobamos si se ha solicitado una emergencia.
                if (Files.exists(ARCHIVO_PARADA)) {
                    paradaEmergencia = true;
                    mostrar("COORDINADOR", "ALERTA",
                            "Parada de emergencia solicitada.");
                    break;
                }

                // Revisamos los procesos sin bloquear la lectura
                // esperando a que uno concreto genere un mensaje.
                for (int i = 0; i < procesos.size(); i++) {
                    Process proceso = procesos.get(i);
                    BufferedReader lector = lectores.get(i);
                    String modulo = modulos.get(i);
                    if (proceso == null) {continue;}
                    if (proceso.isAlive()) {quedanProcesos = true;}
                    // Leemos los mensajes disponibles.
                    if (lector != null) {
                        while (lector.ready()) {
                            String mensaje = lector.readLine();
                            if (mensaje == null) {break;}
                            mostrarMensajeModulo(modulo, mensaje);
                        }
                    }
                    // Comprobamos si el proceso ha terminado.
                    if (!proceso.isAlive()) {
                        int codigoSalida = proceso.exitValue();
                        if (codigoSalida != 0) {
                            mostrar(modulo, "ERROR","Proceso terminado con codigo " + codigoSalida + ".");
                        }
                        procesos.set(i, null);
                        if (lector != null) {
                            try {
                                lector.close();
                            } catch (IOException e) {
                                mostrar(modulo, "ALERTA",
                                        "No se pudo cerrar el lector.");
                            }
                            lectores.set(i, null);
                        }
                    }
                }
                // Pequeña pausa para evitar consumir CPU continuamente.
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
            mostrar("COORDINADOR", "ERROR","Error al gestionar los procesos: " + e.getMessage());
        } finally {
            // Detenemos los procesos si se ha solicitado una emergencia.
            if (paradaEmergencia) {detenerProcesos(procesos);}
            // Cerramos los lectores que sigan abiertos.
            for (BufferedReader lector : lectores) {
                if (lector != null) {
                    try {lector.close();
                    } catch (IOException e) {
                        mostrar("COORDINADOR", "ALERTA",
                                "Error al cerrar un lector.");
                    }
                }
            }
            // Limpiamos los procesos que todavía sigan activos.
            detenerProcesos(procesos);
            try {
                Files.deleteIfExists(ARCHIVO_PARADA);
            } catch (IOException e) {
                mostrar("COORDINADOR", "ALERTA",
                        "No se pudo eliminar el archivo de parada.");
            }
            mostrar("COORDINADOR", "INFO",
                    "Supervision finalizada.");
        }
    }
    /**
     * Devuelve el nombre de la clase Java correspondiente al módulo.
     *
     * @param modulo identificador del módulo
     * @return nombre de la clase que se debe ejecutar
     */
    private static String obtenerClase(String modulo) {
        return switch (modulo) {
            case "ENSAMBLAJE" -> "Ensamblaje";
            case "PINTURA" -> "Pintura";
            case "CALIDAD" -> "Calidad";
            case "EMPAQUETADO" -> "Empaquetado";
            case "ETIQUETADO" -> "Etiquetado";
            case "LOGISTICA" -> "Logistica";
            case "SUPERVISION" -> "Supervision";
            default -> throw new IllegalArgumentException(
                    "Módulo desconocido: " + modulo
            );
        };
    }
    /**
     * Muestra un mensaje con el formato establecido.
     *
     * @param origen identificador del emisor
     * @param estado categoría del mensaje
     * @param mensaje contenido que se va a mostrar
     */
    private static void mostrar(
            String origen, String estado, String mensaje) {
        String hora = LocalTime.now().format(FORMATO_HORA);
        System.out.println("[" + hora + "] " + "[" + origen + "] " + "[" + estado + "] " + mensaje);
    }

    /**
     * Interpreta y presenta el estado recibido de un módulo.
     *
     * @param origen identificador del módulo
     * @param mensaje línea recibida por su salida estándar
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
     *
     * @param procesos lista de procesos que se van a detener
     */
    private static void detenerProcesos(List<Process> procesos) {

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
