package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BitTareaRuti;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mis tareas (archivo SQL 104): la rama M con solo la persona. Sin el 104 en la base, la M no
 * devuelve resultados y falla con un mensaje claro: nunca cae a la L, que traería todo el historial.
 */
class BitTareaRutiDaoMisTareasTest {

    @Test
    @SuppressWarnings("unchecked")
    void pideLaRamaMConSoloLaPersona() {
        SpHelper sp = mock(SpHelper.class);
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        List<BitTareaRuti> filas = Collections.singletonList(new BitTareaRuti());
        when(sp.ejecutarListado(eq("p_list_tac_BitTareaRuti"), params.capture(), eq("M"), eq(BitTareaRuti.class)))
                .thenReturn(filas);

        assertEquals(filas, new BitTareaRutiDao(sp).misTareas(24L));
        assertEquals(Collections.singletonMap("codEmpleado", 24L), params.getValue());
        verify(sp, never()).ejecutarListado(any(String.class), any(Map.class), eq("L"), eq(BitTareaRuti.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sinElSql104FallaYNoCaeALaL() {
        SpHelper sp = mock(SpHelper.class);
        when(sp.ejecutarListado(eq("p_list_tac_BitTareaRuti"), any(Map.class), eq("M"), eq(BitTareaRuti.class)))
                .thenThrow(new RuntimeException("The statement did not return a result set."));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new BitTareaRutiDao(sp).misTareas(24L));
        assertTrue(e.getMessage().contains("SQL 104"));
        verify(sp, never()).ejecutarListado(any(String.class), any(Map.class), eq("L"), eq(BitTareaRuti.class));
    }
}
