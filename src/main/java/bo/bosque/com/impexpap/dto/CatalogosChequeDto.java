package bo.bosque.com.impexpap.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Todos los combos fijos de la pantalla de cheques, en una sola respuesta. Son copias de
 * {@code v_tipos} grupos 21 a 24 y de los combos por boton del legacy (ver {@code Tipos}).
 *
 * <p>Los tres {@code acciones*} son los estados que cada boton del detalle deja elegir: "Fecha Cobro"
 * ({@code VEN}, {@code ADE}), "Cerrar con verificacion" ({@code COB}) y "Cerrar sin verificacion"
 * ({@code CEF}, {@code CCH}, {@code PAP}, {@code DPR}). "Devolver" siempre es {@code DEV}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogosChequeDto implements Serializable {

    private List<OpcionChequeDto> tiposCheque;
    private List<OpcionChequeDto> monedas;
    private List<OpcionChequeDto> estadosCheque;
    private List<OpcionChequeDto> estadosAccion;
    private List<OpcionChequeDto> accionesFechaCobro;
    private List<OpcionChequeDto> accionesCierreConVerificacion;
    private List<OpcionChequeDto> accionesCierreSinVerificacion;
}
