package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * La vista de detalle de un cheque ("Completar" en el legacy): el cheque, su historial de acciones y
 * cuales de las cuatro acciones estan habilitadas. Se entrega junto para que la pantalla no haga tres
 * llamadas y para que las tres cosas sean del mismo instante.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChequeDetalleDto implements Serializable {

    private ChequeFilaDto cheque;
    private List<AccionChequeDto> acciones;
    private BotonesChequeDto botones;
}
