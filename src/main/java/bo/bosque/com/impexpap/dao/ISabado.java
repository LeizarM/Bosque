package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.DiaNoLaborableDto;
import bo.bosque.com.impexpap.model.Sabado;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Los sábados del período.
 * SPs: {@code p_abm_trs_Sabado} · {@code p_list_trs_Sabado}.
 */
public interface ISabado {

    /**
     * Sólo admite 'U'. Los sábados los genera {@code trs_sp_generarRol} caminando
     * de 7 en 7 días: un INSERT suelto rompería la secuencia de {@code indiceAnio},
     * de la que depende la rotación A/B. Y no se eliminan — se desactivan.
     *
     * <p>Con {@code alcanceEvento} + {@code motivoEspecial} declara el evento del día
     * y rehace sus celdas; con {@code activo} lo activa o desactiva.
     *
     * @param sabado Datos a modificar
     * @param acc    'U'
     */
    RespuestaSp registrarSabado(Sabado sabado, String acc);

    /** [L] Los sábados de un rol. */
    List<Sabado> obtenerSabados(long idRol);

    /** [L] Un sábado por su id. */
    Sabado obtenerSabadoPorId(long idSabado);

    /** [C] Cobertura por sábado: el SUBTOTAL de abajo del Excel. @param mes 0 = todos */
    List<Sabado> obtenerCobertura(long idRol, int mes);

    /**
     * [F] Sábados cuya marca de feriado NO coincide con RR.HH., en los dos sentidos.
     * Es el control a mirar cuando cargan un feriado DESPUÉS de generar el rol
     * (el caso del puente: feriado el viernes + sábado declarado aparte).
     * @param idRol 0 = todos los roles
     */
    List<Sabado> obtenerFeriadosDesincronizados(long idRol);

    /**
     * [N] Los días no laborables de RR.HH., con día de semana, si caen sábado, si
     * son puente de viernes y a cuántas sucursales alcanzan.
     * @param anio        0 = todos
     * @param codSucursal 0 = sin filtrar por sucursal
     */
    List<DiaNoLaborableDto> obtenerDiasNoLaborables(int anio, long codSucursal);
}
