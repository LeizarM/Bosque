package bo.bosque.com.impexpap.dao;

/**
 * La <b>escritura</b> de {@code text_SocioNegocio} (los clientes de SAP que usan Cheques y Depositos), por
 * {@code p_abm_SocioNegocio}. Las lecturas siguen en {@link ISocionegocio}; esta interfaz es aparte para no obligar
 * a quien solo lee (ni a sus pruebas) a implementar la escritura.
 */
public interface ISocioNegocioSap {

    /**
     * {@code p_abm_SocioNegocio} 'B': trae a la base del Bosque los clientes de SAP de todas las empresas
     * ({@code v_clientesSAP}: 1, 5, 7 y 8): inserta los que no estan y actualiza nombre, razon social, NIT y ciudad
     * de los que ya estan.
     *
     * <p>Se llama <b>tal como esta el procedimiento</b>, por nombre y sin salidas, igual que lo hace
     * {@code p_list_tdep_BancoXCuenta} 'A' (Depositos): solo {@code @audUsuario} y {@code @ACCION}. No devuelve nada: el
     * procedimiento no informa cuantos clientes trajo. El procedimiento atiende a todas las empresas a la vez, como el boton
     * del legacy: no se filtra por empresa.
     *
     * @param audUsuario quien pide la actualizacion (queda en {@code audUsuario} de todas las filas tocadas)
     * @throws RuntimeException con la causa original si la base falla (por ejemplo, SAP sin responder)
     */
    void actualizarDesdeSap(int audUsuario);
}
