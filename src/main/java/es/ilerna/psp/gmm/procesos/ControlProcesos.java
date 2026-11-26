package es.ilerna.psp.gmm.procesos;

import es.ilerna.psp.gmm.util.EjecutorProcesos;
import es.ilerna.psp.gmm.util.SistemaOperativo;
import es.ilerna.psp.gmm.util.Teclado;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Módulo 03. Control del ciclo de vida de los procesos.
 *
 * - M3.1 Batería de 7 ejecuciones: correctas, ejecutable inexistente,
 *        código de salida distinto de 0, timeout y clase Java con System.exit(n).
 * - M3.2 Tres formas de esperar: waitFor(), waitFor(timeout, unidad) y
 *        comprobar isAlive() antes de pedir exitValue().
 * - M3.3 Cancelación por timeout: avisar, destroy(), destroyForcibly() si
 *        sigue vivo y confirmar que ha terminado.
 * - M3.4 Cada ejecución se guarda en el historial (opción [8] y logs/).
 *
 * La salida de los hijos se envía directamente a la consola (Redirect.INHERIT).
 * Así el padre puede esperar con timeout sin tener que leer ningún flujo y sin
 * riesgo de que el hijo se quede bloqueado con la tubería llena.
 */
public class ControlProcesos {

    private static final String CLASE_AUXILIAR = "es.ilerna.psp.gmm.worker.ProgramaAuxiliar";
    private static final String CLASE_LENTA = "es.ilerna.psp.gmm.worker.ProcesoLento";

    /** Tiempo máximo normal para cada proceso de la batería. */
    private static final int TIMEOUT_NORMAL = 10;
    /** Tiempo máximo corto para provocar el caso de timeout. */
    private static final int TIMEOUT_CORTO = 3;
    /** Segundos que se le dan al proceso para cerrarse tras destroy(). */
    private static final int GRACIA_TRAS_DESTROY = 2;
    /** Indica "esperar sin límite" con waitFor(). */
    private static final int SIN_LIMITE = 0;

    /**
     * Menú del módulo. Se repite hasta que el usuario elige volver.
     */
    public void mostrarMenu() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println("====================================================");
            System.out.println(" MODULO 03. CONTROL DEL CICLO DE VIDA DE PROCESOS");
            System.out.println("====================================================");
            System.out.println("[1] Bateria de 7 ejecuciones (M3.1)");
            System.out.println("[2] Espera completa con waitFor() (M3.2)");
            System.out.println("[3] Espera limitada con waitFor(timeout) (M3.2)");
            System.out.println("[4] Comprobar fin antes de exitValue() (M3.2)");
            System.out.println("[5] Cancelacion por timeout paso a paso (M3.3)");
            System.out.println("[6] Cancelacion manual por el usuario");
            System.out.println("[0] Volver al menu principal");
            System.out.println();

            opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 6);
            System.out.println();

            switch (opcion) {
                case 1:
                    ejecutarBateria();
                    break;
                case 2:
                    ejecutar("waitFor() sin limite: el padre espera lo que haga falta",
                            EjecutorProcesos.crearComandoJava(CLASE_LENTA, "3"), SIN_LIMITE);
                    break;
                case 3:
                    ejecutar("waitFor(" + TIMEOUT_NORMAL + ", SECONDS): el proceso acaba antes del limite",
                            EjecutorProcesos.crearComandoJava(CLASE_LENTA, "2"), TIMEOUT_NORMAL);
                    break;
                case 4:
                    demostrarExitValue();
                    break;
                case 5:
                    ejecutar("Proceso de 30 s con limite de " + TIMEOUT_CORTO + " s",
                            EjecutorProcesos.crearComandoJava(CLASE_LENTA, "30"), TIMEOUT_CORTO);
                    break;
                case 6:
                    demostrarCancelacionManual();
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
    // M3.1 Batería de ejecuciones
    // ------------------------------------------------------------------

    private void ejecutarBateria() {
        ArrayList<RegistroEjecucion> resultados = new ArrayList<>();

        resultados.add(ejecutar("1/7 Proceso correcto: nombre del equipo",
                EjecutorProcesos.crearComando("hostname"), TIMEOUT_NORMAL));
        resultados.add(ejecutar("2/7 Proceso correcto: version de Java",
                EjecutorProcesos.crearComando(EjecutorProcesos.getRutaJava(), "-version"), TIMEOUT_NORMAL));
        resultados.add(ejecutar("3/7 Clase Java auxiliar que termina con System.exit(0)",
                EjecutorProcesos.crearComandoJava(CLASE_AUXILIAR, "bateria", "0"), TIMEOUT_NORMAL));
        resultados.add(ejecutar("4/7 Ejecutable inexistente",
                EjecutorProcesos.crearComando("programa_inexistente_gmm"), TIMEOUT_NORMAL));
        resultados.add(ejecutar("5/7 Comando del sistema con codigo de salida distinto de 0",
                comandoListarCarpetaInexistente(), TIMEOUT_NORMAL));
        resultados.add(ejecutar("6/7 Clase Java auxiliar que termina con System.exit(4)",
                EjecutorProcesos.crearComandoJava(CLASE_AUXILIAR, "bateria", "4"), TIMEOUT_NORMAL));
        resultados.add(ejecutar("7/7 Proceso que supera el tiempo maximo (" + TIMEOUT_CORTO + " s)",
                EjecutorProcesos.crearComandoJava(CLASE_LENTA, "30"), TIMEOUT_CORTO));

        System.out.println();
        System.out.println("=============== RESUMEN DE LA BATERIA ===============");
        for (int i = 0; i < resultados.size(); i++) {
            RegistroEjecucion r = resultados.get(i);
            System.out.println(String.format("  %d. %-10s codigo %-4s %6d ms  %s",
                    i + 1, r.getEstado(), r.getCodigoTexto(), r.getDuracionMs(), r.getComando()));
        }
        System.out.println("  (todo queda guardado en la opcion [8] del menu principal)");
    }

    /**
     * "dir" / "ls" de una carpeta que no existe: el programa arranca bien
     * pero termina con un código distinto de 0.
     */
    private ArrayList<String> comandoListarCarpetaInexistente() {
        if (SistemaOperativo.esWindows()) {
            return EjecutorProcesos.crearComando("cmd.exe", "/c", "dir", "carpeta_inexistente_gmm");
        }
        return EjecutorProcesos.crearComando("ls", "carpeta_inexistente_gmm");
    }

    // ------------------------------------------------------------------
    // Ejecución supervisada (común a todas las opciones)
    // ------------------------------------------------------------------

    /**
     * Lanza un proceso, espera según el límite indicado, lo guarda en el
     * historial y muestra el resultado.
     *
     * @param descripcion      texto que explica la prueba
     * @param comando          programa y argumentos
     * @param timeoutSegundos  segundos máximos de espera; SIN_LIMITE (0) usa waitFor()
     * @return registro con los datos de la ejecución
     */
    private RegistroEjecucion ejecutar(String descripcion, ArrayList<String> comando, int timeoutSegundos) {
        mostrarTitulo(descripcion);
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(comando));
        System.out.println("  Comando : " + registro.getComando());

        Process proceso = lanzar(comando, registro);
        if (proceso != null) {
            if (timeoutSegundos == SIN_LIMITE) {
                esperarSinLimite(proceso, registro);
            } else {
                esperarConLimite(proceso, registro, timeoutSegundos);
            }
        }

        HistorialEjecuciones.guardar(registro);
        mostrarResultado(registro);
        return registro;
    }

    /**
     * Arranca el proceso. Si no puede arrancar, deja el registro en estado ERROR.
     *
     * @return el proceso en marcha o null si no se pudo iniciar
     */
    private Process lanzar(ArrayList<String> comando, RegistroEjecucion registro) {
        ProcessBuilder pb = new ProcessBuilder(comando);
        // La salida y los errores del hijo van directamente a nuestra consola.
        // La entrada NO se hereda para que el hijo no compita con nuestro teclado.
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            Process proceso = pb.start();
            registro.setPid(proceso.pid());
            System.out.println("  PID     : " + proceso.pid() + "  (proceso iniciado)");
            System.out.println("  ------------- salida del proceso hijo -------------");
            return proceso;
        } catch (IOException e) {
            System.out.println("  [ERROR] No se ha podido iniciar el proceso: " + e.getMessage());
            registro.finalizar(null, EstadoEjecucion.ERROR);
            return null;
        }
    }

    /**
     * M3.2 Espera sin límite: waitFor() bloquea hasta que el hijo termina.
     */
    private void esperarSinLimite(Process proceso, RegistroEjecucion registro) {
        try {
            System.out.println("  [i] waitFor(): espero sin limite de tiempo...");
            int codigo = proceso.waitFor();
            registro.finalizar(codigo, estadoSegunCodigo(codigo));
        } catch (InterruptedException e) {
            detenerProceso(proceso);
            registro.finalizar(null, EstadoEjecucion.CANCELADO);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * M3.2 Espera limitada: waitFor(timeout, unidad) devuelve true si el hijo
     * ha terminado a tiempo y false si sigue vivo al acabar el plazo.
     */
    private void esperarConLimite(Process proceso, RegistroEjecucion registro, int timeoutSegundos) {
        try {
            System.out.println("  [i] waitFor(" + timeoutSegundos + ", SECONDS): espero como maximo "
                    + timeoutSegundos + " s...");
            boolean terminado = proceso.waitFor(timeoutSegundos, TimeUnit.SECONDS);

            if (terminado) {
                // Ya ha terminado, así que exitValue() no lanza excepción
                int codigo = proceso.exitValue();
                registro.finalizar(codigo, estadoSegunCodigo(codigo));
            } else {
                System.out.println("  ---------------------------------------------------");
                System.out.println("  [AVISO] El proceso PID " + proceso.pid() + " sigue ejecutandose tras "
                        + timeoutSegundos + " s. Se ha superado el tiempo maximo.");
                detenerProceso(proceso);
                registro.finalizar(codigoSiExiste(proceso), EstadoEjecucion.TIMEOUT);
            }
        } catch (InterruptedException e) {
            detenerProceso(proceso);
            registro.finalizar(null, EstadoEjecucion.CANCELADO);
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------
    // M3.3 Cancelación: destroy() -> destroyForcibly() -> confirmar
    // ------------------------------------------------------------------

    /**
     * Detiene un proceso paso a paso:
     *  1) destroy(): pide al proceso que termine (en Linux envía SIGTERM).
     *  2) Espera unos segundos a que se cierre.
     *  3) Si sigue vivo, destroyForcibly(): lo mata sin preguntar (SIGKILL).
     *  4) waitFor() para confirmar que ya no existe.
     */
    private void detenerProceso(Process proceso) {
        try {
            System.out.println("  Paso 1: destroy() -> se pide al proceso que termine.");
            if (!proceso.supportsNormalTermination()) {
                System.out.println("          (en este sistema destroy() ya es una terminacion forzosa)");
            }
            proceso.destroy();

            System.out.println("  Paso 2: espero " + GRACIA_TRAS_DESTROY + " s a que se cierre...");
            boolean terminado = proceso.waitFor(GRACIA_TRAS_DESTROY, TimeUnit.SECONDS);

            if (terminado) {
                System.out.println("  Paso 3: no hace falta destroyForcibly(), ya ha terminado.");
            } else {
                System.out.println("  Paso 3: sigue vivo -> destroyForcibly() (terminacion forzosa).");
                proceso.destroyForcibly();
                proceso.waitFor();
            }
        } catch (InterruptedException e) {
            proceso.destroyForcibly();
            Thread.currentThread().interrupt();
        }
        System.out.println("  Paso 4: confirmacion -> isAlive() = " + proceso.isAlive());
    }

    // ------------------------------------------------------------------
    // M3.2 Comprobación antes de exitValue()
    // ------------------------------------------------------------------

    /**
     * exitValue() lanza IllegalThreadStateException si el proceso sigue
     * vivo. Por eso primero se comprueba con isAlive().
     */
    private void demostrarExitValue() {
        mostrarTitulo("Comprobar que el proceso ha terminado antes de usar exitValue()");
        ArrayList<String> comando = EjecutorProcesos.crearComandoJava(CLASE_LENTA, "3");
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(comando));
        System.out.println("  Comando : " + registro.getComando());
        Process proceso = lanzar(comando, registro);
        if (proceso == null) {
            HistorialEjecuciones.guardar(registro);
            return;
        }

        // 1) Lo que NO hay que hacer: pedir el código con el proceso en marcha
        try {
            int codigo = proceso.exitValue();
            System.out.println("  [i] El proceso ya habia terminado con codigo " + codigo);
        } catch (IllegalThreadStateException e) {
            System.out.println("  [!] exitValue() con el proceso vivo lanza IllegalThreadStateException.");
        }

        // 2) Lo correcto: comprobar isAlive() y esperar hasta que termine
        try {
            while (proceso.isAlive()) {
                System.out.println("  [i] isAlive() = true -> todavia no puedo pedir exitValue(). Espero 1 s.");
                proceso.waitFor(1, TimeUnit.SECONDS);
            }
            System.out.println("  [i] isAlive() = false -> ahora si puedo llamar a exitValue().");
            int codigo = proceso.exitValue();
            registro.finalizar(codigo, estadoSegunCodigo(codigo));
        } catch (InterruptedException e) {
            detenerProceso(proceso);
            registro.finalizar(null, EstadoEjecucion.CANCELADO);
            Thread.currentThread().interrupt();
        }

        HistorialEjecuciones.guardar(registro);
        mostrarResultado(registro);
    }

    // ------------------------------------------------------------------
    // Cancelación manual (estado CANCELADO)
    // ------------------------------------------------------------------

    /**
     * Lanza un proceso largo y lo cancela cuando el usuario pulsa ENTER.
     */
    private void demostrarCancelacionManual() {
        mostrarTitulo("Cancelacion manual: proceso de 60 s");
        ArrayList<String> comando = EjecutorProcesos.crearComandoJava(CLASE_LENTA, "60");
        RegistroEjecucion registro = new RegistroEjecucion(EjecutorProcesos.comandoResumido(comando));
        System.out.println("  Comando : " + registro.getComando());
        Process proceso = lanzar(comando, registro);
        if (proceso == null) {
            HistorialEjecuciones.guardar(registro);
            return;
        }

        Teclado.leerTexto("  >>> Pulse ENTER cuando quiera CANCELAR el proceso <<<\n");

        if (proceso.isAlive()) {
            System.out.println("  [AVISO] El usuario cancela el proceso PID " + proceso.pid() + ".");
            detenerProceso(proceso);
            registro.finalizar(codigoSiExiste(proceso), EstadoEjecucion.CANCELADO);
        } else {
            // Terminó antes de que el usuario pulsara ENTER
            int codigo = proceso.exitValue();
            registro.finalizar(codigo, estadoSegunCodigo(codigo));
        }

        HistorialEjecuciones.guardar(registro);
        mostrarResultado(registro);
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private EstadoEjecucion estadoSegunCodigo(int codigo) {
        if (codigo == 0) {
            return EstadoEjecucion.COMPLETADO;
        }
        return EstadoEjecucion.ERROR;
    }

    /**
     * @return el código de salida si el proceso ha terminado; null si sigue vivo
     */
    private Integer codigoSiExiste(Process proceso) {
        if (proceso.isAlive()) {
            return null;
        }
        return proceso.exitValue();
    }

    private void mostrarTitulo(String texto) {
        System.out.println();
        System.out.println("####################################################");
        System.out.println(" " + texto);
        System.out.println("####################################################");
    }

    private void mostrarResultado(RegistroEjecucion r) {
        System.out.println("  ---------------------------------------------------");
        System.out.println("  RESULTADO -> estado " + r.getEstado()
                + " | codigo " + r.getCodigoTexto()
                + " | PID " + r.getPidTexto()
                + " | " + r.getInicioTexto() + " -> " + r.getFinTexto()
                + " (" + r.getDuracionMs() + " ms)");
    }
}
