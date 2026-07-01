package bo.bosque.com.impexpap.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
public class DiaNoLaborable {
    private Date fecha;
    private String motivo;
    private int CodEmpleado;
}
