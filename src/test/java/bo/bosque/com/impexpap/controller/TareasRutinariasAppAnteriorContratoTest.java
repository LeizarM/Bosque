package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.config.GlobalExceptionHandler;
import bo.bosque.com.impexpap.config.JacksonConfig;
import bo.bosque.com.impexpap.dao.ICajaChicaFlujo;
import bo.bosque.com.impexpap.dao.ICajaFuerte;
import bo.bosque.com.impexpap.dao.ICoches;
import bo.bosque.com.impexpap.dao.IEmpleado;
import bo.bosque.com.impexpap.dao.ILlegada;
import bo.bosque.com.impexpap.dao.IVistaDao;
import bo.bosque.com.impexpap.dto.RegistrarCajaFuerteRequest;
import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
import bo.bosque.com.impexpap.model.CajaChica;
import bo.bosque.com.impexpap.model.CocheDelDia;
import bo.bosque.com.impexpap.model.Empleado;
import bo.bosque.com.impexpap.model.Vista;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * La app anterior al SQL 96 (APK del 2026-10-09) contra este backend, sin actualizarla.
 *
 * <p>Manda a cada endpoint de Caja Fuerte, Revisar Autos y Caja Chica el JSON EXACTO que arma
 * esa app (copiado de sus repositorios, commit e8c2b9e de AppEsppapel2), pasando por Spring MVC
 * y por el ObjectMapper de produccion (autoconfiguracion de Spring Boot + JacksonConfig + la
 * zona horaria de application.properties). Las respuestas quedan en
 * {@code target/contrato-app-anterior/} y las lee el codigo de la app anterior en su propia prueba.
 */
class TareasRutinariasAppAnteriorContratoTest {

    private static final long COD_EMPLEADO = 75L;
    private static final int COD_USUARIO = 28;
    private static final Path SALIDA = Paths.get("target", "contrato-app-anterior");
    private static ObjectMapper jacksonDeProduccion;

    private ICoches cochesDao;
    private ICajaFuerte cajaFuerteDao;
    private ILlegada llegadaDao;
    private ICajaChicaFlujo cajaChicaFlujoDao;
    private IEmpleado empleadoDao;
    private MockMvc mvc;
    private Authentication auth;

    @BeforeAll
    static void jackson() throws Exception {
        AtomicReference<ObjectMapper> om = new AtomicReference<>();
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(JacksonConfig.class)
                .withPropertyValues("spring.jackson.time-zone=America/La_Paz")
                .run(ctx -> om.set(ctx.getBean(ObjectMapper.class)));
        jacksonDeProduccion = om.get();
        Files.createDirectories(SALIDA);
    }

    @BeforeEach
    void setUp() {
        cochesDao = mock(ICoches.class);
        cajaFuerteDao = mock(ICajaFuerte.class);
        llegadaDao = mock(ILlegada.class);
        cajaChicaFlujoDao = mock(ICajaChicaFlujo.class);
        empleadoDao = mock(IEmpleado.class);
        IVistaDao vistaDao = mock(IVistaDao.class);
        AccesoModuloHelper acceso = mock(AccesoModuloHelper.class);

        TareasRutinariasController controlador = mock(TareasRutinariasController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controlador, "cochesDao", cochesDao);
        ReflectionTestUtils.setField(controlador, "cajaFuerteDao", cajaFuerteDao);
        ReflectionTestUtils.setField(controlador, "llegadaDao", llegadaDao);
        ReflectionTestUtils.setField(controlador, "cajaChicaFlujoDao", cajaChicaFlujoDao);
        ReflectionTestUtils.setField(controlador, "empleadoDao", empleadoDao);
        ReflectionTestUtils.setField(controlador, "vistaDao", vistaDao);
        ReflectionTestUtils.setField(controlador, "accesoModuloHelper", acceso);

        UsernamePasswordAuthenticationToken t = new UsernamePasswordAuthenticationToken(
                "cajero", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_LIM")));
        t.setDetails(new DatosToken(COD_USUARIO, (int) COD_EMPLEADO, 1, "ROLE_LIM"));
        auth = t;
        when(acceso.codEmpleadoDelToken(any())).thenReturn(COD_EMPLEADO);
        when(vistaDao.obtainRoutes(COD_USUARIO)).thenReturn(Arrays.asList(
                ruta("tacTareas/CajaFuerte"), ruta("tacTareas/Coches"), ruta("tacTareas/CajaChica")));

        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(jacksonDeProduccion),
                        new ByteArrayHttpMessageConverter(),
                        new StringHttpMessageConverter(StandardCharsets.UTF_8))
                .build();
    }

