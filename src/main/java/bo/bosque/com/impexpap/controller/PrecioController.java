package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import bo.bosque.com.impexpap.utils.Tipos;
import lombok.extern.slf4j.Slf4j;


@RestController
@CrossOrigin("*")
@RequestMapping("/price")
@Slf4j
public class PrecioController {

    private IAutorizacion autDao;
    private ICostoIncre costoIncreDao;
    private IProducto productoDao;
    private IArticuloPropuesto articuloPropuestoDao;
    private IPrecio precioDao;
    private IPropuesta propuestaDao;
    private IPrecioPropuesta precioPropuestaDao;
    private ICostoSug costoSugDao;


    PrecioController(IAutorizacion autDao, ICostoIncre costoIncreDao, IProducto productoDao, IArticuloPropuesto articuloPropuestoDao, IPrecio precioDao, IPropuesta propuestaDao, IPrecioPropuesta  precioPropuestaDao, ICostoSug costoSugDao  ){
        this.autDao = autDao;
        this.costoIncreDao = costoIncreDao;
        this.productoDao = productoDao;
        this.articuloPropuestoDao = articuloPropuestoDao;
        this.precioDao = precioDao;
        this.propuestaDao = propuestaDao;
        this.precioPropuestaDao = precioPropuestaDao;
        this.costoSugDao =  costoSugDao;

    }

