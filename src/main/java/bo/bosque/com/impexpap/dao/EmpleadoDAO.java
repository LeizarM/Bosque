package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.dto.DescuentoEmpleadoDTO;
import bo.bosque.com.impexpap.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


@Repository
public class EmpleadoDAO implements IEmpleado{

    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Procedimiento para obtener los Empleados
     * @return List<Empleado>
     */
    public List<Empleado> obtenerListaEmpleados( int esActivo ) {
        List<Empleado>lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_Empleado @esActivo=?, @ACCION=?",
                    new Object[] { esActivo, "B" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.setCodEmpleado( rs.getInt(1) );
                        temp.setCodPersona( rs.getInt(2) );
                        temp.getPersona().setDatoPersona( rs.getString(3) );
                        temp.getRelEmpEmpr().setEsActivo( rs.getInt(4) );
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(5));
                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleados, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }

    /**
     *
     * Obtendra a un empleado por su codigo
     * @param codEmpleado
     * @return
     */
    public Empleado obtenerEmpleado( int codEmpleado ) {

        Empleado emp = new Empleado();
        try {
            emp =  this.jdbcTemplate.queryForObject("execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "O" },
                    new int[] { Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.setNumCuenta(rs.getString(3));
                        temp.setCodRelBeneficios( rs.getInt(4) );
                        temp.setCodRelPlanilla( rs.getInt(5) );

                        temp.getRelEmpEmpr().setFechaInicioBeneficio(rs.getDate(6));
                        temp.getRelEmpEmpr().setFechaInicioPlanilla(rs.getDate(7));

                        temp.getRelEmpEmpr().setCodRelEmplEmpr(rs.getInt(8));
                        temp.getRelEmpEmpr().setEsActivo(rs.getInt(9));
                        temp.getRelEmpEmpr().setNombreFileContrato(rs.getString(10));
                        temp.getRelEmpEmpr().setFechaIni(rs.getDate(11));
                        temp.getRelEmpEmpr().setFechaFin(rs.getDate(12));
                        temp.getRelEmpEmpr().setMotivoFin(rs.getString(13));
                        temp.getRelEmpEmpr().setTipoRel(rs.getString(14));

                        temp.getEmpleadoCargo().getCargoSucursal().setCodCargo(rs.getInt(15));
                        temp.getEmpleadoCargo().getCargoSucursal().setCodCargoSucursal(rs.getInt(16));

                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargo(rs.getInt(17));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(18));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresa(rs.getInt(19));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresa(rs.getString(20));
                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(21));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setSucursal(rs.getString(22));

                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargoPlanilla(rs.getInt(23));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcionPlanilla(rs.getString(24));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresaPlanilla(rs.getInt(25));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresaPlanilla(rs.getString(26));
                        temp.getEmpleadoCargo().setCodCargoSucPlanilla(rs.getInt(27));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setSucursalPlanilla(rs.getString(28));

                        temp.getEmpleadoCargo().setFechaInicio(rs.getDate(29));
                        temp.setHaberBasico(rs.getFloat(30));

                    return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            emp = new Empleado();
            this.jdbcTemplate = null;
        }

        return emp;
    }

    /**
     * Procedimiento para el abm del empleado
     * @param emp
     * @param acc
     * @return
     */
    public boolean registroEmpleado(Empleado emp, String acc) {
         int resp;
         try{
             resp = this.jdbcTemplate.update("execute p_abm_empleado  @codEmpleado=?, @codPersona=?, @numCuenta=?, @codRelBeneficios=?, @codRelPlanilla=?, @audUsuarioI=?,@RETORNA=?,@haberBasico=?, @ACCION=?",
                     ps->{
                        ps.setInt (1, emp.getCodEmpleado() );
                        ps.setInt( 2, emp.getCodPersona() );
                        ps.setString( 3, emp.getNumCuenta() );
                        ps.setInt( 4, emp.getCodRelBeneficios() );
                        ps.setInt( 5, emp.getCodRelPlanilla() );
                        ps.setInt( 6, emp.getAudUsuarioI());
                        ps.setNull(7, java.sql.Types.INTEGER);
                        ps.setFloat(8,emp.getHaberBasico());
                        ps.setString( 9, acc  );

                     });
             return true;
         }catch (Exception e) {
             // Capturamos el mensaje del RAISERROR que viene de SQL
             String mensajeSql = e.getMessage();
             if (e.getCause() != null && e.getCause() instanceof SQLException) {
                 mensajeSql = e.getCause().getMessage();
             }
             // Lanzamos una RuntimeException con el mensaje de SQL para que el Controller la atrape
             throw new RuntimeException(mensajeSql);
         }
    }


