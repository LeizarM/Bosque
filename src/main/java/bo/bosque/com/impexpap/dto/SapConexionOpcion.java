package bo.bosque.com.impexpap.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Una conexión a SAP tal como la ve un módulo en su desplegable: sin URL, usuario ni contraseña, que
 * solo se ven en Administración &gt; Conexiones SAP.
 */
@Getter
@Builder
public class SapConexionOpcion {

    private final Integer idConexion;
    private final String nombre;
    private final Integer codEmpresa;
    private final String empresa;
    /** {@code PRODUCCION} o {@code PRUEBA}. */
    private final String ambiente;
    private final String baseSap;
    private final boolean habilitada;
    private final boolean claveCargada;
}
