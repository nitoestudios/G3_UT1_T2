import java.util.Random;

public class Ensamblaje {
    private static final int TOTAL_PIEZAS = 10;
    private static final Random ALEATORIO = new Random();
    public static void main(String[] args) {
        System.out.println("[INFO] Módulo de ensamblaje iniciado.");
        int piezasCorrectas = 0;
        int piezasFallidas = 0;
        boolean forzarError = false;
        // Comprobar si se ha solicitado forzar un fallo.
        for (String argumento : args) {
            if (argumento.equalsIgnoreCase("--force-error")) {forzarError = true;}
        }
        // Procesar las 30 piezas del lote.
        for (int pieza = 1; pieza <= TOTAL_PIEZAS; pieza++) {
            System.out.println("[INFO] Comenzando ensamblaje de la pieza #" + pieza + ".");
            // Esperar entre 1,5 y 2,5 segundos.
            int pausa = 1500 + ALEATORIO.nextInt(1001);
            try {
                Thread.sleep(pausa);
            } catch (InterruptedException e) {
                System.out.println("[ALERTA] Ensamblaje interrumpido en la pieza #" + pieza + ".");
                Thread.currentThread().interrupt();
                System.exit(2);
            }
            // Simular un fallo con una probabilidad del 15 %.
            boolean fallo = Math.random() < 0.15;
            // Forzar un fallo en la primera pieza si se ha solicitado.
            if (forzarError && pieza == 1) {fallo = true;}
            if (fallo) {piezasFallidas++;
                System.out.println("[ERROR] Fallo al ensamblar la pieza #" + pieza + ".");
            } else {
                piezasCorrectas++;
                System.out.println("[OK] Pieza #" + pieza + " ensamblada correctamente.");
            }
        }
        // Mostrar el resultado final.
        System.out.println("[INFO] Resumen de ensamblaje: "
                + piezasCorrectas + " piezas correctas y "
                + piezasFallidas + " piezas fallidas.");
        System.out.println("[INFO] Módulo de ensamblaje finalizado.");
    }
}
