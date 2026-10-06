package bo.bosque.com.impexpap.dao;

import java.util.Date;
import java.util.List;

import bo.bosque.com.impexpap.dto.ChequeFilaDto;
import bo.bosque.com.impexpap.dto.ChequeFiltroDto;
import bo.bosque.com.impexpap.dto.ChequeResumenDto;
import bo.bosque.com.impexpap.dto.HoraAccionDto;
import bo.bosque.com.impexpap.model.ChCheque;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/** Acceso a {@code tch_cheque} por {@code p_list_Cheque} y {@code p_abm_Cheque}. */
public interface IChCheque {

    // ------------------------------ Lecturas ------------------------------

    /**
     * Grilla de la pantalla: {@code p_list_Cheque} 'A' (por fecha de recepcion, mas reciente primero) o
     * 'B' (por fecha de cobro). Trae todas las filas; la pagina la corta quien llama.
     */
    List<ChequeFilaDto> listar(ChequeFiltroDto filtro);

    /** Un cheque con sus datos de JOIN ('A' por codigo, sin filtrar sucursal), o {@code null}. */
    ChequeFilaDto obtener(int codCheque);

    /** 'L': ya existe un cheque con el mismo numero, cliente, banco, sucursal y fecha ({@code existeCheque}). */
    boolean existeDuplicado(String nroCheque, String codCliente, int codBanco, long codSucursal, Date fechaCheque);

    /** 'M': el talonario existe en {@code tmto_talonario}, para esa empresa, con el recibo dentro de su numeracion. */
    boolean talonarioYReciboValidos(String nroTalonario, String reciboManual, int codEmpresa);

    /**
     * 'K': los 4 caracteres que dicen cuales acciones del detalle estan habilitadas (ver
     * {@link bo.bosque.com.impexpap.dto.BotonesChequeDto}). {@code "0000"} si el SP no responde.
     */
    String codigoBotones(int codCheque);

    /** 'C': cheques pendientes de la sucursal que hay que cobrar el dia dado y salieron de caja: lista de "A Custodio". */
    List<ChequeFilaDto> sinCustodio(long codSucursal, Date fechaCobro);

    /** 'J': todos los cheques de la sucursal para elegir uno ("Dar Custodia"; el filtro "sin custodia" esta comentado en el SP). */
    List<ChequeResumenDto> chequesDeSucursal(long codSucursal);

    /** 'I': las entregas a cobranza (CUS) de un dia, para elegir cual copiar. */
    List<HoraAccionDto> custodiasDelDia(long codSucursal, Date fecha);

    // ------------------------------ Escrituras ------------------------------

    /** 'I': inserta el cheque y su accion REC con {@code observacion}. {@code idGenerado} = codCheque. */
    RespuestaSp alta(ChCheque cheque, String observacion);

    /**
     * 'U': reescribe TODAS las columnas editables y la observacion de la accion REC con lo que reciba;
     * quien llama tiene que mandar el cheque completo.
     */
    RespuestaSp actualizar(ChCheque cheque, String observacion);

    /** 'E': cambia solo el estado (PEN / CER). */
    RespuestaSp cambiarEstado(int codCheque, String estado, int audUsuario);

    /** 'F': cambia solo la fecha de cobro. */
    RespuestaSp cambiarFechaCobro(int codCheque, Date fechaCobrar, int audUsuario);
}
