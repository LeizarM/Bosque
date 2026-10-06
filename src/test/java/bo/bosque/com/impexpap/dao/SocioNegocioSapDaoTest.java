package bo.bosque.com.impexpap.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import bo.bosque.com.impexpap.utils.SpHelper;

class SocioNegocioSapDaoTest {

    @Test
    @DisplayName("llama a p_abm_SocioNegocio rama B, por nombre y SIN salidas, mandando SOLO el usuario (el resto queda en el DEFAULT del procedimiento)")
    @SuppressWarnings("unchecked")
    void llamaALaRamaB() {
        SpHelper sp = mock(SpHelper.class);

        new SocioNegocioSapDao(sp).actualizarDesdeSap(7);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarSinSalidas(eq("p_abm_SocioNegocio"), params.capture(), eq("B"));
        assertEquals(Collections.<String, Object>singletonMap("audUsuario", 7), params.getValue());
    }
}
