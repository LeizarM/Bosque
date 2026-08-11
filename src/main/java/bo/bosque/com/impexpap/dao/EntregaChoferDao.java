package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.EntregaNotificacionDTO;
import bo.bosque.com.impexpap.dto.PedidoPendienteEntregaDTO;
import bo.bosque.com.impexpap.model.EntregaChofer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Repository
public class EntregaChoferDao implements IEntregaChofer {

    /*
     * NOTA — se quitaron 6 asignaciones "this.jdbcTemplate = null;" que estaban en los catch
     * de los metodos historicos.
     *
     * Este DAO es un @Repository singleton y ese campo es la UNICA referencia al JdbcTemplate
     * inyectado. Anularlo no "resetea la conexion": deja el bean muerto para toda la vida de la
     * JVM. Desde esa linea en adelante, cualquier metodo hace this.jdbcTemplate.query(...) sobre
     * null -> NullPointerException, que NO es BadSqlGrammarException, escapa del catch y sube.
     *
     * El vector mas silencioso era el cron de las 23:58:59 (DatabaseTaskScheduler, ACCION 'C'):
     * si envenenaba el DAO, su propio catch no se enteraba —el DAO ya se habia tragado la
     * excepcion y devuelto false— y lo unico que quedaba era un System.out.println. El modulo
     * moria de noche, sin una linea de ERROR, y el incidente aparecia a la mañana siguiente
     * cuando ningun chofer podia abrir la app. La causa quedaba 10 horas atras en el log.
     *
     * JdbcTemplate no tiene estado que reciclar: las conexiones las administra el pool de Hikari.
     * Sacando la asignacion, un error de SQL vuelve a ser lo que siempre debio ser: transitorio.
     */

    private static final Logger logger = LoggerFactory.getLogger(EntregaChoferDao.class);

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * RowMapper de la consulta de notificacion (ACCION 'G' de p_list_trch_Entregas).
     * Devuelve 14 columnas y aca se leen POR INDICE, igual que en el resto de la clase. Si se toca el orden de columnas en
     * src/main/resources/sql/27_notificacion_entregas.sql hay que tocar este mapper.
     *
     * Columnas: 1 docEntry, 2 db, 3 docNum, 4 factura, 5 cardCode, 6 cardName, 7 vendedor,
     *           8 uChofer, 9 nombreChofer, 10 direccionEntrega, 11 fechaEntrega (String
     *           'dd/MM/yyyy HH:mm' ya formateado por el SP), 12 obs, 13 cantidadItems, 14 tipo.
     */
    private static final RowMapper<EntregaNotificacionDTO> MAPPER_NOTIFICACION = (rs, rowNum) -> {
        EntregaNotificacionDTO temp = new EntregaNotificacionDTO();

        temp.setDocEntry( rs.getLong(1 ) );
        temp.setDb( rs.getString(2 ) );
        temp.setDocNum( rs.getInt(3 ) );
        temp.setFactura( rs.getInt(4 ) );
        temp.setCardCode( rs.getString(5 ) );
        temp.setCardName( rs.getString(6 ) );
        temp.setVendedor( rs.getString(7 ) );
        temp.setUChofer( rs.getInt(8 ) );
        temp.setNombreChofer( rs.getString(9 ) );
        temp.setDireccionEntrega( rs.getString(10 ) );
        temp.setFechaEntrega( rs.getString(11 ) );
        temp.setObs( rs.getString(12 ) );
        temp.setCantidadItems( rs.getInt(13 ) );
        temp.setTipo( rs.getString(14 ) );

        return temp;
    };

    /**
     * Registrar entrega de chofer
     *
     * @param mb
     * @param acc
     * @return
     */
    /**
     * {@inheritDoc}
     *
     * <p>Se conserva por compatibilidad con el scheduler (ACCION 'C') y con
     * {@code /registro-inicio-fin-entrega} (ACCION 'I'), donde "afecto 0 filas" no tiene un
     * significado propio. Para la ACCION 'B' usar {@link #registrarEntregaChoferFilas}, que
     * distingue los dos casos.
     */
    @Override
    public boolean registrarEntregaChofer(EntregaChofer mb, String acc) {
        return registrarEntregaChoferFilas(mb, acc) > 0;
    }

