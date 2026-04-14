package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.dto.DescuentoEmpleadoDTO;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/rrhh")
public class RrhhController {

    private static final String SUCCESS_MESSAGE = "Operación realizada exitosamente";
    private static final String ERROR_MESSAGE = "Error en la solicitud";

    private JdbcTemplate jdbcTemplate;


    private final IEmpleado empDao;
    private final IPersona perDao;
    private final IEmail emailDao;
    private final ITelefono telfDao;
    private final IExperienciaLaboral expLabDao;
    private final IFormacion formDao;
    private final ILicencia licenDao;
    private final ICiudad ciudadDao;
    private final IPais paisDao;
    private final IZona zonaDao;
    private final ISucursal sucDao;
    private final ICargoSucursal cagoSucDao;
    private final IEmpleadoCargo empCargoDao;
    private final IRelEmpEmpr reeDao;
    private final IEducacion eduDao;
    private final INroCuentaBancaria cuentaDao;

    private final ISeguro segDao;
    private final IAfiliacionSeguro afiSegDao;


    private final IEmpresa empresaDao;
    private final ICargo cargoDao;
    private final INivelJerarquico nivelJerarquicoDao;

    public RrhhController(JdbcTemplate jdbcTemplate, IEmail emailDao, ITelefono telfDao, IEmpleado empDao, IPersona perDao, IExperienciaLaboral expLabDao, IFormacion formDao, ILicencia licenDao, IRelEmpEmpr reeDao, ICiudad ciudadDao, IEmpleadoCargo empCargoDao, IPais paisDao, IZona zonaDao, ISucursal sucDao, ICargoSucursal cagoSucDao, IEmpresa empresaDao, ICargo cargoDao, INivelJerarquico nivelJerarquicoDao, IEducacion eduDao,INroCuentaBancaria cuentaDao,ISeguro segDao,IAfiliacionSeguro afiSegDao) {
        this.jdbcTemplate = jdbcTemplate;

        this.emailDao   = emailDao;
        this.telfDao    = telfDao;
        this.empDao     = empDao;
        this.perDao     = perDao;
        this.expLabDao  = expLabDao;
        this.formDao    = formDao;
        this.licenDao   = licenDao;
        this.reeDao     = reeDao;
        this.ciudadDao  = ciudadDao;
        this.empCargoDao = empCargoDao;
        this.paisDao     = paisDao;
        this.zonaDao     = zonaDao;
        this.sucDao      = sucDao;
        this.cagoSucDao  = cagoSucDao;
        this.empresaDao = empresaDao;
        this.cargoDao = cargoDao;
        this.nivelJerarquicoDao = nivelJerarquicoDao;
        this.eduDao = eduDao;
        this.cuentaDao = cuentaDao;
        this.segDao= segDao;
        this.afiSegDao=afiSegDao;
    }




    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/listEmpleados")
    public List<Empleado> obtenerListaPropuesta(@RequestBody Empleado emp){

        List <Empleado> lstTemp = this.empDao.obtenerListaEmpleados( emp.getRelEmpEmpr().getEsActivo() );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }
    /**
     * Procedimiento que obtendra lo datos de un empleado por su codigo
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/detalleEmpleado")
    public Empleado obtenerDetalleEmpleado ( @RequestBody  Empleado emp ){


        Empleado temp = this.empDao.obtenerEmpleado( emp.getCodEmpleado() );
        if(temp == null) return new Empleado();
        //System.out.println(temp.toString());
        return temp;
    }

    /**
     * Procedimiento para que obtendra los datos personales de un empleado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/datosPersonales")
    public Persona obtenerDatosPersonales ( @RequestBody Persona per ){

        Persona temp = this.perDao.obtenerDatosPersonales( per.getCodPersona() );
        if(temp == null ) return new Persona();
        return temp;
    }
    /**
     * Procedimiento para que obtendra los correos por persona
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/emailPersona")
    public List<Email> obtenerEmail ( @RequestBody Email email ){

        List<Email> lstEmail = this.emailDao.obtenerCorreos( email.getCodPersona() );
        if(lstEmail.size() == 0) return new ArrayList<>();
        return lstEmail;
    }

    /**
     * Procedimiento que obtendra los telefonos de una persona
     * @param tel
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/telfPersona")
    public List<Telefono> obtenerTelefono( @RequestBody Telefono tel ){

        List<Telefono> lstTelefono = this.telfDao.obtenerTelefonos( tel.getCodPersona() );
        if( lstTelefono.size() == 0 ) return new ArrayList<>();
        return lstTelefono;

    }
    /**
     * Procedimiento que obtendra los telefonos de una persona
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoTelefono")
    public List<TipoTelefono> obtenerTipoTelefonos(){

        List<TipoTelefono> lstTelefono = this.telfDao.obtenerTipoTelefono();
        if( lstTelefono.size() == 0 ) return new ArrayList<>();
        return lstTelefono;

    }


    /**
     * Procedimiento para obtener la experiencia laboral de un empleado
     * @param expLab
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/expLabEmpleado")
    public List<ExperienciaLaboral> obtenerExperienciaLaboral ( @RequestBody ExperienciaLaboral expLab ){

        List<ExperienciaLaboral> lstExpLab = this.expLabDao.obtenerExperienciaLaboral( expLab.getCodEmpleado() );
        if( lstExpLab.size() == 0 ) return new ArrayList<>();
        return lstExpLab;

    }


    /**
     * Procedimiento para obtener la formacion de un empleado
     * @param form
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/formacionEmpleado")
    public List<Formacion> obtenerFormacion ( @RequestBody Formacion form ){

        List<Formacion> lstForm = this.formDao.obtenerFormacion( form.getCodEmpleado() );
        if( lstForm.size() == 0) return new ArrayList<>();
        return lstForm;
    }

    /**
     * Procedimiento para obtener la licencja de conducir de una persona
     * @param lic
     * @return
     */
    @Secured({"ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/licenciaPersona")
    public List<Licencia> obtenerLicencia ( @RequestBody Licencia lic ){

        List<Licencia> lstLic = this.licenDao.obtenerLicencia( lic.getCodPersona() );
        if(lstLic.size() == 0 ) return new ArrayList<>();
        return  lstLic;

    }

    /**
     * Procedimiento para obtener las ciudades por pais
     * @param ciu
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ciudadxPais")
    public List<Ciudad> obtenerCiudadxPais( @RequestBody Ciudad ciu  ){

        List<Ciudad> lstCiudad =  this.ciudadDao.obtenerCiudadesXPais( ciu.getCodPais() );
        if( lstCiudad.size() == 0 ) return new ArrayList<>();
        return lstCiudad;
    }

    /**
     * Procedimiento para obtener las Zonas por ciudad
     * @param ciu
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/zonas")
    public List<Zona> obtenerZona(@RequestBody Ciudad ciu) {

        int codCiudad = (ciu != null && ciu.getCodCiudad() > 0) ? ciu.getCodCiudad() : 0; // Si no hay código, usamos 0
        List<Zona> lstZona = this.zonaDao.obtenerZonaXCiudad(codCiudad);

        if (lstZona.size() == 0) return new ArrayList<>();
        return lstZona;
    }


    /**
     * Procedimiento para obtener los paises registrados
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/paises")
    public List<Pais> obtenerPaises(){

        List<Pais> lstPais = this.paisDao.obtenerPais();
        if( lstPais.size() == 0 ) return new ArrayList<>();
        return lstPais;
    }

    /**
     * Procedimiento para el abm de una persona
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroPersona")
    public ResponseEntity<?> registrarPersona( @RequestBody Persona per ){

        Map<String, Object> response = new HashMap<>();

        // ... (Código de inicialización y conversión de fechas)

        String acc = "U";
        if( per.getCodPersona() == 0 ){
            acc = "I";
        }

        // 1. Ejecutar la operación de registro/actualización
        Integer resultado=this.perDao.registrarPersona(per,acc);

        // 🚨 AJUSTE CRÍTICO: Manejo de Duplicidad (SP devuelve -1) 🚨

        // CASO 1: Duplicidad confirmada.
        if (resultado != null && resultado == -1) {
            response.put("msg", "ERROR: El C.I. " + per.getCiNumero() + " ya pertenece a otra persona.");
            response.put("ok", "error");
            // ¡El frontend espera este 409 para el bloqueo/autocompletar!
            return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
        }

        // CASO 2: Error genérico (0 o null, si no es duplicidad)
        if (resultado == null || resultado == 0) {
            response.put("msg", "Error al registrar o actualizar la persona.");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST); // 400 Bad Request
        }

        // CASO 3: Éxito (resultado > 0)
        response.put("msg", "Datos de la Persona registrados/actualizados con éxito");
        response.put("ok", "ok");

        response.put("codPersona", acc.equals("I") ? resultado : per.getCodPersona());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para obtener las sucursales por empresa
     * @param suc
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/sucXEmpresa")
    public List<Sucursal> obtenerSucursalesXEmpresa ( @RequestBody Sucursal suc  ){

        List<Sucursal> lstSuc = this.sucDao.obtenerSucursalesXEmpresa( suc.getCodEmpresa() );
        if(lstSuc.size() == 0 ) return new ArrayList<>();
        return lstSuc;
    }

    /**
     * Procedimiento para devovler una lista de los cargos por sucursal que pertenecea a una empresa
     * @param cs
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/cargoXSuc")
    public List<CargoSucursal> obtenerSucursalesXEmpresa ( @RequestBody CargoSucursal cs  ){

        List<CargoSucursal> lstCs = this.cagoSucDao.obtenerSucursalesXEmpresa(  cs.getCodSucursal() );

        if( lstCs.size() == 0 ) return new ArrayList<>();

        return lstCs;

    }


    /**
     * Procedimiento para devovler una lista de los cargos por sucursal que pertenecea a una empresa
     * @param cs
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/sucXCargo")
    public List<CargoSucursal> obtenerCargoEnSurcursal ( @RequestBody CargoSucursal cs  ){

        List<CargoSucursal> lstCs = this.cagoSucDao.obtenerCargoEnSucursales(  cs.getCodCargo() );

        if( lstCs.size() == 0 ) return new ArrayList<>();

        return lstCs;

    }


    /**
     * Procedimiento para registrar Empleado
     * @param emp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroEmpleado")
    public ResponseEntity<?> registrarEmpleado(@RequestBody Empleado emp) {
        Map<String, Object> response = new HashMap<>();
        String acc = (emp.getCodEmpleado() == 0) ? "I" : "U";

        try {
            this.empDao.registroEmpleado(emp, acc);
            response.put("msg", "Datos Actualizados");
            response.put("ok", "ok");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            // Aquí capturamos el mensaje del RAISERROR
            response.put("msg", e.getMessage()); // Este mensaje será: "El sueldo es menor al mínimo nacional."
            response.put("error", "ok");
            // Usamos BAD_REQUEST (400) para que Flutter lo detecte como un error de validación
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Procedimiento para el registro de empleado y cargo
     * @param empCar
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroEmpleadoCargo")
    public ResponseEntity<?> registrarEmpleado(@RequestBody EmpleadoCargo empCar) {

        // 1. Estandarizar fechas (Java a SQL)
        prepararFechas(empCar);

        // 2. Validación de Duplicados: ¿La fecha ya está ocupada?
        Empleado existente = empDao.verificarCargoDuplicado(empCar.getCodEmpleado(), empCar.getFechaInicio());
        if (esRegistroInvalidoPorDuplicado(empCar, existente)) {
            return buildResponse("La fecha seleccionada ya está ocupada por otro registro.", HttpStatus.BAD_REQUEST);
        }

        // 3. Validación Cronológica: ¿Estamos intentando insertar en el pasado?
        String errorCronologico = chequearCronologia(empCar);
        if (errorCronologico != null) {
            return buildResponse(errorCronologico, HttpStatus.BAD_REQUEST);
        }

        // 4. Ejecución del proceso de guardado
        String accion = (empCar.getExiste() == 0) ? "I" : "U";
        if (!this.empCargoDao.registrarEmpleadoCargo(empCar, accion)) {
            return buildResponse("Error crítico al procesar en la base de datos.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return buildResponse("Operación exitosa", HttpStatus.CREATED);
    }
    //inicio metodos de apoyo registro empleado cargo
    private void prepararFechas(EmpleadoCargo empCar) {
        Utiles utiles = new Utiles();
        empCar.setFechaInicio(utiles.fechaJ_a_Sql(empCar.getFechaInicio()));
        if (empCar.getFechaInicioOriginal() != null) {
            empCar.setFechaInicioOriginal(utiles.fechaJ_a_Sql(empCar.getFechaInicioOriginal()));
        }
    }

    private boolean esRegistroInvalidoPorDuplicado(EmpleadoCargo empCar, Empleado existente) {
        if (existente == null) return false; // No hay duplicado, es válido.

        // Si es nuevo (existe == 0) y ya hay alguien en esa fecha -> Inválido.
        if (empCar.getExiste() == 0) return true;

        // Si es edición, solo es inválido si cambió la fecha y la nueva ya está ocupada.
        return !empCar.getFechaInicio().equals(empCar.getFechaInicioOriginal());
    }

    private String chequearCronologia(EmpleadoCargo empCar) {
        // Si edito, busco cargos anteriores al original. Si soy nuevo, busco anteriores al nuevo.
        Date ref = (empCar.getExiste() == 1) ? empCar.getFechaInicioOriginal() : null;
        Empleado frontera = this.empDao.obtenerFechaInicioUltimoCargo(empCar.getCodEmpleado(), ref);

        if (frontera != null && frontera.getEmpleadoCargo().getFechaInicio() != null) {
            Date limite = frontera.getEmpleadoCargo().getFechaInicio();

            // Regla: Nueva fecha debe ser mayor al límite encontrado.
            if (!empCar.getFechaInicio().after(limite)) {
                String tipo = (empCar.getExiste() == 1) ? "anterior" : "actual";
                return "La fecha debe ser mayor a la del cargo " + tipo + " (" + limite + ")";
            }
        }
        return null; // Todo en orden.
    }

    private ResponseEntity<Map<String, Object>> buildResponse(String msg, HttpStatus status) {
        Map<String, Object> map = new HashMap<>();
        map.put("msg", msg);
        return new ResponseEntity<>(map, status);
    }
    //fin metodos de apoyo registro empleado cargo

    /**
     * Procedimiento para listar las fechas de beneficio interno y para planilla
     * @param ree
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/fechasBeneficio")
    public List<RelEmplEmpr> obtenerFechasBeneficios ( @RequestBody RelEmplEmpr ree  ){

        List<RelEmplEmpr> lstRee = this.reeDao.obtenerRelacionesLaborales(  ree.getCodEmpleado() );

        if( lstRee.size() == 0 ) return new ArrayList<>();

        return lstRee;

    }

    /**
     * Procedimiento para el registro de Relacion del empleado con la empresa
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/registroRelEmp")
    public ResponseEntity<?> registrarRelEmpEmpr(
            @RequestBody RelEmplEmpr ree,
            @RequestParam(value = "validar", defaultValue = "false") boolean validar,
            @RequestParam(value = "esHistorico", defaultValue = "false") boolean esHistorico  // ← NUEVO
    ) {
        Map<String, Object> response = new HashMap<>();
        Utiles utiles = new Utiles();

        // 1. Estandarizar fechas
        ree.setFechaIni(utiles.fechaJ_a_Sql(ree.getFechaIni()));
        ree.setFechaFin(utiles.fechaJ_a_Sql(ree.getFechaFin()));

        // 2. Validación Cronológica (Solo si NO es histórico)
        if (validar && !esHistorico) {  // ← CAMBIO: agregar !esHistorico
            String error = chequearCronologiaRelacion(ree);
            if (error != null) {
                response.put("msg", error);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }

        // 3. Persistencia
        String acc = (ree.getCodRelEmplEmpr() == 0) ? "I" : "U";
        if (!this.reeDao.registrarRelEmpEmpr(ree, acc)) {
            response.put("msg", "Error al procesar la Relación Empresa");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 4. Respuesta con el ID generado para el copyWith de Flutter
        response.put("msg", "Operación exitosa");
        response.put("ok", "ok");
        response.put("codRelEmplEmpr", ree.getCodRelEmplEmpr());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    private String chequearCronologiaRelacion(RelEmplEmpr ree) {
        // A. Validación contra último CARGO
        Empleado frontera = this.empDao.obtenerFechaInicioUltimoCargo(ree.getCodEmpleado(), null);
        if (frontera != null && frontera.getEmpleadoCargo().getFechaInicio() != null) {
            Date fechaUltimoCargo = frontera.getEmpleadoCargo().getFechaInicio();
            if (ree.getFechaIni().before(fechaUltimoCargo)) {
                return "La fecha de inicio (" + ree.getFechaIni() + ") no puede ser anterior al último cargo (" + fechaUltimoCargo + ").";
            }
        }

        // B. Validación contra relación laboral ANTERIOR
        List<RelEmplEmpr> historial = this.reeDao.obtenerRelacionesLaborales(ree.getCodEmpleado());
        if (historial != null && !historial.isEmpty()) {
            historial.sort(Comparator.comparing(RelEmplEmpr::getFechaIni));
            RelEmplEmpr ultima = historial.get(historial.size() - 1);

            if (ultima.getFechaFin() != null && !ree.getFechaIni().after(ultima.getFechaFin())) {
                return "La relación debe iniciar después del fin de la anterior (" + ultima.getFechaFin() + ").";
            } else if (ultima.getFechaFin() == null && ree.getCodRelEmplEmpr() == 0) {
                return "Existe una relación activa. Debe finalizarla antes de crear una nueva.";
            }
        }
        return null;
    }
    /**
     * Procedimiento para el registro de Email
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmail")
    public ResponseEntity<?> registroEmail( @RequestBody Email e ){

        Map<String, Object> response = new HashMap<>();

        if(e.getCodPersona()==0){
            System.out.println("Entro en codPersona e == 0");
            e.setCodPersona(this.emailDao.obtenerUltimoCodPersona(e.getAudUsuario()));
        }

        String acc = "U";
        if( e.getCodEmail() == 0){
            acc = "I";
        }

        if( !this.emailDao.registrarEmail( e, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Email del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para eliminar un Email
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/correo/{codEmail}")
    public ResponseEntity<Map<String, Object>> eliminarEmail(@PathVariable int codEmail) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("📩 Eliminando correo con codEmail: " + codEmail);

        boolean eliminado = this.emailDao.registrarEmail(new Email(codEmail), "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el correo: " + codEmail);
            response.put("msg", "Error al eliminar el correo.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ Correo eliminado correctamente: " + codEmail);
        response.put("msg", "Correo eliminado correctamente.");
        return ResponseEntity.ok(response);
    }


    /**
     * Procedimiento para el registro de Telefono
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroTelefono")
    public ResponseEntity<?> registroTelefono( @RequestBody Telefono tel ){


        if(tel.getCodPersona()==0){
            System.out.println("Entro en codPersona == 0");
            tel.setCodPersona(this.telfDao.obtenerUltimoCodPersona(tel.getAudUsuario()));
            System.out.println(tel.toString());
        }



        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( tel.getCodTelefono() == 0){
            acc = "I";

        }

        if( !this.telfDao.registrarTelefono( tel,acc ) ){

            // 🚨 CAMBIO MÍNIMO AQUÍ 🚨
            response.put("msg", "Error: El teléfono ya se encuentra registrado.");
            response.put("error", "Duplicidad");
            // Devolvemos 409 Conflict para un error de lógica de negocio (duplicidad)
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);

        /* EL CÓDIGO ANTERIOR ERA:
        response.put("msg", "Error al Actualizar los Datos del Telefono del Empleado");
        response.put("error", "ok");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        */
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

        /**
         * Procedimiento para eliminar un telefono
         * @param
         * @return
         */
        @Secured({ "ROLE_ADM", "ROLE_LIM" })
        @DeleteMapping("/telefono/{codTelefono}")
        public ResponseEntity<Map<String, Object>> eliminarTelefono(@PathVariable int codTelefono) {
            Map<String, Object> response = new HashMap<>();

            System.out.println("📩 Eliminando telefono con codTelefono: " + codTelefono);

            boolean eliminado = this.telfDao.registrarTelefono(new Telefono(codTelefono), "D");

            if (!eliminado) {
                System.out.println("❌ Error al eliminar el telf: " + codTelefono);
                response.put("msg", "Error al eliminar el telf.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            System.out.println("✅ telf eliminado correctamente: " + codTelefono);
            response.put("msg", "telf eliminado correctamente.");
            return ResponseEntity.ok(response);
        }

    /**
     * Procedimiento para registrar o actualizar la experiencia Laboral
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarExpLaboral")
    public ResponseEntity<?> registrarExpLaboral( @RequestBody ExperienciaLaboral expl ){
        Map<String, Object> response = new HashMap<>();

        expl.setFechaInicio( new Utiles().fechaJ_a_Sql(expl.getFechaInicio()));
        expl.setFechaFin( new Utiles().fechaJ_a_Sql(expl.getFechaFin()));

        String acc = "U";
        if( expl.getCodExperienciaLaboral() == 0){
            acc = "I";
        }

        if( !this.expLabDao.registrarExpLaboral( expl, acc ) ){
            response.put("msg", "Error al Registrar la experiencia laboral del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de ExperienciaLaboral Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para eliminar experiencia laboral
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/expLaboral/{codExperienciaLaboral}")
    public ResponseEntity<Map<String, Object>> eliminarExperienciaLaboral(@PathVariable int codExperienciaLaboral) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("📩 Eliminando explab con codExperienciaLaboral: " + codExperienciaLaboral);

        boolean eliminado = this.expLabDao.registrarExpLaboral(new ExperienciaLaboral(codExperienciaLaboral), "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el explab: " + codExperienciaLaboral);
            response.put("msg", "Error al eliminar el explab.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ explab eliminado correctamente: " + codExperienciaLaboral);
        response.put("msg", "explab eliminado correctamente.");
        return ResponseEntity.ok(response);
    }

    /**
     * Procedimiento para registrar o actualizar la formacion de un empleado
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarFormacion")
    public ResponseEntity<?> registrarFormacion( @RequestBody Formacion fr ){
        Map<String, Object> response = new HashMap<>();

        fr.setFechaFormacion( new Utiles().fechaJ_a_Sql(fr.getFechaFormacion()) );

        String acc = "U";
        if( fr.getCodFormacion() == 0){
            acc = "I";
        }

        if( !this.formDao.registrarFormacion( fr, acc ) ){
            response.put("msg", "Error al Registrar la Formacion del Empleado");
            response.put("ok", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Formacion Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para eliminar formacion
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @DeleteMapping("/formacion/{codFormacion}")
    public ResponseEntity<Map<String, Object>> eliminarFormacion(@PathVariable int codFormacion) {
        Map<String, Object> response = new HashMap<>();

        System.out.println("📩 Eliminando correo con codFormacion: " + codFormacion);

        boolean eliminado = this.formDao.registrarFormacion(new Formacion(codFormacion), "D");

        if (!eliminado) {
            System.out.println("❌ Error al eliminar el formacion: " + codFormacion);
            response.put("msg", "Error al eliminar el formacion.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        System.out.println("✅ Correo eliminado correctamente: " + codFormacion);
        response.put("msg", "Correo eliminado correctamente.");
        return ResponseEntity.ok(response);
    }

    /**
     * Procedimiento para registrar o actualizar la formacion de un empleado
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registrarLicencia")
    public ResponseEntity<?> registrarLicencia( @RequestBody Licencia lc ){
        Map<String, Object> response = new HashMap<>();

        lc.setFechaCaducidad( new Utiles().fechaJ_a_Sql(lc.getFechaCaducidad()) );

        String acc = "U";
        if( lc.getCodLicencia() == 0){
            acc = "I";
        }

        if( !this.licenDao.registrarLicencia( lc, acc ) ){
            response.put("msg", "Error al Registrar la Licencia del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos de Licencia Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para obtener el ultimo codigo insertado de empleado
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/ultimoCodEmpleado")
    public Empleado obtenerUltimoCodEmpleado ( ){
        Empleado temp = new Empleado();
        temp.setCodEmpleado( this.empDao.obtenerUltimoEmpleado() );
        if(temp.getCodEmpleado() <= 0 ) return new Empleado();
        return temp;
    }

    /**
     * Procedimiento para obtener el ultimo codigo insertado de una persona
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/ultimoCodPersona")
    public Persona obtenerUltimoCodPersona ( ){
        Persona temp = new Persona();
        temp.setCodPersona( this.perDao.obtenerUltimoPersona() );
        if(temp.getCodPersona() <= 0 ) return new Persona();
        return temp;
    }
    /**
     * Devolera una lista de los tipos de sexo
     * @return
     */
     @Secured({ "ROLE_ADM", "ROLE_LIM" })
     @PostMapping("/tiposSexo")
     public List<Tipos> lstSexo(){
     return this.perDao.lstSexo();
     }

    /**
     * Devolera una lista de los tipos de ciExp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposCiExp")
    public List<Tipos> lstCiExp(){
        return this.perDao.lstCiExp();
    }
    /**
     * Devolera una lista de los tipos de sexo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposEstCivil")
    public List<Tipos> lstEstadoCivil(){
        return this.perDao.lstEstadoCivil();
    }
    /**
     * Devolera una lista de los tipos de FORMACION
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposFormacion")
    public List<Tipos> lstTipoFormacion(){
        return this.perDao.lstTipoFormacion();
    }
    /**
     * Obtendra una lista de los tipos de duracion para formacion
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @PostMapping("/tiposDuracionFor")
    public List<Tipos>lstTipoDuracionFormacion(){return this.perDao.lstTipoDuracionFormacion();}

    /**
     * Obtendra una lista de personas
     */

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerListaPersonas")
    public List<Persona>obtenerListaPersonas(){
        List<Persona>lstTemp=this.perDao.obtenerListaPersonas();
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /**
     * Procedimiento para el registrar un pais
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroPais")
    public ResponseEntity<?> registrarPais( @RequestBody Pais p ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( p.getCodPais() == 0){
            acc = "I";
        }

        if( !this.paisDao.registrarPais( p, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Email del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para el registrar una zona
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroZona")
    public ResponseEntity<?> registrarZona( @RequestBody Zona z ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( z.getCodZona() == 0){
            acc = "I";
        }

        if( !this.zonaDao.registrarZona( z, acc ) ){
            response.put("msg", "Error al Actualizar la zona del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Procedimiento para el registrar una zona
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroCiudad")
    public ResponseEntity<?> registrarCiudad( @RequestBody Ciudad c ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( c.getCodCiudad() == 0){
            acc = "I";
        }

        if( !this.ciudadDao.registrarCiudad( c, acc ) ){
            response.put("msg", "Error al Actualizar la zona del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/pdfDependientes")
    public ResponseEntity<?> exportPDFDependientesEdad()  {

        String nombreReporte = "RptDependientesPorEdad";


        try{
            Map<String, Object> params = new HashMap<>();

            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDFStatic( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            e.printStackTrace();  // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage());  // Imprime tipo y mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
    /**
     * PARA GENERAR REPORTES DEPENDIENTES SOLO HIJOS
     */
    /**
     * Procedimiento para exportar PDF de dependientes menores a 12 años
     * @return
     */
    @PostMapping("/pdfDependientesHijos")
    public ResponseEntity<?> exportPDFDependientesHijos()  {

        String nombreReporte = "RptDependientesGeneral";


        try{
            Map<String, Object> params = new HashMap<>();

            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDFStatic( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            e.printStackTrace();  // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage());  // Imprime tipo y mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
    /**
     * Procedimiento para que obtendra los datos personales de una persona mediante el CI
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerPersonaXCarnet")
    public Persona obtenerDatosXCarnet ( @RequestBody Persona per ){

        Persona temp = this.perDao.obtenerDatosXCarnet( per.getCiNumero());
        if(temp == null ) return new Persona();
        return temp;
    }
    /**
     * OBTENDRA EL TELEFONO CORPORATIVO DE UN EMPLEADO
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerCorporativoXEmpleado")
    public Telefono obtenerCoporativoXEmpleado( @RequestBody Telefono tel ){
        //int codTipoTel = tel.getCodTipoTel();
        String telefono = tel.getTelefono();

        Telefono temp = this.telfDao.obtenerCorporativo( telefono );
        if( temp == null ) return new Telefono();
        return temp;

    }


    /**
     * =====================================================================
     * Sub Modulo para la Estructura Orzanizacional y Creacion de Empresas
     * =====================================================================
     * */


    /**
     * Obtiene todos las empresas registradas en el sistema.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lst-empresas")
    public ResponseEntity<?> obtenerEmpresas() {
        try {
            List<Empresa> empresas = empresaDao.obtenerEmpresas();

            if (empresas.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron empresas");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, empresas, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * Obtendra la estructura organigrama de una empresa con variables de informacion personalizada.
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstOrganigramaNew")
    public ResponseEntity<?> obtenerOrganigramaXEmpresa( @RequestBody Empresa empresa ) {

        try {
            List<Cargo> cargos = cargoDao.obtenerCargoXEmpresaNew(empresa.getCodEmpresa());

            if (cargos == null || cargos.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT,
                        "No se encontraron cargos para la empresa seleccionada");
            }

            // 1. Crear mapa para acceso rápido a TODOS los cargos
            Map<Integer, Cargo> mapaCargos = new HashMap<>();
            for (Cargo cargo : cargos) {
                mapaCargos.put(cargo.getCodCargo(), cargo);
            }

            // 2. Ordenar TODOS los cargos por nivel y posición
            cargos.sort((c1, c2) -> {
                int cmpNivel = Integer.compare(c1.getNivel(), c2.getNivel());
                return cmpNivel != 0 ? cmpNivel : Integer.compare(c1.getPosicion(), c2.getPosicion());
            });

            // 3. Construir jerarquía COMPLETA - TODOS los cargos se conectan
            for (Cargo cargo : cargos) {
                if (cargo.getCodCargoPadre() != 0) {
                    Cargo padre = mapaCargos.get(cargo.getCodCargoPadre());
                    if (padre != null) {
                        // Inicializar lista si es necesario
                        if (padre.getItems() == null) {
                            padre.setItems(new ArrayList<>());
                        }
                        // Agregar hijo INDEPENDIENTE de su estado o visibilidad
                        padre.getItems().add(cargo);
                    }
                }
            }

            // 4. Obtener TODAS las raíces (codCargoPadre = 0)
            List<Cargo> raices = new ArrayList<>();
            for (Cargo cargo : cargos) {
                if (cargo.getCodCargoPadre() == 0) {
                    raices.add(cargo);
                }
            }

            // 5. Ordenar raíces por posición
            raices.sort((c1, c2) -> Integer.compare(c1.getPosicion(), c2.getPosicion()));

            // 6. Ordenar recursivamente TODOS los hijos
            for (Cargo raiz : raices) {
                ordenarHijos(raiz);
            }

            if (raices.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT,
                        "No se encontraron cargos raíz para la empresa seleccionada");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, raices, HttpStatus.OK.value()));

        } catch (Exception e) {
            System.out.println("Error obteniendo organigrama: " + e.getMessage());
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }

    /**
     * Método auxiliar para ordenar TODOS los hijos recursivamente
     */
    private void ordenarHijos(Cargo cargo) {
        if (cargo.getItems() != null && !cargo.getItems().isEmpty()) {
            // Ordenar TODOS los hijos por posición
            cargo.getItems().sort((c1, c2) -> Integer.compare(c1.getPosicion(), c2.getPosicion()));

            // Recursivamente ordenar hijos de los hijos
            for (Cargo hijo : cargo.getItems()) {
                ordenarHijos(hijo);
            }
        }
    }


    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstNivelesJerarquicos")
    public ResponseEntity<?> obtenerNivelesJerarquicos( ) {

        try {
            List<NivelJerarquico> niveles = nivelJerarquicoDao.getAllNiveles();

            if (niveles.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron los niveles de Jerarquía");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, niveles, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }


    /**
     * Procedimiento para el registrar un cargo o actualizar
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroCargo")
    public ResponseEntity<?> registrarCargo( @RequestBody Cargo c ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( c.getCodCargo() == 0){
            acc = "I";
        }

        if( !this.cargoDao.registrarCargo( c, acc ) ){
            response.put("msg", "Error al Actualizar el Cargo");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    /**
     * Procedimiento para el registrar un cargo o actualizar
     * @param
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroCargoSucursal")
    public ResponseEntity<?> registrarCargoSucursal( @RequestBody CargoSucursal cs ){

        Map<String, Object> response = new HashMap<>();

        String acc = "U";
        if( cs.getCodCargoSucursal() == 0){
            acc = "I";
        }

        if( !this.cagoSucDao.registrarCargoSucursal( cs, acc ) ){
            response.put("msg", "Error al Actualizar el Cargo Sucursal");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Obtiene los empleados asignados a un cargo que pertenece a una empresa
     * @return
     */
    @PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
    @PostMapping("/lstEmpleadosXCargo")
    public ResponseEntity<?> obtenerEmpleadoXCargo( @RequestBody Cargo c ) {
        try {
            List<Cargo> temp = this.cargoDao.obtenerEmpleadosXCargo( c.getCodCargo()  );

            if (temp.isEmpty()) {
                return buildSuccessResponse(HttpStatus.NO_CONTENT, "No se encontraron empleados para este cargo");
            }

            return ResponseEntity.ok()
                    .body(new ApiResponse<>(SUCCESS_MESSAGE, temp, HttpStatus.OK.value()));

        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }


    /**
     * Procedimiento para el eliminar un cargo de una sucursal
     * @param cs
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/eliminarCargoSuc")
    public ResponseEntity<?> eliminarCargoSucursal( @RequestBody CargoSucursal cs ){

        Map<String, Object> response = new HashMap<>();

        if( !this.cagoSucDao.registrarCargoSucursal( cs, "D" ) ){
            response.put("msg", "Error al Eliminar el Cargo Sucursal");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
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
/**
 * =====================================================================
 * Modulo para la el registro de empleados RRHH
 * =====================================================================
 * */
    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerLstEmpleados")
    public List<Empleado> obtenerLstEmpleados(@RequestBody Empleado emp){
        Integer esActivo=emp.getEsActivo();
        String search=emp.getSearch();
        int pageNumber= emp.getPageNumber();
        int pageSize=emp.getPageSize();
        Integer codEmpresa=emp.getCodEmpresa();

        List <Empleado> lstTemp = this.empDao.obtenerLstEmpleados( search,esActivo,pageNumber,pageSize,codEmpresa );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }
    /**
     * OBTENDRA UNA LISTA DE PERSONAS QUE NO SON EMPLEADOS >=18 AÑOS
     */

    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerLstPersonaNoEmpleado")
    public List<Persona>getLstPersonaNoEmpleado(@RequestBody Persona per){
        String buscarPersona=per.getBuscarPersona();
        List<Persona>lstTemp=this.perDao.getLstPersonaNoEmpleado(buscarPersona);
        if (lstTemp.size()==0)return new ArrayList<>();
        return lstTemp;

    }
    /**
     * Devolera una lista de los tipos de educacion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tiposEducacion")
    public List<Tipos> lstEducacion(){
        return this.perDao.lstEducacion();
    }

    /**
     * PROCEDIMIENTO PARA INSERTAR EDUCACION
     * @param ed
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEducacion")
    public ResponseEntity<?> registroEducacion( @RequestBody Educacion ed ){

        Map<String, Object> response = new HashMap<>();
        ed.setFecha( new Utiles().fechaJ_a_Sql(ed.getFecha()) );  // ✅ AGREGAR ESTO

        String acc = "U";
        if( ed.getCodEducacion() == 0){
            acc = "I";
        }

        if( !this.eduDao.registrarEducacion( ed, acc ) ){
            response.put("msg", "Error al Actualizar los Datos de Educacion del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * ELIMINAR EDUCACION
     * @param codEducacion
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @DeleteMapping("/eliminarEducacion/{codEducacion}")
    public ResponseEntity<Map<String, Object>>eliminarEducacion(@PathVariable int codEducacion){
        Map<String,Object>response = new HashMap<>();
        Educacion temp= new Educacion();
        temp.setCodEducacion(codEducacion);

        boolean eliminado =  this.eduDao.registrarEducacion(temp,"D");

        if (!eliminado){
            System.out.println("Error al eliminar educacion"+codEducacion);
            response.put("msg","Error al eliminar esta educacion");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        System.out.println("Educacion eliminada correctamente:"+codEducacion);
        response.put("msg","Educacion eliminada correctamente.");
        return ResponseEntity.ok(response);
    }

    /**
     * PROCEDIMIENTO PARA OBTENER UNA LISTA DE EDUCACION
     * @param educacion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerEducacion")
    public List<Educacion> obtenerEducacion ( @RequestBody Educacion educacion ){

        List<Educacion> lstEducacion = this.eduDao.obtenerEducacion( educacion.getCodEmpleado() );
        if(lstEducacion.size() == 0) return new ArrayList<>();
        return lstEducacion;
    }

    /**
     *OBTENDRA UNA LISTA DE NROS DE CUENTA DE LOS EMPLEADOS
     * @param cuenta
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerNroCuentaBanco")
    public List<NroCuentaBancaria> obtenerCuentaBanco ( @RequestBody NroCuentaBancaria cuenta ){

        List<NroCuentaBancaria> lstCuentas = this.cuentaDao.obtenerCuentasBanco( cuenta.getCodEmpleado() );
        if(lstCuentas.size() == 0) return new ArrayList<>();
        return lstCuentas;
    }

    /**
     * PROCEDIMIENTO PARA REGISTRAR UN NUMERO DE CUENTA BANCARIA
     * @param cb
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroCuentaBanco")
    public ResponseEntity<?> registrarCuentaBancaria( @RequestBody NroCuentaBancaria cb ){

        Map<String, Object> response = new HashMap<>();



        String acc = "U";
        if( cb.getCodCuenta() == 0){
            acc = "I";
        }

        if( !this.cuentaDao.registrarCuentaBancaria( cb, acc ) ){
            response.put("msg", "Error al Actualizar el nro de cuenta");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * ELIMINAR CUENTA BANCARIA
     * @param codCuenta
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @DeleteMapping("/eliminarCuentaBancaria/{codCuenta}")
    public ResponseEntity<Map<String, Object>>eliminarCuentaBancaria(@PathVariable int codCuenta){
        Map<String,Object>response = new HashMap<>();
        NroCuentaBancaria temp= new NroCuentaBancaria();
        temp.setCodCuenta(codCuenta);

        boolean eliminado =  this.cuentaDao.registrarCuentaBancaria(temp,"D");

        if (!eliminado){
            System.out.println("Error al eliminar cuenta bancaria"+codCuenta);
            response.put("msg","Error al eliminar esta cuenta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        System.out.println("Cuenta bancaria eliminada correctamente:"+codCuenta);
        response.put("msg","Cuenta bancaria eliminada correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * Devolera una lista de los tipos de educacion
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoRelacionLaboral")
    public List<Tipos> lstTipoRelacion(){
        return this.perDao.lstTipoRelacion();
    }
    /**
     * PARA GENERAR EL RPT NOMINA EMPLEADOS
     * @return
     */
    @PostMapping("/pdfNominaEmpleados")
    public ResponseEntity<?> exportPDFNominaEmpleados()  {

        String nombreReporte = "RptNominaEmpleados";


        try{
            Map<String, Object> params = new HashMap<>();

            byte[] reportBytes = new JasperReportExport( this.jdbcTemplate).exportPDFStatic( nombreReporte, params);


            HttpHeaders headers = new HttpHeaders();
            //set the PDF format
            headers.setContentLength(reportBytes.length);
            headers.setContentType(MediaType.APPLICATION_PDF);

            return new ResponseEntity<>(reportBytes,headers ,HttpStatus.OK);
        } catch(Exception e) {
            e.printStackTrace();  // 🔥 Imprime el stack trace COMPLETO en consola para ver el error real
            System.err.println("Error detallado: " + e.getClass().getName() + " - " + e.getMessage());  // Imprime tipo y mensaje
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }
    /**
     * Procedimiento para mostrar la relacion laboral activa del empleado
     * @param ree
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/obtenerRelacionLaboral")
    public List<RelEmplEmpr> obtenerRelLab ( @RequestBody RelEmplEmpr ree  ){

        List<RelEmplEmpr> lstRee = this.reeDao.obtenerRelLab(  ree.getCodEmpleado() );

        if( lstRee.size() == 0 ) return new ArrayList<>();

        return lstRee;

    }
    /**
     * ELIMINAR RELACION LABORAL
     * @param codRelEmp
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @DeleteMapping("/eliminarRelacionLaboral/{codRelEmp}")
    public ResponseEntity<Map<String, Object>>eliminarRelacionLaboral(@PathVariable int codRelEmp){
        Map<String,Object>response = new HashMap<>();
        RelEmplEmpr temp= new RelEmplEmpr();
        temp.setCodRelEmplEmpr(codRelEmp);

        boolean eliminado =  this.reeDao.registrarRelEmpEmpr(temp,"D");

        if (!eliminado){
            System.out.println("Error al eliminar relacion laboral"+codRelEmp);
            response.put("msg","Error al eliminar esta relacion");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        System.out.println("relacion laboral eliminada correctamente:"+codRelEmp);
        response.put("msg","relacion laboral eliminada correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * Procedimiento que obtendra el ultimo cargo de un empleado
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/ultimoCargoEmpleado")
    public Empleado ultimoCargoEmpleado ( @RequestBody  Empleado eCargo ){


        Empleado temp = this.empDao.obtenerEmpleadoCargo( eCargo.getCodEmpleado() );
        if(temp == null) return new Empleado();
        //System.out.println(temp.toString());
        return temp;
    }
    /**
     *OBTENDRA EL HISTORIAL DE CARGOS DEL EMPLEADO
     * @param emp
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerCargosEmpleado")
    public List<Empleado> obtenerEmpleadoCargo ( @RequestBody Empleado emp ){

        List<Empleado> lstCargos = this.empDao.obtenerCargosEmpleado( emp.getCodEmpleado() );
        if(lstCargos.size() == 0) return new ArrayList<>();
        return lstCargos;
    }
    /**
     * ELIMINAR CUENTA BANCARIA
     * @param codEmpleado,codCargoSucursal,fechaInicio,codCargoSucPlanilla
     * @return
     */
    @Secured({"ROLE_ADM","ROLE_LIM"})
    @DeleteMapping("/eliminarCargoEmpleado/{codEmpleado}/{codCargoSucursal}/{fechaInicio}/{codCargoSucPlanilla}")
    public ResponseEntity<Map<String, Object>>eliminarCargoEmpleado(@PathVariable int codEmpleado, @PathVariable int codCargoSucursal, @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") Date fechaInicio, @PathVariable int codCargoSucPlanilla){
        Map<String,Object>response = new HashMap<>();
        EmpleadoCargo temp= new EmpleadoCargo();
        temp.setCodEmpleado(codEmpleado);
        temp.setCodCargoSucursal(codCargoSucursal);
        temp.setFechaInicio(fechaInicio);
        temp.setCodCargoSucPlanilla(codCargoSucPlanilla);

        boolean eliminado =  this.empCargoDao.registrarEmpleadoCargo(temp,"D");

        if (!eliminado){
            System.out.println("Error al eliminar cargo del empleado "+codEmpleado+codCargoSucursal+fechaInicio+codCargoSucPlanilla);
            response.put("msg","Error al eliminar cargo empleado");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        System.out.println("Cargo empleado eliminado correctamente:"+codEmpleado+codCargoSucursal+fechaInicio+codCargoSucPlanilla);
        response.put("msg","Cargo empleado eliminado correctamente.");
        return ResponseEntity.ok(response);
    }
    /**
     * Procedimiento para verificar si existe un cargo duplicado (se usara al momento de editar un cargo de un empleado)
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/verificarCargoDuplicado")
    public Empleado verificarCargoDuplicado (@RequestBody EmpleadoCargo emp ){
        //EmpleadoCargo empleadoCargo= new EmpleadoCargo();
        //empleadoCargo.setCodEmpleado(codEmpleado);
        //empleadoCargo.setFechaInicio(fechaInicio);

        Empleado temp = this.empDao.verificarCargoDuplicado( emp.getCodEmpleado(), emp.getFechaInicio());
        if(temp == null) return new Empleado();
        //System.out.println(temp.toString());
        return temp;
    }
    /**
     * Procedimiento para obtener la fechainicio del cargo mas reciente
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerFechaInicioUltimoCargo")
    public Empleado obtenerFechaInicioUltimoCargo (@RequestBody EmpleadoCargo emp ){


        Empleado temp = this.empDao.obtenerFechaInicioUltimoCargo( emp.getCodEmpleado(), emp.getFechaInicio());
        if(temp == null) return new Empleado();
        //System.out.println(temp.toString());
        return temp;
    }
    /**
     * Procedimiento para obtener el ultimo codRelEmplEmpr (relacion laboral registrada)
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerUltimaRelacionLaboral")
    public RelEmplEmpr obtenerUltimoCodRelEmplEmpr (@RequestBody RelEmplEmpr relEmplEmpr ){


        RelEmplEmpr temp = this.reeDao.obtenerUltimoCodRelEmplEmpr( relEmplEmpr.getCodEmpleado());
        if(temp == null) return new RelEmplEmpr();
        //System.out.println(temp.toString());
        return temp;
    }
    /**
     * Procedimiento para el eliminar una licencia de conducir
     * @param lc
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/eliminarLicenciaConducir")
    public ResponseEntity<?> eliminarLicenciaConducir( @RequestBody Licencia lc ){

        Map<String, Object> response = new HashMap<>();

        if( !this.licenDao.registrarLicencia( lc, "D" ) ){
            response.put("msg", "Error al Eliminar la licencia de conducir");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Devolera una lista de los tipos de sexo
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoLicencia")
    public List<Tipos> lstLicencia(){
        return this.perDao.listTipoLicencia();
    }
    /**
     * procedimiento para eliminar imagen del servidor
     */
    @DeleteMapping("/eliminarFoto")
    public ResponseEntity<Map<String, Object>> eliminarArchivo(
            @RequestParam("codEmpleado") int codEmpleado,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("nombreArchivo") String nombreArchivo
    ) {
        Map<String, Object> response = new HashMap<>();
        Path rutaFinal;

        // Lógica para identificar el destino
        if ("foto_perfil".equalsIgnoreCase(tipoDocumento)) {
            // Caso Foto de Perfil: uploads/172.jpg
            rutaFinal = Paths.get("uploads").resolve(nombreArchivo);
        } else {
            // Caso Documentos: uploads/documentos/172/carnet/archivo.jpg
            rutaFinal = Paths.get("uploads", "documentos", String.valueOf(codEmpleado), tipoDocumento)
                    .resolve(nombreArchivo);
        }

        File archivo = rutaFinal.toAbsolutePath().toFile();

        if (archivo.exists()) {
            if (archivo.delete()) {
                response.put("ok", true);
                response.put("msg", "Archivo eliminado correctamente.");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("ok", false);
                response.put("msg", "Error al eliminar: el archivo puede estar en uso.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        response.put("ok", false);
        response.put("msg", "No se encontró el archivo.");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    /**
     * Procedimiento para obtener la lista de empleados
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/obtenerCargosXEmpresa")
    public List<Empleado> obtenerCargosXEmpresa(@RequestBody Empleado emp){
        String search=emp.getSearch();
        Integer codEmpresa=emp.getCodEmpresa();

        List <Empleado> lstTemp = this.empDao.obtenerCargosXEmpresa( search,codEmpresa );

        if( lstTemp.size() == 0 ) return new ArrayList<>();

        return lstTemp;
    }
    /**
     *Obtendra el listado de los seguros (CAJA)
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerSeguros")
    public List<Seguro> obtenerSeguros ( ){

        List<Seguro> lstSeguros = this.segDao.obtenerSeguros();
        if(lstSeguros.size() == 0) return new ArrayList<>();
        return lstSeguros;
    }
    /**
     *Obtendra la afiliacion del seguro de un empleado
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerAfiliacionSeguro")
    public AfiliacionSeguro obtenerAfiliacionSeguro ( @RequestBody AfiliacionSeguro afSeg){
        int codEmpleado=afSeg.getCodEmpleado();
        AfiliacionSeguro temp= this.afiSegDao.obtenerAfiliacionSeguro(codEmpleado);
        if(temp==null)return new AfiliacionSeguro();
        return temp;

    }

    /**
     * Registrara una nueva afiliacion para un empleado
     * @param afSeg
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroAfiliacionSeguro")
    public ResponseEntity<?> registroAfiliacionSeguro( @RequestBody AfiliacionSeguro afSeg ){
        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( afSeg.getCodAfiliacion() == 0 ){
            acc = "I";
        }

        if( !this.afiSegDao.afiliarSeguroEmpleado( afSeg, acc ) ){
            response.put("msg", "Error al Actualizar los Datos de afiliacion al seguro");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * eliminara una afilacion a la aseguradora
     * @param afSeg
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/eliminarAfiliacionSeguro")
    public ResponseEntity<?> eliminarAfiliacionSeguro( @RequestBody AfiliacionSeguro afSeg ){

        Map<String, Object> response = new HashMap<>();

        if( !this.afiSegDao.afiliarSeguroEmpleado( afSeg, "D" ) ){
            response.put("msg", "Error al Eliminar la afiliacion al seguro");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     *
     * @param seg
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroAseguradora")
    public ResponseEntity<?> registroAseguradora( @RequestBody Seguro seg ){
        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( seg.getCodSeguro() == 0 ){
            acc = "I";
        }

        if( !this.segDao.registroAseguradora( seg, acc ) ){
            response.put("msg", "Error al Actualizar los Datos la aseguradora");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/eliminarAseguradora")
    public ResponseEntity<?> eliminarAseguradora( @RequestBody Seguro seg ){

        Map<String, Object> response = new HashMap<>();

        if( !this.segDao.registroAseguradora( seg, "D" ) ){
            response.put("msg", "Error al Eliminar la aseguradora");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    /**
     * Devolera una lista de los tipos de seguro
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" })
    @PostMapping("/tipoSeguro")
    public List<Tipos> lstSeguro(){
        return this.segDao.listTipoSeguro();
    }
    /**
     *Obtendra la afiliacion del seguro de un empleado
     * @param
     * @return
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/obtenerHaberBasico")
    public Empleado obtenerHaberBasico ( @RequestBody Empleado emp){
        int codEmpleado=emp.getCodEmpleado();
        Empleado temp= this.empDao.obtenerHaberBasico(codEmpleado);
        if(temp==null)return new Empleado();
        return temp;

    }


    /**
     * Obtendra los prestamos, anticipos, multas por empleado y periodo
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM"})
    @PostMapping("/prestamos-multas")
    public List<DescuentoEmpleadoDTO> obtenerPrestamosAnticiposYMultasEmpleado (@RequestBody Empleado mb ){

        List<DescuentoEmpleadoDTO> lstTemp = new ArrayList<>() ;

        lstTemp = this.empDao.obtenerPrestamosAnticiposYMultasEmpleado( mb.getMes(), mb.getAnio(),  mb.getCodEmpleado() );

        if ( lstTemp.isEmpty() ) return new ArrayList<>();

        return lstTemp;

    }


}