    /**
     *
     * Obtendra el codigo del ultimo empleado insertado
     * @return
     */
    public int obtenerUltimoEmpleado() {

        Empleado temp = new Empleado();
        try {
            temp = this.jdbcTemplate.queryForObject("execute p_list_Empleado @ACCION=?",
                    new Object[] {  "K" },
                    new int[] { Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado emp = new Empleado();
                        emp.setCodEmpleado(rs.getInt(1));
                       return emp;
                    });
        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en registroEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            temp = new Empleado();
        }
        return temp.getCodEmpleado();
     }

    /**
     *
     * Procedimiento para obtener emplado y dependientes
     * @return
     */
    public List<Empleado> obtenerListaEmpleadoyDependientes(int codEmpleado) {
        List<Empleado> lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(
                    "execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                    new Object[]{codEmpleado,"X"},
                    new int[]{Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.setCodEmpleado(rs.getInt("codEmpleado"));
                        temp.getPersona().setDatoPersona(rs.getString("NombreCompleto"));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString("CargoActual"));
                        temp.getDependiente().setCodEmpleado(rs.getInt("Dependientes"));
                        temp.getEmpresa().setNombre(rs.getString("Empresa"));
                        temp.getSucursal().setNombre(rs.getString("Sucursal"));
                        temp.getRelEmpEmpr().setEsActivo(rs.getInt("esActivo"));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerListaEmpleadoyDependientes,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }

//PARA OBTENER DATOS PERSONALES DEL EMPLEADO
    public List<Empleado>obtenerDatosPerEmp(int codEmpleado){
        List<Empleado>lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                    new Object[]{codEmpleado,"V"},
                    new int[]{Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt("codEmpleado"));
                        temp.setCodPersona(rs.getInt("codPersona"));
                        temp.setCodZona(rs.getInt("codZona"));
                        temp.getPersona().setDatoPersona(rs.getString("NombreCompleto"));
                        temp.getPersona().setCiExpedido(rs.getString("ciExpedido"));
                        temp.getPersona().setCiFechaVencimiento(rs.getDate("ciFechaVencimiento"));
                        temp.getPersona().setCiNumero(rs.getString("ciNumero"));
                        temp.getPersona().setDireccion(rs.getString("direccion"));
                        temp.getPersona().setEstadoCivil(rs.getString("estadoCivil"));
                        temp.getPersona().setFechaNacimiento(rs.getDate("fechaNacimiento"));
                        temp.getPersona().setLugarNacimiento(rs.getString("lugarNacimiento"));
                        temp.getPersona().setNacionalidad(rs.getInt("nacionalidad"));
                        temp.getPersona().setSexo(rs.getString("sexo"));
                        temp.getPersona().setLat(rs.getInt("lat"));
                        temp.getPersona().setLng(rs.getInt("lng"));
                        temp.getPersona().setAudUsuarioI(rs.getInt("AudUsuarioI"));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerDatosPerEmp,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    //OBTIENE UNA LISTA DE EMPLEADOS CON LA FECHA DE CUMPLEA;OS
    public List<Empleado>listaCumpleEmpleado(){
        List<Empleado>lstTemp;
        try{
            lstTemp = this.jdbcTemplate.query(" execute p_list_Empleado  @ACCION=?",
                    new Object[]{"W"},
                    new int[]{Types.VARCHAR},
                    (rs, rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.getPersona().setDatoPersona(rs.getString(3));
                        temp.getPersona().setFechaNacimiento(rs.getDate(4));
                        temp.getSucursal().setNombre(rs.getString(5));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en listaCumpleEmpleado,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;
    }
    //PARA OBTENER DATOS PERSONALES DEL EMPLEADO
    public Empleado obtenerDatosEmpleado(int codEmpleado){
         Empleado Temp;
        try{
            Temp = this.jdbcTemplate.queryForObject(" execute p_list_Empleado @codEmpleado=?, @ACCION=?",
                    new Object[]{codEmpleado,"V"},
                    new int[]{Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt("codEmpleado"));
                        temp.setCodPersona(rs.getInt("codPersona"));
                        temp.setCodZona(rs.getInt("codZona"));
                        temp.getPersona().setDatoPersona(rs.getString("NombreCompleto"));
                        temp.getPersona().setCiExpedido(rs.getString("ciExpedido"));
                        temp.getPersona().setCiFechaVencimiento(rs.getDate("ciFechaVencimiento"));
                        temp.getPersona().setCiNumero(rs.getString("ciNumero"));
                        temp.getPersona().setDireccion(rs.getString("direccion"));
                        temp.getPersona().setEstadoCivil(rs.getString("estadoCivil"));
                        temp.getPersona().setFechaNacimiento(rs.getDate("fechaNacimiento"));
                        temp.getPersona().setLugarNacimiento(rs.getString("lugarNacimiento"));
                        temp.getPersona().setNacionalidad(rs.getInt("nacionalidad"));
                        temp.getPersona().setSexo(rs.getString("sexo"));
                        temp.getPersona().setLat(rs.getInt("lat"));
                        temp.getPersona().setLng(rs.getInt("lng"));
                        temp.getPersona().setAudUsuarioI(rs.getInt("AudUsuarioI"));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerDatosPerEmp,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            Temp = new Empleado();
            this.jdbcTemplate = null;
        }
        return Temp;
    }
    //RESTRINGIR INFORMACION SEGUN JERARQUIA
    public Empleado obtenerInfoEmpleado(int codEmpleado,int codEmpleadoConsultado){
        Empleado Temp;
        try{
            Temp = this.jdbcTemplate.queryForObject(" execute p_list_Empleado @codEmpleado=?,@codEmpleadoConsultado=?, @ACCION=?",
                    new Object[]{codEmpleado,codEmpleadoConsultado,"Z"},
                    new int[]{Types.INTEGER,Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt("codEmpleado"));
                        temp.setCodPersona(rs.getInt("codPersona"));
                        temp.setCodZona(rs.getInt("codZona"));
                        temp.getPersona().setDatoPersona(rs.getString("NombreCompleto"));
                        temp.getPersona().setCiExpedido(rs.getString("ciExpedido"));
                        temp.getPersona().setCiFechaVencimiento(rs.getDate("ciFechaVencimiento"));
                        temp.getPersona().setCiNumero(rs.getString("ciNumero"));
                        temp.getPersona().setDireccion(rs.getString("direccion"));
                        temp.getPersona().setEstadoCivil(rs.getString("estadoCivil"));
                        temp.getPersona().setFechaNacimiento(rs.getDate("fechaNacimiento"));
                        temp.getPersona().setLugarNacimiento(rs.getString("lugarNacimiento"));
                        temp.getPersona().setNacionalidad(rs.getInt("nacionalidad"));
                        temp.getPersona().setSexo(rs.getString("sexo"));
                        temp.getZona().setZona(rs.getString("zona"));
                        temp.getPersona().setLat(rs.getFloat("lat"));
                        temp.getPersona().setLng(rs.getFloat("lng"));
                        temp.getPersona().setAudUsuarioI(rs.getInt("AudUsuarioI"));
                        return temp;
                    });
        }catch (BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerDatosPerEmp,DataAccessException->"+e.getMessage()+".SQL code->"+((SQLException)e.getCause()).getErrorCode());
            Temp = new Empleado();
            this.jdbcTemplate = null;
        }
        return Temp;
    }

    /**
     * Procedimiento para mostrar lista de empleados
     *
     * @return
     */
    @Override
    public List<Empleado> lisEmpleados() {

        List<Empleado> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute p_list_Empleado @ACCION = ?",
                    new Object[]{ "S" },
                    new int[]{ Types.VARCHAR },
                    (rs, rowCount) ->{
                        Empleado temp = new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.setNombres(rs.getString(3));
                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en lisEmpleados, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;


    }
    /**
     * MODULO EMPLEADOS RRHH
     */
    /**
     * Procedimiento para obtener los Empleados
     * @return List<Empleado>
     */
    public List<Empleado> obtenerLstEmpleados( String search,Integer esActivo, int pageNumber,int pageSize, Integer codEmpresa ) {
        List<Empleado>lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_Empleado @search=?,@esActivo=?,@pageNumber=?,@pageSize=?,@codEmpresa=?, @ACCION=?",
                    new Object[] { search,esActivo,pageNumber,pageSize,codEmpresa, "Y" },
                    new int[] { Types.VARCHAR ,Types.INTEGER,Types.INTEGER,Types.INTEGER,Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.setFila(rs.getInt(1));
                        temp.setCodEmpleado( rs.getInt(2) );
                        temp.setCodPersona( rs.getInt(3) );
                        temp.getPersona().setDatoPersona( rs.getString(4) );
                        temp.getRelEmpEmpr().setEsActivo( rs.getInt(5) );
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(6));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcionPlanilla(rs.getString(7));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresa(rs.getString(8));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresaPlanilla(rs.getInt(9));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresaPlanilla(rs.getString(10));


                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleados, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }
    /**
     * obtendra el ultimo cargo de un empleado
     */
    public Empleado obtenerEmpleadoCargo (int codEmpleado){
        Empleado eCargo;
        try{
            eCargo = this.jdbcTemplate.queryForObject("execute p_list_EmpleadoCargo @codEmpleado=?,@ACCION=?",
                    new Object []{codEmpleado,"C"},
                    new int [] {Types.INTEGER,Types.VARCHAR},
                    (rs,rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.getPersona().setDatoPersona(rs.getString(2));
                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(3));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargo(rs.getInt(4));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(5));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setCodSucursal(rs.getInt(6));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setNombre(rs.getString(7));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().getEmpresa().setCodEmpresa(rs.getInt(8));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().getEmpresa().setNombre(rs.getString(9));
                        temp.getEmpleadoCargo().setCodCargoSucPlanilla(rs.getInt(10));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargoPlanilla(rs.getInt(11));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcionPlanilla(rs.getString(12));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setCodSucursalPlanilla(rs.getInt(13));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setNombrePlanilla(rs.getString(14));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresaPlanilla(rs.getInt(15));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresaPlanilla(rs.getString(16));
                        temp.getEmpleadoCargo().setFechaInicio(rs.getDate(17));
                        return temp;


                    });
        }catch (BadSqlGrammarException e) {
            System.out.println("Error: PersonaDao en obtenerEmpleadoCargo, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            eCargo = new Empleado();
            this.jdbcTemplate = null;
        }

        return eCargo;
    }
    public List<Empleado> obtenerCargosEmpleado( int codEmpleado ) {
        List<Empleado>lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_EmpleadoCargo @codEmpleado=?, @ACCION=?",
                    new Object[] { codEmpleado, "D" },
                    new int[] {Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.getPersona().setDatoPersona(rs.getString(2));
                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(3));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargo(rs.getInt(4));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(5));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setCodSucursal(rs.getInt(6));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setNombre(rs.getString(7));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().getEmpresa().setCodEmpresa(rs.getInt(8));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().getEmpresa().setNombre(rs.getString(9));
                        temp.getEmpleadoCargo().setCodCargoSucPlanilla(rs.getInt(10));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodCargoPlanilla(rs.getInt(11));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcionPlanilla(rs.getString(12));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setCodSucursalPlanilla(rs.getInt(13));
                        temp.getEmpleadoCargo().getCargoSucursal().getSucursal().setNombrePlanilla(rs.getString(14));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresaPlanilla(rs.getInt(15));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresaPlanilla(rs.getString(16));
                        temp.getEmpleadoCargo().setFechaInicio(rs.getDate(17));
                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerCargosEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }
    /**
     * Procedimiento para verificar si existe un cargo duplicado al momento de editar
     */
    public Empleado verificarCargoDuplicado (int codEmpleado, Date fechaInicio){
        Empleado eCargo= null;
        try{
            eCargo = this.jdbcTemplate.queryForObject("execute p_list_EmpleadoCargo @codEmpleado=?, @fechaInicio=?,@ACCION=?",
                    new Object []{codEmpleado,fechaInicio,"L"},
                    new int [] {Types.INTEGER,Types.DATE,Types.VARCHAR},
                    (rs,rowNum)->{
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(2));
                        temp.getEmpleadoCargo().setCodCargoSucPlanilla(rs.getInt(3));
                        temp.getEmpleadoCargo().setFechaInicio(rs.getDate(4));
                        return temp;


                    });
        }catch (Exception e) {
            //System.out.println("Error: PersonaDao en verificarCargoDuplicado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            //eCargo = new Empleado();
            //this.jdbcTemplate = null;
            // Cualquier excepción (incluyendo EmptyResultDataAccessException): devolver null
            System.out.println("Info: No se encontró cargo para codEmpleado=" + codEmpleado + ", fechaInicio=" + fechaInicio);
            eCargo = null;
        }

        return eCargo;
    }
    /**
     *  Procedimiento para obtener la fechainicio del cargo mas reciente
     */
    public Empleado obtenerFechaInicioUltimoCargo (int codEmpleado, Date fechaInicio){
        Empleado eCargo= null;
        try{
            eCargo = this.jdbcTemplate.queryForObject("execute p_list_EmpleadoCargo @codEmpleado=?, @fechaInicioOriginal=?,@ACCION=?",
                    new Object []{codEmpleado,fechaInicio,"E"},
                    new int [] {Types.INTEGER,Types.DATE,Types.VARCHAR},
                    (rs,rowNum)->{
                        Empleado temp= new Empleado();
                        temp.getEmpleadoCargo().setFechaInicio(rs.getDate(1));
                        return temp;
                    });
        }catch (EmptyResultDataAccessException e) {
            System.out.println("Info: No se encontró cargo para codEmpleado=" + codEmpleado);
            return null;
        } catch (Exception e) {
            System.err.println("Error en obtenerFechaInicioUltimoCargo: " + e.getMessage());
            return null;
        }
        return eCargo;
    }
    /**
     * Procedimiento para obtener los Empleados
     * @return List<Empleado>
     */
    public List<Empleado> obtenerCargosXEmpresa( String search,Integer codEmpresa ) {
        List<Empleado>lstTemp;
        try {
            lstTemp =  this.jdbcTemplate.query("execute p_list_EmpleadoCargo @search=?,@codEmpresa=?, @ACCION=?",
                    new Object[] { search,codEmpresa, "F" },
                    new int[] { Types.VARCHAR ,Types.INTEGER,Types.VARCHAR },
                    (rs, rowNum) -> {
                        Empleado temp = new Empleado();
                        temp.getEmpleadoCargo().getCargoSucursal().setCodCargoSucursal(rs.getInt(1));
                        temp.getEmpleadoCargo().getCargoSucursal().setCodCargo(rs.getInt(2));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion(rs.getString(3));
                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(4));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setSucursal(rs.getString(5));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setCodEmpresa(rs.getInt(6));
                        temp.getEmpleadoCargo().getCargoSucursal().getCargo().setNombreEmpresa(rs.getString(7));


                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerEmpleados, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }

        return  lstTemp;
    }
    public Empleado obtenerHaberBasico (int codEmpleado){
        Empleado emp= new Empleado();
        try{
            emp=this.jdbcTemplate.queryForObject("execute p_list_empleado @codEmpleado=?,@ACCION=?",
                    new Object[]{codEmpleado,"F"},
                    new int []{Types.INTEGER,Types.VARCHAR},
                    (rs, rowNum) -> {
                        Empleado temp= new Empleado();
                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setCodPersona(rs.getInt(2));
                        temp.setHaberBasico(rs.getFloat(3));
                        temp.setEsActivo(rs.getInt(4));
                        return temp;
                    });
        }catch(BadSqlGrammarException e){
            System.out.println("Error: EmpleadoDAO en obtenerHaberBasico, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            emp= new Empleado();
        }
        return emp;
    }




    /**
     * Procedimiento para obtener los prestamos, anticipos y multas de un empleado
     *
     * @param mes
     * @param anio
     * @param codEmpleado
     * @return List<DescuentoEmpleadoDTO>
     */
    public List<DescuentoEmpleadoDTO> obtenerPrestamosAnticiposYMultasEmpleado(int mes, int anio, int codEmpleado ) {

        List<DescuentoEmpleadoDTO> lstTemp = new ArrayList<>();
        try {
            lstTemp =  this.jdbcTemplate.query("exec p_list_Empleado @codEmpleado = ?, @mes = ?, @anio = ?, @ACCION = ?",
                    new Object[] { codEmpleado, mes, anio, "A1" },
                    new int[] { Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR },
                    (rs, rowNum) -> {
                        DescuentoEmpleadoDTO temp = new DescuentoEmpleadoDTO();

                        temp.setCodEmpleado(rs.getInt(1));
                        temp.setDescripcion(rs.getString(2));
                        temp.setMoneda(rs.getString(3));
                        temp.setMontoTotal(rs.getFloat(4));
                        temp.setTotalCuotas(rs.getInt(5));
                        temp.setPeriodo(rs.getString(6));
                        temp.setTipoDescuento(rs.getString(7));
                        temp.setEstadoDescuento(rs.getString(8));
                        temp.setPrimeraCuotaMes(rs.getInt(9));
                        temp.setUltimaCuotaMes(rs.getInt(10));
                        temp.setMontoDescuento(rs.getFloat(11));
                        temp.setSaldoRestante(rs.getFloat(12));

                        return temp;
                    });

        }  catch (BadSqlGrammarException e) {
            System.out.println("Error: EmpleadoDAO en obtenerPrestamosAnticiposYMultasEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null; // Cuidado con esto, anular el jdbcTemplate puede romper futuros llamados a este DAO
        }

        return lstTemp;
    }


}
