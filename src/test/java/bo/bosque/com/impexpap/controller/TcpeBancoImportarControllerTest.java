package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.commons.TcpePermisos;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.dao.TcpeBancoDao;
import bo.bosque.com.impexpap.dto.TcpeBancoDuplicadoDto;
import bo.bosque.com.impexpap.dto.TcpeBancoDuplicadosFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeBancoLoteFilaDto;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Banco v6 (contrato 9.2 y 9.3): cada endpoint llama a su SP y acción, con el XML armado y el codUsuario del token
 * (34). El DAO es el real sobre un {@code SpHelper} simulado; el permiso es el de las demás rutas del banco.
 */
class TcpeBancoImportarControllerTest {

    private static final String MENSAJE = "Se importaron 3 preguntas; 1 se omitió por estar duplicada o tener errores.";

    private IVistaDao vistas;
    private SpHelper sp;
    private TcpeBancoController controlador;

    @BeforeEach
    void preparar() {
        vistas = mock(IVistaDao.class);
        sp = mock(SpHelper.class);
        controlador = new TcpeBancoController(new TcpeBancoDao(sp),
                new TcpePermisos(vistas, new AccesoModuloHelper(null, null)));
    }

    private static Authentication usuario(String rol) {
        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "evaluador", null, Collections.singletonList(new SimpleGrantedAuthority(rol)));
        t.setDetails(new DatosToken(34, 1, 1, rol));
        return t;
    }

    private void conPantalla(String direccion) {
        when(vistas.obtainRoutes(34)).thenReturn(Collections.singletonList(new Vista().setDireccion(direccion)));
    }

    private static TcpeBancoImportarDto envio(int cantidad) {
        List<TcpeBancoImportarPreguntaDto> ps = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            TcpeBancoImportarPreguntaDto p = new TcpeBancoImportarPreguntaDto();
            p.setPregunta("Pregunta " + i);
            p.setEsCerrada(false);
            ps.add(p);
        }
        TcpeBancoImportarDto d = new TcpeBancoImportarDto();
        d.setCodClasificacion(4L);
        d.setPreguntas(ps);
        return d;
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Object>> parametros() {
        return ArgumentCaptor.forClass(Map.class);
    }

    @Test
    @DisplayName("duplicados: p_list_tcpe_Banco D con categoría y texto")
    void duplicados() {
        when(sp.ejecutarListado(anyString(), anyMap(), anyString(), eq(TcpeBancoDuplicadoDto.class)))
                .thenReturn(Collections.singletonList(new TcpeBancoDuplicadoDto()));
        TcpeBancoDuplicadosFiltroDto f = new TcpeBancoDuplicadosFiltroDto();
        f.setCodClasificacion(4L);
        f.setTexto("¿Capital de Bolivia?");

        assertEquals(200, controlador.duplicados(f, usuario("ROLE_ADM")).getStatusCodeValue());

        ArgumentCaptor<Map<String, Object>> p = parametros();
        verify(sp).ejecutarListado(eq("p_list_tcpe_Banco"), p.capture(), eq("D"), eq(TcpeBancoDuplicadoDto.class));
        assertEquals(4L, p.getValue().get("codClasificacion"));
        assertEquals("¿Capital de Bolivia?", p.getValue().get("texto"));
    }

    @Test
    @DisplayName("duplicados sin coincidencias: 204")
    void duplicadosSinFilas() {
        when(sp.ejecutarListado(anyString(), anyMap(), anyString(), eq(TcpeBancoDuplicadoDto.class)))
                .thenReturn(Collections.emptyList());
        assertEquals(204, controlador.duplicados(new TcpeBancoDuplicadosFiltroDto(), usuario("ROLE_ADM"))
                .getStatusCodeValue());
    }

    @Test
    @DisplayName("validar: p_list_tcpe_BancoLote V con la categoría y el XML; sin audUsuario y sin escribir")
    void validar() {
        TcpeBancoLoteFilaDto fila = new TcpeBancoLoteFilaDto();
        fila.setFila(1);
        fila.setEstado("OK");
        when(sp.ejecutarListado(anyString(), anyMap(), anyString(), eq(TcpeBancoLoteFilaDto.class)))
                .thenReturn(Collections.singletonList(fila));
        TcpeBancoImportarDto d = envio(2);

        ResponseEntity<ApiResponse<?>> r = controlador.validarImportacion(d, usuario("ROLE_ADM"));

        assertEquals(200, r.getStatusCodeValue());
        assertEquals(Collections.singletonList(fila), r.getBody().getData());
        ArgumentCaptor<Map<String, Object>> p = parametros();
        verify(sp).ejecutarListado(eq("p_list_tcpe_BancoLote"), p.capture(), eq("V"), eq(TcpeBancoLoteFilaDto.class));
        assertEquals(4L, p.getValue().get("codClasificacion"));
        assertEquals(TcpeBancoDao.armarPreguntasXml(d.getPreguntas()), p.getValue().get("preguntas"));
        assertFalse(p.getValue().containsKey("audUsuario"), "V no recibe audUsuario");
        verify(sp, never()).ejecutarAbmMap(anyString(), anyMap(), anyString());
    }

    @Test
    @DisplayName("importar: p_abm_tcpe_BancoLote I con el codUsuario del token; 201 con la cantidad y el mensaje del SP")
    void importar() {
        conPantalla(TcpePermisos.BANCO);
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, MENSAJE, 3L));
        TcpeBancoImportarDto d = envio(4);

        ResponseEntity<ApiResponse<?>> r = controlador.importar(d, usuario("ROLE_LIM"));

        assertEquals(201, r.getStatusCodeValue());
        assertEquals(3L, r.getBody().getData());
        assertEquals(MENSAJE, r.getBody().getMessage());
        ArgumentCaptor<Map<String, Object>> p = parametros();
        verify(sp).ejecutarAbmMap(eq("p_abm_tcpe_BancoLote"), p.capture(), eq("I"));
        assertEquals(34L, p.getValue().get("audUsuario"));
        assertEquals(4L, p.getValue().get("codClasificacion"));
        assertEquals(TcpeBancoDao.armarPreguntasXml(d.getPreguntas()), p.getValue().get("preguntas"));
    }

    @Test
    @DisplayName("sin la pantalla del banco: 403 y no se llama a ningún SP")
    void sinPantallaBanco() {
        conPantalla(TcpePermisos.LOTES);
        Authentication u = usuario("ROLE_LIM");
        assertThrows(SinPermisoException.class, () -> controlador.duplicados(new TcpeBancoDuplicadosFiltroDto(), u));
        assertThrows(SinPermisoException.class, () -> controlador.validarImportacion(envio(1), u));
        assertThrows(SinPermisoException.class, () -> controlador.importar(envio(1), u));
        verifyNoInteractions(sp);
    }

    @Test
    @DisplayName("tope técnico del envío: 500 preguntas pasan, 501 no; el tope de negocio lo pone el SP")
    void topeTecnico() {
        try (ValidatorFactory fabrica = Validation.buildDefaultValidatorFactory()) {
            Validator v = fabrica.getValidator();
            assertTrue(v.validate(envio(TcpeBancoImportarDto.MAX_PREGUNTAS)).isEmpty());
            assertEquals(1, v.validate(envio(TcpeBancoImportarDto.MAX_PREGUNTAS + 1)).size());
            assertTrue(v.validate(new TcpeBancoImportarDto()).isEmpty(), "lo vacío lo responde el SP");
        }
    }
}
