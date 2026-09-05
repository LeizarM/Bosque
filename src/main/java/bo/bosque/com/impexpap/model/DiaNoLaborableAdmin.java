package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DiaNoLaborableAdmin implements Serializable {

    private long idDiaNoLaborable;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    private String motivo;

    /** CSV de codSucursal seleccionados (ej. "3,7,12"). NULL/vacio = feriado GLOBAL. Solo se usa al registrar. */
    private String sucursales;

    private long audUsuario;

    /** Solo lectura: "Global" o "N sucursal(es)". Lo calcula p_list_rrhh_DiaNoLaborable; el ABM lo ignora. */
    private String alcance;

    /** Filtro de listado por anio (YEAR(fecha)). 0 = sin filtrar por gestion. No participa en el ABM. */
    private int gestion;
}
