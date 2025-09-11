package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.dao.*;

import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/gasolinaMaquina")
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.GET})
public class ControlCombustibleMaquinaMontacargaController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";

    private final IControCombustibleMaquinaMontacarga combustibleMaquinaMontacargaDao;
    private final IMaquinaMontacarga maquinaMontacargaDao;
    private final ISucursal sucursalDao;

    private final IContenedor contenedorDao;
    private final IMovimiento movimientoDao;
    private final ICompraGarrafa compraGarrafaDao;
    private final ITipoContenedor tipoContenedorDao;



    public ControlCombustibleMaquinaMontacargaController(IControCombustibleMaquinaMontacarga combustibleMaquinaMontacargaDao, IMaquinaMontacarga maquinaMontacargaDao, ISucursal sucursalDao, IContenedor contenedorDao, IMovimiento movimientoDao, ICompraGarrafa compraGarrafaDao, ITipoContenedor tipoContenedorDao) {
        this.combustibleMaquinaMontacargaDao = combustibleMaquinaMontacargaDao;
        this.maquinaMontacargaDao = maquinaMontacargaDao;
        this.sucursalDao = sucursalDao;


        this.contenedorDao = contenedorDao;
        this.movimientoDao = movimientoDao;
        this.compraGarrafaDao = compraGarrafaDao;
        this.tipoContenedorDao = tipoContenedorDao;
    }


    /**
     * Deprecated: Este método es antiguo
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrarMaquina")
    public ResponseEntity<?> registrarGasolina(@RequestBody ControCombustibleMaquinaMontacarga mb) {
        try {

            // Manejar otros campos que podrían ser null
            if (mb.getFecha() != null) {
                mb.setFecha(new Utiles().fechaJ_a_Sql(mb.getFecha()));
            }

            String acc = mb.getIdCM() == 0 ? "I" : "U";

            boolean operationSuccess = this.combustibleMaquinaMontacargaDao.registrarControlCombustible(mb, acc);

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace(); // Esto ayudará a identificar el error exacto
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }



    /**
     * Deprecated: Obtener los almacenes registrados
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-almacenes")
    public ResponseEntity<?> obtenerAlmacenes() {
        try {
            List<ControCombustibleMaquinaMontacarga> ccmm = this.combustibleMaquinaMontacargaDao.lstAlmacenes();

            if (ccmm.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los almacenes encontrados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, ccmm, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }





    /**
     * Obtener los maquinas o montacargas registradas
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-maqmontacarga")
    public ResponseEntity<?> obtenerMaquinaMontacarga() {
        try {
            List<MaquinaMontacarga> mm = this.maquinaMontacargaDao.listMaquinaMontacargas(); //Listado de vehiculos, bidones, montacargas

            if (mm.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron las maquinas o montacargas encontrados.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, mm, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Deprecated: ****Obtener los registros de bidones por tipo de transacción
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstMovBidones")
    public ResponseEntity<?> listMovimientosBidones( @RequestBody ControCombustibleMaquinaMontacarga mb  ) {



        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.lstRptMovBidonesXTipoTransaccion( mb.getFechaInicio(), mb.getFechaFin(), mb.getCodSucursalMaqVehiDestino() ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el reporte de bidones entre fechas.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener los ultimos movimientos de bidones ( ingresos )
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstUltimoMovBidones")
    public ResponseEntity<?> listSaldosBidonesUltimosMov( ) {
        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.lstUltimosMovBidones( ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el el reporte de bidones los ultimos movimientos.");
            }

            return ResponseEntity.ok().body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener la lista de bidones
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstSaldosBidones")
    public ResponseEntity<?> listSaldosBidones( ) {
        try {
            List<ControCombustibleMaquinaMontacarga> temp = this.combustibleMaquinaMontacargaDao.saldoActualCombustinbleXSucursal( ); //Listado de vehiculos, bidones, montacargas

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron el reporte de saldo por litros por sucursales.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
     * = = = = = = = = NUEVOS METODOS PARA EL REGISTRO DE CONTROL DE BIDONES (GASOLINA, DIESEL, GARRAFAS ) = =
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
     * = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =
     */

    /**
     * Listara los contenedores para el registro de control de bidones (ya sea bidon o  garrafas)
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstContenedores")
    public ResponseEntity<?> listContenedores() {

        try {
            List<Contenedor> temp = this.contenedorDao.listContenedor( ); // por defecto la empresa 6 ( GENERAL )

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los contenedores.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }



    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrarMovimiento")
    public ResponseEntity<?> registrarMovimiento( @RequestBody Movimiento mb ) {
        try {

            // Manejar otros campos que podrían ser null
            if (mb.getFechaMovimiento() != null) {
                mb.setFechaMovimiento(new Utiles().fechaJ_a_Sql(mb.getFechaMovimiento()));
            }

            String acc = mb.getIdMovimiento() == 0 ? "I" : "U";

            boolean operationSuccess = this.movimientoDao.registrarMovimiento( mb, acc );

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace(); // Esto ayudará a identificar el error exacto
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener el detalle por bidon
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstSucursal")
    public ResponseEntity<?> listSucursal(  ) {
        try {
            List<Sucursal> temp = this.sucursalDao.obtenerSucursalesXEmpresa( 6 ); // por defecto la empresa 6 ( GENERAL )

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron las sucursales.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Para registrar la compra de garrafas por sucursales
     * @param mb
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/registrarGarrafa")
    public ResponseEntity<?> registrarGarrafa(@RequestBody CompraGarrafa mb) {
        try {



            String acc = mb.getIdCG() == 0 ? "I" : "U";

            boolean operationSuccess = this.compraGarrafaDao.registrarCompra(mb, acc);

            if (!operationSuccess) {
                return buildErrorResponse(HttpStatus.BAD_REQUEST, ERROR_MESSAGE);
            }

            HttpStatus status = HttpStatus.CREATED;
            return buildSuccessResponse(status, SUCCESS_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace(); // Esto ayudará a identificar el error exacto
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtiene los tipos de contenedores
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstTipoContenedor")
    public ResponseEntity<?> listTipoContenedor(  ) {
        try {
            List<TipoContenedor> temp = this.tipoContenedorDao.lstTipoContenedores();

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los tipos de contenedores.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtiene los movimientos por fecha y sucursal para un tipo de contenido en particular
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstMovimientos")
    public ResponseEntity<?> listMovimientos(  @RequestBody Movimiento mb ) {



        try {
            List<Movimiento> temp = this.movimientoDao.listarMovimientosXFechas(mb.getFechaInicio(), mb.getFechaFin(), mb.getCodSucursal(), mb.getIdTipo());

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los tipos de contenedores.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Para listar el saldo actual de combustible por sucursal y por tipo de contenedores.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstSaldoActuales")
    public ResponseEntity<?> lstSaldoActuales( ) {

        try {
            List<Movimiento> temp = this.movimientoDao.lstSaldosActuales();

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los saldos Actuales por Sucursales");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Obtener los bidones pendientes por sucursal
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstBidonesPendientes")
    public ResponseEntity<?> listBidonesPendientes( @RequestBody Movimiento mb ) {
        try {
            List<Movimiento> temp = this.movimientoDao.obtenerBidonesXSucursal( mb.getSucursalDestino() );

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron bidones pendientes con la sucursal seleccionada.");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtener el detalle por bidon
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstDetalleBidon")
    public ResponseEntity<?> listBidonDetalle( @RequestBody Movimiento mb ) {
        try {
            List<Movimiento> temp = this.movimientoDao.obtenerDetalleBidon( mb.getIdMovimiento() );

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraro informacion del detalle bidon");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }




    private ResponseEntity<ApiResponse<?>> buildErrorResponse(BindingResult result) {
        String errorMsg = Objects.requireNonNull(result.getFieldError()).getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ERROR_MESSAGE, errorMsg, HttpStatus.BAD_REQUEST.value()));
    }

    private ResponseEntity<ApiResponse<?>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null, status.value()));
    }

    private ResponseEntity<ApiResponse<?>> buildSuccessResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiResponse<>(message, null, status.value()));
    }
}
