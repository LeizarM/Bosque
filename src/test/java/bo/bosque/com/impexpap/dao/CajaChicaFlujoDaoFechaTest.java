package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarEgresoCajaChicaRequest;
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
import static org.mockito.Mockito.when;

/**
 * La fecha del egreso (archivo SQL 100): si llega, va al SP como @fecha; si no llega (la app
 * anterior), no se manda y el SP usa hoy. Que no sea anterior al ultimo registro lo valida el SP.
 */
class CajaChicaFlujoDaoFechaTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> parametrosDe(RegistrarEgresoCajaChicaRequest req) {
        SpHelper sp = mock(SpHelper.class);
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        when(sp.ejecutarAbmMap(eq("p_abm_tac_CajaChica"), params.capture(), eq("R")))
                .thenReturn(new RespuestaSp(0, "ok", 1L));
        new CajaChicaFlujoDao(sp).registrarEgreso(req, 75L, 28L);
        return params.getValue();
    }

    private static RegistrarEgresoCajaChicaRequest egreso(Date fecha) {
        RegistrarEgresoCajaChicaRequest r = new RegistrarEgresoCajaChicaRequest();
        r.setMontoEg(35.5);
        r.setDescripcion("Taxi");
        r.setCodEmpDestino(120);
        r.setFecha(fecha);
        return r;
    }

    @Test
    void conFechaLaMandaAlSp() {
        Date fecha = new Date(1_791_950_400_000L);
        assertEquals(fecha, parametrosDe(egreso(fecha)).get("fecha"));
    }

    @Test
    void sinFechaNoLaMandaYElSpUsaHoy() {
        assertFalse(parametrosDe(egreso(null)).containsKey("fecha"));
    }
}
