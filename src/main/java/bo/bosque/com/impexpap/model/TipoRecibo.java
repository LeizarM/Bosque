package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

import lombok.*;

/**
 * Catalogo de tipos de talonario. Tabla tmto_tipoRecibo.
 *
 * La sigla es la que arma el prefijo del nroTalonario y esta atada a la
 * empresa: IR1/IR3/NPI son de Impexpap, ER1/EC2 de Esppapel, PR2/R4 del resto.
 *
 * estado sale de v_tipos grupo 11: '0' Inactivo, '1' Activo.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TipoRecibo implements Serializable {

    private long codTipoRecibo;
    private String nombre;
    private String detalle;
    private String estado;
    private String sigla;
    private long audUsuario;
    private Date audFecha;

    // ---- solo lectura, los llena p_list_tmto_TipoRecibo ----
    private String datoEstado;
    private String datoTipo;
    private int cantTalonarios;
    private int cantGrupos;

    /**
     * Ultimo folio usado por este tipo. El alta masiva propone el bloque
     * siguiente con esto, para no duplicar numeros de recibo.
     *
     * Los folios no tienen constraint de unicidad, pero el negocio los mantiene
     * unicos y contiguos por tipo: 0 solapamientos en 1035 talonarios.
     */
    private int ultimoFolio;

}
