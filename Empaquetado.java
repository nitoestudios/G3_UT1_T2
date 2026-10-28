import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Modulo de empaquetado de la fabrica.
 * No usa hilos: la concurrencia se consigue unicamente con procesos
 * creados con ProcessBuilder.
 * La clase tiene dos modos de ejecucion:
 * - Sin argumentos: actua como supervisor. Lanza una linea de
 *   empaquetado por proceso, lee sus salidas y las reenvia al
 *   Coordinador con el formato [ESTADO] mensaje.
 * - Con el argumento LINEA y un numero: actua como linea de
 *   empaquetado, empaqueta sus palets y termina con un codigo de salida.
 * @author Equipo 3 DAM2
 * @version 1.0
 */
public class Empaquetado {
    /** Nombre del modulo, igual al que usa el Coordinador. */
    private static final String NOMBRE = "EMPAQUETADO";
    /** Archivo cuya existencia activa la parada de emergencia. */
    private static final Path ARCHIVO_PARADA = Path.of("PARADA.txt");
    /** Numero de lineas de empaquetado que se lanzan. */
    private static final int NUM_LINEAS = 3;
    /** Numero de palets que prepara cada linea. */
    private static final int PALETS_POR_LINEA = 3;
    /** Cajas que caben en cada palet. */
    private static final int CAJAS_POR_PALET = 24;
    /** Generador de numeros aleatorios para la simulacion. */
    private static final Random ALEATORIO = new Random();

    /** Metodo principal del modulo.
     * @param args vacio para el supervisor, o LINEA y un numero para una linea
     */
    public static void main(String[] args) {
        if (args.length == 2 && args[0].equals("LINEA")) {
            int numero = Integer.parseInt(args[1]);
            System.exit(ejecutarLinea(numero));
        } else {
            System.exit(supervisar());
        }
    }

    // ------------------------------------------------------------
    // MODO SUPERVISOR
    // ------------------------------------------------------------

    /**
     * Lanza las lineas como procesos independientes, lee lo que
     * escriben y reenvia sus mensajes al Coordinador.
     * @return codigo de salida del modulo (0 correcto, 1 error)
     */
    private static int supervisar() {
        List<Process> procesos = new ArrayList<>();
        List<BufferedReader> lectores = new ArrayList<>();
        boolean emergencia = false;
        int lineasConError = 0;
        int codigoSalida = 0;

        emitir("INFO", "Modulo de " + NOMBRE + " iniciado.");
        try {
            // Lanzamos todas las lineas a la vez, cada una en su proceso.
            for (int i = 1; i <= NUM_LINEAS; i++) {
                try {
                    ProcessBuilder constructor = new ProcessBuilder(
                            "java", "-cp", System.getProperty("java.class.path"),
                            "Empaquetado", "LINEA", String.valueOf(i));
                    // Unimos la salida de error con la salida normal.
                    constructor.redirectErrorStream(true);
                    Process proceso = constructor.start();
                    procesos.add(proceso);
                    lectores.add(new BufferedReader(
                            new InputStreamReader(proceso.getInputStream())));
                    emitir("INFO", "Linea " + i + " iniciada.");
                } catch (IOException e) {
                    emitir("ERROR", "No se pudo iniciar la linea " + i + ": " + e.getMessage());
                    procesos.add(null);
                    lectores.add(null);
                    lineasConError++;
                }
            }

            // Leemos la salida de cada linea hasta que termine.
            for (int i = 0; i < procesos.size() && !emergencia; i++) {
                Process proceso = procesos.get(i);
                BufferedReader lector = lectores.get(i);
                if (proceso == null || lector == null) {
                    continue;
                }
                String linea;
                while ((linea = lector.readLine()) != null) {
                    // Antes de reenviar cada mensaje comprobamos la emergencia.
                    if (Files.exists(ARCHIVO_PARADA)) {
                        emergencia = true;
                        emitir("ALERTA", "Parada de emergencia detectada. Se detienen las lineas.");
                        break;
                    }
                    reenviar(i + 1, linea);
                }
                if (!emergencia) {
                    int codigo = proceso.waitFor();
                    if (codigo != 0) {
                        emitir("ERROR", "Linea " + (i + 1) + " terminada con codigo " + codigo + ".");
                        lineasConError++;
                    } else {
                        emitir("OK", "Linea " + (i + 1) + " finalizada correctamente.");
                    }
                }
            }

            if (emergencia) {
                emitir("ALERTA", "Empaquetado interrumpido antes de terminar.");
            } else if (lineasConError == 0) {
                emitir("OK", "Empaquetado completado en las " + NUM_LINEAS + " lineas.");
            } else {
                emitir("ALERTA", "Empaquetado terminado con " + lineasConError
                        + " linea(s) con error.");
                codigoSalida = 1;
            }
        } catch (IOException e) {
            emitir("ERROR", "Error de lectura en el empaquetado: " + e.getMessage());
            codigoSalida = 1;
        } catch (InterruptedException e) {
            emitir("ALERTA", "Supervision interrumpida.");
            emergencia = true;
            codigoSalida = 1;
        } finally {
            // Cerramos lectores y procesos que sigan activos.
            for (BufferedReader lector : lectores) {
                if (lector != null) {
                    try {
                        lector.close();
                    } catch (IOException e) {
                        emitir("ALERTA", "No se pudo cerrar un lector.");
                    }
                }
            }
            for (Process proceso : procesos) {
                if (proceso != null && proceso.isAlive()) {
                    proceso.destroyForcibly();
                }
            }
            emitir("INFO", "Modulo de " + NOMBRE + " finalizado.");
        }
        return codigoSalida;
    }

