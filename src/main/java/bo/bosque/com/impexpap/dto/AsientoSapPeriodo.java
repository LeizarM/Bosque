package bo.bosque.com.impexpap.dto;

import lombok.Getter;
import lombok.Setter;

/** El periodo y la empresa de un asiento de planilla para SAP. */
@Getter
@Setter
public class AsientoSapPeriodo {

    private Integer anio;
    private Integer mes;
    /** {@code tb_empresa.codEmpresa}; la base de SAP sale de {@code sap.asiento-planilla.empresas}. */
    private Integer codEmpresa;
}
