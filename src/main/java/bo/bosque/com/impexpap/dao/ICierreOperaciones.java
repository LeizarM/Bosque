// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\ICierreOperaciones.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.ChequeCierreDto;
import bo.bosque.com.impexpap.dto.ConfirmarCierreOperacionesRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

public interface ICierreOperaciones {
    RespuestaSp confirmarTraspasos(ConfirmarCierreOperacionesRequest req, long audUsuario);

    /**
     * Los cheques de un día cruzados contra SAP ({@code p_SAP_Rpt_ImpChequesPR
     * @ACCION='A'}). Consulta SAP en vivo por el servidor enlazado SRV_2022 y
     * falla si no contesta: el SP no lo tolera a propósito.
     */
    List<ChequeCierreDto> cheques(Date fecha);
}
