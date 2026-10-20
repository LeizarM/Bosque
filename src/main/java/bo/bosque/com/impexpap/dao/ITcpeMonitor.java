package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeMonitorEspejoDto;
import bo.bosque.com.impexpap.dto.TcpeMonitorExamenDto;
import bo.bosque.com.impexpap.dto.TcpeMonitorLoteDto;

import java.util.List;

/** Panel "En vivo" del evaluador: {@code p_list_tcpe_Monitor} (contrato 8.4). Solo lectura. */
public interface ITcpeMonitor {

    /** {@code D}: lotes con exámenes en curso o con la ventana abierta. */
    List<TcpeMonitorLoteDto> lotes();

    /** {@code L}: los evaluados del lote, primero los que están en curso. */
    List<TcpeMonitorExamenDto> listar(Long codRolExamen);

    /** {@code E}: la pregunta actual de un examen, una fila por opción. */
    List<TcpeMonitorEspejoDto> espejo(Long codExamen);
}
