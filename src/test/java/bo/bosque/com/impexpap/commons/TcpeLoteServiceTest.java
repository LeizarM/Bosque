package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.ITcpeGenerarExamen;
import bo.bosque.com.impexpap.dao.ITcpeLote;
import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dto.TcpeCredencialDto;
import bo.bosque.com.impexpap.dto.TcpeLoteAgregarDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGeneradoDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
import bo.bosque.com.impexpap.dto.TcpePersonaExamenDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoExamenDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Generación de lotes (contratos 7.2 y 10): {@code Lote 'V'} valida con los tres conteos antes de hashear y antes de
 * {@code I}; el lote mixto, los límites y sus mensajes son del SP. Aquí solo se prueba la orquestación y las credenciales.
 */
class TcpeLoteServiceTest {

    private ITcpeLote lotes;
    private ITcpeGenerarExamen generador;
    private ITcpeResultado resultados;
    private TcpeClaveService claves;
    private TcpeLoteService servicio;

    @BeforeEach
    void preparar() {
        lotes = mock(ITcpeLote.class);
        generador = mock(ITcpeGenerarExamen.class);
        resultados = mock(ITcpeResultado.class);
        claves = mock(TcpeClaveService.class);
        servicio = new TcpeLoteService(lotes, generador, resultados, claves);
        when(lotes.crear(any(), eq(7L))).thenReturn(new RespuestaSp(0, "", 30L));
    }

    private static TcpeResultadoExamenDto fila(long codExamen, String login, Long codEmpleado) {
        TcpeResultadoExamenDto r = new TcpeResultadoExamenDto();
        r.setCodExamen(codExamen);
        r.setLogin(login);
        r.setCodEmpleado(codEmpleado);
        return r;
    }

    private static TcpeResultadoExamenDto fila(long codExamen, String login, String ci, String nombre) {
        TcpeResultadoExamenDto r = fila(codExamen, login, null);
        r.setCi(ci);
        r.setNombreCompleto(nombre);
        return r;
    }

    private static TcpePersonaExamenDto persona(String ci, String nombres, String paterno, String materno) {
        return new TcpePersonaExamenDto(ci, nombres, paterno, materno);
    }

    /** Body de generar: un lote nuevo con su modalidad y lo que llegue de cada modo. */
    private static TcpeLoteGuardarDto nuevo(String modalidad, Integer cantidad, List<TcpePersonaExamenDto> personas,
                                            List<Long> empleados) {
        TcpeLoteGuardarDto d = new TcpeLoteGuardarDto();
        d.setModalidad(modalidad);
        d.setCantidadPreg(10);
        d.setCodClasificacion(4L);
        d.setCantidad(cantidad);
        d.setPersonas(personas);
        d.setCodEmpleados(empleados);
        return d;
    }

    /** Body de agregar al lote 30, que ya existe con [modalidad]. */
    private TcpeLoteAgregarDto agregado(String modalidad, Integer cantidad, List<TcpePersonaExamenDto> personas,
                                        List<Long> empleados) {
        TcpeLoteDto l = new TcpeLoteDto();
        l.setCodRolExamen(30L);
        l.setModalidad(modalidad);
        when(lotes.listar(any())).thenReturn(Collections.singletonList(l));
        TcpeLoteAgregarDto d = new TcpeLoteAgregarDto();
        d.setCodRolExamen(30L);
        d.setCantidad(cantidad);
        d.setPersonas(personas);
        d.setCodEmpleados(empleados);
        return d;
    }

    // ── V antes de todo ───────────────────────────────────────────────────

    @Test
    @DisplayName("generar: Lote V con el lote nuevo y los tres conteos, antes de hashear y antes de I")
    void validaAntesDeHashearYDeCrear() {
        when(claves.generarConHash(2)).thenReturn(Arrays.asList(
                new TcpeClaveService.Clave("claveA", "hashA"), new TcpeClaveService.Clave("claveB", "hashB")));
        when(generador.generar(eq(30L), isNull(), anyString(), eq(7L))).thenReturn(101L, 102L);
        when(resultados.listar(30L)).thenReturn(Collections.<TcpeResultadoExamenDto>emptyList());

        TcpeLoteGuardarDto dto = nuevo("O", 2, null, null);
        servicio.generar(dto, servicio.pedido(dto), 7L);

        InOrder orden = inOrder(lotes, claves, generador);
        orden.verify(lotes).validar(dto, 2, 0, 0);
        orden.verify(claves).generarConHash(2);
        orden.verify(lotes).crear(any(), eq(7L));
        orden.verify(generador, times(2)).generar(eq(30L), isNull(), anyString(), eq(7L));
    }

