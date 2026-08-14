package bo.bosque.com.impexpap.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Un día del rango que <b>no descuenta</b> vacación, y por qué.
 *
 * <p>Existe para que la pantalla pueda decir «del 12 al 20 son 9 días corridos, se cobran 6»
 * mostrando cuáles son los otros 3 y el motivo de cada uno. Antes el usuario veía sólo el total
 * y tenía que confiar.
 *
 * <p><b>Los domingos no vienen acá.</b> El cliente los sabe solo a partir de la fecha, y mandarlos
 * por la red sería llenar la respuesta con lo único que no hace falta explicar.
 */
@Getter
@Setter
public class DiaNoHabilDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date fecha;

    /**
     * {@code FERIADO} o {@code SABADO_LIBRE}. Lo consume la UI para elegir el ícono y el color;
     * el texto que se muestra va en {@link #motivo}.
     */
    private String tipo;

    /** Para un feriado, el nombre que tiene cargado; para un sábado, por qué no le toca. */
    private String motivo;

    public static final String FERIADO = "FERIADO";
    public static final String SABADO_LIBRE = "SABADO_LIBRE";
}
