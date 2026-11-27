package es.ilerna.psp.gmm.bernstein;

import es.ilerna.psp.gmm.util.Teclado;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Módulo 06. Analizador de dependencias de Bernstein.
 *
 *  - M6.1 Definición de tareas: a mano o con un ejemplo precargado de 4 tareas.
 *  - M6.2 Análisis de parejas con las tres condiciones (RAW, WAR, WAW).
 *  - M6.3 Diagnóstico justificado: qué condición se cumple o falla y qué variables chocan.
 *  - M6.4 Matriz de compatibilidad (mínimo 4 tareas).
 *  - M6.5 Grafo de dependencias en texto, respetando el orden de las instrucciones.
 *
 * El orden de referencia es el orden en el que se introducen las tareas:
 * la primera tarea va antes que la segunda, la segunda antes que la tercera...
 */
public class AnalizadorBernstein {

    private static final int MINIMO_MATRIZ = 4;
    private static final int MAXIMO_TAREAS = 10;

    private static final String LINEA = "====================================================";
    private static final String LINEA_FINA = "----------------------------------------------------";

    /** Tareas actuales, en el orden de referencia. Se conservan al volver al menú principal. */
    private final ArrayList<Tarea> tareas = new ArrayList<>();

    /**
     * Menú del módulo. Se repite hasta que el usuario elige volver.
     */
    public void mostrarMenu() {
        int opcion = -1;
        while (opcion != 0) {
            System.out.println(LINEA);
            System.out.println(" MODULO 06. ANALIZADOR DE DEPENDENCIAS DE BERNSTEIN");
            System.out.println(LINEA);
            System.out.println(" Tareas cargadas: " + tareas.size());
            System.out.println("[1] Introducir tareas (M6.1)");
            System.out.println("[2] Cargar ejemplo precargado de 4 tareas");
            System.out.println("[3] Ver las tareas actuales");
            System.out.println("[4] Diagnostico por parejas (M6.2 y M6.3)");
            System.out.println("[5] Matriz de compatibilidad (M6.4)");
            System.out.println("[6] Grafo de dependencias (M6.5)");
            System.out.println("[7] Informe completo (todo lo anterior)");
            System.out.println("[0] Volver al menu principal");
            System.out.println();

            opcion = Teclado.leerEntero("Seleccione una opcion: ", 0, 7);
            System.out.println();

            switch (opcion) {
                case 1:
                    introducirTareas();
                    break;
                case 2:
                    cargarEjemplo();
                    mostrarTareas();
                    break;
                case 3:
                    mostrarTareas();
                    break;
                case 4:
                    if (hayTareasSuficientes()) {
                        mostrarDiagnostico();
                    }
                    break;
                case 5:
                    if (hayTareasSuficientes()) {
                        mostrarMatriz();
                    }
                    break;
                case 6:
                    if (hayTareasSuficientes()) {
                        mostrarGrafo();
                    }
                    break;
                case 7:
                    if (hayTareasSuficientes()) {
                        mostrarInformeCompleto();
                    }
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
    // M6.1 Definición de tareas
    // ------------------------------------------------------------------

    /**
     * Pide las tareas una a una. Sustituye a las tareas que hubiera antes.
     */
    private void introducirTareas() {
        System.out.println("Introduzca las tareas EN ORDEN (la primera es la que va antes).");
        System.out.println("Las variables se separan con comas o espacios. ENTER = conjunto vacio.");
        System.out.println();
        int cantidad = Teclado.leerEntero("Numero de tareas (2-" + MAXIMO_TAREAS + "): ", 2, MAXIMO_TAREAS);

        ArrayList<Tarea> nuevas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            System.out.println();
            System.out.println("--- Tarea " + (i + 1) + " de " + cantidad + " ---");
            String id = pedirIdentificador(nuevas, "P" + (i + 1));
            String descripcion = Teclado.leerTexto("Instruccion (opcional, ej. a = x + y): ");
            HashSet<String> lecturas = Tarea.crearConjunto(Teclado.leerTexto("Variables que LEE   L(" + id + "): "));
            HashSet<String> escrituras = Tarea.crearConjunto(Teclado.leerTexto("Variables que ESCRIBE E(" + id + "): "));
            if (escrituras.isEmpty()) {
                System.out.println("  [i] Esta tarea no escribe nada: nunca podra provocar RAW ni WAW.");
            }
            nuevas.add(new Tarea(id, descripcion, lecturas, escrituras));
        }

        tareas.clear();
        tareas.addAll(nuevas);
        System.out.println();
        System.out.println("Se han guardado " + tareas.size() + " tareas.");
        mostrarTareas();
        if (tareas.size() < MINIMO_MATRIZ) {
            System.out.println("  [i] La matriz de compatibilidad pide al menos " + MINIMO_MATRIZ + " tareas.");
        }
    }

    /**
     * Pide un identificador no repetido. Si se deja vacío se usa el propuesto (P1, P2...).
     */
    private String pedirIdentificador(ArrayList<Tarea> yaIntroducidas, String propuesto) {
        while (true) {
            String id = Teclado.leerTexto("Identificador [" + propuesto + "]: ");
            if (id.isEmpty()) {
                id = propuesto;
            }
            if (id.contains(" ")) {
                System.out.println("  [!] El identificador no puede tener espacios.");
                continue;
            }
            boolean repetido = false;
            for (int i = 0; i < yaIntroducidas.size(); i++) {
                if (yaIntroducidas.get(i).getId().equalsIgnoreCase(id)) {
                    repetido = true;
                }
            }
            if (repetido) {
                System.out.println("  [!] Ya existe una tarea con el identificador '" + id + "'.");
            } else {
                return id;
            }
        }
    }

    /**
     * Ejemplo con 4 instrucciones que provoca los tres tipos de conflicto:
     *   P1: a = x + y   ->  L = {x, y}   E = {a}
     *   P2: b = z * 2   ->  L = {z}      E = {b}
     *   P3: c = a + b   ->  L = {a, b}   E = {c}
     *   P4: a = z - 1   ->  L = {z}      E = {a}
     */
    public void cargarEjemplo() {
        tareas.clear();
        tareas.add(new Tarea("P1", "a = x + y", Tarea.crearConjunto("x, y"), Tarea.crearConjunto("a")));
        tareas.add(new Tarea("P2", "b = z * 2", Tarea.crearConjunto("z"), Tarea.crearConjunto("b")));
        tareas.add(new Tarea("P3", "c = a + b", Tarea.crearConjunto("a, b"), Tarea.crearConjunto("c")));
        tareas.add(new Tarea("P4", "a = z - 1", Tarea.crearConjunto("z"), Tarea.crearConjunto("a")));
        System.out.println("Ejemplo cargado (4 tareas).");
    }

    private void mostrarTareas() {
        System.out.println(LINEA_FINA);
        System.out.println(" Tareas (en orden de referencia)");
        System.out.println(LINEA_FINA);
        if (tareas.isEmpty()) {
            System.out.println("  (no hay tareas; use la opcion [1] o [2])");
            return;
        }
        for (int i = 0; i < tareas.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + tareas.get(i));
        }
    }

    private boolean hayTareasSuficientes() {
        if (tareas.size() < 2) {
            System.out.println("  [!] Se necesitan al menos 2 tareas. Use la opcion [1] o [2].");
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // M6.2 y M6.3 Análisis y diagnóstico de cada pareja
    // ------------------------------------------------------------------

    /**
     * Compara TODAS las parejas (i, j) con i < j, de modo que tareas.get(i)
     * siempre va antes que tareas.get(j) en el orden de referencia.
     * Se guarda en una matriz; solo se rellena la mitad superior (i < j).
     */
    private ComparacionPareja[][] compararTodas() {
        int n = tareas.size();
        ComparacionPareja[][] comparaciones = new ComparacionPareja[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                comparaciones[i][j] = new ComparacionPareja(tareas.get(i), tareas.get(j));
            }
        }
        return comparaciones;
    }

    private void mostrarDiagnostico() {
        System.out.println(LINEA);
        System.out.println(" DIAGNOSTICO POR PAREJAS (condiciones de Bernstein)");
        System.out.println(LINEA);
        System.out.println(" Para Pi antes que Pj, pueden ir en paralelo si se cumplen las 3:");
        System.out.println("   C1: E(Pi) inter L(Pj) = {}   (si falla -> RAW, lectura tras escritura)");
        System.out.println("   C2: L(Pi) inter E(Pj) = {}   (si falla -> WAR, escritura tras lectura)");
        System.out.println("   C3: E(Pi) inter E(Pj) = {}   (si falla -> WAW, escritura tras escritura)");

        ComparacionPareja[][] comparaciones = compararTodas();
        int compatibles = 0;
        int total = 0;
        for (int i = 0; i < tareas.size(); i++) {
            for (int j = i + 1; j < tareas.size(); j++) {
                mostrarPareja(comparaciones[i][j]);
                total++;
                if (comparaciones[i][j].esCompatible()) {
                    compatibles++;
                }
            }
        }
        System.out.println();
        System.out.println(" Parejas analizadas: " + total + " | Compatibles: " + compatibles
                + " | Incompatibles: " + (total - compatibles));
        mostrarNotaModeloEstatico();
    }

    /**
     * Justifica una pareja: el resultado de cada condición y, si falla, qué variables chocan.
     */
    private void mostrarPareja(ComparacionPareja c) {
        Tarea pi = c.getAnterior();
        Tarea pj = c.getPosterior();
        String a = pi.getId();
        String b = pj.getId();

        System.out.println();
        System.out.println(LINEA_FINA);
        System.out.println(" " + a + " -> " + b + "   (" + a + " va antes que " + b + ")");
        System.out.println(LINEA_FINA);

        // Condición 1: RAW
        System.out.println("  C1: E(" + a + ") inter L(" + b + ") = " + Tarea.textoConjunto(pi.getEscrituras())
                + " inter " + Tarea.textoConjunto(pj.getLecturas()) + " = " + Tarea.textoConjunto(c.getRaw()));
        if (c.getRaw().isEmpty()) {
            System.out.println("      Se cumple.");
        } else {
            System.out.println("      NO se cumple -> RAW: " + b + " lee " + Tarea.textoConjunto(c.getRaw())
                    + " despues de que " + a + " lo escriba.");
        }

        // Condición 2: WAR
        System.out.println("  C2: L(" + a + ") inter E(" + b + ") = " + Tarea.textoConjunto(pi.getLecturas())
                + " inter " + Tarea.textoConjunto(pj.getEscrituras()) + " = " + Tarea.textoConjunto(c.getWar()));
        if (c.getWar().isEmpty()) {
            System.out.println("      Se cumple.");
        } else {
            System.out.println("      NO se cumple -> WAR: " + b + " escribe " + Tarea.textoConjunto(c.getWar())
                    + " que " + a + " necesita leer antes.");
        }

        // Condición 3: WAW
        System.out.println("  C3: E(" + a + ") inter E(" + b + ") = " + Tarea.textoConjunto(pi.getEscrituras())
                + " inter " + Tarea.textoConjunto(pj.getEscrituras()) + " = " + Tarea.textoConjunto(c.getWaw()));
        if (c.getWaw().isEmpty()) {
            System.out.println("      Se cumple.");
        } else {
            System.out.println("      NO se cumple -> WAW: " + a + " y " + b + " escriben " + Tarea.textoConjunto(c.getWaw())
                    + "; el valor final depende de quien escriba el ultimo.");
        }

        if (c.esCompatible()) {
            System.out.println("  => COMPATIBLES: se cumplen las 3 condiciones; pueden ejecutarse en paralelo.");
        } else {
            System.out.println("  => INCOMPATIBLES (" + c.getTiposConflicto() + "): " + b
                    + " debe esperar a que termine " + a + ".");
        }
    }

    // ------------------------------------------------------------------
    // M6.4 Matriz de compatibilidad
    // ------------------------------------------------------------------

    private void mostrarMatriz() {
        System.out.println(LINEA);
        System.out.println(" MATRIZ DE COMPATIBILIDAD");
        System.out.println(LINEA);
        if (tareas.size() < MINIMO_MATRIZ) {
            System.out.println("  [i] El enunciado pide evaluar al menos " + MINIMO_MATRIZ
                    + " tareas; ahora hay " + tareas.size() + ".");
        }

        ComparacionPareja[][] comparaciones = compararTodas();
        int n = tareas.size();

        // Ancho de cada columna: lo suficiente para "RAW,WAR,WAW" y para el identificador más largo
        int ancho = 12;
        for (int i = 0; i < n; i++) {
            if (tareas.get(i).getId().length() + 2 > ancho) {
                ancho = tareas.get(i).getId().length() + 2;
            }
        }
        String formato = "%-" + ancho + "s";

        // Cabecera
        StringBuilder fila = new StringBuilder(String.format(formato, ""));
        for (int j = 0; j < n; j++) {
            fila.append(String.format(formato, tareas.get(j).getId()));
        }
        System.out.println(fila);

        // Filas: la matriz es simétrica, así que (i, j) y (j, i) muestran la misma comparación
        for (int i = 0; i < n; i++) {
            fila = new StringBuilder(String.format(formato, tareas.get(i).getId()));
            for (int j = 0; j < n; j++) {
                String celda;
                if (i == j) {
                    celda = "-";
                } else if (i < j) {
                    celda = comparaciones[i][j].getTiposConflicto();
                } else {
                    celda = comparaciones[j][i].getTiposConflicto();
                }
                fila.append(String.format(formato, celda));
            }
            System.out.println(fila);
        }

        System.out.println();
        System.out.println(" Leyenda: OK = compatibles (paralelo posible) | RAW/WAR/WAW = incompatibles");

        // Listado de parejas compatibles e incompatibles
        System.out.println();
        System.out.println(" Parejas compatibles:");
        int contador = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (comparaciones[i][j].esCompatible()) {
                    System.out.println("   " + tareas.get(i).getId() + " || " + tareas.get(j).getId());
                    contador++;
                }
            }
        }
        if (contador == 0) {
            System.out.println("   (ninguna)");
        }
        System.out.println(" Parejas incompatibles:");
        contador = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                ComparacionPareja c = comparaciones[i][j];
                if (!c.esCompatible()) {
                    System.out.println("   " + tareas.get(i).getId() + " x " + tareas.get(j).getId()
                            + "  -> " + c.getTiposConflicto() + " por " + textoVariablesConflicto(c));
                    contador++;
                }
            }
        }
        if (contador == 0) {
            System.out.println("   (ninguna)");
        }
    }

    /**
     * Texto con las variables de cada conflicto, por ejemplo "RAW{a} WAW{a}".
     */
    private String textoVariablesConflicto(ComparacionPareja c) {
        String texto = "";
        if (!c.getRaw().isEmpty()) {
            texto += "RAW" + Tarea.textoConjunto(c.getRaw()) + " ";
        }
        if (!c.getWar().isEmpty()) {
            texto += "WAR" + Tarea.textoConjunto(c.getWar()) + " ";
        }
        if (!c.getWaw().isEmpty()) {
            texto += "WAW" + Tarea.textoConjunto(c.getWaw()) + " ";
        }
        return texto.trim();
    }

    // ------------------------------------------------------------------
    // M6.5 Grafo de dependencias
    // ------------------------------------------------------------------

    /**
     * Lista de aristas Pi --> Pj (siempre de la anterior a la posterior) y
     * niveles de ejecución: las tareas de un mismo nivel no dependen entre sí.
     */
    private void mostrarGrafo() {
        System.out.println(LINEA);
        System.out.println(" GRAFO DE DEPENDENCIAS");
        System.out.println(LINEA);

        StringBuilder orden = new StringBuilder(" Orden de referencia: ");
        for (int i = 0; i < tareas.size(); i++) {
            if (i > 0) {
                orden.append(" -> ");
            }
            orden.append(tareas.get(i).getId());
        }
        System.out.println(orden);
        System.out.println();

        ComparacionPareja[][] comparaciones = compararTodas();
        int n = tareas.size();

        // 1) Lista de aristas
        System.out.println(" Aristas (la flecha va de la tarea que debe ir antes a la que espera):");
        int aristas = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                ComparacionPareja c = comparaciones[i][j];
                if (!c.esCompatible()) {
                    System.out.println("   " + tareas.get(i).getId() + " --[" + textoVariablesConflicto(c) + "]--> "
                            + tareas.get(j).getId());
                    aristas++;
                }
            }
        }
        if (aristas == 0) {
            System.out.println("   (sin aristas: todas las tareas son independientes)");
        }

        // 2) Predecesores de cada tarea
        System.out.println();
        System.out.println(" Cada tarea debe esperar a:");
        for (int j = 0; j < n; j++) {
            String espera = "";
            for (int i = 0; i < j; i++) {
                if (!comparaciones[i][j].esCompatible()) {
                    if (!espera.isEmpty()) {
                        espera += ", ";
                    }
                    espera += tareas.get(i).getId();
                }
            }
            if (espera.isEmpty()) {
                espera = "nadie (puede empezar al principio)";
            }
            System.out.println("   " + tareas.get(j).getId() + " <- " + espera);
        }

        // 3) Niveles: nivel de una tarea = 1 + el mayor nivel de las tareas a las que espera
        int[] nivel = new int[n];
        int nivelMaximo = 0;
        for (int j = 0; j < n; j++) {
            nivel[j] = 1;
            for (int i = 0; i < j; i++) {
                if (!comparaciones[i][j].esCompatible() && nivel[i] + 1 > nivel[j]) {
                    nivel[j] = nivel[i] + 1;
                }
            }
            if (nivel[j] > nivelMaximo) {
                nivelMaximo = nivel[j];
            }
        }

        System.out.println();
        System.out.println(" Esquema por niveles (las tareas de un mismo nivel pueden ir en paralelo):");
        for (int k = 1; k <= nivelMaximo; k++) {
            String enNivel = "";
            for (int j = 0; j < n; j++) {
                if (nivel[j] == k) {
                    if (!enNivel.isEmpty()) {
                        enNivel += " || ";
                    }
                    enNivel += tareas.get(j).getId();
                }
            }
            System.out.println("   Nivel " + k + ":  " + enNivel);
            if (k < nivelMaximo) {
                System.out.println("              |");
                System.out.println("              v");
            }
        }
        System.out.println();
        System.out.println(" Se necesitan " + nivelMaximo + " paso(s) en lugar de " + n + " si todo fuera secuencial.");
        mostrarNotaModeloEstatico();
    }

    private void mostrarInformeCompleto() {
        mostrarTareas();
        System.out.println();
        mostrarDiagnostico();
        System.out.println();
        mostrarMatriz();
        System.out.println();
        mostrarGrafo();
    }

    /**
     * Aclaración que pide el enunciado sobre los límites del análisis.
     */
    private void mostrarNotaModeloEstatico() {
        System.out.println();
        System.out.println(" [i] Nota: este es un modelo ESTATICO de dependencias. Solo mira que");
        System.out.println("     variables lee y escribe cada tarea segun lo indicado. No garantiza por");
        System.out.println("     si solo la ausencia de problemas de concurrencia en una aplicacion real");
        System.out.println("     (accesos por punteros/referencias, ficheros, E/S, recursos compartidos,");
        System.out.println("     bloqueos o condiciones de carrera que dependen de la ejecucion).");
    }
}
