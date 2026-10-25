package bo.bosque.com.impexpap.commons;

/**
 * Pantallas del módulo Tickets / Bingo ({@code tb_vista.direccion}, contrato «Acceso por pantalla»).
 * Se exigen con {@link AccesoPantalla}; el administrador pasa siempre.
 */
public final class TtkPantallas {

    /** RRHH: eventos, tipos de vale, participantes e impresión de tickets. */
    public static final String EVENTOS = "ttkEventos/Eventos";
    /** Encargados: configuración del bingo, cartones y su asignación. */
    public static final String CONFIG = "ttkBingo/Configuracion";
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
        return new String[] { CONFIG, PANEL };
    }
}
