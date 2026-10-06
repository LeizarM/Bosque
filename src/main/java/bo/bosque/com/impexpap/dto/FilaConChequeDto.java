package bo.bosque.com.impexpap.dto;

/**
 * Lo que comparten las filas de Verificar Cheques que traen un cheque: el servicio les completa la moneda (que los
 * procedimientos de verificacion no devuelven) y la marca de cheque cerrado con el mismo codigo para las dos.
 */
public interface FilaConChequeDto {

    Long getCodCheque();

    String getDatoEstadoCheque();

    void setChequeCerrado(boolean chequeCerrado);

    void setMoneda(String moneda);

    void setDescMoneda(String descMoneda);
}
