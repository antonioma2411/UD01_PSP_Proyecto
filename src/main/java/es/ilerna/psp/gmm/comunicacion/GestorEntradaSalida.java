package es.ilerna.psp.gmm.comunicacion;

import es.ilerna.psp.gmm.procesos.EstadoEjecucion;
import es.ilerna.psp.gmm.procesos.HistorialEjecuciones;
import es.ilerna.psp.gmm.procesos.RegistroEjecucion;
import es.ilerna.psp.gmm.util.EjecutorProcesos;
import es.ilerna.psp.gmm.util.SistemaOperativo;
import es.ilerna.psp.gmm.util.Teclado;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Módulo 04. Gestión de entrada, salida y registros.
 *
 * - M4.1 Captura de la salida estándar (getInputStream + InputStreamReader + BufferedReader).
 * - M4.2 Captura de errores con getErrorStream() y un error controlado.
 * - M4.3 Flujos separados frente a redirectErrorStream(true).
 * - M4.4 Guardado en ficheros UTF-8: sobrescribir, añadir y errores aparte.
 * - M4.5 Comunicación padre-hijo con EcoLector (getOutputStream + PrintWriter).
 * - M4.6 Herencia de consola con inheritIO(), en una demostración separada.
 *
 * Cada modalidad de redirección se prueba por separado porque algunas son
 * incompatibles (con inheritIO() el padre ya no puede leer la salida del hijo).
 */
public class GestorEntradaSalida {

    private static final String CLASE_AUXILIAR = "es.ilerna.psp.gmm.worker.ProgramaAuxiliar";
    private static final String CLASE_ERRORES = "es.ilerna.psp.gmm.worker.ProgramaConErrores";
    private static final String CLASE_ECO = "es.ilerna.psp.gmm.worker.EcoLector";

    /** Los programas Java del proyecto solo escriben texto sencillo; EcoLector usa UTF-8. */
    private static final Charset CHARSET_HIJO_JAVA = StandardCharsets.UTF_8;

    /** Segundos máximos de espera para que un hijo termine. */
    private static final int TIMEOUT = 10;

    // Ficheros del apartado M4.4 (rutas relativas al directorio de ejecución)
    private static final File FICHERO_SOBRESCRIBIR = new File("resultados", "salida_sobrescribir.txt");
    private static final File FICHERO_ANADIR = new File("resultados", "salida_anadir.txt");
    private static final File FICHERO_SIN_ERRORES = new File("resultados", "salida_sin_errores.txt");
    private static final File FICHERO_ERRORES = new File("logs", "errores_procesos.log");

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Menú del módulo. Se repite hasta que el usuario elige volver.
     */
    public void mostrarMenu() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println("====================================================");
            System.out.println(" MODULO 04. GESTION DE ENTRADA, SALIDA Y REGISTROS");
            System.out.println("====================================================");
            System.out.println("[1] Capturar la salida estandar (M4.1)");
            System.out.println("[2] Capturar los errores con getErrorStream() (M4.2)");
            System.out.println("[3] Flujos separados vs redirectErrorStream(true) (M4.3)");
            System.out.println("[4] Guardar en ficheros: sobrescribir, anadir y errores aparte (M4.4)");
            System.out.println("[5] EcoLector: conversacion interactiva (M4.5)");
            System.out.println("[6] EcoLector: envio automatico y cierre de la entrada (M4.5)");
            System.out.println("[7] Herencia de consola con inheritIO() (M4.6)");
            System.out.println("[0] Volver al menu principal");
            System.out.println();

            opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 7);
            System.out.println();

            switch (opcion) {
                case 1:
                    capturarSalidaEstandar();
                    break;
                case 2:
                    capturarErrores();
                    break;
                case 3:
                    compararFlujos();
                    break;
                case 4:
                    guardarEnFicheros();
                    break;
                case 5:
                    conversarConEcoLector();
                    break;
                case 6:
                    enviarAutomaticoAEcoLector();
                    break;
                case 7:
                    demostrarInheritIO();
                    break;
                case 0:
                    System.out.println("Volviendo al menu principal...");
                    break;
                default:
                    System.out.println("Opcion no valida.");
            }

            if (opcion != 0) {
                Teclado.pausar();
            }
        }
    }

    // ------------------------------------------------------------------
    // M4.1 Captura de la salida estándar
    // ------------------------------------------------------------------

    private void capturarSalidaEstandar() {
        mostrarTitulo("M4.1 Captura de la salida estandar");
        ArrayList<String> comando;
        if (SistemaOperativo.esWindows()) {
            comando = EjecutorProcesos.crearComando("cmd.exe", "/c", "dir");
        } else {
            comando = EjecutorProcesos.crearComando("ls", "-la");
        }

        ProcessBuilder pb = new ProcessBuilder(comando);
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(comando));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }

        // La cadena de lectura paso a paso:
        //   bytes (InputStream) -> caracteres (InputStreamReader) -> líneas (BufferedReader)
        System.out.println("  getInputStream() -> InputStreamReader(" + SistemaOperativo.getCharsetConsola()
                + ") -> BufferedReader");
        System.out.println("  ------------- texto recibido del hijo -------------");
        InputStream bytes = proceso.getInputStream();
        int numero = 0;
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(bytes, SistemaOperativo.getCharsetConsola()))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                numero++;
                System.out.println(String.format("  %3d | %s", numero, linea));
            }
        } catch (IOException e) {
            System.out.println("  [!] Error leyendo la salida: " + e.getMessage());
        }
        System.out.println("  ---------------------------------------------------");
        System.out.println("  Lineas recibidas: " + numero);
        esperarFin(proceso, registro);
    }

    // ------------------------------------------------------------------
    // M4.2 Captura de errores
    // ------------------------------------------------------------------

    private void capturarErrores() {
        mostrarTitulo("M4.2 Captura de errores con getErrorStream()");
        System.out.println("  ProgramaConErrores intenta abrir un fichero que no existe (error controlado).");

        ProcessBuilder pb = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ERRORES));
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(pb.command()));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }

        // El hijo escribe muy poco, así que se puede leer un flujo y después el otro.
        // Con salidas muy grandes habría que leerlos a la vez (hilos) o usar
        // redirectErrorStream(true), porque si se llena una tubería el hijo se bloquea.
        ArrayList<String> salida = leerLineas(proceso.getInputStream(), CHARSET_HIJO_JAVA);
        ArrayList<String> errores = leerLineas(proceso.getErrorStream(), CHARSET_HIJO_JAVA);

        mostrarLineas("getInputStream()  -> salida normal", "[OUT]", salida);
        mostrarLineas("getErrorStream()  -> errores", "[ERR]", errores);
        int codigo = esperarFin(proceso, registro);
        if (codigo != 0 && !errores.isEmpty()) {
            System.out.println("  [OK] Error controlado detectado: el hijo informo por stderr y termino con codigo "
                    + codigo + ".");
        }
    }

    // ------------------------------------------------------------------
    // M4.3 Flujos separados frente a redirectErrorStream(true)
    // ------------------------------------------------------------------

    private void compararFlujos() {
        mostrarTitulo("M4.3 Flujos separados vs redirectErrorStream(true)");

        System.out.println();
        System.out.println("  A) FLUJOS SEPARADOS (por defecto)");
        ProcessBuilder separados = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ERRORES));
        RegistroEjecucion registroA = new RegistroEjecucion("[separados] "
                + EjecutorProcesos.comandoResumido(separados.command()));
        Process procesoA = iniciar(separados, registroA);
        if (procesoA != null) {
            mostrarLineas("getInputStream()", "[OUT]", leerLineas(procesoA.getInputStream(), CHARSET_HIJO_JAVA));
            mostrarLineas("getErrorStream()", "[ERR]", leerLineas(procesoA.getErrorStream(), CHARSET_HIJO_JAVA));
            esperarFin(procesoA, registroA);
        }

        System.out.println();
        System.out.println("  B) FLUJOS COMBINADOS con redirectErrorStream(true)");
        ProcessBuilder combinados = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ERRORES));
        combinados.redirectErrorStream(true);
        RegistroEjecucion registroB = new RegistroEjecucion("[combinados] "
                + EjecutorProcesos.comandoResumido(combinados.command()));
        Process procesoB = iniciar(combinados, registroB);
        if (procesoB != null) {
            mostrarLineas("getInputStream() (salida + errores)", "[MIX]",
                    leerLineas(procesoB.getInputStream(), CHARSET_HIJO_JAVA));
            mostrarLineas("getErrorStream() (queda vacio)", "[ERR]",
                    leerLineas(procesoB.getErrorStream(), CHARSET_HIJO_JAVA));
            esperarFin(procesoB, registroB);
        }

        System.out.println();
        System.out.println("  DIFERENCIAS:");
        System.out.println("   - Separados: se sabe que linea es error y cual no, pero se pierde el orden");
        System.out.println("     real (primero se leen todas las OUT y luego todas las ERR). Hay que leer");
        System.out.println("     los dos flujos; si uno se llena y no se lee, el hijo se queda bloqueado.");
        System.out.println("   - Combinados: un solo flujo, las lineas salen en el orden real en que se");
        System.out.println("     escribieron y solo hay que leer uno (sin riesgo de bloqueo), pero ya no");
        System.out.println("     se puede distinguir que lineas eran errores. getErrorStream() queda vacio.");
    }

    // ------------------------------------------------------------------
    // M4.4 Registro en ficheros (UTF-8)
    // ------------------------------------------------------------------

    private void guardarEnFicheros() {
        mostrarTitulo("M4.4 Guardar la salida en ficheros UTF-8");

        ProcessBuilder pb = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ERRORES));
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(pb.command()));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }
        ArrayList<String> salida = leerLineas(proceso.getInputStream(), CHARSET_HIJO_JAVA);
        ArrayList<String> errores = leerLineas(proceso.getErrorStream(), CHARSET_HIJO_JAVA);
        int codigo = esperarFin(proceso, registro);

        String cabecera = "=== Ejecucion del " + LocalDateTime.now().format(FORMATO_FECHA)
                + " (codigo de salida " + codigo + ") ===";

        // Modo 1: SOBRESCRIBIR -> el fichero se vacía y solo queda la última ejecución
        ArrayList<String> todo = new ArrayList<>();
        todo.add(cabecera);
        todo.addAll(salida);
        todo.addAll(errores);
        escribirFichero(FICHERO_SOBRESCRIBIR, todo, false);

        // Modo 2: AÑADIR -> se escribe al final y se conservan las ejecuciones anteriores
        escribirFichero(FICHERO_ANADIR, todo, true);

        // Modo 3: ERRORES APARTE -> salida normal en un fichero, errores en otro
        ArrayList<String> soloSalida = new ArrayList<>();
        soloSalida.add(cabecera);
        soloSalida.addAll(salida);
        escribirFichero(FICHERO_SIN_ERRORES, soloSalida, false);

        ArrayList<String> soloErrores = new ArrayList<>();
        soloErrores.add(cabecera);
        soloErrores.addAll(errores);
        escribirFichero(FICHERO_ERRORES, soloErrores, true);

        System.out.println();
        System.out.println("  Ficheros generados (UTF-8):");
        informarFichero("1) Sobrescribir", FICHERO_SOBRESCRIBIR);
        informarFichero("2) Anadir", FICHERO_ANADIR);
        informarFichero("3a) Salida sin errores", FICHERO_SIN_ERRORES);
        informarFichero("3b) Solo errores", FICHERO_ERRORES);
        System.out.println();
        System.out.println("  [i] Repita esta opcion: el fichero 1 siempre tiene las mismas lineas,");
        System.out.println("      el 2 y el de errores crecen en cada ejecucion.");
    }

    /**
     * Escribe líneas en un fichero de texto en UTF-8.
     *
     * @param fichero destino (se crea su carpeta si no existe)
     * @param lineas  texto a escribir
     * @param anadir  true = añadir al final; false = sobrescribir
     */
    private void escribirFichero(File fichero, List<String> lineas, boolean anadir) {
        File carpeta = fichero.getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }
        try (PrintWriter escritor = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(fichero, anadir), StandardCharsets.UTF_8))) {
            for (int i = 0; i < lineas.size(); i++) {
                escritor.println(lineas.get(i));
            }
        } catch (IOException e) {
            System.out.println("  [!] No se pudo escribir " + fichero.getPath() + ": " + e.getMessage());
        }
    }

    private void informarFichero(String modo, File fichero) {
        int lineas = 0;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(fichero), StandardCharsets.UTF_8))) {
            while (lector.readLine() != null) {
                lineas++;
            }
        } catch (IOException e) {
            System.out.println("  " + modo + ": no se pudo leer (" + e.getMessage() + ")");
            return;
        }
        System.out.println(String.format("  %-24s %-40s %4d lineas", modo, fichero.getPath(), lineas));
    }

    // ------------------------------------------------------------------
    // M4.5 Comunicación padre-hijo con EcoLector
    // ------------------------------------------------------------------

    /**
     * El usuario escribe, el padre lo envía a EcoLector por su entrada estándar
     * y muestra la respuesta. Termina al escribir FIN.
     */
    private void conversarConEcoLector() {
        mostrarTitulo("M4.5 Conversacion con EcoLector (escriba FIN para terminar)");
        ProcessBuilder pb = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ECO));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);   // si el hijo falla, se ve en consola
        RegistroEjecucion registro = new RegistroEjecucion("[interactivo] "
                + EjecutorProcesos.comandoResumido(pb.command()));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }

        // Padre -> hijo: getOutputStream() + PrintWriter (autoflush = true: cada println se envía ya)
        // Hijo -> padre: getInputStream() + BufferedReader
        try (PrintWriter alHijo = new PrintWriter(new OutputStreamWriter(
                     proceso.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader delHijo = new BufferedReader(new InputStreamReader(
                     proceso.getInputStream(), StandardCharsets.UTF_8))) {

            boolean seguir = true;
            while (seguir) {
                String texto = Teclado.leerTexto("  PADRE escribe > ");
                alHijo.println(texto);
                String respuesta = delHijo.readLine();
                if (respuesta == null) {
                    System.out.println("  [!] El hijo ha cerrado su salida.");
                    seguir = false;
                } else {
                    System.out.println("  HIJO responde  < " + respuesta);
                }
                if (texto.equalsIgnoreCase("FIN")) {
                    seguir = false;
                }
            }
        } catch (IOException e) {
            System.out.println("  [!] Error en la comunicacion: " + e.getMessage());
        }
        esperarFin(proceso, registro);
    }

    /**
     * El padre envía varios mensajes y después CIERRA la entrada del hijo sin
     * mandar FIN. EcoLector detecta el fin de entrada (readLine() == null) y termina.
     */
    private void enviarAutomaticoAEcoLector() {
        mostrarTitulo("M4.5 Envio automatico a EcoLector y cierre de su entrada");
        String[] mensajes = {"hola EcoLector", "programación de servicios y procesos", "año 2026: ñandú"};

        ProcessBuilder pb = new ProcessBuilder(EjecutorProcesos.crearComandoJava(CLASE_ECO));
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        RegistroEjecucion registro = new RegistroEjecucion("[automatico] "
                + EjecutorProcesos.comandoResumido(pb.command()));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }

        try (PrintWriter alHijo = new PrintWriter(new OutputStreamWriter(
                     proceso.getOutputStream(), StandardCharsets.UTF_8), true);
             BufferedReader delHijo = new BufferedReader(new InputStreamReader(
                     proceso.getInputStream(), StandardCharsets.UTF_8))) {

            for (int i = 0; i < mensajes.length; i++) {
                System.out.println("  PADRE envia    > " + mensajes[i]);
                alHijo.println(mensajes[i]);
                System.out.println("  HIJO responde  < " + delHijo.readLine());
            }

            System.out.println("  PADRE cierra la entrada del hijo (close) sin enviar FIN...");
            alHijo.close();

            // Se lee lo que quede hasta que el hijo cierre su salida (null)
            String linea;
            while ((linea = delHijo.readLine()) != null) {
                System.out.println("  HIJO responde  < " + linea);
            }
        } catch (IOException e) {
            System.out.println("  [!] Error en la comunicacion: " + e.getMessage());
        }
        esperarFin(proceso, registro);
    }

    // ------------------------------------------------------------------
    // M4.6 Herencia de consola
    // ------------------------------------------------------------------

    private void demostrarInheritIO() {
        mostrarTitulo("M4.6 Herencia de consola con inheritIO()");
        System.out.println("  Con inheritIO() el hijo usa la MISMA consola que GMM-Java: escribe");
        System.out.println("  directamente en pantalla y el padre no recibe nada por las tuberias.");

        ProcessBuilder pb = new ProcessBuilder(
                EjecutorProcesos.crearComandoJava(CLASE_AUXILIAR, "consola heredada", "0"));
        pb.inheritIO();
        RegistroEjecucion registro = new RegistroEjecucion("[inheritIO] "
                + EjecutorProcesos.comandoResumido(pb.command()));
        Process proceso = iniciar(pb, registro);
        if (proceso == null) {
            return;
        }
        System.out.println("  ---- lo siguiente lo escribe el hijo, no GMM-Java ----");
        esperarFin(proceso, registro);
        System.out.println("  ------------------------------------------------------");

        // La salida no pasa por el padre: su getInputStream() está vacío
        try (InputStream flujo = proceso.getInputStream()) {
            int primerByte = flujo.read();
            System.out.println("  [i] getInputStream().read() devuelve " + primerByte
                    + " -> el padre no ha capturado nada (-1 = flujo vacio).");
        } catch (IOException e) {
            System.out.println("  [!] " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Ayudas comunes
    // ------------------------------------------------------------------

    /**
     * Arranca el proceso y apunta el PID en el registro. Si falla, guarda el
     * registro como ERROR.
     *
     * @return el proceso o null si no se pudo iniciar
     */
    private Process iniciar(ProcessBuilder pb, RegistroEjecucion registro) {
        try {
            Process proceso = pb.start();
            registro.setPid(proceso.pid());
            System.out.println("  Proceso hijo iniciado. PID " + proceso.pid());
            return proceso;
        } catch (IOException e) {
            System.out.println("  [ERROR] No se pudo iniciar el proceso: " + e.getMessage());
            registro.finalizar(null, EstadoEjecucion.ERROR);
            HistorialEjecuciones.guardar(registro);
            return null;
        }
    }

    /**
     * Espera como máximo TIMEOUT segundos. Si el hijo no termina, se fuerza
     * su cierre. Guarda el registro en el historial.
     *
     * @return código de salida del hijo, o -1 si no se pudo obtener
     */
    private int esperarFin(Process proceso, RegistroEjecucion registro) {
        try {
            boolean terminado = proceso.waitFor(TIMEOUT, TimeUnit.SECONDS);
            if (!terminado) {
                System.out.println("  [AVISO] El hijo no ha terminado en " + TIMEOUT + " s. Se fuerza su cierre.");
                proceso.destroyForcibly();
                proceso.waitFor();
                registro.finalizar(proceso.exitValue(), EstadoEjecucion.TIMEOUT);
                HistorialEjecuciones.guardar(registro);
                return -1;
            }
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            registro.finalizar(null, EstadoEjecucion.CANCELADO);
            HistorialEjecuciones.guardar(registro);
            Thread.currentThread().interrupt();
            return -1;
        }

        int codigo = proceso.exitValue();
        if (codigo == 0) {
            registro.finalizar(codigo, EstadoEjecucion.COMPLETADO);
        } else {
            registro.finalizar(codigo, EstadoEjecucion.ERROR);
        }
        HistorialEjecuciones.guardar(registro);
        System.out.println("  Codigo de salida del hijo: " + codigo + " (" + registro.getEstado() + ")");
        return codigo;
    }

    /**
     * Lee todas las líneas de un flujo y lo cierra (try-with-resources).
     */
    private ArrayList<String> leerLineas(InputStream flujo, Charset charset) {
        ArrayList<String> lineas = new ArrayList<>();
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(flujo, charset))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                lineas.add(linea);
            }
        } catch (IOException e) {
            lineas.add("[No se pudo leer el flujo: " + e.getMessage() + "]");
        }
        return lineas;
    }

    private void mostrarLineas(String titulo, String etiqueta, List<String> lineas) {
        System.out.println("  --- " + titulo + ": " + lineas.size() + " linea(s) ---");
        for (int i = 0; i < lineas.size(); i++) {
            System.out.println("  " + etiqueta + " " + lineas.get(i));
        }
    }

    private void mostrarTitulo(String texto) {
        System.out.println();
        System.out.println("####################################################");
        System.out.println(" " + texto);
        System.out.println("####################################################");
    }
}
