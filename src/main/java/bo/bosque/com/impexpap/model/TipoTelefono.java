package bo.bosque.com.impexpap.model;


import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tabla {@code trh_tipoTelefono}. {@code audUsuario} es la columna {@code audUsuarioI}.
 * {@code usos} solo lo llena {@code p_list_TipoTelefono 'P'}: cuántos teléfonos usan el tipo.
 */
@Data
@NoArgsConstructor
public class TipoTelefono {


    private int codTipoTel;
    private String tipo;
    private int audUsuario;
    private int usos;


}

