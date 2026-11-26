package es.ilerna.psp.gmm.procesos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Historial de ejecuciones (M3.4) y opción [8] del menú principal.
 *
 * Cada ejecución se guarda en dos sitios:
 *  - En memoria (lista), para mostrar una tabla de la sesión actual.
 *  - En el fichero logs/historial_ejecuciones.log (UTF-8, modo añadir), para
 *    que quede un registro persistente aunque se cierre el programa.
 *
 * La ruta es relativa al directorio desde el que se ejecuta GMM-Java,
 * así no depende de ninguna carpeta concreta del ordenador.
 */
public class HistorialEjecuciones {

    private static final File FICHERO_LOG = new File("logs", "historial_ejecuciones.log");
    private static final int LINEAS_LOG_A_MOSTRAR = 20;

    private static final ArrayList<RegistroEjecucion> REGISTROS = new ArrayList<>();

    /**
     * Añade un registro a la lista y lo escribe al final del fichero de log.
     *
     * @param registro ejecución ya finalizada
     */
    public static void guardar(RegistroEjecucion registro) {
        REGISTROS.add(registro);
        escribirEnLog(registro.toLineaLog());
    }

    private static void escribirEnLog(String linea) {
        File carpeta = FICHERO_LOG.getParentFile();
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        // FileOutputStream(fichero, true) = modo AÑADIR al final del fichero
        try (PrintWriter escritor = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(FICHERO_LOG, true), StandardCharsets.UTF_8))) {
            escritor.println(linea);
        } catch (IOException e) {
            System.out.println("  [!] No se pudo escribir en el log: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Opción [8] Consultar registros de ejecución
    // ------------------------------------------------------------------

    /**
     * Muestra la tabla de la sesión actual, un resumen por estados y las
     * últimas líneas del fichero de log persistente.
     */
    public static void mostrar() {
        System.out.println("====================================================");
        System.out.println(" REGISTROS DE EJECUCION");
        System.out.println("====================================================");
        mostrarTablaSesion();
        mostrarResumen();
        mostrarFicheroLog();
    }

    private static void mostrarTablaSesion() {
        System.out.println();
        System.out.println("--- Ejecuciones de esta sesion ---");
        if (REGISTROS.isEmpty()) {
            System.out.println("  (todavia no se ha ejecutado nada; pruebe la opcion [3])");
            return;
        }
        String formato = "  %-3s %-7s %-12s %-12s %8s %6s  %-10s %s";
        System.out.println(String.format(formato, "N", "PID", "INICIO", "FIN", "DURACION", "COD", "ESTADO", "COMANDO"));
        for (int i = 0; i < REGISTROS.size(); i++) {
            RegistroEjecucion r = REGISTROS.get(i);
            System.out.println(String.format(formato,
                    String.valueOf(i + 1), r.getPidTexto(), r.getInicioTexto(), r.getFinTexto(),
                    r.getDuracionMs() + " ms", r.getCodigoTexto(), r.getEstado(), recortar(r.getComando(), 60)));
        }
    }

    private static void mostrarResumen() {
        int completados = 0;
        int errores = 0;
        int timeouts = 0;
        int cancelados = 0;
        for (int i = 0; i < REGISTROS.size(); i++) {
            EstadoEjecucion estado = REGISTROS.get(i).getEstado();
            if (estado == EstadoEjecucion.COMPLETADO) {
                completados++;
            } else if (estado == EstadoEjecucion.ERROR) {
                errores++;
            } else if (estado == EstadoEjecucion.TIMEOUT) {
                timeouts++;
            } else if (estado == EstadoEjecucion.CANCELADO) {
                cancelados++;
            }
        }
        System.out.println();
        System.out.println("--- Resumen de la sesion ---");
        System.out.println("  Iniciadas: " + REGISTROS.size()
                + " | Completadas: " + completados
                + " | Con error: " + errores
                + " | Timeout: " + timeouts
                + " | Canceladas: " + cancelados);
    }

    /**
     * Lee el fichero de log y muestra solo las últimas líneas.
     */
    private static void mostrarFicheroLog() {
        System.out.println();
        System.out.println("--- Fichero persistente: " + FICHERO_LOG.getPath() + " ---");
        if (!FICHERO_LOG.exists()) {
            System.out.println("  (el fichero aun no existe)");
            return;
        }

        ArrayList<String> lineas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(FICHERO_LOG), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (IOException e) {
            System.out.println("  [!] No se pudo leer el log: " + e.getMessage());
            return;
        }

        System.out.println("  Total de lineas guardadas: " + lineas.size()
                + " (se muestran las " + LINEAS_LOG_A_MOSTRAR + " ultimas como maximo)");
        int primera = lineas.size() - LINEAS_LOG_A_MOSTRAR;
        if (primera < 0) {
            primera = 0;
        }
        for (int i = primera; i < lineas.size(); i++) {
            System.out.println("  " + lineas.get(i));
        }
    }

    /**
     * Acorta un texto largo para que la tabla no se descuadre.
     */
    private static String recortar(String texto, int maximo) {
        if (texto.length() <= maximo) {
            return texto;
        }
        return "..." + texto.substring(texto.length() - (maximo - 3));
    }
}
