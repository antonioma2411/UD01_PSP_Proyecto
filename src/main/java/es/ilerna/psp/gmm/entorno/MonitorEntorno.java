package es.ilerna.psp.gmm.entorno;

import es.ilerna.psp.gmm.util.SistemaOperativo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Módulo 01. Monitor de entorno y propiedades del sistema.
 *
 * Muestra un informe con:
 *  - M1.1 Propiedades del sistema (System.getProperty)
 *  - M1.2 Información de la JVM (Runtime.getRuntime)
 *  - M1.3 Variables de entorno (ProcessBuilder.environment)
 *  - M1.4 Identificación multiplataforma (Windows / Linux / otro)
 *
 * Ningún dato que falte provoca un error: se muestra "(no disponible)".
 */
public class MonitorEntorno {

    private static final String NO_DISPONIBLE = "(no disponible)";
    private static final long BYTES_POR_MB = 1024 * 1024;

    /**
     * Muestra el informe completo, sección a sección.
     */
    public void mostrarInforme() {
        System.out.println("====================================================");
        System.out.println(" MODULO 01. MONITOR DE ENTORNO DEL SISTEMA");
        System.out.println("====================================================");
        mostrarPropiedadesSistema();
        mostrarInformacionJvm();
        mostrarVariablesEntorno();
        mostrarCompatibilidad();
    }

    // ------------------------------------------------------------------
    // M1.1 Identificación del sistema operativo
    // ------------------------------------------------------------------

    private void mostrarPropiedadesSistema() {
        titulo("M1.1 Propiedades del sistema (System.getProperty)");
        fila("Sistema operativo", leerPropiedad("os.name"));
        fila("Version del SO", leerPropiedad("os.version"));
        fila("Arquitectura", leerPropiedad("os.arch"));
        fila("Version de Java", leerPropiedad("java.version"));
        fila("Fabricante de Java", leerPropiedad("java.vendor"));
        fila("Directorio de trabajo", leerPropiedad("user.dir"));
        fila("Directorio personal", leerPropiedad("user.home"));
        fila("Directorio temporal", leerPropiedad("java.io.tmpdir"));
    }

    /**
     * Lee una propiedad de la JVM sin fallar nunca.
     *
     * @param clave nombre de la propiedad, por ejemplo "os.name"
     * @return valor de la propiedad o "(no disponible)"
     */
    private String leerPropiedad(String clave) {
        try {
            String valor = System.getProperty(clave);
            if (valor == null || valor.isEmpty()) {
                return NO_DISPONIBLE;
            }
            return valor;
        } catch (SecurityException e) {
            // Puede ocurrir si un gestor de seguridad no permite leer la propiedad
            return NO_DISPONIBLE + " - sin permiso";
        }
    }

    // ------------------------------------------------------------------
    // M1.2 Información de la JVM
    // ------------------------------------------------------------------

    private void mostrarInformacionJvm() {
        titulo("M1.2 Informacion de la JVM (Runtime.getRuntime)");
        Runtime runtime = Runtime.getRuntime();

        long total = runtime.totalMemory();   // memoria que la JVM tiene reservada ahora
        long libre = runtime.freeMemory();    // parte de esa memoria que no se está usando
        long maxima = runtime.maxMemory();    // límite que la JVM podría llegar a usar
        long usada = total - libre;

        fila("Procesadores disponibles", String.valueOf(runtime.availableProcessors()));
        fila("Memoria total del heap", aMegas(total));
        fila("Memoria libre del heap", aMegas(libre));
        fila("Memoria usada del heap", aMegas(usada));
        fila("Memoria maxima de la JVM", aMegas(maxima));
        System.out.println();
        System.out.println("  [i] IMPORTANTE: estas cantidades son la memoria que gestiona la");
        System.out.println("      JVM (su heap), NO la memoria RAM total del ordenador.");
    }

