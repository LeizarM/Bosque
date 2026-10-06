package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;

/**
 * La parte de {@code p_list_Sucursal} y {@code p_list_Empresa} que usa el modulo de cheques.
 *
 * <p>Las ramas {@code D} y {@code E} de {@code p_list_Sucursal} <b>consultan una tabla fija escrita dentro del
 * propio procedimiento</b> (usuario, sucursal, empresa) mas tres {@code codUsuario} con acceso total, y la
 * {@code C} acota a unos usuarios a una sola sucursal. Por la decision 2 de CLAUDE.md el backend las
 * <b>llama</b> y no copia esas tablas a Java: si alguien cambia el procedimiento, el legacy y este backend
 * cambian juntos.
 *
 * <p><b>La rama {@code D} esta rota:</b> un {@code --union all} comentado (linea 215) deja las filas de
 * tres usuarios fuera del {@code INSERT} y salen como un {@code SELECT} suelto, asi que devuelve DOS result
 * sets. El primero (3 columnas, 6 filas) es el que leen el legacy ({@code rs.getInt(1)}, ultima fila = 13)
 * y {@code JdbcTemplate} (primera fila = 1): ambos dan "autorizado" siempre.
 */
public interface ISucursalCheque {

    /**
     * {@code p_list_Sucursal} 'D': si el usuario puede <b>escribir</b> en la sucursal
     * ({@code SucursalManagedBean.sucPermiss}). El administrador se resuelve aparte, con el rol.
     */
    boolean puedeEscribir(long codSucursal, int codUsuario);

    /**
     * {@code p_list_Sucursal} 'E': la sucursal con la que el usuario abre la pantalla <b>en esa empresa</b>, o 0
     * si no tiene ({@code SucursalDao.sucPorUsr}).
     *
     * @param codSucursalPrevia la que ya estaba elegida: si el usuario tiene permiso sobre ella la
     *                          conserva; 0 = ninguna
     */
    long sucursalInicial(int codEmpresa, int codUsuario, long codSucursalPrevia);

    /**
     * {@code p_list_Sucursal} 'C': las sucursales del combo de la pantalla para una empresa
     * ({@code SucursalDao.listadoSucursalCheqs}): solo las parametrizadas en {@code tch_ParametroSuc}, y unos
     * usuarios ven una sola. <b>No es la rama {@code L}</b> (todas las sucursales de la empresa, mas una fila
     * "-- Defina Nueva Sucursal --" con codigo -1). Sin repetidas, en el orden del procedimiento.
     */
    List<SucursalChequeDto> sucursalesDeEmpresa(int codEmpresa, int codUsuario);

    /**
     * {@code p_list_Empresa} 'C': las empresas del combo "Empresa" ({@code EmpresaDao.listEmprCheqs}), sin
     * repetir y por codigo. Hoy: 1 IMPEXPAP y 5 ESPPAPEL (las que figuran en {@code tch_ParametroSuc}).
     */
    List<EmpresaChequeDto> empresasDeCheques();
}
