package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Configuración del asiento de planilla en SAP de una empresa ({@code tsap_AsientoPlanilla}).
 *
 * <p>Con qué SAP se conecta (base, linked server y Service Layer) no está aquí: es la conexión
 * {@code idConexion} de {@code tsap_Conexion}, que comparten todos los módulos. De ella llegan, solo para
 * mostrar, el nombre, el ambiente, la base y si tiene la contraseña cargada; la contraseña nunca.
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

    /** La conexión a SAP a la que va el asiento; tiene que ser de la misma empresa. */
    private Integer idConexion;

    // De la conexión, solo para mostrar.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String conexion;
    /** {@code PRODUCCION} o {@code PRUEBA}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String ambiente;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String linkedServer;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String baseSap;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean conexionHabilitada;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean claveCargada;

    // El SP que arma el asiento, en la base de SAP de la conexión.
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

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;
}