    /**
     * Reenvia al Coordinador un mensaje recibido de una linea,
     * anadiendo el numero de linea. Si el mensaje trae un estado
     * reconocido entre corchetes se respeta; si no, se envia como INFO.
     * @param numero numero de la linea que lo emitio
     * @param mensaje linea recibida por su salida estandar
     */
    private static void reenviar(int numero, String mensaje) {
        if (mensaje.startsWith("[") && mensaje.indexOf(']') > 1) {
            int fin = mensaje.indexOf(']');
            String estado = mensaje.substring(1, fin);
            String contenido = mensaje.substring(fin + 1).trim();
            if (estado.equals("OK") || estado.equals("INFO")
                    || estado.equals("ALERTA") || estado.equals("ERROR")) {
                emitir(estado, "Linea " + numero + ": " + contenido);
                return;
            }
        }
        emitir("INFO", "Linea " + numero + ": " + mensaje);
    }

    // ------------------------------------------------------------
    // MODO LINEA DE EMPAQUETADO
    // ------------------------------------------------------------

    /**
     * Simula el trabajo de una linea: empaqueta varios palets de
     * cajas, los precinta y los deja listos para etiquetar.
     * @param numero numero de la linea
     * @return 0 si termina bien, 1 si se produce un atasco
     */
    private static int ejecutarLinea(int numero) {
        emitir("INFO", "Arrancando cinta de empaquetado.");
        for (int palet = 1; palet <= PALETS_POR_LINEA; palet++) {
            emitir("INFO", "Palet " + palet + ": colocando " + CAJAS_POR_PALET + " cajas.");

            // Probabilidad pequena de que la cinta se atasque.
            if (ALEATORIO.nextInt(20) == 0) {
                emitir("ERROR", "Atasco en la cinta durante el palet " + palet + ".");
                return 1;
            }
            // Probabilidad de falta de material de embalaje, que se repone.
            if (ALEATORIO.nextInt(8) == 0) {
                emitir("ALERTA", "Palet " + palet + ": falta de film, se repone material.");
            }
            emitir("INFO", "Palet " + palet + ": film protector aplicado.");
            emitir("OK", "Palet " + palet + ": precintado y listo para etiquetar.");
        }
        emitir("OK", "Todos los palets de la linea " + numero + " empaquetados.");
        return 0;
    }

    // ------------------------------------------------------------
    // UTILIDADES
    // ------------------------------------------------------------

    /**
     * Escribe una linea con el formato [ESTADO] mensaje
     * y vacia el buffer para que el lector la reciba al instante.
     * @param estado categoria del mensaje (OK, INFO, ALERTA o ERROR)
     * @param mensaje contenido que se va a enviar
     */
    private static void emitir(String estado, String mensaje) {
        System.out.println("[" + estado + "] " + mensaje);
        System.out.flush();
    }
}
