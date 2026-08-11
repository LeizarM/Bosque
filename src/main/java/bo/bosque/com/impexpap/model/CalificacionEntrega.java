package bo.bosque.com.impexpap.model;

import java.io.Serializable;
import java.util.Date;

/**
 * Una calificacion de entrega: la fila de trch_EntregaCalificacion que representa la encuesta
 * de UNA entrega puntual.
 *
 * <p>Ciclo de vida de la fila (dos momentos, dos ACCIONes del SP p_abm_trch_EntregaCalificacion):</p>
 * <ol>
 *   <li><b>ACCION 'I'</b> - cuando se le manda al cliente el aviso de entrega con el pedido de
 *       calificacion. La fila nace con los datos del documento y del chofer, con {@code chatId}
 *       cargado y con {@code puntaje}, {@code comentario} y {@code fechaRespuesta} en NULL.
 *       Eso es lo que la vuelve "pendiente".</li>
 *   <li><b>ACCION 'U'</b> - cuando el cliente responde por WhatsApp. Se completan {@code puntaje},
 *       {@code comentario}, {@code mensajeCrudo} y {@code fechaRespuesta} sobre la fila pendiente
 *       que se encontro con la ACCION 'P' del SP de lectura.</li>
 * </ol>
 *
 * <p><b>puntaje NULL no es lo mismo que puntaje 0.</b> NULL significa "todavia no contesto" y es el
 * estado con el que la fila espera la respuesta; un 0 significaria una calificacion de cero, que no
 * existe (la escala es 1..5). Por eso todos los numericos de esta clase son tipos envueltos
 * ({@link Integer}, {@link Long}) y nunca primitivos: un {@code int} no puede representar "sin
 * responder" y el RowMapper del DAO lee estas columnas con {@code wasNull()} justamente para no
 * confundir los dos casos.</p>
 *
 * <p><b>OJO - inconsistencia deliberada, no la "arregles":</b> el resto del paquete {@code model}
 * usa Lombok (ver {@link EntregaChofer}). Esta clase NO: es un POJO plano con getters y setters
 * escritos a mano, para seguir la misma convencion que ya fijo
 * {@code dto.EntregaNotificacionDTO} en el feature de notificaciones de entrega, del que esta
 * clase es la continuacion. Los accesores estan nombrados EXACTAMENTE como los generaria Lombok
 * (en particular {@code getUChofer()} / {@code setUChofer()} para el campo {@code uChofer}), de
 * modo que el JSON que produce Jackson no cambia respecto del resto del modulo de entregas.</p>
 *
 * <p>Las tres fechas son {@link java.util.Date} y no String, a diferencia de
 * {@code EntregaNotificacionDTO.fechaEntrega}. Aca si conviene el Date porque estas fechas viajan
 * al frontend dentro del reporte de calificaciones y no al texto de un WhatsApp:
 * {@code config.JacksonConfig} las serializa como {@code yyyy-MM-dd HH:mm:ss} en zona
 * America/La_Paz, que es lo que el resto de los listados del proyecto ya devuelve.</p>
 *
 * <p><b>Contrato de columnas:</b> el RowMapper de {@code dao.CalificacionEntregaDao} lee las
 * columnas POR INDICE, en el mismo orden en que estan declarados los campos de esta clase. Si se
 * agrega, quita o reordena un campo aca hay que tocar tambien el SP de lectura y ese RowMapper.</p>
 */
public class CalificacionEntrega implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Largo al que {@link #toString()} recorta los textos libres para no inundar el log. */
    private static final int LARGO_TEXTO_EN_TOSTRING = 60;

    /** PK identity de trch_EntregaCalificacion. Va en NULL al insertar (ACCION 'I'). */
    private Long idCalificacion;

    /** docEntry del documento en SAP. Junto con {@link #db} identifica la entrega calificada. */
    private Long docEntry;

    /** Base de datos / empresa de origen: IPX, ESP, PAP, PRODPAP. */
    private String db;

    /** Codigo del cliente en SAP (cardCode). */
    private String cardCode;

    /** Razon social / nombre del cliente, congelado al momento del envio. */
    private String cardName;

    /**
     * Codigo del empleado chofer (u_Chofer = tb_empleado.codEmpleado). Se guarda en la fila,
     * y no se resuelve despues por join, porque la calificacion es del chofer que hizo ESA
     * entrega: si mas adelante la entrega se reasigna, el puntaje tiene que seguir perteneciendo
     * a quien la hizo.
     */
    private Integer uChofer;

