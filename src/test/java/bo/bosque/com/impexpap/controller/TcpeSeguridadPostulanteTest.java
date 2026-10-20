package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.SesionService;
import bo.bosque.com.impexpap.commons.TcpeCatalogo;
import bo.bosque.com.impexpap.commons.TcpeClaveService;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.JacksonConfig;
import bo.bosque.com.impexpap.dao.ITcpeAcceso;
import bo.bosque.com.impexpap.dao.ITcpeConfig;
import bo.bosque.com.impexpap.dao.ITcpeRendir;
import bo.bosque.com.impexpap.dao.LoginDaoImpl;
import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeConfigDto;
import bo.bosque.com.impexpap.security.ClienteIp;
import bo.bosque.com.impexpap.security.jwt.JwtEntryPoint;
import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import bo.bosque.com.impexpap.security.jwt.UsuarioToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cadena de seguridad <b>real</b> ({@code MainSecurity} + {@code JwtTokenFilter}, solo los datos mockeados): el token
 * del postulante no sale de {@code /examen-publico/**}, el de empleado no entra ahí y el filtro no busca al
 * postulante en {@code tb_usuario}.
 */
@WebMvcTest(controllers = { TcpeCatalogoBancoController.class, TcpeExamenPublicoController.class,
        TcpeExamenPublicoAuthController.class, TcpeConfigController.class })
@Import({ JwtProvider.class, ClienteIp.class, JwtEntryPoint.class, JacksonConfig.class })
@TestPropertySource(properties = {
        "jwt.secret=" + TcpeSeguridadPostulanteTest.SECRETO,
        "jwt.expiration=3600" })
class TcpeSeguridadPostulanteTest {

    /** 72 bytes en Base64: alcanza para HS512. Solo para la prueba. */
    static final String SECRETO =
            "dGNwZS1wcnVlYmEtc2VjcmV0by1qd3QtdGNwZS1wcnVlYmEtc2VjcmV0by1qd3QtdGNwZS1wcnVlYmEtc2VjcmV0by1qd3Qt";

    private static final long POSTULANTE = 42L;

    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider jwt;

    // Seguridad: MainSecurity pide LoginDaoImpl, y el filtro JWT, SesionService.
    @MockBean private LoginDaoImpl loginDao;
    @MockBean private SesionService sesiones;

    // Lo que piden los controladores del slice.
    @MockBean private TcpeCatalogo catalogo;
    @MockBean private TcpePermisos permisos;
    @MockBean private TcpeRendicion rendicion;
    @MockBean private ITcpeAcceso acceso;
    @MockBean private ITcpeRendir rendir;
    @MockBean private ITcpeConfig config;
    @MockBean private TcpeClaveService claves;

    private String tokenPostulante() {
        return "Bearer " + jwt.generarTokenPostulante(POSTULANTE, "IPX-1", jwt.expiracionDesdeAhora());
    }

    /** Un token de empleado como el que emite LoginController (sin el claim tipo). */
    private static String tokenEmpleado() {
        return "Bearer " + Jwts.builder()
                .setSubject("jperez")
                .setId("7")
                .claim("codEmpleado", 3)
                .claim("codEmpresa", 1)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(SignatureAlgorithm.HS512, Base64.getDecoder().decode(SECRETO))
                .compact();
    }

    @Test
    @DisplayName("token de postulante en una ruta de empleado del módulo: 403 y no se toca la base")
    void postulanteNoEntraAlCatalogo() throws Exception {
        mvc.perform(post("/examenes/catalogo/area/listar")
                        .header("Authorization", tokenPostulante())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(catalogo, permisos);
        // El filtro no fue a buscarlo a tb_usuario.
        verify(loginDao, never()).cargarParaToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("token de postulante en una ruta existente fuera del módulo: 403")
    void postulanteNoEntraAOtrosModulos() throws Exception {
        mvc.perform(post("/pagos-extranjeros/obtener-solicitudes")
                        .header("Authorization", tokenPostulante())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/rrhh/eliminarFoto")
                        .header("Authorization", tokenPostulante()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token de postulante en /examen-publico: entra y el codUsuario sale del token")
    void postulanteEntraASusExamenes() throws Exception {
        when(rendicion.listar(POSTULANTE)).thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>("No tiene exámenes asignados.", null, 204)));

        mvc.perform(post("/examen-publico/examenes/listar")
                        .header("Authorization", tokenPostulante()))
                .andExpect(status().isNoContent());

        verify(rendicion).listar(POSTULANTE);
        verify(loginDao, never()).cargarParaToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("revisión del postulante: el codUsuario del cuerpo se ignora y va el del token")
    void revisionConCodUsuarioDelToken() throws Exception {
        when(rendicion.revision(eq(POSTULANTE), any())).thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApiResponse<>("Sin revisión.", null, 204)));

        mvc.perform(post("/examen-publico/examenes/revision")
                        .header("Authorization", tokenPostulante())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codExamen\":10,\"codUsuario\":999}"))
                .andExpect(status().isNoContent());

        verify(rendicion).revision(eq(POSTULANTE), any());
        verify(rendicion, never()).revision(eq(999L), any());
    }

    @Test
    @DisplayName("config pública sin token: 200 con la lista de p_list_tcpe_Config; la de empleados sin token, 401")
    void configPublicaSinToken() throws Exception {
        TcpeConfigDto c = new TcpeConfigDto();
        c.setClave("ayudaIngreso");
        c.setValor("Escriba el usuario y la clave que le entregaron.");
        when(config.listar()).thenReturn(Collections.singletonList(c));

        mvc.perform(post("/examen-publico/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].clave").value("ayudaIngreso"));
        mvc.perform(post("/examenes/config"))
                .andExpect(status().isUnauthorized());
        verify(loginDao, never()).cargarParaToken(anyString(), anyLong());
    }

    @Test
    @DisplayName("latido del postulante: el codUsuario del cuerpo se ignora y va el del token")
    void latidoConCodUsuarioDelToken() throws Exception {
        when(rendicion.latido(eq(POSTULANTE), any())).thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("ok", 600L, 201)));

        mvc.perform(post("/examen-publico/examenes/latido")
                        .header("Authorization", tokenPostulante())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codExamen\":10,\"codPreguntaH\":5,\"salioPestana\":true,\"codUsuario\":999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value(600));

        verify(rendicion).latido(eq(POSTULANTE), any());
        verify(rendicion, never()).latido(eq(999L), any());
    }

    @Test
    @DisplayName("token de empleado en /examen-publico: 403")
    void empleadoNoEntraAlExamenPublico() throws Exception {
        when(loginDao.cargarParaToken(eq("jperez"), anyLong())).thenReturn(new UsuarioToken(
                "jperez", "", true, true, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")),
                false, null));

        mvc.perform(post("/examen-publico/examenes/listar")
                        .header("Authorization", tokenEmpleado()))
                .andExpect(status().isForbidden());

        verify(rendicion, never()).listar(anyLong());
    }

    @Test
    @DisplayName("sin token, /examen-publico/examenes es 401 y el login es público")
    void loginPublicoYRestoCerrado() throws Exception {
        mvc.perform(post("/examen-publico/examenes/listar"))
                .andExpect(status().isUnauthorized());

        when(acceso.porLogin("IPX-1")).thenReturn(null);
        mvc.perform(post("/examen-publico/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"IPX-1\",\"password2\":\"xxxxxxxxxx\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(TcpeExamenPublicoAuthController.MSG_CREDENCIALES));

        verify(claves, never()).hash(any());
    }

    @Test
    @DisplayName("login: solo entra el tipo 2 (externo); con la clave correcta devuelve un token que sirve en /examen-publico")
    void loginSoloExternos() throws Exception {
        TcpeAccesoDto u = new TcpeAccesoDto();
        u.setCodUsuario(POSTULANTE);
        u.setLogin("IPX-1");
        u.setPassword2("$2a$14$" + String.join("", Collections.nCopies(53, "a")));   // ya es hash: sin migración
        u.setActivo(true);
        u.setDatosCompletos(false);
        u.setPuedeIngresar(true);
        u.setCodTipoUsuario(1L);
        when(acceso.porLogin("IPX-1")).thenReturn(u);
        when(claves.coincide(eq("Abc2345xyz"), any())).thenReturn(true);
        String cuerpo = "{\"login\":\"IPX-1\",\"password2\":\"Abc2345xyz\"}";

        // p_list_tcpe_Acceso no filtra por tipo: otro tipo no entra, aunque la clave coincida.
        mvc.perform(post("/examen-publico/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnauthorized());

        u.setCodTipoUsuario(2L);
        String respuesta = mvc.perform(post("/examen-publico/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codUsuario").value((int) POSTULANTE))
                .andExpect(jsonPath("$.data.datosCompletos").value(false))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.password2").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        verify(acceso, never()).guardarHash(anyLong(), any(), any());

        String token = com.jayway.jsonpath.JsonPath.read(respuesta, "$.data.token");
        when(rendicion.listar(POSTULANTE)).thenReturn(ResponseEntity.ok(new ApiResponse<>("ok", null, 200)));
        mvc.perform(post("/examen-publico/examenes/listar").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
