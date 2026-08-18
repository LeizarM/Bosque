package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.IProgramador;
import bo.bosque.com.impexpap.dao.IUsuarioBtn;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.model.UsuarioBtn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de {@link AccesoModuloHelper#exigirBoton(Authentication, int, String)}.
 *
 * <p>Es una puerta de seguridad, así que los dos errores posibles no cuestan lo mismo: dejar
 * pasar a quien no debe es un agujero; frenar a quien sí puede es una molestia. Por eso la
 * mayoría de los casos de acá son "NO tiene que pasar", incluido el que importa de verdad —
 * <b>el ACL caído no abre la puerta</b>.
 *
 * <p>Se usa un {@code UsernamePasswordAuthenticationToken} de verdad y no un mock de
 * {@code Authentication}: es lo mismo que arma la cadena de seguridad y no hay que pelearse
 * con el genérico de {@code getAuthorities()}.
 */
class AccesoModuloHelperTest {

    private static final int VISTA_PERMISOS = 24;

    private IProgramador programadorDao;
    private IUsuarioBtn  usuarioBtnDao;
    private AccesoModuloHelper acceso;

    @BeforeEach
    void setUp() {
        programadorDao = mock(IProgramador.class);
        usuarioBtnDao  = mock(IUsuarioBtn.class);
        acceso = new AccesoModuloHelper(programadorDao, usuarioBtnDao);
    }

    // ============================================================
    // Lo que SÍ pasa
    // ============================================================

    @Test
    @DisplayName("ROLE_ADM entra sin consultar el ACL — el fallback de Loggin.autorizarBtn()")
    void adminEntraSiempre() {
        assertDoesNotThrow(() ->
                acceso.exigirBoton(tokenDe("msistemas", "ROLE_ADM"), VISTA_PERMISOS, "btnDetalles"));

        // Y sin pagar las dos consultas: es la unica razon por la que el admin va primero.
        verify(programadorDao, never()).resolverPermiso(anyString());
        verify(usuarioBtnDao,  never()).botonesXUsuario(anyInt());
    }

    @Test
    @DisplayName("ROLE_LIM con el boton habilitado entra — es el caso de 'rramos'")
    void limConElBotonEntra() {
        dado(77L, boton("btnDetalles", 1, VISTA_PERMISOS));

        assertDoesNotThrow(() ->
                acceso.exigirBoton(tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    @Test
    @DisplayName("el boton duplicado en tb_vistaBtn no rompe: alcanza con que UNA fila lo habilite")
    void botonDuplicado() {
        // btnImprimirSolicitud existe con codBtn 111 y 115: el lookup por nombre da 2 filas.
        dado(77L, boton("btnImprimirSolicitud", 0, VISTA_PERMISOS),
                  boton("btnImprimirSolicitud", 1, VISTA_PERMISOS));

        assertDoesNotThrow(() -> acceso.exigirBoton(
                tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnImprimirSolicitud"));
    }

    // ============================================================
    // Lo que NO pasa
    // ============================================================

    @Test
    @DisplayName("sin el boton -> 403")
    void sinElBoton() {
        dado(77L, boton("btnOtraCosa", 1, VISTA_PERMISOS));

        assertThrows(AccessDeniedException.class, () ->
                acceso.exigirBoton(tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    @Test
    @DisplayName("el boton correcto pero de OTRA vista no habilita esta")
    void botonDeOtraVista() {
        dado(77L, boton("btnDetalles", 1, 31));

        assertThrows(AccessDeniedException.class, () ->
                acceso.exigirBoton(tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    @Test
    @DisplayName("nivelAcceso 0 no es tener el boton")
    void nivelAccesoCero() {
        dado(77L, boton("btnDetalles", 0, VISTA_PERMISOS));

        assertThrows(AccessDeniedException.class, () ->
                acceso.exigirBoton(tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    @Test
    @DisplayName("EL QUE IMPORTA: con el ACL caido (lista vacia) falla CERRADO, no abierto")
    void aclCaidoNiegaElAcceso() {
        // UsuarioBtnDao se anula el JdbcTemplate en su catch: si alguna vez falla, devuelve
        // lista vacia para siempre. Eso tiene que verse como un 403 masivo, no como barra libre.
        dado(77L);

        assertThrows(AccessDeniedException.class, () ->
                acceso.exigirBoton(tokenDe("rramos", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    @Test
    @DisplayName("un login que no resuelve a ningun codUsuario -> 403")
    void loginSinUsuario() {
        MiEquipoDto sinUsuario = new MiEquipoDto();
        sinUsuario.setCodUsuario(null);
        when(programadorDao.resolverPermiso("fantasma")).thenReturn(sinUsuario);

        assertThrows(AccessDeniedException.class, () ->
                acceso.exigirBoton(tokenDe("fantasma", "ROLE_LIM"), VISTA_PERMISOS, "btnDetalles"));
    }

    // ============================================================
    // Andamiaje
    // ============================================================

    private Authentication tokenDe(String login, String rol) {
        return new UsernamePasswordAuthenticationToken(
                login, "n/a", AuthorityUtils.createAuthorityList(rol));
    }

    /** Deja al login 'rramos'/'fantasma' resuelto a {@code codUsuario} y con esos botones. */
    private void dado(long codUsuario, UsuarioBtn... botones) {
        MiEquipoDto yo = new MiEquipoDto();
        yo.setCodUsuario(codUsuario);
        when(programadorDao.resolverPermiso(anyString())).thenReturn(yo);

        List<UsuarioBtn> lst = botones.length == 0
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(botones));
        when(usuarioBtnDao.botonesXUsuario((int) codUsuario)).thenReturn(lst);
    }

    /** Una fila de {@code p_list_UsuarioBtn @ACCION='A'}: (boton, permiso, codVista). */
    private UsuarioBtn boton(String nombre, int permiso, int codVista) {
        UsuarioBtn b = new UsuarioBtn();
        b.setBoton(nombre);
        b.setPermiso(permiso);
        b.setPertenVist(codVista);
        return b;
    }
}
