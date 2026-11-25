package es.ilerna.psp.gmm.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente reutilizable para lanzar procesos con ProcessBuilder.
 *
 * Cualquier módulo del proyecto puede usarlo así:
 * <pre>
 *   ArrayList&lt;String&gt; comando = new ArrayList&lt;&gt;();
 *   comando.add("hostname");
 *   ResultadoProceso r = EjecutorProcesos.ejecutar(comando, new File("."));
 * </pre>
 */
public class EjecutorProcesos {

    /** Extensiones que Windows prueba al buscar un ejecutable en el PATH. */
    private static final String[] EXTENSIONES_WINDOWS = {"", ".exe", ".com", ".bat", ".cmd"};

    /**
     * Lanza un proceso, espera a que termine y devuelve su resultado.
     *
     * La salida de error se une a la salida estándar (redirectErrorStream)
     * para mostrar todo junto y en orden. Así, por ejemplo, "java -version",
     * que escribe por la salida de error, también se ve.
     *
     * @param comando    programa y argumentos, cada uno en una posición de la lista
     * @param directorio directorio de trabajo del proceso (null = el actual)
     * @return resultado de la ejecución (nunca null)
     */
    public static ResultadoProceso ejecutar(List<String> comando, File directorio) {
        ResultadoProceso resultado = new ResultadoProceso();

        ProcessBuilder pb = new ProcessBuilder(comando);
        if (directorio != null) {
            pb.directory(directorio);
        }
        pb.redirectErrorStream(true);

        // 1) Intentar arrancar el proceso
        Process proceso;
        try {
            proceso = pb.start();
        } catch (IOException e) {
            // El sistema operativo no ha podido crear el proceso
            resultado.setIniciado(false);
            resultado.setMensajeError(e.getMessage());
            return resultado;
        }
        resultado.setIniciado(true);
        resultado.setPid(proceso.pid());

        // 2) Leer todo lo que escribe el proceso (try-with-resources cierra el lector)
        StringBuilder salida = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(proceso.getInputStream(), SistemaOperativo.getCharsetConsola()))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                salida.append(linea).append(System.lineSeparator());
            }
        } catch (IOException e) {
            salida.append("[No se pudo leer la salida: ").append(e.getMessage()).append("]");
        }
        resultado.setSalida(salida.toString());

        // 3) Esperar a que termine y guardar el código de salida
        try {
            int codigo = proceso.waitFor();
            resultado.setCodigoSalida(codigo);
        } catch (InterruptedException e) {
            proceso.destroy();
            Thread.currentThread().interrupt();
            resultado.setMensajeError("Espera interrumpida");
        }
        return resultado;
    }

    /**
     * Comprueba si un programa se puede encontrar en este sistema.
     *
     * - Si es una ruta (contiene separador), se comprueba que el fichero exista.
     * - Si es solo un nombre (por ejemplo "hostname"), se busca en cada
     *   carpeta de la variable de entorno PATH.
     *
     * @param programa nombre o ruta del programa
     * @return true si se ha encontrado
     */
    public static boolean comandoDisponible(String programa) {
        if (programa == null || programa.isEmpty()) {
            return false;
        }

        // Caso 1: se ha indicado una ruta completa o relativa
        if (programa.contains("/") || programa.contains("\\")) {
            return existeEjecutable(new File(programa));
        }

        // Caso 2: buscar en las carpetas del PATH
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        String[] carpetas = path.split(File.pathSeparator);
        for (int i = 0; i < carpetas.length; i++) {
            if (carpetas[i].isEmpty()) {
                continue;
            }
            if (existeEjecutable(new File(carpetas[i], programa))) {
                return true;
            }
        }
        return false;
    }

    /**
     * En Linux basta con que el fichero exista. En Windows, además, se prueban
     * las extensiones típicas (.exe, .bat...) porque se puede escribir
     * "hostname" en lugar de "hostname.exe".
     */
    private static boolean existeEjecutable(File fichero) {
        if (!SistemaOperativo.esWindows()) {
            return fichero.isFile();
        }
        for (int i = 0; i < EXTENSIONES_WINDOWS.length; i++) {
            File candidato = new File(fichero.getPath() + EXTENSIONES_WINDOWS[i]);
            if (candidato.isFile()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ruta absoluta del ejecutable "java" de la JVM que está ejecutando GMM-Java.
     * Usarla evita depender de que "java" esté en el PATH.
     *
     * @return ruta del ejecutable java
     */
    public static String getRutaJava() {
        File carpetaBin = new File(System.getProperty("java.home"), "bin");
        String nombre = "java";
        if (SistemaOperativo.esWindows()) {
            nombre = "java.exe";
        }
        return new File(carpetaBin, nombre).getAbsolutePath();
    }

    /**
     * Convierte la lista del comando en un texto legible, solo para mostrarlo.
     * Al proceso siempre se le pasa la lista, nunca este texto.
     *
     * @param comando lista con programa y argumentos
     * @return texto con los elementos separados por espacios
     */
    public static String comandoComoTexto(List<String> comando) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < comando.size(); i++) {
            if (i > 0) {
                texto.append(" ");
            }
            String parte = comando.get(i);
            // Si un argumento tiene espacios, se muestra entre comillas para que se vea que es uno solo
            if (parte.contains(" ")) {
                texto.append("\"").append(parte).append("\"");
            } else {
                texto.append(parte);
            }
        }
        return texto.toString();
    }

    /**
     * Crea una lista de comando a partir de varios textos sueltos.
     * Ejemplo: crearComando("cmd.exe", "/c", "dir")
     *
     * @param partes programa y argumentos
     * @return lista modificable con las partes
     */
    public static ArrayList<String> crearComando(String... partes) {
        ArrayList<String> comando = new ArrayList<>();
        for (int i = 0; i < partes.length; i++) {
            comando.add(partes[i]);
        }
        return comando;
    }
}
