package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.FamiliaPolitica;
import bo.bosque.com.impexpap.model.DescuentoDetalle;
import bo.bosque.com.impexpap.model.FamiliaSapOpcion;
import bo.bosque.com.impexpap.model.VendedorClienteExcluido;
import bo.bosque.com.impexpap.model.VendedorExentoBond;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacion contra los p_abm_ / p_list_ de las tres tablas de politica.
 * <p>
 * Se usa el overload de Map y no el de modelo: los modelos llevan campos de solo
 * lectura -grpFam, nomVenSAP- que los SP de escritura no declaran, y mandarlos
 * haria fallar la llamada.
 */
@Repository
public class PoliticaBondDao implements IPoliticaBond {

    private static final String SP_LIST_SAP = "p_list_tcom_GrupoFamiliaSap";
    private static final String SP_LIST_DET = "p_list_tcom_DescuentoDetalle";
    private static final String SP_ABM_FAM  = "p_abm_tcom_FamiliaPolitica";
    private static final String SP_LIST_FAM = "p_list_tcom_FamiliaPolitica";
    private static final String SP_ABM_EXE  = "p_abm_tcom_VendedorExentoBond";
    private static final String SP_LIST_EXE = "p_list_tcom_VendedorExentoBond";
    private static final String SP_ABM_CLI  = "p_abm_tcom_VendedorClienteExcluido";
    private static final String SP_LIST_CLI = "p_list_tcom_VendedorClienteExcluido";

    private final SpHelper spHelper;

    public PoliticaBondDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    // ==================== DETALLE DE LO DESCONTADO ====================

    @Override
    public List<DescuentoDetalle> obtenerDescuentoDetalle(Integer mes, Integer anio,
                                                          String origen, Long idVendedor,
                                                          String accion) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("mes", mes);
        p.put("anio", anio);
        p.put("origen", origen);
        p.put("idVendedor", idVendedor);
        return spHelper.ejecutarListado(SP_LIST_DET, p, accion, DescuentoDetalle.class);
    }

    // ==================== FAMILIAS DE SAP ====================

    @Override
    public List<FamiliaSapOpcion> obtenerFamiliasSap(boolean disponibles) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idGrpFamiliaSap", null);
        return spHelper.ejecutarListado(SP_LIST_SAP, p,
                disponibles ? "D" : "L", FamiliaSapOpcion.class);
    }

    // ==================== POLITICA POR FAMILIA ====================

    @Override
    public RespuestaSp registrarFamiliaPolitica(FamiliaPolitica mb, String acc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idFamPolitica",   mb.getIdFamPolitica());
        p.put("idGrpFamiliaSap", mb.getIdGrpFamiliaSap());
        p.put("porcentajePago",  mb.getPorcentajePago());
        p.put("vigenteDesde",    mb.getVigenteDesde());
        p.put("vigenteHasta",    mb.getVigenteHasta());
        p.put("activo",          mb.getActivo());
        p.put("audUsuario",      mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM_FAM, p, acc);
    }

    @Override
    public List<FamiliaPolitica> obtenerFamiliaPolitica() {
        return spHelper.ejecutarListado(SP_LIST_FAM, filtroFam(null), "L", FamiliaPolitica.class);
    }

    @Override
    public List<FamiliaPolitica> obtenerFamiliaPoliticaVigente(Date fecha) {
        return spHelper.ejecutarListado(SP_LIST_FAM, filtroFam(fecha), "A", FamiliaPolitica.class);
    }

    private Map<String, Object> filtroFam(Date fecha) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idFamPolitica",   null);
        p.put("idGrpFamiliaSap", null);
        p.put("fecha",           fecha);
        return p;
    }

    // ==================== VENDEDORES EXENTOS ====================

    @Override
    public RespuestaSp registrarVendedorExento(VendedorExentoBond mb, String acc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVenExento",  mb.getIdVenExento());
        p.put("idVendedor",   mb.getIdVendedor());
        p.put("vigenteDesde", mb.getVigenteDesde());
        p.put("vigenteHasta", mb.getVigenteHasta());
        p.put("activo",       mb.getActivo());
        p.put("motivo",       mb.getMotivo());
        p.put("audUsuario",   mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM_EXE, p, acc);
    }

    @Override
    public List<VendedorExentoBond> obtenerVendedoresExentos() {
        return spHelper.ejecutarListado(SP_LIST_EXE, filtroExe(null), "L", VendedorExentoBond.class);
    }

    @Override
    public List<VendedorExentoBond> obtenerVendedoresExentosVigentes(Date fecha) {
        return spHelper.ejecutarListado(SP_LIST_EXE, filtroExe(fecha), "A", VendedorExentoBond.class);
    }

    private Map<String, Object> filtroExe(Date fecha) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVenExento", null);
        p.put("idVendedor",  null);
        p.put("fecha",       fecha);
        return p;
    }

    // ==================== CLIENTES EXCLUIDOS ====================

    @Override
    public RespuestaSp registrarClienteExcluido(VendedorClienteExcluido mb, String acc) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVenCliExc",  mb.getIdVenCliExc());
        p.put("idVendedor",   mb.getIdVendedor());
        p.put("cardCode",     mb.getCardCode());
        p.put("origen",       mb.getOrigen());
        p.put("vigenteDesde", mb.getVigenteDesde());
        p.put("vigenteHasta", mb.getVigenteHasta());
        p.put("activo",       mb.getActivo());
        p.put("motivo",       mb.getMotivo());
        p.put("audUsuario",   mb.getAudUsuario());
        return spHelper.ejecutarAbmMap(SP_ABM_CLI, p, acc);
    }

    @Override
    public List<VendedorClienteExcluido> obtenerClientesExcluidos() {
        return spHelper.ejecutarListado(SP_LIST_CLI, filtroCli(null), "L",
                VendedorClienteExcluido.class);
    }

    @Override
    public List<VendedorClienteExcluido> obtenerClientesExcluidosVigentes(Date fecha) {
        return spHelper.ejecutarListado(SP_LIST_CLI, filtroCli(fecha), "A",
                VendedorClienteExcluido.class);
    }

    private Map<String, Object> filtroCli(Date fecha) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("idVenCliExc", null);
        p.put("idVendedor",  null);
        p.put("cardCode",    null);
        p.put("fecha",       fecha);
        return p;
    }
}
