package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Rrhh;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * El padrón de RR.HH.: quiénes pueden corregir CUALQUIER celda del rol de sábados.
 * SPs: {@code p_abm_trs_Rrhh} · {@code p_list_trs_Rrhh}.
 */
public interface IRrhh {

    /**
     * Da de alta, actualiza o da de BAJA LÓGICA ({@code estado='I'}).
     *
     * <p>El alta REACTIVA si la persona ya estuvo: la tabla tiene UNIQUE sobre
     * {@code codEmpleado}, y sacar a alguien por unos meses y volver a ponerlo es el
     * caso normal. Sin eso, el segundo alta reventaría contra la constraint con un
     * "violación de índice único" que no le dice nada a nadie.
     *
     * @param acc 'I' | 'U' | 'D'
     */
    RespuestaSp registrarRrhh(Rrhh rrhh, String acc);

    /**
     * [L] El padrón, con nombre, cargo y sucursal resueltos.
     *
     * @param estado 'A', 'I', o vacío/null para traer los dos
     */
    List<Rrhh> obtenerRrhh(String estado);
}