    /**
     * Pasa una cantidad de bytes a megabytes con dos decimales.
     */
    private String aMegas(long bytes) {
        double megas = (double) bytes / BYTES_POR_MB;
        return String.format("%.2f MB", megas);
    }

    // ------------------------------------------------------------------
    // M1.3 Variables de entorno
    // ------------------------------------------------------------------

    private void mostrarVariablesEntorno() {
        titulo("M1.3 Variables de entorno (ProcessBuilder.environment)");

        // Copia de las variables de entorno que heredaría un proceso hijo
        Map<String, String> entorno = new ProcessBuilder().environment();

        // Descripción de cada variable que nos interesa
        HashMap<String, String> descripciones = new HashMap<>();
        descripciones.put("JAVA_HOME", "Carpeta de instalacion de Java");
        descripciones.put("TEMP", "Carpeta temporal (Windows)");
        descripciones.put("TMP", "Carpeta temporal (Windows)");
        descripciones.put("USERNAME", "Usuario (Windows)");
        descripciones.put("COMPUTERNAME", "Nombre del equipo (Windows)");
        descripciones.put("HOME", "Carpeta personal (Linux)");
        descripciones.put("USER", "Usuario (Linux)");
        descripciones.put("SHELL", "Interprete de comandos (Linux)");

        // Lista de variables a mostrar según el sistema operativo
        ArrayList<String> variables = new ArrayList<>();
        variables.add("JAVA_HOME");
        if (SistemaOperativo.esWindows()) {
            variables.add("TEMP");
            variables.add("TMP");
            variables.add("USERNAME");
            variables.add("COMPUTERNAME");
        } else {
            variables.add("HOME");
            variables.add("USER");
            variables.add("SHELL");
        }

        for (int i = 0; i < variables.size(); i++) {
            String nombre = variables.get(i);
            String valor = entorno.get(nombre);   // devuelve null si no existe
            if (valor == null || valor.isEmpty()) {
                valor = "(no definida)";
            }
            System.out.println("  " + nombre + " -> " + valor);
            System.out.println("      " + descripciones.get(nombre));
        }

        // PATH se muestra aparte, una carpeta por línea, porque suele ser muy largo
        System.out.println();
        String path = entorno.get("PATH");
        if (path == null || path.isEmpty()) {
            System.out.println("  PATH -> (no definida)");
        } else {
            String[] carpetas = path.split(File.pathSeparator);
            System.out.println("  PATH -> " + carpetas.length + " carpetas (separador '" + File.pathSeparator + "')");
            for (int i = 0; i < carpetas.length; i++) {
                System.out.println("      [" + (i + 1) + "] " + carpetas[i]);
            }
        }
    }

    // ------------------------------------------------------------------
    // M1.4 Identificación multiplataforma
    // ------------------------------------------------------------------

    private void mostrarCompatibilidad() {
        titulo("M1.4 Identificacion multiplataforma");
        if (SistemaOperativo.esWindows()) {
            System.out.println("  [OK] Sistema Windows detectado.");
            System.out.println("       Se usaran comandos de Windows (cmd.exe /c dir, ipconfig...).");
        } else if (SistemaOperativo.esLinux()) {
            System.out.println("  [OK] Sistema Linux detectado.");
            System.out.println("       Se usaran comandos de Linux (ls -la, ip addr...).");
        } else {
            System.out.println("  [AVISO] Sistema '" + SistemaOperativo.getNombre() + "' no soportado.");
            System.out.println("          No se podran ejecutar los comandos especificos del sistema.");
            System.out.println("          Las opciones que solo usan Java seguiran funcionando.");
        }
    }

    // ------------------------------------------------------------------
    // Ayudas para dar formato al informe
    // ------------------------------------------------------------------

    private void titulo(String texto) {
        System.out.println();
        System.out.println("--- " + texto + " ---");
    }

    private void fila(String etiqueta, String valor) {
        System.out.println(String.format("  %-26s: %s", etiqueta, valor));
    }
}
