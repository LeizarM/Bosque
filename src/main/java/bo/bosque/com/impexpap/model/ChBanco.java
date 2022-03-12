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
public class ChBanco implements Serializable {
	
	private int codBanco;
    private String nombre;
    private int audUsuario;

    private int fila;

    public ChBanco( int codBanco, String nombre ) {
        this.codBanco = codBanco;
        this.nombre = nombre;
    }
    
    public ChBanco( int codBanco, String nombre, int fila) {
        this.codBanco = codBanco;
        this.nombre = nombre;
        this.fila = fila;
    }
    
}
