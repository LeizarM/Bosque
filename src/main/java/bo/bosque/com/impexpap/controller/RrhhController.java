package bo.bosque.com.impexpap.controller;
import bo.bosque.com.impexpap.commons.JasperReportExport;
import bo.bosque.com.impexpap.dao.*;
import bo.bosque.com.impexpap.model.*;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.Tipos;
import bo.bosque.com.impexpap.utils.Utiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


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


    private final IEmpresa empresaDao;
    private final ICargo cargoDao;
    private final INivelJerarquico nivelJerarquicoDao;

    public RrhhController(JdbcTemplate jdbcTemplate, IEmail emailDao, ITelefono telfDao, IEmpleado empDao, IPersona perDao, IExperienciaLaboral expLabDao, IFormacion formDao, ILicencia licenDao, IRelEmpEmpr reeDao, ICiudad ciudadDao, IEmpleadoCargo empCargoDao, IPais paisDao, IZona zonaDao, ISucursal sucDao, ICargoSucursal cagoSucDao, IEmpresa empresaDao, ICargo cargoDao, INivelJerarquico nivelJerarquicoDao) {
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
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmpleado")
    public ResponseEntity<?> registrarEmpleado( @RequestBody Empleado emp ){
        Map<String, Object> response = new HashMap<>();


        String acc = "U";
        if( emp.getCodEmpleado() == 0 ){
            acc = "I";
        }

        if( !this.empDao.registroEmpleado( emp, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Procedimiento para el registro de empleado y cargo
     * @param empCar
     * @return
     */
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroEmpleadoCargo")
    public ResponseEntity<?> registrarEmpleado( @RequestBody EmpleadoCargo empCar ){


        Map<String, Object> response = new HashMap<>();
        empCar.setFechaInicio( new Utiles().fechaJ_a_Sql(empCar.getFechaInicio()));
        System.out.println(empCar.toString());

        String acc = "U";
        if(empCar.getExiste() == 0)  acc = "I";

        if( !this.empCargoDao.registrarEmpleadoCargo( empCar, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado Cargo");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

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
    @Secured ( { "ROLE_ADM", "ROLE_LIM" }  )
    @PostMapping("/registroRelEmp")
    public ResponseEntity<?> registrarRelEmpEmpr( @RequestBody RelEmplEmpr ree ){
        Map<String, Object> response = new HashMap<>();
        ree.setFechaIni( new Utiles().fechaJ_a_Sql(ree.getFechaIni()));
        ree.setFechaFin( new Utiles().fechaJ_a_Sql(ree.getFechaFin()));

        String acc = "U";
        if( ree.getCodRelEmplEmpr() == 0){
            acc = "I";
        }

        if( !this.reeDao.registrarRelEmpEmpr( ree, acc ) ){
            response.put("msg", "Error al Actualizar los Datos del Empleado Relacion Empresa");
            response.put("error", "ok");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        response.put("msg", "Datos Actualizados");
        response.put("ok", "ok");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
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
        int codTipoTel = tel.getCodTipoTel();
        String telefono = tel.getTelefono();

        Telefono temp = this.telfDao.obtenerCorporativo( codTipoTel,telefono );
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


}
