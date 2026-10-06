package bo.bosque.com.impexpap.dao;

import java.util.Date;
import java.util.List;

import bo.bosque.com.impexpap.dto.ChequeCobranzaRptDto;
import bo.bosque.com.impexpap.dto.ChequeCustodioRptDto;
import bo.bosque.com.impexpap.dto.ChequeRecibidoRptDto;
import bo.bosque.com.impexpap.dto.ChequeTraspasoRptDto;
import bo.bosque.com.impexpap.dto.HoraTraspasoChequeDto;
import bo.bosque.com.impexpap.dto.NotaRemisionRptDto;
import bo.bosque.com.impexpap.dto.ReciboChequeRptDto;

/**
 * Los datos de los reportes de cheques: lecturas de {@code p_list_Cheque} (ramas {@code R P S D E T G H}) y
 * {@code p_list_NotaRemision} 'A'. En el legacy las plantillas Jasper ejecutaban estas mismas llamadas con su
 * propia conexion; aqui las hace el DAO (todo por procedimiento almacenado) y la plantilla recibe la coleccion.
 *
 * <p>Las fechas de rango van como texto {@code dd/MM/yyyy}: el SP las convierte con {@code CONVERT(datetime, x, 103)}
 * y, si no llegan (null), usa hoy. Ninguna rama valida la sucursal contra el usuario: lo hace el servicio.
 */
public interface IChChequeReporte {

    /** 'R': cheques recibidos en caja (acciones {@code REC}) entre dos fechas, de una sucursal. */
    List<ChequeRecibidoRptDto> recibidos(long codSucursal, String desde, String hasta);

    /**
     * 'P': cheques recibidos entre dos fechas con su estado y cliente (cobranzas).
     *
     * @param estado     {@code PEN} / {@code CER}, o null = todos
     * @param codCliente codigo del cliente (CardCode SAP), o null = todos
     */
    List<ChequeCobranzaRptDto> cobranzas(long codSucursal, String desde, String hasta, String estado, String codCliente);

    /**
     * 'S': cheques entregados a custodia/cobranza.
     *
     * @param fecha       {@code dd/MM/yyyy}, o null = todos los dias
     * @param codEmpleado el cobrador, o null = todos
     */
    List<ChequeCustodioRptDto> custodio(long codSucursal, String fecha, Integer codEmpleado);

    /** 'D': el recibo de caja de un cheque, o null si no existe en esa sucursal. */
    ReciboChequeRptDto recibo(long codCheque, long codSucursal);

    /** 'E': el ultimo cheque que registro el usuario en la sucursal, o null si no registro ninguno. */
    Integer ultimoChequeDelUsuario(int codUsuario, long codSucursal);

    /** 'T': la nomina del ultimo traspaso de la sucursal. */
    List<ChequeTraspasoRptDto> ultimoTraspaso(long codSucursal);

    /** 'H': la nomina de un traspaso elegido ({@code codAccion} de su accion {@code TRASP}). */
    List<ChequeTraspasoRptDto> reimpresionTraspaso(long codSucursal, long codAccion);

    /** 'G': los traspasos de un dia, para elegir cual reimprimir. */
    List<HoraTraspasoChequeDto> horasDeTraspaso(long codSucursal, Date fecha);

    /** {@code p_list_NotaRemision} 'A': las notas de remision de un cheque (subreporte del reporte de custodio). */
    List<NotaRemisionRptDto> notasDeRemision(long codCheque);
}
