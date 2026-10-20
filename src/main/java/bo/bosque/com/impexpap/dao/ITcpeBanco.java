package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeBancoDuplicadoDto;
import bo.bosque.com.impexpap.dto.TcpeBancoFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeBancoGuardarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoLoteFilaDto;
import bo.bosque.com.impexpap.dto.TcpeBancoOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeBancoPreguntaDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Banco de preguntas: {@code p_list/p_abm_tcpe_Banco} (contrato 3.2) e importación con {@code BancoLote} (9.3). */
public interface ITcpeBanco {

    /** {@code L}: preguntas con su categoría, su respuesta y el resumen de opciones. */
    List<TcpeBancoPreguntaDto> listar(TcpeBancoFiltroDto filtro);

    /** {@code O}: opciones de una pregunta o de todas las de una categoría. */
    List<TcpeBancoOpcionDto> opciones(Long codPregunta, Long codClasificacion);

    /** {@code I}/{@code U}: pregunta, respuesta y opciones en una sola operación. {@code idGenerado} = codPregunta. */
    RespuestaSp guardar(TcpeBancoGuardarDto dto, long audUsuario);

    /** {@code D}: falla si la pregunta ya se usó en exámenes (hay que desactivarla). */
    RespuestaSp eliminar(long codPregunta, long audUsuario);

    /** {@code A}: activa o desactiva. */
    RespuestaSp activar(long codPregunta, Boolean activo, long audUsuario);

    /** {@code D} (v6): iguales al texto (texto normalizado) y hasta 5 parecidas de la categoría. */
    List<TcpeBancoDuplicadoDto> duplicados(Long codClasificacion, String texto);

    /** {@code p_list_tcpe_BancoLote 'V'}: valida una importación sin escribir, una fila por pregunta. */
    List<TcpeBancoLoteFilaDto> validarLote(TcpeBancoImportarDto dto);

    /** {@code p_abm_tcpe_BancoLote 'I'}: inserta solo las filas OK. {@code idGenerado} = cantidad importada. */
    RespuestaSp importarLote(TcpeBancoImportarDto dto, long audUsuario);
}
