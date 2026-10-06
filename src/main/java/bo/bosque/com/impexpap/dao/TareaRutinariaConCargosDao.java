// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\TareaRutinariaConCargosDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CargoAsignacionDto;
import bo.bosque.com.impexpap.dto.RegistrarTareaRutinariaRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class TareaRutinariaConCargosDao implements ITareaRutinariaConCargos {

    // p_registrar_tac_tareaRutinariaConCargos espera fechaInicio/fechaFin
    // como DATE (sin hora) en los atributos fi/ff del XML.
    // NO static: SimpleDateFormat no es thread-safe: compartir una sola
    // instancia entre requests concurrentes de este bean singleton puede
    // corromper la fecha formateada. Se crea una instancia nueva por llamada.
    private SimpleDateFormat formatoFecha() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    private final SpHelper spHelper;

    public TareaRutinariaConCargosDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(RegistrarTareaRutinariaRequest req) {
        log.info("Registrando tarea rutinaria con cargos: {} cargo(s), codUsuario={}, modoJefe={}",
                req.getCargos() == null ? 0 : req.getCargos().size(), req.getCodUsuario(), req.isModoJefe());

        Map<String, Object> params = new HashMap<>();
        params.put("descripcion", req.getDescripcion());
        params.put("idFrec", req.getIdFrec());
        params.put("idArea", req.getIdArea());
        params.put("fechaPartida", req.getFechaPartida());
        params.put("idATR", req.getIdATR());
        params.put("iniFin", req.getIniFin());
        params.put("cargosXml", construirCargosXml(req.getCargos()));
        params.put("codUsuario", req.getCodUsuario());
        params.put("modoJefe", req.isModoJefe());
        // nivelMaximoJefe ya NO se envía — el SP lo resuelve de dbo.tac_configuracion.

        // El proc no tiene un dispatch real por ACCION (no es un ABM I/U/D) —
        // el valor "R" es un relleno sin efecto en el SP, solo para que
        // ejecutarAbmMap pueda invocarlo (siempre inyecta @ACCION). Ver
        // 2026-09-02_tac_tareaRutinaria_07_fix_sphelper_compat.sql.
        return spHelper.ejecutarAbmMap("p_registrar_tac_tareaRutinariaConCargos", params, "R");
    }

    /**
     * Formato esperado por el SP — atributos OMITIDOS (no vacíos) cuando el
     * valor es NULL, para que .value('@x','TIPO') devuelva NULL en vez de
     * fallar al castear un string vacío:
     *   <cargos><c cc="131"/><c cc="185" ccs="251" fi="2026-09-02" ff="2026-12-31"/></cargos>
     */
    private String construirCargosXml(java.util.List<CargoAsignacionDto> cargos) {
        SimpleDateFormat formato = formatoFecha();
        StringBuilder xml = new StringBuilder("<cargos>");
        if (cargos != null) {
            for (CargoAsignacionDto c : cargos) {
                xml.append("<c cc=\"").append(c.getCodCargo()).append('"');
                if (c.getCodCargoSucursal() != null) {
                    xml.append(" ccs=\"").append(c.getCodCargoSucursal()).append('"');
                }
                if (c.getFechaInicio() != null) {
                    xml.append(" fi=\"").append(formato.format(c.getFechaInicio())).append('"');
                }
                if (c.getFechaFin() != null) {
                    xml.append(" ff=\"").append(formato.format(c.getFechaFin())).append('"');
                }
                xml.append("/>");
            }
        }
        xml.append("</cargos>");
        return xml.toString();
    }
}
