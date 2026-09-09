package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
public class GeneradorTareasRutinariasDao implements IGeneradorTareasRutinarias {

    private final SpHelper spHelper;

    public GeneradorTareasRutinariasDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp generarOcurrencias() {
        // Sin parámetros reales — "R" es relleno sin efecto en el SP, solo
        // para que ejecutarAbmMap pueda invocarlo (siempre inyecta @ACCION).
        return spHelper.ejecutarAbmMap("p_generar_tac_bitTareaRuti", new HashMap<>(), "R");
    }
}
