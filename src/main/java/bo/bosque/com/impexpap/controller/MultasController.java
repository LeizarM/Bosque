package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IMultas;
import bo.bosque.com.impexpap.model.Multas;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/multas")
@CrossOrigin (origins = "*",methods = {RequestMethod.POST})
@PreAuthorize("hasAnyRole('ROLE_ADM','ROLE_LIM')")
public class MultasController {
    private static final String SUCCESS_MESSAGE = "OPERACION REALIZADA EXITOSAMENTE";

    private final IMultas multasDao;

    public MultasController (IMultas multasDao){this.multasDao = multasDao;}

    @PostMapping("/listarMultas")
    public ResponseEntity<ApiResponse<?>> listarMultas(@RequestBody Multas m){
        List<Multas> lista =  multasDao.listarMultas(m);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(SUCCESS_MESSAGE,lista,HttpStatus.OK.value()));
    }
    @PostMapping("/generarMultas")
    public ResponseEntity<ApiResponse<?>> generarMultas(@RequestBody Multas m) {
        // Si codMulta es 0, asumimos que es Generación Masiva (C)
        // Caso contrario, es un Update de una multa específica (U)
        //String accion = m.getCodMulta() == 0 ? "C" : "U2";
        String accion = (m.getXmlMultas() != null && !m.getXmlMultas().trim().isEmpty()) ? "U2" : "C";

        RespuestaSp res = multasDao.generarMultas(m, accion);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(res.getErrormsg(), res.getIdGenerado(), HttpStatus.CREATED.value())
        );
    }

}
