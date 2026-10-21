package bo.bosque.com.impexpap.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Tabla {@code tb_funcionesXModulo}: una función de un módulo (Cheques: cajero, cobrador...).
 * {@code modulo} y {@code asignaciones} solo los llena {@code p_list_FuncionesXModulo 'P'}.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class FuncionesXModulo implements Serializable {

    private long codFuncMod;
    /** {@code tb_vista.codVista} del módulo. */
    private long codModulo;
    private String modulo;
    private String funcion;
    private String detalle;
    /** SI / NO. */
    private String requerido;
    /** 1 activo, 0 inactivo. */
    private String estado;
    private long audUsuario;
    private int asignaciones;
}
