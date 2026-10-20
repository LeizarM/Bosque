package bo.bosque.com.impexpap.utils;

/**
 * Horas ({@code time(7)}) del módulo como texto {@code HH:mm:ss}: jTDS las entrega como texto con siete decimales que
 * {@code getTime} no convierte, así que viajan como {@code String} (igual que en {@code SolicitudPermiso}) y se
 * recortan aquí. Al SP van como texto y SQL Server las convierte.
 */
public final class TcpeHora {

    /** {@code HH:mm} o {@code HH:mm:ss}, ya normalizado. */
    public static final String PATRON = "^([01]\\d|2[0-3]):[0-5]\\d(:[0-5]\\d)?$";

    public static final String MENSAJE = "La hora debe tener el formato HH:mm o HH:mm:ss.";

    private TcpeHora() {
    }

    /**
     * Quita espacios y la fracción de segundo; vacío pasa a null. Lo que no tenga forma de hora se deja tal cual
     * para que la validación lo rechace con su mensaje, en vez de corregirlo en silencio.
     */
    public static String normalizar(String hora) {
        if (hora == null) return null;
        String h = hora.trim();
        if (h.isEmpty()) return null;
        int punto = h.indexOf('.');
        if (punto > 0 && h.substring(0, punto).matches("\\d{2}:\\d{2}:\\d{2}")) {
            h = h.substring(0, punto);
        }
        return h;
    }
}
