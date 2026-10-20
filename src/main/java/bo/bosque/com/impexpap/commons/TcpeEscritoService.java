package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dto.TcpeEscritoFilaDto;
import bo.bosque.com.impexpap.dto.TcpeExamenCabeceraDto;
import bo.bosque.com.impexpap.dto.TcpeSolucionarioDto;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF del examen escrito y de su solucionario (contrato 7.7) con {@code RptTcpeExamenEscrito.jrxml}: cabeceras de
 * {@code p_list_tcpe_Resultado 'E'} y preguntas de {@code 'S'}, unidas en filas planas para una
 * {@code JRBeanCollectionDataSource}. El reporte no tiene queryString: todo dato sale de los SP.
 */
@Service
public class TcpeEscritoService {

    static final String REPORTE = "RptTcpeExamenEscrito";

    private final ITcpeResultado resultados;
    private final JasperReportExport jasper;

    public TcpeEscritoService(ITcpeResultado resultados, JasperReportExport jasper) {
        this.resultados = resultados;
        this.jasper = jasper;
    }

    /** Todos los exámenes del lote, o solo {@code codExamen}. Sin nada que imprimir, un arreglo vacío. */
    public byte[] pdf(Long codRolExamen, Long codExamen, boolean solucionario) {
        List<TcpeEscritoFilaDto> filas = filas(resultados.cabeceras(codRolExamen, codExamen),
                resultados.solucionario(codExamen, codRolExamen));
        if (filas.isEmpty()) return new byte[0];
        Map<String, Object> p = new HashMap<>();
        p.put("solucionario", solucionario);
        return jasper.exportPDFDesdeColeccion(REPORTE, filas, p);
    }

    /** Una fila por opción (la abierta, una sola), en el orden de las cabeceras y de las preguntas. */
    static List<TcpeEscritoFilaDto> filas(List<TcpeExamenCabeceraDto> cabeceras, List<TcpeSolucionarioDto> preguntas) {
        Map<Long, Map<Long, List<TcpeSolucionarioDto>>> porExamen = new LinkedHashMap<>();
        for (TcpeSolucionarioDto s : preguntas == null ? Collections.<TcpeSolucionarioDto>emptyList() : preguntas) {
            porExamen.computeIfAbsent(s.getCodExamen(), k -> new LinkedHashMap<>())
                    .computeIfAbsent(s.getCodPreguntaH(), k -> new ArrayList<>()).add(s);
        }
        List<TcpeEscritoFilaDto> r = new ArrayList<>();
        for (TcpeExamenCabeceraDto c : cabeceras == null ? Collections.<TcpeExamenCabeceraDto>emptyList() : cabeceras) {
            Map<Long, List<TcpeSolucionarioDto>> ps = porExamen.get(c.getCodExamen());
            if (ps == null) continue;
            int numero = 0;
            for (List<TcpeSolucionarioDto> opciones : ps.values()) {
                numero++;
                int i = 0;
                for (TcpeSolucionarioDto s : opciones) {
                    r.add(fila(c, s, s.getOrden() != null ? s.getOrden() : numero,
                            s.getOpcionRspta() == null ? null : letra(i++)));
                }
            }
        }
        return r;
    }

    /** a, b, ... z y después números (por si una pregunta tiene más de 26 opciones). */
    static String letra(int i) {
        return i < 26 ? String.valueOf((char) ('a' + i)) : String.valueOf(i + 1);
    }

    private static TcpeEscritoFilaDto fila(TcpeExamenCabeceraDto c, TcpeSolucionarioDto s, int orden, String letra) {
        TcpeEscritoFilaDto f = new TcpeEscritoFilaDto();
        f.setCodExamen(c.getCodExamen());
        f.setNumero(c.getNumero());
        f.setTituloLote(c.getTituloLote());
        f.setNombreClasificacion(c.getNombreClasificacion());
        f.setNombreArea(c.getNombreArea());
        f.setFecha(c.getFecha() == null ? null : Date.valueOf(c.getFecha()));
        f.setDuracionMinutos(c.getDuracionMinutos());
        f.setNotaAprobacion(c.getNotaAprobacion());
        f.setNombreCompleto(c.getNombreCompleto());
        f.setCi(c.getCi());
        f.setCodPreguntaH(s.getCodPreguntaH());
        f.setOrden(orden);
        f.setPregunta(s.getPregunta());
        f.setEsCerrada(s.getEsCerrada());
        f.setLetra(letra);
        f.setOpcionRspta(s.getOpcionRspta());
        f.setEsValido(s.getEsValido());
        f.setRespuestaModelo(s.getRespuestaModelo());
        return f;
    }
}
