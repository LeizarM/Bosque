package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.NotaRemision;

import java.util.List;

public interface INotaRemision {

    boolean registrarNotaRemision( NotaRemision mb, String acc );

    List<NotaRemision> listarNotasRemisiones( NotaRemision mb);
}
