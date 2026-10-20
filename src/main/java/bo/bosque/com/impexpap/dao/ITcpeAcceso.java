package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.TcpeAccesoDto;
import bo.bosque.com.impexpap.dto.TcpeRegistroPostulanteDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/** Acceso de externos y claves: {@code p_list_tcpe_Acceso} y {@code p_abm_tcpe_Acceso} (contrato 3.6). */
public interface ITcpeAcceso {

    /**
     * {@code L}: el externo ({@code codEmpleado IS NULL}) con ese login, con su {@code password2} y sus
     * {@code examenesPendientes}; null si no existe.
     */
    TcpeAccesoDto porLogin(String login);

    /** {@code E}: el {@code tcpe_usuario} de un empleado, o null si todavía no tiene. */
    Long codUsuarioDeEmpleado(long codEmpleado);

    /** {@code M}: usuarios cuya {@code password2} todavía no es un hash. */
    List<TcpeAccesoDto> sinHash();

    /** {@code P}: guarda un hash BCrypt (el SP rechaza lo que no lo sea). */
    RespuestaSp guardarHash(long codUsuario, String hash, Long audUsuario);

    /** {@code R}: el postulante completa sus datos; {@code audUsuario} queda en NULL. */
    RespuestaSp registrar(long codUsuario, TcpeRegistroPostulanteDto datos);
}
