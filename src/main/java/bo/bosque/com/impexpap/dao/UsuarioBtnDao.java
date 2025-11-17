package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SocioNegocio;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Repository
public class UsuarioBtnDao implements  IUsuarioBtn {


    /**
     * El Datasource
     */
    @Autowired
    private JdbcTemplate jdbcTemplate;


    /**
     * Obtendra los permisos por botones por usuario
     * @param codUsuario
     * @return List
     */
    @Override
    public List<UsuarioBtn> botonesXUsuario(int codUsuario) {


        List<UsuarioBtn> lstTemp =  new ArrayList<>();

        try{
            lstTemp = this.jdbcTemplate.query("execute dbo.p_list_UsuarioBtn @codUsuario=?, @ACCION=?",
                    new Object[]{ codUsuario, "A" },
                    new int[]{ Types.INTEGER, Types.VARCHAR },
                    (rs, rowCount) ->{
                        UsuarioBtn temp = new UsuarioBtn();

                        temp.setBoton( rs.getString(1) );
                        temp.setPermiso(rs.getInt(2) );
                        temp.setPertenVist( rs.getInt(3) );


                        return temp;
                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: UsuarioBtnDao en botonesXUsuario, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            lstTemp = new ArrayList<>();
            this.jdbcTemplate = null;
        }
        return lstTemp;



    }

    /**
     * Para registrar un botón en la base de datos
     *
     * @param usuarioBtn
     * @param acc
     * @return
     */
    @Override
    public boolean registroBoton(UsuarioBtn usuarioBtn, String acc) {



        int resp;

        try{
            resp = this.jdbcTemplate.update("execute p_abm_UsuarioBtn  @codUsuario = ?, @codBtn = ?, @nivelAcceso = ?, @audUsuario = ?, @ACCION = ?",
                    ps -> {

                        ps.setInt(1, usuarioBtn.getCodUsuario());
                        ps.setInt(2, usuarioBtn.getCodBtn());
                        ps.setInt(3, usuarioBtn.getNivelAcceso());
                        ps.setInt(4, usuarioBtn.getAudUsuario());
                        ps.setString(5, acc);

                    });

        }catch ( BadSqlGrammarException e){
            System.out.println("Error: UsuarioBtnDao en registroBoton, DataAccessException->" + e.getMessage() + ",SQL Code->" + ((SQLException) e.getCause()).getErrorCode());
            this.jdbcTemplate = null;
            resp = 0;
        }

        return resp!=0;


    }
}
