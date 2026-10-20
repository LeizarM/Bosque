package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcpePersonaPost;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

/**
 * {@code tcpe_personaPost} vía {@code p_list_tcpe_PersonaPost} y {@code p_abm_tcpe_PersonaPost}.
 */
@Repository
public class TcpePersonaPostDao extends TcpeCrudDao<TcpePersonaPost> implements ITcpePersonaPost {

    /** Parámetros de p_list (PK, FK); NULL = sin filtro. */
    private static final String[] FILTROS = { "codPersonaPost" };

    /** Columnas de datos de p_abm, sin PK, audUsuario, audFecha ni password2. */
    private static final String[] COLUMNAS = { "ci", "nombre", "apPaterno", "apMaterno", "email", "ciudad", "pais", "fechaNacimiento" };

    public TcpePersonaPostDao(SpHelper spHelper) {
        super(spHelper, TcpePersonaPost.class, "personaPost", "codPersonaPost", true, FILTROS, COLUMNAS);
    }
}
