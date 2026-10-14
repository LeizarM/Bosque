package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CodigoRecuperacion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface ICodigoRecuperacion {

    RespuestaSp solicitar(String login, String codigoHash, String ip);

    CodigoRecuperacion obtenerPorId(long idCodigo);

    CodigoRecuperacion obtenerVigente(String login);

    /** Reserva un intento antes de comparar el código; falla (SpBusinessException) si no quedan. */
    RespuestaSp reservarIntento(long idCodigo);

    RespuestaSp restablecer(long idCodigo, String password2, String ip);

    RespuestaSp eliminar(long idCodigo);
}
