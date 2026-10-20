package bo.bosque.com.impexpap.model;

import bo.bosque.com.impexpap.dto.FechaDiaJson;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * Fila de {@code tcpe_convocatoria} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Convocatoria} y se escribe con {@code p_abm_tcpe_Convocatoria}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeConvocatoria implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codConvocatoria;

    /** bigint. NULL. */
    private Long codArea;

    /** bigint. NULL. */
    private Long codDpto;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate gestion;

    /** bit. NULL. */
    private Boolean esInterna;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String titulo;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String observacion;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String descripcion;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String nombrePuesto;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String competencias;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String objCargo;

    /** varchar(150). NULL. */
    @Size(max = 150, message = "Máximo 150 caracteres.")
    private String funciones;

    /** varchar(50). NULL. */
    @Size(max = 50, message = "Máximo 50 caracteres.")
    private String estado;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate proFechaInicio;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate proFechaFin;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaInicio;

    /** date. NULL. Viaja como yyyy-MM-dd. */
    @FechaDiaJson
    private LocalDate fechaFin;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Convocatoria}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
