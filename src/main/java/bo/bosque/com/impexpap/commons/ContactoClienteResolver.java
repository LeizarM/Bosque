package bo.bosque.com.impexpap.commons;

/**
 * Punto enchufable para averiguar A QUÉ NÚMERO DE WHATSAPP se le avisa a un cliente
 * cuando su entrega queda marcada como entregada.
 *
 * <p><b>Por qué existe una interfaz para algo tan chico.</b> El aviso al cliente está
 * escrito y funcionando (ver {@link NotificacionEntregaService}), pero hoy la base
 * {@code BOSQUE-2_0} NO tiene el teléfono del cliente en ninguna tabla: lo único que
 * cruza con {@code trch_Entregas.cardCode} es {@code tped_clienteSAP.codClienteSAP}
 * (1170 de 1170 códigos), y esa tabla solo guarda {@code correo}. Entonces, en lugar de
 * dejar el feature a medias o de cablear una consulta que todavía no se puede escribir,
 * la resolución del contacto queda detrás de esta interfaz. La implementación por
 * defecto ({@link ContactoClienteResolverStub}) devuelve {@code null} y el servicio de
 * notificación lo trata como "no hay a quién avisar" — sin error, sin ruido.
 *
 * <p><b>Cómo se activa el día que haya teléfonos.</b> Se crea otra clase
 * {@code @Service @Primary} que implemente esta misma interfaz leyendo de donde
 * corresponda (lo natural: {@code OCRD.Phone1} / {@code OCRD.Cellular} de SAP Business
 * One), y se pone {@code openwa.entregas.notificar-cliente=true}. No hay que tocar
 * {@code NotificacionEntregaService} ni el controlador de entregas.
 *
 * @see ContactoClienteResolverStub ContactoClienteResolverStub — la implementación
 *      actual, con el detalle completo de dónde viven los teléfonos.
 */
public interface ContactoClienteResolver {

    /**
     * Devuelve el chatId WhatsApp (formato 591XXXXXXXX@c.us) o null si no se pudo resolver.
     *
     * <p>Contrato para quien implemente: devolver {@code null} es una respuesta VÁLIDA y
     * esperada (cliente sin teléfono, teléfono basura, fuente de datos caída). El método
     * NO debe lanzar excepciones — el llamador corre en un hilo asíncrono cuyo único
     * trabajo es no molestar al chofer que está registrando la entrega. Si adentro se
     * consulta una base o un servicio externo, atrapar ahí mismo, loguear y devolver
     * {@code null}.
     *
     * <p>El String devuelto se pasa TAL CUAL a {@code WhatsAppService.enviarMensaje},
     * así que tiene que venir ya normalizado al formato de openWA
     * ({@code 59178888274@c.us}). Para eso está
     * {@code WhatsAppService.normalizarChatId(String)}.
     *
     * @param cardCode código de cliente SAP (columna {@code cardCode} de {@code trch_Entregas})
     * @param db       base/empresa a la que pertenece el documento (columna {@code db})
     * @return chatId listo para enviar, o {@code null} si no hay contacto utilizable
     */
    String resolverChatId(String cardCode, String db);
}
