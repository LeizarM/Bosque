// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\FuenteCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

/**
 * Una fuente que se puede elegir para una sección nueva del cierre
 * ({@code p_list_tac_ResponsableCierre 'W'}, archivo SQL 86): una función o un
 * SP de otro módulo, registrado en {@code tac_fuenteCierre}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FuenteCierreDto {
    private String fuente;
    private String tipo;
    private String nombre;
    private String descripcion;
    /** 1 si la marca se guarda en el módulo de la fila (spMarcar). */
    private Integer guardaEnSuModulo;
    private Integer secciones;
    /** Archivo 87: el registro completo, para administrarlo. */
    private String spMarcar;
    private Integer estado;
    /** 1 si su función o SP existe en la base. */
    private Integer existe;
    /** 1 si devuelve las columnas mínimas (clave, titulo). */
    private Integer columnasOk;
    /** Las secciones que la usan, separadas por coma. */
    private String usadaPor;
}
