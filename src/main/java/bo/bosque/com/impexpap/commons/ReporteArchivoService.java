package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.config.SpBusinessException;
import bo.bosque.com.impexpap.dao.IArchivoDocumental;
import bo.bosque.com.impexpap.dao.IEmpresa;
import bo.bosque.com.impexpap.dto.DetalleArchivoRptDto;
import bo.bosque.com.impexpap.dto.FiltroReporteArchivo;
import bo.bosque.com.impexpap.dto.ResumenArchivoRptDto;
import bo.bosque.com.impexpap.model.Empresa;
import org.springframework.stereotype.Service;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Los reportes del archivo de documentos: validan el pedido, traen las filas por SP y llenan el jrxml.
 * Sin filas no hay PDF: sale un error de negocio que la app muestra tal cual.
 */
@Service
public class ReporteArchivoService {

    /** Cada reporte con su jrxml y el titulo que se imprime. */
    public enum Reporte {
        POR_SUCURSAL("RptTarcPorSucursal", "Documentos por sucursal"),
        POR_TIPO("RptTarcPorTipo", "Dónde está cada tipo de documento"),
        DETALLE("RptTarcDetalle", "Detalle de documentos por caja");

        private final String jrxml;
        private final String titulo;

        Reporte(String jrxml, String titulo) {
            this.jrxml = jrxml;
            this.titulo = titulo;
        }

        public String getJrxml() { return jrxml; }

        public String getTitulo() { return titulo; }
    }

    private static final String SEPARADOR = "   ·   ";

    private final IArchivoDocumental dao;
    private final IEmpresa empresas;
    private final JasperReportExport jasper;

    public ReporteArchivoService(IArchivoDocumental dao, IEmpresa empresas, JasperReportExport jasper) {
        this.dao = dao;
        this.empresas = empresas;
        this.jasper = jasper;
    }

    public byte[] generar(FiltroReporteArchivo filtro, String usuario) {
        Reporte reporte = reporteDe(filtro);
        validarAnios(filtro);

        List<?> filas;
        String totales;
        String sucursal = null;
        String tipo = null;
        if (reporte == Reporte.DETALLE) {
            List<DetalleArchivoRptDto> detalle = dao.detalleReporte(filtro);
            sinFilas(detalle);
            filas = detalle;
            totales = totalesDetalle(detalle);
            sucursal = detalle.get(0).getSucursalTexto();
            tipo = detalle.get(0).getNombreTipoDocumento();
        } else {
            List<ResumenArchivoRptDto> resumen = new ArrayList<>(dao.resumenReporte(filtro));
            sinFilas(resumen);
            // El SP ordena por archivo y tipo; "por tipo" agrupa al reves. Collator: "Órdenes" va con la O.
            if (reporte == Reporte.POR_TIPO) {
                Collator es = Collator.getInstance(new Locale("es"));
                es.setStrength(Collator.PRIMARY);
                resumen.sort(Comparator.comparing((ResumenArchivoRptDto r) -> texto(r.getNombreTipoDocumento()), es)
                        .thenComparing(r -> texto(r.getNombreArchivo()), es));
            }
            filas = resumen;
            totales = totalesResumen(resumen);
            sucursal = resumen.get(0).getSucursalTexto();
            tipo = resumen.get(0).getNombreTipoDocumento();
        }

        Map<String, Object> p = new HashMap<>();
        p.put("titulo", reporte.getTitulo());
        p.put("filtros", describir(filtro, sucursal, tipo));
        p.put("totales", totales);
        p.put("usuario", usuario == null || usuario.trim().isEmpty() ? "-" : usuario.trim());
        p.put("generado", ahora());
        // Un stream nuevo por llamada: el llenado lo consume.
        p.put("logoEmpresa", getClass().getResourceAsStream("/logos/logoEmpresa.jpg"));
        return jasper.exportPDFDesdeColeccion(reporte.getJrxml(), filas, p);
    }

