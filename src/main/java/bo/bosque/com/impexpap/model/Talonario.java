package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Un talonario fisico. Tabla tmto_talonario.
 *
 * numeracionInicial..numeracionFinal es el rango de folios (los lotes del
 * legacy generan bloques de 50). nroTalonario es UNICO A NIVEL GLOBAL
 * (constraint tmto_talonario_uq), no por tipo ni por empresa.
 *
 * OJO con {@link #estado}: esta MUERTO. Los 1035 registros de produccion
 * tienen '1'. El campo util es {@link #codEstadoActual} / {@link #estadoActual},
 * que NO estan guardados en ninguna columna: p_list_tmto_Talonario los deriva
 * contando filas del log tmto_talonarioDetalle.
 *
 *   cierres  >= 1              -> Cerrado (terminal)
 *   entregas =  devoluciones   -> disponible, se puede entregar
 *   entregas >  devoluciones   -> en poder de alguien, se puede devolver o cerrar
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Talonario implements Serializable {

    private long codTalonario;
    private long codTipoRecibo;
    private String nroTalonario;
    private double costoBs;
    private int numeracionInicial;
    private int numeracionFinal;
    /** Vestigial: siempre '1'. Usar estadoActual. */
    private String estado;
    private long codEmpresa;
    private String observacion;
    private long audUsuario;
    private Date audFecha;

    // ---- solo lectura, los llena p_list_tmto_Talonario ----
    private String sigla;
    private String datoTipoNombre;
    private String datoTipo;
    private String datoEmpresa;
    private String datoTalonario;

    /** Conteos del log de eventos, de los que sale el estado. */
    private int entregas;
    private int devoluciones;
    private int cierres;

    /** 1 Adquirido, 2 Entregado, 3 Devuelto, 4 Cerrado (v_tipos grupo 45). */
    private int codEstadoActual;
    private String estadoActual;

    /** Reemplazan la mascara de 4 caracteres del JSF ('0100', '0011', '0000'). */
    private boolean puedeEntregar;
    private boolean puedeDevolver;
    private boolean puedeCerrar;

    /** Quien lo tiene AHORA. Vacio si esta disponible o cerrado. */
    private String datoDestinatario;

}