    /**
     * Igual que {@link #registrarEntregaChofer} pero devolviendo el conteo real de filas.
     *
     * <p><b>Por que hace falta.</b> El booleano colapsa dos cosas que no son lo mismo:
     * "el SP corrio y no encontro nada que actualizar" y "el SP exploto". Mientras la ACCION 'B'
     * actualizaba por (docEntry, db) sin exigir que la fila estuviera pendiente, esa diferencia
     * daba igual. Al agregarle {@code AND ISNULL(fueEntregado,0) = 0} —que es lo que corta el
     * pisado de documentos ajenos— re-marcar una entrega ya marcada pasa a afectar 0 filas, y
     * con el booleano eso se convertia en un 500 sobre una entrega que estaba PERFECTAMENTE
     * registrada. El chofer veria un error rojo por tocar dos veces el mismo boton.
     *
     * @return filas afectadas (0 = el SP corrio pero no encontro nada), o -1 si hubo excepcion
     */
    @Override
    public int registrarEntregaChoferFilas(EntregaChofer mb, String acc) {

        int resp;
        try{
            resp = this.jdbcTemplate.update("execute p_abm_trch_Entregas  @idEntrega = ?, @docEntry = ?, @docNum = ?, @factura = ?, @docDate = ?, @docTime  =? , @cardCode = ?, @cardName = ?, @addressEntregaFac = ?, @addressEntregaMat = ?, @vendedor = ?, @u_Chofer = ?, @itemCode = ?, @dscription = ?, @whsCode = ?, @quantity = ?, @openQty = ?, @db = ?, @valido = ?, @fueEntregado = ?, @fechaEntrega = ?, @latitud =? , @longitud = ?, @direccionEntrega = ?, @obs = ?, @codSucursalChofer=?, @codCiudadChofer=?  ,@audUsuario = ?, @ACCION = ?",
                    ps ->{
                        ps.setInt(1,mb.getIdEntrega());
                        ps.setInt(2,mb.getDocEntry());
                        ps.setInt(3,mb.getDocNum());
                        ps.setInt(4,mb.getFactura());
                        ps.setString(5,  mb.getDocDate());
                        ps.setString(6, mb.getDocTime());
                        ps.setString(7, mb.getCardCode());
                        ps.setString(8, mb.getCardName());
                        ps.setString(9, mb.getAddressEntregaFac());
                        ps.setString(10, mb.getAddressEntregaMat());
                        ps.setString(11, mb.getVendedor());
                        ps.setInt(12, mb.getUChofer());
                        ps.setString(13, mb.getItemCode());
                        ps.setString(14, mb.getDscription());
                        ps.setString(15, mb.getWhsCode());
                        ps.setFloat(16, mb.getQuantity());
                        ps.setFloat(17, mb.getOpenQty());
                        ps.setString(18, mb.getDb());
                        ps.setString(19, mb.getValido());
                        ps.setInt(20, mb.getFueEntregado());
                        ps.setString(21,  mb.getFechaEntrega());
                        ps.setFloat(22, mb.getLatitud());
                        ps.setFloat(23, mb.getLongitud());
                        ps.setString(24, mb.getDireccionEntrega());
                        ps.setString(25, mb.getObs());
                        ps.setInt(26, mb.getCodSucursalChofer());
                        ps.setInt(27, mb.getCodCiudadChofer());
                        ps.setInt(28, mb.getAudUsuario());
                        ps.setString(29, acc);
                    });
        }catch (BadSqlGrammarException e){
            logger.error("Error de SQL en registrarEntregaChofer (ACCION={}): {} · SQL Code {}",
                         acc, e.getMessage(),
                         (e.getCause() instanceof SQLException) ? ((SQLException) e.getCause()).getErrorCode() : -1);
            return -1;
        }

        return resp;
    }

