package bo.bosque.com.impexpap.controller;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.Email;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.GaranteReferencia;

import bo.bosque.com.impexpap.utils.Tipos;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;

import org.springframework.web.bind.annotation.*;

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
    private final IEmpleado empleadoDao;



    public FichaTrabajadorController( JdbcTemplate jdbcTemplate, IDependiente dependienteDao, IGaranteReferencia garanteReferenciaDao, IPersona personaDao,IEmpleado empleadoDao){

        this.jdbcTemplate = jdbcTemplate;
        this.dependienteDao = dependienteDao;
        this.garanteReferenciaDao = garanteReferenciaDao;
        this.personaDao = personaDao;
        this.empleadoDao= empleadoDao;
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



        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( dep.getCodDependiente() == 0){
            acc = "I";
            int codPersona =  this.personaDao.obtenerUltimoPersona();
            dep.setCodPersona(codPersona);
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
    public ResponseEntity<?> registrarGaranteReferencia(@RequestBody GaranteReferencia garRef) {
        System.out.println(garRef);

        Map<String, Object> response = new HashMap<>();

        // 🔥 Verificar si la persona ya tiene 10 garantes
        int cantidadGarantes = garanteReferenciaDao.contarGarantesPorPersona(garRef.getCodPersona());
        if (cantidadGarantes >= 10) {
            response.put("msg", "No puedes registrar más de 10 garantes");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        if (garRef.getCodGarante() == 0 && garanteReferenciaDao.existeGaranteTipo(
                garRef.getCodPersona(), garRef.getCodEmpleado(), garRef.getTipo())) {
            response.put("msg", "Esta persona ya está registrada con este tipo de garante/referencia para este empleado");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }


        // 🔥 Determinar acción (Insert o Update)
        String acc = (garRef.getCodGarante() == 0) ? "I" : "U";
        if (acc.equals("I") && garRef.getCodPersona() <= 0) {
            int codPersona = personaDao.obtenerUltimoPersona();
            garRef.setCodPersona(codPersona);
        }

        // 🔥 Registrar en la base de datos
        if (!garanteReferenciaDao.registrarGaranteReferencia(garRef, acc)) {
            response.put("msg", "Error al registrar garante o referencia");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.put("msg", "Datos de garante o referencia actualizados");
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
    public ResponseEntity<Resource> verFoto(@PathVariable String nombreFoto) {
        Path rutaArchivo = Paths.get("uploads").resolve(nombreFoto).toAbsolutePath();
        Path rutaImagenPorDefecto = Paths.get("uploads").resolve("icon.png").toAbsolutePath(); // 🔹 Definir imagen por defecto
        Resource recurso;

        try {
            recurso = new UrlResource(rutaArchivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                // 🔹 Si la imagen no existe, usar la imagen por defecto
                recurso = new UrlResource(rutaImagenPorDefecto.toUri());
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");

        header.add("Access-Control-Expose-Headers", "Content-Disposition");

        return new ResponseEntity<>(recurso, header, HttpStatus.OK);
    }
    @GetMapping("uploads/documentos/{codEmpleado}/{tipoDocumento}/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verDocumentoAdjunto(
            @PathVariable String codEmpleado,
            @PathVariable String tipoDocumento,
            @PathVariable String nombreArchivo) {
        // Validación para evitar path traversal
        if (nombreArchivo.contains("..") || !nombreArchivo.matches("^[\\w\\-.]+\\.(jpg|jpeg|png|pdf)$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Path rutaArchivo = Paths.get("uploads", "documentos", codEmpleado, tipoDocumento).resolve(nombreArchivo).toAbsolutePath();
        Path rutaImagenPorDefecto = Paths.get("uploads").resolve("icon.png").toAbsolutePath();
        Resource recurso;

        try {
            recurso = new UrlResource(rutaArchivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                recurso = new UrlResource(rutaImagenPorDefecto.toUri());
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + recurso.getFilename() + "\"");
        header.add("Access-Control-Expose-Headers", "Content-Disposition");

        return new ResponseEntity<>(recurso, header, HttpStatus.OK);
    }
    //lista de todos los documentos (foto: carnet, pasaporte, licencia conducir)
    @GetMapping("/uploads/documentos/{codEmpleado}/all")
    public ResponseEntity<Map<String, List<String>>> listarTodosLosDocumentos(@PathVariable String codEmpleado) {
        Map<String, List<String>> resultado = new HashMap<>();
        String[] tipos = {"carnet", "licencia", "pasaporte"}; // Agrega aquí todos los tipos que uses
        for (String tipo : tipos) {
            Path ruta = Paths.get("uploads", "documentos", codEmpleado, tipo);
            List<String> archivos = new ArrayList<>();
            if (Files.exists(ruta)) {
                try (Stream<Path> stream = Files.walk(ruta, 1)) {
                    archivos = stream
                            .filter(Files::isRegularFile)
                            .map(p -> p.getFileName().toString())
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    // manejar error
                }
            }
            resultado.put(tipo, archivos);
        }
        return ResponseEntity.ok(resultado);
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
    /**
     * Procedimiento para obtener lista de empleados y cantidad de dependientes
     * @param
     * @return
     */

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerDep")
    public List<Empleado> obtenerDependientes(  ){


        List<Empleado>lstTemp=this.empleadoDao.obtenerListaEmpleadoyDependientes();


        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /**
     * Procedimiento para obtener dependientes por empleado
     * @param mb
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerDepEmp")
    public List<Dependiente> obtenerDepEmp( @RequestBody Empleado mb ){
        List<Dependiente>lstTemp=this.dependienteDao.obtenerDepEmp(mb.getCodEmpleado());
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /*
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerInfoEmp")
    public List<Empleado>obtenerInfoEmpleado(){
        List<Empleado>lstTemp=this.empleadoDao.obtenerInfoEmp();
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }*/

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerDatosEmp")
    public List<Empleado>obtenerDatosPerEmp(@RequestBody Empleado dp){
        List<Empleado>lstTemp=this.empleadoDao.obtenerDatosPerEmp(dp.getCodEmpleado());
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /**
     * Devolera una lista de los tipos de FORMACION
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposGarRef")
    public List<Tipos> lstTipoGarRef(){
        return this.garanteReferenciaDao.lstTipoGarRef();
    }
    /**
     * Obtendra una lista de garantes
     */

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerListaGarantes")
    public List<GaranteReferencia>obtenerListaGarantes(){
        List<GaranteReferencia>lstTemp=this.garanteReferenciaDao.obtenerListaGarantes();
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /**
     * Procedimiento para eliminar un GARANTE
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/garante/{codGarante}")
    public ResponseEntity<Map<String, Object>> eliminarGarante(@PathVariable int codGarante) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("📩 Eliminando garante con codGarante: " + codGarante);

        boolean eliminado = this.garanteReferenciaDao.registrarGaranteReferencia(new GaranteReferencia(codGarante), "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el correo: " + codGarante);
            response.put("msg", "Error al eliminar el correo.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ Garante eliminado correctamente: " + codGarante);
        response.put("msg", "Correo eliminado correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * Devolera una lista de los tipos de parentesco
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @PostMapping("/tiposParentesco")
    public List<Tipos>lstTipoDependiente(){return this.dependienteDao.lstTipoDependiente();}
    /**
     * Devolera una lista de los tipos de parentesco
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @PostMapping("/tipoActivo")
    public List<Tipos>lstTipoActivo(){return this.dependienteDao.lstTipoActivo();}
    /**
     * Devolera una lista empleados para ver cumplea;os
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/cumples")
    public List<Empleado>listaCumpleEmpleado(){
        List<Empleado>lstTemp=this.empleadoDao.listaCumpleEmpleado();
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/dependiente/{codDependiente}")
    public ResponseEntity<Map<String, Object>> eliminarDependiente(@PathVariable int codDependiente) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("📩 Eliminando dependiente con codDependiente: " + codDependiente);

        boolean eliminado = this.dependienteDao.registrarDependiente(new Dependiente(codDependiente), "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el dependiente: " + codDependiente);
            response.put("msg", "Error al eliminar el dependiente.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ dependiente eliminado correctamente: " + codDependiente);
        response.put("msg", "dependiente eliminado correctamente.");
        return ResponseEntity.ok(response);
    }
    @PostMapping("/uploads/documentos")
    public ResponseEntity<?> subirDocumentoPersonal(
            @RequestParam("file") MultipartFile file,
            @RequestParam("codEmpleado") int codEmpleado,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("lado") String lado) {

        Map<String, Object> response = new HashMap<>();

        // Validar tipo de documento permitido
        List<String> tiposValidos = Arrays.asList("CARNET", "PASAPORTE", "LICENCIA");
        if (!tiposValidos.contains(tipoDocumento.toUpperCase())) {
            response.put("msg", "Tipo de documento no válido");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        // Validar tipo MIME del archivo
        List<String> tiposPermitidos = Arrays.asList("image/jpeg", "image/png", "application/pdf");
        String contentType = file.getContentType();
        if (file.isEmpty() || contentType == null || !tiposPermitidos.contains(contentType)) {
            response.put("msg", "Archivo vacío o tipo no permitido");
            return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        // Validar lado permitido
        List<String> ladosValidos = Arrays.asList("anverso", "reverso", "foto");
        if (!ladosValidos.contains(lado.toLowerCase())) {
            response.put("msg", "Lado no válido");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            // Crear nombre de archivo
            String extension = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String nombreArchivo = codEmpleado + "_" + tipoDocumento.toLowerCase() + "_" + lado.toLowerCase() + extension;

            // Crear ruta: uploads/documentos/{codEmpleado}/{tipoDocumento}/
            Path ruta = Paths.get("uploads", "pendientes", String.valueOf(codEmpleado), tipoDocumento.toLowerCase());
            Files.createDirectories(ruta);
            Path rutaArchivo = ruta.resolve(nombreArchivo).toAbsolutePath();

            // Guardar archivo
            Files.copy(file.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            // (Opcional) Guardar metadatos en base de datos
            // documentoDao.guardar(new DocumentoPersonal(...));

            response.put("msg", "Documento subido exitosamente");
            response.put("archivo", nombreArchivo);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (IOException e) {
            response.put("msg", "Error al guardar el archivo: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * listado de documentos pendientes
     * @param
     * @return
     */
    @GetMapping("/uploads/pendientes/all")
    public ResponseEntity<List<Map<String, String>>> listarPendientes() {
        List<Map<String, String>> resultado = new ArrayList<>();
        File baseDir = new File("uploads/pendientes");
        if (baseDir.exists()) {
            for (File empleadoDir : baseDir.listFiles()) {
                String codEmpleado = empleadoDir.getName();

                // Aquí usas tu DAO para obtener el nombre completo
                String nombreCompleto = "";
                try {
                    int codEmpInt = Integer.parseInt(codEmpleado);
                    List<Empleado> empleados = empleadoDao.obtenerDatosPerEmp(codEmpInt);
                    if (!empleados.isEmpty()) {
                        Empleado emp = empleados.get(0);
                        nombreCompleto = emp.getPersona().getDatoPersona();
                    }
                } catch (Exception e) {
                    nombreCompleto = "";
                }

                for (File tipoDir : empleadoDir.listFiles()) {
                    String tipoDocumento = tipoDir.getName();
                    for (File archivo : tipoDir.listFiles()) {
                        if (archivo.isFile()) {
                            Map<String, String> info = new HashMap<>();
                            info.put("codEmpleado", codEmpleado);
                            info.put("tipoDocumento", tipoDocumento);
                            info.put("nombreArchivo", archivo.getName());
                            info.put("url", "/fichaTrabajador/uploads/pendientes/" + codEmpleado + "/" + tipoDocumento + "/" + archivo.getName());
                            info.put("nombreCompleto", nombreCompleto); // <-- AGREGA AQUÍ
                            resultado.add(info);
                        }
                    }
                }
            }
        }
        return ResponseEntity.ok(resultado);
    }
    /**
     * si el adm aprueba la img esta se mueve a la carpeta documentos
     * @param
     * @return
     */
    @PostMapping("/uploads/pendientes/aprobar")
    public ResponseEntity<?> aprobarDocumento(@RequestBody Map<String, String> body) {
        String codEmpleado = body.get("codEmpleado");
        String tipoDocumento = body.get("tipoDocumento");
        String nombreArchivo = body.get("nombreArchivo");

        Path origen = Paths.get("uploads", "pendientes", codEmpleado, tipoDocumento, nombreArchivo);
        Path destinoDir = Paths.get("uploads", "documentos", codEmpleado, tipoDocumento);
        Path destino = destinoDir.resolve(nombreArchivo);

        try {
            Files.createDirectories(destinoDir);
            Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok().body("Imagen aprobada y movida correctamente");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al mover la imagen: " + e.getMessage());
        }
    }
    /**
     * si el adm rechaza la img esta se elimina
     * @param
     * @return
     */
    @PostMapping("/uploads/pendientes/rechazar")
    public ResponseEntity<?> rechazarDocumento(@RequestBody Map<String, String> body) {
        String codEmpleado = body.get("codEmpleado");
        String tipoDocumento = body.get("tipoDocumento");
        String nombreArchivo = body.get("nombreArchivo");

        Path archivo = Paths.get("uploads", "pendientes", codEmpleado, tipoDocumento, nombreArchivo);

        try {
            Files.deleteIfExists(archivo);
            // Aquí podrías notificar al usuario (por ejemplo, guardar un archivo de texto, enviar email, etc.)
            return ResponseEntity.ok().body("Imagen rechazada y eliminada");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al eliminar la imagen: " + e.getMessage());
        }
    }
    /**
     * para servir imagenes
     * @param
     * @return
     */
    @GetMapping("/uploads/pendientes/{codEmpleado}/{tipoDocumento}/{nombreArchivo:.+}")
    public ResponseEntity<Resource> verDocumentoPendiente(
            @PathVariable String codEmpleado,
            @PathVariable String tipoDocumento,
            @PathVariable String nombreArchivo) {
        // Validación para evitar path traversal
        if (nombreArchivo.contains("..") || !nombreArchivo.matches("^[\\w\\-.]+\\.(jpg|jpeg|png|pdf)$")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Path rutaArchivo = Paths.get("uploads", "pendientes", codEmpleado, tipoDocumento).resolve(nombreArchivo).toAbsolutePath();
        Path rutaImagenPorDefecto = Paths.get("uploads").resolve("icon.png").toAbsolutePath();
        Resource recurso;

        try {
            recurso = new UrlResource(rutaArchivo.toUri());
            if (!recurso.exists() || !recurso.isReadable()) {
                recurso = new UrlResource(rutaImagenPorDefecto.toUri());
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + recurso.getFilename() + "\"");
        header.add("Access-Control-Expose-Headers", "Content-Disposition");

        return new ResponseEntity<>(recurso, header, HttpStatus.OK);
    }


    //comentario test (borrar si ya no se necesita)
}

