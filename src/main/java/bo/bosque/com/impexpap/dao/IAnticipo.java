package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.AnticipoPreview;
import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;

public interface IAnticipo {

    /**
     * procedimiento ABM para anticipos
     * @param an
     * @param acc
     * @return
     */
    RespuestaSp registrarAnticipo (Anticipo an,String acc);

    /**
     * obtendra los anticipos provenientes de SAP que ya fueron importados al BOSQUE
     * @param an
     * @return
     */
    List<Anticipo> obtenerAnticiposRegistrados(Anticipo an);
    /**
     * LISTA UNIFICADA DE ANTICIPOS SAP - BOSQUE
     * @param an
     * @return
     */
    List<Anticipo> anticiposUnificados(Anticipo an);

    /**
     * obtendra el listado de todos los anticipos que estan en SAP (AUN SIN REGISTRAR EN BOSQUE)
     * @param an
     * @return
     */
    List <Anticipo> obtenerAnticiposSAP(Anticipo an);

    /**
     * lista tipo asignacion anticipo
     * @return
     */
    List<Tipos>listTipoAsignacion();

    /**
     * ESTO ES UN ABM PERO DEVUELVE UN LISTADO (PREVIEW) NO HACE NINGUN INSERT, SOLO CALCULA MONTOS.
     * @param an
     * @return
     */
    List<AnticipoPreview> previsualizarAsignacion(Anticipo an);
}
