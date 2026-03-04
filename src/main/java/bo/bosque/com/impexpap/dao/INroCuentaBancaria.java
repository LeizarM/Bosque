package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NroCuentaBancaria;

import java.util.List;

public interface INroCuentaBancaria {

    List<NroCuentaBancaria> obtenerCuentasBanco (int codEmpleado);

    boolean registrarCuentaBancaria (NroCuentaBancaria cb,String acc);

}
