package bo.bosque.com.impexpap.dto;

/**
 * Una fila del reporte agregado de calificaciones: como le fue a UN chofer en un rango de fechas.
 *
 * <p>La produce {@code dao.CalificacionEntregaDao.resumenPorChofer(Date, Date)} con la ACCION 'R'
 * del SP {@code p_list_trch_EntregaCalificacion}, que agrupa trch_EntregaCalificacion por
 * {@code uChofer}.</p>
 *
 * <p><b>Las dos cuentas no miden lo mismo y esa es la gracia del reporte:</b>
 * {@link #getTotalEnviadas()} son todas las encuestas mandadas (filas creadas con la ACCION 'I'),
 * respondidas o no; {@link #getTotalRespondidas()} son solo las que volvieron con puntaje. La
 * diferencia entre las dos es la tasa de no-respuesta, que en una encuesta por WhatsApp suele ser
 * la mayoria. {@link #getPromedio()} se calcula UNICAMENTE sobre las respondidas: promediar sobre
 * las enviadas trataria cada silencio como un cero y hundiria a cualquier chofer.</p>
 *
 * <p>{@link #getPromedio()} es {@link Double} y viene en NULL cuando
 * {@code totalRespondidas == 0}: un chofer al que nadie le contesto no tiene promedio, y un 0.0
 * ahi seria una nota, no un "sin datos". Quien pinte la grilla tiene que contemplar el null.</p>
 *
 * <p>{@link #getC1()} .. {@link #getC5()} son el histograma: cuantas respuestas de 1, de 2, ... de
 * 5 estrellas. Deben sumar exactamente {@code totalRespondidas}. Sirven para ver la forma de la
 * distribucion, que el promedio solo esconde: cuatro cincos y dos unos promedian 3,7 igual que seis
 * cuatros, y no son para nada el mismo chofer.</p>
 *
 * <p><b>OJO - inconsistencia deliberada, no la "arregles":</b> los otros DTOs de este paquete usan
 * Lombok. Este NO, por la misma razon que {@link EntregaNotificacionDTO}: es un POJO plano con
 * getters y setters a mano, nombrados exactamente como los generaria Lombok
 * ({@code getUChofer()} / {@code setUChofer()} para el campo {@code uChofer}), para que el JSON
 * que devuelve el endpoint no cambie respecto del resto del modulo de entregas.</p>
 *
 * <p><b>Contrato de columnas:</b> el RowMapper de {@code dao.CalificacionEntregaDao} lee las 10
 * columnas POR INDICE, en el mismo orden en que estan declarados los campos de esta clase. Si se
 * agrega, quita o reordena un campo aca hay que tocar tambien la ACCION 'R' del SP y ese
 * RowMapper.</p>
 */
public class ResumenCalificacionDTO {

    /** Codigo del empleado chofer (u_Chofer = tb_empleado.codEmpleado). Es la clave del agrupamiento. */
    private Integer uChofer;

    /** Nombre completo del chofer. */
    private String nombreChofer;

    /** Encuestas mandadas en el rango, respondidas o no. */
    private Integer totalEnviadas;

    /** Encuestas que el cliente efectivamente respondio con un puntaje. */
    private Integer totalRespondidas;

    /**
     * Promedio de los puntajes, calculado solo sobre las respondidas.
     * NULL cuando {@link #totalRespondidas} es 0.
     */
    private Double promedio;

    /** Cantidad de respuestas con puntaje 1. */
    private Integer c1;

    /** Cantidad de respuestas con puntaje 2. */
    private Integer c2;

    /** Cantidad de respuestas con puntaje 3. */
    private Integer c3;

    /** Cantidad de respuestas con puntaje 4. */
    private Integer c4;

    /** Cantidad de respuestas con puntaje 5. */
    private Integer c5;

    public ResumenCalificacionDTO() {
    }

    public Integer getUChofer() {
        return uChofer;
    }

    public void setUChofer(Integer uChofer) {
        this.uChofer = uChofer;
    }

    public String getNombreChofer() {
        return nombreChofer;
    }

    public void setNombreChofer(String nombreChofer) {
        this.nombreChofer = nombreChofer;
    }

    public Integer getTotalEnviadas() {
        return totalEnviadas;
    }

    public void setTotalEnviadas(Integer totalEnviadas) {
        this.totalEnviadas = totalEnviadas;
    }

    public Integer getTotalRespondidas() {
        return totalRespondidas;
    }

    public void setTotalRespondidas(Integer totalRespondidas) {
        this.totalRespondidas = totalRespondidas;
    }

    public Double getPromedio() {
        return promedio;
    }

    public void setPromedio(Double promedio) {
        this.promedio = promedio;
    }

    public Integer getC1() {
        return c1;
    }

    public void setC1(Integer c1) {
        this.c1 = c1;
    }

    public Integer getC2() {
        return c2;
    }

    public void setC2(Integer c2) {
        this.c2 = c2;
    }

    public Integer getC3() {
        return c3;
    }

    public void setC3(Integer c3) {
        this.c3 = c3;
    }

    public Integer getC4() {
        return c4;
    }

    public void setC4(Integer c4) {
        this.c4 = c4;
    }

    public Integer getC5() {
        return c5;
    }

    public void setC5(Integer c5) {
        this.c5 = c5;
    }

    @Override
    public String toString() {
        return "ResumenCalificacionDTO{" +
                "uChofer=" + uChofer +
                ", nombreChofer='" + nombreChofer + '\'' +
                ", totalEnviadas=" + totalEnviadas +
                ", totalRespondidas=" + totalRespondidas +
                ", promedio=" + promedio +
                ", c1=" + c1 +
                ", c2=" + c2 +
                ", c3=" + c3 +
                ", c4=" + c4 +
                ", c5=" + c5 +
                '}';
    }
}
