package es.ilerna.psp.gmm.procesos;

import es.ilerna.psp.gmm.util.EjecutorProcesos;
import es.ilerna.psp.gmm.util.ResultadoProceso;
import es.ilerna.psp.gmm.util.SistemaOperativo;
import es.ilerna.psp.gmm.util.Teclado;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Módulo 02. Ejecutor de comandos multiplataforma.
 *
 * - M2.1 El usuario elige una operación de un menú cerrado (no puede escribir
 *        comandos libres) y se ejecuta la versión de Windows o de Linux.
 * - M2.2 Los comandos se construyen como una lista: cada argumento va en su
 *        propia posición, sin concatenar textos.
 * - M2.3 Se puede elegir el directorio de trabajo del proceso hijo con
 *        ProcessBuilder.directory(), comprobando antes que existe y es accesible.
 * - M2.4 Se distinguen tres tipos de error:
 *        TIPO 1: el programa no puede iniciarse (start() lanza IOException).
 *        TIPO 2: el proceso arranca pero termina con código distinto de 0.
 *        TIPO 3: el comando no está disponible en este sistema operativo.
 */
public class EjecutorMultiplataforma {

    private static final String CLASE_AUXILIAR = "es.ilerna.psp.gmm.worker.ProgramaAuxiliar";

    /** Directorio donde se ejecutarán los procesos hijos. Por defecto, el actual. */
    private File directorioTrabajo;

    public EjecutorMultiplataforma() {
        directorioTrabajo = new File(System.getProperty("user.dir")).getAbsoluteFile();
    }

    /**
     * Menú del módulo. Se repite hasta que el usuario elige volver.
     */
    public void mostrarMenu() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println("====================================================");
            System.out.println(" MODULO 02. EJECUTOR MULTIPLATAFORMA DE COMANDOS");
            System.out.println("====================================================");
            System.out.println(" Sistema: " + SistemaOperativo.getTipo());
            System.out.println(" Directorio de trabajo: " + directorioTrabajo.getPath());
            System.out.println("----------------------------------------------------");
            System.out.println("[1] Listar archivos");
            System.out.println("[2] Mostrar informacion de red");
            System.out.println("[3] Obtener nombre del equipo");
            System.out.println("[4] Consultar version de Java");
            System.out.println("[5] Ejecutar una clase Java auxiliar");
            System.out.println("[6] Cambiar directorio de trabajo");
            System.out.println("[7] Demostracion de los 3 tipos de error");
            System.out.println("[0] Volver al menu principal");
            System.out.println();

            opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 7);
            System.out.println();

            switch (opcion) {
                case 1:
                    ejecutarYMostrar("Listar archivos", comandoListarArchivos());
                    break;
                case 2:
                    ejecutarYMostrar("Mostrar informacion de red", comandoInformacionRed());
                    break;
                case 3:
                    ejecutarYMostrar("Obtener nombre del equipo", comandoNombreEquipo());
                    break;
                case 4:
                    ejecutarYMostrar("Consultar version de Java", comandoVersionJava());
                    break;
                case 5:
                    ejecutarClaseAuxiliar();
                    break;
                case 6:
                    cambiarDirectorioTrabajo();
                    break;
                case 7:
                    demostrarErrores();
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
    // M2.1 / M2.2 Construcción de los comandos (uno por operación y SO)
    // Devuelven null si el sistema operativo no está soportado.
    // ------------------------------------------------------------------

    private ArrayList<String> comandoListarArchivos() {
        if (SistemaOperativo.esWindows()) {
            // "dir" no es un programa, es una orden interna de cmd.exe
            return EjecutorProcesos.crearComando("cmd.exe", "/c", "dir");
        } else if (SistemaOperativo.esLinux()) {
            return EjecutorProcesos.crearComando("ls", "-la");
        }
        return null;
    }

    private ArrayList<String> comandoInformacionRed() {
        if (SistemaOperativo.esWindows()) {
            return EjecutorProcesos.crearComando("ipconfig");
        } else if (SistemaOperativo.esLinux()) {
            // Algunas distribuciones no traen "ip"; en ese caso se prueba "ifconfig"
            if (EjecutorProcesos.comandoDisponible("ip")) {
                return EjecutorProcesos.crearComando("ip", "addr");
            } else if (EjecutorProcesos.comandoDisponible("ifconfig")) {
                return EjecutorProcesos.crearComando("ifconfig");
            }
            // Ninguno disponible: se devuelve "ip addr" y se informará del error tipo 3
            return EjecutorProcesos.crearComando("ip", "addr");
        }
        return null;
    }

    private ArrayList<String> comandoNombreEquipo() {
        if (SistemaOperativo.esSoportado()) {
            return EjecutorProcesos.crearComando("hostname");
        }
        return null;
    }

    private ArrayList<String> comandoVersionJava() {
        // Se usa el java de la propia JVM: funciona igual en Windows y en Linux
        return EjecutorProcesos.crearComando(EjecutorProcesos.getRutaJava(), "-version");
    }

    /**
     * Comando para lanzar ProgramaAuxiliar: java -cp CLASSPATH Clase nombre codigo
     */
    private ArrayList<String> comandoClaseAuxiliar(String nombre, int codigoSalida) {
        // El nombre va en un solo argumento aunque tenga espacios.
        // Se usa el mismo classpath que GMM-Java (ver EjecutorProcesos.getClasspathAbsoluto)
        return EjecutorProcesos.crearComandoJava(CLASE_AUXILIAR, nombre, String.valueOf(codigoSalida));
    }

    // ------------------------------------------------------------------
    // Ejecución y presentación del resultado (común a todas las operaciones)
    // ------------------------------------------------------------------

    /**
     * Ejecuta un comando en el directorio de trabajo y muestra: comando,
     * argumentos, directorio, salida y resultado, clasificando el error si lo hay.
     *
     * @param operacion nombre de la operación para mostrarlo
     * @param comando   lista con programa y argumentos (null si el SO no está soportado)
     */
    private void ejecutarYMostrar(String operacion, ArrayList<String> comando) {
        System.out.println(">>> Operacion: " + operacion);

        // TIPO 3 (caso a): el sistema operativo no tiene comando para esta operación
        if (comando == null) {
            System.out.println("[ERROR TIPO 3] Comando no disponible en este sistema operativo.");
            System.out.println("               '" + SistemaOperativo.getNombre() + "' no esta soportado"
                    + " para comandos especificos.");
            return;
        }

        System.out.println("    Comando    : " + EjecutorProcesos.comandoComoTexto(comando));
        System.out.println("    Argumentos : (se pasan por separado a ProcessBuilder)");
        for (int i = 0; i < comando.size(); i++) {
            System.out.println("        [" + i + "] " + comando.get(i));
        }
        System.out.println("    Directorio : " + directorioTrabajo.getPath());

        // TIPO 3 (caso b): el programa no se encuentra en este sistema
        String programa = comando.get(0);
        if (!EjecutorProcesos.comandoDisponible(programa)) {
            System.out.println();
            System.out.println("[ERROR TIPO 3] Comando no disponible en este sistema operativo.");
            System.out.println("               No se encuentra '" + programa + "' en el PATH ni como fichero.");
            return;
        }

        // Antes de lanzar, se comprueba de nuevo que el directorio sigue siendo válido
        String problemaDirectorio = validarDirectorio(directorioTrabajo);
        if (problemaDirectorio != null) {
            System.out.println();
            System.out.println("[ERROR] Directorio de trabajo no valido: " + problemaDirectorio);
            System.out.println("        Cambielo con la opcion [6].");
            return;
        }

        ResultadoProceso resultado = EjecutorProcesos.ejecutar(comando, directorioTrabajo);

        // TIPO 1: el sistema operativo no ha podido crear el proceso
        if (!resultado.isIniciado()) {
            System.out.println();
            System.out.println("[ERROR TIPO 1] El programa no ha podido iniciarse.");
            System.out.println("               Motivo: " + resultado.getMensajeError());
            return;
        }

        System.out.println("    PID        : " + resultado.getPid());
        System.out.println("------------------- SALIDA -------------------------");
        if (resultado.getSalida().isEmpty()) {
            System.out.println("(el proceso no ha escrito nada)");
        } else {
            System.out.print(resultado.getSalida());
        }
        System.out.println("----------------------------------------------------");

        // Código 0 = todo bien. TIPO 2: arrancó pero terminó con error
        if (resultado.getCodigoSalida() == 0) {
            System.out.println("[OK] Proceso finalizado correctamente. Codigo de salida: 0");
        } else {
            System.out.println("[ERROR TIPO 2] El proceso se inicio correctamente, pero termino"
                    + " con codigo de error: " + resultado.getCodigoSalida());
        }
    }

    // ------------------------------------------------------------------
    // Opción 5: clase Java auxiliar con argumentos del usuario
    // ------------------------------------------------------------------

    private void ejecutarClaseAuxiliar() {
        System.out.println("Se lanzara " + CLASE_AUXILIAR + " como proceso hijo.");
        String nombre = Teclado.leerTexto("Nombre que recibira como argumento (puede tener espacios): ");
        if (nombre.isEmpty()) {
            nombre = "alumno DAM";
        }
        int codigo = Teclado.leerEntero("Codigo de salida que devolvera (0-255): ", 0, 255);
        System.out.println();
        ejecutarYMostrar("Ejecutar clase Java auxiliar", comandoClaseAuxiliar(nombre, codigo));
    }

    // ------------------------------------------------------------------
    // M2.3 Directorio de trabajo
    // ------------------------------------------------------------------

    private void cambiarDirectorioTrabajo() {
        System.out.println("Directorio actual: " + directorioTrabajo.getPath());
        String ruta = Teclado.leerTexto("Nueva ruta (ENTER para no cambiar): ");
        if (ruta.isEmpty()) {
            System.out.println("[i] Se mantiene el directorio actual.");
            return;
        }

        File nuevo = new File(ruta).getAbsoluteFile();
        String problema = validarDirectorio(nuevo);
        if (problema != null) {
            System.out.println("[ERROR] " + problema);
            System.out.println("        Se mantiene: " + directorioTrabajo.getPath());
            return;
        }

        directorioTrabajo = nuevo;
        System.out.println("[OK] Nuevo directorio de trabajo: " + directorioTrabajo.getPath());
    }

    /**
     * Comprueba que una ruta existe, es un directorio y se puede leer.
     *
     * @param directorio ruta a comprobar
     * @return null si es válido; si no, un texto con el problema
     */
    private String validarDirectorio(File directorio) {
        if (!directorio.exists()) {
            return "La ruta '" + directorio.getPath() + "' no existe.";
        }
        if (!directorio.isDirectory()) {
            return "La ruta '" + directorio.getPath() + "' no es un directorio.";
        }
        if (!directorio.canRead()) {
            return "No hay permiso de lectura sobre '" + directorio.getPath() + "'.";
        }
        return null;
    }

    // ------------------------------------------------------------------
    // M2.4 Demostración de los tres tipos de error
    // ------------------------------------------------------------------

    private void demostrarErrores() {
        System.out.println("####################################################");
        System.out.println(" CASO 1: programa que NO PUEDE INICIARSE");
        System.out.println(" Se intenta ejecutar un fichero de texto como si fuera un programa.");
        System.out.println("####################################################");
        File ficheroTexto = null;
        try {
            ficheroTexto = File.createTempFile("gmm_no_ejecutable", ".txt");
            ejecutarYMostrar("Ejecutar un fichero que no es un programa",
                    EjecutorProcesos.crearComando(ficheroTexto.getAbsolutePath()));
        } catch (IOException e) {
            System.out.println("[i] No se pudo preparar el caso 1: " + e.getMessage());
        } finally {
            if (ficheroTexto != null) {
                ficheroTexto.delete();
            }
        }

        System.out.println();
        System.out.println("####################################################");
        System.out.println(" CASO 2: proceso que arranca pero DEVUELVE UN ERROR");
        System.out.println(" ProgramaAuxiliar termina con System.exit(3).");
        System.out.println("####################################################");
        ejecutarYMostrar("Clase auxiliar con codigo de error",
                comandoClaseAuxiliar("prueba de error", 3));

        System.out.println();
        System.out.println("####################################################");
        System.out.println(" CASO 3: comando NO DISPONIBLE en este sistema operativo");
        System.out.println(" Se pide un comando propio del otro sistema operativo.");
        System.out.println("####################################################");
        ArrayList<String> comandoOtroSistema;
        if (SistemaOperativo.esWindows()) {
            comandoOtroSistema = EjecutorProcesos.crearComando("ifconfig");      // comando de Linux
        } else if (SistemaOperativo.esLinux()) {
            comandoOtroSistema = EjecutorProcesos.crearComando("ipconfig");      // comando de Windows
        } else {
            comandoOtroSistema = null;
        }
        ejecutarYMostrar("Comando de otro sistema operativo", comandoOtroSistema);
    }
}
