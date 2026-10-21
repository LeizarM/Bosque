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
 * Fila de {@code tcpe_rsptaSemana} (v12/v13): lo que un usuario de BOSQUE respondió a una pregunta de la semana (un
 * texto si es abierta, un número si es de escala), una sola respuesta por pregunta. Se lee con {@code p_list_tcpe_RsptaSemana}; se registra desde el inicio con
 * {@code p_abm_tcpe_RsptaSemana 'I'} y el usuario del token, nunca por el catálogo genérico.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeRsptaSemana implements Serializable {

    private static final long serialVersionUID = 1L;

    /** bigint. PK IDENTITY. */
    private Long codRsptaSemana;

    /** bigint. NOT NULL. FK a {@code tcpe_preguntaSemana}. */
    private Long codPreguntaSemana;

    /** bigint. NOT NULL. FK a {@code tb_usuario}: quien respondió. */
    private Long codUsuario;

    /** nvarchar(1000). Abierta: el texto (el largo lo fija {@code maxRespuestaSemana}); de escala, NULL. */
    @Size(max = 1000, message = "Máximo 1000 caracteres.")
    private String respuesta;

    /** tinyint. De escala: el número elegido, dentro del rango de la pregunta; abierta, NULL. */
    private Integer valor;

    /** bigint. Lo pone el backend con el usuario del token; nunca sale del cuerpo. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;

    /** datetime. Cuándo respondió (la sella el procedimiento). */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;

    /** Login de {@code tb_usuario} según {@code audUsuario}. Solo lectura: lo agrega {@code p_list_tcpe_RsptaSemana}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String nombreUsuario;
}
