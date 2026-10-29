import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Modulo de control de calidad de la fabrica.
 * Se ejecuta como un proceso independiente lanzado por el Coordinador.
 * Simula la inspeccion de lotes de producto: para cada lote revisa
 * las dimensiones, el acabado y el peso de una muestra de piezas,
 * calcula el porcentaje de defectuosas y decide si el lote se
 * aprueba, se aprueba con reservas o se rechaza.
 * Cada linea que escribe en la salida estandar sigue el formato
 * [ESTADO] mensaje, que el Coordinador sabe interpretar.
 * @author Equipo 3 DAM2
 * @version 1.0
 */
public class Calidad {
    /** Nombre del modulo, igual al que usa el Coordinador. */
    private static final String NOMBRE = "CALIDAD";
    /** Archivo cuya existencia activa la parada de emergencia. */
    private static final Path ARCHIVO_PARADA = Path.of("PARADA.txt");
    /** Numero de lotes que se van a inspeccionar. */
    private static final int TOTAL_LOTES = 5;
    /** Numero de piezas que se revisan en la muestra de cada lote. */
    private static final int TAMANO_MUESTRA = 20;
    /** Porcentaje maximo de defectuosas para aprobar sin reservas. */
    private static final double LIMITE_APROBADO = 5.0;
    /** Porcentaje maximo de defectuosas para aprobar con reservas. */
    private static final double LIMITE_RESERVAS = 15.0;
    /** Criterios que se comprueban en cada pieza. */
    private static final String[] CRITERIOS = {
        "dimensiones",
        "acabado superficial",
        "peso"
    };
    /** Generador de numeros aleatorios para la simulacion. */
    private static final Random ALEATORIO = new Random();

    /** Metodo principal del modulo.
     * @param args argumentos de ejecucion (se ignoran)
     */
    public static void main(String[] args) {
        int codigoSalida = 0;
        int lotesAprobados = 0;
        int lotesConReservas = 0;
        int lotesRechazados = 0;

        emitir("INFO", "Modulo de " + NOMBRE + " iniciado.");
        try {
            emitir("INFO", "Calibrando instrumentos de medida.");
            if (!pausa(500)) {
                emitir("ALERTA", "Parada solicitada durante la calibracion.");
                return;
            }
            emitir("OK", "Instrumentos calibrados.");

            for (int lote = 1; lote <= TOTAL_LOTES; lote++) {
                // Antes de cada lote comprobamos si hay emergencia.
                if (Files.exists(ARCHIVO_PARADA)) {
                    emitir("ALERTA", "Parada de emergencia detectada. Se interrumpe la inspeccion.");
                    break;
                }
                emitir("INFO", "Lote " + lote + ": inspeccionando muestra de "
                        + TAMANO_MUESTRA + " piezas.");

                // Revisamos cada criterio sobre la muestra.
                int defectuosas = 0;
                for (String criterio : CRITERIOS) {
                    int fallos = contarFallos();
                    defectuosas += fallos;
                    if (fallos == 0) {
                        emitir("OK", "Lote " + lote + ": " + criterio + " correcto.");
                    } else {
                        emitir("ALERTA", "Lote " + lote + ": " + fallos
                                + " piezas fuera de tolerancia en " + criterio + ".");
                    }
                    if (!pausa(300 + ALEATORIO.nextInt(300))) {
                        emitir("ALERTA", "Proceso interrumpido durante la inspeccion.");
                        return;
                    }
                }

                // Una pieza puede fallar en varios criterios, asi que
                // limitamos el total al tamano de la muestra.
                defectuosas = Math.min(defectuosas, TAMANO_MUESTRA);
                double porcentaje = defectuosas * 100.0 / TAMANO_MUESTRA;
                emitir("INFO", "Lote " + lote + ": " + defectuosas + " piezas defectuosas ("
                        + String.format("%.1f", porcentaje) + "%).");

                // Decision final sobre el lote.
                if (porcentaje <= LIMITE_APROBADO) {
                    emitir("OK", "Lote " + lote + ": APROBADO.");
                    lotesAprobados++;
                } else if (porcentaje <= LIMITE_RESERVAS) {
                    emitir("ALERTA", "Lote " + lote + ": aprobado CON RESERVAS. Se recomienda revisar el proceso.");
                    lotesConReservas++;
                } else {
                    emitir("ERROR", "Lote " + lote + ": RECHAZADO. Debe volver a produccion.");
                    lotesRechazados++;
                }
                if (!pausa(300)) {
                    return;
                }
            }

            emitir("INFO", "Resumen: " + lotesAprobados + " aprobados, "
                    + lotesConReservas + " con reservas, "
                    + lotesRechazados + " rechazados.");
            if (lotesRechazados == 0 && lotesConReservas == 0) {
                emitir("OK", "Control de calidad superado en todos los lotes.");
            } else if (lotesRechazados == 0) {
                emitir("ALERTA", "Control de calidad superado con reservas.");
            } else {
                emitir("ALERTA", "Hay lotes rechazados pendientes de revision.");
            }
        } catch (Exception e) {
            emitir("ERROR", "Fallo inesperado en calidad: " + e.getMessage());
            codigoSalida = 1;
        } finally {
            emitir("INFO", "Modulo de " + NOMBRE + " finalizado.");
        }
        System.exit(codigoSalida);
    }

    /**
     * Simula la revision de un criterio sobre la muestra.
     * La mayoria de las veces no hay fallos; ocasionalmente aparecen
     * algunas piezas fuera de tolerancia.
     * @return numero de piezas que no cumplen el criterio
     */
    private static int contarFallos() {
        int azar = ALEATORIO.nextInt(10);
        if (azar < 6) {
            return 0;
        } else if (azar < 9) {
            return 1;
        }
        return 2 + ALEATORIO.nextInt(3);
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
