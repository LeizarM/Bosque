package bo.bosque.com.impexpap.dto;

import java.io.Serializable;

import lombok.*;

/**
 * Cliente de SAP para el buscador del alta de garantias: {@code p_list_GarantiaCbr}
 * ACCION 'S' (maximo 50 filas, un cliente por CardCode aunque este en varias empresas).
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClienteSapDto implements Serializable {

    private String codClienteSAP;
    private String datoCliente;
}
