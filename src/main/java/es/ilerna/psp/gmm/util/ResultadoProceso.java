package es.ilerna.psp.gmm.util;

/**
 * Guarda lo que ha ocurrido al ejecutar un proceso con {@link EjecutorProcesos}.
 *
 * Es una clase sencilla de datos: solo tiene atributos, getters y setters.
 */
public class ResultadoProceso {

    /** true si el proceso llegó a arrancar (start() no falló). */
    private boolean iniciado;

    /** Identificador del proceso en el sistema operativo (-1 si no arrancó). */
    private long pid = -1;

    /** Código de salida del proceso (-1 si no arrancó o no terminó). */
    private int codigoSalida = -1;

    /** Texto que el proceso ha escrito por pantalla. */
    private String salida = "";

    /** Motivo del fallo cuando el proceso no ha podido iniciarse. */
    private String mensajeError = "";

    public boolean isIniciado() {
        return iniciado;
    }

    public void setIniciado(boolean iniciado) {
        this.iniciado = iniciado;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getCodigoSalida() {
        return codigoSalida;
    }

    public void setCodigoSalida(int codigoSalida) {
        this.codigoSalida = codigoSalida;
    }

    public String getSalida() {
        return salida;
    }

    public void setSalida(String salida) {
        this.salida = salida;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    /**
     * @return true si el proceso arrancó y terminó con código 0
     */
    public boolean esCorrecto() {
        return iniciado && codigoSalida == 0;
    }
}
