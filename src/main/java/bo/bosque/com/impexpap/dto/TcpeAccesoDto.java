package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Fila de {@code p_list_tcpe_Acceso} {@code L} (login de un externo) y {@code M} (claves sin hash). <b>Uso interno</b>: lleva {@code password2} y nunca se devuelve al cliente.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class TcpeAccesoDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long codUsuario;

    private String login;

    /** Hash BCrypt o, en los no migrados, la clave en texto plano del legacy. */
    @JsonIgnore
    @ToString.Exclude
    private String password2;

    private Boolean activo;

    private Long codTipoUsuario;

    private String ci;

    private String nombres;

    private String apellidoPaterno;

    private String apellidoMaterno;

    private Boolean datosCompletos;

    /** Solo {@code L}: exámenes {@code SIN_INICIAR} o {@code EN_CURSO} sin vencer (dato informativo). */
    private Integer examenesPendientes;

    /** Solo {@code L}: lo decide SQL (contrato 7.4). En false, el login responde 401 con {@code mensajeIngreso}. */
    private Boolean puedeIngresar;

    private String mensajeIngreso;
}
