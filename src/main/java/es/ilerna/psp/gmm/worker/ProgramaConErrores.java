package es.ilerna.psp.gmm.worker;

import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Programa auxiliar que escribe por los DOS flujos de salida.
 *
 * - Por la salida estándar (System.out) escribe mensajes normales.
 * - Por la salida de errores (System.err) escribe un error CONTROLADO:
 *   intenta abrir un fichero que no existe, captura la excepción y la informa.
 *
 * Termina con System.exit(1) para indicar que ha habido un error.
 * Se usa en el Módulo 04 para comparar getInputStream() y getErrorStream().
 */
public class ProgramaConErrores {

    /** Fichero que no existe a propósito para provocar el error controlado. */
    private static final String FICHERO_INEXISTENTE = "fichero_que_no_existe_gmm.txt";

    public static void main(String[] args) {
        System.out.println("OUT 1: Empiezo a trabajar.");
        System.out.println("OUT 2: Voy a abrir el fichero " + FICHERO_INEXISTENTE);

        boolean hayError = false;
        try (FileReader lector = new FileReader(FICHERO_INEXISTENTE)) {
            System.out.println("OUT 3: Fichero abierto (no deberia pasar).");
        } catch (FileNotFoundException e) {
            // Error controlado: se informa por la salida de errores
            System.err.println("ERR 1: No se encuentra el fichero " + FICHERO_INEXISTENTE);
            System.err.println("ERR 2: Detalle -> " + e.getMessage());
            hayError = true;
        } catch (Exception e) {
            System.err.println("ERR 1: Error inesperado -> " + e);
            hayError = true;
        }

        System.out.println("OUT 3: Sigo funcionando despues del error.");
        System.out.println("OUT 4: Termino.");

        if (hayError) {
            System.exit(1);
        }
        System.exit(0);
    }
}
