package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoModuloHelper;
import bo.bosque.com.impexpap.config.JacksonConfig;
import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.config.SpConfirmableException;
import bo.bosque.com.impexpap.config.SpConflictException;
import bo.bosque.com.impexpap.dao.IAbonoDias;
import bo.bosque.com.impexpap.dao.IPermiso;
import bo.bosque.com.impexpap.dao.IVacacionAsignada;
import bo.bosque.com.impexpap.dto.CalculoAntiguedadDto;
import bo.bosque.com.impexpap.dto.DesgloseSaldoDto;
import bo.bosque.com.impexpap.dto.EmpleadoColectivoDto;
import bo.bosque.com.impexpap.dto.FichaSaldoDto;
import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.PermisoKardexDto;
import bo.bosque.com.impexpap.dto.TipoPermisoDto;
import bo.bosque.com.impexpap.dto.TramoSaldoDto;
import bo.bosque.com.impexpap.model.AbonoDias;
import bo.bosque.com.impexpap.model.VacacionAsignada;
import bo.bosque.com.impexpap.security.MainSecurity;
import bo.bosque.com.impexpap.security.SecurityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fija el <b>contrato HTTP</b> de {@code /permiso-rrhh}: qué código y qué cuerpo sale en cada
 * situación. Es lo que impide que un refactor "que no cambia nada" convierta un 400 con motivo
 * en un 204 mudo, o un 409 en un 400.
 *
 * <h3>Por qué la cadena de seguridad está apagada acá</h3>
 * Se excluyen {@code MainSecurity} —que necesita {@code LoginDaoImpl} y con él toda la capa de
 * datos— y {@code SecurityFilter} —el WAF de patrones SQLi—, más las dos autoconfiguraciones de
 * seguridad. Lo que se prueba es el controlador: los códigos 403 de este test salen del
 * {@code AccesoModuloHelper} mockeado y llegan al cliente por {@code GlobalExceptionHandler},
 * que sí está en el slice. El {@code @Secured} de cada endpoint es una segunda reja y se verifica
 * con un token real, no acá.
 *
 * <p>Como no hay contexto de seguridad, el parámetro {@code Authentication} de los endpoints
 * llega {@code null}; da igual, porque el único que lo mira es el helper mockeado.
 *
 * <h3>{@code JacksonConfig} SÍ entra, y no es opcional</h3>
 * {@code @WebMvcTest} no escanea {@code @Configuration}, así que sin este {@code @Import} el slice
 * corre con el {@code ObjectMapper} de fábrica — que <b>no parsea {@code "2026-04-06T08:30:00"}</b>,
 * justo el formato que manda el cliente para la vacación colectiva. El test pasaría o fallaría por
 * un motivo que no existe en producción. Con el import, los cuerpos de acá son literalmente los que
 * viajan por el cable.
 */
@WebMvcTest(controllers = PermisoRrhhController.class,
        excludeAutoConfiguration = { SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class },
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = { MainSecurity.class, SecurityFilter.class }))
@Import(JacksonConfig.class)
class PermisoRrhhControllerTest {

    private static final String FICHA    = "/permiso-rrhh/saldo/ficha";
    private static final String DESGLOSE = "/permiso-rrhh/saldo/desglose";
    private static final String CALCULO  = "/permiso-rrhh/herramientas/calculo-antiguedad";

    private static final String VAC_REGISTRAR = "/permiso-rrhh/vacacion-asignada/registrar";
    private static final String VAC_ELIMINAR  = "/permiso-rrhh/vacacion-asignada/eliminar";
    private static final String VAC_HISTORIAL = "/permiso-rrhh/vacacion-asignada/historial";
    private static final String ABO_REGISTRAR = "/permiso-rrhh/abono-dias/registrar";
    private static final String ABO_ELIMINAR  = "/permiso-rrhh/abono-dias/eliminar";
    private static final String ABO_HISTORIAL = "/permiso-rrhh/abono-dias/historial";
    private static final String GRUPAL_SIMULAR = "/permiso-rrhh/colectivo/abono-dias/simular";
    private static final String GRUPAL_APLICAR = "/permiso-rrhh/colectivo/abono-dias/aplicar";
    private static final String COL_EMPLEADOS  = "/permiso-rrhh/colectivo/empleados";
    private static final String COL_VAC_SIMULAR = "/permiso-rrhh/colectivo/vacacion/simular";
    private static final String COL_VAC_APLICAR = "/permiso-rrhh/colectivo/vacacion/aplicar";

    private static final String KARDEX          = "/permiso-rrhh/permisos/kardex";
    private static final String VAC_GANADAS     = "/permiso-rrhh/permisos/vacaciones-ganadas";
    private static final String TIPOS           = "/permiso-rrhh/permisos/tipos";
    private static final String CALCULAR        = "/permiso-rrhh/permisos/calcular";
    private static final String PERM_REGISTRAR  = "/permiso-rrhh/permisos/registrar";
    private static final String VAC_IND_REGISTRAR = "/permiso-rrhh/vacacion/registrar";
    private static final String VAC_PAGAR       = "/permiso-rrhh/vacacion/pagar";

    /** El {@code codUsuario} que resuelve el token del que llama. NUNCA sale del body. */
    private static final long YO = 7L;

    @Autowired private MockMvc mvc;

    @MockBean private IPermiso pDao;
    @MockBean private AccesoModuloHelper acceso;
    @MockBean private IVacacionAsignada vacDao;
    @MockBean private IAbonoDias abonoDao;

    /**
     * Lo pide el constructor desde que existen los reportes de estado de cuenta: Jasper corre su
     * propia consulta contra la conexión, no contra un DAO. {@code @WebMvcTest} no levanta el
     * DataSource, así que sin este mock no arranca ni el contexto.
     */
    @MockBean private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    /**
     * Lo pide el constructor desde que la resolución de la relación laboral vigente pasa por
     * {@code p_list_AbonoDias @ACCION='R'} en vez de armar SQL en Java. Mismo motivo que el
     * {@code JdbcTemplate} de arriba: {@code @WebMvcTest} no lo trae solo.
     */
    @MockBean private bo.bosque.com.impexpap.utils.SpHelper spHelper;

    @BeforeEach
    void identidadDelToken() {
        MiEquipoDto yo = new MiEquipoDto();
        yo.setCodUsuario(YO);
        yo.setCodEmpleado(130L);
        when(acceso.miPermiso(any())).thenReturn(yo);
    }

    // ============================================================
    // 200
    // ============================================================

