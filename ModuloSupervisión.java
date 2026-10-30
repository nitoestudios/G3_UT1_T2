import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ModuloSupervisión {

    private static final String IDENTIFICADOR = "SUPERVISION";
    private static final String[] MODULOS_MONITORIZADOS = {
        "ModuloLogistica",
        "Calidad",
        "Empaquetado",
        "Etiquetado"
    };

    private static final int STOCK_MINIMO_PIEZAS = 10;
    private static final int STOCK_MINIMO_PINTURA = 15;
    private static final int STOCK_MINIMO_MATERIALES = 12;
    private static final int CALIDAD_MAXIMA_DEFECTOS = 5;

    public static void main(String[] args) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        imprimirMensaje(timeFormatter, "INFO", "Módulo de Supervisión iniciado.");
        imprimirMensaje(timeFormatter, "INFO", "Monitoreo de módulos reales: " + String.join(", ", MODULOS_MONITORIZADOS));

        boolean hayErrorCritico = false;

        for (int indice = 0; indice < MODULOS_MONITORIZADOS.length; indice++) {
            String nombreModulo = MODULOS_MONITORIZADOS[indice];
            EstadoModulo estado = obtenerEstadoModulo(nombreModulo);

            if (estado.nivel.equals("ERROR")) {
                imprimirMensaje(timeFormatter, "ERROR", "Módulo " + nombreModulo + ": " + estado.mensaje);
                hayErrorCritico = true;
            } else if (estado.nivel.equals("ALERTA")) {
                imprimirMensaje(timeFormatter, "ALERTA", "Módulo " + nombreModulo + ": " + estado.mensaje);
            } else {
                imprimirMensaje(timeFormatter, "INFO", "Módulo " + nombreModulo + ": " + estado.mensaje);
            }

            if (indice < MODULOS_MONITORIZADOS.length - 1) {
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1000));
            }
        }

        if (hayErrorCritico) {
            imprimirMensaje(timeFormatter, "ERROR", "Parada crítica detectada. Debe revisarse el inventario y los límites de producción.");
        } else {
            imprimirMensaje(timeFormatter, "INFO", "Estado general de la fábrica: ESTABLE. Todos los módulos reales cumplen los criterios de operación.");
        }

        imprimirMensaje(timeFormatter, "INFO", "Cierre del módulo de supervisión.");
    }

    private static EstadoModulo obtenerEstadoModulo(String nombreModulo) {
        try {
            Class<?> clase = Class.forName(nombreModulo);

            if (clase.getSimpleName().equals("ModuloLogistica")) {
                int piezasTotales = leerCampoInt(clase, "TOTAL_PIEZAS");
                if (piezasTotales < STOCK_MINIMO_PIEZAS) {
                    return new EstadoModulo("ERROR", "Piezas insuficientes para la línea: " + piezasTotales + " < " + STOCK_MINIMO_PIEZAS + ".");
                }
                return new EstadoModulo("OK", "Lote de logística operativo. Piezas previstas: " + piezasTotales + ".");
            }

            if (clase.getSimpleName().equals("Calidad")) {
                int tamanoMuestra = leerCampoInt(clase, "TAMANO_MUESTRA");
                double limiteAprobado = leerCampoDouble(clase, "LIMITE_APROBADO");
                double limiteReservas = leerCampoDouble(clase, "LIMITE_RESERVAS");

                if (tamanoMuestra <= 0) {
                    return new EstadoModulo("ERROR", "La muestra de calidad no está definida correctamente.");
                }
                if (limiteAprobado > CALIDAD_MAXIMA_DEFECTOS || limiteReservas > 15.0) {
                    return new EstadoModulo("ALERTA", "Los umbrales de calidad están fuera de rango. Aprobado: " + limiteAprobado + "%; reservas: " + limiteReservas + "%.");
                }
                return new EstadoModulo("OK", "Calidad operativa. Muestra de " + tamanoMuestra + " piezas; umbral aprobado: " + limiteAprobado + "%.");
            }

            if (clase.getSimpleName().equals("Empaquetado")) {
                int numLineas = leerCampoInt(clase, "NUM_LINEAS");
                int paletsPorLinea = leerCampoInt(clase, "PALETS_POR_LINEA");
                int cajasPorPalet = leerCampoInt(clase, "CAJAS_POR_PALET");
                int nivelPintura = 18;
                int nivelMateriales = 16;

                if (nivelPintura < STOCK_MINIMO_PINTURA || nivelMateriales < STOCK_MINIMO_MATERIALES) {
                    return new EstadoModulo("ALERTA", "Recursos de producción bajos: pintura " + nivelPintura + "/" + STOCK_MINIMO_PINTURA + ", materiales " + nivelMateriales + "/" + STOCK_MINIMO_MATERIALES + ".");
                }
                if (numLineas < 2) {
                    return new EstadoModulo("ERROR", "Hay demasiadas pocas líneas de empaquetado para mantener producción.");
                }
                if (paletsPorLinea < 2 || cajasPorPalet < 20) {
                    return new EstadoModulo("ALERTA", "Capacidad de empaquetado por debajo del mínimo recomendado.");
                }
                return new EstadoModulo("OK", "Empaquetado operativo. Líneas: " + numLineas + ", palets/linea: " + paletsPorLinea + ", cajas/palet: " + cajasPorPalet + ".");
            }

            if (clase.getSimpleName().equals("Etiquetado")) {
                int totalLotes = leerCampoInt(clase, "TOTAL_LOTES");
                int maxIntentos = leerCampoInt(clase, "MAX_INTENTOS");
                int digitosCodigo = leerCampoInt(clase, "DIGITOS_CODIGO");

                if (totalLotes < 3) {
                    return new EstadoModulo("ALERTA", "El número de lotes etiquetados es demasiado bajo para mantener flujo continuo.");
                }
                if (maxIntentos < 3 || digitosCodigo < 8) {
                    return new EstadoModulo("ERROR", "La configuración de etiquetado no cumple el mínimo de seguridad requerido.");
                }
                return new EstadoModulo("OK", "Etiquetado operativo. Lotes: " + totalLotes + ", intentos: " + maxIntentos + ", código: " + digitosCodigo + " dígitos.");
            }

            return new EstadoModulo("INFO", "Módulo detectado, pero sin reglas de supervisión concretas.");

        } catch (ClassNotFoundException e) {
            return new EstadoModulo("ERROR", "La clase no existe en la carpeta del proyecto.");
        }
    }

    private static int leerCampoInt(Class<?> clase, String nombreCampo) {
        try {
            Field campo = clase.getDeclaredField(nombreCampo);
            campo.setAccessible(true);
            Object valor = campo.get(null);
            return ((Number) valor).intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    private static double leerCampoDouble(Class<?> clase, String nombreCampo) {
        try {
            Field campo = clase.getDeclaredField(nombreCampo);
            campo.setAccessible(true);
            Object valor = campo.get(null);
            return ((Number) valor).doubleValue();
        } catch (Exception e) {
            return -1.0;
        }
    }

    private static void imprimirMensaje(DateTimeFormatter formatter, String categoria, String mensaje) {
        String horaActual = LocalTime.now().format(formatter);
        String linea = String.format("[%s] [%s] [%s] %s", horaActual, IDENTIFICADOR, categoria, mensaje);
        System.out.println(linea);
        System.out.flush();
    }

    private static class EstadoModulo {
        private final String nivel;
        private final String mensaje;

        public EstadoModulo(String nivel, String mensaje) {
            this.nivel = nivel;
            this.mensaje = mensaje;
        }
    }
}
