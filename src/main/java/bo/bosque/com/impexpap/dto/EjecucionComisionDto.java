package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/** Periodo sobre el que se carga o ejecuta el pago de comisiones. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EjecucionComisionDto implements Serializable {

    private int  mes;
    private int  anio;
    /** 1 vendedores internos, 0 externos. */
    private int  esInterno;
    private long audUsuario;
}