    /**
     * Listar entregas por empleado
     *
     * @param codEmpleado
     * @return
     */
    @Override
    public List<EntregaChofer> listarEntregasXEmpleado( int codEmpleado ) {

        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas @u_Chofer=?, @ACCION = ?",
                    new Object[] { codEmpleado, "A" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setIdEntrega( rs.getInt(1 ) );
                        temp.setDocEntry( rs.getInt(2 ) );
                        temp.setDocNum( rs.getInt(3 ) );
                        temp.setFactura( rs.getInt(4 ) );
                        temp.setDocDate( rs.getString(5 ) );
                        temp.setDocTime( rs.getString(6 ) );
                        temp.setCardCode( rs.getString(7 ) );
                        temp.setCardName( rs.getString(8 ) );
                        temp.setAddressEntregaFac( rs.getString(9 ) );
                        temp.setAddressEntregaMat( rs.getString(10 ) );
                        temp.setVendedor( rs.getString(11 ) );
                        temp.setUChofer( rs.getInt(12 ) );
                        temp.setItemCode( rs.getString(13 ) );
                        temp.setDscription( rs.getString(14 ) );
                        temp.setWhsCode( rs.getString(15 ) );
                        temp.setQuantity( rs.getInt(16 ) );
                        temp.setOpenQty( rs.getInt(17 ) );
                        temp.setDb( rs.getString(18 ) );
                        temp.setValido( rs.getString(19 ) );
                        temp.setFueEntregado( rs.getInt(20 ) );
                        temp.setFechaEntrega( rs.getString(21 ) );
                        temp.setLatitud( rs.getFloat(22 ) );
                        temp.setLongitud( rs.getFloat(23 ) );
                        temp.setDireccionEntrega( rs.getString(24 ) );
                        temp.setObs( rs.getString(25 ) );
                        temp.setTipo( rs.getString(26 ) );
                        temp.setObsF( rs.getString(27 ) );
                        temp.setAudUsuario( rs.getInt(28 ) );


                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en listarEntregasXEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
        }
        return lstTemp;
    }

