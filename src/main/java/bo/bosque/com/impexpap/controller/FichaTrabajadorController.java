package bo.bosque.com.impexpap.controller;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.IGaranteReferencia;
import bo.bosque.com.impexpap.dao.IPersona;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.GaranteReferencia;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;

import org.springframework.web.bind.annotation.*;

import bo.bosque.com.impexpap.dao.IDependiente;
import bo.bosque.com.impexpap.model.Dependiente;
import org.springframework.web.multipart.MultipartFile;



@RestController
@CrossOrigin("*")
@RequestMapping("/fichaTrabajador")
public class FichaTrabajadorController {


    private JdbcTemplate jdbcTemplate;
    private final IDependiente dependienteDao;
    private final IGaranteReferencia garanteReferenciaDao;
    private final IPersona personaDao;



    public FichaTrabajadorController( JdbcTemplate jdbcTemplate, IDependiente dependienteDao, IGaranteReferencia garanteReferenciaDao, IPersona personaDao){

        this.jdbcTemplate = jdbcTemplate;
        this.dependienteDao = dependienteDao;
        this.garanteReferenciaDao = garanteReferenciaDao;
        this.personaDao = personaDao;
    }

    /**
     * Servicio para obtener los dependientes por empleado
     * @param dep
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/dependientes")
    public List<Dependiente> obtenerDependientes( @RequestBody Dependiente dep ){

        List<Dependiente> lstTemp = this.dependienteDao.obtenerDependientes( dep.getCodEmpleado() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }

    /**
     * Servicio para obtener los garantes y referencia por empleado
     * @param garRef
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/garanteReferencia")
    public List<GaranteReferencia> obtenerGaranteReferencia( @RequestBody GaranteReferencia garRef ){

        List<GaranteReferencia> lstTemp = this.garanteReferenciaDao.obtenerGaranteReferencia(  garRef.getCodEmpleado() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;

    }


    /**
     * Procedimiento para registrar o actualizar la formacion de un dependiente
     * @param dep
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarDependiente")
    public ResponseEntity<?> registrarDependiente( @RequestBody Dependiente dep ){

        int codPersona =  this.personaDao.obtenerUltimoPersona();
        dep.setCodPersona(codPersona);

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( dep.getCodDependiente() == 0){
            acc = "I";
        }

        if( !this.dependienteDao.registrarDependiente( dep, acc ) ){
            response.put("msg", "Error al Registrar Al Dependiente");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Dependientes Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para registrar o actualizar la informacion de un garante y referencia
     * @param garRef
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarGaranteReferencia")
    public ResponseEntity<?> registrarGaranteReferencia( @RequestBody GaranteReferencia garRef ){

        int codPersona =  this.personaDao.obtenerUltimoPersona();
        garRef.setCodPersona(codPersona);

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( garRef.getCodGarante() == 0){
            acc = "I";
        }

        if( !this.garanteReferenciaDao.registrarGaranteReferencia ( garRef, acc ) ){
            response.put("msg", "Error al Registrar Al Garante o Referencia");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Garante o Referencia Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * para subir la imagen al servidor
     * @param file
     * @param codEmpleado
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("codEmpleado") int codEmpleado ){

        Map<String, Object> response = new HashMap<>();
        //obtener datos de persona ??
        if(!file.isEmpty()){

            String nombreArchivo = file.getOriginalFilename().replace(file.getOriginalFilename(),  String.valueOf(codEmpleado) +".jpg");// y de paso renombramos el archivo

            Path rutaFotoAnterior = Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();
            File archivoFotoAnterior = rutaFotoAnterior.toFile();

            if(archivoFotoAnterior.exists() || archivoFotoAnterior.canRead()) {
                archivoFotoAnterior.delete();
            }


            Path rutaArchivo = Paths.get("uploads").resolve(nombreArchivo).toAbsolutePath();

            try {
                Files.copy(file.getInputStream(), rutaArchivo);



            } catch (IOException e) {
                response.put("msg", "Error al subir la imagen "+e.getMessage());
                response.put("ok", "error");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

        }

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Para obtener la imagen directamente del servidor
     * @param nombreFoto
     * @return
     */
    @GetMapping("uploads/img/{nombreFoto:.+}")
    public ResponseEntity<Resource> verFoto(@PathVariable String nombreFoto ){

        Path rutaArchivo = Paths.get("uploads").resolve(nombreFoto).toAbsolutePath();
        Resource recurso = null;

        try {
            recurso = new UrlResource(rutaArchivo.toUri());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        if( !recurso.exists() && !recurso.isReadable() ) {
            throw new RuntimeException("Error, no se pudo cargar la imagen o foto "+nombreFoto );
        }

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");


        return new ResponseEntity<Resource>(recurso, header, HttpStatus.OK);
    }


    /**
     * Procedimiento para exportar a pdf
     * @param emp
     * @return
     */
    @PostMapping("/pdf")
    public ResponseEntity<?> exportPDF(@RequestBody Empleado emp)  {

        String nombreReporte = "RptFichaTrabajador";


        try{
            Map<String, Object> params = new HashMap<>();
            params.put("codEmpleado", emp.getCodEmpleado()  );
            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDF( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            System.out.println(e.getCause()+" msg:  "+e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }



    }


}

