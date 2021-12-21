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
public class Vista implements Serializable {

    private int codVista;
    private int codVistaPadre;
    private String direccion; //la url de la vista
    private String titulo;
    private String descripcion;
    private String imagen; //nombre de la imagen
    private String esRaiz;
    private int autorizar;
    private int audUsuarioI;

    /**
     * Constructores
     */
    public Vista(int codVista, int codVistaPadre, String direccion, String titulo, String descripcion, String imagen, String esRaiz, int autorizar, int audUsuarioI) {
        this.codVista = codVista;
        this.codVistaPadre = codVistaPadre;
        this.direccion = direccion;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.imagen = imagen;
        this.esRaiz = esRaiz;
        this.autorizar = autorizar;
        this.audUsuarioI = audUsuarioI;
    }
}
