package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.MiEquipoDto;
import bo.bosque.com.impexpap.dto.ProgramadorDependienteDto;
import bo.bosque.com.impexpap.model.Programador;
import bo.bosque.com.impexpap.utils.RespuestaSp;

import java.util.List;

/**
 * Los jefes con permiso de programar a otros.
 * SPs: {@code p_abm_trs_Programador} · {@code p_list_trs_Programador}.
 */
public interface IProgramador {

    /**
     * Da de alta, modifica o da de BAJA LÓGICA ({@code estado='I'}) a un programador.
     * Un borrado físico dejaría huérfanas las programaciones que ya hizo.
     *
     * <p>En el alta, el SP devuelve en {@code errormsg} CUÁNTOS dependientes le da
     * el organigrama. Si son 0, el permiso no sirve de nada — conviene enterarse
     * ahí y no cuando intente programar.
     *
     * @param programador Datos del programador
     * @param acc         'I' | 'U' | 'D'
     */
    RespuestaSp registrarProgramador(Programador programador, String acc);

    /** [L] Los programadores, con su reemplazo y cuántos dependientes tienen hoy. */
    List<Programador> obtenerProgramadores(long codSucursal, String estado);

    /** [L] Un programador por su id. */
    Programador obtenerProgramadorPorId(long idProgramador);

    /**
     * [D] El organigrama expandido de un jefe (recursivo sobre
     * {@code trh_cargo.codCargoPadre}). {@code profundidad} 1 = reportes directos.
     * @param codEmpleado 0 = todos los programadores
     */
    List<ProgramadorDependienteDto> obtenerDependientes(long codEmpleado);

    /**
     * [P] A quiénes le quedaría a cargo un permiso que TODAVÍA NO EXISTE.
     *
     * <p>Es la misma pregunta que {@link #obtenerDependientes(long)}, pero antes de
     * guardar. Esa no sirve para esto: arranca de {@code trs_Programador}, así que
     * sobre un jefe que no está cargado devuelve vacío, y no tiene forma de recibir
     * un alcance hipotético.
     *
     * <p>Las dos salen del MISMO árbol ({@code fn_trs_DependientePorCargo}), y eso es
     * lo que importa: si fueran dos recorridos distintos, la pantalla mostraría un
     * equipo y {@code trs_sp_programar} validaría contra otro.
     *
     * <p><b>{@code alcance} y {@code codSucursal} son dos cosas distintas</b> y se
     * combinan libremente: el alcance dice cuán HONDO se baja y la sucursal cuán ANCHO.
     * Confundirlas ya costó caro — un JEFE COMERCIAL Y DE PRODUCCIÓN tenía a su
     * RESPONSABLE DE PRODUCCIÓN colgando DIRECTO pero en otra planta, y con la sucursal
     * fija no aparecía con ningún alcance, porque la sucursal corta antes que la
     * profundidad.
     *
     * @param codEmpleado el futuro jefe
     * @param codSucursal <b>0 = todas las sucursales</b>, igual que el NULL que guarda
     *                    {@code trs_Programador.codSucursal}. Es el mismo idioma que la
     *                    tabla a propósito: lo que se previsualiza tiene que ser lo que
     *                    se guarda
     * @param alcance     {@code SUBARBOL} | {@code DIRECTOS}
     * @param idRol       para marcar cuáles además participan del rol
     */
    List<ProgramadorDependienteDto> previsualizarDependientes(
            long codEmpleado, long codSucursal, String alcance, long idRol);

    // ==================================================================
    // "SU EQUIPO" — la identidad la resuelve el servidor, no el cliente
    // ==================================================================

    /**
     * [E] Quién es el que llama y si puede programar a alguien.
     *
     * <p>La identidad se resuelve SÓLO por el login del token: {@code login → tb_usuario
     * → codEmpleado → trs_Programador}. No hay forma de pedir "el permiso de otro",
     * justamente para que nadie programe la gente ajena mandando un {@code codEmpleado}
     * inventado en el body.
     *
     * <p>Entra tanto el titular como su reemplazo ({@code estado='A' AND (codEmpleado=@codEmp
     * OR codEmpleadoReemplazo=@codEmp)}), que es el mismo criterio que aplica
     * {@code trs_sp_programar} al escribir.
     *
     * @param login el {@code auth.getName()} del token
     * @return siempre un DTO, nunca {@code null}; con {@code esProgramador=0} si no lo es
     */
    MiEquipoDto resolverPermiso(String login);

    /**
     * [D] Los dependientes de UN permiso, buscados por {@code idProgramador}.
     *
     * <p><b>Por qué por {@code idProgramador} y no por {@code codEmpleado}:</b> porque así
     * el REEMPLAZO hereda el equipo del titular sin tocar
     * {@code fn_trs_ProgramadorDependiente()}, que no sabe nada de
     * {@code codEmpleadoReemplazo} — camina el organigrama desde el jefe. Si buscáramos
     * por el {@code codEmpleado} del que llama, el suplente vería su propio árbol (o
     * ninguno) en vez del que en realidad puede programar.
     */
    List<ProgramadorDependienteDto> obtenerDependientesPorProgramador(long idProgramador);

    /**
     * Lo que consume la pestaña "Su Equipo": el permiso del que llama + su gente.
     *
     * @param login el {@code auth.getName()} del token
     */
    MiEquipoDto obtenerMiEquipo(String login);
}
