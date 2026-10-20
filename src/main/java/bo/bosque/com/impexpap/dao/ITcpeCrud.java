package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Contrato común de las 38 tablas {@code tcpe_}: un {@code p_list_tcpe_<Entidad>} y un {@code p_abm_tcpe_<Entidad>}
 * con la misma forma; sus metadatos evitan un {@code switch} de 38 casos en el catálogo genérico.
 * @param <T> el modelo {@code Tcpe<Entidad>}
 */
public interface ITcpeCrud<T> {

    /**
     * {@code p_list_tcpe_<Entidad> 'L'} con los filtros que traiga el modelo: PK, FK y {@code activo}.
     * Un filtro nulo, o un código en 0, no se manda (en estos SP NULL es "sin filtro").
     * @param filtro modelo con los filtros; null lista todo
     */
    List<T> listar(T filtro);

    /** Un registro por su PK, o null si no existe. */
    T obtener(Long id);

    /**
     * {@code p_abm_tcpe_<Entidad>}: alta si la PK es nula o 0, modificación si no. En las tablas cuya PK no es
     * IDENTITY, sin código el SP asigna el siguiente número y con código la acción se decide según exista.
     * @param audUsuario el {@code codUsuario} del token, nunca del cuerpo
     */
    RespuestaSp guardar(T modelo, long audUsuario);

    /** {@code p_abm_tcpe_<Entidad> 'D'}. El SP revisa los hijos antes de borrar. */
    RespuestaSp eliminar(Long id, long audUsuario);

    /** Segmento de la ruta del catálogo: el nombre de la tabla sin {@code tcpe_} ({@code rsptaOpcionH}). */
    String entidad();

    /** La clase del modelo, para convertir el cuerpo del pedido. */
    Class<T> tipo();

    /** Nombre de la propiedad PK ({@code codRsptaOpH}). */
    String pk();
}
