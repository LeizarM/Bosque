package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

public class Color implements Serializable {

    private int idColor;
    private String color;
    private int estado;
    private int audUsuario;
}
