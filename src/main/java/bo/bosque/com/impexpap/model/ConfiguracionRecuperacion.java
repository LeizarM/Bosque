package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/** Fila de trec_configuracion. En los de tipo 'secreto' el valor va cifrado y nunca sale hacia la app. */
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionRecuperacion implements Serializable {

    private String clave;
    private String valor;
    private String nombre;
    private String descripcion;
    private String tipo;
    private String opciones;
    private Integer minimo;
    private Integer maximo;
    private String grupo;
    private int orden;
    private Long audUsuario;
    private Date audFecha;

    // Apoyo para la pantalla: si el parámetro tiene valor (en los secretos, el valor no viaja).
    private boolean configurado;
}
