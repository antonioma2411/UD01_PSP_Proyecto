package es.ilerna.psp.gmm.worker;

/**
 * Programa Java auxiliar que GMM-Java lanza como proceso hijo.
 *
 * Uso: java -cp ... es.ilerna.psp.gmm.worker.ProgramaAuxiliar [nombre] [codigoSalida]
 *
 *  - nombre: texto con el que saluda (puede tener espacios si llega como un solo argumento).
 *  - codigoSalida: número con el que termina mediante System.exit(n). Por defecto 0.
 *
 * Sirve para comprobar que los argumentos llegan separados y que el
 * proceso padre puede leer el código de salida del hijo.
 */
public class ProgramaAuxiliar {

    public static void main(String[] args) {
        String nombre = "desconocido";
        int codigoSalida = 0;

        if (args.length >= 1) {
            nombre = args[0];
        }
        if (args.length >= 2) {
            try {
                codigoSalida = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("[ProgramaAuxiliar] El codigo '" + args[1] + "' no es un numero.");
                codigoSalida = 2;
            }
        }

        System.out.println("[ProgramaAuxiliar] Hola, " + nombre + ".");
        System.out.println("[ProgramaAuxiliar] He recibido " + args.length + " argumento(s):");
        for (int i = 0; i < args.length; i++) {
            System.out.println("    args[" + i + "] = " + args[i]);
        }
        System.out.println("[ProgramaAuxiliar] Termino con System.exit(" + codigoSalida + ").");

        System.exit(codigoSalida);
    }
}
