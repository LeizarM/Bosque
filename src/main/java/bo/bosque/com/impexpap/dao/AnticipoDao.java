package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.AnticipoPreview;
import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AnticipoDao implements IAnticipo {
    private final SpHelper spHelper;

    public AnticipoDao (SpHelper spHelper){this.spHelper = spHelper;}

    /**
     * procedimiento para registrar o actualizar un anticipo
     * @param an
     * @param acc
     * @return
     */
    @Override
    public RespuestaSp registrarAnticipo(Anticipo an,String acc){
        return this.spHelper.ejecutarAbm("p_abm_Anticipo",an,acc);
    }
    @Override
    public List<Anticipo> obtenerAnticiposRegistrados(Anticipo an){
        return this.spHelper.ejecutarListado("p_list_Anticipo",an,"B",Anticipo.class);
    }

    /**
     * LISTA ANTICIPOS UNIFICADOS SAP - BOSQUE
     * @param an
     * @return
     */
    @Override
    public List<Anticipo> anticiposUnificados(Anticipo an){
        return this.spHelper.ejecutarListado("p_list_Anticipo",an,"C",Anticipo.class);
    }

    /**
     * LISTADO DE LOS ANTICIPOS REGISTRADOS EN SAP LISTOS PARA IMPORTAR AL BOSQUE.
     * @param an
     * @return
     */
    @Override
    public List<Anticipo> obtenerAnticiposSAP(Anticipo an){
        return this.spHelper.ejecutarListado("p_list_Anticipo",an,"A",Anticipo.class);
    }
    /**
     * Obtendra una lista de tipo de renovacion chip tigo
     * @return
     */
    public List<Tipos> listTipoAsignacion() {
        return new Tipos().listTipoAsignacion();
    }

    /**
     * CALCULO DE MONTOS (PREVISUALIZACION)
     * @param an
     * @return
     */
    @Override
    public List<AnticipoPreview> previsualizarAsignacion(Anticipo an) {
        // ✅ Usar Map explícito con SOLO los parámetros necesarios
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("codEmpresa",   an.getCodEmpresa());
        params.put("debe",         an.getDebe());
        params.put("xmlEmpleados", an.getXmlEmpleados());
        // Opcionales para que el SP no falle en validación:
        if (an.getNumAsiento() != null) params.put("numAsiento", an.getNumAsiento());
        if (an.getFechaAsiento() != null) params.put("fechaAsiento", an.getFechaAsiento());
        //if (an.getAudUsuarioI()) params.put("audUsuarioI", an.getAudUsuarioI());

        return this.spHelper.ejecutarListado(
                "p_abm_Anticipo", params, "P", AnticipoPreview.class
        );
    }


}
