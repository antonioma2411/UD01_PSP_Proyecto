package es.ilerna.psp.gmm.util;

import java.util.Scanner;

/**
 * Lectura segura de datos por teclado.
 *
 * Se usa un único Scanner para todo el programa. Si se crearan varios
 * Scanner sobre System.in y se cerrara alguno, se cerraría también la
 * entrada estándar y el programa no podría volver a leer.
 */
public class Teclado {

    private static final Scanner ENTRADA = new Scanner(System.in);

    /**
     * Muestra un mensaje y devuelve la línea escrita por el usuario (sin espacios
     * al principio ni al final).
     *
     * @param mensaje texto que se muestra antes de leer
     * @return texto introducido (puede ser una cadena vacía)
     */
    public static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        String linea = ENTRADA.nextLine();
        return linea.trim();
    }

    /**
     * Pide un número entero hasta que el usuario escriba uno válido dentro
     * del rango indicado. Nunca lanza NumberFormatException hacia fuera.
     *
     * @param mensaje texto que se muestra antes de leer
     * @param minimo  valor mínimo permitido (incluido)
     * @param maximo  valor máximo permitido (incluido)
     * @return número válido introducido por el usuario
     */
    public static int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                int numero = Integer.parseInt(texto);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                System.out.println("  [!] El numero debe estar entre " + minimo + " y " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("  [!] '" + texto + "' no es un numero entero valido.");
            }
        }
    }

    /**
     * Detiene el programa hasta que el usuario pulse ENTER.
     */
    public static void pausar() {
        leerTexto("\nPulse ENTER para continuar...");
    }
}