    /**
     * Procedimiento para obtener la lista de propuesta para que sean autorizadas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/autorizacion")
    public List<Autorizacion> obtenerListaPropuesta(){
        List <Autorizacion> lstAut = this.autDao.listAutorizacion();

        if( lstAut.size() == 0 ) return new ArrayList<>();
        
        return lstAut;
    }

    /**
     * Devovlera una lista del estado de las propuestas
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/estadoPropuesta")
    public List<Tipos> listPropuestas(){
        return this.autDao.lstEstadoPropuestas();
    }

    /**
     * Devolvera la lista de costo de flete de transporte
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/costoFlete")
    public List<CostoIncre> listTransporte(){
        List<CostoIncre> lstTemp = this.costoIncreDao.costoTransporteCiudad();

        if( lstTemp.size() == 0 ) return new ArrayList<CostoIncre>();

        return lstTemp;
    }

    /**
     * Devolvera una lista de los proveedores
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstProveedor")
    public List<Producto> listProveedor(){

        List<Producto> lstTemp = this.productoDao.listadoProveedor();
        if( lstTemp.size() == 0 ) return new ArrayList<Producto>();
        return lstTemp;

    }

    /**
     * Devolvera una lista de familia
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listFamilia")
    public List<Producto> listFamilia(@RequestBody Producto producto){

        List<Producto> lstTemp = this.productoDao.listadoFamilia(producto.getCodigoFamilia());

        if( lstTemp.size() == 0 ) return new ArrayList<Producto>();
        return lstTemp;
    }

    /**
     * Devolvera la lista de familias por grupo de familia
     * @param producto
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listFamiliaXGrupo")
    public List<Producto> lstFamiliaXGrupo( @RequestBody Producto producto ) {

        List <Producto> lstTemp = this.productoDao.listadoFamiliaXGrupo( producto.getIdGrpFamiliaSap() );

        if( lstTemp.size() == 0 ) return new ArrayList<Producto>();

        return lstTemp;

    }

    /**
     * Devolvera una lista de articulos por familia o familias seleccionados
     * @param artProp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listFamiliaXArticulo")
    public List<ArticuloPropuesto> lstArticulosXFamilia( @RequestBody  ArticuloPropuesto artProp ){

        List<ArticuloPropuesto> lstTemp = this.articuloPropuestoDao.listarArticulosXFamilia( artProp.getCodCad() );

        if( lstTemp.size() == 0 ) return new ArrayList<ArticuloPropuesto>();

        return lstTemp;

    }

    /**
     * Regresara un objeto que contiene los datos de unan familia
     * @param producto
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/cargarProducto")
    public Producto obtenerDatoFamilia( @RequestBody Producto producto  ){
        Producto temp = new Producto();
        temp  = this.productoDao.cargarDatoFamilia( producto.getCodigoFamilia() );
        if( temp == null ) return new Producto();
        return temp;
    }


    /**
     * Devovlera una lista de los precios en toneladas actuales por codigo de familia
     * @param precio
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstPrecioTonXFamilia")
    public List<Precio> obtenerPreciosActualesXFamilia( @RequestBody Precio precio ){

        System.out.println("precio.toString() = " + precio.toString());
        
        List<Precio> temp = new ArrayList<>();
        temp = this.precioDao.listPrecioToneladasActuales( precio.getCodigoFamilia() );

        if( temp.size() == 0 ) return new ArrayList<>();

        return temp;

    }

    /**
     * Servicio para registrar la propuesta
     * @param prop
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarPropuesta")
    public ResponseEntity<?> registrarPropuesta(@RequestBody Propuesta prop ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( prop.getIdPropuesta() == 0){
            acc = "I";
        }

        if( !this.propuestaDao.registrarPropuesta ( prop, acc ) ){
            response.put("msg", "Error al Registrar La propuesta");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos De la propuesta registrados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Servicio para registrar la propuesta
     * @param costoIncre
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarCostoIncre")
    public ResponseEntity<?> registrarCostroIncremento( @RequestBody CostoIncre costoIncre  ){
        Map<String, Object> response = new HashMap<>();

        costoIncre.setIdPropuesta( this.propuestaDao.ultimaIdPropuesta( costoIncre.getAudUsuario() ) );


        String acc = "U";
        if( costoIncre.getIdIncre() == 0){
            acc = "I";
        }

        if( !this.costoIncreDao.registrarCostoIncre ( costoIncre, acc ) ){
            response.put("msg", "Error al Registrar el costo Incremento o de Flete de transporte");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos De la costo de incremento o Transporte De Flete registrado Correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Registrara los precios propuestos en toneladas por familia
     * @param precioPropuesta
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarPrecioPropuesta")
    public ResponseEntity<?> registrarPrecioPropuesta( @RequestBody PrecioPropuesta precioPropuesta  ){
        Map<String, Object> response = new HashMap<>();

        precioPropuesta.setIdPropuesta( this.propuestaDao.ultimaIdPropuesta( precioPropuesta.getAudUsuario() ) );

        String acc = "U";
        if( precioPropuesta.getIdPrecioPropuesta() == 0){
            acc = "I";
        }

        if( !this.precioPropuestaDao.registrarPrecioPropuesta ( precioPropuesta, acc ) ){
            response.put("msg", "Error al registrar el Precio Propuesta");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Precio Propuesto registrados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Metodo para el registro del costo sugerido
     * @param costoSug
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarCostoSug")
    public ResponseEntity<?> registrarCostoSug( @RequestBody CostoSug costoSug  ){
        Map<String, Object> response = new HashMap<>();

        costoSug.setIdPropuesta( this.propuestaDao.ultimaIdPropuesta( costoSug.getAudUsuario() ) );

        String acc = "U";
        if( costoSug.getIdCosSug() == 0){
            acc = "I";
        }

        if( !this.costoSugDao.registrarCostoSug ( costoSug, acc ) ){
            response.put("msg", "Error al registrar el Costo Sugerido");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Costo Sugerido registrados correctamente");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * +
     * @param articuloPropuesto
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/lstArticulosXPropuesta")
    public List<ArticuloPropuesto> obtenerListaDeArticulosXPropuesta( @RequestBody ArticuloPropuesto articuloPropuesto ){


        List<ArticuloPropuesto> temp = new ArrayList<>();
        temp = this.articuloPropuestoDao.listarArticulosPropuesta(articuloPropuesto.getIdPropuesta());

        if( temp.size() == 0 ) return new ArrayList<>();

        return temp;

    }


}
