package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.model.TcpeArea;
import bo.bosque.com.impexpap.model.TcpeConocimientosPost;
import bo.bosque.com.impexpap.model.TcpeEmpleado;
import bo.bosque.com.impexpap.model.TcpeExamen;
import bo.bosque.com.impexpap.model.TcpePostulacion;
import bo.bosque.com.impexpap.model.TcpeRolExamen;
import bo.bosque.com.impexpap.model.TcpeUsuario;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qué parámetros le llegan a cada SP {@code tcpe_} desde el DAO genérico. Es lo que más fácil se rompe sin que
 * nada falle en compilación: un PK en 0 como filtro (cero filas), un parámetro que el SP no declara (error de
 * SQL Server) o {@code password2} viajando por el ABM genérico.
 */
class TcpeCrudDaoTest {

    private SpHelper sp;

    @BeforeEach
    void preparar() {
        sp = mock(SpHelper.class);
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "", 9L));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> listadoEnviado(String spName, Class<?> tipo) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarListado(eq(spName), p.capture(), eq("L"), eq((Class<Object>) tipo));
        return p.getValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> abmEnviado(String spName, String accion) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarAbmMap(eq(spName), p.capture(), eq(accion));
        return p.getValue();
    }

    @Test
    @DisplayName("listar no manda un PK en 0 ni los nulos; sí manda activo = false")
    void listarSinCeros() {
        TcpeArea filtro = new TcpeArea();
        filtro.setCodArea(0L);
        filtro.setActivo(false);
        filtro.setNombreArea("no es filtro");

        new TcpeAreaDao(sp).listar(filtro);

        Map<String, Object> p = listadoEnviado("p_list_tcpe_Area", TcpeArea.class);
        assertEquals(Collections.singletonMap("activo", false), p);
    }

    @Test
    @DisplayName("guardar con PK nula es I: PK en 0, sin nulos, audUsuario del parámetro")
    void guardarAlta() {
        TcpeArea a = new TcpeArea();
        a.setNombreArea("Contabilidad");
        a.setAudUsuario(999L);   // lo que diga el cuerpo no cuenta

        new TcpeAreaDao(sp).guardar(a, 7L);

        Map<String, Object> p = abmEnviado("p_abm_tcpe_Area", "I");
        assertEquals(0L, p.get("codArea"));
        assertEquals("Contabilidad", p.get("nombreArea"));
        assertEquals(7L, p.get("audUsuario"));
        assertFalse(p.containsKey("descripArea"));
        assertFalse(p.containsKey("nombreUsuario"));
        assertFalse(p.containsKey("audFecha"));
    }

    @Test
    @DisplayName("guardar con PK es U; una fecha LocalDate viaja como java.sql.Date")
    void guardarModificacionYFecha() {
        TcpeRolExamen r = new TcpeRolExamen();
        r.setCodRolExamen(15L);
        r.setFecha(LocalDate.of(2026, 10, 19));
        r.setHoraInicio("08:30:00.0000000");

        new TcpeRolExamenDao(sp).guardar(r, 7L);

        Map<String, Object> p = abmEnviado("p_abm_tcpe_RolExamen", "U");
        assertEquals(15L, p.get("codRolExamen"));
        assertEquals(java.sql.Date.valueOf("2026-10-19"), p.get("fecha"));
        assertEquals("08:30:00", p.get("horaInicio"));
    }

    @Test
    @DisplayName("examen U lleva las columnas v5 del panel En vivo (si no, la actualización completa las borra)")
    void examenConColumnasV5() {
        TcpeExamen e = new TcpeExamen();
        e.setCodExamen(80L);
        e.setPreguntaActual(501L);
        e.setSalidasPestana(3);
        e.setUltimaSalida(new java.util.Date(0));

        new TcpeExamenDao(sp).guardar(e, 7L);

        Map<String, Object> p = abmEnviado("p_abm_tcpe_Examen", "U");
        assertEquals(501L, p.get("preguntaActual"));
        assertEquals(3, p.get("salidasPestana"));
        assertTrue(p.containsKey("ultimaSalida"));
        assertFalse(p.containsKey("ultimaActividad"), "lo nulo no viaja");
    }

    @Test
    @DisplayName("password2 nunca viaja por el ABM genérico de usuario")
    void sinPassword() {
        TcpeUsuario u = new TcpeUsuario();
        u.setCodUsuario(3L);
        u.setLogin("IPX-3");
        u.setPassword2("texto-plano");

        new TcpeUsuarioDao(sp).guardar(u, 7L);

        Map<String, Object> p = abmEnviado("p_abm_tcpe_Usuario", "U");
        assertFalse(p.containsKey("password2"));
        assertEquals("IPX-3", p.get("login"));
    }

    @Test
    @DisplayName("PK sin IDENTITY: sin código es I (el SP numera); con código, I o U según exista")
    void pkManual() {
        TcpeEmpleadoDao dao = new TcpeEmpleadoDao(sp);
        dao.guardar(new TcpeEmpleado(), 7L);
        Map<String, Object> alta = abmEnviado("p_abm_tcpe_Empleado", "I");
        assertEquals(0L, alta.get("codEmpleado"));
        verify(sp, never()).ejecutarListado(anyString(), anyMap(), anyString(), eq(TcpeEmpleado.class));

        TcpeEmpleado e = new TcpeEmpleado();
        e.setCodEmpleado(50L);
        when(sp.ejecutarListado(eq("p_list_tcpe_Empleado"), anyMap(), eq("L"), eq(TcpeEmpleado.class)))
                .thenReturn(Collections.emptyList());
        dao.guardar(e, 7L);
        verify(sp, org.mockito.Mockito.times(2)).ejecutarAbmMap(eq("p_abm_tcpe_Empleado"), anyMap(), eq("I"));

        when(sp.ejecutarListado(eq("p_list_tcpe_Empleado"), anyMap(), eq("L"), eq(TcpeEmpleado.class)))
                .thenReturn(Collections.singletonList(e));
        dao.guardar(e, 7L);
        verify(sp).ejecutarAbmMap(eq("p_abm_tcpe_Empleado"), anyMap(), eq("U"));
    }

    @Test
    @DisplayName("las tres tablas sin IDENTITY consultan su p_list solo por PK antes de elegir I o U")
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void pkManualConsultaPorPk() {
        Object[][] casos = {
                { TcpeConocimientosPostDao.class, new TcpeConocimientosPost(), "ConocimientosPost", "codConocimiento" },
                { TcpeEmpleadoDao.class, new TcpeEmpleado(), "Empleado", "codEmpleado" },
                { TcpePostulacionDao.class, new TcpePostulacion(), "Postulacion", "codPostulacion" },
        };
        for (Object[] c : casos) {
            sp = mock(SpHelper.class);
            when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "", 31L));
            TcpeCrudDao dao;
            try {
                dao = (TcpeCrudDao) ((Class<?>) c[0]).getConstructor(SpHelper.class).newInstance(sp);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
            Object modelo = c[1];
            String sufijo = (String) c[2];
            String pk = (String) c[3];
            PropertyAccessorFactory.forBeanPropertyAccess(modelo).setPropertyValue(pk, 31L);

            // No existe: I con el PK que cargó el usuario.
            when(sp.ejecutarListado(eq("p_list_tcpe_" + sufijo), anyMap(), eq("L"), eq((Class) modelo.getClass())))
                    .thenReturn(Collections.emptyList());
            dao.guardar(modelo, 7L);
            ArgumentCaptor<Map<String, Object>> filtro = ArgumentCaptor.forClass(Map.class);
            verify(sp).ejecutarListado(eq("p_list_tcpe_" + sufijo), filtro.capture(), eq("L"), eq((Class) modelo.getClass()));
            assertEquals(Collections.singletonMap(pk, 31L), filtro.getValue(), sufijo + ": el p_list va solo con el PK");
            assertEquals(31L, abmEnviado("p_abm_tcpe_" + sufijo, "I").get(pk), sufijo + ": I conserva el PK");

            // Ya existe: U.
            when(sp.ejecutarListado(eq("p_list_tcpe_" + sufijo), anyMap(), eq("L"), eq((Class) modelo.getClass())))
                    .thenReturn(Collections.singletonList(modelo));
            dao.guardar(modelo, 7L);
            verify(sp).ejecutarAbmMap(eq("p_abm_tcpe_" + sufijo), anyMap(), eq("U"));
        }
    }

    @Test
    @DisplayName("eliminar manda solo PK y audUsuario, y rechaza un PK vacío")
    void eliminar() {
        TcpeAreaDao dao = new TcpeAreaDao(sp);
        assertThrows(SpBusinessException.class, () -> dao.eliminar(0L, 7L));

        dao.eliminar(4L, 7L);
        Map<String, Object> p = abmEnviado("p_abm_tcpe_Area", "D");
        assertEquals(2, p.size());
        assertEquals(4L, p.get("codArea"));
        assertEquals(7L, p.get("audUsuario"));
    }

    @Test
    @DisplayName("hay 40 DAO tcpe_, uno por tabla, y cada filtro y columna existe en su modelo")
    void losCuarentaSonCoherentes() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AssignableTypeFilter(TcpeCrudDao.class));

        List<String> problemas = new ArrayList<>();
        Set<String> entidades = new HashSet<>();
        int cantidad = 0;
        for (BeanDefinition bd : scanner.findCandidateComponents("bo.bosque.com.impexpap.dao")) {
            Class<?> clase = Class.forName(bd.getBeanClassName());
            TcpeCrudDao<?> dao = (TcpeCrudDao<?>) clase.getConstructor(SpHelper.class).newInstance(sp);
            cantidad++;
            if (!entidades.add(dao.entidad())) problemas.add("entidad repetida: " + dao.entidad());
            if (!ITcpeCrud.class.isAssignableFrom(clase)) problemas.add(clase.getSimpleName() + " sin interfaz");

            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(dao.tipo().getDeclaredConstructor().newInstance());
            List<String> props = new ArrayList<>(dao.filtros());
            props.addAll(dao.columnas());
            props.add(dao.pk());
            props.add("audUsuario");
            props.add("audFecha");
            props.add("nombreUsuario");
            for (String prop : props) {
                if (!bw.isWritableProperty(prop) || !bw.isReadableProperty(prop)) {
                    problemas.add(clase.getSimpleName() + ": el modelo no tiene " + prop);
                }
            }
            if (!dao.filtros().contains(dao.pk())) problemas.add(clase.getSimpleName() + ": el PK no es filtro");
            if (dao.columnas().contains("password2")) problemas.add(clase.getSimpleName() + ": manda password2");
        }
        assertEquals(40, cantidad, "DAO tcpe_ encontrados");
        assertTrue(problemas.isEmpty(), String.join("\n", problemas));
    }

    @Test
    @DisplayName("un DAO sin modelo en el cuerpo lista todo (sin filtros)")
    void listarSinFiltro() {
        new TcpeAreaDao(sp).listar(null);
        assertTrue(listadoEnviado("p_list_tcpe_Area", TcpeArea.class).isEmpty());
        verify(sp, never()).ejecutarAbmMap(anyString(), any(), anyString());
    }
}
