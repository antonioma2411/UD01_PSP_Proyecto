package es.ilerna.psp.gmm.worker;

/**
 * Programa auxiliar que tarda varios segundos en terminar.
 *
 * Uso: java -cp ... es.ilerna.psp.gmm.worker.ProcesoLento [segundos]
 *
 * Escribe una línea por cada segundo que pasa y termina con System.exit(0).
 * Se usa en el Módulo 03 para probar las esperas (waitFor) y la
 * cancelación por timeout (destroy / destroyForcibly).
 */
public class ProcesoLento {

    public static void main(String[] args) {
        int segundos = 5;
        if (args.length >= 1) {
            try {
                segundos = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("[ProcesoLento] '" + args[0] + "' no es un numero. Uso 5 segundos.");
            }
        }

        System.out.println("[ProcesoLento] Empiezo. Voy a tardar " + segundos + " segundo(s).");
        for (int i = 1; i <= segundos; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("[ProcesoLento] Me han interrumpido.");
                System.exit(1);
            }
            System.out.println("[ProcesoLento] ... segundo " + i + " de " + segundos);
        }
        System.out.println("[ProcesoLento] He terminado a tiempo. System.exit(0).");
        System.exit(0);
    }
}
