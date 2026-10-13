package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DetalleArchivoRptDto;
import bo.bosque.com.impexpap.dto.FiltroReporteArchivo;
import bo.bosque.com.impexpap.dto.ResumenArchivoRptDto;
import bo.bosque.com.impexpap.dto.SucursalArchivoDto;
import bo.bosque.com.impexpap.model.ArchivoFisico;
import bo.bosque.com.impexpap.model.CajaArchivo;
import bo.bosque.com.impexpap.model.ContenidoCaja;
import bo.bosque.com.impexpap.model.FotoCaja;
import bo.bosque.com.impexpap.model.TipoDocumentoArchivo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Los listados arman el Map a mano: los p_list_tarc_* toman NULL como "sin filtro" y un id en 0 si filtraria. */
@Slf4j
@Repository
public class ArchivoDocumentalDao implements IArchivoDocumental {

    private static final String LISTAR = "L";

    private final SpHelper spHelper;

    public ArchivoDocumentalDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<ArchivoFisico> listarArchivos() {
        return spHelper.ejecutarListado("p_list_tarc_Archivo", new HashMap<String, Object>(), LISTAR, ArchivoFisico.class);
    }

    @Override
    public RespuestaSp registrarArchivo(ArchivoFisico mb, String acc) {
        log.info("Archivo documental: {} accion {}", mb, acc);
        return spHelper.ejecutarAbm("p_abm_tarc_Archivo", mb, acc);
    }

    @Override
    public List<SucursalArchivoDto> listarSucursales() {
        return spHelper.ejecutarListado("p_list_tarc_Archivo", new HashMap<String, Object>(), "S",
                SucursalArchivoDto.class);
    }

    @Override
    public List<ResumenArchivoRptDto> resumenReporte(FiltroReporteArchivo filtro) {
        return spHelper.ejecutarListado("p_list_tarc_Contenido", filtrosDeReporte(filtro), "R",
                ResumenArchivoRptDto.class);
    }

    @Override
    public List<DetalleArchivoRptDto> detalleReporte(FiltroReporteArchivo filtro) {
        return spHelper.ejecutarListado("p_list_tarc_Contenido", filtrosDeReporte(filtro), "D",
                DetalleArchivoRptDto.class);
    }

    @Override
    public List<TipoDocumentoArchivo> listarTipos() {
        return spHelper.ejecutarListado("p_list_tarc_TipoDocumento", new HashMap<String, Object>(), LISTAR,
                TipoDocumentoArchivo.class);
    }

    @Override
    public RespuestaSp registrarTipo(TipoDocumentoArchivo mb, String acc) {
        log.info("Tipo de documento del archivo: {} accion {}", mb, acc);
        return spHelper.ejecutarAbm("p_abm_tarc_TipoDocumento", mb, acc);
    }

    @Override
    public List<CajaArchivo> listarCajas(Long codArchivo) {
        return spHelper.ejecutarListado("p_list_tarc_Caja", soloPositivo("codArchivo", codArchivo), LISTAR,
                CajaArchivo.class);
    }

    @Override
    public CajaArchivo obtenerCaja(long codCaja) {
        // Sin id el SP devolveria todas las cajas: se corta antes.
        if (codCaja <= 0) return null;
        List<CajaArchivo> filas = spHelper.ejecutarListado("p_list_tarc_Caja", soloPositivo("codCaja", codCaja), LISTAR,
                CajaArchivo.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public int siguienteNumero(long codArchivo) {
        if (codArchivo <= 0) return 1;
        List<CajaArchivo> filas = spHelper.ejecutarListado("p_list_tarc_Caja", soloPositivo("codArchivo", codArchivo),
                "N", CajaArchivo.class);
        Integer numero = filas.isEmpty() ? null : filas.get(0).getNumero();
        return numero == null ? 1 : numero;
    }

    @Override
    public RespuestaSp registrarCaja(CajaArchivo mb, String acc) {
        log.info("Caja del archivo: {} accion {}", mb, acc);
        return spHelper.ejecutarAbm("p_abm_tarc_Caja", mb, acc);
    }

    @Override
    public List<ContenidoCaja> listarContenido(Long codCaja) {
        return spHelper.ejecutarListado("p_list_tarc_Contenido", soloPositivo("codCaja", codCaja), LISTAR,
                ContenidoCaja.class);
    }

    @Override
    public RespuestaSp registrarContenido(ContenidoCaja mb, String acc) {
        log.info("Contenido de caja: {} accion {}", mb, acc);
        return spHelper.ejecutarAbm("p_abm_tarc_Contenido", mb, acc);
    }

    @Override
    public List<FotoCaja> listarFotos(long codCaja) {
        if (codCaja <= 0) return new ArrayList<>();
        return spHelper.ejecutarListado("p_list_tarc_CajaFoto", soloPositivo("codCaja", codCaja), LISTAR,
                FotoCaja.class);
    }

    @Override
    public FotoCaja obtenerFoto(long codFoto) {
        if (codFoto <= 0) return null;
        List<FotoCaja> filas = spHelper.ejecutarListado("p_list_tarc_CajaFoto", soloPositivo("codFoto", codFoto), LISTAR,
                FotoCaja.class);
        return filas.isEmpty() ? null : filas.get(0);
    }

    @Override
    public RespuestaSp registrarFoto(FotoCaja mb, String acc) {
        log.info("Foto de caja: {} accion {}", mb, acc);
        return spHelper.ejecutarAbm("p_abm_tarc_CajaFoto", mb, acc);
    }

    /** Los ids viajan solo si son positivos; la empresa tambien en 0, que el SP lee como "sin empresa". */
    private static Map<String, Object> filtrosDeReporte(FiltroReporteArchivo f) {
        Map<String, Object> params = new HashMap<>();
        if (f == null) return params;
        params.putAll(soloPositivo("codSucursal", f.getCodSucursal()));
        params.putAll(soloPositivo("codTipoDocumento", f.getCodTipoDocumento()));
        if (f.getCodEmpresa() != null && f.getCodEmpresa() >= 0) params.put("codEmpresa", f.getCodEmpresa());
        if (f.getAnioDesde() != null) params.put("anioDesde", f.getAnioDesde());
        if (f.getAnioHasta() != null) params.put("anioHasta", f.getAnioHasta());
        return params;
    }

    /** Un id nulo o no positivo no viaja: el SP lo recibe como NULL (sin filtro). */
    private static Map<String, Object> soloPositivo(String clave, Long valor) {
        Map<String, Object> params = new HashMap<>();
        if (valor != null && valor > 0) params.put(clave, valor);
        return params;
    }
}
