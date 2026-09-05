// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\model\TareaRutinaria.java
package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TareaRutinaria implements Serializable {

    // ── campos de la tabla tac_tareaRutinaria ──────────────────────────────
    // Solo idTarRuti (PK) y audUsuario son NOT NULL de verdad — el resto son
    // Long/Integer envoltorio, no long/int primitivos: p_abm_tac_TareaRutinaria/
    // p_list_tac_TareaRutinaria los declaran todos NULL-ables, y el flujo real
    // de alta (registrar-tarea-rutinaria-con-cargos) nunca completa idArea/
    // idATR. Un primitivo ahí hace que BeanPropertyRowMapper reviente con
    // NullPointerException/SQLException al mapear cualquier fila con esas
    // columnas en NULL (encontrado en code-review, 2026-09-02).
    private long idTarRuti;
    private Long idFrec;
    private Long idArea;
    private Date fechaPartida;
    // "iniFin", no "IniFin": con el campo capitalizado, Lombok genera
    // getIniFin()/setIniFin() y Jackson deriva la propiedad JSON como
    // "iniFin" de todos modos (decapitaliza solo la primera letra) — el
    // modelo Dart mandaba literalmente "IniFin" y el bind fallaba en
    // silencio, persistiendo siempre 0. Ver hallazgo de code-review.
    private Integer iniFin;
    private Integer idATR;
    private String descripcion;
    private long audUsuario;
    private Date audFecha;
}
