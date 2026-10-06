package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una opcion de un combo: el codigo que se guarda y el texto que se muestra. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionChequeDto implements Serializable {

    private String codigo;
    private String nombre;
}
