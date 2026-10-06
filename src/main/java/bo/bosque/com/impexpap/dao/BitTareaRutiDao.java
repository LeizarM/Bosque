// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\BitTareaRutiDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.BitacoraCumplimientoDto;
import bo.bosque.com.impexpap.dto.BitacoraFiltroRequest;
import bo.bosque.com.impexpap.dto.DiagnosticoGeneracionDto;
import bo.bosque.com.impexpap.dto.ResumenGeneracionDto;
import bo.bosque.com.impexpap.model.BitTareaRuti;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class BitTareaRutiDao implements IBitTareaRuti {

    private static final String SP_LIST = "p_list_tac_BitTareaRuti";

    private final SpHelper spHelper;

    public BitTareaRutiDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // FIX 2026-09-07 (incidente en producción): p_abm_tac_BitTareaRuti ganó
    // @fecha/@traspasosXml (archivo 27, solo para ACCION='C' de Cierre de
    // Operaciones). ejecutarAbm es metadata-driven (SimpleJdbcCall) y exige
    // un valor para TODO parámetro que el proc declare, sin importar que
    // tenga DEFAULT NULL en SQL — como BitTareaRuti.java nunca tuvo esos 2
    // campos, la llamada empezó a fallar con "Required input parameter
    // 'fecha' is missing" en TODAS las acciones (I/U/D), no solo en la nueva.
    // ejecutarAbmMap arma el EXEC solo con las claves que se le pasan aquí,
    // dejando que @fecha/@traspasosXml tomen su propio DEFAULT NULL — mismo
    // patrón que ya usan los DAOs de "flujo especial" (p.ej.
    // CierreOperacionesDao), que por eso nunca se rompieron con esto.
    // idATR/idFrec quedan afuera del Map a propósito: son columnas
    // proyectadas de solo lectura (LEFT JOIN en el p_list), no existen como
    // parámetro real de este proc — pasarlas aquí (a diferencia de con
    // ejecutarAbm, que las ignora en silencio) rompería el EXEC.
    @Override
    public RespuestaSp registrar(BitTareaRuti mb, String acc) {
        log.info("Registrando BitTareaRuti: {}, Accion: {}", mb.toString(), acc);
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", mb.getIdBitTarea());
        params.put("fechaActivo", mb.getFechaActivo());
        params.put("fechaPresentacion", mb.getFechaPresentacion());
        params.put("idTarRuti", mb.getIdTarRuti());
        params.put("nombreTareaRutinaria", mb.getNombreTareaRutinaria());
        params.put("codEmpleado", mb.getCodEmpleado());
        params.put("descripCargo", mb.getDescripCargo());
        params.put("fechaCompletado", mb.getFechaCompletado());
        params.put("fueRealizado", mb.getFueRealizado());
        params.put("obs", mb.getObs());
        params.put("estado", mb.getEstado());
        params.put("audUsuario", mb.getAudUsuario());
        return spHelper.ejecutarAbmMap("p_abm_tac_BitTareaRuti", params, acc);
    }

    @Override
    public List<BitTareaRuti> listar(BitTareaRuti filtro) {
        return spHelper.ejecutarListadoSinCero(SP_LIST, filtro, "L", BitTareaRuti.class);
    }

    // FIX: usa Map (no el modelo) para que idBitTarea==0 no viaje como filtro real —
    // el SP trata @idBitTarea IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public BitTareaRuti obtenerPorId(long idBitTarea) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idBitTarea", idBitTarea);
        List<BitTareaRuti> resultado = spHelper.ejecutarListado(
                SP_LIST, filtro, "L", BitTareaRuti.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // Mismo motivo que registrar(): ejecutarAbmMap y no ejecutarAbm. Aquí viajan
    // solo TRES parámetros y el resto tiene que tomar su DEFAULT NULL — con
    // ejecutarAbm (binding por metadatos) habría que mandarle un valor a los 15
    // que el proc declara, que es exactamente lo que rompió este mismo proc
    // cuando el archivo 27 le agregó @fecha y @traspasosXml.
    //
    // @fecha se omite a propósito: el SP resuelve "hoy" con GETDATE() del lado
    // de SQL Server. Mandar la fecha desde Java metería el reloj del servidor de
    // aplicación en la ecuación y, cerca de medianoche, podría abrir la
    // ocurrencia de ayer.
    @Override
    public RespuestaSp abrirFlujoARequerimiento(long idTarRuti, long codEmpleado, long audUsuario) {
        log.info("Abriendo flujo a requerimiento: tarea {}, empleado {}", idTarRuti, codEmpleado);
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", idTarRuti);
        params.put("codEmpleado", codEmpleado);
        params.put("audUsuario", audUsuario);
        RespuestaSp r = spHelper.ejecutarAbmMap("p_abm_tac_BitTareaRuti", params, "A");

        // Los bloques del proc son IF sueltos, sin ELSE final: un @ACCION que no
        // conoce NO falla — sale por abajo con @error=0, @errormsg='' e
        // idGenerado=0. O sea que si este backend se despliega sin haber corrido
        // el archivo SQL 40, la pantalla abriría el submódulo con idBitTarea=0 y
        // el flujo trabajaría contra una ocurrencia inexistente. La ACCION 'A'
        // siempre deja id y mensaje, así que un id en 0 sin error solo puede
        // significar eso.
        if (r != null && r.getError() == 0 && r.getIdGenerado() <= 0) {
            log.error("p_abm_tac_BitTareaRuti no reconoce ACCION='A': falta correr el script SQL 40.");
            return new RespuestaSp(40,
                    "Los submódulos a requerimiento todavía no están habilitados en la base de datos "
                            + "(falta correr el script 40).", 0);
        }
        return r;
    }

    // Mismo motivo que abrirFlujoARequerimiento: ejecutarAbmMap con solo los
    // parámetros de la rama, y la misma trampa de un @ACCION que el proc no
    // conoce. La rama 'O' siempre devuelve el id en idGenerado.
    @Override
    public RespuestaSp agregarObservacion(long idBitTarea, String obs, long audUsuario) {
        log.info("Agregando observación a la ocurrencia {}", idBitTarea);
        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarea", idBitTarea);
        params.put("obs", obs);
        params.put("audUsuario", audUsuario);
        RespuestaSp r = spHelper.ejecutarAbmMap("p_abm_tac_BitTareaRuti", params, "O");

        if (r != null && r.getError() == 0 && r.getIdGenerado() <= 0) {
            log.error("p_abm_tac_BitTareaRuti no reconoce ACCION='O': falta correr el script SQL 75.");
            return new RespuestaSp(75,
                    "Agregar observaciones todavía no está habilitado en la base de datos "
                            + "(falta correr el script 75).", 0);
        }
        return r;
    }

    @Override
    public List<BitacoraCumplimientoDto> tareasDelCierre(long idBitTarea, Date fecha, boolean todasSucursales) {
        Map<String, Object> p = new HashMap<>();
        p.put("idBitTarea", idBitTarea);
        p.put("fechaPresentacion", fecha);
        p.put("todasSucursales", todasSucursales ? 1 : 0);
        return spHelper.ejecutarListado(SP_LIST, p, "R", BitacoraCumplimientoDto.class);
    }

    // ==================== BITÁCORAS (archivo SQL 55) ====================
    // Las tres van por el overload de Map y mandan solo las claves con valor:
    // el resto toma su DEFAULT NULL, que en este proc es "sin filtro". Con el
    // overload de modelo, un id primitivo viajaría en 0 y el SP lo tomaría como
    // un filtro real.

    @Override
    public List<BitacoraCumplimientoDto> bitacoraCumplimiento(BitacoraFiltroRequest f, Long codEmpleadoJefe) {
        Map<String, Object> p = new HashMap<>();
        ponerSiHay(p, "fechaIni", f.getFechaIni());
        ponerSiHay(p, "fechaFin", f.getFechaFin());
        ponerSiHay(p, "codSucursal", f.getCodSucursal());
        ponerSiHay(p, "codCargo", f.getCodCargo());
        ponerSiHay(p, "codEmpleado", f.getCodEmpleado());
        ponerSiHay(p, "idTarRuti", f.getIdTarRuti());
        ponerSiHay(p, "cumplimiento", f.getCumplimiento());
        ponerSiHay(p, "codEmpleadoJefe", codEmpleadoJefe);
        return spHelper.ejecutarListado(SP_LIST, p, "B", BitacoraCumplimientoDto.class);
    }

    @Override
    public List<ResumenGeneracionDto> resumenGeneracion(Date fechaIni, Date fechaFin) {
        Map<String, Object> p = new HashMap<>();
        ponerSiHay(p, "fechaIni", fechaIni);
        ponerSiHay(p, "fechaFin", fechaFin);
        return spHelper.ejecutarListado(SP_LIST, p, "G", ResumenGeneracionDto.class);
    }

    @Override
    public List<DiagnosticoGeneracionDto> porQue(long codEmpleado, Date fecha, Long idTarRuti, Long codEmpleadoJefe) {
        Map<String, Object> p = new HashMap<>();
        p.put("codEmpleado", codEmpleado);
        ponerSiHay(p, "fechaPresentacion", fecha);
        ponerSiHay(p, "idTarRuti", idTarRuti);
        ponerSiHay(p, "codEmpleadoJefe", codEmpleadoJefe);
        return spHelper.ejecutarListado(SP_LIST, p, "P", DiagnosticoGeneracionDto.class);
    }

    /** Un nulo o un texto en blanco no viaja: el SP usa su DEFAULT NULL. */
    private static void ponerSiHay(Map<String, Object> p, String clave, Object valor) {
        if (valor == null) return;
        if (valor instanceof String && ((String) valor).trim().isEmpty()) return;
        p.put(clave, valor instanceof String ? ((String) valor).trim() : valor);
    }
}
