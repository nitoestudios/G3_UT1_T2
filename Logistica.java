import java.util.Random;

public class Logistica {
    // Número total de piezas que se van a transportar.
    private static final int TOTAL_PIEZAS = 10;
    // Probabilidad de que ocurra una incidencia logística (10 %).
    private static final double PROBABILIDAD_FALLO = 0.10;
    // Generador de números aleatorios para simular las incidencias.
    private static final Random ALEATORIO = new Random();
    public static void main(String[] args) {
        int piezasTransportadas = 0;
        int piezasConIncidencia = 0;
        System.out.println("[INFO] Módulo de logística iniciado.");
        System.out.println("[INFO] Preparando el transporte de " + TOTAL_PIEZAS + " piezas al almacén.");
        // Recorremos todas las piezas del lote.
        for (int pieza = 1; pieza <= TOTAL_PIEZAS; pieza++) {
            System.out.println("[INFO] Recogiendo la pieza #" + pieza + " de la zona de producción.");
            try {
                // Simula el tiempo necesario para transportar la pieza.
                int tiempoEspera = 1000 + ALEATORIO.nextInt(1001);
                Thread.sleep(tiempoEspera);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[ALERTA] El transporte logístico " + "ha sido interrumpido.");
                break;
            }
            // Comprobamos si ocurre una incidencia durante el transporte.
            if (ALEATORIO.nextDouble() < PROBABILIDAD_FALLO) {
                piezasConIncidencia++;
                int tipoIncidencia = ALEATORIO.nextInt(3);
                switch (tipoIncidencia) {
                    case 0:
                        System.out.println("[ALERTA] La pieza #" + pieza
                                + " no se ha podido transportar por un "
                                + "atasco en la cinta transportadora.");
                        break;
                    case 1:
                        System.out.println("[ALERTA] No hay espacio " + "disponible en el almacén para la pieza #" 
                        + pieza + ".");
                        break;
                    case 2:
                        System.out.println("[ERROR] No se ha podido " + "registrar la entrada de la pieza #" 
                        + pieza + " en el inventario.");
                        break;
                    default:
                        break;
                }
                System.out.println("[INFO] La pieza #" + pieza + " queda pendiente de resolución.");
            } else {
                piezasTransportadas++;
                System.out.println("[OK] Pieza #" + pieza + " transportada al almacén y registrada " + "correctamente.");
            }
        }
        // Resumen final de la actividad logística.
        System.out.println("[INFO] Transporte logístico finalizado.");
        System.out.println("[INFO] Piezas almacenadas correctamente: " + piezasTransportadas);
        System.out.println("[INFO] Piezas pendientes por incidencias: " + piezasConIncidencia);
        System.out.println("[INFO] Total de piezas revisadas: " + (piezasTransportadas + piezasConIncidencia));
    }
}