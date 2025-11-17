package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;


@Getter
@Setter
@NoArgsConstructor
@Data
public class UsuarioBtn implements Serializable {

    private int codUsuario;
    private int codBtn;
    private int nivelAcceso;
    private int audUsuario;



    private String boton;
    private  int permiso;
    private int pertenVist;

    /**
     * Constructores
     */
    public UsuarioBtn(int codUsuario, int codBtn, int nivelAcceso, int audUsuario) {
        this.codUsuario = codUsuario;
        this.codBtn = codBtn;
        this.nivelAcceso = nivelAcceso;
        this.audUsuario = audUsuario;
    }
}
