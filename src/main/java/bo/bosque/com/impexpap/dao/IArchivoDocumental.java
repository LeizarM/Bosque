package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DetalleArchivoRptDto;
import bo.bosque.com.impexpap.dto.EmpleadoArchivoDto;
import bo.bosque.com.impexpap.dto.FiltroReporteArchivo;
import bo.bosque.com.impexpap.dto.PedidoEntrega;
import bo.bosque.com.impexpap.dto.PedidoMovimientoCaja;
import bo.bosque.com.impexpap.dto.ResumenArchivoRptDto;
import bo.bosque.com.impexpap.dto.SucursalArchivoDto;
import bo.bosque.com.impexpap.model.ArchivoFisico;
import bo.bosque.com.impexpap.model.CajaArchivo;
import bo.bosque.com.impexpap.model.ContenidoCaja;
import bo.bosque.com.impexpap.model.DestruccionDocumento;
import bo.bosque.com.impexpap.model.EntregaDocumento;
import bo.bosque.com.impexpap.model.FotoCaja;
import bo.bosque.com.impexpap.model.PrestamoDocumento;
import bo.bosque.com.impexpap.model.TipoDocumentoArchivo;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Archivo de documentos (tablas {@code tarc_*}): un {@code p_abm_tarc_*} y un {@code p_list_tarc_*} por tabla.
 * En los listados, null significa "sin filtro"; un error de negocio del SP sale como SpBusinessException.
 */
public interface IArchivoDocumental {

    List<ArchivoFisico> listarArchivos();

    RespuestaSp registrarArchivo(ArchivoFisico mb, String acc);

    /** Sucursales para el combo del archivo ({@code p_list_tarc_Archivo} 'S'). */
    List<SucursalArchivoDto> listarSucursales();

    /** Empleados para elegir responsable o a quien prestar ({@code p_list_tarc_Archivo} 'E'). */
    List<EmpleadoArchivoDto> listarEmpleados();

    /** Reporte resumido: un tipo de documento por archivo ({@code p_list_tarc_Contenido} 'R'). */
    List<ResumenArchivoRptDto> resumenReporte(FiltroReporteArchivo filtro);

    /** Reporte detallado: un documento por fila ({@code p_list_tarc_Contenido} 'D'). */
    List<DetalleArchivoRptDto> detalleReporte(FiltroReporteArchivo filtro);

    /** Reporte de una caja: sus documentos con la situacion de cada uno ({@code p_list_tarc_Contenido} 'C'). */
    List<DetalleArchivoRptDto> contenidoCajaReporte(long codCaja);

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

    /** La foto o PDF por id, o null si no existe. */
    FotoCaja obtenerFoto(long codFoto);

    RespuestaSp registrarFoto(FotoCaja mb, String acc);

    /** Entregas a empleados, las que tienen algo afuera primero. */
    List<EntregaDocumento> listarEntregas();

    /** Entrega a un empleado, en un solo paso, las cajas completas y los documentos sueltos del pedido. */
    RespuestaSp registrarEntrega(PedidoEntrega pedido, long audUsuario);

    /** Registra la devolucion de todo lo que sigue afuera de la entrega, con la hora del servidor. */
    RespuestaSp devolverEntrega(long codEntrega, String observacion, long audUsuario);

    /** Anula una entrega registrada por error; con alguna devolucion ya es historial. */
    RespuestaSp anularEntrega(long codEntrega, long audUsuario);

    /** Renglones de las entregas, lo que sigue afuera primero; {@code codCaja} null = de todas las cajas. */
    List<PrestamoDocumento> listarPrestamos(Long codCaja);

    /** Registra la devolucion de un renglon con la hora del servidor. */
    RespuestaSp devolver(long codPrestamo, String observacionDevolucion, long audUsuario);

    /** Anula un renglon que sigue afuera (uno devuelto es historial); el ultimo se lleva su entrega. */
    RespuestaSp anularPrestamo(long codPrestamo, long audUsuario);

    /** La bitacora de destruccion, la mas nueva arriba; {@code codCaja} null = de todas las cajas. */
    List<DestruccionDocumento> listarDestrucciones(Long codCaja);

    /** Registra en la bitacora las lineas destruidas del pedido, o la caja completa si no trae lineas. */
    RespuestaSp destruir(PedidoMovimientoCaja pedido, long audUsuario);

    /** Anula un registro de destruccion hecho por error. */
    RespuestaSp anularDestruccion(long codDestruccion, long audUsuario);
}
