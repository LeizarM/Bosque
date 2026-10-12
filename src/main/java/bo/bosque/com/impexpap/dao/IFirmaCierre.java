// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IFirmaCierre.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CeldaCierreDto;
import bo.bosque.com.impexpap.dto.CeldaTableroDto;
import bo.bosque.com.impexpap.dto.FilaSeccionDto;
import bo.bosque.com.impexpap.dto.FilaSeccionRequest;
import bo.bosque.com.impexpap.dto.FirmarSeccionRequest;
import bo.bosque.com.impexpap.dto.ResumenCierrePersonaDto;
import bo.bosque.com.impexpap.utils.ListadoConEstado;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/**
 * Verificar Cierre por responsables (archivo SQL 81): la tarea 39 es una sola,
 * pero cada sección la firma quien la matriz dice. Las reglas viven en
 * {@code p_abm_tac_FirmaCierre}; aquí solo se arman los parámetros.
 */
public interface IFirmaCierre {

    /** [M] Las celdas de una ocurrencia de la 39, con su firma. */
    List<CeldaCierreDto> misCeldas(long idBitTarea);

    /** [F] Firma una celda. Con la última, la ocurrencia queda hecha (lo hace el SP). */
    RespuestaSp firmar(FirmarSeccionRequest req, long audUsuario);

    /** [X] Anula una firma; el SP exige motivo y que quien anula sea responsable. */
    RespuestaSp anular(long idFC, String obs, long audUsuario);

    /**
     * [M] Marca o desmarca una fila (A arqueo, F llegada) de una celda propia.
     * El SP firma la celda con la última marca, o anula su firma al quitar una.
     */
    RespuestaSp marcarFila(long idBitTarea, String seccion, long idFila, int marca, long audUsuario);

    /** [G] Las filas de una celda con fuente; si no se pudo leer, el estado trae el motivo (archivo SQL 86). */
    ListadoConEstado<FilaSeccionDto> filasSeccion(FilaSeccionRequest req, long audUsuario);

    /** [G] Marca o desmarca una fila de una celda con fuente. */
    RespuestaSp marcarFilaSeccion(FilaSeccionRequest req, long audUsuario);

    /** [K] El tablero: las celdas de quien mira en el rango. */
    List<CeldaTableroDto> tablero(Date fechaIni, Date fechaFin, long audUsuario);

    /** [P] El resumen por persona de esas mismas celdas. */
    List<ResumenCierrePersonaDto> resumen(Date fechaIni, Date fechaFin, long audUsuario);
}
