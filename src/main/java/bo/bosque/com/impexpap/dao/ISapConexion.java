package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.SapConexion;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Las conexiones a SAP que comparten los módulos ({@code tsap_Conexion}). */
public interface ISapConexion {

    List<SapConexion> listar();

    /** {@code null} si no existe. */
    SapConexion obtener(int idConexion);

    /** Alta ({@code idConexion} null) o cambio; no toca la clave. En el alta, {@code idGenerado} es el id. */
    RespuestaSp guardar(SapConexion c, long audUsuario);

    /** {@code claveCifrada} ya viene cifrada por el backend. */
    RespuestaSp guardarClave(int idConexion, String claveCifrada, long audUsuario);

    /** Falla si algún módulo la usa. */
    RespuestaSp eliminar(int idConexion, long audUsuario);
}
