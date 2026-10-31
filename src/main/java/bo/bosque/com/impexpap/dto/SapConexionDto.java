package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/** Una conexión a SAP para cargar su contraseña, probarla o quitarla; {@code clave} solo al cargarla. */
@Getter
@Setter
public class SapConexionDto {

    private Integer idConexion;
    /** La contraseña del Service Layer, en claro solo en este request: el backend la cifra antes de guardarla. */
    private String clave;
}
