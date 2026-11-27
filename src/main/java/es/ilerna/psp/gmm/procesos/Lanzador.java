package es.ilerna.psp.gmm.procesos;

import es.ilerna.psp.gmm.util.EjecutorProcesos;
import es.ilerna.psp.gmm.util.Teclado;
import es.ilerna.psp.gmm.worker.ProcesadorFichero;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Módulo 05. Procesamiento paralelo mediante Lanzador y ProcesadorFichero.
 *
 * El Lanzador NO cuenta las vocales: crea cinco procesos Java independientes
 * (uno por vocal, clase worker.ProcesadorFichero), les pasa los argumentos,
 * espera a que terminen, recoge sus códigos de salida y lee sus resultados.
 *
 * La misma carga se ejecuta de dos formas para comparar tiempos (M5.4):
 *  - Secuencial: lanzar A y esperar, lanzar E y esperar, ...
 *  - Concurrente: lanzar A, E, I, O, U y DESPUÉS esperar a todos.
 *
 * Para comprobar que los procesos han contado bien, el Lanzador hace además
 * un recuento secuencial de referencia (solo para verificar, no para el trabajo).
 */
public class Lanzador {

    private static final String CLASE_PROCESADOR = "es.ilerna.psp.gmm.worker.ProcesadorFichero";
    private static final String[] VOCALES = {"A", "E", "I", "O", "U"};

    private static final File CARPETA_RESULTADOS = new File("resultados");
    private static final File FICHERO_POR_DEFECTO = new File("datos", "texto_prueba.txt");
    private static final File FICHERO_GRANDE = new File("datos", "texto_grande.txt");

    /** Segundos máximos que se espera a cada proceso antes de finalizarlo. */
    private static final int TIMEOUT_SEGUNDOS = 120;

    private static final String LINEA = "====================================================";
    private static final String LINEA_FINA = "----------------------------------------------------";

    /**
     * Menú del módulo. Se repite hasta que el usuario elige volver.
     */
    public void mostrarMenu() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println(LINEA);
            System.out.println(" MODULO 05. PROCESAMIENTO PARALELO DE FICHEROS");
            System.out.println(LINEA);
            System.out.println("[1] Analizar el fichero de prueba (" + FICHERO_POR_DEFECTO.getPath() + ")");
            System.out.println("[2] Analizar otro fichero (indicar ruta)");
            System.out.println("[3] Generar un fichero grande y analizarlo");
            System.out.println("[4] Prueba de error controlado (vocal no valida -> Errores_X.txt)");
            System.out.println("[0] Volver al menu principal");
            System.out.println();

            opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 4);
            System.out.println();

            switch (opcion) {
                case 1:
                    analizar(FICHERO_POR_DEFECTO);
                    break;
                case 2:
                    String ruta = Teclado.leerTexto("Ruta del fichero de texto (UTF-8): ");
                    analizar(new File(ruta));
                    break;
                case 3:
                    int megas = Teclado.leerEntero("Tamano aproximado en MB (5-200): ", 5, 200);
                    if (generarFicheroGrande(megas)) {
                        analizar(FICHERO_GRANDE);
                    }
                    break;
                case 4:
                    probarErrorControlado();
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
    // Análisis completo: validar, referencia, secuencial, concurrente, resumen
    // ------------------------------------------------------------------

    /**
     * Hace todo el trabajo del módulo sobre un fichero.
     *
     * @param fichero fichero de texto que se va a analizar
     * @return true si la operación terminó COMPLETADA (todo correcto y el recuento coincide)
     */
    public boolean analizar(File fichero) {
        // 1) Validar el fichero antes de lanzar nada
        if (!validarFichero(fichero)) {
            return false;
        }
        if (!CARPETA_RESULTADOS.exists()) {
            CARPETA_RESULTADOS.mkdirs();
        }

        System.out.println(LINEA);
        System.out.println(" Fichero: " + fichero.getPath() + " (" + formatearTamano(fichero.length()) + ")");
        System.out.println(" Nucleos disponibles: " + Runtime.getRuntime().availableProcessors());
        System.out.println(LINEA);

        // 2) Recuento de referencia (lo hace este programa, de forma secuencial)
        long[] referencia;
        try {
            referencia = recuentoReferencia(fichero);
        } catch (IOException e) {
            System.out.println("  [!] No se pudo leer el fichero: " + e.getMessage());
            return false;
        }

        // 3) Modalidad secuencial
        System.out.println();
        System.out.println("--- Ejecucion SECUENCIAL (lanzar y esperar uno a uno) ---");
        ArrayList<ProcesoVocal> secuencial = crearProcesos();
        long inicio = System.nanoTime();
        ejecutarSecuencial(secuencial, fichero);
        long tiempoSecuencial = System.nanoTime() - inicio;
        mostrarTablaProcesos(secuencial, referencia);

        // 4) Modalidad concurrente
        System.out.println();
        System.out.println("--- Ejecucion CONCURRENTE (lanzar los 5 y despues esperar) ---");
        ArrayList<ProcesoVocal> concurrente = crearProcesos();
        inicio = System.nanoTime();
        ejecutarConcurrente(concurrente, fichero);
        long tiempoConcurrente = System.nanoTime() - inicio;
        mostrarTablaProcesos(concurrente, referencia);

        // 5) Resumen final con el formato del enunciado
        return mostrarResumen(fichero, secuencial, concurrente, referencia, tiempoSecuencial, tiempoConcurrente);
    }

    /**
     * Comprueba que el fichero existe, es un fichero normal, se puede leer y no está vacío.
     */
    private boolean validarFichero(File fichero) {
        if (fichero.getPath().isEmpty()) {
            System.out.println("  [!] No se ha indicado ninguna ruta.");
            return false;
        }
        if (!fichero.exists()) {
            System.out.println("  [!] El fichero '" + fichero.getPath() + "' no existe.");
            System.out.println("      Directorio actual: " + new File(".").getAbsoluteFile().getParent());
            return false;
        }
        if (!fichero.isFile()) {
            System.out.println("  [!] '" + fichero.getPath() + "' no es un fichero (es una carpeta).");
            return false;
        }
        if (!fichero.canRead()) {
            System.out.println("  [!] No hay permiso de lectura sobre '" + fichero.getPath() + "'.");
            return false;
        }
        if (fichero.length() == 0) {
            System.out.println("  [!] El fichero esta vacio; no hay nada que contar.");
            return false;
        }
        return true;
    }

    private ArrayList<ProcesoVocal> crearProcesos() {
        ArrayList<ProcesoVocal> lista = new ArrayList<>();
        for (int i = 0; i < VOCALES.length; i++) {
            lista.add(new ProcesoVocal(VOCALES[i], CARPETA_RESULTADOS));
        }
        return lista;
    }

    /**
     * Secuencial: cada proceso se lanza y se espera ANTES de lanzar el siguiente.
     * Nunca hay dos procesos trabajando a la vez.
     */
    private void ejecutarSecuencial(ArrayList<ProcesoVocal> procesos, File fichero) {
        for (int i = 0; i < procesos.size(); i++) {
            ProcesoVocal pv = procesos.get(i);
            lanzar(pv, fichero);
            esperar(pv);
        }
    }

    /**
     * Concurrente: PRIMERO se lanzan los cinco procesos y DESPUÉS se espera a
     * cada uno. Mientras se espera al de la A, los demás ya están trabajando.
     */
    private void ejecutarConcurrente(ArrayList<ProcesoVocal> procesos, File fichero) {
        for (int i = 0; i < procesos.size(); i++) {
            lanzar(procesos.get(i), fichero);
        }
        for (int i = 0; i < procesos.size(); i++) {
            esperar(procesos.get(i));
        }
    }

    /**
     * Construye el comando y arranca UN proceso ProcesadorFichero.
     *  - La salida estándar del hijo se descarta (no escribe nada por pantalla).
     *  - La salida de error se redirige a resultados/Errores_X.txt.
     */
    private void lanzar(ProcesoVocal pv, File fichero) {
        // Se borra el resultado anterior para no leer por error un fichero antiguo
        if (pv.getFicheroResultado().exists()) {
            pv.getFicheroResultado().delete();
        }

        ArrayList<String> comando = EjecutorProcesos.crearComandoJava(CLASE_PROCESADOR,
                fichero.getAbsolutePath(), pv.getVocal(), pv.getFicheroResultado().getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(pv.getFicheroErrores()); // sobrescribe el fichero de errores anterior

        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(comando));
        pv.setRegistro(registro);
        pv.setInicioNanos(System.nanoTime());
        try {
            Process proceso = pb.start();
            pv.setProceso(proceso);
            registro.setPid(proceso.pid());
        } catch (IOException e) {
            pv.setEstado("No iniciado");
            pv.setFinNanos(System.nanoTime());
            registro.finalizar(null, EstadoEjecucion.ERROR);
            HistorialEjecuciones.guardar(registro);
            System.out.println("  [!] No se pudo lanzar el proceso de la " + pv.getVocal() + ": " + e.getMessage());
        }
    }

    /**
     * Espera a que termine UN proceso, guarda su código de salida y lee su resultado.
     */
    private void esperar(ProcesoVocal pv) {
        Process proceso = pv.getProceso();
        if (proceso == null) {
            return; // no llegó a arrancar; ya se informó en lanzar()
        }
        RegistroEjecucion registro = pv.getRegistro();
        try {
            boolean terminado = proceso.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);
            pv.setFinNanos(System.nanoTime());
            if (!terminado) {
                proceso.destroyForcibly();
                pv.setEstado("Timeout");
                registro.finalizar(null, EstadoEjecucion.TIMEOUT);
                HistorialEjecuciones.guardar(registro);
                return;
            }
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            Thread.currentThread().interrupt();
            pv.setFinNanos(System.nanoTime());
            pv.setEstado("Cancelado");
            registro.finalizar(null, EstadoEjecucion.CANCELADO);
            HistorialEjecuciones.guardar(registro);
            return;
        }

        int codigo = proceso.exitValue();
        pv.setCodigoSalida(codigo);
        if (codigo == ProcesadorFichero.OK) {
            pv.setApariciones(leerApariciones(pv.getFicheroResultado()));
        }

        if (pv.esCorrecto()) {
            pv.setEstado("Finalizado");
            registro.finalizar(codigo, EstadoEjecucion.COMPLETADO);
        } else {
            pv.setEstado("Error (ver " + pv.getFicheroErrores().getName() + ")");
            registro.finalizar(codigo, EstadoEjecucion.ERROR);
        }
        HistorialEjecuciones.guardar(registro);
    }

    /**
     * Busca la línea "Apariciones: N" en el fichero de resultado.
     *
     * @return el número leído o -1 si no se encuentra
     */
    private long leerApariciones(File resultado) {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(resultado), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.startsWith(ProcesadorFichero.ETIQUETA_APARICIONES)) {
                    String numero = linea.substring(ProcesadorFichero.ETIQUETA_APARICIONES.length()).trim();
                    return Long.parseLong(numero);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("  [!] No se pudo leer " + resultado.getName() + ": " + e.getMessage());
        }
        return -1;
    }

    /**
     * Recuento de referencia: lee el fichero UNA vez y cuenta las cinco vocales a la vez.
     * Solo sirve para verificar que los procesos han contado bien.
     *
     * @return array con 5 posiciones (A, E, I, O, U)
     */
    private long[] recuentoReferencia(File fichero) throws IOException {
        long[] contadores = new long[VOCALES.length];
        String vocales = "aeiou";
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(fichero), StandardCharsets.UTF_8))) {
            int caracter;
            while ((caracter = lector.read()) != -1) {
                int posicion = vocales.indexOf(Character.toLowerCase((char) caracter));
                if (posicion >= 0) {
                    contadores[posicion]++;
                }
            }
        }
        return contadores;
    }

    // ------------------------------------------------------------------
    // Presentación de resultados
    // ------------------------------------------------------------------

    private void mostrarTablaProcesos(ArrayList<ProcesoVocal> procesos, long[] referencia) {
        String formato = "  %-6s %-7s %-7s %12s %12s %9s  %s";
        System.out.println(String.format(formato, "VOCAL", "PID", "CODIGO", "APARICIONES", "REFERENCIA", "TIEMPO", "ESTADO"));
        for (int i = 0; i < procesos.size(); i++) {
            ProcesoVocal pv = procesos.get(i);
            String apariciones = "-";
            if (pv.getApariciones() >= 0) {
                apariciones = String.valueOf(pv.getApariciones());
            }
            System.out.println(String.format(formato, pv.getVocal(), pv.getPidTexto(), pv.getCodigoTexto(),
                    apariciones, String.valueOf(referencia[i]), pv.getDuracionMs() + " ms", pv.getEstado()));
        }
    }

    /**
     * Resumen final con el formato del enunciado, verificación y explicación.
     *
     * @return true si la operación está COMPLETADA
     */
    private boolean mostrarResumen(File fichero, ArrayList<ProcesoVocal> secuencial,
                                   ArrayList<ProcesoVocal> concurrente, long[] referencia,
                                   long nanosSecuencial, long nanosConcurrente) {
        // Comprobaciones
        boolean todosCorrectos = true;
        boolean coincide = true;
        long totalReferencia = 0;
        long totalSecuencial = 0;
        long totalConcurrente = 0;
        for (int i = 0; i < VOCALES.length; i++) {
            ProcesoVocal s = secuencial.get(i);
            ProcesoVocal c = concurrente.get(i);
            if (!s.esCorrecto() || !c.esCorrecto()) {
                todosCorrectos = false;
            }
            if (s.getApariciones() != referencia[i] || c.getApariciones() != referencia[i]) {
                coincide = false;
            }
            totalReferencia += referencia[i];
            totalSecuencial += s.getApariciones();
            totalConcurrente += c.getApariciones();
        }

        System.out.println();
        System.out.println(LINEA);
        System.out.println("ANALISIS DEL FICHERO");
        System.out.println();
        System.out.println("Fichero: " + fichero.getName());
        System.out.println("Procesos lanzados: " + VOCALES.length + " (en cada modalidad)");
        System.out.println();
        for (int i = 0; i < concurrente.size(); i++) {
            ProcesoVocal pv = concurrente.get(i);
            System.out.println(pv.getVocal() + " -> " + pv.getFicheroResultado().getName() + " | " + pv.getEstado());
        }
        System.out.println();
        System.out.println("Tiempo secuencial: " + formatearMs(nanosSecuencial));
        System.out.println("Tiempo concurrente: " + formatearMs(nanosConcurrente));
        System.out.println();

        // Verificación contra la referencia
        System.out.println("Total de vocales (referencia secuencial): " + totalReferencia);
        System.out.println("Total de vocales (procesos secuenciales): " + totalSecuencial);
        System.out.println("Total de vocales (procesos concurrentes):  " + totalConcurrente);
        if (coincide) {
            System.out.println("Verificacion: el recuento de los procesos COINCIDE con la referencia.");
        } else {
            System.out.println("Verificacion: el recuento NO coincide con la referencia.");
        }
        System.out.println();

        String estado;
        if (todosCorrectos && coincide) {
            estado = "COMPLETADA";
        } else if (!todosCorrectos) {
            estado = "CON ERRORES (revise los ficheros Errores_X.txt)";
        } else {
            estado = "INCORRECTA (el recuento no coincide)";
        }
        System.out.println("Estado de la operacion: " + estado);
        System.out.println(LINEA);

        explicarTiempos(fichero, nanosSecuencial, nanosConcurrente);
        return todosCorrectos && coincide;
    }

    /**
     * M5.4. Explica el resultado medido y por qué la concurrencia no siempre gana.
     */
    private void explicarTiempos(File fichero, long nanosSecuencial, long nanosConcurrente) {
        System.out.println();
        System.out.println(LINEA_FINA);
        System.out.println(" Por que el concurrente no siempre es mas rapido");
        System.out.println(LINEA_FINA);
        if (nanosConcurrente < nanosSecuencial) {
            double veces = (double) nanosSecuencial / nanosConcurrente;
            System.out.println(String.format(" En esta ejecucion el concurrente ha sido %.2f veces mas rapido.", veces));
            System.out.println(" Los 5 procesos se han solapado en el tiempo usando varios nucleos.");
        } else {
            System.out.println(" En esta ejecucion el concurrente NO ha sido mas rapido.");
        }
        System.out.println(" Aun asi, nunca sera 5 veces mas rapido, porque:");
        System.out.println("  1. Crear un proceso cuesta: cada hijo arranca su propia JVM (memoria,");
        System.out.println("     carga de clases...). Con un fichero pequeno ese arranque pesa mas");
        System.out.println("     que el propio recuento (este fichero ocupa " + formatearTamano(fichero.length()) + ").");
        System.out.println("  2. Tamano del fichero: cuanto mas grande, mas trabajo util hay que");
        System.out.println("     repartir y mas se nota la ganancia (pruebe la opcion [3]).");
        System.out.println("  3. Recursos disponibles: hay " + Runtime.getRuntime().availableProcessors()
                + " nucleos. Si hay menos nucleos que procesos, o");
        System.out.println("     el equipo esta ocupado, los procesos esperan turno de CPU.");
        System.out.println("  4. Los 5 procesos leen el MISMO fichero: compiten por el disco y la");
        System.out.println("     memoria. La primera lectura puede ir mas lenta (fichero aun no en cache).");
    }

    // ------------------------------------------------------------------
    // Opciones auxiliares del menú
    // ------------------------------------------------------------------

    /**
     * Crea datos/texto_grande.txt repitiendo el contenido de texto_prueba.txt
     * hasta llegar al tamaño pedido. Sirve para ver cómo influye el tamaño.
     */
    private boolean generarFicheroGrande(int megas) {
        if (!FICHERO_POR_DEFECTO.isFile()) {
            System.out.println("  [!] No existe " + FICHERO_POR_DEFECTO.getPath() + " para usarlo como base.");
            return false;
        }
        long objetivo = (long) megas * 1024 * 1024;
        System.out.println("Generando " + FICHERO_GRANDE.getPath() + " (" + megas + " MB)...");
        try {
            byte[] base = leerBytes(FICHERO_POR_DEFECTO);
            long escritos = 0;
            try (OutputStream salida = new FileOutputStream(FICHERO_GRANDE)) {
                while (escritos < objetivo) {
                    salida.write(base);
                    escritos += base.length;
                }
            }
            System.out.println("Fichero generado: " + formatearTamano(FICHERO_GRANDE.length()));
            System.out.println();
            return true;
        } catch (IOException e) {
            System.out.println("  [!] No se pudo generar el fichero: " + e.getMessage());
            return false;
        }
    }

    private byte[] leerBytes(File fichero) throws IOException {
        try (FileInputStream entrada = new FileInputStream(fichero)) {
            return entrada.readAllBytes();
        }
    }

    /**
     * Lanza un ProcesadorFichero con la vocal "X" para comprobar que el hijo
     * termina con código 1 y que su mensaje queda guardado en Errores_X.txt.
     */
    private void probarErrorControlado() {
        if (!CARPETA_RESULTADOS.exists()) {
            CARPETA_RESULTADOS.mkdirs();
        }
        ProcesoVocal pv = new ProcesoVocal("X", CARPETA_RESULTADOS);
        System.out.println("Se lanza ProcesadorFichero con la vocal 'X' (no valida)...");
        lanzar(pv, FICHERO_POR_DEFECTO);
        esperar(pv);
        System.out.println("  Codigo de salida: " + pv.getCodigoTexto() + " (1 = argumentos incorrectos)");
        System.out.println("  Estado: " + pv.getEstado());
        System.out.println("  Contenido de " + pv.getFicheroErrores().getPath() + ":");
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(pv.getFicheroErrores()), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                System.out.println("    " + linea);
            }
        } catch (IOException e) {
            System.out.println("  [!] No se pudo leer el fichero de errores: " + e.getMessage());
        }
    }

    private String formatearMs(long nanos) {
        return String.format("%.1f ms", nanos / 1_000_000.0);
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
