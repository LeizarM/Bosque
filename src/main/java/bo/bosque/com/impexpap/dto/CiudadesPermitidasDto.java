package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bo.bosque.com.impexpap.model.CiudadVenta;
import lombok.Getter;
import lombok.Setter;

/**
 * Lo que arma el selector de ciudad del catalogo de Ventas.
 *
 * <ul>
 *   <li>{@code ciudades}: entre cuales puede elegir, en orden.</li>
 *   <li>{@code todas}: si se ofrece "Todas" ({@code codCiudad = 0}). Para un
 *       administrador es el pais entero; para un usuario con ciudades asignadas
 *       es la suma de esas.</li>
 *   <li>{@code codCiudadInicial}: con cual arranca la pantalla (la del login si
 *       esta permitida; si no, la primera).</li>
 * </ul>
 */
@Getter
@Setter
public class CiudadesPermitidasDto implements Serializable {

    private List<CiudadVenta> ciudades = new ArrayList<>();
    private boolean todas;
    private boolean esAdmin;
    private int codCiudadInicial;

}
