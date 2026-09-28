// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dto\RegistrarArqueoRequest.java
package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.util.List;

/** Cuerpo de POST /tareas-rutinarias/arqueo-caja/registrar. */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarArqueoRequest {
    private long idTarRuti;
    private long idBitTarea;
    private Double saldoMovSap;
    private Double tc;
    private String obs;
    private List<CorteArqueoDto> cortes;
    private List<DocumentacionArqueoDto> documentacion;
    private List<ValeArqueoDto> vales;
}
