package bo.bosque.com.impexpap.model;

import lombok.*;

import java.io.Serializable;

/**
 * Un item del catalogo SAP (text_ItemSAP), que es de donde salen los articulos
 * de una solicitud de corte con todas sus medidas.
 *
 * La tabla la refresca un proceso del lado del servidor; aqui solo se lee.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ItemSap implements Serializable {

    private String codItem;
    private String datoItem;
    private double cantidadDisponible;
    private int codTipo;
    private String datoTipo;
    private int codFabricante;
    private String datoFabricante;
    private double gramaje;
    private double largo;
    private double ancho;
    private double utm;
    private double cantHojas;
    private String empaque;
    private String formato;

}
