package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una opción de un combo de las pantallas de parámetros y de la agenda: el código que se guarda,
 * el texto que se muestra y un grupo para ordenar o filtrar (la empresa de una sucursal, por ejemplo).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionDto implements Serializable {

    private long codigo;
    private String nombre;
    private String grupo;
}
