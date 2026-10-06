// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\CajaFuerteDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.LlegadaCajaFuerteDto;
import bo.bosque.com.impexpap.dto.RegistrarCajaFuerteRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Caja Fuerte" (idATR=4) — reemplaza dlgCajaFuerte de
 * WizardTareas.java. La lógica vive como ACCION='R' del proc genérico
 * p_abm_tac_Llegada (no en un proc con nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql).
 */
@Slf4j
@Repository
public class CajaFuerteDao implements ICajaFuerte {

    private final SpHelper spHelper;

    public CajaFuerteDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(RegistrarCajaFuerteRequest req, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", req.getIdTarRuti());
        params.put("idBitTarRuti", req.getIdBitTarea());
        params.put("llegadasXml", construirLlegadasXml(req.getLlegadas()));
        params.put("audUsuario", audUsuario);

        return spHelper.ejecutarAbmMap("p_abm_tac_Llegada", params, "R");
    }

    private String construirLlegadasXml(List<LlegadaCajaFuerteDto> llegadas) {
        StringBuilder xml = new StringBuilder("<llegadas>");
        if (llegadas != null) {
            for (LlegadaCajaFuerteDto l : llegadas) {
                xml.append("<l");
                atributo(xml, "cliente", l.getCliente());
                atributo(xml, "moneda", l.getMoneda());
                if (l.getImporte() != null) {
                    xml.append(" importe=\"").append(l.getImporte()).append('"');
                }
                atributo(xml, "tipo", l.getTipo());
                atributo(xml, "destino", l.getDestino());
                atributo(xml, "obs", l.getObs());
                xml.append("/>");
            }
        }
        xml.append("</llegadas>");
        return xml.toString();
    }

    private void atributo(StringBuilder xml, String nombre, String valor) {
        if (valor == null || valor.isEmpty()) return;
        xml.append(' ').append(nombre).append("=\"").append(escaparXml(valor)).append('"');
    }

    /**
     * cliente/obs/etc. son texto libre del usuario — a diferencia de
     * construirCargosXml (solo ids/fechas, sin riesgo), aquí SÍ hace falta
     * escapar & &lt; &gt; " para no romper el XML (o peor, inyectar
     * elementos &lt;l&gt; extra) si alguien escribe un nombre con esos
     * caracteres.
     */
    private String escaparXml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
