// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\TraspasoMovCajaDao.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TraspasoDelDiaDto;
import bo.bosque.com.impexpap.dto.VerificarTraspasoRequest;
import bo.bosque.com.impexpap.model.TraspasoMovCaja;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class TraspasoMovCajaDao implements ITraspasoMovCaja {

    private final SpHelper spHelper;

    public TraspasoMovCajaDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public RespuestaSp registrar(TraspasoMovCaja mb, String acc) {
        log.info("Registrando TraspasoMovCaja: {}, Accion: {}", mb.toString(), acc);
        return spHelper.ejecutarAbm("p_abm_tac_TraspasoMovCaja", mb, acc);
    }

    @Override
    public List<TraspasoMovCaja> listar(TraspasoMovCaja filtro) {
        return spHelper.ejecutarListadoSinCero("p_list_tac_TraspasoMovCaja", filtro, "L", TraspasoMovCaja.class);
    }

    // FIX: usa Map (no el modelo) para que idTrasp==0 no viaje como filtro real —
    // el SP trata @idTrasp IS NULL como "sin filtro"; con el overload de modelo,
    // Jackson serializa el primitivo long en 0 y el SP lo tomaría como un id real.
    @Override
    public TraspasoMovCaja obtenerPorId(long idTrasp) {
        Map<String, Object> filtro = new HashMap<>();
        filtro.put("idTrasp", idTrasp);
        List<TraspasoMovCaja> resultado = spHelper.ejecutarListado(
                "p_list_tac_TraspasoMovCaja", filtro, "L", TraspasoMovCaja.class);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    // ==================== TAREA 289 - VERIFICAR TRASPASO ENTRE SISTEMAS ====
    //
    // Los tres metodos usan el camino de Map y no el de modelo. No es
    // preferencia: ejecutarAbm arma la llamada desde la metadata del
    // procedimiento y exige un valor para CADA parametro declarado, asi que
    // mandar un modelo parcial falla. ejecutarAbmMap manda solo las claves
    // provistas, que es lo que necesitan las ACCIONes V y S — ninguna de las
    // dos usa la lista entera de columnas.

    @Override
    public List<TraspasoDelDiaDto> listarDelDia(java.util.Date fecha) {
        Map<String, Object> params = new HashMap<>();
        params.put("fecha", fecha);
        return spHelper.ejecutarListado(
                "p_list_tac_TraspasoMovCaja", params, "A", TraspasoDelDiaDto.class);
    }

    @Override
    public RespuestaSp verificar(VerificarTraspasoRequest req, long audUsuario) {
        log.info("Verificando traspaso idTrasp={} ocurrencia={} cuadra={}",
                req.getIdTrasp(), req.getIdBitTarRuti(), req.getFueVerificado());

        Map<String, Object> params = new HashMap<>();
        params.put("idTrasp", req.getIdTrasp());
        params.put("bd", req.getBd());
        params.put("fecha", req.getFecha());
        params.put("account", req.getAccount());
        params.put("contraAct", req.getContraAct());
        params.put("acctName", req.getAcctName());
        params.put("tipoTransaccion", req.getTipoTransaccion());
        params.put("dolares", req.getDolares());
        params.put("bs", req.getBs());
        params.put("fueVerificado", req.getFueVerificado());
        params.put("obs", req.getObs());
        params.put("idBitTarRuti", req.getIdBitTarRuti());
        // Del token, nunca del body: quien firma la verificacion no lo elige
        // el cliente.
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_TraspasoMovCaja", params, "V");
    }

    @Override
    public RespuestaSp sinNovedad(long idBitTarRuti, java.util.Date fecha, long audUsuario) {
        log.info("Cerrando sin novedad la ocurrencia {} del {}", idBitTarRuti, fecha);

        Map<String, Object> params = new HashMap<>();
        params.put("idBitTarRuti", idBitTarRuti);
        params.put("fecha", fecha);
        params.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap("p_abm_tac_TraspasoMovCaja", params, "S");
    }
}
