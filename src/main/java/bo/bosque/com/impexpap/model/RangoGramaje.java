package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RangoGramaje implements Serializable {

    private int idRangoGram;
    private float min;
    private float max;
    private int audUsuario;

}
