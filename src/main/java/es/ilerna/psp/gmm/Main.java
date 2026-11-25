package es.ilerna.psp.gmm;

import es.ilerna.psp.gmm.entorno.MonitorEntorno;
import es.ilerna.psp.gmm.procesos.EjecutorMultiplataforma;
import es.ilerna.psp.gmm.util.SistemaOperativo;
import es.ilerna.psp.gmm.util.Teclado;

import java.util.NoSuchElementException;

/**
 * Punto de entrada de GMM-Java (Gestor Multiproceso Multiplataforma).
 *
 * Muestra el menú principal y llama al módulo elegido. Todo el trabajo de
 * cada opción está en su propia clase; aquí solo está la navegación.
 * Cada opción se ejecuta dentro de un try/catch para que un fallo en un
 * módulo no cierre el programa.
 */
public class Main {

    private static final String LINEA = "====================================================";

    public static void main(String[] args) {
        // El ejecutor guarda el directorio de trabajo elegido, por eso se crea una sola vez
        EjecutorMultiplataforma ejecutor = new EjecutorMultiplataforma();

        boolean salir = false;
        while (!salir) {
            mostrarCabecera();
            mostrarMenu();
            try {
                int opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 8);
                System.out.println();

                switch (opcion) {
                    case 1:
                        new MonitorEntorno().mostrarInforme();
                        break;
                    case 2:
                        ejecutor.mostrarMenu();
                        break;
                    case 3:
                        mostrarPendiente("Control y supervision de procesos");
                        break;
                    case 4:
                        mostrarPendiente("Gestion de entrada/salida y logs");
                        break;
                    case 5:
                        mostrarPendiente("Procesamiento paralelo de ficheros");
                        break;
                    case 6:
                        mostrarPendiente("Analisis de dependencias de Bernstein");
                        break;
                    case 7:
                        mostrarPendiente("Simulador de planificacion de CPU");
                        break;
                    case 8:
                        mostrarPendiente("Consultar registros de ejecucion");
                        break;
                    case 0:
                        salir = true;
                        break;
                    default:
                        // No debería llegar aquí porque leerEntero ya valida el rango
                        System.out.println("Opcion no valida.");
                }

                // Tras cada módulo se espera al usuario antes de volver a pintar el menú
                // (las opciones 1 y pendientes; la 2 ya tiene su propio menú)
                if (opcion == 1 || opcion >= 3) {
                    Teclado.pausar();
                }

            } catch (NoSuchElementException e) {
                // La entrada estándar se ha cerrado (por ejemplo, Ctrl+Z / Ctrl+D)
                System.out.println("\nNo hay mas datos de entrada. Cerrando GMM-Java.");
                salir = true;
            } catch (Exception e) {
                // Cualquier otro fallo inesperado: se informa y se vuelve al menú
                System.out.println("\n[ERROR] Se ha producido un error inesperado: " + e);
                System.out.println("El programa continua. Volviendo al menu principal...");
            }
        }
        System.out.println("Hasta pronto. GMM-Java finalizado.");
    }

    /**
     * Cabecera con el sistema operativo, la versión de Java y los procesadores.
     */
    private static void mostrarCabecera() {
        System.out.println();
        System.out.println(LINEA);
        System.out.println("          GMM-JAVA | GESTOR MULTIPROCESO");
        System.out.println(LINEA);
        System.out.println("Sistema operativo: " + SistemaOperativo.getTipo());
        System.out.println("Version de Java: " + System.getProperty("java.version"));
        System.out.println("Procesadores disponibles: " + Runtime.getRuntime().availableProcessors());
        System.out.println(LINEA);
    }

    /**
     * Opciones del menú principal, tal y como indica el enunciado.
     */
    private static void mostrarMenu() {
        System.out.println("[1] Informacion del sistema");
        System.out.println("[2] Ejecutor multiplataforma de comandos");
        System.out.println("[3] Control y supervision de procesos");
        System.out.println("[4] Gestion de entrada/salida y logs");
        System.out.println("[5] Procesamiento paralelo de ficheros");
        System.out.println("[6] Analisis de dependencias de Bernstein");
        System.out.println("[7] Simulador de planificacion de CPU");
        System.out.println("[8] Consultar registros de ejecucion");
        System.out.println("[0] Salir");
        System.out.println();
    }

    /**
     * Aviso para los módulos que todavía no están implementados.
     *
     * @param nombreModulo nombre que se muestra al usuario
     */
    private static void mostrarPendiente(String nombreModulo) {
        System.out.println("----------------------------------------------------");
        System.out.println(" " + nombreModulo);
        System.out.println("----------------------------------------------------");
        System.out.println(" [i] Este modulo esta pendiente de implementacion.");
        System.out.println("     Estara disponible en una proxima version.");
    }
}
