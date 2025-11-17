package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Data
public class VistaUsuario implements Serializable {

    private int codUsuario;
    private int codVista;
    private int nivelAcceso;
    private int autorizador;
    private int audUsuarioI;


    /**
     * Atributos de apoyo o auxiliares
     */

    private int fila;
    private int codVistaPadre;
    private int codBoton;
    private String direccion;
    private String nombreComponente;
    private String modulo;
    private String vista;
    private String boton;
    private String descripcion;
    private String imagen; //hasta el momento no se utiliza
    private int nivelAccesoBoton;
    private String tipo;








}
