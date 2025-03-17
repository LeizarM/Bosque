package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.BancoXCuenta;

import java.util.List;

public interface IBancoXCuenta {

    List<BancoXCuenta> listarBancosXCuentas( int codEmpresa );
}
