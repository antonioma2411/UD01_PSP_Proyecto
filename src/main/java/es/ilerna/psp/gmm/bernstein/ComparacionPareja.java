package es.ilerna.psp.gmm.bernstein;

import java.util.HashSet;

/**
 * Resultado de comparar dos tareas con las tres condiciones de Bernstein (M6.2).
 *
 * "anterior" va antes que "posterior" en el orden de referencia del programa.
 *
 *  Condición 1: E(anterior) ∩ L(posterior) = vacío  -> si no, conflicto RAW
 *               (Read After Write: la posterior LEE lo que la anterior ESCRIBE)
 *  Condición 2: L(anterior) ∩ E(posterior) = vacío  -> si no, conflicto WAR
 *               (Write After Read: la posterior ESCRIBE lo que la anterior LEE)
 *  Condición 3: E(anterior) ∩ E(posterior) = vacío  -> si no, conflicto WAW
 *               (Write After Write: las dos ESCRIBEN la misma variable)
 */
public class ComparacionPareja {

    private final Tarea anterior;
    private final Tarea posterior;
    private final HashSet<String> raw;
    private final HashSet<String> war;
    private final HashSet<String> waw;

    public ComparacionPareja(Tarea anterior, Tarea posterior) {
        this.anterior = anterior;
        this.posterior = posterior;
        this.raw = interseccion(anterior.getEscrituras(), posterior.getLecturas());
        this.war = interseccion(anterior.getLecturas(), posterior.getEscrituras());
        this.waw = interseccion(anterior.getEscrituras(), posterior.getEscrituras());
    }

    /**
     * Intersección de dos conjuntos: los elementos que están en los dos.
     * Se recorre el primero y se pregunta al segundo con contains().
     *
     * @return conjunto nuevo con los elementos comunes (vacío si no hay ninguno)
     */
    public static HashSet<String> interseccion(HashSet<String> a, HashSet<String> b) {
        HashSet<String> comunes = new HashSet<>();
        for (String variable : a) {
            if (b.contains(variable)) {
                comunes.add(variable);
            }
        }
        return comunes;
    }

    /**
     * @return true si se cumplen las tres condiciones (las tres intersecciones vacías)
     */
    public boolean esCompatible() {
        return raw.isEmpty() && war.isEmpty() && waw.isEmpty();
    }

    /**
     * Tipos de conflicto encontrados, por ejemplo "RAW,WAW" u "OK" si no hay ninguno.
     */
    public String getTiposConflicto() {
        if (esCompatible()) {
            return "OK";
        }
        String tipos = "";
        if (!raw.isEmpty()) {
            tipos = "RAW";
        }
        if (!war.isEmpty()) {
            if (!tipos.isEmpty()) {
                tipos += ",";
            }
            tipos += "WAR";
        }
        if (!waw.isEmpty()) {
            if (!tipos.isEmpty()) {
                tipos += ",";
            }
            tipos += "WAW";
        }
        return tipos;
    }

    public Tarea getAnterior() {
        return anterior;
    }

    public Tarea getPosterior() {
        return posterior;
    }

    public HashSet<String> getRaw() {
        return raw;
    }

    public HashSet<String> getWar() {
        return war;
    }

    public HashSet<String> getWaw() {
        return waw;
    }
}
