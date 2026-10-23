import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Modulo de logistica de la fabrica.
 * Se ejecuta como un proceso independiente lanzado por el Coordinador.
 * Simula la preparacion y el despacho de lotes de producto ya
 * empaquetado y etiquetado: asigna un muelle, carga el camion,
 * comprueba la documentacion y confirma la salida.
 * Cada linea que escribe en la salida estandar sigue el formato
 * [ESTADO] mensaje, que el Coordinador sabe interpretar.
 * @author Equipo 3 DAM2
 * @version 1.0
 */
public class Logistica {
    /** Nombre del modulo, igual al que usa el Coordinador. */
    private static final String NOMBRE = "LOGISTICA";
    /** Archivo cuya existencia activa la parada de emergencia. */
    private static final Path ARCHIVO_PARADA = Path.of("PARADA.txt");
    /** Numero de lotes que se van a despachar. */
    private static final int TOTAL_LOTES = 5;
    /** Numero de muelles de carga disponibles. */
    private static final int MUELLES = 3;
    /** Capacidad maxima de cada camion, en cajas. */
    private static final int CAPACIDAD_CAMION = 120;
    /** Generador de numeros aleatorios para la simulacion. */
    private static final Random ALEATORIO = new Random();

    /** Metodo principal del modulo.
     * @param args argumentos de ejecucion (se ignoran)
     */
    public static void main(String[] args) {
        int codigoSalida = 0;
        int lotesDespachados = 0;
        int lotesConIncidencia = 0;

        emitir("INFO", "Modulo de " + NOMBRE + " iniciado.");
        try {
            emitir("INFO", "Comprobando muelles de carga disponibles: " + MUELLES);
            if (!pausa(500)) {
                emitir("ALERTA", "Parada solicitada durante el arranque.");
                return;
            }
            emitir("OK", "Muelles operativos.");

            for (int lote = 1; lote <= TOTAL_LOTES; lote++) {
                // Antes de cada lote comprobamos si hay emergencia.
                if (Files.exists(ARCHIVO_PARADA)) {
                    emitir("ALERTA", "Parada de emergencia detectada. Se interrumpe el despacho.");
                    codigoSalida = 0;
                    break;
                }
                int muelle = ((lote - 1) % MUELLES) + 1;
                int cajas = 40 + ALEATORIO.nextInt(CAPACIDAD_CAMION - 39);

                emitir("INFO", "Lote " + lote + ": " + cajas
                        + " cajas listas para cargar.");
                emitir("INFO", "Lote " + lote + ": camion asignado al muelle " + muelle + ".");
                if (!pausa(400 + ALEATORIO.nextInt(400))) {
                    emitir("ALERTA", "Proceso interrumpido durante la asignacion.");
                    return;
                }

                // Simulamos la carga del camion por tramos.
                int cargadas = 0;
                while (cargadas < cajas) {
                    int tramo = Math.min(30, cajas - cargadas);
                    cargadas += tramo;
                    emitir("INFO", "Lote " + lote + ": cargadas "
                            + cargadas + "/" + cajas + " cajas.");
                    if (!pausa(250)) {
                        emitir("ALERTA", "Proceso interrumpido durante la carga.");
                        return;
                    }
                }

                // Comprobacion de documentacion con un pequeño riesgo de incidencia.
                int azar = ALEATORIO.nextInt(10);
                if (azar == 0) {
                    emitir("ALERTA", "Lote " + lote
                            + ": albaran con datos incompletos. Se reintenta la emision.");
                    lotesConIncidencia++;
                    if (!pausa(500)) {
                        return;
                    }
                    emitir("OK", "Lote " + lote + ": albaran corregido.");
                } else {
                    emitir("OK", "Lote " + lote + ": documentacion correcta.");
                }

                emitir("OK", "Lote " + lote + ": camion despachado desde el muelle " + muelle + ".");
                lotesDespachados++;
                if (!pausa(300)) {
                    return;
                }
            }

            emitir("INFO", "Resumen: " + lotesDespachados + " lotes despachados, "
                    + lotesConIncidencia + " con incidencia.");
            if (lotesDespachados == TOTAL_LOTES) {
                emitir("OK", "Despacho de produccion completado.");
            } else {
                emitir("ALERTA", "Despacho incompleto: " + lotesDespachados
                        + " de " + TOTAL_LOTES + " lotes.");
            }
        } catch (Exception e) {
            emitir("ERROR", "Fallo inesperado en logistica: " + e.getMessage());
            codigoSalida = 1;
        } finally {
            emitir("INFO", "Modulo de " + NOMBRE + " finalizado.");
        }
        System.exit(codigoSalida);
    }

    /**
     * Escribe una linea con el formato [ESTADO] mensaje
     * y vacia el buffer para que el Coordinador la reciba al instante.
     * @param estado categoria del mensaje (OK, INFO, ALERTA o ERROR)
     * @param mensaje contenido que se va a enviar
     */
    private static void emitir(String estado, String mensaje) {
        System.out.println("[" + estado + "] " + mensaje);
        System.out.flush();
    }

    /**
     * Espera un tiempo indicado, comprobando que no se haya pedido parada.
     * @param milisegundos duracion de la pausa
     * @return true si puede continuar, false si fue interrumpido
     */
    private static boolean pausa(long milisegundos) {
        try {
            TimeUnit.MILLISECONDS.sleep(milisegundos);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
