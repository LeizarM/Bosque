// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\SeccionCierreDto.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.Date;

/**
 * Una sección del catálogo de Verificar Cierre ({@code p_list_tac_ResponsableCierre
 * 'E'}, archivo SQL 86). Las propias (A F T C R) tienen panel programado; las
 * demás leen sus filas de una fuente (función o SP de otro módulo).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SeccionCierreDto {
    private String seccion;
    private String nombre;
    private String queRevisar;
    private Integer porSucursal;
    private Integer propia;
    private String fuente;
    private String nombreFuente;
    private Integer marcaPorFila;
    private Integer firmaSola;
    private Integer obsDesde;
    private String diaRevisado;
    private Integer orden;
    private Date desde;
    private Date hasta;
    /** 1 si se pide hoy. */
    private Integer vigente;
    private Integer responsables;
}
