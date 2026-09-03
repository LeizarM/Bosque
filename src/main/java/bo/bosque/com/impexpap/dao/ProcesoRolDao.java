package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.ConvocarBloqueDto;
import bo.bosque.com.impexpap.dto.GenerarRolDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class ProcesoRolDao implements IProcesoRol {

    /* Este DAO NO usa SpHelper, y es la excepcion del modulo.
       SpHelper sirve para el contrato de los ABM (@error/@errormsg/@idGenerado
       OUTPUT) y para los listados. Los SPs de proceso no tienen ninguno de los
       dos: no devuelven parametros de salida y no devuelven resultset. Cortan
       con RAISERROR, que el driver levanta como excepcion.
       Por eso se llama al JdbcTemplate directo y se traduce el RAISERROR a
       SpBusinessException, para que el front reciba el mismo 400 con el texto
       del SP que recibe de cualquier ABM.                                     */
    private final JdbcTemplate jdbcTemplate;

    /* Numero que SQL Server le pone a un RAISERROR con mensaje literal, es
       decir sin id de sysmessages. Es como cortan los trs_sp_ cuando rebotan
       por una regla del negocio; un error del motor trae SU propio numero.
       Verificado contra el driver que usa el proyecto (mssql-jdbc 9.4):
         RAISERROR('...',16,1)          -> getErrorCode() = 50000
         SELECT * FROM tabla_inexistente -> 208
         SELEC 1                         -> 2812
       Por eso el discriminador es el codigo y no una heuristica sobre el texto:
       el texto de una regla lo escribe cada SP y cambia cuando alguien lo
       reescribe, el 50000 no.                                                 */
    private static final int RAISERROR_LITERAL = 50000;

    /* Pero 50000 solo no alcanza, y esta es la trampa: el CATCH de los trs_sp_
       RE-LANZA con RAISERROR lo que atrapa, asi que un deadlock tambien sale
       como 50000. Lo que lo delata es el prefijo que el CATCH le agrega para
       no perder el numero original:
         'Err 1205 (linea 30): Transaction was deadlocked...'
       Ese prefijo es marca de excepcion del motor: por ahi no viaja ninguna
       regla, porque las reglas estan a proposito FUERA del TRY. Sin este
       chequeo, un deadlock le llegaria al usuario como si fuera algo que el
       puede resolver.                                                         */
    private static final Pattern ENVOLTORIO_DEL_CATCH =
        Pattern.compile("^\\s*Err\\s+\\d+\\s+\\(linea\\s+\\d+\\)\\s*:",
                        Pattern.CASE_INSENSITIVE);

    public ProcesoRolDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ejecuta un SP de proceso tolerando que devuelva resultsets.
     *
     * <p><b>Por qué no {@code jdbcTemplate.update()}.</b> Varios de estos SPs
     * terminan con SELECTs de resumen — {@code trs_sp_generarRol} devuelve
     * cuatro: la cabecera, los sábados especiales, la convocatoria y los
     * cumpleaños. Sirven cuando se los corre a mano desde SSMS. Pero
     * {@code update()} exige que la sentencia NO devuelva nada y el driver corta
     * con «Se ha generado un conjunto de resultados para actualización», después
     * de que el SP ya hizo todo su trabajo.
     *
     * <p>Se usa {@code execute()}, que acepta las dos formas, y se DRENAN todos
     * los resultados: si quedan colgando, el driver los arrastra a la siguiente
     * consulta que use esa conexión del pool y el error aparece en otra parte.
     *
     * <p><b>Los parámetros se atan con {@code setObject} a mano y no con los
     * {@code args} de Spring.</b> No es sólo comodidad: varios de estos SPs
     * reciben NULL para decir «sin filtro», y cuando un argumento va en null
     * Spring le pregunta el tipo al driver. El de SQL Server responde eso
     * PARSEANDO el SQL con ANTLR, y {@code antlr4-runtime} es una dependencia
     * opcional que este proyecto no trae: revienta con
     * {@code NoClassDefFoundError}. {@code setObject(n, null)} manda el NULL sin
     * consultar metadatos.
     */
    private void ejecutarProceso(String sql, Object... params) {
        jdbcTemplate.execute(sql, (PreparedStatementCallback<Void>) ps -> {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.execute();
            drenar(ps);
            return null;
        });
    }

    /**
     * Resuelve el idRol de un alcance puntual (anio + empresa + sucursal) via
     * {@code p_list_trs_Rol @ACCION='A'}, en vez de leer dbo.trs_Rol con un
     * SELECT suelto.
     *
     * <p>Igual que en {@link #ejecutarProceso}, los parametros se atan a mano
     * con {@code setObject}: {@code codEmpresa}/{@code codSucursal} pueden
     * venir en null, y dejar que {@code jdbcTemplate.queryForObject(sql,
     * Class, Object...)} les adivine el tipo dispara el mismo
     * {@code NoClassDefFoundError} de ANTLR documentado arriba.
     */
    private Long obtenerIdRol(int anio, Long codEmpresa, Long codSucursal) {
        return jdbcTemplate.execute(
            "EXEC dbo.p_list_trs_Rol @ACCION='A', @anio=?, @codEmpresa=?, @codSucursal=?",
            (PreparedStatementCallback<Long>) ps -> {
                ps.setObject(1, anio);
                ps.setObject(2, codEmpresa);
                ps.setObject(3, codSucursal);

                Long idRol = null;
                if (ps.execute()) {
                    try (ResultSet rs = ps.getResultSet()) {
                        if (rs.next()) {
                            long valor = rs.getLong("idRol");
                            idRol = rs.wasNull() ? null : valor;
                        }
                    }
                }
                drenar(ps);
                return idRol;
            });
    }

    /** Consume todo lo que haya dejado el SP: resultsets y contadores. */
    private void drenar(PreparedStatement ps) throws SQLException {
        // El corte del contrato JDBC: no hay mas resultados cuando getMoreResults()
        // da false Y getUpdateCount() da -1. El tope es una red por si un driver
        // no respeta el contrato.
        for (int vuelta = 0; vuelta < 100; vuelta++) {
            if (!ps.getMoreResults() && ps.getUpdateCount() == -1) return;
        }
        log.warn("El SP devolvio mas de 100 resultados; se deja de drenar.");
    }

    // ── generar / regenerar el rol entero ─────────────────────────────────
    @Override
    public long generarRol(GenerarRolDto dto) {
        String modo = (dto.getModo() == null || dto.getModo().trim().isEmpty())
                    ? "CREAR" : dto.getModo().trim().toUpperCase();

        if (!"CREAR".equals(modo) && !"REGENERAR".equals(modo)) {
            throw new SpBusinessException("modo invalido: " + modo + ". Use CREAR o REGENERAR.");
        }
        if (dto.getAnio() < 2000 || dto.getAnio() > 2100) {
            throw new SpBusinessException("Anio fuera de rango razonable: " + dto.getAnio() + ".");
        }

        log.info("trs_sp_generarRol modo={} anio={} codSucursal={}", modo, dto.getAnio(), dto.getCodSucursal());
        try {
            ejecutarProceso(
                "EXEC dbo.trs_sp_generarRol @anio=?, @codSucursal=?, @codEmpresa=?,"
              + " @turnosObjetivoA=?, @turnosObjetivoB=?, @coberturaObjetivo=?,"
              + " @aplicaAsuetoCumple=?, @audUsuario=?, @modo=?, @todosPrimerSabadoTrimestre=?",
                dto.getAnio(), dto.getCodSucursal(), dto.getCodEmpresa(),
                dto.getTurnosObjetivoA(), dto.getTurnosObjetivoB(), dto.getCoberturaObjetivo(),
                dto.getAplicaAsuetoCumple(), dto.getAudUsuario(), modo,
                dto.getTodosPrimerSabadoTrimestre());

            /* El SP no devuelve el id: se lo busca aparte, por el MISMO
               criterio de alcance que usa el SP para decidir si el rol ya
               existe (anio + version 1 + empresa + sucursal). Va por SP y no
               por un SELECT suelto contra dbo.trs_Rol: este modulo no admite
               SQL crudo contra tablas desde el DAO. p_list_trs_Rol
               @ACCION='A' expone ese mismo criterio -- a diferencia de
               @ACCION='L', que trata NULL/0 como "todas".                   */
            Long idRol = obtenerIdRol(dto.getAnio(), dto.getCodEmpresa(), dto.getCodSucursal());

            return idRol == null ? 0L : idRol;

        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al generar el rol");
        }
    }

    // ── sincronizar feriados contra RR.HH. ────────────────────────────────
    @Override
    public void refrescarFeriados(long idRol, boolean soloInformar, Long audUsuario) {
        log.info("trs_sp_refrescarFeriados idRol={} soloInformar={}", idRol, soloInformar);
        try {
            ejecutarProceso(
                "EXEC dbo.trs_sp_refrescarFeriados @idRol=?, @soloInformar=?, @audUsuario=?",
                idRol, soloInformar ? 1 : 0, audUsuario);
        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al refrescar feriados");
        }
    }

    // ── convocar o excusar en bloque ──────────────────────────────────────
    @Override
    public void convocar(ConvocarBloqueDto dto) {
        // Los tres criterios son excluyentes. El SP no lo valida: si van dos,
        // aplica el primero que encuentra y el otro se pierde en silencio.
        int criterios = (dto.getCodEmpleado()     != null ? 1 : 0)
                      + (dto.getCodEmpleadoJefe() != null ? 1 : 0)
                      + (dto.getCodCargo()        != null ? 1 : 0);
        if (criterios != 1) {
            throw new SpBusinessException(
                "Mande EXACTAMENTE UNO de codEmpleado, codEmpleadoJefe o codCargo. Recibidos: " + criterios + ".");
        }

        log.info("trs_sp_convocar idSabado={} tipo={}", dto.getIdSabado(), dto.getTipo());
        try {
            ejecutarProceso(
                "EXEC dbo.trs_sp_convocar @idSabado=?, @tipo=?, @codEmpleado=?, @codEmpleadoJefe=?,"
              + " @codCargo=?, @motivo=?, @codEmpleadoAutoriza=?, @audUsuario=?",
                dto.getIdSabado(), dto.getTipo(), dto.getCodEmpleado(), dto.getCodEmpleadoJefe(),
                dto.getCodCargo(), dto.getMotivo(), dto.getCodEmpleadoAutoriza(), dto.getAudUsuario());
        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al convocar");
        }
    }

    // ── cruzar con vacaciones y permisos ──────────────────────────────────
    @Override
    public void refrescarPermisos(long idRol, long idSabado, boolean soloInformar,
                                  Long audUsuario) {
        log.info("trs_sp_refrescarPermisos idRol={} idSabado={} soloInformar={}",
                 idRol, idSabado, soloInformar);
        try {
            ejecutarProceso(
                "EXEC dbo.trs_sp_refrescarPermisos @idRol=?, @idSabado=?,"
              + " @soloInformar=?, @audUsuario=?",
                // 0 quiere decir "todo el rol", y el SP lo espera como NULL.
                idRol, idSabado == 0 ? null : idSabado,
                soloInformar ? 1 : 0, audUsuario);
        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al cruzar los permisos");
        }
    }

    // ── rehacer un solo sábado ────────────────────────────────────────────
    @Override
    public void regenerarSabado(long idSabado, Long audUsuario) {
        log.info("trs_sp_regenerarSabado idSabado={}", idSabado);
        try {
            ejecutarProceso(
                "EXEC dbo.trs_sp_regenerarSabado @idSabado=?, @audUsuario=?",
                idSabado, audUsuario);
        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al regenerar el sabado");
        }
    }

    // ── crear el rol de la gestion siguiente ──────────────────────────────
    @Override
    public void generarGestionSiguiente(int anio, Long audUsuario) {
        log.info("trs_sp_generarGestionSiguiente anio={}", anio);
        try {
            /* Este SP devuelve resultsets propios (el de resumen) Y arrastra los
               cuatro que devuelve cada trs_sp_generarRol de adentro de su WHILE.
               ejecutarProceso los drena a todos: si quedaran colgando, el driver
               se los pasa a la siguiente consulta que agarre esa conexion del
               pool y el error aparece en otra parte.                             */
            ejecutarProceso(
                "EXEC dbo.trs_sp_generarGestionSiguiente @anio=?, @audUsuario=?",
                anio, audUsuario);
        } catch (DataAccessException ex) {
            throw traducir(ex, "Error al generar la gestion siguiente");
        }
    }

    /**
     * Separa el rechazo de negocio del fallo tecnico, que es la unica decision
     * que importa acá: de un lado un texto que el usuario tiene que leer entero
     * y una operacion que no hizo falta hacer; del otro, algo roto que él no
     * puede arreglar y que Sistemas tiene que ver con stack y todo.
     *
     * <p>Devuelve la excepcion en vez de tirarla para que el que llama escriba
     * {@code throw traducir(...)}: asi el compilador ve que el catch corta y no
     * pide un return de relleno.
     *
     * <p><b>Regla de negocio</b> -> {@link SpBusinessException}, que
     * {@code GlobalExceptionHandler} devuelve como <b>400</b> con el mensaje
     * adentro. Es el mismo trato que reciben los rechazos que pasan por
     * SpHelper con el contrato {@code @error/@errormsg}.
     *
     * <p><b>Cualquier otra cosa</b> -> se devuelve la excepcion ORIGINAL, sin
     * tocar. Sube al handler generico, que la loguea entera y contesta
     * <b>500</b>. Envolverla en SpBusinessException seria mentir dos veces: le
     * diria al cliente «arreglá tu pedido» por un deadlock, y nos escondería el
     * stack del problema de verdad.
     */
    private RuntimeException traducir(DataAccessException ex, String contexto) {
        SQLException sql = primeraSQLException(ex);

        /* El texto que escribio el SP viaja en la SQLException; la de arriba es
           el envoltorio de Spring ("StatementCallback; uncategorized
           SQLException..."). El driver NO le agrega nada al mensaje del
           RAISERROR —comprobado: getMessage() devuelve la frase tal cual y
           getCause() es null—, asi que no hay ruido que limpiar de este lado.  */
        if (sql != null
                && sql.getErrorCode() == RAISERROR_LITERAL
                && !ENVOLTORIO_DEL_CATCH.matcher(sql.getMessage() == null ? "" : sql.getMessage()).find()) {
            String msg = sql.getMessage();
            // warn y no error: que el SP rebote por una regla es funcionamiento
            // normal, no un incidente. Sin stack, que no aporta nada.
            log.warn("{}: el SP rechazo la operacion: {}", contexto, msg);
            return new SpBusinessException(contexto + ": " + msg);
        }

        /* Se loguea el contexto —qué se estaba haciendo—, que es lo unico que
           el handler generico no sabe. El stack lo imprime él, asi que no se
           duplica acá.                                                         */
        log.error("{}: fallo tecnico (errorCode={})", contexto,
                  sql != null ? sql.getErrorCode() : null);
        return ex;
    }

    /** La primera SQLException de la cadena de causas, o null si no hay
     *  ninguna: eso ultimo pasa, por ejemplo, cuando el que falla es el SELECT
     *  del idRol y no devuelve filas. No es un rechazo del SP, es un 500. */
    private SQLException primeraSQLException(Throwable t) {
        // Acotado por si alguna vez aparece un ciclo de causas: mejor devolver
        // null y contestar 500 que colgar el hilo del request.
        for (int salto = 0; salto < 20 && t != null; salto++) {
            if (t instanceof SQLException) return (SQLException) t;
            t = t.getCause();
        }
        return null;
    }
}