    /** Nombre completo del chofer, congelado al momento del envio por el mismo motivo que {@link #uChofer}. */
    private String nombreChofer;

    /**
     * Puntaje que dio el cliente, de 1 a 5.
     * <b>NULL mientras la encuesta esta pendiente de respuesta.</b>
     */
    private Integer puntaje;

    /**
     * Texto libre que acompano al puntaje, ya saneado.
     * <b>Dato de origen no confiable: lo escribe el cliente.</b> Nunca debe volver a salir en un
     * mensaje de WhatsApp sin pasar por
     * {@code commons.NotificacionEntregaService.sanearParaMensaje(String, int)}, ni concatenarse
     * en SQL (el DAO lo manda siempre como parametro).
     */
    private String comentario;

    /**
     * Chat de WhatsApp del cliente, en el formato que devuelve
     * {@code commons.WhatsAppService.normalizarChatId(String)}: {@code 591XXXXXXXX@c.us}.
     * Es la clave con la que el webhook encuentra la encuesta pendiente cuando entra un mensaje.
     */
    private String chatId;

    /**
     * Texto crudo del mensaje entrante, tal cual lo mando el cliente, guardado para poder auditar
     * despues por que se interpreto un puntaje y no otro.
     * <b>Dato de origen no confiable</b>, con las mismas precauciones que {@link #comentario}.
     */
    private String mensajeCrudo;

    /** Fecha y hora en que se entrego el pedido (y en que se mando la encuesta). */
    private Date fechaEntrega;

    /** Fecha y hora en que el cliente respondio. NULL mientras la encuesta esta pendiente. */
    private Date fechaRespuesta;

    /** Fecha de auditoria de la fila; la pone la base de datos, el DAO no la manda. */
    private Date audFecha;

    public CalificacionEntrega() {
    }

    public Long getIdCalificacion() {
        return idCalificacion;
    }

    public void setIdCalificacion(Long idCalificacion) {
        this.idCalificacion = idCalificacion;
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

    public Integer getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(Integer puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMensajeCrudo() {
        return mensajeCrudo;
    }

    public void setMensajeCrudo(String mensajeCrudo) {
        this.mensajeCrudo = mensajeCrudo;
    }

    public Date getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(Date fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public Date getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(Date fechaRespuesta) {
        this.fechaRespuesta = fechaRespuesta;
    }

    public Date getAudFecha() {
        return audFecha;
    }

    public void setAudFecha(Date audFecha) {
        this.audFecha = audFecha;
    }

    /**
     * Representacion para el log.
     *
     * <p>{@code comentario} y {@code mensajeCrudo} salen RECORTADOS a
     * {@value #LARGO_TEXTO_EN_TOSTRING} caracteres a proposito: son texto libre escrito por el
     * cliente, sin tope de largo real del lado de WhatsApp, y un toString() completo dentro de un
     * logger.error convertiria un mensaje de 20.000 caracteres en 20.000 caracteres de log. Si se
     * necesita el texto entero, hay que leerlo de la base, no del log.</p>
     */
    @Override
    public String toString() {
        return "CalificacionEntrega{" +
                "idCalificacion=" + idCalificacion +
                ", docEntry=" + docEntry +
                ", db='" + db + '\'' +
                ", cardCode='" + cardCode + '\'' +
                ", cardName='" + cardName + '\'' +
                ", uChofer=" + uChofer +
                ", nombreChofer='" + nombreChofer + '\'' +
                ", puntaje=" + puntaje +
                ", comentario='" + recortar(comentario, LARGO_TEXTO_EN_TOSTRING) + '\'' +
                ", chatId='" + chatId + '\'' +
                ", mensajeCrudo='" + recortar(mensajeCrudo, LARGO_TEXTO_EN_TOSTRING) + '\'' +
                ", fechaEntrega=" + fechaEntrega +
                ", fechaRespuesta=" + fechaRespuesta +
                ", audFecha=" + audFecha +
                '}';
    }

    /**
     * Recorta un texto para el log. Devuelve null tal cual si el valor es null, para que el
     * toString() distinga "sin comentario" de "comentario vacio".
     */
    private static String recortar(String valor, int largoMaximo) {
        if (valor == null) {
            return null;
        }
        if (valor.length() <= largoMaximo) {
            return valor;
        }
        return valor.substring(0, largoMaximo) + "...(" + valor.length() + " chars)";
    }
}
