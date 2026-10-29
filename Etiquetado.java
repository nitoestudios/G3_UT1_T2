import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * Modulo de etiquetado de la fabrica.
 * Se ejecuta como un proceso independiente lanzado por el Coordinador.
 * Simula el etiquetado de lotes de cajas ya empaquetadas: genera un
 * codigo de barras con digito de control, imprime la etiqueta, la
 * aplica y la verifica leyendola de nuevo. Si la lectura falla se
 * reimprime la etiqueta, con un maximo de intentos por lote.
 * No usa hilos: no hay pausas simuladas ni clases de concurrencia.
 * Cada linea que escribe en la salida estandar sigue el formato
 * [ESTADO] mensaje, que el Coordinador sabe interpretar.
 * @author Equipo 3 DAM2
 * @version 1.0
 */
public class Etiquetado {
    /** Nombre del modulo, igual al que usa el Coordinador. */
    private static final String NOMBRE = "ETIQUETADO";
    /** Archivo cuya existencia activa la parada de emergencia. */
    private static final Path ARCHIVO_PARADA = Path.of("PARADA.txt");
    /** Numero de lotes que se van a etiquetar. */
    private static final int TOTAL_LOTES = 5;
    /** Maximo de intentos de impresion por etiqueta. */
    private static final int MAX_INTENTOS = 3;
    /** Numero de digitos de datos del codigo de barras. */
    private static final int DIGITOS_CODIGO = 8;
    /** Generador de numeros aleatorios para la simulacion. */
    private static final Random ALEATORIO = new Random();

    /** Metodo principal del modulo.
     * @param args argumentos de ejecucion (se ignoran)
     */
    public static void main(String[] args) {
        int codigoSalida = 0;
        int lotesEtiquetados = 0;
        int lotesSinEtiquetar = 0;
        int reimpresiones = 0;

        emitir("INFO", "Modulo de " + NOMBRE + " iniciado.");
        try {
            emitir("INFO", "Comprobando impresora de etiquetas y rollo de papel.");
            emitir("OK", "Impresora lista.");

            for (int lote = 1; lote <= TOTAL_LOTES; lote++) {
                // Antes de cada lote comprobamos si hay emergencia.
                if (Files.exists(ARCHIVO_PARADA)) {
                    emitir("ALERTA", "Parada de emergencia detectada. Se interrumpe el etiquetado.");
                    break;
                }
                String codigo = generarCodigo();
                emitir("INFO", "Lote " + lote + ": codigo de barras generado " + codigo + ".");

                // Imprimimos y verificamos hasta que la lectura sea correcta.
                boolean verificada = false;
                int intento = 1;
                while (!verificada && intento <= MAX_INTENTOS) {
                    emitir("INFO", "Lote " + lote + ": imprimiendo etiqueta (intento "
                            + intento + " de " + MAX_INTENTOS + ").");
                    verificada = leerEtiqueta(codigo);
                    if (!verificada) {
                        emitir("ALERTA", "Lote " + lote + ": codigo ilegible, se reimprime la etiqueta.");
                        reimpresiones++;
                        intento++;
                    }
                }

                if (verificada) {
                    emitir("OK", "Lote " + lote + ": etiqueta aplicada y verificada.");
                    lotesEtiquetados++;
                } else {
                    emitir("ERROR", "Lote " + lote + ": no se pudo etiquetar tras "
                            + MAX_INTENTOS + " intentos.");
                    lotesSinEtiquetar++;
                }
            }

            emitir("INFO", "Resumen: " + lotesEtiquetados + " lotes etiquetados, "
                    + lotesSinEtiquetar + " sin etiquetar, "
                    + reimpresiones + " reimpresiones.");
            if (lotesSinEtiquetar == 0 && lotesEtiquetados == TOTAL_LOTES) {
                emitir("OK", "Etiquetado completado en todos los lotes.");
            } else {
                emitir("ALERTA", "Etiquetado incompleto: " + lotesEtiquetados
                        + " de " + TOTAL_LOTES + " lotes.");
            }
        } catch (Exception e) {
            emitir("ERROR", "Fallo inesperado en etiquetado: " + e.getMessage());
            codigoSalida = 1;
        } finally {
            emitir("INFO", "Modulo de " + NOMBRE + " finalizado.");
        }
        System.exit(codigoSalida);
    }

    /**
     * Genera un codigo de barras de datos aleatorios seguido de su
     * digito de control.
     * @return codigo completo (datos mas digito de control)
     */
    private static String generarCodigo() {
        StringBuilder datos = new StringBuilder();
        for (int i = 0; i < DIGITOS_CODIGO; i++) {
            datos.append(ALEATORIO.nextInt(10));
        }
        return datos.toString() + calcularControl(datos.toString());
    }

    /**
     * Calcula el digito de control: suma de los digitos, cada uno
     * multiplicado por 3 en las posiciones pares y por 1 en las impares,
     * y complemento a 10 del resultado.
     * @param datos cadena de digitos sin el digito de control
     * @return digito de control entre 0 y 9
     */
    private static int calcularControl(String datos) {
        int suma = 0;
        for (int i = 0; i < datos.length(); i++) {
            int digito = datos.charAt(i) - '0';
            suma += (i % 2 == 0) ? digito * 3 : digito;
        }
        return (10 - (suma % 10)) % 10;
    }

    /**
     * Simula la lectura de la etiqueta con el lector de codigos.
     * Con una probabilidad pequena la lectura falla (etiqueta
     * arrugada, tinta defectuosa) y, si no, se comprueba que el
     * digito de control del codigo leido es correcto.
     * @param codigo codigo impreso en la etiqueta
     * @return true si la lectura es valida
     */
    private static boolean leerEtiqueta(String codigo) {
        if (ALEATORIO.nextInt(6) == 0) {
            return false;
        }
        String datos = codigo.substring(0, DIGITOS_CODIGO);
        int control = codigo.charAt(DIGITOS_CODIGO) - '0';
        return calcularControl(datos) == control;
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
}
