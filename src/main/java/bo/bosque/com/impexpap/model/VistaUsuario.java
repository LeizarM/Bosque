package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class VistaUsuario implements Serializable {

    private int codUsuario;
    private int codVista;
    private int nivelAcceso;
    private int autorizador;
    private int audUsuarioI;


    public VistaUsuario(int codUsuario, int codVista, int nivelAcceso, int autorizador, int audUsuarioI) {
        this.codUsuario = codUsuario;
        this.codVista = codVista;
        this.nivelAcceso = nivelAcceso;
        this.autorizador = autorizador;
        this.audUsuarioI = audUsuarioI;
    }
}
