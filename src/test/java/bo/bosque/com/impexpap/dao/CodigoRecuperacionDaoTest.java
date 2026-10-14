package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CodigoRecuperacion;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Cada acción de p_abm_trec_CodigoRecuperacion recibe exactamente los parámetros que declara el SP. */
class CodigoRecuperacionDaoTest {

    private SpHelper spHelper;
    private CodigoRecuperacionDao dao;

    @BeforeEach
    void setUp() {
        spHelper = mock(SpHelper.class);
        dao = new CodigoRecuperacionDao(spHelper);
    }

    @SuppressWarnings("unchecked")
    private List<String> parametrosDe(String accion) {
        ArgumentCaptor<Map<String, Object>> p = ArgumentCaptor.forClass(Map.class);
        verify(spHelper).ejecutarAbmMap(eq("p_abm_trec_CodigoRecuperacion"), p.capture(), eq(accion));
        return new ArrayList<>(p.getValue().keySet());
    }

    @Test
    void solicitar() {
        dao.solicitar("jperez", "hash", "10.0.0.1");
        assertEquals(Arrays.asList("login", "codigoHash", "ip"), parametrosDe("I"));
    }

    @Test
    void reservarIntento() {
        dao.reservarIntento(20);
        assertEquals(Arrays.asList("idCodigo"), parametrosDe("F"));
    }

    @Test
    void restablecer() {
        dao.restablecer(20, "hash", "10.0.0.1");
        assertEquals(Arrays.asList("idCodigo", "password2", "ip"), parametrosDe("R"));
    }

    @Test
    void eliminar() {
        dao.eliminar(20);
        assertEquals(Arrays.asList("idCodigo"), parametrosDe("D"));
    }

    @Test
    void listados() {
        dao.obtenerPorId(20);
        dao.obtenerVigente("jperez");
        verify(spHelper).ejecutarListado(eq("p_list_trec_CodigoRecuperacion"), anyMap(), eq("L"), eq(CodigoRecuperacion.class));
        verify(spHelper).ejecutarListado(eq("p_list_trec_CodigoRecuperacion"), anyMap(), eq("V"), eq(CodigoRecuperacion.class));
    }
}
