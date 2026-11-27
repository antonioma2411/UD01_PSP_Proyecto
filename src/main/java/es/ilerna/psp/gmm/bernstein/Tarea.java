package es.ilerna.psp.gmm.bernstein;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

/**
 * Una tarea (o instrucción) para el análisis de Bernstein (M6.1).
 *
 *  - id: identificador, por ejemplo "P1".
 *  - descripcion: instrucción de ejemplo, por ejemplo "a = x + y" (opcional).
 *  - lecturas: conjunto L(P) de variables que la tarea LEE.
 *  - escrituras: conjunto E(P) de variables que la tarea ESCRIBE.
 *
 * Se usan HashSet porque en un conjunto no hay repetidos, igual que en matemáticas.
 */
public class Tarea {

    private final String id;
    private final String descripcion;
    private final HashSet<String> lecturas;
    private final HashSet<String> escrituras;

    public Tarea(String id, String descripcion, HashSet<String> lecturas, HashSet<String> escrituras) {
        this.id = id;
        this.descripcion = descripcion;
        this.lecturas = lecturas;
        this.escrituras = escrituras;
    }

    /**
     * Convierte un texto como "x, y z" en el conjunto {x, y, z}.
     * Las variables se pueden separar con comas y/o espacios. Texto vacío = conjunto vacío.
     *
     * @param texto variables escritas por el usuario
     * @return conjunto con las variables (sin repetidos)
     */
    public static HashSet<String> crearConjunto(String texto) {
        HashSet<String> conjunto = new HashSet<>();
        String limpio = texto.replace("{", " ").replace("}", " ").trim();
        if (limpio.isEmpty()) {
            return conjunto;
        }
        String[] partes = limpio.split("[,\\s]+");
        for (int i = 0; i < partes.length; i++) {
            if (!partes[i].isEmpty()) {
                conjunto.add(partes[i]);
            }
        }
        return conjunto;
    }

    /**
     * Muestra un conjunto ordenado alfabéticamente, por ejemplo "{a, b}" o "{}" si está vacío.
     * (Un HashSet no tiene orden, así que se copia a una lista y se ordena solo para mostrarlo.)
     *
     * @param conjunto conjunto de variables
     * @return texto del conjunto
     */
    public static String textoConjunto(HashSet<String> conjunto) {
        ArrayList<String> ordenadas = new ArrayList<>(conjunto);
        Collections.sort(ordenadas);
        StringBuilder texto = new StringBuilder("{");
        for (int i = 0; i < ordenadas.size(); i++) {
            if (i > 0) {
                texto.append(", ");
            }
            texto.append(ordenadas.get(i));
        }
        texto.append("}");
        return texto.toString();
    }

    public String getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public HashSet<String> getLecturas() {
        return lecturas;
    }

    public HashSet<String> getEscrituras() {
        return escrituras;
    }

    @Override
    public String toString() {
        String texto = id + ": L(" + id + ") = " + textoConjunto(lecturas)
                + "  E(" + id + ") = " + textoConjunto(escrituras);
        if (descripcion != null && !descripcion.isEmpty()) {
            texto += "   [" + descripcion + "]";
        }
        return texto;
    }
}
