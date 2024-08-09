package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistroResma implements Serializable {

        private int idMer;
        private Date fecha;
        private float totalPeso;
        private float totalUSD;
        private String obs;
        private int codEmpleado;
        private int audUsuario;


}
