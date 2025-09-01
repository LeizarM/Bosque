package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.Movimiento;

public interface IMovimiento {

    boolean registrarMovimiento( Movimiento mb, String acc );
}
