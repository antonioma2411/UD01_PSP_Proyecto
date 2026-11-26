package es.ilerna.psp.gmm.procesos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Datos de UNA ejecución para el historial (M3.4).
 *
 * Guarda: PID (si existe), comando, hora de inicio y de fin, duración,
 * código de salida (si existe) y estado final.
 */
public class RegistroEjecucion {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** -1 si el proceso no llegó a arrancar y por tanto no tiene PID. */
    private long pid = -1;
    private String comando;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    /** null si no hay código de salida (el proceso no arrancó). */
    private Integer codigoSalida;
    private EstadoEjecucion estado;

    /**
     * Crea el registro y apunta la hora de inicio.
     *
     * @param comando comando en forma de texto, solo para mostrarlo
     */
    public RegistroEjecucion(String comando) {
        this.comando = comando;
        this.inicio = LocalDateTime.now();
    }

    /**
     * Apunta la hora de fin, el código de salida y el estado final.
     *
     * @param codigoSalida código del proceso o null si no existe
     * @param estado       estado final
     */
    public void finalizar(Integer codigoSalida, EstadoEjecucion estado) {
        this.fin = LocalDateTime.now();
        this.codigoSalida = codigoSalida;
        this.estado = estado;
    }

    /**
     * @return milisegundos entre el inicio y el fin (0 si aún no ha terminado)
     */
    public long getDuracionMs() {
        if (fin == null) {
            return 0;
        }
        return Duration.between(inicio, fin).toMillis();
    }

    public String getPidTexto() {
        if (pid < 0) {
            return "-";
        }
        return String.valueOf(pid);
    }

    public String getCodigoTexto() {
        if (codigoSalida == null) {
            return "-";
        }
        return String.valueOf(codigoSalida);
    }

    public String getInicioTexto() {
        return inicio.format(FORMATO_HORA);
    }

    public String getFinTexto() {
        if (fin == null) {
            return "-";
        }
        return fin.format(FORMATO_HORA);
    }

    /**
     * Línea completa para el fichero de log, con la fecha incluida.
     * Ejemplo: 2026-11-26 | 10:00:01.120 -> 10:00:01.480 | 360 ms | PID 1234 | codigo 0 | COMPLETADO | hostname
     *
     * @return texto de una línea
     */
    public String toLineaLog() {
        return inicio.format(FORMATO_FECHA)
                + " | " + getInicioTexto() + " -> " + getFinTexto()
                + " | " + getDuracionMs() + " ms"
                + " | PID " + getPidTexto()
                + " | codigo " + getCodigoTexto()
                + " | " + estado
                + " | " + comando;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getComando() {
        return comando;
    }

    public EstadoEjecucion getEstado() {
        return estado;
    }
}
