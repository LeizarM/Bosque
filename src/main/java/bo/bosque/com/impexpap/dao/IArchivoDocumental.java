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

import java.util.List;

/**
 * Archivo de documentos (tablas {@code tarc_*}): un {@code p_abm_tarc_*} (I/U/D) y un {@code p_list_tarc_*} por tabla.
 * En los listados, null significa "sin filtro"; un error de negocio del SP sale como SpBusinessException.
 */
public interface IArchivoDocumental {

    List<ArchivoFisico> listarArchivos();

    RespuestaSp registrarArchivo(ArchivoFisico mb, String acc);

    /** Sucursales para el combo del archivo ({@code p_list_tarc_Archivo} 'S'). */
    List<SucursalArchivoDto> listarSucursales();

    /** Reporte resumido: un tipo de documento por archivo ({@code p_list_tarc_Contenido} 'R'). */
    List<ResumenArchivoRptDto> resumenReporte(FiltroReporteArchivo filtro);

    /** Reporte detallado: un documento por fila ({@code p_list_tarc_Contenido} 'D'). */
    List<DetalleArchivoRptDto> detalleReporte(FiltroReporteArchivo filtro);

    List<TipoDocumentoArchivo> listarTipos();

    RespuestaSp registrarTipo(TipoDocumentoArchivo mb, String acc);

    List<CajaArchivo> listarCajas(Long codArchivo);

    /** La caja por id, o null si no existe. */
    CajaArchivo obtenerCaja(long codCaja);

    /** El numero que sigue en un archivo (maximo + 1, o 1 si no tiene cajas). */
    int siguienteNumero(long codArchivo);

    RespuestaSp registrarCaja(CajaArchivo mb, String acc);

    List<ContenidoCaja> listarContenido(Long codCaja);

    RespuestaSp registrarContenido(ContenidoCaja mb, String acc);

    List<FotoCaja> listarFotos(long codCaja);

    /** La foto por id, o null si no existe. */
    FotoCaja obtenerFoto(long codFoto);

    RespuestaSp registrarFoto(FotoCaja mb, String acc);
}
