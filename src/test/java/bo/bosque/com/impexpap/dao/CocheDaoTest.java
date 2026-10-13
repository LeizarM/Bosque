package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Coche;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * El contrato del listado de gestion con p_list_tac_Coche: que ACCION se llama y que parametros
 * viajan. El SQL real lo cubre 2026-10-12_tac_coche_02_prueba_humo.sql en BOSQUE2PRUEBA
 * (esta en la carpeta sql/ del espacio de trabajo ClaudeGestionVehiculos, no en este repo).
 */
class CocheDaoTest {

    private SpHelper sp;
    private CocheDao dao;

    @BeforeEach
    void setUp() {
        sp = mock(SpHelper.class);
        dao = new CocheDao(sp);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parametrosEnviados() {
        ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
        verify(sp).ejecutarListado(eq("p_list_tac_Coche"), cap.capture(), eq("S"), eq(Coche.class));
        return cap.getValue();
    }

    @Test
    void sinFiltroLlamaALaAccionSConElMapaVacio() {
        List<Coche> filas = Collections.singletonList(new Coche());
        when(sp.ejecutarListado(eq("p_list_tac_Coche"), org.mockito.ArgumentMatchers.<Map<String, Object>>any(),
                eq("S"), eq(Coche.class))).thenReturn(filas);

        List<Coche> r = dao.listarGestion(null);

        assertSame(filas, r);
        assertTrue(parametrosEnviados().isEmpty(), "sin filtro no viaja ningun parametro");
    }

    @Test
    void unCuerpoVacioNoFiltraPorLosPrimitivosEnCero() {
        // El front manda {} y Jackson deja idCoche=0 y audUsuario=0: no son filtros.
        dao.listarGestion(new Coche());

        Map<String, Object> p = parametrosEnviados();
        assertFalse(p.containsKey("idCoche"));
        assertFalse(p.containsKey("audUsuario"));
        assertTrue(p.isEmpty());
    }

    @Test
    void estadoCeroSiFiltraPorqueSonLosInactivos() {
        Coche f = new Coche();
        f.setEstado(0);

        Map<String, Object> p = parametrosEnviados0(f);

        assertEquals(0, p.get("estado"));
    }

    @Test
    void textoEnBlancoNoFiltraYElTextoSeRecorta() {
        Coche f = new Coche();
        f.setMarca("   ");
        f.setPlaca("  917-SKP ");
        f.setCodSucursal(20);
        f.setAnio(2015);

        Map<String, Object> p = parametrosEnviados0(f);

        assertFalse(p.containsKey("marca"));
        assertEquals("917-SKP", p.get("placa"));
        assertEquals(20, p.get("codSucursal"));
        assertEquals(2015, p.get("anio"));
    }

    @Test
    void unIdPositivoSiViaja() {
        Coche f = new Coche();
        f.setIdCoche(7L);

        assertEquals(7L, parametrosEnviados0(f).get("idCoche"));
    }

    private Map<String, Object> parametrosEnviados0(Coche filtro) {
        dao.listarGestion(filtro);
        return parametrosEnviados();
    }
}
