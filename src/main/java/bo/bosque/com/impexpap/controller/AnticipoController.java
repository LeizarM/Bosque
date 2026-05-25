package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IAnticipo;
import bo.bosque.com.impexpap.dao.IAnticipoDetalle;
import bo.bosque.com.impexpap.dto.AnticipoPreview;
import bo.bosque.com.impexpap.model.Anticipo;
import bo.bosque.com.impexpap.model.AnticipoDetalle;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.xml.ws.Response;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping ("/anticipo")
@CrossOrigin (origins = "*",methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM')")
public class AnticipoController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IAnticipo anticipoDao;
    private final IAnticipoDetalle anticipoDetalleDao;

    //**************ENDPOINTS PARA TPL_ANTICIPO*****************
    public AnticipoController(IAnticipo anticipoDao, IAnticipoDetalle anticipoDetalleDao){
        this.anticipoDao =  anticipoDao;
        this.anticipoDetalleDao = anticipoDetalleDao;
    }

    /**
     * registrar o actualizar un anticipo
     * @param an
     * @return
     */
    @PostMapping("/registrarAnticipo")
    public ResponseEntity<ApiResponse<?>> registrarAnticipo(@RequestBody Anticipo an) {
        // ✅ Evitar NPE al comparar Long nullable con primitivo 0
        String accion = (an.getCodAnticipo() == null || an.getCodAnticipo() == 0L) ? "I" : "U";
        RespuestaSp res = anticipoDao.registrarAnticipo(an, accion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(res.getErrormsg(), Collections.emptyList(), HttpStatus.CREATED.value()));
    }

    /**
     * listado de anticipos provenienetes del SAP que YA FUERON IMPORTADOS AL BOSQUE
     * @param an
     * @return
     */
    @PostMapping("/obtenerAnticipos")
    public  ResponseEntity<ApiResponse<?>>obtenerAnticiposRegistrados(@RequestBody Anticipo an){
        List<Anticipo> lista =  anticipoDao.obtenerAnticiposRegistrados(an);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE,lista,HttpStatus.OK.value()));
    }
    /**
     * LISTA UNIFICADA ANTICIPOS SAP - BOSQUE
     * @param an
     * @return
     */
    @PostMapping("/listAnticipos")
    public  ResponseEntity<ApiResponse<?>>anticiposUnificados(@RequestBody Anticipo an){
        List<Anticipo> lista =  anticipoDao.anticiposUnificados(an);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE,lista,HttpStatus.OK.value()));
    }
    /**
     * Devolera una lista de los tipos del motivo de renovacion chip tigo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoAsigAnticipo")
    public List<Tipos> listTipoAsignacion(){
        return this.anticipoDao.listTipoAsignacion();
    }

    /**
     * listado de anticipos QUE ESTAN EN SAP (SIN IMPORTAR A BOSQUE)
     * @param an
     * @return
     */
    @PostMapping("/listarAnticiposSAP")
    public ResponseEntity<ApiResponse<?>> listarAnticiposSAP(@RequestBody Anticipo an) {
        List<Anticipo> lista = anticipoDao.obtenerAnticiposSAP(an);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
    // En AnticipoController.java
    @PostMapping("/previsualizarAsignacion")
    public ResponseEntity<ApiResponse<?>> previsualizarAsignacion(@RequestBody Anticipo an) {
        List<AnticipoPreview> lista = anticipoDao.previsualizarAsignacion(an);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>("Previsualización generada exitosamente", lista, HttpStatus.OK.value()));
    }
    /**
     * Anular un anticipo de forma lógica (Desvincula o Anula según el moduloOrigen)
     * @param an Requiere el codAnticipo y audUsuarioI
     * @return
     */
    @PostMapping("/anularAnticipo")
    public ResponseEntity<ApiResponse<?>> anularAnticipo(@RequestBody Anticipo an) {
        // ✅ Endpoint limpio y directo. Mandamos 'R' al DAO existente.
        RespuestaSp res = anticipoDao.registrarAnticipo(an, "R");

        if (res.getError() != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(res.getErrormsg(), Collections.emptyList(), HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }
    /**
     * Reemplazo atómico de la distribución manual de un anticipo (ACCION = 'M')
     * @param an Requiere codAnticipo, codEmpresa, audUsuarioI y xmlEmpleados
     * @return
     */
    @PostMapping("/editarAsignacion")
    public ResponseEntity<ApiResponse<?>> editarAsignacion(@RequestBody Anticipo an) {
        // Enviamos 'M' explícitamente para invocar el bloque de edición con XML en el SP
        RespuestaSp res = anticipoDao.registrarAnticipo(an, "M");

        if (res.getError() != 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(res.getErrormsg(), Collections.emptyList(), HttpStatus.BAD_REQUEST.value()));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(res.getErrormsg(), Collections.emptyList(), HttpStatus.OK.value()));
    }

    //*******************FIN ENDPOINTS TPL_ANTICIPO********************************
    //*******************ENDPOINTS TPL_ANTICIPODETALLE***************************
    /**
     * registrar o actualizar un anticipodetallado x empleado
     * @param ad
     * @return
     */
    @PostMapping("/registrarAnticipoDetalle")
    public ResponseEntity<ApiResponse<?>>registrarAnticipoDetalle(@RequestBody AnticipoDetalle ad){
        RespuestaSp res = anticipoDetalleDao.registrarAnticipoDetalle(ad,ad.getCodAntDetalle()==0?"I":"U");
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(res.getErrormsg(),res.getIdGenerado(),HttpStatus.CREATED.value()));
    }
    /**
     * LISTADO ANTICIPOS DETALLADOS POR EMPLEADO
     * @param ad
     * @return
     */
    @PostMapping("/obtenerAnticipoDetalle")
    public  ResponseEntity<ApiResponse<?>>obtenerAnticipoDetalle(@RequestBody AnticipoDetalle ad){
        List<AnticipoDetalle> lista =  anticipoDetalleDao.obtenerAnticipoDetalle(ad);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE,lista,HttpStatus.OK.value()));
    }

    /**
     *
     * @param ad
     * @return
     */
    @PostMapping("/anticipoNoAsignado")
    public  ResponseEntity<ApiResponse<?>>anticipoNoAsignado(@RequestBody AnticipoDetalle ad){
        List<AnticipoDetalle> lista =  anticipoDetalleDao.anticipoNoAsignado(ad);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE,lista,HttpStatus.OK.value()));
    }

    /**
     * OBTENDRA EL ESTADO AGRUPADO DE UN ANTICIPO EN ESPECIFICO
     * @param ad
     * @return
     */
    @PostMapping("/estadoAnticipo")
    public ResponseEntity<ApiResponse<?>> estadoAnticipo(@RequestBody AnticipoDetalle ad) {
        List<AnticipoDetalle> lista = anticipoDetalleDao.estadoAnticipo(ad);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }
    //******************FIN ENDPOINTS TPL_ANTICIPODETALLE


}
