package bo.bosque.com.impexpap.commons;

/**
 * Pantallas del módulo Tickets / Bingo ({@code tb_vista.direccion}, contrato «Acceso por pantalla»).
 * Se exigen con {@link AccesoPantalla}; el administrador pasa siempre.
 */
public final class TtkPantallas {

    /** RRHH: eventos, tipos de vale, participantes e impresión de tickets. */
    public static final String EVENTOS = "ttkEventos/Eventos";
    /** Encargados: parámetros, figuras, generación de cartones y estados del bingo. */
    public static final String CONFIG = "ttkBingo/Configuracion";
    /** Encargados: listar, asignar, liberar e imprimir cartones (sin tocar la configuración). */
    public static final String CARTONES = "ttkBingo/Cartones";
    /** Encargados: panel en vivo (marcar bolillos, ranking, ganadores). */
    public static final String PANEL = "ttkBingo/Panel";
    /** Todos los empleados: elegir e imprimir su cartón. */
    public static final String MI_CARTON = "ttkBingo/MiCarton";
    /** Todos los empleados: tablero en vivo, sin ganadores. */
    public static final String EN_VIVO = "ttkBingo/EnVivo";

    private TtkPantallas() {
    }

    /** Pantallas que pueden ver el estado público del bingo (tópico /publico y estado-publico). */
    public static String[] publico() {
        return new String[] { EN_VIVO, MI_CARTON, CONFIG, PANEL };
    }

    /** Pantallas de los encargados (listar bingos). */
    public static String[] encargado() {
        return new String[] { CONFIG, PANEL, CARTONES };
    }

    /** Pantallas que manejan cartones con su dueño: Configuración (resumen e impresión) y Cartones. */
    public static String[] cartones() {
        return new String[] { CONFIG, CARTONES };
    }
}
