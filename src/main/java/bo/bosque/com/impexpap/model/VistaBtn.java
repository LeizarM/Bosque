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
public class VistaBtn implements Serializable {

    private int codBtn;
    private int codVista;
    private String nombreBtn;
    private String detalle;
    private int audUsuarioI;

    /**
     * Constructores
     */
    public VistaBtn( int codBtn, int codVista, String nombreBtn, String detalle, int audUsuarioI ) {
        this.codBtn = codBtn;
        this.codVista = codVista;
        this.nombreBtn = nombreBtn;
        this.detalle = detalle;
        this.audUsuarioI = audUsuarioI;
    }
}
