package bo.bosque.com.impexpap.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** El asiento de planilla ya armado: lo que se muestra en la vista previa y lo que se manda a SAP. */
@Getter
@Builder
public class AsientoSapResumen {

    private final int anio;
    private final int mes;
    private final int codEmpresa;
    private final String empresa;
    /** {@code CompanyDB} del Service Layer y base donde corre el SP. */
    private final String baseSap;
    /** El nombre de la conexión a SAP a la que va. */
    private final String conexion;
    /** {@code PRODUCCION} o {@code PRUEBA}: la app lo destaca para que nadie envíe a la base equivocada. */
    private final String ambiente;
    /** {@code yyyy-MM-dd}: {@code ReferenceDate}, {@code TaxDate} y {@code DueDate}. */
    private final String fecha;
    private final String memo;
    /** Marca del periodo en SAP; con ella se detecta un asiento ya enviado. */
    private final String referencia;
    private final List<AsientoSapLinea> lineas;
    private final BigDecimal totalDebito;
    private final BigDecimal totalCredito;
    /** {@code totalDebito - totalCredito}. */
    private final BigDecimal diferencia;
    private final boolean cuadra;
}
