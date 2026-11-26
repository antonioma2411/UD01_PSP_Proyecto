package es.ilerna.psp.gmm.procesos;

/**
 * Estado final de una ejecución (M3.4).
 *
 *  - COMPLETADO: el proceso terminó solo y con código 0.
 *  - ERROR: no pudo iniciarse o terminó con un código distinto de 0.
 *  - TIMEOUT: superó el tiempo máximo y GMM-Java lo tuvo que finalizar.
 *  - CANCELADO: el usuario pidió detenerlo antes de que terminara.
 */
public enum EstadoEjecucion {
    COMPLETADO,
    ERROR,
    TIMEOUT,
    CANCELADO
}
