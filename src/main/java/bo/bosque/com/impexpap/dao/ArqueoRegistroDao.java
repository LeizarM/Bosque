// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ArqueoRegistroDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CorteArqueoDto;
import bo.bosque.com.impexpap.dto.DocumentacionArqueoDto;
import bo.bosque.com.impexpap.dto.RegistrarArqueoRequest;
import bo.bosque.com.impexpap.dto.ValeArqueoDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO del flujo especial "Arqueo de Caja" (idATR=2) — reemplaza dlgArqCaja
 * de WizardTareas.java. La lógica vive como ACCION='R' del proc genérico
 * p_abm_tac_ArqueoCajaSucursales (no en un proc con nombre propio — ver
 * sql/2026-09-03_tac_tareaRutinaria_24_flujos_especiales_sobre_procs_genericos.sql),
 * la misma tabla que {@link IArqueoCajaSucursales}/ArqueoCajaSucursalesDao
 * usa para el CRUD estándar (I/U/D/L) — este DAO queda separado solo porque
 * arma el XML de detalle, no porque llame a otro proc.
 */
@Slf4j
@Repository
public class ArqueoRegistroDao implements IArqueoRegistro {

    // NO static: SimpleDateFormat no es thread-safe (ver
    // TareaRutinariaConCargosDao para el mismo criterio ya aplicado aquí).
    private SimpleDateFormat formatoFecha() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    private final SpHelper spHelper;

    public ArqueoRegistroDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(RegistrarArqueoRequest req, long codEmpleadoEncargado, long audUsuario) {
        Map<String, Object> params = new HashMap<>();
        params.put("idTarRuti", req.getIdTarRuti());
        params.put("idBitTarea", req.getIdBitTarea());
        params.put("codEmpleadoEncargado", codEmpleadoEncargado);
        params.put("saldoMovSap", req.getSaldoMovSap() != null ? req.getSaldoMovSap() : 0);
        params.put("tc", req.getTc() != null ? req.getTc() : 1);
        params.put("obs", req.getObs());
        params.put("cortesXml", construirCortesXml(req.getCortes()));
        params.put("documentacionXml", construirDocumentacionXml(req.getDocumentacion()));
        params.put("valesXml", construirValesXml(req.getVales()));
        params.put("audUsuario", audUsuario);

        return spHelper.ejecutarAbmMap("p_abm_tac_ArqueoCajaSucursales", params, "R");
    }

    private String construirCortesXml(List<CorteArqueoDto> cortes) {
        StringBuilder xml = new StringBuilder("<cortes>");
        if (cortes != null) {
            for (CorteArqueoDto c : cortes) {
                xml.append("<c ic=\"").append(c.getIdCorte()).append("\" cant=\"").append(c.getCantidad()).append("\"/>");
            }
        }
        return xml.append("</cortes>").toString();
    }

    private String construirDocumentacionXml(List<DocumentacionArqueoDto> docs) {
        StringBuilder xml = new StringBuilder("<docs>");
        if (docs != null) {
            for (DocumentacionArqueoDto d : docs) {
                xml.append("<d id=\"").append(d.getIdDoc()).append("\" m=\"").append(d.getMonto()).append("\"/>");
            }
        }
        return xml.append("</docs>").toString();
    }

    private String construirValesXml(List<ValeArqueoDto> vales) {
        SimpleDateFormat formato = formatoFecha();
        StringBuilder xml = new StringBuilder("<vales>");
        if (vales != null) {
            for (ValeArqueoDto v : vales) {
                xml.append("<v");
                if (v.getNumVale() != null) xml.append(" nv=\"").append(v.getNumVale()).append('"');
                atributo(xml, "nom", v.getNombre());
                if (v.getMonto() != null) xml.append(" m=\"").append(v.getMonto()).append('"');
                if (v.getFecha() != null) xml.append(" f=\"").append(formato.format(v.getFecha())).append('"');
                if (v.getCodEmpresa() != null) xml.append(" ce=\"").append(v.getCodEmpresa()).append('"');
                atributo(xml, "o", v.getObs());
                xml.append("/>");
            }
        }
        return xml.append("</vales>").toString();
    }

    private void atributo(StringBuilder xml, String nombre, String valor) {
        if (valor == null || valor.isEmpty()) return;
        xml.append(' ').append(nombre).append("=\"").append(escaparXml(valor)).append('"');
    }

    /** cliente/obs/nombre son texto libre — ver el mismo criterio en CajaFuerteDao. */
    private String escaparXml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
