package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dto.AsientoSapLinea;
import bo.bosque.com.impexpap.dto.AsientoSapResumen;
import bo.bosque.com.impexpap.model.AsientoSapConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte las filas del SP de la empresa en el asiento de SAP. Es la parte del script de Node
 * que mapeaba y validaba, sin base ni red, para poder probarla sola.
 *
 * <p>Dos diferencias con el script, las dos a propósito:
 * <ul>
 *   <li>Los montos se redondean a 2 decimales por línea y se suman así, con {@code BigDecimal}: el
 *       cuadre se mide sobre lo mismo que recibe SAP, no sobre sumas en coma flotante.</li>
 *   <li>El día fijo (30) se acota al largo del mes: el script armaba {@code 2026-02-30}.</li>
 * </ul>
 */
public final class AsientoPlanillaArmador {

    /** {@code OJDT.Memo} y {@code JDT1.LineMemo} son de 50 caracteres: más largo, SAP rechaza el asiento. */
    static final int LARGO_MEMO = 50;
    private static final int DIA_POR_DEFECTO = 30;

    private AsientoPlanillaArmador() {
    }

    public static AsientoSapResumen armar(List<Map<String, Object>> filas, int anio, int mes, AsientoSapConfig cfg) {
        exigirColumnas(filas, cfg);
        List<String> prefijos = prefijos(cfg.getPrefijosSocio());

        List<AsientoSapLinea> lineas = new ArrayList<>();
        BigDecimal totalDebito = cero();
        BigDecimal totalCredito = cero();
        int numero = 0;
        for (Map<String, Object> fila : filas) {
            numero++;
            BigDecimal debito = monto(fila.get(cfg.getColDebito()), numero, cfg.getColDebito());
            BigDecimal credito = monto(fila.get(cfg.getColCredito()), numero, cfg.getColCredito());
            if (debito.signum() == 0 && credito.signum() == 0) {
                continue; // como el script: las líneas en cero no van a SAP
            }
            String codigo = texto(fila.get(cfg.getColCodigo()));
            if (codigo.isEmpty()) {
                throw new SpBusinessException("La fila " + numero + " del SP no trae " + cfg.getColCodigo() + ".");
            }
            boolean socio = esSocio(codigo, prefijos);
            String cuenta = socio ? texto(fila.get(cfg.getColCuenta())) : codigo;
            String nombre = recortar(texto(fila.get(cfg.getColNombre())), LARGO_MEMO);
            lineas.add(new AsientoSapLinea(codigo, nombre, cuenta, socio ? codigo : null, debito, credito));
            totalDebito = totalDebito.add(debito);
            totalCredito = totalCredito.add(credito);
        }

        BigDecimal diferencia = totalDebito.subtract(totalCredito);
        BigDecimal tolerancia = cfg.getTolerancia() == null ? BigDecimal.ZERO : cfg.getTolerancia().abs();
        int dia = cfg.getDiaDelMes() == null ? DIA_POR_DEFECTO : cfg.getDiaDelMes();
        return AsientoSapResumen.builder()
                .anio(anio)
                .mes(mes)
                .codEmpresa(cfg.getCodEmpresa())
                .empresa(cfg.getEmpresa())
                .baseSap(cfg.getBaseSap().trim())
                .fecha(fecha(anio, mes, dia).toString())
                .memo(recortar(texto(cfg.getMemo(), anio, mes), LARGO_MEMO))
                .referencia(texto(cfg.getReferencia(), anio, mes))
                .lineas(lineas)
                .totalDebito(totalDebito)
                .totalCredito(totalCredito)
                .diferencia(diferencia)
                .cuadra(!lineas.isEmpty() && diferencia.abs().compareTo(tolerancia) <= 0)
                .build();
    }

    /** El cuerpo de {@code POST JournalEntries}. */
    public static Map<String, Object> payload(AsientoSapResumen asiento, String campoReferencia) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ReferenceDate", asiento.getFecha());
        cuerpo.put("TaxDate", asiento.getFecha());
        cuerpo.put("DueDate", asiento.getFecha());
        cuerpo.put("Memo", asiento.getMemo());
        if (campoReferencia != null && !campoReferencia.trim().isEmpty()) {
            cuerpo.put(campoReferencia.trim(), asiento.getReferencia());
        }
        List<Map<String, Object>> lineas = new ArrayList<>();
        for (AsientoSapLinea l : asiento.getLineas()) {
            Map<String, Object> linea = new LinkedHashMap<>();
            if (!l.getCuenta().isEmpty()) {
                linea.put("AccountCode", l.getCuenta());
            }
            if (l.getSocio() != null) {
                linea.put("ShortName", l.getSocio());
            }
            linea.put("LineMemo", l.getNombre());
            linea.put("Debit", l.getDebito());
            linea.put("Credit", l.getCredito());
            lineas.add(linea);
        }
        cuerpo.put("JournalEntryLines", lineas);
        return cuerpo;
    }

    static List<String> prefijos(String separadosPorComa) {
        List<String> lista = new ArrayList<>();
        if (separadosPorComa == null) return lista;
        for (String p : separadosPorComa.split(",")) {
            if (!p.trim().isEmpty()) lista.add(p.trim().toUpperCase(Locale.ROOT));
        }
        return lista;
    }

    static boolean esSocio(String codigo, List<String> prefijos) {
        String mayus = codigo.toUpperCase(Locale.ROOT);
        for (String p : prefijos) {
            if (mayus.startsWith(p)) return true;
        }
        return false;
    }

    /** El día pedido, o el último del mes si el mes es más corto o si se pidió 0. */
    static LocalDate fecha(int anio, int mes, int dia) {
        YearMonth periodo = YearMonth.of(anio, mes);
        return dia <= 0 || dia > periodo.lengthOfMonth() ? periodo.atEndOfMonth() : periodo.atDay(dia);
    }

    /** Reemplaza {anio}, {mes} y {mes2} en una plantilla de la configuración. */
    static String texto(String plantilla, int anio, int mes) {
        if (plantilla == null) return "";
        return plantilla.replace("{anio}", String.valueOf(anio))
                .replace("{mes2}", String.format("%02d", mes))
                .replace("{mes}", String.valueOf(mes));
    }

    private static void exigirColumnas(List<Map<String, Object>> filas, AsientoSapConfig cfg) {
        if (filas.isEmpty()) return;
        Map<String, Object> primera = filas.get(0);
        String[][] esperadas = {
                {cfg.getColCodigo(), "código"}, {cfg.getColNombre(), "nombre"}, {cfg.getColCuenta(), "cuenta asociada"},
                {cfg.getColDebito(), "débito"}, {cfg.getColCredito(), "crédito"}};
        for (String[] e : esperadas) {
            if (!primera.containsKey(e[0])) {
                throw new SpBusinessException("El SP no devolvió la columna '" + e[0] + "' (columna del " + e[1]
                        + " en la configuración). Trae: " + primera.keySet() + ".");
            }
        }
    }

    private static BigDecimal monto(Object valor, int fila, String columna) {
        if (valor == null || valor.toString().trim().isEmpty()) return cero();
        try {
            BigDecimal m = valor instanceof BigDecimal ? (BigDecimal) valor : new BigDecimal(valor.toString().trim());
            return m.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new SpBusinessException("La fila " + fila + " del SP trae un monto que no es número en "
                    + columna + ": '" + valor + "'.");
        }
    }

    private static BigDecimal cero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static String texto(Object valor) {
        return valor == null ? "" : valor.toString().trim();
    }

    private static String recortar(String s, int largo) {
        return s.length() <= largo ? s : s.substring(0, largo);
    }
}