    static Reporte reporteDe(FiltroReporteArchivo filtro) {
        String nombre = filtro == null || filtro.getReporte() == null ? "" : filtro.getReporte().trim();
        if (nombre.isEmpty()) throw new SpBusinessException("Elige qué reporte quieres generar.");
        try {
            return Reporte.valueOf(nombre.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SpBusinessException("No existe el reporte " + nombre + ".");
        }
    }

    private static void validarAnios(FiltroReporteArchivo f) {
        Integer desde = f.getAnioDesde();
        Integer hasta = f.getAnioHasta();
        if ((desde != null && (desde < 1900 || desde > 2100)) || (hasta != null && (hasta < 1900 || hasta > 2100))) {
            throw new SpBusinessException("Los años de la gestión van de 1900 a 2100.");
        }
        if (desde != null && hasta != null && desde > hasta) {
            throw new SpBusinessException("El año «desde» no puede ser mayor que el año «hasta».");
        }
    }

    private static void sinFilas(List<?> filas) {
        if (filas == null || filas.isEmpty()) {
            throw new SpBusinessException("No hay documentos que cumplan esos filtros. Prueba quitando alguno.");
        }
    }

    /** "1.079 documentos en 201 cajas de 2 archivos": las cajas se cuentan una vez por archivo, no por tipo. */
    static String totalesResumen(List<ResumenArchivoRptDto> filas) {
        Map<Long, Integer> cajasPorArchivo = new HashMap<>();
        int documentos = 0;
        for (ResumenArchivoRptDto f : filas) {
            documentos += f.getLineas() == null ? 0 : f.getLineas();
            cajasPorArchivo.put(f.getCodArchivo(), f.getCajasArchivo() == null ? 0 : f.getCajasArchivo());
        }
        int cajas = 0;
        for (Integer c : cajasPorArchivo.values()) cajas += c;
        return totales(documentos, cajas, cajasPorArchivo.size());
    }

    static String totalesDetalle(List<DetalleArchivoRptDto> filas) {
        Set<Long> cajas = new HashSet<>();
        Set<Long> archivos = new HashSet<>();
        for (DetalleArchivoRptDto f : filas) {
            cajas.add(f.getCodCaja());
            archivos.add(f.getCodArchivo());
        }
        return totales(filas.size(), cajas.size(), archivos.size());
    }

    private static String totales(int documentos, int cajas, int archivos) {
        return ResumenArchivoRptDto.cuenta(documentos, "documento", "documentos") + " en "
                + ResumenArchivoRptDto.cuenta(cajas, "caja", "cajas") + " de "
                + ResumenArchivoRptDto.cuenta(archivos, "archivo", "archivos");
    }

    /** Lo que se filtro, en palabras; los nombres salen de las filas (todas comparten el filtro). */
    String describir(FiltroReporteArchivo f, String sucursal, String tipo) {
        List<String> partes = new ArrayList<>();
        if (f.getCodSucursal() != null && f.getCodSucursal() > 0) partes.add("Sucursal: " + sucursal);
        if (f.getCodTipoDocumento() != null && f.getCodTipoDocumento() > 0) partes.add("Tipo: " + texto(tipo));
        if (f.getCodEmpresa() != null && f.getCodEmpresa() == 0) partes.add("Empresa: sin empresa (otros)");
        if (f.getCodEmpresa() != null && f.getCodEmpresa() > 0) partes.add("Empresa: " + nombreEmpresa(f.getCodEmpresa()));
        Integer desde = f.getAnioDesde();
        Integer hasta = f.getAnioHasta();
        if (desde != null && hasta != null) {
            partes.add(desde.equals(hasta) ? "Gestión " + desde : "Gestión " + desde + " a " + hasta);
        } else if (desde != null) {
            partes.add("Gestión desde " + desde);
        } else if (hasta != null) {
            partes.add("Gestión hasta " + hasta);
        }
        return partes.isEmpty() ? "Todo el archivo, sin filtros" : String.join(SEPARADOR, partes);
    }

    private String nombreEmpresa(long codEmpresa) {
        for (Empresa e : empresas.obtenerEmpresas()) {
            if (e.getCodEmpresa() == codEmpresa) return e.getNombre();
        }
        return "#" + codEmpresa;
    }

    private static String texto(String s) {
        return s == null ? "" : s.trim();
    }

    private static String ahora() {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        f.setTimeZone(TimeZone.getTimeZone("America/La_Paz"));
        return f.format(new Date());
    }
}
