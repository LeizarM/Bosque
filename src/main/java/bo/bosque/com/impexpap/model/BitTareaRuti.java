// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\BitTareaRuti.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class BitTareaRuti implements Serializable {

    // ── campos de la tabla tac_bitTareaRuti ─────────────────────────────────
    // Solo idBitTarea (PK) y audUsuario son NOT NULL de verdad — el resto
    // son Long/Integer envoltorio: p_abm_tac_BitTareaRuti/
    // p_list_tac_BitTareaRuti los declaran todos NULL-ables (filtros
    // opcionales). Hallazgo de code-review, 2026-09-02.
    private long idBitTarea;
    private Date fechaActivo;
    private Date fechaPresentacion;
    private Long idTarRuti;
    private String nombreTareaRutinaria;
    private Long codEmpleado;
    private String descripCargo;
    private Date fechaCompletado;
    private Integer fueRealizado;
    private String obs;
    private Integer estado;
    private long audUsuario;
    private Date audFecha;

    // ── proyectadas desde tac_tareaRutinaria (solo lectura, LEFT JOIN en el
    // p_list; no existen como columna propia ni como parámetro del p_abm) ──
    private Integer idATR;
    private Long idFrec;

    // Plazo para responder (archivo SQL 65). diasPlazo es de la tarea padre;
    // fechaLimite no es columna de ninguna tabla: la calcula
    // fn_tac_fechaLimite(fechaPresentacion, diasPlazo) dentro del p_list.
    // Viaja ya resuelta para que el cliente no vuelva a deducir "vencida"
    // contra el reloj del dispositivo.
    private Integer diasPlazo;
    private Date fechaLimite;
}
