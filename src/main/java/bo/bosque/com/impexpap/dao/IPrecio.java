package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.PrecioADto;
import bo.bosque.com.impexpap.dto.PrecioCDto;
import bo.bosque.com.impexpap.dto.PrecioDDto;
import bo.bosque.com.impexpap.dto.PrecioGDto;
import bo.bosque.com.impexpap.model.Precio;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Acceso a dbo.tpr_precio. Todo pasa por procedimiento almacenado:
 * {@code p_abm_precio} para escribir y {@code p_list_precio} para leer.
 *
 * <p>Un metodo por ACCION real del procedimiento. La rama {@code 'F'} de
 * {@code p_list_precio} NO se expone: esta muerta (ver {@link PrecioDao}).
 */
public interface IPrecio {

    /**
     * ABM sobre tpr_precio. Invoca {@code p_abm_precio}.
     *
     * @param precio datos a grabar; en 'I' el idPrecio viaja en null
     * @param accion "I" alta, "U" modificacion, "D" baja fisica
     * @return respuesta del SP; {@code getIdGenerado()} trae el idPrecio nuevo en el alta
     */
    RespuestaSp registrar(Precio precio, String accion);

    /**
     * Listado de tpr_precio. Invoca {@code p_list_precio @ACCION = 'L'}.
     *
     * @param filtro campos en null = sin filtro; null devuelve la tabla entera
     */
    List<Precio> listar(Precio filtro);

    /**
     * Un registro por su PK. Invoca {@code p_list_precio @ACCION = 'L'}.
     *
     * @return el registro, o null si no existe
     */
    Precio obtenerPorId(long idPrecio);

    /**
     * Ficha de la familia con su precio, para llenar el formulario de captura.
     * Invoca {@code p_list_precio @ACCION = 'A'}.
     */
    List<PrecioADto> obtenerDatosCaptura(int codigoFamilia);

    /**
     * Precios de una familia, uno por clasificacion y sucursal.
     * Invoca {@code p_list_precio @ACCION = 'C'}.
     */
    List<PrecioCDto> listarPorFamilia(int codigoFamilia);

    /**
     * Grilla completa de reprecio de una familia (producto, sucursal, lista,
     * precio, porcentaje, iva e it). Invoca {@code p_list_precio @ACCION = 'D'}.
     */
    List<PrecioDDto> listarDetalleParaReprecio(int codigoFamilia);

    /**
     * Igual que {@link #listarDetalleParaReprecio(int)} pero dejando fuera los
     * precios que ya estan cargados en la propuesta indicada, para no
     * duplicarlos. Invoca {@code p_list_precio @ACCION = 'E'}.
     */
    List<PrecioDDto> listarNoIncluidosEnPropuesta(int codigoFamilia, long idPropuesta);

    /**
     * Precios de una familia dentro de una propuesta: actual contra propuesto.
     * Invoca {@code p_list_precio @ACCION = 'G'}.
     */
    List<PrecioGDto> listarDePropuesta(int codigoFamilia, long idPropuesta);
}
