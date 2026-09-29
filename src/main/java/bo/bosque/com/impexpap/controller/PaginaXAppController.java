package bo.bosque.com.impexpap.controller;


import bo.bosque.com.impexpap.commons.CiudadesVentaHelper;
import bo.bosque.com.impexpap.dao.IArticuloPrecioDisponible;

import bo.bosque.com.impexpap.dao.IUsuarioCiudad;
import bo.bosque.com.impexpap.dto.CiudadesPermitidasDto;
import bo.bosque.com.impexpap.model.ArticuloPrecioDisponible;
import bo.bosque.com.impexpap.model.CiudadVenta;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/paginaXApp")
public class PaginaXAppController {

    private final IArticuloPrecioDisponible articuloPrecioDisponibleDao;
    private final IUsuarioCiudad usuarioCiudadDao;
    private final CiudadesVentaHelper ciudadesVenta;


    public PaginaXAppController( IArticuloPrecioDisponible articuloPrecioDisponibleDao,
                                 IUsuarioCiudad usuarioCiudadDao,
                                 CiudadesVentaHelper ciudadesVenta ) {

        this.articuloPrecioDisponibleDao = articuloPrecioDisponibleDao;
        this.usuarioCiudadDao = usuarioCiudadDao;
        this.ciudadesVenta = ciudadesVenta;
    }



    /**
     * Servicio para obtener los articulos de IPX y ESPP.
     *
     * <p>{@code codCiudad} del body solo elige entre las ciudades que el usuario
     * puede ver ({@link CiudadesVentaHelper}); 0 = "Todas". Otra ciudad es 403.
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })

    @PostMapping("/articulosX") //que un usuario admin o limitado si tiene acceso para consumir este recurso
    public List<ArticuloPrecioDisponible> listadoX(  @RequestBody ArticuloPrecioDisponible apd, Authentication auth  ) {

        return this.ciudadesVenta.catalogo( auth, apd.getCodCiudad(),
                this.articuloPrecioDisponibleDao::obtenerArticulosIPXyESPP );

    }



    /**
     * Servicio para obtener los articulos por item y disponibilidad en almacen.
     * Misma regla de ciudades que {@code /articulosX}.
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })

    @PostMapping("/articulosXAlmacen") //que un usuario admin o limitado si tiene acceso para consumir este recurso
    public List<ArticuloPrecioDisponible> listadoXAlmacen(  @RequestBody ArticuloPrecioDisponible apd, Authentication auth  ) {

        return this.ciudadesVenta.almacenes( auth, apd.getCodCiudad(),
                codCiudad -> this.articuloPrecioDisponibleDao.obtenerAlmacenXItem( apd.getCodArticulo(), codCiudad ) );

    }



    /**
     * Las ciudades que el que llama puede elegir en el catalogo, si tiene
     * "Todas" y con cual arranca.
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ciudadesPermitidas")
    public ResponseEntity<ApiResponse<CiudadesPermitidasDto>> ciudadesPermitidas( Authentication auth ) {

        CiudadesPermitidasDto dto = this.ciudadesVenta.permitidas( auth );
        return ResponseEntity.ok( new ApiResponse<>( "OK", dto, HttpStatus.OK.value() ) );
    }



    // ─── Gestion de ciudades por usuario ────────────────────────────────────
    // ROLE_ADM o el boton btnCiudadesUsuario (CiudadesVentaHelper.exigirGestion).

    /** Todas las ciudades de venta, para las casillas de la pantalla de gestion. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/usuarioCiudad/ciudadesVenta")
    public ResponseEntity<ApiResponse<List<CiudadVenta>>> ciudadesVenta( Authentication auth ) {

        this.ciudadesVenta.exigirGestion( auth );
        List<CiudadVenta> lista = this.usuarioCiudadDao.ciudadesVenta();
        return ResponseEntity.ok( new ApiResponse<>( "OK", lista, HttpStatus.OK.value() ) );
    }

    /** Todas las asignaciones de {@code tven_UsuarioCiudad}. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/usuarioCiudad/asignaciones")
    public ResponseEntity<ApiResponse<List<CiudadVenta>>> asignaciones( Authentication auth ) {

        this.ciudadesVenta.exigirGestion( auth );
        List<CiudadVenta> lista = this.usuarioCiudadDao.todasLasAsignaciones();
        return ResponseEntity.ok( new ApiResponse<>( "OK", lista, HttpStatus.OK.value() ) );
    }

    /** Asigna una ciudad a un usuario. {@code audUsuario} sale del token. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/usuarioCiudad/registrar")
    public ResponseEntity<ApiResponse<Long>> asignar( @RequestBody CiudadVenta cv, Authentication auth ) {

        this.ciudadesVenta.exigirGestion( auth );
        RespuestaSp r = this.usuarioCiudadDao.asignar( cv.getCodUsuario(), cv.getCodCiudad(),
                DatosToken.codUsuarioDe( auth ) );
        return new ResponseEntity<>( new ApiResponse<>( "Ciudad asignada", r.getIdGenerado(),
                HttpStatus.CREATED.value() ), HttpStatus.CREATED );
    }

    /** Quita una ciudad a un usuario. Si era la ultima, vuelve a ver su ciudad del login. */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/usuarioCiudad/eliminar")
    public ResponseEntity<ApiResponse<Long>> quitar( @RequestBody CiudadVenta cv, Authentication auth ) {

        this.ciudadesVenta.exigirGestion( auth );
        RespuestaSp r = this.usuarioCiudadDao.quitar( cv.getCodUsuario(), cv.getCodCiudad(),
                DatosToken.codUsuarioDe( auth ) );
        return ResponseEntity.ok( new ApiResponse<>( "Ciudad quitada", r.getIdGenerado(),
                HttpStatus.OK.value() ) );
    }

}
