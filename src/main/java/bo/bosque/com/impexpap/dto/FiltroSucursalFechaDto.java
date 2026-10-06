package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sucursal y un dia: para listar las entregas a cobranza de ese dia ("Dar Custodia"). */
@Getter
@Setter
@NoArgsConstructor
public class FiltroSucursalFechaDto implements Serializable {

    private Long codSucursal;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;
}
