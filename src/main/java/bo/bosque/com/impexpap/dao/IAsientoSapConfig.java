package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.AsientoSapConfig;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Configuración del asiento de planilla en SAP por empresa ({@code tsap_AsientoPlanilla}). */
public interface IAsientoSapConfig {

    List<AsientoSapConfig> listar();

    /** {@code null} si la empresa no tiene configuración. */
    AsientoSapConfig obtener(int codEmpresa);

    /** Alta o cambio; no toca la clave del Service Layer. */
    RespuestaSp guardar(AsientoSapConfig c, long audUsuario);

    /** {@code claveCifrada} ya viene cifrada por el backend. */
    RespuestaSp guardarClave(int codEmpresa, String claveCifrada, long audUsuario);

    RespuestaSp eliminar(int codEmpresa, long audUsuario);
}
