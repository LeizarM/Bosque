package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Configuración del asiento de planilla en SAP de una empresa ({@code tsap_AsientoPlanilla}).
 *
 * <p>{@code slClave} viaja cifrada entre la base y el backend, y nunca hacia la app: por eso
 * {@code @JsonIgnore}. La app solo ve {@code claveCargada}. Sin {@code @Data}: su {@code toString()}
 * imprimiría la clave cifrada en cualquier log.
 */
@Getter
@Setter
public class AsientoSapConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer codEmpresa;
    /** Nombre de la empresa en {@code tb_empresa}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String empresa;
    private Boolean habilitado;

    // El SP que arma el asiento, en la base de SAP de la empresa.
    private String baseSap;
    private String linkedServer;
    private String esquema;
    private String sp;
    private String paramAnio;
    private String paramMes;

    // Las columnas que devuelve el SP.
    private String colCodigo;
    private String colNombre;
    private String colCuenta;
    private String colDebito;
    private String colCredito;

    // El asiento.
    /** Separados por coma: SU,CO,AFT. */
    private String prefijosSocio;
    private Integer diaDelMes;
    private String memo;
    private String referencia;
    private String campoReferencia;
    private Boolean evitarDuplicados;
    private BigDecimal tolerancia;

    // El Service Layer.
    private String slUrl;
    private String slUsuario;
    @JsonIgnore
    private String slClave;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean claveCargada;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;
}
