package es.ilerna.psp.gmm.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Detección del sistema operativo en el que se está ejecutando el programa.
 *
 * Se basa en la propiedad "os.name" de la JVM. Solo se consideran
 * soportados Windows y Linux para los comandos específicos del sistema.
 */
public class SistemaOperativo {

    /**
     * @return nombre del sistema operativo según la JVM, o "Desconocido"
     */
    public static String getNombre() {
        String nombre = System.getProperty("os.name");
        if (nombre == null || nombre.isEmpty()) {
            return "Desconocido";
        }
        return nombre;
    }

    /**
     * @return true si el sistema es Windows
     */
    public static boolean esWindows() {
        return getNombre().toLowerCase().startsWith("windows");
    }

    /**
     * @return true si el sistema es Linux
     */
    public static boolean esLinux() {
        return getNombre().toLowerCase().contains("linux");
    }

    /**
     * @return true si el sistema es Windows o Linux
     */
    public static boolean esSoportado() {
        return esWindows() || esLinux();
    }

    /**
     * Devuelve un texto corto con el tipo de sistema, para la cabecera.
     *
     * @return "Windows", "Linux" o un aviso de sistema no soportado
     */
    public static String getTipo() {
        if (esWindows()) {
            return "Windows";
        } else if (esLinux()) {
            return "Linux";
        } else {
            return getNombre() + " (no soportado para comandos especificos)";
        }
    }

    /**
     * Juego de caracteres con el que los comandos del sistema escriben su salida.
     *
     * En Windows los comandos de consola (dir, ipconfig...) usan la página de
     * códigos OEM, que en España es la 850. Si se leyera como UTF-8, las tildes
     * y las eñes saldrían mal. En Linux la consola trabaja en UTF-8.
     *
     * @return charset adecuado para leer la salida de los procesos hijos
     */
    public static Charset getCharsetConsola() {
        if (esWindows() && Charset.isSupported("IBM850")) {
            return Charset.forName("IBM850");
        }
        return StandardCharsets.UTF_8;
    }
}
