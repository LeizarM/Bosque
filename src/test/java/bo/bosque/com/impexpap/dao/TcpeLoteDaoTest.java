package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@code notaAprobacion} y {@code mostrarRespuestas} hacia {@code p_abm_tcpe_Lote} I y U (contrato 6.5). */
class TcpeLoteDaoTest {

    private SpHelper sp;
    private TcpeLoteDao dao;

    @BeforeEach
    void preparar() {
        sp = mock(SpHelper.class);
        when(sp.ejecutarAbmMap(anyString(), anyMap(), anyString())).thenReturn(new RespuestaSp(0, "", 30L));
        dao = new TcpeLoteDao(sp);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> enviado(String accion) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarAbmMap(eq("p_abm_tcpe_Lote"), p.capture(), eq(accion));
        return p.getValue();
    }

    @Test
    @DisplayName("I sin nota ni mostrarRespuestas: no se mandan y el SP pone los de f_tcpe_config")
    void altaSinDefectosEnJava() {
        dao.crear(new TcpeLoteGuardarDto(), 7L);

        Map<String, Object> p = enviado("I");
        assertFalse(p.containsKey("notaAprobacion"));
        assertFalse(p.containsKey("mostrarRespuestas"));
        assertEquals(0L, p.get("codRolExamen"));
        assertEquals(7L, p.get("audUsuario"));
    }

    @Test
    @DisplayName("V de un lote nuevo: los datos de I (duración incluida) y los tres conteos juntos (lote mixto)")
    void validar() {
        TcpeLoteGuardarDto d = new TcpeLoteGuardarDto();
        d.setModalidad("E");
        d.setCantidadPreg(12);
        d.setCodClasificacion(4L);
        d.setDuracionMinutos(45);

        dao.validar(d, 5, 3, 2);

        Map<String, Object> p = enviado("V");
        assertEquals("E", p.get("modalidad"));
        assertEquals(12, p.get("cantidadPreg"));
        assertEquals(4L, p.get("codClasificacion"));
        assertEquals(45, p.get("duracionMinutos"));
        assertEquals(5, p.get("cantidad"));
        assertEquals(3, p.get("cantPersonas"));
        assertEquals(2, p.get("cantEmpleados"));
        assertFalse(p.containsKey("codRolExamen"), "sin codRolExamen: V valida un lote nuevo");
        assertFalse(p.containsKey("audUsuario"));
    }

    @Test
    @DisplayName("V para agregar: codRolExamen y los tres conteos; modalidad y categoría las pone el SP")
    void validarAgregar() {
        dao.validarAgregar(30L, 0, 4, 1);

        Map<String, Object> p = enviado("V");
        assertEquals(30L, p.get("codRolExamen"));
        assertEquals(0, p.get("cantidad"));
        assertEquals(4, p.get("cantPersonas"));
        assertEquals(1, p.get("cantEmpleados"));
        assertFalse(p.containsKey("modalidad"));
    }

    @Test
    @DisplayName("I con los valores del pedido: pasan tal cual")
    void altaConValores() {
        TcpeLoteGuardarDto d = new TcpeLoteGuardarDto();
        d.setNotaAprobacion(new BigDecimal("70.5"));
        d.setMostrarRespuestas(false);

        dao.crear(d, 7L);

        Map<String, Object> p = enviado("I");
        assertEquals(new BigDecimal("70.5"), p.get("notaAprobacion"));
        assertEquals(Boolean.FALSE, p.get("mostrarRespuestas"));
    }

    @Test
    @DisplayName("U: con valor se manda; nulo no se manda (el SP conserva lo que había)")
    void actualizar() {
        TcpeLoteGuardarDto d = new TcpeLoteGuardarDto();
        d.setCodRolExamen(30L);
        d.setNotaAprobacion(new BigDecimal("60"));

        dao.actualizar(d, 7L);

        Map<String, Object> p = enviado("U");
        assertEquals(30L, p.get("codRolExamen"));
        assertEquals(new BigDecimal("60"), p.get("notaAprobacion"));
        assertFalse(p.containsKey("mostrarRespuestas"));
    }
}
