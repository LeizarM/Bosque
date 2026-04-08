package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.CambiosTigo;
import bo.bosque.com.impexpap.model.ChipTigo;
import bo.bosque.com.impexpap.utils.RespuestaSp;
import bo.bosque.com.impexpap.utils.Tipos;

import java.util.List;

public interface IChipTigo {
    // ABM: Registrar / Actualizar / Eliminar /
    RespuestaSp abmChipTigo(ChipTigo ct, String acc);
    // LISTA DE CHIPS PERDIDOS
    List<ChipTigo> listarPerdidas(ChipTigo filtro);
    //LISTA PERIODOS - PARA EL FILTRO DROPDOWN
    List <ChipTigo> listarPeriodos();
   //LISTA TIPO RENOVACION CHIP TIGO
    List<Tipos>listTipoRenovacion();
}
