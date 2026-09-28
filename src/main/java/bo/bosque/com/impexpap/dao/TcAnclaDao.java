package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcAncla;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a <b>tpr_tcAncla</b> (configuracion del reprecio nocturno). Todo pasa por
 * procedimiento almacenado via {@link SpHelper}; no hay SQL crudo.
 *
 * <p><b>Alcance:</b> este DAO es unicamente consulta y ABM de la fila de configuracion.
 * La logica del reprecio (calcular el delta contra el ancla, decidir si dispara,
 * repreciar {@code tpr_precio}, leer el tipo de cambio de SAP) esta FUERA DE ALCANCE
 * de esta migracion y no vive aqui: quien la implemente debe llamar a
 * {@link #moverAncla} o a {@link #registrarTcVisto} segun el resultado, pero la
 * decision no se toma en esta clase.
 *
 * <p><b>Un solo SP para todo:</b> a diferencia del resto del modulo, tpr_tcAncla no
 * tiene {@code p_list_tcAncla} — ese procedimiento NO existe en la base, pese a lo que
 * anuncia el javadoc de {@link ITcAncla}. El listado se resuelve con la rama
 * {@code @ACCION='L'} del propio {@code p_abm_tcAncla}, que es el unico
 * {@code p_abm_*} de todo el modulo que emite un resultset al cliente: devuelve las
 * 6 columnas de la tabla (companyDB, tcAncla, fechaAncla, tcUltimo, fechaUltimo,
 * umbral) ordenadas por companyDB y filtradas por
 * {@code (@companyDB IS NULL OR companyDB = @companyDB)}.
 *
 * <p><b>Acciones, que no son I/U/D:</b> este SP no usa la convencion del resto del
 * modulo. Sus seis ramas son:
 * <ul>
 *   <li><b>L</b> — listar / obtener (unica rama de lectura).</li>
 *   <li><b>A</b> — alta idempotente: solo inserta si la empresa todavia no tiene ancla
 *       ({@code IF NOT EXISTS}); si ya la tiene no hace nada y tampoco avisa.</li>
 *   <li><b>B</b> — baja fisica de la fila de esa empresa.</li>
 *   <li><b>M</b> — mover el ancla: fija tcAncla, fechaAncla, tcUltimo y fechaUltimo.</li>
 *   <li><b>S</b> — solo registrar el tc observado: toca tcUltimo y fechaUltimo, deja el
 *       ancla quieta. Ojo: es un UPDATE, NO una lectura.</li>
 *   <li><b>U</b> — ajustar el umbral, sin tocar el ancla.</li>
 * </ul>
 *
 * <p><b>Por que todas las escrituras usan {@code ejecutarAbmMap} y nunca
 * {@code ejecutarAbm}:</b> el modelo {@link TcAncla} tiene las 6 columnas de la tabla,
 * pero el SP solo declara {@code @companyDB, @tc, @umbral, @fechaAncla}. Mandar el
 * modelo entero armaria un EXEC con {@code @tcAncla}, {@code @tcUltimo} y
 * {@code @fechaUltimo}, que no existen como parametro, y SQL Server lo rechazaria.
 * Ademas el parametro del tipo de cambio se llama {@code @tc} y la columna
 * {@code tcAncla}: los nombres no coinciden. Por eso cada metodo arma su Map con los
 * parametros exactos de su rama y deja el resto en DEFAULT NULL.
 *
 * <p><b>idGenerado siempre 0:</b> la PK es {@code companyDB} (nvarchar) y la tabla no
 * tiene columna IDENTITY, asi que ninguna rama puede devolver SCOPE_IDENTITY().
 *
 * @see ITcAncla
 * @see TcAncla
 */
@Repository
public class TcAnclaDao implements ITcAncla {

    /**
     * Unico SP de la tabla: hace de ABM y de listado a la vez.
     * No hay p_list_tcAncla; la lectura es la rama 'L' de este mismo procedimiento.
     */
    private static final String SP_ABM = "p_abm_tcAncla";

    private final SpHelper spHelper;

    public TcAnclaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // --------------------------- Lectura (rama 'L') ---------------------------

    /**
     * ACCION 'L' de p_abm_tcAncla sin filtro: manda el Map vacio para que
     * {@code @companyDB} quede en su DEFAULT NULL y el {@code IS NULL} del WHERE
     * actue como comodin.
     *
     * @return todas las anclas, ya ordenadas por companyDB por el ORDER BY del SP;
     *         lista vacia si la tabla no tiene filas
     */
    @Override
    public List<TcAncla> listar() {
        return spHelper.ejecutarListado(SP_ABM, new LinkedHashMap<String, Object>(), "L", TcAncla.class);
    }

    /**
     * ACCION 'L' de p_abm_tcAncla filtrando por {@code @companyDB}.
     *
     * <p>Guarda deliberada contra el companyDB vacio: el SP trata el NULL como
     * "sin filtro", asi que mandar null devolveria la tabla entera y este metodo
     * terminaria entregando el ancla de OTRA empresa (la primera por companyDB) como
     * si fuera la pedida. Ante un companyDB nulo o en blanco se corta y se devuelve
     * null, que es lo mismo que "esa empresa no tiene ancla".
     *
     * @param companyDB base de datos SAP de la empresa (PK de la tabla)
     * @return el ancla de esa empresa, o null si todavia no fue sembrada
     */
    @Override
    public TcAncla obtenerPorCompanyDB(String companyDB) {
        if (companyDB == null || companyDB.trim().isEmpty()) return null;

        Map<String, Object> filtro = new LinkedHashMap<>();
        filtro.put("companyDB", companyDB);

        List<TcAncla> filas = spHelper.ejecutarListado(SP_ABM, filtro, "L", TcAncla.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    // --------------------------- Escritura ---------------------------

    /**
     * ACCION 'A' de p_abm_tcAncla: siembra el ancla de una empresa que todavia no la tiene.
     *
     * <p>El SP inserta con {@code IF NOT EXISTS}, de modo que es idempotente: si la
     * empresa ya tiene ancla no inserta nada y tampoco lo reporta como error. El alta
     * copia {@code @tc} tanto en tcAncla como en tcUltimo y pone fechaUltimo en GETDATE().
     *
     * <p>Los parametros opcionales se omiten del Map cuando llegan en null, para que
     * queden en su DEFAULT NULL y sea el propio SP el que aplique sus valores por
     * defecto: {@code ISNULL(@umbral, 0.10)} y
     * {@code ISNULL(@fechaAncla, '19000101')}. La fecha vieja es intencional: el job
     * cuenta como pendientes todo tpr_precio con {@code audFecha > fechaAncla}, asi que
     * sembrar con la fecha del dia dejaria a la empresa nueva con cero pendientes.
     *
     * @param companyDB  base de datos SAP de la empresa (obligatorio)
     * @param tc         tipo de cambio inicial del ancla (obligatorio)
     * @param umbral     variacion minima para disparar reprecio; null deja el 0.10 del SP
     * @param fechaAncla fecha del ancla; null deja el 1900-01-01 del SP
     * @return RespuestaSp; getIdGenerado() siempre 0, la PK no es IDENTITY
     */
    @Override
    public RespuestaSp sembrarAncla(String companyDB, BigDecimal tc, BigDecimal umbral, Date fechaAncla) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyDB", companyDB);
        params.put("tc", tc);
        if (umbral != null)     params.put("umbral", umbral);
        if (fechaAncla != null) params.put("fechaAncla", fechaAncla);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "A");
    }

    /**
     * ACCION 'B' de p_abm_tcAncla: borra fisicamente el ancla de la empresa para poder
     * volver a sembrarla desde cero.
     *
     * <p>Es un DELETE sin verificacion previa: si la empresa no tenia ancla no borra
     * nada y el SP no lo reporta como error.
     *
     * @param companyDB base de datos SAP de la empresa
     * @return RespuestaSp; getIdGenerado() en 0
     */
    @Override
    public RespuestaSp eliminarAncla(String companyDB) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyDB", companyDB);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "B");
    }

    /**
     * ACCION 'M' de p_abm_tcAncla: mueve el ancla despues de un reprecio aplicado con
     * exito. Fija tcAncla y tcUltimo con {@code @tc}, y fechaAncla y fechaUltimo con
     * GETDATE() del servidor (la fecha no se manda como parametro en esta rama).
     *
     * <p>Lo llama el job nocturno, no una pantalla. Este DAO solo ejecuta la escritura:
     * la decision de si correspondia mover el ancla se toma fuera.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param tc        tipo de cambio que pasa a ser el nuevo ancla
     * @return RespuestaSp; getIdGenerado() en 0
     */
    @Override
    public RespuestaSp moverAncla(String companyDB, BigDecimal tc) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyDB", companyDB);
        params.put("tc", tc);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "M");
    }

    /**
     * ACCION 'S' de p_abm_tcAncla: registra el tipo de cambio observado sin mover el
     * ancla. Actualiza solo tcUltimo con {@code @tc} y fechaUltimo con GETDATE().
     *
     * <p>Pese a la letra, 'S' aqui es una escritura, no un "select": el SP no devuelve
     * ninguna fila en esta rama.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param tc        tipo de cambio observado
     * @return RespuestaSp; getIdGenerado() en 0
     */
    @Override
    public RespuestaSp registrarTcVisto(String companyDB, BigDecimal tc) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyDB", companyDB);
        params.put("tc", tc);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "S");
    }

    /**
     * ACCION 'U' de p_abm_tcAncla: ajusta el umbral de disparo de la empresa sin tocar
     * el ancla ni el ultimo tc observado.
     *
     * <p>El umbral es una proporcion, no un porcentaje: 0.10 significa 10 %. El SP no
     * valida el rango, asi que la validacion de negocio corresponde a la capa de servicio.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param umbral    nueva variacion minima
     * @return RespuestaSp; getIdGenerado() en 0
     */
    @Override
    public RespuestaSp actualizarUmbral(String companyDB, BigDecimal umbral) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("companyDB", companyDB);
        params.put("umbral", umbral);
        return spHelper.ejecutarAbmMap(SP_ABM, params, "U");
    }
}
