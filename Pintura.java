import java.util.Random;

/**
 * Simula el proceso industrial de pintura de una fábrica.
 * Procesa un lote de piezas y registra los posibles fallos.
 *
 * @author Equipo 3 DAM2
 * @version 1.0
 */
public class Pintura {
    /** Número de piezas que se van a procesar. */
    private static final int TOTAL_PIEZAS = 10;
    /** Probabilidad de que falle una pieza. */
    private static final double PROBABILIDAD_FALLO = 0.15;
    /** Generador de números aleatorios. */
    private static final Random ALEATORIO = new Random();
    /** Tiempo mínimo de procesamiento en milisegundos. */
    private static final int TIEMPO_MINIMO = 1500;
    /** Variación máxima del tiempo de procesamiento. */
    private static final int VARIACION_TIEMPO = 1001;
    /** Causas posibles de fallo durante la pintura. */
    private static final String[] FALLOS = {
        "Se ha agotado la pintura del depósito.",
        "La máquina de pintura se ha atascado.",
        "Se ha detectado un fallo en el pulverizador.",
        "La pieza ha quedado con una cobertura irregular.",
        "Se ha producido un error en el sistema de presión."
    };

    /**
     * Método principal del módulo.
     *
     * @param args argumentos de ejecución.
     *             --force-error fuerza el primer fallo.
     */
    public static void main(String[] args) {
        int piezasCorrectas = 0;
        int piezasFallidas = 0;
        boolean forzarFallo = false;
        // Comprobamos si se ha solicitado forzar un fallo.
        for (String argumento : args) {
            if (argumento.equalsIgnoreCase("--force-error")) {forzarFallo = true;}
        }
        System.out.println("[INFO] Módulo de pintura iniciado.");
        System.out.println("[INFO] Lote de " + TOTAL_PIEZAS + " piezas preparado.");
        for (int pieza = 1; pieza <= TOTAL_PIEZAS; pieza++) {
            System.out.println("[INFO] Comenzando pintura de la pieza #" + pieza + ".");
            // Simulamos el tiempo necesario para pintar la pieza.
            try {
                int tiempo = TIEMPO_MINIMO + ALEATORIO.nextInt(VARIACION_TIEMPO);
                Thread.sleep(tiempo);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[ALERTA] Proceso de pintura interrumpido.");
                break;
            }
            // El primer fallo se fuerza cuando se usa --force-error.
            boolean fallo = (pieza == 1 && forzarFallo) || ALEATORIO.nextDouble() < PROBABILIDAD_FALLO;
            if (fallo) {
                String causa = FALLOS[ALEATORIO.nextInt(FALLOS.length)];
                piezasFallidas++;
                System.out.println("[ERROR] Fallo al pintar la pieza #" + pieza + ": " + causa);
            } else {
                piezasCorrectas++;
                System.out.println("[OK] Pieza #" + pieza + " pintada correctamente.");
            }
        }
        System.out.println("[INFO] Proceso de pintura finalizado.");
        System.out.println("[INFO] Piezas pintadas correctamente: " + piezasCorrectas);
        System.out.println("[INFO] Piezas fallidas: " + piezasFallidas);
        System.out.println("[INFO] Total de piezas procesadas: " + (piezasCorrectas + piezasFallidas));
    }
}
