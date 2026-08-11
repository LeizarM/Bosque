package bo.bosque.com.impexpap.dto;

/**
 * Datos de UNA entrega, listos para armar el texto del aviso por WhatsApp.
 *
 * <p>"Una entrega" es un par (docEntry, db). En la tabla trch_Entregas ese documento tiene N filas,
 * una por itemCode, y el SP p_abm_trch_Entregas ACCION 'B' marca fueEntregado = 1 en TODAS ellas.
 * Por eso este DTO representa el documento entero y no una linea: los campos escalares vienen de la
 * cabecera y {@link #getCantidadItems()} trae el COUNT de filas de ese documento.</p>
 *
 * <p>Se llena desde EntregaChoferDao.obtenerDatosNotificacion (ACCION 'G') y
 * EntregaChoferDao.obtenerDatosNotificacion (ACCION 'G'). Esa accion devuelve las
 * 13 columnas EN EL MISMO ORDEN, y el RowMapper del DAO las lee POR INDICE. Si se agrega, quita o
 * reordena un campo aca, hay que tocar tambien el SP (src/main/resources/sql/27_notificacion_entregas.sql)
 * y el RowMapper del DAO.</p>
 *
 * <p><b>OJO — inconsistencia deliberada, no la "arregles":</b> los otros DTOs de este paquete usan
 * Lombok (@Getter/@Setter/@Data). Esta clase NO: es un POJO plano con getters y setters escritos a
 * mano, porque asi lo fija el contrato acordado con el resto del feature de notificaciones. Los
 * accesores estan nombrados EXACTAMENTE como los generaria Lombok (en particular
 * {@code getUChofer()} / {@code setUChofer()} para el campo {@code uChofer}), de modo que el JSON
 * que produciria Jackson no cambia respecto del resto del modulo de entregas.</p>
 *
 * <p>{@code fechaEntrega} es un String ya formateado por el SP como {@code dd/MM/yyyy HH:mm}
 * (zona del servidor, America/La_Paz). Es String y no Date a proposito: el mensaje de WhatsApp lo
 * usa tal cual y asi se esquiva la serializacion de fechas de JacksonConfig.</p>
 */
public class EntregaNotificacionDTO {

    /** docEntry del documento en SAP. Junto con {@link #db} identifica la entrega. */
    private Long docEntry;

    /** Base de datos / empresa de origen: IPX, ESP, PRODPAP. */
    private String db;

    /** Numero de documento (docNum). */
    private Integer docNum;

    /** Numero de factura; 0 cuando el documento no facturo. */
    private Integer factura;

    /** Codigo del cliente en SAP (cardCode). */
    private String cardCode;

    /** Razon social / nombre del cliente. */
    private String cardName;

    /** Nombre del vendedor asignado al documento. */
    private String vendedor;

    /** Codigo del empleado chofer (u_Chofer = tb_empleado.codEmpleado). */
    private Integer uChofer;

    /** Nombre completo del chofer, resuelto por el SP contra trh_persona. */
    private String nombreChofer;

    /** Direccion donde se entrego. */
    private String direccionEntrega;

    /** Fecha y hora de la entrega, ya formateada por el SP como dd/MM/yyyy HH:mm. */
    private String fechaEntrega;

    /** Observacion cargada por el chofer. */
    private String obs;

    /** Cantidad de filas (items) que tiene el documento en trch_Entregas. */
    private Integer cantidadItems;

    /**
     * 'Factura' | 'Orden Venta' | 'Traspaso'. Decide si al documento le corresponde un aviso:
     * un Traspaso es un movimiento entre almacenes, no tiene cliente ni numero, y su cardCode
     * viene SIEMPRE vacio (2.344 de 2.344 filas en datos reales).
     */
    private String tipo;

    public EntregaNotificacionDTO() {
    }

    public Long getDocEntry() {
        return docEntry;
    }

    public void setDocEntry(Long docEntry) {
        this.docEntry = docEntry;
    }

    public String getDb() {
        return db;
    }

    public void setDb(String db) {
        this.db = db;
    }

    public Integer getDocNum() {
        return docNum;
    }

    public void setDocNum(Integer docNum) {
        this.docNum = docNum;
    }

    public Integer getFactura() {
        return factura;
    }

    public void setFactura(Integer factura) {
        this.factura = factura;
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getVendedor() {
        return vendedor;
    }

    public void setVendedor(String vendedor) {
        this.vendedor = vendedor;
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

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(String fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public String getObs() {
        return obs;
    }

    public void setObs(String obs) {
        this.obs = obs;
    }

    public Integer getCantidadItems() {
        return cantidadItems;
    }

    public void setCantidadItems(Integer cantidadItems) {
        this.cantidadItems = cantidadItems;
    }

    @Override
    public String toString() {
        return "EntregaNotificacionDTO{" +
                "docEntry=" + docEntry +
                ", db='" + db + '\'' +
                ", docNum=" + docNum +
                ", factura=" + factura +
                ", cardCode='" + cardCode + '\'' +
                ", cardName='" + cardName + '\'' +
                ", vendedor='" + vendedor + '\'' +
                ", uChofer=" + uChofer +
                ", nombreChofer='" + nombreChofer + '\'' +
                ", direccionEntrega='" + direccionEntrega + '\'' +
                ", fechaEntrega='" + fechaEntrega + '\'' +
                ", obs='" + obs + '\'' +
                ", cantidadItems=" + cantidadItems +
                '}';
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
}
