package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * Fila de {@code tcpe_examen} (módulo de exámenes). Mismos nombres que las columnas, con tipos envoltorio
 * para que un NULL de la base siga siendo null (y no un 0 que el SP tomaría como filtro o como dato).
 * Se lee con {@code p_list_tcpe_Examen} y se escribe con {@code p_abm_tcpe_Examen}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeExamen implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. Nulo o 0 = alta. */
    private Long codExamen;

    /** bigint. NULL. */
    private Long codRolExamen;

    /** bigint. NULL. */
    private Long codUsuario;

    /** bigint. NULL. */
    private Long codPostulacion;

    /** bit. NULL. */
    private Boolean esPostulacion;

    /** float. NULL. */
    private Double resultado;

    /** varchar(200). NULL. */
    @Size(max = 200, message = "Máximo 200 caracteres.")
    private String observaciones;

    /** varchar(100). NULL. */
    @Size(max = 100, message = "Máximo 100 caracteres.")
    private String titulo;

    /** datetime. NULL. */
    private Date fechaInicio;

    /** datetime. NULL. */
    private Date fechaFin;

    /** bigint. NULL. v5: el {@code codPreguntaH} que está viendo (latido). */
    private Long preguntaActual;

    /** datetime. NULL. v5: último latido o respuesta. */
    private Date ultimaActividad;

    /** int. NOT NULL DEFAULT 0. v5: salidas de la pestaña; nulo toma el default del SP. */
    private Integer salidasPestana;

    /** datetime. NULL. v5: última salida de la pestaña. */
    private Date ultimaSalida;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. La sella el procedimiento. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_Examen}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
