package bo.bosque.com.impexpap.dao;
import bo.bosque.com.impexpap.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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

                        temp.getEmpleadoCargo().setCodCargoSucursal(rs.getInt(15));
                        temp.getEmpleadoCargo().setCodCargoSucPlanilla(rs.getInt(16));

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
             resp = this.jdbcTemplate.update("execute p_abm_empleado  @codEmpleado=?, @codPersona=?, @numCuenta=?, @codRelBeneficios=?, @codRelPlanilla=?, @audUsuarioI=?, @ACCION=?",
                     ps->{
                        ps.setInt (1, emp.getCodEmpleado() );
                        ps.setInt( 2, emp.getCodPersona() );
                        ps.setString( 3, emp.getNumCuenta() );
                        ps.setInt( 4, emp.getCodRelBeneficios() );
                        ps.setInt( 5, emp.getCodRelPlanilla() );
                        ps.setInt( 6, emp.getAudUsuarioI());
                        ps.setString( 7, acc  );

                     });

         }catch ( BadSqlGrammarException e ){
             System.out.println("Error: EmpleadoDAO en registroEmpleado, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
             this.jdbcTemplate = null;
             resp = 0;
         }

        return resp!=0;
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
    public List<Empleado> obtenerListaEmpleadoyDependientes() {
        List<Empleado> lstTemp;
        try{
        lstTemp = this.jdbcTemplate.query(
                "execute p_list_Empleado @ACCION=?",
                new Object[]{"P"},
                new int[]{Types.VARCHAR},
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

}
