package bo.bosque.com.impexpap.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Accessors( chain = true )
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
    private int esRaiz;
    private int autorizar;
    private int audUsuarioI;

    /**
     * Variables de Apoyo
     */
    private int fila;
    private List<Vista> items = new ArrayList<Vista>();
    private String label; // label para desplegarlo en el arbol de primeNG
    private int tieneHijo;
    private String routerLink; // router de Angular
    private String icon; // icono de PrimeNG
    private String path;

    /**
     * Constructores
     */
    public Vista(int codVista, int codVistaPadre, String direccion, String titulo, String descripcion, String imagen, int esRaiz, int autorizar, int audUsuarioI) {
        this.codVista       = codVista;
        this.codVistaPadre  = codVistaPadre;
        this.direccion      = direccion;
        this.titulo         = titulo;
        this.descripcion    = descripcion;
        this.imagen         = imagen;
        this.esRaiz         = esRaiz;
        this.autorizar      = autorizar;
        this.audUsuarioI    = audUsuarioI;
    }

    public Vista(int fila, int codVista, int codVistaPadre, String direccion, String titulo, String descripcion, String imagen, int esRaiz, int autorizar, List<Vista> items, String label, int tieneHijo, String routerLink, String icon, String path,int audUsuarioI) {
        this.fila           = fila;
        this.codVista       = codVista;
        this.codVistaPadre  = codVistaPadre;
        this.direccion      = direccion;
        this.titulo         = titulo;
        this.descripcion    = descripcion;
        this.imagen         = imagen;
        this.esRaiz         = esRaiz;
        this.autorizar      = autorizar;
        this.items          = items;
        this.titulo         = label;
        this.tieneHijo      = tieneHijo;
        this.routerLink     = routerLink;
        this.icon           = icon;
        this.path           = path;
        this.audUsuarioI    = audUsuarioI;
    }



}
