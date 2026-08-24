package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Resultado de traer las notas de SAP, tal como lo devuelve
 * {@code p_abm_tcom_SincronizarNotas}.
 *
 * <p>Las tres respuestas posibles son "cargué", "no hacía falta" y "ya hay otra
 * corriendo", y las tres son normales: por eso vienen como datos y no como
 * error. La pantalla las trata igual —refresca y sigue—; el mensaje existe para
 * poder mostrar de cuándo son los números.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SincronizacionNotas implements Serializable {

    /** 1 si realmente se ejecutó la carga contra SAP. */
    private boolean sincronizado;
    /** Antigüedad de los datos ANTES de esta llamada, en minutos. */
    private Integer minutosDesde;
    private String  mensaje;
}
