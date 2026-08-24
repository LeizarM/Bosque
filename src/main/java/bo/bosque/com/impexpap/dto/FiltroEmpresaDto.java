package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/** Filtro por empresa SAP (columna bd). 0 significa "todas". */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroEmpresaDto implements Serializable {

    private int bd;
}
