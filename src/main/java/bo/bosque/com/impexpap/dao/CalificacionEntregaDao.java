package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ResumenCalificacionDTO;
import bo.bosque.com.impexpap.model.CalificacionEntrega;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DAO de las calificaciones de entrega (tabla trch_EntregaCalificacion).
 *
 * <p>=====================================================================================<br>
 * POR QUE NINGUN CATCH DE ESTA CLASE HACE {@code this.jdbcTemplate = null;}<br>
 * =====================================================================================</p>
 *
 * <p>Los metodos historicos de {@code EntregaChoferDao} anulan el campo {@code jdbcTemplate} dentro
 * del catch. Este DAO es un singleton de Spring y ese campo es LA UNICA referencia al JdbcTemplate
 * inyectado: ponerlo en null no "resetea" nada, deja el bean muerto para siempre, y a partir de ahi
 * cualquier llamada posterior revienta con NullPointerException hasta reiniciar la aplicacion.</p>
 *
 * <p>Aca el problema seria peor todavia que alla, por donde entran las llamadas: la ACCION 'I' se
 * invoca colgada del aviso de WhatsApp que a su vez cuelga del registro de entrega del chofer, y la
 * ACCION 'P' se invoca desde el webhook PUBLICO, al que le puede escribir cualquiera. Un solo
 * mensaje de WhatsApp mal formado bastaria para dejar el DAO inservible. Por eso, en toda la clase:
 * se atrapa {@link DataAccessException} (no solo BadSqlGrammarException, que dejaria pasar los
 * RAISERROR y los timeouts del SP) mas {@link RuntimeException} como red final, se loguea con slf4j
 * y se devuelve false / null / lista vacia. El campo {@code jdbcTemplate} no se toca NUNCA.</p>
 *
 * <p>Cada metodo arranca ademas con una guarda defensiva por si OTRO DAO del proyecto ya dejo el
 * JdbcTemplate en null; en ese caso se saltea la operacion en silencio en vez de propagar un NPE.</p>
 *
 * <p>=====================================================================================<br>
 * CONTRATO CON LOS STORED PROCEDURES (leer antes de escribir el SQL)<br>
 * =====================================================================================</p>
 *
 * <p><b>1) p_abm_trch_EntregaCalificacion NO DEBE LLEVAR {@code SET NOCOUNT ON}.</b> Este es el
 * punto mas facil de romper y el mas dificil de diagnosticar. Los metodos de escritura devuelven
 * {@code jdbcTemplate.update(...) != 0}, o sea que dependen de que el conteo de filas afectadas
 * llegue al driver JDBC. Con NOCOUNT activo ese conteo nunca sale, {@code update()} devuelve 0 y
 * {@link #registrarCalificacion(CalificacionEntrega, String)} informa false AUNQUE EL INSERT HAYA
 * FUNCIONADO: la fila queda escrita en la base pero el servicio cree que fallo. No es una teoria,
 * es la razon por la que el p_abm_trch_Entregas que ya esta en produccion no lo tiene (verificado
 * contra la definicion viva del servidor), mientras que los p_list_ si lo llevan.</p>
 *
 * <p><b>2) Los parametros se pasan POR NOMBRE</b>, con exactamente estos nombres. El SP puede
 * declararlos en el orden que quiera y con default NULL, pero los nombres tienen que coincidir:</p>
 * <pre>
 *   p_abm_trch_EntregaCalificacion:
 *     &#64;idCalificacion &#64;docEntry &#64;db &#64;cardCode &#64;cardName &#64;uChofer &#64;nombreChofer
 *     &#64;puntaje &#64;comentario &#64;chatId &#64;mensajeCrudo &#64;fechaEntrega &#64;fechaRespuesta &#64;ACCION
 *
 *   p_list_trch_EntregaCalificacion:
 *     ACCION 'P' -> &#64;chatId &#64;horasVentana
 *     ACCION 'L' -> &#64;fechaIni &#64;fechaFin &#64;uChofer
 *     ACCION 'R' -> &#64;fechaIni &#64;fechaFin
 * </pre>
 *
 * <p><b>3) Las columnas de los SELECT se leen POR INDICE</b>, en el orden en que estan declarados
 * los campos de {@link CalificacionEntrega} (14 columnas) y de {@link ResumenCalificacionDTO}
 * (10 columnas). Ver {@link #MAPPER_CALIFICACION} y {@link #MAPPER_RESUMEN}. Agregar, quitar o
 * reordenar una columna obliga a tocar los tres lugares: el SP, el mapper y la clase.</p>
 *
 * <p><b>4) Largo minimo de las columnas de texto libre:</b> {@code comentario} al menos
 * VARCHAR({@value #LARGO_MAX_COMENTARIO}) y {@code mensajeCrudo} al menos
 * VARCHAR({@value #LARGO_MAX_MENSAJE_CRUDO}). Este DAO recorta los dos campos a esos largos antes
 * de mandarlos, porque los escribe el cliente por WhatsApp y no tienen tope: sin el recorte, un
 * mensaje largo haria fallar el SP con "String or binary data would be truncated" y se perderia la
 * calificacion entera. Si en el SQL se declaran columnas mas cortas que estas constantes, el error
 * vuelve.</p>
 *
 * <p><b>5) Interpretacion de los filtros:</b> {@code @fechaIni} / {@code @fechaFin} llegan como
 * datetime y pueden venir en NULL, que significa "sin limite" de ese lado. El SP es el responsable
 * de que el rango sea inclusivo en el extremo superior (por ejemplo comparando contra
 * {@code DATEADD(day, 1, @fechaFin)}), porque el frontend manda las fechas a las 00:00 y de lo
 * contrario el ultimo dia del rango quedaria afuera. {@code @uChofer} llega en 0 cuando el reporte
 * es de todos los choferes.</p>
 */
@Repository
public class CalificacionEntregaDao implements ICalificacionEntrega {

    private static final Logger logger = LoggerFactory.getLogger(CalificacionEntregaDao.class);

    /**
     * Largo maximo del comentario del cliente. Ver el punto 4 del contrato con los SPs: el texto se
     * recorta aca para que un mensaje largo no haga fallar el INSERT entero.
     */
    private static final int LARGO_MAX_COMENTARIO = 500;

    /** Largo maximo del texto crudo del mensaje entrante. Mismo motivo que {@link #LARGO_MAX_COMENTARIO}. */
    private static final int LARGO_MAX_MENSAJE_CRUDO = 1000;

    // Los nombres tienen que coincidir LETRA POR LETRA con la firma de los SPs de
    // 28_calificacion_entregas.sql. SQL Server no falla en compilacion por esto: rechaza el
    // EXEC en tiempo de ejecucion con "@x is not a parameter for procedure", y como el DAO
    // atrapa la excepcion y devuelve false, la calificacion se perdia en silencio.
    //
    // Es @u_Chofer con guion bajo, igual que la columna de trch_Entregas. Y NO existe
    // @nombreChofer: el nombre del chofer no se guarda en la tabla, se resuelve por join
    // contra RRHH en la ACCION 'R' del listado.
    private static final String SQL_ABM =
            "execute p_abm_trch_EntregaCalificacion @idCalificacion = ?, @docEntry = ?, @db = ?, "
          + "@cardCode = ?, @cardName = ?, @u_Chofer = ?, @puntaje = ?, "
          + "@comentario = ?, @chatId = ?, @mensajeCrudo = ?, @fechaEntrega = ?, "
          + "@fechaRespuesta = ?, @ACCION = ?";

    private static final String SQL_PENDIENTE =
            "execute p_list_trch_EntregaCalificacion @chatId = ?, @horas = ?, @ACCION = ?";

    private static final String SQL_LISTADO =
            "execute p_list_trch_EntregaCalificacion @fechaIni = ?, @fechaFin = ?, @u_Chofer = ?, @ACCION = ?";

    private static final String SQL_RESUMEN =
            "execute p_list_trch_EntregaCalificacion @fechaIni = ?, @fechaFin = ?, @ACCION = ?";

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * RowMapper de una fila de trch_EntregaCalificacion. Lee POR INDICE, en el orden de los campos
     * de {@link CalificacionEntrega}.
     *
     * <p>Columnas: 1 idCalificacion, 2 docEntry, 3 db, 4 cardCode, 5 cardName, 6 uChofer,
     * 7 nombreChofer, 8 puntaje, 9 comentario, 10 chatId, 11 mensajeCrudo, 12 fechaEntrega,
     * 13 fechaRespuesta, 14 audFecha.</p>
     *
     * <p>A diferencia del mapper de notificaciones de {@code EntregaChoferDao}, aca los numericos
     * NO se leen con {@code rs.getInt()} pelado sino con helpers que consultan {@code wasNull()}:
     * en esta tabla el NULL tiene significado propio. Una encuesta sin responder tiene
     * {@code puntaje IS NULL}, y {@code rs.getInt()} devuelve 0 para NULL, con lo cual una encuesta
     * pendiente se leeria como una calificacion de cero estrellas. El SP tampoco puede taparlo con
     * ISNULL por el mismo motivo.</p>
     */
    private static final RowMapper<CalificacionEntrega> MAPPER_CALIFICACION = (rs, rowNum) -> {
        CalificacionEntrega temp = new CalificacionEntrega();

        temp.setIdCalificacion( leerLong(rs, 1 ) );
        temp.setDocEntry( leerLong(rs, 2 ) );
        temp.setDb( rs.getString(3 ) );
        temp.setCardCode( rs.getString(4 ) );
        temp.setCardName( rs.getString(5 ) );
        temp.setUChofer( leerInteger(rs, 6 ) );
        temp.setNombreChofer( rs.getString(7 ) );
        temp.setPuntaje( leerInteger(rs, 8 ) );
        temp.setComentario( rs.getString(9 ) );
        temp.setChatId( rs.getString(10 ) );
        temp.setMensajeCrudo( rs.getString(11 ) );
        temp.setFechaEntrega( leerFecha(rs, 12 ) );
        temp.setFechaRespuesta( leerFecha(rs, 13 ) );
        temp.setAudFecha( leerFecha(rs, 14 ) );

        return temp;
    };

    /**
     * RowMapper del reporte agregado (ACCION 'R'). Lee POR INDICE, en el orden de los campos de
     * {@link ResumenCalificacionDTO}.
     *
     * <p>Columnas: 1 uChofer, 2 nombreChofer, 3 totalEnviadas, 4 totalRespondidas, 5 promedio,
     * 6 c1, 7 c2, 8 c3, 9 c4, 10 c5.</p>
     *
     * <p>{@code promedio} se lee con {@code wasNull()} porque el AVG de un chofer al que nadie le
     * respondio es NULL, y eso hay que mostrarlo como "sin datos" y no como un 0,0.</p>
     */
    private static final RowMapper<ResumenCalificacionDTO> MAPPER_RESUMEN = (rs, rowNum) -> {
        ResumenCalificacionDTO temp = new ResumenCalificacionDTO();

        temp.setUChofer( leerInteger(rs, 1 ) );
        temp.setNombreChofer( rs.getString(2 ) );
        temp.setTotalEnviadas( leerInteger(rs, 3 ) );
        temp.setTotalRespondidas( leerInteger(rs, 4 ) );
        temp.setPromedio( leerDouble(rs, 5 ) );
        temp.setC1( leerInteger(rs, 6 ) );
        temp.setC2( leerInteger(rs, 7 ) );
        temp.setC3( leerInteger(rs, 8 ) );
        temp.setC4( leerInteger(rs, 9 ) );
        temp.setC5( leerInteger(rs, 10 ) );

        return temp;
    };


    /**
     * Escribe una calificacion (ACCION 'I' = alta pendiente, 'U' = respuesta del cliente).
     *
     * NUNCA lanza excepcion.
     *
     * @param mb la calificacion
     * @param accion 'I' o 'U'
     * @return true si el SP afecto al menos una fila
     */
    @Override
    public boolean registrarCalificacion( CalificacionEntrega mb, String accion ) {

        if ( this.jdbcTemplate == null ) {
            logger.warn("registrarCalificacion: jdbcTemplate esta en null (lo anulo un metodo de otro DAO). Se omite la escritura de la calificacion.");
            return false;
        }

        if ( mb == null ) {
            logger.warn("registrarCalificacion: se recibio una calificacion null (ACCION={}). No hay nada que escribir.", accion);
            return false;
        }

        if ( accion == null || accion.trim().isEmpty() ) {
            logger.warn("registrarCalificacion: ACCION vacia para docEntry={}, db={}. Se omite la escritura.", mb.getDocEntry(), mb.getDb());
            return false;
        }

        String acc = accion.trim();

        // Los dos campos que escribe el cliente por WhatsApp no tienen tope de largo. Se recortan
        // aca y no en el SP: si llegaran enteros, SQL Server abortaria el statement con "String or
        // binary data would be truncated" y se perderia la calificacion completa, comentario y
        // puntaje incluidos, por culpa de un cliente que escribio de mas.
        String comentario = recortar( mb.getComentario(), LARGO_MAX_COMENTARIO );
        String mensajeCrudo = recortar( mb.getMensajeCrudo(), LARGO_MAX_MENSAJE_CRUDO );

        try {
            Object[] args = new Object[] {
                    mb.getIdCalificacion(),
                    mb.getDocEntry(),
                    mb.getDb(),
                    mb.getCardCode(),
                    mb.getCardName(),
                    mb.getUChofer(),
                    mb.getPuntaje(),
                    comentario,
                    mb.getChatId(),
                    mensajeCrudo,
                    mb.getFechaEntrega(),
                    mb.getFechaRespuesta(),
                    acc
            };

            int[] tipos = new int[] {
                    Types.BIGINT,       // idCalificacion
                    Types.BIGINT,       // docEntry
                    Types.VARCHAR,      // db
                    Types.VARCHAR,      // cardCode
                    Types.VARCHAR,      // cardName
                    Types.INTEGER,      // u_Chofer
                    Types.INTEGER,      // puntaje
                    Types.VARCHAR,      // comentario
                    Types.VARCHAR,      // chatId
                    Types.VARCHAR,      // mensajeCrudo
                    Types.TIMESTAMP,    // fechaEntrega
                    Types.TIMESTAMP,    // fechaRespuesta
                    Types.VARCHAR       // ACCION
            };

            // Se usa el overload con array de tipos, y no un PreparedStatementSetter con setInt/setLong
            // como los metodos historicos, justamente porque aca los numericos son wrappers que pueden
            // venir en null (puntaje de una encuesta pendiente, idCalificacion de un alta). Con el tipo
            // declarado, Spring emite un setNull correcto; un ps.setInt(mb.getPuntaje()) seria un NPE.
            int resp = this.jdbcTemplate.update( SQL_ABM, args, tipos );

            if ( resp == 0 ) {
                logger.warn("registrarCalificacion: el SP no afecto ninguna fila (ACCION={}, idCalificacion={}, docEntry={}, db={}, chatId={}). "
                          + "Si la fila si deberia haberse escrito, revisar que p_abm_trch_EntregaCalificacion NO tenga SET NOCOUNT ON: "
                          + "con NOCOUNT activo el conteo de filas nunca llega al driver, este metodo devuelve false y la calificacion "
                          + "queda escrita en la base pero informada como fallida.",
                          acc, mb.getIdCalificacion(), mb.getDocEntry(), mb.getDb(), mb.getChatId());
            }

            return resp != 0;

        } catch ( DataAccessException e ) {
            logger.error("Error de BD en registrarCalificacion (ACCION={}, docEntry={}, db={}, chatId={}): {}",
                    acc, mb.getDocEntry(), mb.getDb(), mb.getChatId(), e.getMessage(), e);
            return false;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en registrarCalificacion (ACCION={}, docEntry={}, db={}, chatId={}): {}",
                    acc, mb.getDocEntry(), mb.getDb(), mb.getChatId(), e.getMessage(), e);
            return false;
        }
    }


    /**
     * Busca la encuesta PENDIENTE mas reciente de un chat (ACCION 'P').
     *
     * NUNCA lanza excepcion.
     *
     * @param chatId chat normalizado 591XXXXXXXX@c.us
     * @param horasVentana antiguedad maxima de la entrega, en horas
     * @return la calificacion pendiente, o null
     */
    @Override
    public CalificacionEntrega buscarPendientePorChat( String chatId, int horasVentana ) {

        if ( this.jdbcTemplate == null ) {
            logger.warn("buscarPendientePorChat: jdbcTemplate esta en null (lo anulo un metodo de otro DAO). Se omite la busqueda.");
            return null;
        }

        // Entra desde el webhook publico: que llegue vacio o basura es esperable, no es un error.
        if ( chatId == null || chatId.trim().isEmpty() ) {
            logger.debug("buscarPendientePorChat: llego un chatId vacio. Se ignora el mensaje.");
            return null;
        }

        String chat = chatId.trim();

        if ( horasVentana <= 0 ) {
            logger.warn("buscarPendientePorChat: la ventana configurada es de {} horas, o sea que ninguna encuesta puede estar vigente. "
                      + "Revisar openwa.entregas.calificacion-ventana-horas. Se ignora el mensaje de chatId={}.",
                      Integer.valueOf(horasVentana), chat);
            return null;
        }

        try {
            // chatId es dato NO CONFIABLE (viene de afuera, por el webhook). Va siempre como
            // parametro enlazado; jamas concatenado al SQL.
            List<CalificacionEntrega> lstTemp = this.jdbcTemplate.query( SQL_PENDIENTE,
                    new Object[] { chat, Integer.valueOf(horasVentana), "P" },
                    new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR },
                    MAPPER_CALIFICACION );

            if ( lstTemp == null || lstTemp.isEmpty() ) {
                // Caso normal y mayoritario: el cliente escribio cualquier otra cosa. Va en debug
                // para no llenar el log con un warn por cada mensaje entrante.
                logger.debug("buscarPendientePorChat: no hay encuesta pendiente para chatId={} dentro de las ultimas {} horas.",
                        chat, Integer.valueOf(horasVentana));
                return null;
            }

            // El SP deberia devolver una sola fila (la mas reciente). Si devuelve varias es que el
            // mismo cliente tiene mas de una entrega sin calificar en la ventana: se toma la primera,
            // que por contrato viene ordenada por fechaEntrega DESC, y se deja constancia.
            if ( lstTemp.size() > 1 ) {
                logger.warn("buscarPendientePorChat: chatId={} tiene {} encuestas pendientes en las ultimas {} horas; "
                          + "se aplica la respuesta a la mas reciente (idCalificacion={}).",
                          chat, Integer.valueOf(lstTemp.size()), Integer.valueOf(horasVentana),
                          lstTemp.get(0).getIdCalificacion());
            }

            return lstTemp.get(0);

        } catch ( DataAccessException e ) {
            logger.error("Error de BD en buscarPendientePorChat (chatId={}, horasVentana={}): {}",
                    chat, Integer.valueOf(horasVentana), e.getMessage(), e);
            return null;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en buscarPendientePorChat (chatId={}, horasVentana={}): {}",
                    chat, Integer.valueOf(horasVentana), e.getMessage(), e);
            return null;
        }
    }


    /**
     * Listado de calificaciones para el reporte de detalle (ACCION 'L').
     *
     * NUNCA lanza excepcion.
     *
     * @param desde inicio del rango, o null
     * @param hasta fin del rango, o null
     * @param uChofer codigo del chofer, o 0 para todos
     * @return la lista, o una lista vacia
     */
    @Override
    public List<CalificacionEntrega> listarCalificaciones( Date desde, Date hasta, int uChofer ) {

        List<CalificacionEntrega> lstTemp = new ArrayList<CalificacionEntrega>();

        if ( this.jdbcTemplate == null ) {
            logger.warn("listarCalificaciones: jdbcTemplate esta en null (lo anulo un metodo de otro DAO). Se devuelve lista vacia.");
            return lstTemp;
        }

        try {
            lstTemp = this.jdbcTemplate.query( SQL_LISTADO,
                    new Object[] { desde, hasta, Integer.valueOf(uChofer), "L" },
                    new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER, Types.VARCHAR },
                    MAPPER_CALIFICACION );

        } catch ( DataAccessException e ) {
            logger.error("Error de BD en listarCalificaciones (desde={}, hasta={}, uChofer={}): {}",
                    desde, hasta, Integer.valueOf(uChofer), e.getMessage(), e);
            lstTemp = new ArrayList<CalificacionEntrega>();
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en listarCalificaciones (desde={}, hasta={}, uChofer={}): {}",
                    desde, hasta, Integer.valueOf(uChofer), e.getMessage(), e);
            lstTemp = new ArrayList<CalificacionEntrega>();
        }

        return lstTemp;
    }


    /**
     * Reporte agregado por chofer (ACCION 'R').
     *
     * NUNCA lanza excepcion.
     *
     * @param desde inicio del rango, o null
     * @param hasta fin del rango, o null
     * @return una fila por chofer, o una lista vacia
     */
    @Override
    public List<ResumenCalificacionDTO> resumenPorChofer( Date desde, Date hasta ) {

        List<ResumenCalificacionDTO> lstTemp = new ArrayList<ResumenCalificacionDTO>();

        if ( this.jdbcTemplate == null ) {
            logger.warn("resumenPorChofer: jdbcTemplate esta en null (lo anulo un metodo de otro DAO). Se devuelve lista vacia.");
            return lstTemp;
        }

        try {
            lstTemp = this.jdbcTemplate.query( SQL_RESUMEN,
                    new Object[] { desde, hasta, "R" },
                    new int[] { Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR },
                    MAPPER_RESUMEN );

        } catch ( DataAccessException e ) {
            logger.error("Error de BD en resumenPorChofer (desde={}, hasta={}): {}",
                    desde, hasta, e.getMessage(), e);
            lstTemp = new ArrayList<ResumenCalificacionDTO>();
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en resumenPorChofer (desde={}, hasta={}): {}",
                    desde, hasta, e.getMessage(), e);
            lstTemp = new ArrayList<ResumenCalificacionDTO>();
        }

        return lstTemp;
    }


    /* =====================================================================================
     * HELPERS DE LECTURA NULL-SAFE
     * -------------------------------------------------------------------------------------
     * rs.getInt() / rs.getLong() / rs.getDouble() devuelven 0 cuando la columna es NULL, y no hay
     * forma de distinguir ese 0 de un 0 real sin preguntar despues por rs.wasNull(). En esta tabla
     * esa diferencia es justamente la que importa: puntaje NULL = "el cliente todavia no contesto",
     * puntaje 0 = una nota de cero que no existe en la escala 1..5. Lo mismo con el promedio del
     * resumen, que es NULL cuando el chofer no tiene ninguna respuesta.
     * ===================================================================================== */

    /** Lee un BIGINT que puede ser NULL. */
    private static Long leerLong( ResultSet rs, int indice ) throws SQLException {
        long valor = rs.getLong( indice );
        return rs.wasNull() ? null : Long.valueOf( valor );
    }

    /** Lee un INT que puede ser NULL. */
    private static Integer leerInteger( ResultSet rs, int indice ) throws SQLException {
        int valor = rs.getInt( indice );
        return rs.wasNull() ? null : Integer.valueOf( valor );
    }

    /** Lee un numerico decimal que puede ser NULL. */
    private static Double leerDouble( ResultSet rs, int indice ) throws SQLException {
        double valor = rs.getDouble( indice );
        return rs.wasNull() ? null : Double.valueOf( valor );
    }

    /**
     * Lee un datetime que puede ser NULL y lo devuelve como java.util.Date.
     *
     * Se convierte a java.util.Date en vez de devolver el java.sql.Timestamp tal cual (que es
     * subclase suya y compilaria igual) porque Timestamp.equals(Date) no es simetrico: comparar los
     * dos tipos da resultados distintos segun de que lado este cada uno. Devolver siempre el tipo
     * declarado del campo evita esa trampa.
     */
    private static Date leerFecha( ResultSet rs, int indice ) throws SQLException {
        Timestamp ts = rs.getTimestamp( indice );
        return ts == null ? null : new Date( ts.getTime() );
    }

    /** Recorta un texto al largo maximo indicado. Devuelve null si el valor es null. */
    private static String recortar( String valor, int largoMaximo ) {
        if ( valor == null ) {
            return null;
        }
        if ( valor.length() <= largoMaximo ) {
            return valor;
        }
        return valor.substring( 0, largoMaximo );
    }

}
