package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.*;

/**
 * Persona con su cargo vigente, para los combos del módulo CITE.
 *
 * <p>Cubre tres respuestas del SP de listado, porque la forma es la misma:
 * el listado de empleados activos (ACCION M) que alimenta el destinatario de
 * memorandos y comunicaciones internas, la búsqueda de uno por código
 * (ACCION C) y los datos del usuario logueado (ACCION U), que se usan para
 * precargar la firma. En esa última {@code codEmpleado} viene en 0.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CiteEmpleado implements Serializable {

    private long codEmpleado;
    private String nombreCompleto;
    private String cargo;
}
