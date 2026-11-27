package es.ilerna.psp.gmm.worker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Módulo 05 (M5.2). Proceso secundario que cuenta UNA vocal en un fichero.
 *
 * Uso: java -cp ... es.ilerna.psp.gmm.worker.ProcesadorFichero FICHERO VOCAL FICHERO_RESULTADO
 *
 *  - Lee el fichero en UTF-8.
 *  - Cuenta la vocal sin distinguir mayúsculas y minúsculas ('a' y 'A' cuentan igual).
 *  - Las vocales acentuadas (á, é, í, ó, ú, ü) son caracteres distintos y NO se cuentan.
 *  - Guarda el resultado en FICHERO_RESULTADO (UTF-8).
 *  - Los errores se escriben en la salida de error; el Lanzador la redirige a Errores_X.txt.
 *
 * Códigos de salida:
 *   0 = correcto
 *   1 = argumentos incorrectos
 *   2 = no se pudo leer el fichero de entrada
 *   3 = no se pudo escribir el fichero de resultado
 */
public class ProcesadorFichero {

    public static final int OK = 0;
    public static final int ERROR_ARGUMENTOS = 1;
    public static final int ERROR_LECTURA = 2;
    public static final int ERROR_ESCRITURA = 3;

    /** Texto con el que empieza la línea del recuento; el Lanzador la busca para leer el número. */
    public static final String ETIQUETA_APARICIONES = "Apariciones: ";

    private static final String VOCALES = "aeiou";

    public static void main(String[] args) {
        // 1) Comprobar argumentos
        if (args.length != 3) {
            System.err.println("Uso: ProcesadorFichero FICHERO VOCAL FICHERO_RESULTADO");
            System.err.println("Se han recibido " + args.length + " argumento(s).");
            System.exit(ERROR_ARGUMENTOS);
        }
        File entrada = new File(args[0]);
        String textoVocal = args[1].toLowerCase();
        File salida = new File(args[2]);

        if (textoVocal.length() != 1 || VOCALES.indexOf(textoVocal.charAt(0)) < 0) {
            System.err.println("La vocal '" + args[1] + "' no es valida. Use A, E, I, O o U.");
            System.exit(ERROR_ARGUMENTOS);
        }
        char vocal = textoVocal.charAt(0);

        // 2) Contar
        long inicio = System.nanoTime();
        long apariciones;
        try {
            apariciones = contarVocal(entrada, vocal);
        } catch (IOException e) {
            System.err.println("No se pudo leer el fichero '" + entrada.getPath() + "': " + e.getMessage());
            System.exit(ERROR_LECTURA);
            return; // no se llega aquí, pero el compilador necesita saber que apariciones tiene valor
        }
        long milis = (System.nanoTime() - inicio) / 1_000_000;

        // 3) Guardar el resultado
        try {
            guardarResultado(salida, entrada, vocal, apariciones, milis);
        } catch (IOException e) {
            System.err.println("No se pudo escribir el resultado en '" + salida.getPath() + "': " + e.getMessage());
            System.exit(ERROR_ESCRITURA);
        }

        System.exit(OK);
    }

    /**
     * Recorre el fichero carácter a carácter y cuenta la vocal indicada.
     * Character.toLowerCase convierte 'A' en 'a', pero 'Á' pasa a 'á',
     * que es un carácter distinto de 'a', así que las acentuadas no se cuentan.
     *
     * @param fichero fichero de texto en UTF-8
     * @param vocal   vocal en minúscula (a, e, i, o, u)
     * @return número de apariciones
     * @throws IOException si no se puede leer el fichero
     */
    public static long contarVocal(File fichero, char vocal) throws IOException {
        long contador = 0;
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
                new FileInputStream(fichero), StandardCharsets.UTF_8))) {
            int caracter;
            while ((caracter = lector.read()) != -1) {
                if (Character.toLowerCase((char) caracter) == vocal) {
                    contador++;
                }
            }
        }
        return contador;
    }

    private static void guardarResultado(File salida, File entrada, char vocal, long apariciones, long milis)
            throws IOException {
        File carpeta = salida.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }
        // FileOutputStream sin "true" = sobrescribir el resultado anterior
        try (PrintWriter escritor = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(salida), StandardCharsets.UTF_8))) {
            escritor.println("Fichero: " + entrada.getName());
            escritor.println("Vocal: " + Character.toUpperCase(vocal));
            escritor.println(ETIQUETA_APARICIONES + apariciones);
            escritor.println("Tiempo de recuento: " + milis + " ms");
            escritor.println("PID: " + ProcessHandle.current().pid());
            if (escritor.checkError()) {
                throw new IOException("error al escribir en el fichero");
            }
        }
    }
}
