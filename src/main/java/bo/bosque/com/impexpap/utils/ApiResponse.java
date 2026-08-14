// En utils/ApiResponse.java (archivo existente en estructura)
package bo.bosque.com.impexpap.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponse<T> {

    private String message;
    private T data;
    private int status;

    /**
     * <b>Sólo en los 400 que el usuario PUEDE confirmar.</b> Marca la diferencia entre "repetiste
     * algo legítimo, decime si va igual" y "esto está mal, arreglalo": las dos salen 400 con un
     * mensaje, y sin este marcador el cliente tendría que adivinar por el texto para saber si
     * ofrecer el botón "Confirmar".
     *
     * <p>Va en el <b>nivel superior del cuerpo</b> y no adentro de {@code data} porque ahí es donde
     * el cliente ya mira el error ({@code e.response.data['message']}); {@code data} viaja en
     * {@code null} en todos los errores.
     *
     * <p>{@code Boolean} + {@code NON_NULL}: queda fuera del JSON en las otras miles de respuestas
     * del backend, así que agregarlo no cambió el contrato de ningún otro módulo.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean confirmable;

    /**
     * El constructor de siempre, ahora explícito.
     *
     * <p>Era {@code @AllArgsConstructor}, y con el campo nuevo Lombok habría generado uno de cuatro
     * argumentos rompiendo las ~200 llamadas de tres que hay en el repo. Escrito a mano, el campo
     * nuevo se pone con su setter y nadie más se entera.
     */
    public ApiResponse(String message, T data, int status) {
        this.message = message;
        this.data = data;
        this.status = status;
    }
}
