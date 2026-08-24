package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.PagadoItem;
import bo.bosque.com.impexpap.model.PagadoItemCorte;
import bo.bosque.com.impexpap.model.PagadoItemResumen;

import java.util.List;

/**
 * El detalle por item de lo ya pagado.
 * <p>
 * Las dos lecturas van juntas a proposito: la lista de items no se puede
 * mostrar sin el corte. Un periodo puede tener CERO items y estar bien, y el
 * corte es lo unico que lo explica; sin el, la pantalla mostraria un cero pelado
 * que no se distingue de un historico roto.
 * <p>
 * <b>origen no es opcional en la practica.</b> IMPEXPAP / PAPIRUS / PRODUCTIVA
 * PAPEL y ESPPAPEL se congelan las dos con {@code esInterno = 1}: quien no lo
 * mande recibe las dos empresas juntas.
 */
public interface IPagadoItem {

    /**
     * Los items de un periodo, ordenados por (origen, docNum, itemCode).
     * <p>
     * El orden lo pone el SP y empieza por origen a proposito: docNum se repite
     * entre empresas, asi que quien agrupe por nota tiene que agrupar por el par
     * (origen, docNum) y no por docNum solo.
     *
     * @param idPagado      una nota puntual; null trae el periodo entero
     * @param docNum        una nota puntual por numero; null trae todas
     * @param origen        la empresa; null trae todas las del mismo esInterno
     * @param soloExcluidos solo lo que NO descontó, que es lo que casi nunca se
     *                      puede ver en otro lado
     */
    List<PagadoItem> obtenerItems(int mes, int anio, int esInterno,
                                  Long idPagado, Long docNum, String origen,
                                  boolean soloExcluidos);

    /**
     * Cuantos items y cuanta plata hay detras de cada motivo.
     * <p>
     * Los tres filtros puntuales pesan tanto aca como en el listado: un resumen
     * sin origen suma las dos empresas y no cuadra con ningun reporte.
     */
    List<PagadoItemResumen> obtenerResumen(int mes, int anio, int esInterno,
                                           Long idPagado, Long docNum, String origen);

    /**
     * El corte de los periodos ya congelados. Cualquiera de los tres filtros en
     * null es "sin filtro": sin ninguno devuelve el historico completo.
     */
    List<PagadoItemCorte> obtenerCorte(Integer mes, Integer anio, Integer esInterno);
}