    /**
     * Listar entregas por chofer
     *
     *
     */
    @Override
    public List<EntregaChofer> listarEntregasXChofer(String fechaEntrega, int codEmpleado ) {
        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas @fechaEntrega=?, @u_Chofer=? ,@ACCION = ?",
                    new Object[] { fechaEntrega, codEmpleado  ,"B" },
                    new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setDocEntry( rs.getInt(1 ) );
                        temp.setFactura( rs.getInt(2 ) );
                        temp.setCardCode( rs.getString(3 ) );
                        temp.setCardName( rs.getString(4 ) );
                        temp.setFechaNota( rs.getString(5 ) );
                        temp.setFechaEntrega( rs.getString(6 ) );
                        temp.setDiferenciaMinutos( rs.getInt(7 ) );
                        temp.setDireccionEntrega( rs.getString(8 ) );
                        temp.setAddressEntregaFac( rs.getString(9 ) );
                        temp.setAddressEntregaMat( rs.getString(10 ) );
                        temp.setVendedor( rs.getString(11 ) );
                        temp.setDb( rs.getString(12 ) );
                        temp.setUChofer( rs.getInt(13 ) );
                        temp.setNombreCompleto( rs.getString(14 ) );
                        temp.setLatitud(rs.getFloat(15));
                        temp.setLongitud(rs.getFloat(16));
                        temp.setObs( rs.getString(17 ) );
                        temp.setDocNumF(rs.getInt(18 ) );
                        temp.setPeso( rs.getFloat(19 ) );
                        temp.setCochePlaca( rs.getString(20 ) );
                        temp.setPrioridad( rs.getString(21 ) );
                        temp.setTipo( rs.getString(22 ) );


                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en listarEntregasXChofer, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
        }
        return lstTemp;

    }


    /**
     * Listara a todos los choferes
     *
     */
    @Override
    public List<EntregaChofer> lstChoferes() {
        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas  @ACCION = ?",
                    new Object[] {  "D" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setCodEmpleado( rs.getInt(1 ) );
                        temp.setNombreCompleto( rs.getString(2 ) );
                        temp.setCargo( rs.getString(3 ) );
                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en lstChoferes, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
        }
        return lstTemp;

    }

    /**
     * Mostrara un extracto de entregas si finalizaron o no
     *
     * @param fechaIni
     * @param fechaFin
     * @param codEmpleado
     * @return
     */
    public List<EntregaChofer> lstChoferesExtracto(Date fechaIni, Date fechaFin, int codSucursal, int codEmpleado) {

        List<EntregaChofer> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_trch_Entregas @fechaIni=?, @fechaFin=?, @codSucursal=?, @codEmpleado=? ,@ACCION = ?",
                    new Object[] { fechaIni, fechaFin, codSucursal ,codEmpleado  ,"E" },
                    new int[] { Types.DATE, Types.DATE, Types.INTEGER, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        EntregaChofer temp = new EntregaChofer();

                        temp.setOrd( rs.getInt(1 ) );
                        temp.setCodEmpleado( rs.getInt(2 ) );
                        temp.setNombreCompleto( rs.getString(3 ) );
                        temp.setFechaEntregaCad( rs.getString(4 ) );
                        temp.setRutaDiaria( rs.getInt(5 ) );
                        temp.setFechaInicioRutaCad( rs.getString(6 ) );
                        temp.setFechaFinRutaCad( rs.getString(7 ) );
                        temp.setEstatusRuta( rs.getString(8 ) );
                        temp.setFlag( rs.getInt(9 ) );



                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en lstChoferesExtracto, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<EntregaChofer>();
        }
        return lstTemp;


    }

    /**
     * Listar pedidos pendientes de entrega del SAP
     *
     * @return
     */
    @Override
    public List<PedidoPendienteEntregaDTO> lstPedidosPendientesEntrega() {
        List<PedidoPendienteEntregaDTO> lstTemp = new ArrayList<>();

        try {
            lstTemp =  this.jdbcTemplate.query("exec p_list_trch_Entregas  @ACCION = ?",
                    new Object[] { "F" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        PedidoPendienteEntregaDTO temp = new PedidoPendienteEntregaDTO();

                        temp.setEmpresa( rs.getString(1 ) );
                        temp.setDocEntry( rs.getLong(2 ) );
                        temp.setCardName( rs.getString(3 ) );
                        temp.setDocDate( rs.getDate(4) );
                        temp.setHoraCreacion( rs.getString(5 ) );
                        temp.setWeight( rs.getBigDecimal(6 ) );
                        temp.setCantidad( rs.getBigDecimal(7 ) );
                        temp.setComments( rs.getString(8 ) );
                        temp.setDireccionEntrega( rs.getString(9 ) );
                        temp.setVendedor( rs.getString(10 ) );
                        temp.setSistema( rs.getString(11 ) );
                        temp.setDocNum( rs.getString(12 ) );
                        temp.setSeriesName( rs.getString(13 ) );
                        temp.setTipoEntrega( rs.getString(14 ) );

                        return temp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EntregaChoferDao en lstPedidosPendientesEntrega, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<PedidoPendienteEntregaDTO>();
        }
        return lstTemp;
    }


    /* =====================================================================================
     * METODOS DE NOTIFICACION (aviso por WhatsApp de entregas)
     * -------------------------------------------------------------------------------------
     * POR QUE ESTOS DOS METODOS NO COPIAN EL PATRON DE CATCH DE LOS DE ARRIBA:
     *
     * Los metodos historicos hacen `this.jdbcTemplate = null;` dentro del catch. Este DAO es un
     * singleton de Spring y ese campo es LA UNICA referencia al JdbcTemplate inyectado: ponerlo
     * en null no "resetea" nada, deja el bean muerto. A partir de ahi CUALQUIER llamada posterior
     * a CUALQUIER metodo de esta clase --incluido registrarEntregaChofer-- tira NullPointerException,
     * que no es BadSqlGrammarException, escapa del catch, llega al catch(Exception) del controller
     * y devuelve 500 de forma permanente hasta reiniciar la aplicacion.
     *
     * Estos dos metodos se invocan desde el aviso por WhatsApp, que corre COLGADO del registro de
     * entrega del chofer. Si copiaran esa linea, un simple error de SQL en las ACCIONes nuevas
     * (por ejemplo, que 27_notificacion_entregas.sql todavia no se haya corrido en produccion)
     * mataria el DAO y con el la registracion de entregas: exactamente lo que la restriccion del
     * feature prohibe. Por eso aca NO se toca el campo jdbcTemplate.
     *
     * Ademas se atrapa DataAccessException (no solo BadSqlGrammarException, que dejaria pasar
     * RAISERROR y timeouts del SP) mas un catch de RuntimeException como red final, se loguea con
     * slf4j en vez de System.out, y se devuelve null / lista vacia. Tampoco se hace el
     * `((SQLException) e.getCause()).getErrorCode()` de los metodos viejos, que revienta con
     * ClassCastException o NPE cuando la causa no es una SQLException.
     * ===================================================================================== */

    /**
     * Datos de UNA entrega para el aviso por WhatsApp (p_list_trch_Entregas ACCION 'G').
     *
     * Devuelve una sola fila por (docEntry, db): el SP agrupa las N filas del documento
     * (una por itemCode) e informa cantidadItems.
     *
     * NUNCA lanza excepcion.
     *
     * @param docEntry docEntry del documento
     * @param db base de datos / empresa: IPX, ESP, PRODPAP
     * @return el DTO, o null si el documento no existe o si hubo cualquier error
     */
    @Override
    public EntregaNotificacionDTO obtenerDatosNotificacion( long docEntry, String db ) {

        // Guarda defensiva: algun metodo historico de esta clase pudo haber dejado el campo en null.
        // Si eso paso, el aviso se saltea en silencio; jamas se propaga un NPE al hilo que registra.
        if ( this.jdbcTemplate == null ) {
            logger.warn("obtenerDatosNotificacion: jdbcTemplate esta en null (lo anulo un metodo anterior del DAO). Se omite la notificacion de docEntry={}, db={}", docEntry, db);
            return null;
        }

        try {
            List<EntregaNotificacionDTO> lstTemp = this.jdbcTemplate.query("execute p_list_trch_Entregas @docEntry=?, @db=?, @ACCION = ?",
                    new Object[] { docEntry, db, "G" },
                    new int[] { Types.BIGINT, Types.VARCHAR, Types.VARCHAR },
                    MAPPER_NOTIFICACION);

            if ( lstTemp == null || lstTemp.isEmpty() ) {
                logger.warn("obtenerDatosNotificacion: el SP no devolvio filas para docEntry={}, db={}", docEntry, db);
                return null;
            }

            // La ACCION 'G' agrupa por (docEntry, db, cardCode), asi que mas de una fila
            // significa que el documento tiene VARIOS clientes. No hay forma de saber a cual
            // avisarle, y elegir el primero es exactamente el error que se quiere evitar:
            // mandarle a un cliente el nombre, la nota o la direccion de otro. Se omite.
            if ( lstTemp.size() > 1 ) {
                logger.warn("obtenerDatosNotificacion: docEntry={} db={} tiene {} clientes distintos; "
                          + "no se puede determinar el destinatario y se omite el aviso.",
                          docEntry, db, Integer.valueOf(lstTemp.size()));
                return null;
            }

            return lstTemp.get(0);

        } catch ( DataAccessException e ) {
            logger.error("Error de BD en obtenerDatosNotificacion (docEntry={}, db={}): {}", docEntry, db, e.getMessage(), e);
            return null;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en obtenerDatosNotificacion (docEntry={}, db={}): {}", docEntry, db, e.getMessage(), e);
            return null;
        }
    }


    /* =====================================================================================
     * SINCRONIZACION CON SAP SEPARADA DE LA LECTURA
     * -------------------------------------------------------------------------------------
     * QUE PROBLEMA RESUELVEN ESTOS DOS METODOS:
     *
     * listarEntregasXEmpleado (ACCION 'A', mas arriba) hace DOS cosas en una: primero llama a
     * p_abm_trch_Entregas @ACCION='A', que sincroniza trch_Entregas entera contra SAP por el
     * linked server, y despues filtra los pendientes de UN chofer. La sincronizacion esta medida:
     * ~1,4 s en caliente (1.145 ms el mejor caso, 2.129 ms el peor), 417 filas de 30 dias y 4
     * empresas, 490.427 lecturas logicas y 281 MB de memory grant por ejecucion.
     *
     * O sea que un chofer que abre su lista sincroniza a TODOS los choferes de TODAS las empresas,
     * dentro de su propio request HTTP, y lo vuelve a hacer en cada refresh. Quince choferes a la
     * misma hora eran quince sincronizaciones identicas y simultaneas contra SAP.
     *
     * Aca se parte en dos:
     *   - listarEntregasXEmpleadoSinSync -> ACCION 'Y': solo el SELECT, sin sincronizar.
     *   - sincronizarConSap              -> p_abm_trch_EntregasSync ACCION 'S': solo la sincronizacion.
     * Quien decide CUANDO sincronizar (como mucho una vez cada N segundos, un solo hilo a la vez,
     * y sin que nadie espere a nadie) es commons.SincronizacionEntregasService.
     *
     * NINGUNO DE LOS DOS LANZA, y ninguno pone this.jdbcTemplate = null. Vale palabra por palabra
     * lo explicado en el bloque de notificacion de mas arriba: este DAO es un singleton y anular
     * ese campo deja el bean muerto para siempre. Aca la consecuencia seria peor todavia, porque
     * el que se lleva el NullPointerException es el chofer que esta abriendo la app.
     *
     * NOTA PARA QUIEN TOQUE EL LADO SQL: mientras un hilo corre la ACCION 'S', los SELECT de la
     * ACCION 'Y' pueden quedar esperando los locks de escritura sobre trch_Entregas bajo READ
     * COMMITTED. Cuanto mas corta sea la ventana de escritura del SP de sincronizacion (armar todo
     * en una temporal y recien ahi tocar la tabla final), menos se nota. Es la unica forma en que
     * un chofer podria seguir esperando a la sincronizacion sin haberla pedido.
     * ===================================================================================== */

    /** Piso del timeout de la sincronizacion si la propiedad viene en 0 o negativa. */
    private static final int TIMEOUT_SYNC_POR_DEFECTO = 30;

    /**
     * Sincronizar trch_Entregas con SAP. Es la mitad cara de la vieja ACCION 'A'.
     *
     * No se le pasa @audUsuario: el unico parametro que este DAO conoce del SP es @ACCION. Si el SP
     * necesitara mas, deben tener DEFAULT.
     */
    private static final String SQL_SINCRONIZAR_CON_SAP = "execute p_abm_trch_EntregasSync @ACCION = ?";

    /**
     * Cuantos segundos se espera a que SAP conteste la sincronizacion antes de cortarla.
     *
     * El valor por defecto es holgado a proposito: lo normal son 1,4 s y el peor caso medido son
     * 2,1 s, asi que llegar a 30 s significa que SAP esta realmente mal, no lento. Cuando eso pasa,
     * cortar es lo correcto: el statement se cancela, SQL Server revierte lo que la sincronizacion
     * hubiera empezado a escribir, la tabla queda como estaba y el chofer recibe igual su lista con
     * los datos anteriores. Bajarlo es seguro; subirlo hace que el unico chofer que agarro el
     * candado espere mas.
     */
    @Value("${entregas.sync.timeout-segundos:30}")
    private int timeoutSyncSegundos;

    /**
     * RowMapper de la ACCION 'Y'.
     *
     * ES UNA COPIA EXACTA, COLUMNA POR COLUMNA, del mapper que listarEntregasXEmpleado tiene
     * embebido para la ACCION 'A'. Esta duplicado y no compartido porque el metodo historico no se
     * toca; la contrapartida es que las dos ACCIONes devuelven la MISMA lista de columnas en el
     * MISMO orden y si alguna vez cambia una, hay que cambiar las dos y los dos mappers.
     *
     * Columnas: 1 idEntrega, 2 docEntry, 3 docNum, 4 factura, 5 docDate, 6 docTime, 7 cardCode,
     *           8 cardName, 9 addressEntregaFac, 10 addressEntregaMat, 11 vendedor, 12 uChofer,
     *           13 itemCode, 14 dscription, 15 whsCode, 16 quantity, 17 openQty, 18 db, 19 valido,
     *           20 fueEntregado, 21 fechaEntrega, 22 latitud, 23 longitud, 24 direccionEntrega,
     *           25 obs, 26 tipo, 27 obsF, 28 audUsuario.
     */
    private static final RowMapper<EntregaChofer> MAPPER_ENTREGA_PENDIENTE = (rs, rowNum) -> {
        EntregaChofer temp = new EntregaChofer();

        temp.setIdEntrega( rs.getInt(1 ) );
        temp.setDocEntry( rs.getInt(2 ) );
        temp.setDocNum( rs.getInt(3 ) );
        temp.setFactura( rs.getInt(4 ) );
        temp.setDocDate( rs.getString(5 ) );
        temp.setDocTime( rs.getString(6 ) );
        temp.setCardCode( rs.getString(7 ) );
        temp.setCardName( rs.getString(8 ) );
        temp.setAddressEntregaFac( rs.getString(9 ) );
        temp.setAddressEntregaMat( rs.getString(10 ) );
        temp.setVendedor( rs.getString(11 ) );
        temp.setUChofer( rs.getInt(12 ) );
        temp.setItemCode( rs.getString(13 ) );
        temp.setDscription( rs.getString(14 ) );
        temp.setWhsCode( rs.getString(15 ) );
        temp.setQuantity( rs.getInt(16 ) );
        temp.setOpenQty( rs.getInt(17 ) );
        temp.setDb( rs.getString(18 ) );
        temp.setValido( rs.getString(19 ) );
        temp.setFueEntregado( rs.getInt(20 ) );
        temp.setFechaEntrega( rs.getString(21 ) );
        temp.setLatitud( rs.getFloat(22 ) );
        temp.setLongitud( rs.getFloat(23 ) );
        temp.setDireccionEntrega( rs.getString(24 ) );
        temp.setObs( rs.getString(25 ) );
        temp.setTipo( rs.getString(26 ) );
        temp.setObsF( rs.getString(27 ) );
        temp.setAudUsuario( rs.getInt(28 ) );

        return temp;
    };

    /**
     * Entregas pendientes de un chofer SIN sincronizar con SAP (ACCION 'Y').
     *
     * NUNCA lanza. Devuelve null si la consulta fallo y lista vacia si el chofer no tiene nada
     * pendiente: son dos cosas distintas y el controlador las trata distinto. Devolver lista vacia
     * ante un error le estaria diciendo al chofer "no tenes entregas", que es la peor respuesta
     * posible y ademas es silenciosa.
     *
     * @param codEmpleado el chofer
     * @return sus pendientes, lista vacia si no tiene, o null si la consulta fallo
     */
    @Override
    public List<EntregaChofer> listarEntregasXEmpleadoSinSync( int codEmpleado ) {

        if ( this.jdbcTemplate == null ) {
            logger.error("listarEntregasXEmpleadoSinSync: jdbcTemplate esta en null (lo anulo un metodo anterior "
                       + "del DAO). No se pueden listar las entregas del chofer {}.", Integer.valueOf(codEmpleado));
            return null;
        }

        try {
            List<EntregaChofer> lstTemp = this.jdbcTemplate.query("execute p_list_trch_Entregas @u_Chofer=?, @ACCION = ?",
                    new Object[] { codEmpleado, "Y" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    MAPPER_ENTREGA_PENDIENTE);

            return lstTemp == null ? Collections.<EntregaChofer>emptyList() : lstTemp;

        } catch ( DataAccessException e ) {
            // Caso tipico: el script con la ACCION 'Y' todavia no se corrio en esta base. El
            // controlador lo resuelve cayendo al camino historico, que si existe siempre.
            logger.error("Error de BD en listarEntregasXEmpleadoSinSync (chofer={}): {}",
                         Integer.valueOf(codEmpleado), e.getMessage(), e);
            return null;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado en listarEntregasXEmpleadoSinSync (chofer={}): {}",
                         Integer.valueOf(codEmpleado), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sincroniza trch_Entregas con SAP (p_abm_trch_EntregasSync ACCION 'S'). NUNCA lanza.
     *
     * Se ejecuta con un JdbcTemplate propio y acotado: es una llamada a otro servidor por linked
     * server y no puede quedarse colgada ocupando el hilo del request. El JdbcTemplate compartido
     * NO se toca — setQueryTimeout sobre el bean inyectado le cambiaria el timeout a toda la
     * aplicacion.
     *
     * @return true si el SP termino sin error
     */
    @Override
    public boolean sincronizarConSap() {

        if ( this.jdbcTemplate == null ) {
            logger.error("sincronizarConSap: jdbcTemplate esta en null (lo anulo un metodo anterior del DAO). "
                       + "No se sincroniza con SAP.");
            return false;
        }

        try {
            JdbcTemplate acotado = new JdbcTemplate(this.jdbcTemplate.getDataSource());
            // Un 0 significa SIN LIMITE por especificacion JDBC y un negativo hace que Spring ni
            // llame a setQueryTimeout; las dos escrituras dejarian este hilo colgado esperando a
            // SAP, que es justo lo que el timeout viene a evitar. Se fuerza un piso.
            acotado.setQueryTimeout(this.timeoutSyncSegundos > 0 ? this.timeoutSyncSegundos : TIMEOUT_SYNC_POR_DEFECTO);

            // Se usa execute()/PreparedStatementCallback y no query() a proposito. query() llama a
            // executeQuery(), y el driver de SQL Server tira "The statement did not return a result
            // set" si el SP no devuelve filas: reportariamos como fallida una sincronizacion que
            // en realidad funciono. Con execute() da igual si el SP devuelve resumen o no devuelve
            // nada. El booleano de retorno depende SOLO de que el statement no haya tirado error;
            // las columnas de resumen son diagnostico y se leen best-effort.
            Boolean ok = acotado.execute(SQL_SINCRONIZAR_CON_SAP, (PreparedStatementCallback<Boolean>) ps -> {
                ps.setString(1, "S");

                boolean hayResultSet = ps.execute();
                // Se recorren todos los resultados por si el SP emite algun rowcount antes de la
                // fila de resumen. La guarda es por si un driver no terminara nunca de decir que no
                // hay mas: preferible salir del bucle que colgar el hilo del chofer.
                for ( int i = 0; i < 20; i++ ) {
                    if ( hayResultSet ) {
                        ResultSet rs = ps.getResultSet();
                        try {
                            if ( rs != null && rs.next() ) {
                                registrarResumenSincronizacion(rs);
                            }
                        } finally {
                            if ( rs != null ) {
                                rs.close();
                            }
                        }
                    } else if ( ps.getUpdateCount() == -1 ) {
                        break;
                    }
                    hayResultSet = ps.getMoreResults();
                }
                return Boolean.TRUE;
            });

            return Boolean.TRUE.equals(ok);

        } catch ( DataAccessException e ) {
            // Incluye el timeout (el statement se cancela y SQL Server revierte lo que hubiera
            // escrito) y el caso de que p_abm_trch_EntregasSync todavia no exista en esta base.
            logger.error("Error de BD al sincronizar entregas con SAP: {}", e.getMessage(), e);
            return false;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado al sincronizar entregas con SAP: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deja en el log el resumen que devuelve la ACCION 'S' (1 filasSincronizadas, 2 fechaSync).
     *
     * Es informativo y se lee por indice mirando primero cuantas columnas vinieron: si el SP
     * cambiara su resumen, lo peor que puede pasar es que el log quede mas pobre. Una
     * sincronizacion que funciono no se puede reportar como fallida por no poder loguearla.
     */
    private void registrarResumenSincronizacion(ResultSet rs) {
        try {
            int columnas = rs.getMetaData().getColumnCount();
            int filas = columnas >= 1 ? rs.getInt(1) : -1;
            String fechaSync = columnas >= 2 ? rs.getString(2) : null;
            logger.info("Sincronizacion con SAP: {} fila(s) sincronizada(s){}",
                        Integer.valueOf(filas),
                        fechaSync == null ? "" : ", fechaSync=" + fechaSync);
        } catch ( SQLException e ) {
            logger.debug("La sincronizacion con SAP termino bien pero no se pudo leer su fila de resumen: {}",
                         e.getMessage());
        }
    }

    /**
     * ¿Existen la ACCION 'Y' y el SP de sincronizacion? Se le pregunta al CATALOGO de la base.
     *
     * <p>Hace falta una comprobacion POSITIVA porque estos SP son una cadena de
     * {@code IF @ACCION = ...}: una accion que no existe no da error, simplemente no entra a
     * ningun IF y devuelve CERO FILAS. Y cero filas es lo mismo que devuelve un chofer sin
     * pendientes, asi que por el resultado los dos casos son indistinguibles. Deducir "la
     * accion funciona" de "vino vacia" es justamente lo que no se puede hacer: si el primer
     * chofer del dia no tiene nada pendiente, la conclusion queda fijada mal para todo el
     * proceso y TODOS ven la lista vacia, en silencio, hasta reiniciar.
     *
     * <p>Ante cualquier duda devuelve false. Quedarse en el camino historico es lento; dejar a
     * todos los choferes sin lista es un incidente.
     */
    @Override
    public boolean existeSoporteSyncDesacoplada() {

        if ( this.jdbcTemplate == null ) {
            logger.warn("existeSoporteSyncDesacoplada: jdbcTemplate en null; se asume que NO hay soporte.");
            return false;
        }

        try {
            Integer ok = this.jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN OBJECT_ID('p_abm_trch_EntregasSync') IS NOT NULL "
                  + "        AND OBJECT_DEFINITION(OBJECT_ID('p_list_trch_Entregas')) LIKE '%@ACCION = ''Y''%' "
                  + "      THEN 1 ELSE 0 END", Integer.class);
            boolean hay = ok != null && ok.intValue() == 1;
            if ( hay ) {
                logger.info("Sincronizacion desacoplada disponible: existe p_abm_trch_EntregasSync y la ACCION 'Y'.");
            } else {
                logger.error("El script 29_entregas_sync_desacoplada.sql NO esta corrido en esta base: falta "
                           + "p_abm_trch_EntregasSync o la ACCION 'Y' de p_list_trch_Entregas. Se usa el camino "
                           + "historico (ACCION 'A', con sincronizacion dentro del request). Los choferes ven su "
                           + "lista completa; solo siguen esperando ~1,4 s por consulta.");
            }
            return hay;

        } catch ( DataAccessException e ) {
            logger.error("No se pudo verificar el soporte de sincronizacion desacoplada: {}. Se asume que NO hay.",
                         e.getMessage());
            return false;
        } catch ( RuntimeException e ) {
            logger.error("Error inesperado verificando el soporte de sincronizacion desacoplada: {}. Se asume que NO hay.",
                         e.getMessage());
            return false;
        }
    }

}
