package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/** Una lista de precio activa: el VPP de la columna y la sucursal que la agrupa. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ListaVistaPropuestaDto implements Serializable {

    /** 1 a 12: la columna del PDF. */
    private Integer vpp;

    /** tb_sucursal.nombre. */
    private String sucursal;
}
