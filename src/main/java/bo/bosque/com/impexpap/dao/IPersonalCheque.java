package bo.bosque.com.impexpap.dao;

import java.util.List;

import bo.bosque.com.impexpap.dto.PersonalChequeDto;

/**
 * Las personas que intervienen en un cheque, segun las funciones del modulo 41
 * ({@code tb_funcionesXModulo} / {@code tb_asigFunModXEmpl}): ver {@link PersonalChequeDto}.
 */
public interface IPersonalCheque {

    /**
     * {@code p_list_Empleado} 'U': jefe de cobranzas y cobradores activos de la sucursal. Es el
     * listado del responsable de custodia ("A Custodio").
     */
    List<PersonalChequeDto> responsablesDeCustodia(long codSucursal);

    /**
     * {@code p_list_Empleado} 'C': igual que 'U' mas los choferes. Es el listado de "Entregado por"
     * del formulario de cheque.
     */
    List<PersonalChequeDto> quienesEntregan(long codSucursal);
}
