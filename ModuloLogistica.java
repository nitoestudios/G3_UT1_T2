import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.TimeUnit;

public class ModuloLogistica {

    private static final String IDENTIFICADOR = "LOGISTICA";
    private static final int TOTAL_PIEZAS = 30; // Lote de 30 piezas

    public static void main(String[] args) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        Random random = new Random();

        imprimirMensaje(timeFormatter, "INFO", "Módulo de Logística iniciado.");

        // Bucle para procesar el lote pieza a pieza
        for (int pieza = 1; pieza <= TOTAL_PIEZAS; pieza++) {
            // Calcular tiempo de espera entre 1500 y 2500 milisegundos (1.5 a 2.5 segundos)[cite: 2]
            long tiempoEsperaMs = 1500 + random.nextInt(1001);
            
            // Pausa el proceso actual usando LockSupport (sin usar la clase Thread)
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(tiempoEsperaMs));

            // Emisión del mensaje de actividad de la pieza
            imprimirMensaje(timeFormatter, "INFO", "Pieza #" + pieza + " procesada correctamente.");
        }

        imprimirMensaje(timeFormatter, "INFO", "Lote de producción de logística completado[cite: 2].");
    }

    private static void imprimirMensaje(DateTimeFormatter formatter, String categoria, String mensaje) {
        String horaActual = LocalTime.now().format(formatter);
        // Formato exacto requerido para la consola: [HH:mm:ss] [<IDENTIFICADOR_ORIGEN>] [<ESTADO/CATEGORÍA>] <MENSAJE>[cite: 2]
        System.out.printf("[%s] [%s] [%s] %s%n", horaActual, IDENTIFICADOR, categoria, mensaje);
        System.out.flush(); // Obligatorio para que ProcessBuilder lea el flujo en tiempo real
    }
}