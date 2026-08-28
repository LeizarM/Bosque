package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Filtros de los reportes de talonarios.
 *
 * Es hermano de {@link TalonarioFiltroDto} pero no lo extiende: el listado y los
 * reportes filtran por cosas distintas —el listado nunca filtra por sucursal ni
 * por empleado, y los reportes nunca necesitan el interruptor de paginacion— y
 * atarlos obligaria a que cada campo nuevo de uno apareciera en el otro.
 *
 * Todo va en envoltorio (Long / Integer / Boolean) y no en primitivo: null
 * significa "sin filtro" y el stored procedure usa su DEFAULT NULL. Con un
 * primitivo, el 0 filtraria por id = 0 y el reporte saldria vacio sin decir
 * por que.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ReporteTalonarioDto implements Serializable {

    /** Obligatorio solo en la ficha; en el resto acota a un talonario. */
    private Long codTalonario;

    private Long codTipoRecibo;
    private Long codEmpresa;
    private Long codGrupo;

    /** 1 Adquirido, 2 Entregado, 3 Devuelto, 4 Cerrado. Solo inventario. */
    private Integer codEstadoActual;

    /** Solo trazabilidad: acota a quien recibio. Son excluyentes entre si. */
    private Long codSucursal;
    private Long codEmpleado;

    /**
     * En inventario filtra el alta del talonario; en trazabilidad, la fecha de
     * la entrega. Es el mismo par de parametros pero no la misma pregunta, y
     * por eso el subtitulo del PDF aclara sobre que fecha se aplico.
     */
    private Date fechaDesde;
    private Date fechaHasta;

    /** Solo inventario. Por defecto FALSE: los cerrados son estado terminal. */
    private Boolean incluirCerrados;

    // ---------- solo conciliacion con SAP ----------

    /**
     * "cobros" (p_SAP_Rpt_tmntoTalonario) o "salidas"
     * (p_SAP_Rpt_tmntoSalidTalonario). Son dos SPs distintos y por eso dos
     * .jrxml distintos: Jasper admite un unico queryString por reporte.
     */
    private String origen;

    /**
     * Accion del SP de SAP: 'A' conciliados con filtro, 'B' conciliados sin
     * filtro, 'C' conciliados renumerados, 'D' documentos sin talonario.
     * La pantalla expone A y D, que son las que responden preguntas distintas;
     * B y C quedan alcanzables para no perder nada del wizard viejo.
     */
    private String accionSap;

    /**
     * Lista de codTalonario separados por coma. Si viene con contenido, el SP
     * se corre en modo seleccion; si no, por grupo.
     */
    private String seleccion;

    // ---------- solo custodia ----------

    /** 'S' sucursales, 'E' personal, null ambos. */
    private String tipoDestinatario;

    /** Solo lo que lleva N o mas dias en poder de alguien. */
    private Integer diasMinimos;

    /**
     * Fecha a la que se calcula la custodia. Null = hoy.
     *
     * Va como parametro y no como GETDATE() suelto para que el mismo reporte
     * se pueda reproducir mas adelante: un PDF que dice "al dia de hoy" deja
     * de ser verificable en cuanto pasa el dia.
     */
    private Date fechaCorte;

}
