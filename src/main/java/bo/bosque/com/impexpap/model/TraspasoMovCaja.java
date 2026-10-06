// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\TraspasoMovCaja.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TraspasoMovCaja implements Serializable {

    // ── campos de la tabla tac_traspasoMovCaja ──────────────────────────────
    // Solo idTrasp (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer/Double envoltorio: p_abm_tac_TraspasoMovCaja/
    // p_list_tac_TraspasoMovCaja los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idTrasp;
    private String bd;
    private Date fecha;
    private String account;
    private String contraAct;
    private String acctName;
    private String tipoTransaccion;
    private Double dolares;
    private Double bs;
    private Integer fueVerificado;
    /**
     * Por que NO cuadro un traspaso. Obligatoria cuando
     * {@code fueVerificado} viene en 0: el SP la exige (error 23).
     *
     * Tiene que estar en el modelo aunque nadie la lea desde aqui.
     * {@code registrar()} pasa por {@code SpHelper.ejecutarAbm}, que
     * construye la llamada desde la metadata del procedimiento y pide un
     * valor para cada parametro declarado; el archivo SQL 49 agrego
     * {@code @obs} a p_abm_tac_TraspasoMovCaja.
     */
    private String obs;
    private Long idBitTarRuti;
    private long audUsuario;
    private Date audFecha;
}
