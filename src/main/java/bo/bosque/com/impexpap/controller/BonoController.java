package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IBono;
import bo.bosque.com.impexpap.model.Bono;
import bo.bosque.com.impexpap.model.BonoEmpleado;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bono")
@CrossOrigin (origins = "*",methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM')")
public class BonoController {

    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IBono bonoDao;

    public BonoController(IBono bonoDao) {
        this.bonoDao = bonoDao;
    }

    @PostMapping("/listarBono")
    public ResponseEntity<ApiResponse<?>> listarBono(@RequestBody Bono b) {
        List<Bono> lista = bonoDao.listarBono(b);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/listarBonoEmpleado")
    public ResponseEntity<ApiResponse<?>> listarBonoEmpleado(@RequestBody BonoEmpleado be) {
        List<BonoEmpleado> lista = bonoDao.listarBonoEmpleado(be);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE, lista, HttpStatus.OK.value()));
    }

    @PostMapping("/abmBono")
    public ResponseEntity<ApiResponse<?>> abmBono(@RequestBody Bono b) {
        // En base al modelo, definimos la acción B (Generar) o U (Actualizar) si tuviera ID.
        String accion = b.getCodBono() == null || b.getCodBono() == 0 ? "B" : "U";
        RespuestaSp res = bonoDao.abmBono(b, accion);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value())
        );
    }
}
