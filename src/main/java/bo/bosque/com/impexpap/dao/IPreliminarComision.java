package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NotaPreliminar;
import bo.bosque.com.impexpap.model.PreliminarComision;

import java.math.BigDecimal;
import java.util.List;

/**
 * Vistas preliminares de comisiones.
 * <p>
 * Llama al SP p_list_paraPagar que ya existe, con las mismas letras de ACCION
 * que usa Bosque v2. La migracion no cambia la matematica: si el numero difiere
 * del sistema viejo, es un defecto de la migracion y no una mejora.
 */
public interface IPreliminarComision {

    /** Rama F. Preliminar de vendedores internos. */
    List<PreliminarComision> preliminarInterno(int mes, int anio, BigDecimal tc);

    /** Rama I. Preliminar de vendedores externos. */
    List<PreliminarComision> preliminarExterno(int mes, int anio, BigDecimal tc);

    /** Rama J. Comision dinamica, modalidad anterior. */
    List<PreliminarComision> preliminarDinamicaAnterior(int mes, int anio, BigDecimal tc);

    /** Rama K. Comision dinamica, modalidad vigente. */
    List<PreliminarComision> preliminarDinamicaVigente(int mes, int anio, BigDecimal tc);

    /**
     * Rama G1. Las notas que componen UNA fila del preliminar.
     * <p>
     * Es el detalle que en Bosque v2 abria «Ver Notas a Pagar»: las facturas
     * cerradas y sin pagar de ese vendedor, en ese periodo, cuya diferencia de
     * dias las dejo en ese tramo. Cierra con una fila de total.
     *
     * @param comisionCad el factor como cadena; el SP lo declara VARCHAR(6) y lo
     *                    compara contra una expresion float
     */
    List<NotaPreliminar> notasDeFila(int idVendedor, int mes, int anio, String comisionCad);
}
