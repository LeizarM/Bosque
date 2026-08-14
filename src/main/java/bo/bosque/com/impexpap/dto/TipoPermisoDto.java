package bo.bosque.com.impexpap.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Una opción del combo de tipo de permiso: {@code v_tipos} grupo 13 (9 códigos medidos en la base).
 *
 * <p>Alimenta dos combos con el mismo método: el del modal de "Programar permiso" —que excluye
 * {@code vac} y {@code pva}, igual que {@code Tipos.cargarList13A()} del legacy, porque esos dos
 * tienen su propia pantalla— y el filtro de la nómina, que los incluye a los nueve.
 *
 * <p>No hay {@code model/Tipos}: {@code v_tipos} es una vista de catálogo sin SP de listado, se lee
 * con un {@code SELECT} de dos columnas y no tiene más forma que ésta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TipoPermisoDto implements Serializable {

    /** {@code v_tipos.codigo} — lo que se graba en {@code trh_permiso.tipoPermiso}. */
    private String codigo;
    /** {@code v_tipos.descripcion} — lo que ve el usuario. */
    private String descripcion;
}
