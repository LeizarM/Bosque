package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeExamenCabeceraDto;
import bo.bosque.com.impexpap.dto.TcpeHistorialDto;
import bo.bosque.com.impexpap.dto.TcpeInformeDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoExamenDto;
import bo.bosque.com.impexpap.dto.TcpeRevisionDto;
import bo.bosque.com.impexpap.dto.TcpeSolucionarioDto;

import java.time.LocalDate;
import java.util.List;

/** Resultados: {@code p_list_tcpe_Resultado} (contratos 3.5 y 7.7). Sin el filtro que pide cada acción, vacía. */
public interface ITcpeResultado {

    /** {@code L}: exámenes de un lote con estado y nota; también cruza {@code codExamen} con {@code login}. */
    List<TcpeResultadoExamenDto> listar(Long codRolExamen);

    /** {@code R}: revisión pregunta por pregunta de un examen. */
    List<TcpeRevisionDto> revision(Long codExamen);

    /** {@code S}: solucionario de un examen o de todo el lote. */
    List<TcpeSolucionarioDto> solucionario(Long codExamen, Long codRolExamen);

    /** {@code E}: cabecera de cada examen del lote (o de uno), para los PDF de los escritos. */
    List<TcpeExamenCabeceraDto> cabeceras(Long codRolExamen, Long codExamen);

    /** {@code H}: historial por CI. */
    List<TcpeHistorialDto> historial(String ci);

    /** {@code C}: informe por categoría y fechas (cualquiera puede ir nulo). */
    List<TcpeInformeDto> informe(Long codClasificacion, LocalDate fechaDesde, LocalDate fechaHasta);
}
