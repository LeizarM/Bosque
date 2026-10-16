package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.FilaSeccionRequest;
import bo.bosque.com.impexpap.dto.FirmarSeccionRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/**
 * Verificar Cierre (archivo SQL 109): otro día se firma o se marca desde la tarea de hoy.
 * El día viaja como @fechaRevisada; sin él, el SP usa el de la tarea, como antes.
 */
class FirmaCierreDaoOtroDiaTest {

    @Test
    @SuppressWarnings("unchecked")
    void firmarManda_elDia_soloSiLlega() {
        SpHelper sp = mock(SpHelper.class);
        when(sp.ejecutarAbmMap(eq("p_abm_tac_FirmaCierre"), any(Map.class), eq("F")))
                .thenReturn(new RespuestaSp(0, "ok", 1));
        FirmaCierreDao dao = new FirmaCierreDao(sp);
        Date dia = new Date(0);

        FirmarSeccionRequest otroDia = new FirmarSeccionRequest();
        otroDia.setIdBitTarea(176009L);
        otroDia.setSeccion("A");
        otroDia.setCodSucursal(20L);
        otroDia.setFecha(dia);
        dao.firmar(otroDia, 22L);
        FirmarSeccionRequest deHoy = new FirmarSeccionRequest();
        deHoy.setIdBitTarea(176009L);
        deHoy.setSeccion("A");
        dao.firmar(deHoy, 22L);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(sp, org.mockito.Mockito.times(2)).ejecutarAbmMap(eq("p_abm_tac_FirmaCierre"), params.capture(), eq("F"));
        assertEquals(dia, params.getAllValues().get(0).get("fechaRevisada"));
        assertEquals(176009L, params.getAllValues().get(0).get("idBitTarea"));
        assertFalse(params.getAllValues().get(1).containsKey("fechaRevisada"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void marcarFilaConFuenteManda_elDia() {
        SpHelper sp = mock(SpHelper.class);
        when(sp.ejecutarAbmMap(eq("p_abm_tac_FirmaCierre"), any(Map.class), eq("G")))
                .thenReturn(new RespuestaSp(0, "ok", 1));
        Date dia = new Date(0);
        FilaSeccionRequest req = new FilaSeccionRequest();
        req.setIdBitTarea(176009L);
        req.setSeccion("G");
        req.setClave("x");
        req.setMarca(1);
        req.setFecha(dia);

        new FirmaCierreDao(sp).marcarFilaSeccion(req, 22L);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarAbmMap(eq("p_abm_tac_FirmaCierre"), params.capture(), eq("G"));
        assertEquals(dia, params.getValue().get("fechaRevisada"));
    }
}
