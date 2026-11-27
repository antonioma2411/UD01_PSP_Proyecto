package es.ilerna.psp.gmm.procesos;

import java.io.File;

/**
 * Datos de UNO de los cinco procesos del Módulo 05 (una vocal).
 *
 * Clase sencilla de datos: el Lanzador la va rellenando mientras lanza el
 * proceso, espera a que termine y lee su fichero de resultado.
 */
public class ProcesoVocal {

    private final String vocal;
    private final File ficheroResultado;
    private final File ficheroErrores;

    /** Proceso del sistema operativo (null si no llegó a arrancar). */
    private Process proceso;
    private RegistroEjecucion registro;
    private long inicioNanos;
    private long finNanos;

    /** null mientras no haya terminado o si no llegó a arrancar. */
    private Integer codigoSalida;
    /** -1 si no se ha podido leer el resultado. */
    private long apariciones = -1;
    private String estado = "Pendiente";

    public ProcesoVocal(String vocal, File carpetaResultados) {
        this.vocal = vocal;
        this.ficheroResultado = new File(carpetaResultados, "Resultado_" + vocal + ".txt");
        this.ficheroErrores = new File(carpetaResultados, "Errores_" + vocal + ".txt");
    }

    /**
     * @return true si el proceso terminó con código 0 y se leyó su recuento
     */
    public boolean esCorrecto() {
        return codigoSalida != null && codigoSalida == 0 && apariciones >= 0;
    }

    /**
     * @return milisegundos desde que se lanzó hasta que se detectó su fin
     */
    public long getDuracionMs() {
        if (finNanos == 0) {
            return 0;
        }
        return (finNanos - inicioNanos) / 1_000_000;
    }

    public String getPidTexto() {
        if (proceso == null) {
            return "-";
        }
        return String.valueOf(proceso.pid());
    }

    public String getCodigoTexto() {
        if (codigoSalida == null) {
            return "-";
        }
        return String.valueOf(codigoSalida);
    }

    public String getVocal() {
        return vocal;
    }

    public File getFicheroResultado() {
        return ficheroResultado;
    }

    public File getFicheroErrores() {
        return ficheroErrores;
    }

    public Process getProceso() {
        return proceso;
    }

    public void setProceso(Process proceso) {
        this.proceso = proceso;
    }

    public RegistroEjecucion getRegistro() {
        return registro;
    }

    public void setRegistro(RegistroEjecucion registro) {
        this.registro = registro;
    }

    public void setInicioNanos(long inicioNanos) {
        this.inicioNanos = inicioNanos;
    }

    public void setFinNanos(long finNanos) {
        this.finNanos = finNanos;
    }

    public Integer getCodigoSalida() {
        return codigoSalida;
    }

    public void setCodigoSalida(Integer codigoSalida) {
        this.codigoSalida = codigoSalida;
    }

    public long getApariciones() {
        return apariciones;
    }

    public void setApariciones(long apariciones) {
        this.apariciones = apariciones;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