    private static Vista ruta(String direccion) {
        Vista v = new Vista();
        v.setDireccion(direccion);
        return v;
    }

    /** POST como lo hace la app (Dio, JSON) y guarda la respuesta para la prueba de la app. */
    private String postear(String endpoint, String json, int statusEsperado, String archivo) throws Exception {
        MvcResult r = mvc.perform(post("/tareas-rutinarias" + endpoint)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
        String cuerpo = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals(statusEsperado, r.getResponse().getStatus(), endpoint + " -> " + cuerpo);
        if (archivo != null) {
            Files.write(SALIDA.resolve(archivo + ".json"), cuerpo.getBytes(StandardCharsets.UTF_8));
        }
        return cuerpo;
    }

    private Map<?, ?> json(String cuerpo) throws Exception {
        return jacksonDeProduccion.readValue(cuerpo, Map.class);
    }

    // ------------------------------------------------------------------ abrir

    @Test
    void abrirFlujoDevuelveUnNumeroMayorQueCeroParaLosTres() throws Exception {
        for (long idTarRuti : new long[] {40, 41, 42}) {
            String cuerpo = postear("/abrir-flujo", "{\"idTarRuti\":" + idTarRuti + "}", 200, "abrir_flujo_" + idTarRuti);
            assertEquals(75, ((Number) json(cuerpo).get("data")).intValue());
        }
    }

    // ------------------------------------------------------------ caja fuerte

    @Test
    void cajaFuerteRegistrarConElCuerpoDeLaAppAnterior() throws Exception {
        ArgumentCaptor<RegistrarCajaFuerteRequest> req = ArgumentCaptor.forClass(RegistrarCajaFuerteRequest.class);
        when(cajaFuerteDao.registrar(req.capture(), eq(40L), eq(COD_EMPLEADO), eq((long) COD_USUARIO)))
                .thenReturn(new RespuestaSp(0, "Llegadas registradas correctamente.", 2));

        // CajaFuerteImpl.registrar de la app anterior: idTarRuti, idBitTarea y las llegadas.
        String cuerpo = postear("/caja-fuerte/registrar",
                "{\"idTarRuti\":40,\"idBitTarea\":75,\"llegadas\":["
                        + "{\"cliente\":\"Ferretería del Sur\",\"moneda\":\"BS\",\"importe\":1200.5,\"tipo\":\"efect\",\"destino\":null,\"obs\":null},"
                        + "{\"cliente\":\"Distribuidora Norte\",\"moneda\":\"USD\",\"importe\":300,\"tipo\":\"chq\",\"destino\":\"Bóveda\",\"obs\":\"Cheque BNB\"}]}",
                201, "caja_fuerte_registrar");

        assertEquals(2, ((Number) json(cuerpo).get("data")).intValue());
        assertEquals(2, req.getValue().getLlegadas().size());
        assertEquals("Ferretería del Sur", req.getValue().getLlegadas().get(0).getCliente());
        assertEquals(1200.5, req.getValue().getLlegadas().get(0).getImporte());
        assertEquals("efect", req.getValue().getLlegadas().get(0).getTipo());
        assertNull(req.getValue().getLlegadas().get(0).getDestino());
        assertEquals("USD", req.getValue().getLlegadas().get(1).getMoneda());
        assertEquals("Bóveda", req.getValue().getLlegadas().get(1).getDestino());
    }

    @Test
    void cajaFuerteDelDiaDevuelveLasColumnasDelSp() throws Exception {
        // p_list_tac_Llegada 'B' tal como lo entrega JDBC (bigint Long, int Integer, date
        // java.sql.Date, datetime Timestamp, float Double).
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("idRp", 501L);
        fila.put("fecha", java.sql.Date.valueOf("2026-10-13"));
        fila.put("horallegada", Timestamp.valueOf("2026-10-13 10:42:17.123"));
        fila.put("persona", "MARIA LOPEZ");
        fila.put("cliente", "Ferretería del Sur");
        fila.put("moneda", "BS");
        fila.put("importe", 1200.5);
        fila.put("destino", null);
        fila.put("tipo", "efect");
        fila.put("obs", null);
        fila.put("fueVerificado", 0);
        fila.put("codEmpVerificado", null);
        fila.put("codSucursal", 20);
        fila.put("nombreSucursal", "CENTRAL");
        List<Map<String, Object>> filas = Collections.singletonList(fila);
        when(llegadaDao.obtenerDeHoyDe(COD_EMPLEADO)).thenReturn(filas);

        postear("/caja-fuerte/del-dia", "{\"idBitTarea\":75}", 200, "caja_fuerte_del_dia");
        verify(llegadaDao).obtenerDeHoyDe(COD_EMPLEADO);
    }

    // ------------------------------------------------------------------ autos

    @Test
    void autosListarYMarcarConElCuerpoDeLaAppAnterior() throws Exception {
        CocheDelDia sinMarcar = new CocheDelDia();
        sinMarcar.setIdCo(9001L);
        sinMarcar.setIdTarRuti(41L);
        sinMarcar.setIdCoche(12L);
        sinMarcar.setIdBitTarRuti(null); // Archivo 96: la lista de la sucursal no tiene ocurrencia
        sinMarcar.setDescripcion("Camioneta reparto");
        sinMarcar.setMarca("Toyota");
        sinMarcar.setClase("Camioneta");
        sinMarcar.setPlaca("917-SKP");
        sinMarcar.setColor("Blanco");
        sinMarcar.setAnio(2019);
        CocheDelDia marcado = new CocheDelDia();
        marcado.setIdCo(9002L);
        marcado.setIdTarRuti(41L);
        marcado.setIdCoche(13L);
        marcado.setIdBitTarRuti(null);
        marcado.setFecha(Timestamp.valueOf("2026-10-13 08:15:00"));
        marcado.setLlego(1);
        marcado.setObs("Llegó con raspón");
        marcado.setMarca("Nissan");
        marcado.setPlaca("2210-KLM");
        marcado.setQuienMarco("JUAN PEREZ");
        when(cochesDao.listarDelDia(41L, COD_EMPLEADO, COD_USUARIO)).thenReturn(Arrays.asList(sinMarcar, marcado));
        when(cochesDao.marcarLlegada(9001L, 1, "", COD_EMPLEADO, COD_USUARIO))
                .thenReturn(new RespuestaSp(0, "Revisión de coche registrada correctamente.", 9001L));
        when(cochesDao.marcarLlegada(9001L, 0, "No llegó", COD_EMPLEADO, COD_USUARIO))
                .thenReturn(new RespuestaSp(0, "Revisión de coche registrada correctamente.", 9001L));

        // CochesImpl de la app anterior: sin observacion manda "" (lo registra su prueba).
        postear("/coches/listar-del-dia", "{\"idTarRuti\":41,\"idBitTarea\":75}", 200, "coches_listar_del_dia");
        postear("/coches/marcar-llegada", "{\"idCo\":9001,\"llego\":1,\"obs\":\"\"}", 201, "coches_marcar_llegada");
        postear("/coches/marcar-llegada", "{\"idCo\":9001,\"llego\":0,\"obs\":\"No llegó\"}", 201, null);

        verify(cochesDao).marcarLlegada(9001L, 1, "", COD_EMPLEADO, COD_USUARIO);
        verify(cochesDao).marcarLlegada(9001L, 0, "No llegó", COD_EMPLEADO, COD_USUARIO);
    }

    // ------------------------------------------------------------- caja chica

    private static CajaChica movimiento(long idCC, Long idBitTarRuti, Double ing, Double eg, double saldo, String desc) {
        CajaChica c = new CajaChica();
        c.setIdCC(idCC);
        c.setIdBitTarRuti(idBitTarRuti);
        c.setMontoIng(ing);
        c.setMontoEg(eg);
        c.setSaldo(saldo);
        c.setMoneda("BS");
        c.setDescripcion(desc);
        c.setFecha(java.sql.Date.valueOf("2026-10-13"));
        c.setCodSucursal(20L);
        c.setLote(30L);
        c.setAudUsuario(COD_USUARIO);
        c.setAudFecha(Timestamp.valueOf("2026-10-13 09:00:00"));
        return c;
    }

    @Test
    void cajaChicaConElCuerpoDeLaAppAnterior() throws Exception {
        CajaChica inicial = movimiento(1001L, 555L, 200.0, 0.0, 200.0, "Saldo inicial del lote");
        CajaChica egreso = movimiento(1002L, null, null, 35.5, 164.5, "Taxi");
        egreso.setNumVale(7);
        egreso.setNumFactura(null);
        egreso.setCodEmpDestino(120L);
        ReflectionTestUtils.setField(egreso, "nombreEmpDestino", "JUAN PEREZ");
        ReflectionTestUtils.setField(egreso, "codTalonario", 3L);
        ReflectionTestUtils.setField(egreso, "nroTalonario", "VCC001");
        when(cajaChicaFlujoDao.listarDelLote(COD_EMPLEADO, COD_USUARIO)).thenReturn(Arrays.asList(inicial, egreso));

        ArgumentCaptor<RegistrarEgresoCajaChicaRequest> req = ArgumentCaptor.forClass(RegistrarEgresoCajaChicaRequest.class);
        when(cajaChicaFlujoDao.registrarEgreso(req.capture(), eq(COD_EMPLEADO), eq((long) COD_USUARIO)))
                .thenReturn(new RespuestaSp(0, "Egreso registrado correctamente.", 1003L));
        when(cajaChicaFlujoDao.cerrarLote(COD_EMPLEADO, COD_USUARIO))
                .thenReturn(new RespuestaSp(0, "Lote cerrado.", 31L));

        Map<String, Object> lote = new LinkedHashMap<>();
        lote.put("lote", 30L);
        lote.put("codSucursal", 20L);
        lote.put("desde", java.sql.Date.valueOf("2026-09-22"));
        lote.put("hasta", java.sql.Date.valueOf("2026-10-13"));
        lote.put("nombreSucursal", "CENTRAL");
        lote.put("totalEgresos", 35.5);
        when(cajaChicaFlujoDao.obtenerHistorialLotes(COD_EMPLEADO)).thenReturn(Collections.singletonList(lote));

        Empleado e = new Empleado();
        e.setCodEmpleado(120);
        e.getPersona().setDatoPersona("PEREZ ROJAS JUAN");
        e.getEmpleadoCargo().getCargoSucursal().getCargo().setDescripcion("CAJERO");
        when(empleadoDao.obtenerLstEmpleados("jua", 1, 0, 20, null)).thenReturn(Collections.singletonList(e));

        // CajaChicaFlujoImpl de la app anterior, en el orden en que se usa la pantalla.
        postear("/caja-chica/listar-del-lote", "{\"idBitTarea\":75}", 200, "caja_chica_listar_del_lote");
        postear("/caja-chica/buscar-empleados", "{\"search\":\"jua\"}", 200, "caja_chica_buscar_empleados");
        postear("/caja-chica/registrar-egreso",
                "{\"idBitTarea\":75,\"montoEg\":35.5,\"descripcion\":\"Taxi\",\"codEmpDestino\":120,"
                        + "\"numFactura\":null,\"numVale\":7,\"moneda\":\"BS\"}",
                201, "caja_chica_registrar_egreso");
        assertEquals(35.5, req.getValue().getMontoEg());
        assertEquals("Taxi", req.getValue().getDescripcion());
        assertEquals(120L, req.getValue().getCodEmpDestino());
        assertNull(req.getValue().getNumFactura());
        assertEquals(Integer.valueOf(7), req.getValue().getNumVale());
        assertEquals("BS", req.getValue().getMoneda());

        // Sin "Entregado a" ni recibo la app manda null: el backend lo recibe igual.
        postear("/caja-chica/registrar-egreso",
                "{\"idBitTarea\":75,\"montoEg\":10,\"descripcion\":\"Útiles\",\"codEmpDestino\":null,"
                        + "\"numFactura\":15,\"numVale\":null,\"moneda\":null}",
                201, null);
        assertEquals(0L, req.getValue().getCodEmpDestino());
        assertNull(req.getValue().getNumVale());

        postear("/caja-chica/historial-lotes", "{\"idBitTarea\":75}", 200, "caja_chica_historial_lotes");
        postear("/caja-chica/cerrar-lote", "{\"idBitTarea\":75}", 201, "caja_chica_cerrar_lote");
        verify(cajaChicaFlujoDao).cerrarLote(COD_EMPLEADO, COD_USUARIO);

        // PDF de un lote que no es de su sucursal: el cuerpo de la app ({lote, codSucursal}) se lee.
        String rechazo = postear("/caja-chica/reporte-pdf", "{\"lote\":99,\"codSucursal\":20}", 400, null);
        assertEquals("Ese lote no es de la caja chica de tu sucursal.", json(rechazo).get("message"));
    }

    // ------------------------------------------------------------- vacios

    @Test
    void listasVaciasContestan204ComoAntes() throws Exception {
        when(cochesDao.listarDelDia(41L, COD_EMPLEADO, COD_USUARIO)).thenReturn(Collections.emptyList());
        when(llegadaDao.obtenerDeHoyDe(COD_EMPLEADO)).thenReturn(Collections.emptyList());
        when(cajaChicaFlujoDao.listarDelLote(COD_EMPLEADO, COD_USUARIO)).thenReturn(Collections.emptyList());
        postear("/coches/listar-del-dia", "{\"idTarRuti\":41,\"idBitTarea\":75}", 204, null);
        postear("/caja-fuerte/del-dia", "{\"idBitTarea\":75}", 204, null);
        postear("/caja-chica/listar-del-lote", "{\"idBitTarea\":75}", 204, null);
    }
}
