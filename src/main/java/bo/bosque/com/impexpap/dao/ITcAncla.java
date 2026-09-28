package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.TcAncla;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Acceso a la tabla <b>tpr_tcAncla</b> (configuracion del reprecio nocturno).
 *
 * <p>Alcance deliberadamente acotado: lectura del ancla mas los ABM que ya existen
 * en {@code p_abm_tcAncla}. La logica del reprecio (calcular el delta, decidir si
 * dispara, repreciar {@code tpr_precio}) NO se implementa aqui: esta fuera de alcance.
 *
 * <p>Procedimientos: {@code p_abm_tcAncla} (acciones A, B, L, M, S, U) y
 * {@code p_list_tcAncla} (accion L, creado en la migracion).
 */
public interface ITcAncla {

    /**
     * Devuelve todas las anclas configuradas, una por empresa.
     * Invoca {@code p_list_tcAncla} con ACCION 'L' sin filtro.
     *
     * @return lista de anclas ordenada por companyDB; vacia si no hay ninguna
     */
    List<TcAncla> listar();

    /**
     * Devuelve el ancla de una empresa puntual.
     * Invoca {@code p_list_tcAncla} con ACCION 'L' filtrando por {@code @companyDB}.
     *
     * @param companyDB base de datos SAP de la empresa (PK de la tabla)
     * @return el ancla de esa empresa, o {@code null} si todavia no fue sembrada
     */
    TcAncla obtenerPorCompanyDB(String companyDB);

    /**
     * Siembra el ancla de una empresa que todavia no la tiene.
     * Invoca {@code p_abm_tcAncla} con ACCION 'A'.
     *
     * @param companyDB  base de datos SAP de la empresa (obligatorio)
     * @param tc         tipo de cambio inicial del ancla (obligatorio)
     * @param umbral     variacion minima para disparar reprecio; {@code null} = 0.10
     * @param fechaAncla fecha del ancla; {@code null} = 1900-01-01, es decir
     *                   "nada sincronizado todavia"
     * @return respuesta del SP; {@code idGenerado} siempre 0 (la PK no es IDENTITY)
     */
    RespuestaSp sembrarAncla(String companyDB, BigDecimal tc, BigDecimal umbral, Date fechaAncla);

    /**
     * Borra el ancla de una empresa para poder volver a sembrarla desde cero.
     * Invoca {@code p_abm_tcAncla} con ACCION 'B'.
     *
     * @param companyDB base de datos SAP de la empresa
     * @return respuesta del SP
     */
    RespuestaSp eliminarAncla(String companyDB);

    /**
     * Mueve el ancla despues de un reprecio aplicado con exito: fija
     * {@code tcAncla}, {@code fechaAncla}, {@code tcUltimo} y {@code fechaUltimo}.
     * Invoca {@code p_abm_tcAncla} con ACCION 'M'.
     *
     * <p>Lo llama el job nocturno una vez que el reprecio termino bien; no es una
     * operacion de pantalla.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param tc        tipo de cambio que pasa a ser el nuevo ancla
     * @return respuesta del SP
     */
    RespuestaSp moverAncla(String companyDB, BigDecimal tc);

    /**
     * Registra el tipo de cambio observado sin mover el ancla (no hubo disparo):
     * actualiza solo {@code tcUltimo} y {@code fechaUltimo}.
     * Invoca {@code p_abm_tcAncla} con ACCION 'S'.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param tc        tipo de cambio observado
     * @return respuesta del SP
     */
    RespuestaSp registrarTcVisto(String companyDB, BigDecimal tc);

    /**
     * Ajusta el umbral de disparo de una empresa, sin tocar el ancla.
     * Invoca {@code p_abm_tcAncla} con ACCION 'U'.
     *
     * @param companyDB base de datos SAP de la empresa
     * @param umbral    nueva variacion minima (proporcion: 0.10 = 10 %)
     * @return respuesta del SP
     */
    RespuestaSp actualizarUmbral(String companyDB, BigDecimal umbral);
}
