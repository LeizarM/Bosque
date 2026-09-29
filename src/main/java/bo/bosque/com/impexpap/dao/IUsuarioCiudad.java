package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.model.CiudadVenta;
import bo.bosque.com.impexpap.utils.RespuestaSp;

/**
 * Ciudades que cada usuario puede ver en el catalogo de Ventas
 * ({@code tven_UsuarioCiudad}).
 */
public interface IUsuarioCiudad {

    /** Ciudades con sucursal de venta (la 3 ya viene como 1). */
    List<CiudadVenta> ciudadesVenta();

    /** Ciudades asignadas a un usuario; vacia si no tiene excepciones. */
    List<CiudadVenta> asignadasA(long codUsuario);

    /** Todas las asignaciones, para la pantalla de gestion. */
    List<CiudadVenta> todasLasAsignaciones();

    RespuestaSp asignar(long codUsuario, int codCiudad, long audUsuario);

    RespuestaSp quitar(long codUsuario, int codCiudad, long audUsuario);
}
