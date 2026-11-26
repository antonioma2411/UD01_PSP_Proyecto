package es.ilerna.psp.gmm.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Programa auxiliar para la comunicación bidireccional padre-hijo (M4.5).
 *
 * Lee líneas de su entrada estándar (lo que el padre le envía) y, por cada
 * línea, responde por su salida estándar con UNA línea:
 *     ECO> TEXTO EN MAYUSCULAS (n caracteres)
 *
 * Termina correctamente en dos casos:
 *  1) Recibe la orden "FIN" (en mayúsculas o minúsculas).
 *  2) Se cierra su entrada estándar (readLine() devuelve null).
 *
 * Padre e hijo usan UTF-8 en los dos sentidos para que las tildes y la ñ
 * lleguen bien en Windows y en Linux.
 */
public class EcoLector {

    private static final String ORDEN_FIN = "FIN";

    public static void main(String[] args) {
        // Salida en UTF-8 con autoflush: cada println se envía al padre en el momento
        PrintStream salida = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        int contador = 0;

        try (BufferedReader entrada = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            String linea = entrada.readLine();
            while (linea != null) {
                if (linea.trim().equalsIgnoreCase(ORDEN_FIN)) {
                    salida.println("ECO> Orden FIN recibida. He procesado " + contador
                            + " mensaje(s). Adios.");
                    System.exit(0);
                }
                contador++;
                salida.println("ECO> " + linea.toUpperCase() + " (" + linea.length() + " caracteres)");
                linea = entrada.readLine();
            }

            // readLine() ha devuelto null: el padre ha cerrado nuestra entrada
            salida.println("ECO> Fin de entrada detectado. He procesado " + contador
                    + " mensaje(s). Adios.");
        } catch (IOException e) {
            System.err.println("[EcoLector] Error leyendo la entrada: " + e.getMessage());
            System.exit(2);
        }
        System.exit(0);
    }
}
