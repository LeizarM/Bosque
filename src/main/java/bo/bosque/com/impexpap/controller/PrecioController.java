package bo.bosque.com.impexpap.controller;

import java.util.ArrayList;
import java.util.List;

import bo.bosque.com.impexpap.dao.IArticuloPropuesto;
import bo.bosque.com.impexpap.dao.ICostoIncre;
import bo.bosque.com.impexpap.dao.IProducto;
import bo.bosque.com.impexpap.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import bo.bosque.com.impexpap.dao.IAutorizacion;
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

    PrecioController(IAutorizacion autDao, ICostoIncre costoIncreDao, IProducto productoDao, IArticuloPropuesto articuloPropuestoDao){
        this.autDao = autDao;
        this.costoIncreDao = costoIncreDao;
        this.productoDao = productoDao;
        this.articuloPropuestoDao = articuloPropuestoDao;

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




}
