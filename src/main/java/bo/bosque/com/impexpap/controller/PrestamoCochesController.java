package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin("*")
@RequestMapping("/prestamo-coches")
public class PrestamoCochesController {

    private final ISolicitudChofer solicitudChoferDao;
    private final IEstadoChofer estadoChoferDao;
    private final IPrestamoChofer prestamoChoferDao;
    private final IPrestamoEstado prestamoEstadoDao;
    private final ITipoSolicitud tipoSolicitudDao;


    public PrestamoCochesController(ISolicitudChofer solicitudChoferDao, IEstadoChofer estadoChoferDao, IPrestamoChofer prestamoChoferDao, IPrestamoEstado prestamoEstadoDao, ITipoSolicitud tipoSolicitudDao) {

        this.solicitudChoferDao = solicitudChoferDao;
        this.estadoChoferDao = estadoChoferDao;
        this.prestamoChoferDao = prestamoChoferDao;
        this.prestamoEstadoDao = prestamoEstadoDao;
        this.tipoSolicitudDao = tipoSolicitudDao;
    }

    /**
     * Registrar las facturas
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroSolicitud")
    public ResponseEntity<?> registrarSolicitud( @RequestBody SolicitudChofer mb ) {

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( mb.getIdSolicitud() == 0){
            acc = "I";
        }

        if( !this.solicitudChoferDao.registrarSolicitudChofer( mb, acc ) ){
            response.put("msg", "Error al Registrar El Registro de Solicitud de Chofer");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Registro de Solicitud de Choferes Registrados Correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    /**
     * Obtiene las las solicitudes por empleado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/solicitudes")
    public List<SolicitudChofer> obtenerSolicitudesPorEmpleado(@RequestBody SolicitudChofer mb ){


        List<SolicitudChofer> lstTemp = this.solicitudChoferDao.lstSolicitudesXEmpleado(  mb.getCodEmpSoli() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }

    /**
     * Obtiene la lista de coches para el prestamo de los coches
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/coches")
    public List<SolicitudChofer> obtenerCoche(){

        List<SolicitudChofer> lstTemp = this.solicitudChoferDao.lstCoches();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }

    /**
     * Obtiene los estados de los coches
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/estados")
    public List<EstadoChofer> obtenerEstados(){

        List<EstadoChofer> lstTemp = this.estadoChoferDao.listarEstadoChofer();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }


    /**
     * Para registar los prestamos de los coches
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroPrestamo")
    public ResponseEntity<?> registrarPrestamo( @RequestBody PrestamoChofer mb ) {

        PrestamoEstado prestamoEstado = new PrestamoEstado();

        Map<String, Object> response = new HashMap<>();

        String acc = "A";
        if( mb.getIdPrestamo() == 0 ){
            acc = "I";

        }

        if( mb.getIdPrestamo() == 0 ){
            mb.setEstadoLateralesEntrega(1); //Key 1
            mb.setEstadoInteriorEntrega(2);
            mb.setEstadoDelanteraEntrega(3);
            mb.setEstadoTraseraEntrega(4);
            mb.setEstadoCapoteEntrega(5);

        }else{
            mb.setEstadoLateralRecepcion(1); //Key 1
            mb.setEstadoInteriorRecepcion(2);
            mb.setEstadoDelanteraRecepcion(3);
            mb.setEstadoTraseraRecepcion(4);
            mb.setEstadoCapoteRecepcion(5);
        }


        if( !this.prestamoChoferDao.registrarPrestamoChofer( mb, acc ) ){
            response.put("msg", "Error al Registrar El Registro de Prestamo de Chofer");
            response.put("ok", "error");

            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }



        if( mb.getIdPrestamo() == 0 ){


            List<Integer> numeros = Utiles.extraerNumeros( mb.getEstadoLateralesEntregaAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoLateralesEntrega());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "A" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoInteriorEntregaAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoInteriorEntrega());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "A" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoDelanteraEntregaAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoDelanteraEntrega());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "A" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoTraseraEntregaAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoTraseraEntrega());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "A" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoCapoteEntregaAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoCapoteEntrega());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "A" );
            }


        }else{


            List<Integer> numeros = Utiles.extraerNumeros( mb.getEstadoLateralRecepcionAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoLateralRecepcion());
                prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "B" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoInteriorRecepcionAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoInteriorRecepcion());
                prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado( prestamoEstado, "B" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoDelanteraRecepcionAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoDelanteraRecepcion());
                prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B" );
            }

            numeros = Utiles.extraerNumeros( mb.getEstadoTraseraRecepcionAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoTraseraRecepcion());
                prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B" );

            }

            numeros = Utiles.extraerNumeros( mb.getEstadoCapoteRecepcionAux() );

            for (int numero : numeros) {
                prestamoEstado.setIdPE(mb.getEstadoCapoteRecepcion());
                prestamoEstado.setIdPrestamo(mb.getIdPrestamo());
                prestamoEstado.setIdEst(numero);
                prestamoEstado.setAudUsuario( mb.getAudUsuario() );

                this.prestamoEstadoDao.registrarPrestamoEstado(prestamoEstado, "B" );
            }


        }



        response.put("msg", "Datos de Registro de Solicitud de Choferes Registrados Correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Obtiene las solicitudes por sucursal para el prestamo de los coches
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/solicitudesPrestamo")
    public List<PrestamoChofer> obtenerSolicitudesPrestamo( @RequestBody PrestamoChofer mb  ){

        List<PrestamoChofer> lstTemp = this.prestamoChoferDao.lstSolicitudes( mb.getCodSucursal(), mb.getCodEmpEntregadoPor() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }

    /**
     * Para actualizar el estado del de la solicitud de prestamo de los coches
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/actualizarSolicitud")
    public ResponseEntity<?> actualizarSolicitud( @RequestBody SolicitudChofer mb ) {

        Map<String, Object> response = new HashMap<>();



        if( !this.solicitudChoferDao.registrarSolicitudChofer( mb, "A" ) ){
            response.put("msg", "Error al Actualizar el estado del la solicitud");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Actualizados de la Solicitud Correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Para obtener el detalle de la solicitud de prestamo de los coches
     * @return
     */

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoSolicitudes")
    public List<TipoSolicitud> obtenerTipoSolicitud(){


        List<TipoSolicitud> lstTemp = this.tipoSolicitudDao.obtenerTipoSolicitudes();

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


}