    @Test
    @DisplayName("agregar: Lote V con el codRolExamen (el SP suma lo que ya tiene) antes de leer el lote y hashear")
    void agregarValidaConElLote() {
        when(claves.generarConHash(1)).thenReturn(
                Collections.singletonList(new TcpeClaveService.Clave("claveA", "hashA")));
        TcpeLoteAgregarDto dto = agregado("O", 1, null, Collections.singletonList(55L));

        servicio.pedido(dto);

        InOrder orden = inOrder(lotes, claves);
        orden.verify(lotes).validarAgregar(30L, 1, 0, 1);
        orden.verify(lotes).listar(any());
        orden.verify(claves).generarConHash(1);
        verify(lotes, never()).validar(any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("si V rechaza, su mensaje llega tal cual y no se hashea ni se escribe nada")
    void vRechaza() {
        SpBusinessException delSp = new SpBusinessException("Un lote admite hasta 200 exámenes: pediste 201.");
        when(lotes.validar(any(), anyInt(), anyInt(), anyInt())).thenThrow(delSp);
        TcpeLoteGuardarDto dto = nuevo("O", 199, Collections.singletonList(persona("1", "Ana", "Paz", null)),
                Collections.singletonList(9L));

        SpBusinessException e = assertThrows(SpBusinessException.class, () -> servicio.pedido(dto));

        assertSame(delSp, e);
        verify(lotes).validar(dto, 199, 1, 1);
        verifyNoInteractions(claves, generador);
        verify(lotes, never()).crear(any(), anyLong());
    }

    // ── Lote mixto ────────────────────────────────────────────────────────

    @Test
    @DisplayName("lote mixto en línea: un solo lote de hash para los externos; credenciales solo de los externos")
    void mixtoEnLinea() {
        when(claves.generarConHash(3)).thenReturn(Arrays.asList(new TcpeClaveService.Clave("claveA", "hashA"),
                new TcpeClaveService.Clave("claveB", "hashB"), new TcpeClaveService.Clave("Azar234567", "hashP")));
        when(generador.generar(30L, 55L, null, 7L)).thenReturn(201L);
        when(generador.generar(30L, 56L, null, 7L)).thenReturn(202L);
        when(generador.generar(30L, null, "hashA", 7L)).thenReturn(101L);
        when(generador.generar(30L, null, "hashB", 7L)).thenReturn(102L);
        when(generador.generarParaPersona(eq(30L), any(), eq("hashP"), eq(7L))).thenReturn(401L);
        when(resultados.listar(30L)).thenReturn(Arrays.asList(fila(201L, "jperez", 55L), fila(202L, "lrojas", 56L),
                fila(101L, "IPX-1", null), fila(102L, "IPX-2", null), fila(401L, "IPX-3", "1234567", "Paz Ana")));

        TcpeLoteGuardarDto dto = nuevo("O", 2, Collections.singletonList(persona("1234567", "Ana", "Paz", null)),
                Arrays.asList(55L, 56L));
        TcpeLoteService.Pedido pedido = servicio.pedido(dto);
        TcpeLoteGeneradoDto r = servicio.generar(dto, pedido, 7L);

        verify(lotes).validar(dto, 2, 1, 2);
        verify(claves, times(1)).generarConHash(3);
        assertEquals(5, pedido.cantidad());
        verify(generador).generar(30L, 55L, null, 7L);
        verify(generador).generar(30L, 56L, null, 7L);
        List<TcpeCredencialDto> c = r.getCredenciales();
        assertEquals(3, c.size(), "los empleados no reciben credencial");
        assertEquals("IPX-1", c.get(0).getLogin());
        assertEquals("claveA", c.get(0).getPassword());
        assertEquals("IPX-2", c.get(1).getLogin());
        assertEquals("IPX-3", c.get(2).getLogin());
        assertEquals("Azar234567", c.get(2).getPassword());
        assertEquals("1234567", c.get(2).getCi());
        for (TcpeCredencialDto x : c) assertNull(x.getCodEmpleado());
    }

    @Test
    @DisplayName("lote mixto escrito: externos y empleados sin claves ni hash y sin credenciales")
    void mixtoEscrito() {
        when(generador.generar(eq(30L), isNull(), isNull(), eq(7L))).thenReturn(301L, 302L);
        when(generador.generarParaPersona(eq(30L), any(), isNull(), eq(7L))).thenReturn(501L);
        when(generador.generar(30L, 55L, null, 7L)).thenReturn(201L);

        TcpeLoteAgregarDto dto = agregado("E", 2, Collections.singletonList(persona("99", "Eva", "Luna", null)),
                Collections.singletonList(55L));
        TcpeLoteGeneradoDto r = servicio.agregar(30L, servicio.pedido(dto), 7L);

        verify(lotes).validarAgregar(30L, 2, 1, 1);
        verify(claves, never()).generarConHash(anyInt());
        verify(generador, times(2)).generar(30L, null, null, 7L);
        verify(generador).generarParaPersona(eq(30L), any(), isNull(), eq(7L));
        verify(generador).generar(30L, 55L, null, 7L);
        verify(resultados, never()).listar(any());
        assertTrue(r.getCredenciales().isEmpty());
    }

    // ── Un solo grupo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("externos online: cada examen lleva su hash y la credencial cruza codExamen con login y clave")
    void externosOnline() {
        when(claves.generarConHash(2)).thenReturn(Arrays.asList(
                new TcpeClaveService.Clave("claveA", "hashA"), new TcpeClaveService.Clave("claveB", "hashB")));
        when(generador.generar(30L, null, "hashA", 7L)).thenReturn(101L);
        when(generador.generar(30L, null, "hashB", 7L)).thenReturn(102L);
        // El SP devuelve los exámenes en otro orden y con otros del lote: el cruce es por codExamen.
        when(resultados.listar(30L)).thenReturn(Arrays.asList(
                fila(102L, "IPX-2", null), fila(90L, "IPX-1", null), fila(101L, "IPX-3", null)));

        TcpeLoteGuardarDto dto = nuevo("O", 2, null, null);
        TcpeLoteGeneradoDto r = servicio.generar(dto, servicio.pedido(dto), 7L);

        assertEquals(30L, r.getCodRolExamen());
        List<TcpeCredencialDto> c = r.getCredenciales();
        assertEquals(2, c.size());
        assertEquals("IPX-3", c.get(0).getLogin());
        assertEquals("claveA", c.get(0).getPassword());
        assertEquals("IPX-2", c.get(1).getLogin());
        assertEquals("claveB", c.get(1).getPassword());
    }

    @Test
    @DisplayName("cantidad en un escrito: sin claves ni hash, password2 NULL y sin credenciales")
    void cantidadEscrita() {
        when(generador.generar(eq(30L), isNull(), isNull(), eq(7L))).thenReturn(301L, 302L, 303L);

        TcpeLoteGuardarDto dto = nuevo("E", 3, null, null);
        TcpeLoteGeneradoDto r = servicio.generar(dto, servicio.pedido(dto), 7L);

        verify(claves, never()).generarConHash(anyInt());
        verify(generador, times(3)).generar(30L, null, null, 7L);
        verify(resultados, never()).listar(any());
        assertTrue(r.getCredenciales().isEmpty());
    }

    @Test
    @DisplayName("solo empleados en línea: sin clave, un examen por empleado y sin credenciales")
    void empleados() {
        when(generador.generar(eq(30L), eq(55L), isNull(), anyLong())).thenReturn(201L);

        TcpeLoteGeneradoDto r = servicio.agregar(30L,
                servicio.pedido(agregado("O", null, null, Collections.singletonList(55L))), 7L);

        verify(lotes).validarAgregar(30L, 0, 0, 1);
        verify(generador).generar(30L, 55L, null, 7L);
        verify(claves, never()).generarConHash(anyInt());
        verify(resultados, never()).listar(any());
        assertTrue(r.getCredenciales().isEmpty());
    }

    // ── Personas (externos con CI) ────────────────────────────────────────

    @Test
    @DisplayName("personas en línea: clave temporal al azar (nunca el CI) y la credencial trae ci y nombre")
    void personasEnLinea() {
        when(claves.generarConHash(2)).thenReturn(Arrays.asList(
                new TcpeClaveService.Clave("Azar234567", "hashA"), new TcpeClaveService.Clave("Otra345678", "hashB")));
        when(generador.generarParaPersona(eq(30L), any(), anyString(), eq(7L))).thenReturn(401L, 402L);
        when(resultados.listar(30L)).thenReturn(Arrays.asList(
                fila(402L, "IPX-9", "7654321", "Rios Vega Luis"), fila(401L, "IPX-4", "1234567", "Paz Mena Ana")));

        TcpeLoteGuardarDto dto = nuevo("O", null,
                Arrays.asList(persona("1234567", "Ana", "Paz", "Mena"), persona("7654321", "Luis", "Rios", null)), null);
        TcpeLoteGeneradoDto r = servicio.generar(dto, servicio.pedido(dto), 7L);

        verify(lotes).validar(dto, 0, 2, 0);
        ArgumentCaptor<TcpePersonaExamenDto> enviada = ArgumentCaptor.forClass(TcpePersonaExamenDto.class);
        verify(generador).generarParaPersona(eq(30L), enviada.capture(), eq("hashA"), eq(7L));
        assertEquals("1234567", enviada.getValue().getCi());

        List<TcpeCredencialDto> c = r.getCredenciales();
        assertEquals(2, c.size());
        assertEquals("IPX-4", c.get(0).getLogin());
        assertEquals("Azar234567", c.get(0).getPassword());
        assertEquals("1234567", c.get(0).getCi());
        assertEquals("Paz Mena Ana", c.get(0).getNombreCompleto());
        assertEquals("Otra345678", c.get(1).getPassword());
    }

    @Test
    @DisplayName("personas en un escrito: se generan sin clave ni hash y sin credenciales (contrato 7.7)")
    void personasEscritas() {
        when(generador.generarParaPersona(eq(30L), any(), isNull(), eq(7L))).thenReturn(501L);

        TcpeLoteGeneradoDto r = servicio.agregar(30L, servicio.pedido(agregado("E", null,
                Collections.singletonList(persona("99", "Eva", "Luna", null)), null)), 7L);

        verify(lotes).validarAgregar(30L, 0, 1, 0);
        verify(claves, never()).generarConHash(anyInt());
        verify(generador).generarParaPersona(eq(30L), any(), isNull(), eq(7L));
        assertTrue(r.getCredenciales().isEmpty());
    }

    @Test
    @DisplayName("personas sin fila en el listado: el nombre y el CI salen del pedido")
    void personasSinFilaEnListado() {
        when(claves.generarConHash(1)).thenReturn(
                Collections.singletonList(new TcpeClaveService.Clave("Azar234567", "hashA")));
        when(generador.generarParaPersona(eq(30L), any(), eq("hashA"), eq(7L))).thenReturn(501L);
        when(resultados.listar(30L)).thenReturn(Collections.<TcpeResultadoExamenDto>emptyList());

        TcpeLoteGeneradoDto r = servicio.agregar(30L, servicio.pedido(agregado("O", null,
                Collections.singletonList(persona("99", "Eva", "Luna", null)), null)), 7L);

        TcpeCredencialDto c = r.getCredenciales().get(0);
        assertEquals("Azar234567", c.getPassword());
        assertEquals("99", c.getCi());
        assertEquals("Luna Eva", c.getNombreCompleto());
    }

    @Test
    @DisplayName("nombreCompleto sigue el formato de f_tcpe_datosUsuario y salta los vacíos")
    void formatoNombre() {
        assertEquals("Paz Mena Ana", TcpeLoteService.nombreCompleto("Paz", "Mena", "Ana"));
        assertEquals("Paz Ana", TcpeLoteService.nombreCompleto("Paz", " ", "Ana"));
        assertNull(TcpeLoteService.nombreCompleto(null, null, null));
    }
}
