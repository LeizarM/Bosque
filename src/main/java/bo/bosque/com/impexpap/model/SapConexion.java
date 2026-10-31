package bo.bosque.com.impexpap.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * Una conexión a SAP ({@code tsap_Conexion}): de qué empresa es, si apunta a la base real o a la de
 * prueba, dónde corren sus SP (linked server y base) y con qué usuario se entra al Service Layer.
 *
 * <p>La comparten todos los módulos que hablan con SAP: cada uno guarda solo su {@code idConexion}
 * (ver {@code commons/SapConexiones}). {@code slClave} viaja cifrada entre la base y el backend, y nunca
 * hacia la app: por eso {@code @JsonIgnore}. La app solo ve {@code claveCargada}. Sin {@code @Data}: su
 * {@code toString()} imprimiría la clave cifrada en cualquier log.
 */
@Getter
@Setter
public class SapConexion implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PRODUCCION = "PRODUCCION";
    public static final String PRUEBA = "PRUEBA";

    private Integer idConexion;
    /** Único: es lo que eligen los módulos en su desplegable. */
    private String nombre;
    private Integer codEmpresa;
    /** Nombre de la empresa en {@code tb_empresa}. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String empresa;
    /** {@link #PRODUCCION} o {@link #PRUEBA}. */
    private String ambiente;

    // Dónde corren los SP de los módulos.
    /** Vacío = la misma instancia que BOSQUE. Necesita RPC Out. */
    private String linkedServer;
    /** {@code CompanyDB} del Service Layer y base donde corren los SP. */
    private String baseSap;

    // El Service Layer.
    private String slUrl;
    private String slUsuario;
    @JsonIgnore
    private String slClave;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Boolean claveCargada;

    private Boolean habilitada;
    /** Cuántas configuraciones de módulos la usan: con alguna, no se puede borrar. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer enUso;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long audUsuario;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date audFecha;
}
