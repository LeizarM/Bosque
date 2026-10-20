package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeBancoDuplicadoDto;
import bo.bosque.com.impexpap.dto.TcpeBancoFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeBancoGuardarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeBancoImportarPreguntaDto;
import bo.bosque.com.impexpap.dto.TcpeBancoLoteFilaDto;
import bo.bosque.com.impexpap.dto.TcpeBancoOpcionDto;
import bo.bosque.com.impexpap.dto.TcpeBancoPreguntaDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.SpHelper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class TcpeBancoDao implements ITcpeBanco {

    static final String SP_LIST = "p_list_tcpe_Banco";
    static final String SP_ABM = "p_abm_tcpe_Banco";
    static final String SP_LOTE_LIST = "p_list_tcpe_BancoLote";
    static final String SP_LOTE_ABM = "p_abm_tcpe_BancoLote";

    private final SpHelper spHelper;

    public TcpeBancoDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public List<TcpeBancoPreguntaDto> listar(TcpeBancoFiltroDto f) {
        Map<String, Object> p = new LinkedHashMap<>();
        if (f != null) {
            TcpeParametros.filtro(p, "codPregunta", f.getCodPregunta());
            TcpeParametros.filtro(p, "codClasificacion", f.getCodClasificacion());
            TcpeParametros.filtro(p, "codArea", f.getCodArea());
            TcpeParametros.filtro(p, "activo", f.getActivo());
            TcpeParametros.filtro(p, "texto", f.getTexto());
        }
        return spHelper.ejecutarListado(SP_LIST, p, "L", TcpeBancoPreguntaDto.class);
    }

    @Override
    public List<TcpeBancoOpcionDto> opciones(Long codPregunta, Long codClasificacion) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codPregunta", codPregunta);
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        return spHelper.ejecutarListado(SP_LIST, p, "O", TcpeBancoOpcionDto.class);
    }

    @Override
    public RespuestaSp guardar(TcpeBancoGuardarDto dto, long audUsuario) {
        Long cod = TcpeParametros.id(dto.getCodPregunta());
        boolean cerrada = Boolean.TRUE.equals(dto.getEsCerrada());
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPregunta", cod == null ? 0L : cod);
        TcpeParametros.poner(p, "codClasificacion", dto.getCodClasificacion());
        TcpeParametros.poner(p, "pregunta", dto.getPregunta());
        TcpeParametros.poner(p, "prioridad", dto.getPrioridad());
        TcpeParametros.poner(p, "activo", dto.getActivo());
        p.put("esCerrada", cerrada);
        TcpeParametros.poner(p, "respuesta", dto.getRespuesta());
        // Una abierta va sin opciones: si tenía, el SP las quita (y avisa si alguna ya se respondió).
        p.put("opciones", armarOpcionesXml(cerrada ? dto.getOpciones() : Collections.<TcpeBancoOpcionDto>emptyList()));
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, cod == null ? "I" : "U");
    }

    @Override
    public RespuestaSp eliminar(long codPregunta, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPregunta", codPregunta);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "D");
    }

    @Override
    public RespuestaSp activar(long codPregunta, Boolean activo, long audUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codPregunta", codPregunta);
        TcpeParametros.poner(p, "activo", activo);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_ABM, p, "A");
    }

    @Override
    public List<TcpeBancoDuplicadoDto> duplicados(Long codClasificacion, String texto) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.filtro(p, "codClasificacion", codClasificacion);
        TcpeParametros.filtro(p, "texto", texto);
        return spHelper.ejecutarListado(SP_LIST, p, "D", TcpeBancoDuplicadoDto.class);
    }

    @Override
    public List<TcpeBancoLoteFilaDto> validarLote(TcpeBancoImportarDto dto) {
        return spHelper.ejecutarListado(SP_LOTE_LIST, parametrosLote(dto), "V", TcpeBancoLoteFilaDto.class);
    }

    @Override
    public RespuestaSp importarLote(TcpeBancoImportarDto dto, long audUsuario) {
        Map<String, Object> p = parametrosLote(dto);
        p.put("audUsuario", audUsuario);
        return spHelper.ejecutarAbmMap(SP_LOTE_ABM, p, "I");
    }

    private static Map<String, Object> parametrosLote(TcpeBancoImportarDto dto) {
        Map<String, Object> p = new LinkedHashMap<>();
        TcpeParametros.poner(p, "codClasificacion", dto.getCodClasificacion());
        p.put("preguntas", armarPreguntasXml(dto.getPreguntas()));
        return p;
    }

    /**
     * {@code @preguntas} de BancoLote: {@code <preguntas><p f c pr><t>..</t><r>..</r><o v>..</o></p></preguntas>}.
     * {@code f} numera desde 1 en el orden recibido; lo nulo (aun una pregunta u opción entera) va vacío y lo juzga el SP.
     */
    public static String armarPreguntasXml(List<TcpeBancoImportarPreguntaDto> preguntas) {
        StringBuilder sb = new StringBuilder("<preguntas>");
        if (preguntas != null) {
            int fila = 0;
            for (TcpeBancoImportarPreguntaDto item : preguntas) {
                TcpeBancoImportarPreguntaDto p = item == null ? new TcpeBancoImportarPreguntaDto() : item;
                sb.append("<p f=\"").append(++fila)
                  .append("\" c=\"").append(p.getEsCerrada() == null ? "" : p.getEsCerrada() ? "1" : "0")
                  .append("\" pr=\"").append(p.getPrioridad() == null ? "" : p.getPrioridad().toString())
                  .append("\"><t>");
                escaparTexto(recortado(p.getPregunta()), sb);
                sb.append("</t><r>");
                escaparTexto(recortado(p.getRespuesta()), sb);
                sb.append("</r>");
                if (p.getOpciones() != null) {
                    for (TcpeBancoImportarOpcionDto o : p.getOpciones()) {
                        sb.append("<o v=\"").append(o != null && Boolean.TRUE.equals(o.getEsValido()) ? 1 : 0).append("\">");
                        escaparTexto(recortado(o == null ? null : o.getOpcionRspta()), sb);
                        sb.append("</o>");
                    }
                }
                sb.append("</p>");
            }
        }
        return sb.append("</preguntas>").toString();
    }

    private static String recortado(String texto) {
        return texto == null ? "" : texto.trim();
    }

    /**
     * {@code @opciones} de {@code p_abm_tcpe_Banco}: {@code <opciones><o c="codRsptaOp o 0" v="1|0">texto</o>...}.
     * El texto del usuario se escapa aquí y no en el SP; saltos y tabulaciones van como referencia de carácter y se
     * quitan los controles que XML 1.0 no admite, o el {@code CAST(@opciones AS XML)} fallaría con un error críptico.
     */
    public static String armarOpcionesXml(List<TcpeBancoOpcionDto> opciones) {
        StringBuilder sb = new StringBuilder("<opciones>");
        if (opciones != null) {
            for (TcpeBancoOpcionDto o : opciones) {
                if (o == null) continue;
                long cod = o.getCodRsptaOp() == null || o.getCodRsptaOp() < 0 ? 0L : o.getCodRsptaOp();
                String texto = o.getOpcionRspta() == null ? "" : o.getOpcionRspta().trim();
                sb.append("<o c=\"").append(cod)
                  .append("\" v=\"").append(Boolean.TRUE.equals(o.getEsValido()) ? 1 : 0)
                  .append("\">");
                escaparTexto(texto, sb);
                sb.append("</o>");
            }
        }
        return sb.append("</opciones>").toString();
    }

    private static void escaparTexto(String texto, StringBuilder sb) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;");  break;
                case '<':  sb.append("&lt;");   break;
                case '>':  sb.append("&gt;");   break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                case '\t': sb.append("&#9;");   break;
                case '\n': sb.append("&#10;");  break;
                case '\r': sb.append("&#13;");  break;
                default:
                    if (Character.isHighSurrogate(c) && i + 1 < texto.length()
                            && Character.isLowSurrogate(texto.charAt(i + 1))) {
                        sb.append(c).append(texto.charAt(++i));
                    } else if (esCaracterXmlValido(c)) {
                        sb.append(c);
                    }
                    // lo demás (controles, surrogates sueltos, U+FFFE/U+FFFF) se descarta
            }
        }
    }

    private static boolean esCaracterXmlValido(char c) {
        return (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD);
    }
}
