package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.BancoDto;
import bo.bosque.com.impexpap.model.ChBanco;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_banco} por {@code p_list_Banco} y {@code p_abm_Banco}. */
public interface IChBanco {

    /** {@code p_list_Banco} 'L': todos los bancos, por nombre. Forma de {@code /banco/bancosX}. */
    List<BancoDto> listBancos();

    /**
     * {@code p_list_Banco} 'A': los bancos habilitados para planillas. <b>Son los codigos 0, 2, 3, 5
     * y 9 escritos dentro del procedimiento</b>, no una columna de la tabla.
     */
    List<BancoDto> listBancosPlanilla();

    /** Un banco por su codigo, o {@code null} si no existe. */
    ChBanco obtener(int codBanco);

    /** {@code p_abm_Banco} 'I' (codBanco null o 0) o 'U'. Devuelve el id en {@code idGenerado}. */
    RespuestaSp registrar(ChBanco banco);

    /**
     * {@code p_abm_Banco} 'D'. Si el banco tiene filas en Depositos ({@code tdep_BancoXCuenta}) o en
     * Pagos al Exterior ({@code tpex_*}) el procedimiento responde error 547 y no lo borra.
     */
    RespuestaSp eliminar(int codBanco, int audUsuario);
}
