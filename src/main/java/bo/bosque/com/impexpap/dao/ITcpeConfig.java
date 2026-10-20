package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeConfigDto;

import java.util.List;

/** Configuración del módulo de exámenes: {@code p_list_tcpe_Config} (contrato 7.1). */
public interface ITcpeConfig {

    /** {@code L}: todas las claves con su valor y descripción. */
    List<TcpeConfigDto> listar();
}