    @Test
    @DisplayName("200 · la ficha viaja como LISTA dentro del envelope, con los *Txt ya armados")
    void fichaOk() throws Exception {
        when(pDao.fichaSaldoEmpleado(130L)).thenReturn(new ArrayList<>(
                Collections.singletonList(fichaDe(130L, 12.5, 2.0))));

        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].codEmpleado").value(130))
                .andExpect(jsonPath("$.data[0].totalDias").value(14.5))
                .andExpect(jsonPath("$.data[0].totalDiasTxt").value("14,5 días"))
                // D0: el rótulo de alcance viaja en el contrato, no es cosa de la pantalla.
                .andExpect(jsonPath("$.data[0].tieneRelacionesAnteriores").value(true))
                .andExpect(jsonPath("$.data[0].movimientosFueraDeRelacionVigente").value(184));
    }

    @Test
    @DisplayName("200 · el desglose viaja como OBJETO (no lista) con los 5 tramos y el total")
    void desgloseOk() throws Exception {
        when(pDao.desgloseSaldo(130L)).thenReturn(desgloseDe());

        mvc.perform(post(DESGLOSE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130}"))
                .andExpect(status().isOk())
                // Lista vs objeto NO es un detalle: del lado Flutter se eligen helpers distintos
                // y equivocarse devuelve null en silencio.
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.tramos.length()").value(5))
                // El tramo 3 NO entra en el total (D1) y el contrato lo dice con el signo.
                .andExpect(jsonPath("$.data.tramos[2].signo").value("INFO"))
                .andExpect(jsonPath("$.data.montoTotal").value(12.5))
                .andExpect(jsonPath("$.data.etiquetaTotal").value("TOTAL VACACION NO UTILIZADO"))
                .andExpect(jsonPath("$.data.desgloseAplicable").value(true))
                // Criterio #4: la etiqueta del SP se pasa LITERAL, con el espacio antes del ')'.
                .andExpect(jsonPath("$.data.tramos[0].etiqueta")
                        .value("Saldo hasta el penultimo año cumplido (28/02/2025 )"));
    }

    @Test
    @DisplayName("200 · la calculadora viaja como LISTA de una fila, con la frase del SP literal")
    void calculoOk() throws Exception {
        String frase = " Trabajo 11 año(s) 5 mes(es) y 10 dia(s) y le corresponden = 30 dias por"
                + " antiguedad, tiene un total de 13.8 Dias acumulados , hasta el momento";
        when(pDao.calcularAntiguedad(any(), any())).thenReturn(new CalculoAntiguedadDto(frase));

        mvc.perform(post(CALCULO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2015-03-01\",\"hasta\":\"2026-08-11\"}"))
                .andExpect(status().isOk())
                // LISTA y no objeto. Es lo que ancla el contrato del plan §6.3 ④ contra el
                // cliente: `postAndReturnList` castea `data` a List y con un objeto lanza un
                // TypeError —no devuelve null en silencio—, así que la pestaña entera se cae.
                // Este `isArray()` es exactamente lo que faltaba para cazarlo.
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].datoAntCalc").value(frase));
    }

    // ============================================================
    // 204 — sin cuerpo
    // ============================================================

    @Test
    @DisplayName("204 · consulta correcta y sin filas: sin cuerpo, y el cliente pinta el vacío")
    void fichaVacia() throws Exception {
        when(pDao.fichaSaldoEmpleado(999L)).thenReturn(new ArrayList<>());

        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":999}"))
                // Sólo se fija el código. El cuerpo que arma el controlador para el 204 NO llega
                // a nadie: Tomcat lo descarta —un 204 no lleva cuerpo— y del otro lado
                // postAndReturnList hace "return []" sin mirar. (MockMvc sí lo muestra, porque
                // no emula esa parte del contenedor; no confundirlo con el comportamiento real.)
                // Corolario: todo lo que haya que explicarle al usuario sale como 400.
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("204 · el desglose sin fila devuelve 204, no un objeto vacío")
    void desgloseVacio() throws Exception {
        when(pDao.desgloseSaldo(999L)).thenReturn(null);

        mvc.perform(post(DESGLOSE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":999}"))
                .andExpect(status().isNoContent());
    }

    // ============================================================
    // 400
    // ============================================================

    @Test
    @DisplayName("400 · sin empleado seleccionado se corta ANTES de tocar la base")
    void sinEmpleado() throws Exception {
        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe seleccionar un empleado."));

        verify(pDao, never()).fichaSaldoEmpleado(anyLong());
    }

    @Test
    @DisplayName("400 · el motivo del vacío llega tal cual lo escribió el DAO (consulta discriminante)")
    void motivoDelVacio() throws Exception {
        when(pDao.fichaSaldoEmpleado(77L)).thenThrow(new SpBusinessException(
                "El empleado no tiene una relacion laboral activa, asi que no se le puede "
              + "calcular el saldo de vacacion. Registre la relacion laboral en el ERP."));

        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":77}"))
                .andExpect(status().isBadRequest())
                // Decir "sin relación activa" cuando falta el cargo manda a revisar el lugar
                // equivocado: por eso el mensaje del DAO no se reemplaza por uno genérico.
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("relacion laboral activa")));
    }

    @Test
    @DisplayName("400 · la calculadora sin fechas (el SP no valida nada)")
    void calculoSinFechas() throws Exception {
        mvc.perform(post(CALCULO).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Debe indicar la fecha de inicio de beneficio y la fecha a calcular."));

        verify(pDao, never()).calcularAntiguedad(any(), any());
    }

    @Test
    @DisplayName("400 · rango de más de 60 años")
    void calculoRangoAbsurdo() throws Exception {
        mvc.perform(post(CALCULO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"1900-01-01\",\"hasta\":\"2026-08-11\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("60 años")));
    }

    // ============================================================
    // 403
    // ============================================================

    @Test
    @DisplayName("403 · sin el botón del ACL no se llega a la base, aunque el token sea válido")
    void sinPermiso() throws Exception {
        doThrow(new AccessDeniedException("no"))
                .when(acceso).exigirBoton(any(), anyInt(), anyString());

        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130}"))
                .andExpect(status().isForbidden())
                // El handler global PISA el mensaje de la excepción con uno fijo. Se fija acá
                // para que nadie pierda tiempo redactando textos de 403 que nunca se ven.
                .andExpect(jsonPath("$.message")
                        .value("No tienes los permisos necesarios para realizar esta acción."));

        verify(pDao, never()).fichaSaldoEmpleado(anyLong());
    }

    // ============================================================
    // 409
    // ============================================================

    @Test
    @DisplayName("409 · dos relaciones activas: se corta y se dice qué arreglar, no se elige una")
    void dosRelacionesActivas() throws Exception {
        when(pDao.fichaSaldoEmpleado(139L)).thenThrow(new SpConflictException(
                "El empleado tiene más de una relación laboral activa (2); corrija en el ERP antes de consultar."));

        mvc.perform(post(FICHA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":139}"))
                // 409 y no 400: la pregunta está bien hecha, el que está mal es el dato, y el
                // cliente no lo arregla reintentando.
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("más de una relación laboral activa")));
    }

    // ============================================================
    // ESCRITURAS · vacación asignada
    // ============================================================

    @Test
    @DisplayName("201 · alta: el id en 0 es un alta, y el que firma sale del TOKEN, no del body")
    void altaVacacionAsignada() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenReturn(vacacionDe(880L, 15));

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        // El body MIENTE sobre quién firma: manda audUsuarioI 9999. Tiene que
                        // llegar el 7 del token igual, o cualquiera audita a nombre de otro.
                        .content("{\"codVacacionAsignada\":0,\"codEmpleado\":130,"
                               + "\"codRelEmplEmpr\":411,\"dias\":15,"
                               + "\"motivo\":\"Aniversario 2026\",\"fecha\":\"2026-03-01\","
                               + "\"audUsuarioI\":9999,\"codUsuario\":9999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.codVacacionAsignada").value(880))
                .andExpect(jsonPath("$.data.diasAsignadosTxt").value("15 días"));

        ArgumentCaptor<VacacionAsignada> cap = ArgumentCaptor.forClass(VacacionAsignada.class);
        verify(vacDao).alta(cap.capture(), eq(YO), eq(false));
        assertEquals(130L, cap.getValue().getCodEmpleado());
        // La relación NO se acepta del body aunque el cliente la mande: el body dice 411 y acá
        // tiene que llegar null, porque la resuelve el DAO contra la base. Con la del body, un
        // POST podía insertar la vacación en la relación de otra persona.
        org.junit.jupiter.api.Assertions.assertNull(cap.getValue().getCodRelEmplEmpr());
        // Alta y edición comparten endpoint: si se confunden, se pisa la fila de otro año.
        verify(vacDao, never()).edicion(anyLong(), anyDouble(), anyString(), anyLong());
    }

    @Test
    @DisplayName("CONTRATO · el `registrar` devuelve la FILA (objeto), nunca un id suelto")
    void registrarDevuelveObjetoNoNumero() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenReturn(vacacionDe(880L, 15));
        when(abonoDao.alta(any(), anyLong(), anyBoolean())).thenReturn(abonoDe(501L, 2.5));

        // Los SP de este módulo NO devuelven SCOPE_IDENTITY: releer es obligatorio igual, así que
        // se devuelve la fila entera. Si alguien "simplifica" esto a un id, el cliente —que parsea
        // `data` con el Model— revienta, y las hojas dejan de poder mostrar lo que quedó guardado.
        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"dias\":15,\"motivo\":\"Aniversario 2026\","
                               + "\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.codVacacionAsignada").value(880))
                .andExpect(jsonPath("$.data.diasAsignados").value(15.0));

        mvc.perform(post(ABO_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"dias\":2.5,\"motivo\":\"Compensación\","
                               + "\"fecha\":\"2026-04-12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.codAbonoDias").value(501));
    }

    @Test
    @DisplayName("CONTRATO · la clave de días es `dias`, y los nombres viejos entran por @JsonAlias")
    void laClaveDeDiasEsDias() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenReturn(vacacionDe(880L, 15));

        // Sin FAIL_ON_UNKNOWN_PROPERTIES, `diasAsignados` se descartaba en silencio y `dias`
        // llegaba 0 por ser primitivo: se grababa una vacación de CERO días —que además es un
        // valor legítimo, así que ninguna validación la frenaba— y el 201 decía que todo bien.
        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"diasAsignados\":15,"
                               + "\"motivo\":\"Aniversario 2026\",\"fecha\":\"2026-03-01\","
                               + "\"confirmarDuplicado\":true}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<VacacionAsignada> cap = ArgumentCaptor.forClass(VacacionAsignada.class);
        // 15 y no 0; y la confirmación del nombre viejo también llega.
        verify(vacDao).alta(cap.capture(), eq(YO), eq(true));
        assertEquals(15.0, cap.getValue().getDiasAsignados(), 0.0001);
    }

    @Test
    @DisplayName("CONTRATO · el 400 confirmable se marca con `confirmable`, el común no")
    void elCuerpoDistingueElCuatrocientosConfirmable() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenThrow(new SpConfirmableException(
                "Ese aniversario ya tiene una vacación asignada de 15 días (registro 880)."));

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"dias\":15,\"motivo\":\"Aniversario 2026\","
                               + "\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isBadRequest())
                // Sin este marcador el cliente tenía que adivinar por el TEXTO si ofrecer el botón
                // "Confirmar": el día que alguien reescriba el mensaje, dejaría de ofrecerlo.
                .andExpect(jsonPath("$.confirmable").value(true));

        // Y el 400 de regla de negocio común NO lo lleva: reintentar no lo arregla.
        when(abonoDao.alta(any(), anyLong(), anyBoolean())).thenThrow(
                new SpBusinessException("Se debe asignar un número de días mayor a 0."));

        mvc.perform(post(ABO_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"dias\":0,\"motivo\":\"Prueba\","
                               + "\"fecha\":\"2026-04-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.confirmable").doesNotExist());
    }

    @Test
    @DisplayName("200 · el mismo endpoint con id > 0 EDITA, y no toca empleado ni relación")
    void edicionVacacionAsignada() throws Exception {
        when(vacDao.edicion(eq(880L), eq(20.0), anyString(), eq(YO)))
                .thenReturn(vacacionDe(880L, 20));

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        // codEmpleado y codRelEmplEmpr viajan y se IGNORAN: el SP no los actualiza
                        // (mover la fila de relación le cambiaría el saldo a dos personas a la vez).
                        .content("{\"codVacacionAsignada\":880,\"codEmpleado\":999,"
                               + "\"codRelEmplEmpr\":999,\"dias\":20,\"motivo\":\"Corrección\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diasAsignados").value(20));

        verify(vacDao, never()).alta(any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("400 · la regla que viola el alta llega con el motivo puesto, no como 500")
    void reglaDeNegocioViolada() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenThrow(new SpBusinessException(
                "La fecha indicada no es un aniversario de la relación laboral del empleado."));

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,\"dias\":15,"
                               + "\"motivo\":\"Prueba\",\"fecha\":\"2026-05-09\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("no es un aniversario")));
    }

    @Test
    @DisplayName("409 · doble toque: la fila idéntica recién cargada no se vuelve a insertar")
    void duplicadoDaConflicto() throws Exception {
        when(vacDao.alta(any(), anyLong(), anyBoolean())).thenThrow(new SpConflictException(
                "Esa misma vacación asignada acaba de registrarse (registro 1425). "
              + "No se cargó de nuevo."));

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        // El segundo toque manda EXACTAMENTE el mismo cuerpo, confirmación incluida:
                        // por eso el 409 tiene que salir igual con confirmado en true.
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,\"dias\":15,"
                               + "\"motivo\":\"Aniversario 2026\",\"fecha\":\"2026-03-01\","
                               + "\"confirmado\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("acaba de registrarse")));
    }

    @Test
    @DisplayName("403 · sin el botón del ACL no se escribe NADA, aunque el token sea válido")
    void sinPermisoNoEscribe() throws Exception {
        doThrow(new AccessDeniedException("no"))
                .when(acceso).exigirBoton(any(), anyInt(), anyString());

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,\"dias\":15,"
                               + "\"motivo\":\"Aniversario 2026\",\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post(GRUPAL_APLICAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dias\":1,\"motivo\":\"Feriado\",\"fecha\":\"2026-08-01\","
                               + "\"codEmpleados\":[1,2,3]}"))
                .andExpect(status().isForbidden());

        // Ni una sola llamada al DAO: el gate está ANTES, no después de armar el modelo.
        verifyNoInteractions(vacDao);
        verifyNoInteractions(abonoDao);
    }

    @Test
    @DisplayName("400 · si el token no resuelve a un usuario no se escribe (audUsuarioI es NOT NULL)")
    void sinIdentidadNoEscribe() throws Exception {
        when(acceso.miPermiso(any())).thenReturn(new MiEquipoDto());   // codUsuario null

        mvc.perform(post(VAC_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,\"dias\":15,"
                               + "\"motivo\":\"Aniversario 2026\",\"fecha\":\"2026-03-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("identificar su usuario")));

        verify(vacDao, never()).alta(any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("400 · la baja sin empleado se corta acá: el DELETE del SP no mira de quién es la fila")
    void bajaExigeElDuenoDeLaFila() throws Exception {
        mvc.perform(post(VAC_ELIMINAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codVacacionAsignada\":880}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe seleccionar un empleado."));

        // Sin codEmpleado el DAO no puede comprobar la pertenencia y borraría la fila de cualquiera.
        verify(vacDao, never()).baja(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("200 · la baja devuelve la fila borrada Y firma con el usuario del TOKEN")
    void bajaDevuelveLoBorrado() throws Exception {
        when(vacDao.baja(880L, 130L, YO)).thenReturn(vacacionDe(880L, 15));

        mvc.perform(post(VAC_ELIMINAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codVacacionAsignada\":880,\"codEmpleado\":130,"
                               + "\"audUsuarioI\":9999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codVacacionAsignada").value(880))
                .andExpect(jsonPath("$.data.diasAsignados").value(15));

        // El SP de la 'D' no recibe @audUsuarioI: sin este dato el trigger archivaría la fila con
        // el usuario que la CREÓ y la bitácora diría que la borró alguien que no fue.
        verify(vacDao).baja(880L, 130L, YO);
    }

    @Test
    @DisplayName("200 · el historial mezcla las filas reales con los aniversarios sintéticos")
    void historialConSinteticos() throws Exception {
        VacacionAsignada sintetica = vacacionDe(0L, 0);
        sintetica.setSintetico(true);
        when(vacDao.historial(eq(130L), eq(411L), any()))
                .thenReturn(new ArrayList<>(Arrays.asList(vacacionDe(880L, 15), sintetica)));

        mvc.perform(post(VAC_HISTORIAL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                // De acá sale el "Nuevo" de la pantalla: sin este flag no hay alta posible.
                .andExpect(jsonPath("$.data[1].sintetico").value(true))
                .andExpect(jsonPath("$.data[1].codVacacionAsignada").value(0));
    }

    @Test
    @DisplayName("200 · el historial SIN relación no es un 400: la resuelve el servidor")
    void historialSinRelacionLaResuelveElServidor() throws Exception {
        when(vacDao.historial(eq(130L), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(vacacionDe(880L, 15))));
        when(abonoDao.historial(eq(130L), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(abonoDe(501L, 2.5))));

        // El cliente manda 0 (o nada) cuando todavía no consultó la ficha de saldo. Exigirla acá
        // dejaba los DOS historiales en un 400 permanente: el módulo no podía ni listar.
        mvc.perform(post(VAC_HISTORIAL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mvc.perform(post(ABO_HISTORIAL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // El null y el 0 llegan TAL CUAL al DAO, que es quien decide resolver la vigente. Que la
        // decisión viva en el DAO y no acá es lo que permite que el alta y el historial usen la
        // misma consulta.
        verify(vacDao).historial(130L, null, null);
        verify(abonoDao).historial(130L, 0L);
    }

    // ============================================================
    // ESCRITURAS · abono de días
    // ============================================================

    @Test
    @DisplayName("201 · alta de abono: la relación laboral NO viaja del cliente, la resuelve el DAO")
    void altaAbonoIgnoraLaRelacionDelBody() throws Exception {
        when(abonoDao.alta(any(), anyLong(), anyBoolean())).thenReturn(abonoDe(501L, 2.5));

        mvc.perform(post(ABO_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codAbonoDias\":0,\"codEmpleado\":130,"
                               + "\"codRelEmplEmpr\":999,\"dias\":2.5,"
                               + "\"motivo\":\"Compensación día del niño\","
                               + "\"fecha\":\"2026-04-12\",\"confirmado\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codAbonoDias").value(501))
                .andExpect(jsonPath("$.data.diasAbonadosTxt").value("2,5 días"));

        ArgumentCaptor<AbonoDias> cap = ArgumentCaptor.forClass(AbonoDias.class);
        verify(abonoDao).alta(cap.capture(), eq(YO), eq(true));
        // La relación equivocada le movería el saldo a otra persona-relación: se descarta acá.
        org.junit.jupiter.api.Assertions.assertNull(cap.getValue().getCodRelEmplEmpr());
        assertEquals(2.5, cap.getValue().getDiasAbonados(), 0.0001);
    }

    @Test
    @DisplayName("400 · el umbral del abono es > 0 estricto, distinto al de vacación asignada")
    void abonoDeCeroDias() throws Exception {
        when(abonoDao.alta(any(), anyLong(), anyBoolean())).thenThrow(
                new SpBusinessException("Se debe asignar un número de días mayor a 0."));

        mvc.perform(post(ABO_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"dias\":0,\"motivo\":\"Prueba\","
                               + "\"fecha\":\"2026-04-12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Se debe asignar un número de días mayor a 0."));
    }

    @Test
    @DisplayName("200 · el abono con id > 0 edita y no da de alta uno nuevo")
    void edicionAbono() throws Exception {
        when(abonoDao.edicion(eq(501L), eq(3.0), anyString(), eq(YO)))
                .thenReturn(abonoDe(501L, 3.0));

        mvc.perform(post(ABO_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codAbonoDias\":501,\"dias\":3,\"motivo\":\"Corrección\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.diasAbonados").value(3.0));

        verify(abonoDao, never()).alta(any(), anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("400 · la baja de abono también exige el dueño de la fila")
    void bajaAbonoExigeEmpleado() throws Exception {
        mvc.perform(post(ABO_ELIMINAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codAbonoDias\":501}"))
                .andExpect(status().isBadRequest());

        verify(abonoDao, never()).baja(anyLong(), anyLong(), anyLong());
    }

    // ============================================================
    // ESCRITURAS · carga colectiva
    // ============================================================

    @Test
    @DisplayName("200 · SIMULAR NO ESCRIBE: devuelve quién entra y quién no, y no toca la base")
    void simularNoEscribeNada() throws Exception {
        AbonoDias entra = abonoDe(0L, 1);
        entra.setCodEmpleado(130L);
        entra.setDatoEmpleado("PEREZ GOMEZ JUAN CARLOS");
        entra.setAplica(true);
        entra.setMotivoOmision("");

        AbonoDias queda = abonoDe(0L, 1);
        queda.setCodEmpleado(140L);
        queda.setDatoEmpleado("ROJAS LIMA ANA");
        queda.setAplica(false);
        queda.setMotivoOmision("Ya tiene un abono cargado ese día (1 día: Feriado).");

        when(abonoDao.simularGrupal(any(), any()))
                .thenReturn(new ArrayList<>(Arrays.asList(entra, queda)));

        mvc.perform(post(GRUPAL_SIMULAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dias\":1,\"motivo\":\"Feriado\",\"fecha\":\"2026-08-01\","
                               + "\"codEmpleados\":[130,140]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].aplica").value(true))
                .andExpect(jsonPath("$.data[1].aplica").value(false))
                .andExpect(jsonPath("$.data[1].motivoOmision").value(
                        org.hamcrest.Matchers.containsString("Ya tiene un abono")));

        // LO QUE IMPORTA DE ESTE TEST: la simulación no llama a NINGÚN método de escritura.
        // Si alguna vez alguien la "optimiza" reutilizando aplicarGrupal, esto se cae.
        verify(abonoDao, never()).aplicarGrupal(any(), anyList(), anyLong());
        verify(abonoDao, never()).alta(any(), anyLong(), anyBoolean());
        verify(abonoDao, never()).edicion(anyLong(), anyDouble(), anyString(), anyLong());
        verify(abonoDao, never()).baja(anyLong(), anyLong(), anyLong());
        verifyNoInteractions(vacDao);
    }

    @Test
    @DisplayName("201 · aplicar dice a cuántos se les cargó, y firma con el usuario del token")
    void aplicarGrupal() throws Exception {
        when(abonoDao.aplicarGrupal(any(), any(), eq(YO))).thenReturn(38);

        mvc.perform(post(GRUPAL_APLICAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dias\":1,\"motivo\":\"Feriado\",\"fecha\":\"2026-08-01\","
                               + "\"codEmpleados\":[130,140]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data").value(38))
                .andExpect(jsonPath("$.message").value("Se registró el abono a 38 empleado(s)."));

        ArgumentCaptor<AbonoDias> cap = ArgumentCaptor.forClass(AbonoDias.class);
        verify(abonoDao).aplicarGrupal(cap.capture(), eq(Arrays.asList(130L, 140L)), eq(YO));
        assertEquals("Feriado", cap.getValue().getMotivo());
    }

    @Test
    @DisplayName("400 · si la carga colectiva falla, el mensaje dice que NO quedó ninguna fila")
    void aplicarGrupalTodoONada() throws Exception {
        when(abonoDao.aplicarGrupal(any(), any(), anyLong())).thenThrow(new SpBusinessException(
                "No se pudo registrar el abono de días: no se guardó ninguno."));

        mvc.perform(post(GRUPAL_APLICAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dias\":1,\"motivo\":\"Feriado\",\"fecha\":\"2026-08-01\","
                               + "\"codEmpleados\":[130,140]}"))
                .andExpect(status().isBadRequest())
                // El legacy decía "Problema(s) al registrar N empleado(s)" sobre un fondo de éxitos
                // parciales. Con @Transactional eso sería mentira: o entraron todos o ninguno.
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("no se guardó ninguno")));
    }

    // ============================================================
    // ESCRITURAS · vacación colectiva
    // ============================================================

    @Test
    @DisplayName("200 · el padrón se abre con CUALQUIERA de los dos botones colectivos")
    void padronConCualquieraDeLosDosBotones() throws Exception {
        // Sólo tiene el de abono grupal. El padrón sirve a los dos asistentes: exigir los dos
        // dejaría afuera a los 4 usuarios que sólo tienen éste.
        when(acceso.tieneBoton(any(), anyInt(), eq("btnNuevoAbonoGrupal"))).thenReturn(true);
        when(acceso.tieneBoton(any(), anyInt(), eq("btnNuevaVacGrupal"))).thenReturn(false);
        when(pDao.padronColectivo(null))
                .thenReturn(new ArrayList<>(Collections.singletonList(delPadron(130L))));

        mvc.perform(post(COL_EMPLEADOS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].codRelEmplEmpr").value(411))
                .andExpect(jsonPath("$.data[0].codSucursal").value(3));
    }

    @Test
    @DisplayName("403 · sin ninguno de los dos botones el padrón no se ve (es dato de RR.HH.)")
    void padronSinNingunBoton() throws Exception {
        when(acceso.tieneBoton(any(), anyInt(), anyString())).thenReturn(false);

        mvc.perform(post(COL_EMPLEADOS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        verify(pDao, never()).padronColectivo(any());
    }

    @Test
    @DisplayName("200 · SIMULAR la vacación colectiva NO escribe, y devuelve los días POR PERSONA")
    void simularVacacionColectivaNoEscribe() throws Exception {
        EmpleadoColectivoDto entra = delPadron(130L);
        entra.setDias(5);
        entra.setDiasTxt("5 días");

        EmpleadoColectivoDto recortado = delPadron(140L);
        recortado.setDatoEmpleado("ROJAS LIMA ANA");
        recortado.setCodSucursal(7);
        recortado.setDias(4.5);        // su sucursal tenía un feriado adentro del rango
        recortado.setDiasTxt("4,5 días");

        when(pDao.simularVacacionColectiva(any(), any(), any(), anyString()))
                .thenReturn(new ArrayList<>(Arrays.asList(entra, recortado)));

        mvc.perform(post(COL_VAC_SIMULAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleados\":[130,140],"
                               + "\"desde\":\"2026-04-06T08:30:00\","
                               + "\"hasta\":\"2026-04-10T18:30:00\","
                               + "\"motivo\":\"Cierre de planta\"}"))
                .andExpect(status().isOk())
                // LO QUE IMPORTA: dos personas, el MISMO rango, DÍAS DISTINTOS. Si esto se
                // aplanara al número del cabezal, la confirmación hablaría de otra cosa que la
                // que se graba.
                .andExpect(jsonPath("$.data[0].dias").value(5.0))
                .andExpect(jsonPath("$.data[1].dias").value(4.5))
                .andExpect(jsonPath("$.data[1].diasTxt").value("4,5 días"));

        verify(pDao, never()).aplicarVacacionColectiva(any(), any(), any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("201 · aplicar firma con el usuario del TOKEN y dice a cuántos se les cargó")
    void aplicarVacacionColectiva() throws Exception {
        when(pDao.aplicarVacacionColectiva(any(), any(), any(), anyString(), eq(YO))).thenReturn(38);

        mvc.perform(post(COL_VAC_APLICAR).contentType(MediaType.APPLICATION_JSON)
                        // El body miente sobre el autorizador: tiene que ganar el del token.
                        .content("{\"codEmpleados\":[130,140],"
                               + "\"desde\":\"2026-04-06T08:30:00\","
                               + "\"hasta\":\"2026-04-10T18:30:00\","
                               + "\"motivo\":\"Cierre de planta\",\"codUsuario\":9999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Se registró la vacación a 38 empleado(s)."));

        ArgumentCaptor<Date> desde = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> hasta = ArgumentCaptor.forClass(Date.class);
        verify(pDao).aplicarVacacionColectiva(eq(Arrays.asList(130L, 140L)),
                desde.capture(), hasta.capture(), eq("Cierre de planta"), eq(YO));

        // LA HORA LLEGA ENTERA, y por esto se captura en vez de mirar con any(): el rango de la
        // vacación colectiva viaja como "2026-04-06T08:30:00" y f_CalcularDiasHabilesPermiso cuenta
        // MINUTOS hábiles entre las dos puntas. Si el deserializador se la come —lo que pasaba
        // cuando "yyyy-MM-dd" estaba antes que los formatos ISO en JacksonConfig, porque
        // SimpleDateFormat.parse acepta el prefijo y descarta el resto SIN error—, el rango llega
        // de medianoche a medianoche, el último día vale cero y a todo el lote se le descuenta un
        // día menos del que le corresponde. Sin este assert, el bug pasa con los 72 tests en verde.
        assertEquals("2026-04-06 08:30:00", conHora(desde.getValue()));
        assertEquals("2026-04-10 18:30:00", conHora(hasta.getValue()));
    }

    /** La fecha como la ve el DAO, con hora. Zona fija: la misma que usa {@code JacksonConfig}. */
    private static String conHora(Date d) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        f.setTimeZone(java.util.TimeZone.getTimeZone("America/La_Paz"));
        return f.format(d);
    }

    @Test
    @DisplayName("403 · sin btnNuevaVacGrupal no se simula ni se aplica la vacación colectiva")
    void vacacionColectivaExigeSuBoton() throws Exception {
        doThrow(new AccessDeniedException("no"))
                .when(acceso).exigirBoton(any(), anyInt(), anyString());

        String cuerpo = "{\"codEmpleados\":[130],\"desde\":\"2026-04-06T08:30:00\","
                      + "\"hasta\":\"2026-04-10T18:30:00\",\"motivo\":\"Cierre\"}";

        mvc.perform(post(COL_VAC_SIMULAR).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        mvc.perform(post(COL_VAC_APLICAR).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());

        verify(pDao, never()).simularVacacionColectiva(any(), any(), any(), anyString());
        verify(pDao, never()).aplicarVacacionColectiva(any(), any(), any(), anyString(), anyLong());
    }

    @Test
    @DisplayName("400 · si la vacación colectiva falla, el mensaje dice que NO quedó ninguna fila")
    void aplicarVacacionColectivaTodoONada() throws Exception {
        when(pDao.aplicarVacacionColectiva(any(), any(), any(), anyString(), anyLong()))
                .thenThrow(new SpBusinessException(
                        "No se pudo registrar la vacación colectiva: no se guardó ninguna."));

        mvc.perform(post(COL_VAC_APLICAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleados\":[130],\"desde\":\"2026-04-06T08:30:00\","
                               + "\"hasta\":\"2026-04-10T18:30:00\",\"motivo\":\"Cierre\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("no se guardó ninguna")));
    }

    // ============================================================
    // NÓMINA DE PERMISOS · lecturas
    // ============================================================

    @Test
    @DisplayName("200 · el kardex devuelve las 8 columnas de la grilla y NINGUNA de deuda")
    void kardexOk() throws Exception {
        when(pDao.kardex(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(kardexDe(8462L, "vac", 1.125))));

        mvc.perform(post(KARDEX).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,"
                               + "\"tipoPermiso\":\"vac\",\"desde\":\"2026-01-01\","
                               + "\"hasta\":\"2026-12-31\",\"fecRango\":\"2026-08-11\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].datoTipoPermiso").value("Vacación"))
                .andExpect(jsonPath("$.data[0].cantidadDias").value(1.125))
                // Las horas son días*8 y salen del servidor ya formateadas: si el cliente
                // redondeara por su cuenta, la misma fila diría 1,13 días y 9 horas.
                .andExpect(jsonPath("$.data[0].horas").value(9.0))
                .andExpect(jsonPath("$.data[0].horasTxt").value("9 horas"))
                // "Deuda En Día(s)" / "Deuda En Hr(s)" NO se portaron: trh_repper tiene 0 filas y
                // la fórmula del SP devuelve el NEGATIVO de los días. Si alguien mapea la columna,
                // esto se cae antes de que la grilla muestre deudas de -2 días.
                .andExpect(jsonPath("$.data[0].diasAdeudados").doesNotExist());

        // Los cuatro filtros llegan al DAO TAL CUAL. Sin `tipoPermiso` y `fecRango` declarados en el
        // DTO, Jackson los descartaba en silencio y la grilla salía sin filtrar con un 200 alegre.
        ArgumentCaptor<Date> fecRango = ArgumentCaptor.forClass(Date.class);
        verify(pDao).kardex(eq(130L), eq(411L), eq("vac"), any(), any(), fecRango.capture());
        assertEquals("2026-08-11 00:00:00", conHora(fecRango.getValue()));
    }

    @Test
    @DisplayName("400 · el kardex sin empleado se corta acá: la ACCION 'Q' sin filtro trae 8.459 filas")
    void kardexSinEmpleado() throws Exception {
        mvc.perform(post(KARDEX).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Debe seleccionar un empleado."));

        verify(pDao, never()).kardex(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("200 · 'Buscar Vac Ganadas' es la búsqueda por RANGO, no el historial por aniversario")
    void vacacionesGanadasOk() throws Exception {
        when(vacDao.buscarPorRango(anyLong(), any(), any(), any()))
                .thenReturn(new ArrayList<>(Collections.singletonList(vacacionDe(880L, 15))));

        mvc.perform(post(VAC_GANADAS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"codRelEmplEmpr\":411,"
                               + "\"desde\":\"2026-01-01\",\"hasta\":\"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].codVacacionAsignada").value(880));

        verify(vacDao).buscarPorRango(eq(130L), eq(411L), any(), any());
        // Son dos preguntas distintas: el historial mete filas SINTÉTICAS por aniversario y esta
        // grilla muestra lo que hay en la tabla. Confundirlas llena la nómina de filas inventadas.
        verify(vacDao, never()).historial(anyLong(), any(), any());
    }

    @Test
    @DisplayName("200 · el combo excluye vac y pva POR DEFECTO, y los incluye sólo si se pide")
    void comboDeTipos() throws Exception {
        when(pDao.tiposPermiso(anyBoolean())).thenReturn(new ArrayList<>(Arrays.asList(
                new TipoPermisoDto("baja", "Baja por Enfermedad"),
                new TipoPermisoDto("otro", "Otros"))));

        mvc.perform(post(TIPOS).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].codigo").value("baja"));

        // El default es el EXCLUYENTE: un flag olvidado deja al filtro sin dos opciones —se ve—,
        // no al modal ofreciendo "Vacación" a quien no tiene btnProgramarVacacion.
        verify(pDao).tiposPermiso(false);

        mvc.perform(post(TIPOS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incluirVacacionYPago\":true}"))
                .andExpect(status().isOk());
        verify(pDao).tiposPermiso(true);
    }

    // ============================================================
    // /permisos/calcular — el que NO escribe
    // ============================================================

    @Test
    @DisplayName("200 · CALCULAR NO ESCRIBE: devuelve horas, días y horas a reponer, y no toca nada")
    void calcularNoEscribeNada() throws Exception {
        EmpleadoColectivoDto previo = delPadron(130L);
        previo.setDias(1.125);
        previo.setDiasTxt("1,13 días");
        previo.setHoras(9.0);
        previo.setHorasTxt("9 horas");
        previo.setHorasAReponer(9.0);          // tipo 'otro' → hay que reponer las horas
        previo.setHorasAReponerTxt("9 horas");
        when(pDao.previsualizarPermiso(anyLong(), any(), any(), any())).thenReturn(previo);

        mvc.perform(post(CALCULAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                               + "\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-17T18:00:00\"}"))
                .andExpect(status().isOk())
                // OBJETO, no lista: es una persona. Del lado Flutter se eligen helpers distintos.
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.dias").value(1.125))
                .andExpect(jsonPath("$.data.horas").value(9.0))
                .andExpect(jsonPath("$.data.horasAReponer").value(9.0))
                .andExpect(jsonPath("$.data.entra").value(true));

        // LO QUE IMPORTA DE ESTE TEST: la previsualización no llama a NINGÚN método de escritura.
        // El campo se recalcula con cada tecla; si alguien lo "optimiza" reutilizando el alta,
        // mover una hora en el modal grabaría una fila.
        verify(pDao, never()).registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong());
        verify(pDao, never()).registrarVacacionPagada(anyLong(), any(), anyDouble(), any(),
                anyLong(), anyBoolean());
        verify(pDao, never()).aplicarVacacionColectiva(any(), any(), any(), anyString(), anyLong());
        verifyNoInteractions(vacDao);
        verifyNoInteractions(abonoDao);
    }

    @Test
    @DisplayName("400 · calcular sin empleado no llega a la base")
    void calcularSinEmpleado() throws Exception {
        mvc.perform(post(CALCULAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-17T18:00:00\"}"))
                .andExpect(status().isBadRequest());

        verify(pDao, never()).previsualizarPermiso(anyLong(), any(), any(), any());
    }

    // ============================================================
    // ESCRITURAS · las tres altas individuales
    // ============================================================

    @Test
    @DisplayName("201 · alta de permiso: el tipo viaja, la hora entera, y la firma sale del TOKEN")
    void altaDePermiso() throws Exception {
        when(pDao.registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong()))
                .thenReturn(kardexDe(8500L, "otro", 1.125));

        mvc.perform(post(PERM_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        // El body MIENTE sobre quién firma: manda 9999. Tiene que ganar el 7 del token.
                        .content("{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                               + "\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-17T18:00:00\","
                               + "\"motivo\":\"Trámite personal\",\"codUsuario\":9999}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                // La fila releída, no un id: p_abm_Permiso no devuelve SCOPE_IDENTITY.
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.codPermiso").value(8500));

        ArgumentCaptor<Date> desde = ArgumentCaptor.forClass(Date.class);
        ArgumentCaptor<Date> hasta = ArgumentCaptor.forClass(Date.class);
        verify(pDao).registrarPermiso(eq(130L), eq("otro"), desde.capture(), hasta.capture(),
                eq("Trámite personal"), eq(YO));
        // La hora NO es decorativa: f_CalcularDiasHabilesPermiso conmuta a jornada continua cuando
        // `hasta` pasa de las 17:30. Perderla acá cambia el número de días que se graba.
        assertEquals("2026-08-17 08:00:00", conHora(desde.getValue()));
        assertEquals("2026-08-17 18:00:00", conHora(hasta.getValue()));
    }

    @Test
    @DisplayName("400 · 'vac' NO entra por /permisos/registrar: es otro botón del ACL")
    void permisoNoAceptaVacacion() throws Exception {
        mvc.perform(post(PERM_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"tipoPermiso\":\"VAC\","
                               + "\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-20T10:00:00\",\"motivo\":\"Vacación\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("programar vacación")));

        // btnProgramarPermiso lo tienen 4 usuarios y btnProgramarVacacion 5: sin este corte, un
        // tipoPermiso:"vac" le daría a los 4 una atribución que el ACL no les dio.
        verify(pDao, never()).registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("201 · la vacación individual pone el tipo EN EL SERVIDOR e ignora el del body")
    void altaDeVacacionIndividual() throws Exception {
        when(pDao.registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong()))
                .thenReturn(kardexDe(8501L, "vac", 3.25));

        mvc.perform(post(VAC_IND_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        // El body intenta colar "otro": si el tipo saliera de acá, esta ruta sería
                        // un segundo camino para dar de alta cualquier permiso salteándose su botón.
                        .content("{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                               + "\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-20T10:00:00\",\"motivo\":\"Vacación anual\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipoPermiso").value("vac"));

        verify(pDao).registrarPermiso(eq(130L), eq("vac"), any(), any(), eq("Vacación anual"), eq(YO));
    }

    @Test
    @DisplayName("403 · permiso y vacación NO comparten botón: con uno solo, la otra ruta corta")
    void losDosModalesTienenBotonesDISTINTOS() throws Exception {
        when(pDao.registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong()))
                .thenReturn(kardexDe(8501L, "vac", 3.25));
        doThrow(new AccessDeniedException("no")).when(acceso)
                .exigirBoton(any(), anyInt(), eq("btnProgramarPermiso"));

        String cuerpo = "{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                      + "\"desde\":\"2026-08-17T08:00:00\","
                      + "\"hasta\":\"2026-08-20T10:00:00\",\"motivo\":\"Prueba\"}";

        mvc.perform(post(PERM_REGISTRAR).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isForbidden());
        // …y la vacación sigue entrando, porque su botón es otro (5 usuarios contra 4).
        mvc.perform(post(VAC_IND_REGISTRAR).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("400 · el cruce con un permiso ya cargado llega con el motivo puesto, no como 500")
    void permisoQueSeCruza() throws Exception {
        when(pDao.registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong()))
                .thenThrow(new SpBusinessException("Ya tiene un permiso cargado que se cruza con "
                        + "ese rango (del 17/08/2026 08:00 al 20/08/2026 10:00)."));

        mvc.perform(post(PERM_REGISTRAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                               + "\"desde\":\"2026-08-17T08:00:00\","
                               + "\"hasta\":\"2026-08-20T10:00:00\",\"motivo\":\"Prueba\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("se cruza con ese rango")))
                // No es confirmable: el usuario no lo arregla apretando "Confirmar", tiene que
                // cambiar las fechas o borrar el otro permiso.
                .andExpect(jsonPath("$.confirmable").doesNotExist());
    }

    // ============================================================
    // ESCRITURAS · vacación PAGADA (esto paga plata)
    // ============================================================

    @Test
    @DisplayName("201 · el pago usa `dias` y `fecha` del body, y el autorizador del TOKEN")
    void pagoDeVacacion() throws Exception {
        when(pDao.registrarVacacionPagada(anyLong(), any(), anyDouble(), any(), anyLong(),
                anyBoolean())).thenReturn(kardexDe(8502L, "pva", 5.0));

        mvc.perform(post(VAC_PAGAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"fecha\":\"2026-08-12\",\"dias\":5,"
                               + "\"motivo\":\"Pago de vacación 2025\",\"confirmado\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipoPermiso").value("pva"))
                .andExpect(jsonPath("$.data.cantidadDias").value(5.0));

        // `dias` y no un `diasAPagar` inventado: un segundo nombre para lo mismo es la falla que ya
        // pasó con `diasAsignados` —se descartaba en silencio y se grababan CERO días—.
        verify(pDao).registrarVacacionPagada(eq(130L), any(), eq(5.0), eq("Pago de vacación 2025"),
                eq(YO), eq(true));
    }

    @Test
    @DisplayName("400 confirmable · el primer POST devuelve el impacto (saldo antes y después)")
    void pagoPideConfirmacionConElImpacto() throws Exception {
        when(pDao.registrarVacacionPagada(anyLong(), any(), anyDouble(), any(), anyLong(),
                anyBoolean())).thenThrow(new SpConfirmableException(
                        "Le va a pagar 40 días y su saldo es de 12,5: quedaría en -27,5."));

        mvc.perform(post(VAC_PAGAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"fecha\":\"2026-08-12\",\"dias\":40,"
                               + "\"motivo\":\"Pago excepcional\"}"))
                .andExpect(status().isBadRequest())
                // El marcador es lo que le dice al cliente que ofrezca "Confirmar". Sin él tendría
                // que adivinar por el TEXTO, y esta es la pantalla donde eso cuesta plata.
                .andExpect(jsonPath("$.confirmable").value(true))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("quedaría en")));
    }

    @Test
    @DisplayName("409 · doble toque en el pago: se rechaza AUNQUE el cuerpo venga confirmado")
    void pagoDobleToque() throws Exception {
        when(pDao.registrarVacacionPagada(anyLong(), any(), anyDouble(), any(), anyLong(),
                anyBoolean())).thenThrow(new SpConflictException(
                        "Ese mismo pago de vacación acaba de registrarse (permiso 8502). "
                      + "No se cargó de nuevo."));

        mvc.perform(post(VAC_PAGAR).contentType(MediaType.APPLICATION_JSON)
                        // El segundo toque del teléfono manda EXACTAMENTE el mismo cuerpo, con la
                        // confirmación adentro. Si el flag lo dejara pasar, se pagan dos veces.
                        .content("{\"codEmpleado\":130,\"fecha\":\"2026-08-12\",\"dias\":5,"
                               + "\"motivo\":\"Pago de vacación 2025\",\"confirmado\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("acaba de registrarse")));
    }

    @Test
    @DisplayName("403 · sin botón no se registra ni un permiso, ni una vacación, ni un pago")
    void sinPermisoNoEscribeNingunaIndividual() throws Exception {
        doThrow(new AccessDeniedException("no"))
                .when(acceso).exigirBoton(any(), anyInt(), anyString());

        String rango = "{\"codEmpleado\":130,\"tipoPermiso\":\"otro\","
                     + "\"desde\":\"2026-08-17T08:00:00\","
                     + "\"hasta\":\"2026-08-20T10:00:00\",\"motivo\":\"Prueba\"}";

        mvc.perform(post(PERM_REGISTRAR).contentType(MediaType.APPLICATION_JSON).content(rango))
                .andExpect(status().isForbidden());
        mvc.perform(post(VAC_IND_REGISTRAR).contentType(MediaType.APPLICATION_JSON).content(rango))
                .andExpect(status().isForbidden());
        mvc.perform(post(VAC_PAGAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"fecha\":\"2026-08-12\",\"dias\":5,"
                               + "\"motivo\":\"Pago\"}"))
                .andExpect(status().isForbidden());
        // Y las lecturas de la nómina tampoco: es el kardex de una persona, no un dato público.
        mvc.perform(post(KARDEX).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130}"))
                .andExpect(status().isForbidden());

        verify(pDao, never()).registrarPermiso(anyLong(), any(), any(), any(), any(), anyLong());
        verify(pDao, never()).registrarVacacionPagada(anyLong(), any(), anyDouble(), any(),
                anyLong(), anyBoolean());
        verify(pDao, never()).kardex(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("400 · sin identidad no se paga: audUsuarioI es NOT NULL y la firma sale del token")
    void sinIdentidadNoSePaga() throws Exception {
        when(acceso.miPermiso(any())).thenReturn(new MiEquipoDto());   // codUsuario null

        mvc.perform(post(VAC_PAGAR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codEmpleado\":130,\"fecha\":\"2026-08-12\",\"dias\":5,"
                               + "\"motivo\":\"Pago\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("identificar su usuario")));

        verify(pDao, never()).registrarVacacionPagada(anyLong(), any(), anyDouble(), any(),
                anyLong(), anyBoolean());
    }

    // ============================================================
    // Andamiaje
    // ============================================================

    private PermisoKardexDto kardexDe(long cod, String tipo, double dias) {
        PermisoKardexDto k = new PermisoKardexDto();
        k.setCodPermiso(cod);
        k.setCodEmpleado(130L);
        k.setCodRelEmplEmpr(411L);
        k.setTipoPermiso(tipo);
        k.setDatoTipoPermiso("vac".equals(tipo) ? "Vacación"
                           : "pva".equals(tipo) ? "Pago Vacación" : "Otros");
        k.setDesde(new Date());
        k.setHasta(new Date());
        k.setMotivo("Trámite personal");
        k.setCantidadDias(dias);
        k.setHoras(dias * 8.0);
        k.setCantidadDiasTxt(dias + " días");
        k.setHorasTxt(dias * 8.0 == 9.0 ? "9 horas" : (dias * 8.0) + " horas");
        return k;
    }

    private EmpleadoColectivoDto delPadron(long cod) {
        EmpleadoColectivoDto d = new EmpleadoColectivoDto();
        d.setCodEmpleado(cod);
        d.setDatoEmpleado("PEREZ GOMEZ JUAN CARLOS");
        d.setCodRelEmplEmpr(411L);
        d.setCodSucursal(3L);
        d.setDatoSucursal("PLANTA");
        d.setDatoEmpresa("IMPEXPAP");
        d.setDiasTxt("0 días");
        d.setEntra(true);
        d.setDetalle("");
        return d;
    }

    private VacacionAsignada vacacionDe(long cod, double dias) {
        VacacionAsignada v = new VacacionAsignada();
        v.setCodVacacionAsignada(cod);
        v.setCodEmpleado(130L);
        v.setCodRelEmplEmpr(411L);
        v.setDiasAsignados(dias);
        v.setDiasAsignadosTxt(dias == 15 ? "15 días" : String.valueOf(dias));
        v.setMotivo("Aniversario 2026");
        v.setFecha(new Date());
        v.setSintetico(cod <= 0);
        return v;
    }

    private AbonoDias abonoDe(long cod, double dias) {
        AbonoDias a = new AbonoDias();
        a.setCodAbonoDias(cod);
        a.setCodEmpleado(130L);
        a.setCodRelEmplEmpr(411L);
        a.setDiasAbonados(dias);
        a.setDiasAbonadosTxt(dias == 2.5 ? "2,5 días" : String.valueOf(dias));
        a.setMotivo("Compensación día del niño");
        a.setFecha(new Date());
        return a;
    }

    private FichaSaldoDto fichaDe(long cod, double noUsados, double abonados) {
        FichaSaldoDto d = new FichaSaldoDto();
        d.setCodEmpleado(cod);
        d.setDatoEmpleado("PEREZ GOMEZ JUAN CARLOS");
        d.setDiasNoUsados(noUsados);
        d.setDiasAbonados(abonados);
        d.setTotalDias(noUsados + abonados);
        d.setDiasNoUsadosTxt("12,5 días");
        d.setDiasAbonadosTxt("2 días");
        d.setTotalDiasTxt("14,5 días");
        d.setDatoRelEmplEmprVigente(411L);
        d.setTieneRelacionesAnteriores(true);
        d.setMovimientosFueraDeRelacionVigente(184);
        return d;
    }

    private DesgloseSaldoDto desgloseDe() {
        DesgloseSaldoDto r = new DesgloseSaldoDto();
        r.setCodEmpleado(130L);
        r.setDatoAnios(11);
        r.setTieneFechaBeneficio(true);
        r.setDesgloseAplicable(true);
        r.setTramos(new ArrayList<>(Arrays.asList(
                TramoSaldoDto.de(TramoSaldoDto.SALDO_PENULTIMO,
                        "Saldo hasta el penultimo año cumplido (28/02/2025 )", 8.0, TramoSaldoDto.DEBE),
                TramoSaldoDto.de(TramoSaldoDto.ASIGNADA_ANIO,
                        "Vacación correspondiente a su 10º año cumplido  (01/03/2025 al 28/02/2026 )", 30.0, TramoSaldoDto.DEBE),
                TramoSaldoDto.de(TramoSaldoDto.ACUMULADA,
                        "Vacación acumulada desde su ultimo año cumplido (01/03/2026 ) hasta hoy ", 12.9, TramoSaldoDto.INFO),
                TramoSaldoDto.de(TramoSaldoDto.UTILIZADA,
                        "Vacación utilizada desde el (28/02/2025 ) hasta hoy ", 22.0, TramoSaldoDto.HABER),
                TramoSaldoDto.de(TramoSaldoDto.PROGRAMADA,
                        "Vacación programada a partir de hoy 11/08/2026", 3.5, TramoSaldoDto.HABER))));
        r.setMontoTotalDebe(38.0);
        r.setMontoTotalHaber(25.5);
        r.setMontoTotal(12.5);
        r.setMontoTotalTxt("12,5 días");
        r.setEtiquetaTotal(DesgloseSaldoDto.ETIQUETA_TOTAL);
        return r;
    }
}
