// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IResponsableCierre.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.CandidatoCierreDto;
import bo.bosque.com.impexpap.dto.FuenteCierreDto;
import bo.bosque.com.impexpap.dto.FuenteCierreRequest;
import bo.bosque.com.impexpap.dto.CandidatoFuenteDto;
import bo.bosque.com.impexpap.dto.SeccionCierreDto;
import bo.bosque.com.impexpap.dto.SeccionCierreRequest;
import bo.bosque.com.impexpap.dto.ResponsableCierreDto;
import bo.bosque.com.impexpap.dto.ResponsableCierreRequest;
import bo.bosque.com.impexpap.dto.SucursalCajaDto;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * La matriz de responsables de Verificar Cierre (archivo SQL 81): quién revisa
 * qué sección y de qué sucursal. La deciden RRHH o un administrador desde el
 * organigrama.
 */
public interface IResponsableCierre {

    List<ResponsableCierreDto> listar(long idTarRuti);

    List<CandidatoCierreDto> candidatos(long idTarRuti);

    List<SucursalCajaDto> sucursales();

    /** [I] Si el cargo no tenía la tarea, el SP se la asigna. */
    RespuestaSp agregar(ResponsableCierreRequest req, long audUsuario);

    /** [U] */
    RespuestaSp cambiar(ResponsableCierreRequest req, long audUsuario);

    /** [D] Baja lógica (estado 0). */
    RespuestaSp quitar(long idRC, long audUsuario);

    /** [E] El catálogo de secciones (archivo SQL 86). */
    List<SeccionCierreDto> secciones(long idTarRuti);

    /** [W] Las fuentes que se pueden elegir para una sección nueva. */
    List<FuenteCierreDto> fuentes();

    /** [S] Alta (sin sección) o cambio de una sección. */
    RespuestaSp guardarSeccion(SeccionCierreRequest req, long audUsuario);

    /** [Q] estado 0: desde hoy no se pide; 1: se vuelve a pedir. */
    RespuestaSp quitarSeccion(String seccion, int estado, long audUsuario);

    /** [V] Todas las fuentes, también las inactivas (archivo SQL 87). */
    List<FuenteCierreDto> fuentesTodas();

    /** [O] Las consultas del cierre que existen y no se registraron. */
    List<CandidatoFuenteDto> candidatasFuente();

    /** [F] Registra una fuente o cambia su nombre, descripción y SP de marca. */
    RespuestaSp guardarFuente(FuenteCierreRequest req, long audUsuario);

    /** [A] estado 0 la desactiva; 1 la activa. */
    RespuestaSp estadoFuente(String fuente, int estado, long audUsuario);

    /** [B] La borra del registro si ninguna sección la usa. */
    RespuestaSp borrarFuente(String fuente, long audUsuario);
}
