package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.FamiliaPolitica;
import bo.bosque.com.impexpap.model.DescuentoDetalle;
import bo.bosque.com.impexpap.model.FamiliaSapOpcion;
import bo.bosque.com.impexpap.model.VendedorClienteExcluido;
import bo.bosque.com.impexpap.model.VendedorExentoBond;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.Date;
import java.util.List;

/**
 * La politica del descuento por familia, configurable sin tocar codigo.
 * <p>
 * Las tres tablas viven juntas porque se administran desde la misma pantalla,
 * pero son reglas distintas: las dos primeras son el descuento Bond, la tercera
 * es la exclusion de clientes por vendedor.
 */
public interface IPoliticaBond {

    // ---------- Detalle de lo descontado ----------

    /**
     * Que se esta descontando, item por item.
     *
     * @param accion P periodo abierto, H historico ya pagado, R resumen por
     *               vendedor y familia
     */
    List<DescuentoDetalle> obtenerDescuentoDetalle(Integer mes, Integer anio,
                                                   String origen, Long idVendedor,
                                                   String accion);

    // ---------- Familias de SAP ----------

    /** Todas las familias. disponibles=true trae solo las que aun no tienen
     *  politica activa, que es lo que se ofrece al agregar una regla. */
    List<FamiliaSapOpcion> obtenerFamiliasSap(boolean disponibles);

    // ---------- Politica por familia ----------

    /** Alta, modificacion o baja logica. acc: I, U, D. */
    RespuestaSp registrarFamiliaPolitica(FamiliaPolitica mb, String acc);

    /** Todas, con su historial de vigencias. */
    List<FamiliaPolitica> obtenerFamiliaPolitica();

    /** Solo lo que rige en esa fecha. Sin fecha, hoy. */
    List<FamiliaPolitica> obtenerFamiliaPoliticaVigente(Date fecha);

    // ---------- Vendedores exentos ----------

    RespuestaSp registrarVendedorExento(VendedorExentoBond mb, String acc);

    List<VendedorExentoBond> obtenerVendedoresExentos();

    List<VendedorExentoBond> obtenerVendedoresExentosVigentes(Date fecha);

    // ---------- Clientes excluidos ----------

    RespuestaSp registrarClienteExcluido(VendedorClienteExcluido mb, String acc);

    List<VendedorClienteExcluido> obtenerClientesExcluidos();

    List<VendedorClienteExcluido> obtenerClientesExcluidosVigentes(Date fecha);
}
