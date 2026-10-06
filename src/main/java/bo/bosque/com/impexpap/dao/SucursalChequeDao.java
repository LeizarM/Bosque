package bo.bosque.com.impexpap.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import bo.bosque.com.impexpap.dto.EmpresaChequeDto;
import bo.bosque.com.impexpap.dto.SucursalChequeDto;
import bo.bosque.com.impexpap.utils.SpHelper;

/** Ver {@link ISucursalCheque}. Todo pasa por {@code p_list_Sucursal} y {@code p_list_Empresa}. */
@Repository
public class SucursalChequeDao implements ISucursalCheque {

    private static final String SP = "p_list_Sucursal";
    private static final String SP_EMPRESA = "p_list_Empresa";

    private final SpHelper spHelper;

    public SucursalChequeDao(SpHelper spHelper) {
        this.spHelper = spHelper;
    }

    @Override
    public boolean puedeEscribir(long codSucursal, int codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codSucursal", codSucursal);
        p.put("audUsuarioI", codUsuario);
        // Con el SP actual el primer result set tiene 6 filas de 3 columnas (ver ISucursalCheque), asi que esto
        // da true siempre; con el SP corregido es el count(*) unico. En los dos casos "> 0" es lo que hace el legacy.
        List<Integer> filas = spHelper.ejecutarListadoPorPosicion(SP, p, "D", (rs, i) -> rs.getInt(1));
        return !filas.isEmpty() && filas.get(0) > 0;
    }

    @Override
    public long sucursalInicial(int codEmpresa, int codUsuario, long codSucursalPrevia) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        p.put("audUsuarioI", codUsuario);
        if (codSucursalPrevia > 0) p.put("codCiudad", codSucursalPrevia);   // el SP la usa para preferirla
        List<Long> filas = spHelper.ejecutarListadoPorPosicion(SP, p, "E", (rs, i) -> rs.getLong(1));
        return filas.isEmpty() ? 0L : filas.get(0);
    }

    @Override
    public List<SucursalChequeDto> sucursalesDeEmpresa(int codEmpresa, int codUsuario) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("codEmpresa", codEmpresa);
        // El legacy manda NULL cuando no hay usuario (SucursalDao.listadoSucursalCheqs); omitirlo es lo mismo.
        if (codUsuario > 0) p.put("audUsuarioI", codUsuario);
        // Columnas: codSucursal, nombre, codEmpresa, codCiudad, origen. El union all del SP puede repetir una
        // sucursal; el combo del legacy no la deduplicaba, aqui si (un desplegable con dos iguales no sirve).
        List<SucursalChequeDto> filas = spHelper.ejecutarListadoPorPosicion(SP, p, "C",
                (rs, i) -> new SucursalChequeDto(rs.getInt(1), rs.getString(2)));
        Map<Integer, SucursalChequeDto> unicas = new LinkedHashMap<>();
        for (SucursalChequeDto s : filas) unicas.putIfAbsent(s.getCodSucursal(), s);
        return new ArrayList<>(unicas.values());
    }

    @Override
    public List<EmpresaChequeDto> empresasDeCheques() {
        // Columnas: codEmpr, datoEmpresa, origen. Un union all: una empresa puede salir dos veces (origen 0 y 1).
        List<EmpresaChequeDto> filas = spHelper.ejecutarListadoPorPosicion(SP_EMPRESA, new LinkedHashMap<>(), "C",
                (rs, i) -> new EmpresaChequeDto(rs.getInt(1), rs.getString(2)));
        Map<Integer, EmpresaChequeDto> unicas = new LinkedHashMap<>();
        for (EmpresaChequeDto e : filas) unicas.putIfAbsent(e.getCodEmpresa(), e);
        return new ArrayList<>(unicas.values());
    }
}
