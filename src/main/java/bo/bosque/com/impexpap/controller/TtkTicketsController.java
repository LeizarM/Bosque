package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.commons.TtkPantallas;
import bo.bosque.com.impexpap.dao.ITtkEvento;
import bo.bosque.com.impexpap.dao.ITtkParticipante;
import bo.bosque.com.impexpap.dao.ITtkParticipanteTicket;
import bo.bosque.com.impexpap.dao.ITtkTipoTicket;
import bo.bosque.com.impexpap.dto.TtkValeLote;
import bo.bosque.com.impexpap.model.TtkEvento;
import bo.bosque.com.impexpap.model.TtkParticipante;
import bo.bosque.com.impexpap.model.TtkParticipanteTicket;
import bo.bosque.com.impexpap.model.TtkTipoTicket;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Tickets de eventos (RRHH): eventos, tipos de vale, participantes y vales. Todo exige la pantalla
 * {@value TtkPantallas#EVENTOS}; audUsuario sale del token. Reglas y mensajes los da cada SP.
 */
@RestController
@CrossOrigin(originPatterns = "${ttk.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/tickets")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TtkTicketsController {

    private final ITtkEvento eventos;
    private final ITtkTipoTicket tipos;
    private final ITtkParticipante participantes;
    private final ITtkParticipanteTicket vales;
    private final AccesoPantalla pantallas;

    public TtkTicketsController(ITtkEvento eventos, ITtkTipoTicket tipos, ITtkParticipante participantes,
                                ITtkParticipanteTicket vales, AccesoPantalla pantallas) {
        this.eventos = eventos;
        this.tipos = tipos;
        this.participantes = participantes;
        this.vales = vales;
        this.pantallas = pantallas;
    }

    // ------------------------------------------------------------------ eventos

    @PostMapping("/evento/listar")
    public ResponseEntity<ApiResponse<?>> listarEventos(@RequestBody(required = false) TtkEvento f, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.listado(eventos.listar(f == null ? new TtkEvento() : f));
    }

    /** Sin codEvento es un alta. */
    @PostMapping("/evento/guardar")
    public ResponseEntity<ApiResponse<?>> guardarEvento(@Valid @RequestBody TtkEvento mb, Authentication auth) {
        return evento(mb, auth, TtkRespuestas.esAlta(mb.getCodEvento()) ? "I" : "U");
    }

    @PostMapping("/evento/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarEvento(@Valid @RequestBody TtkEvento mb, Authentication auth) {
        return evento(mb, auth, "D");
    }

    @PostMapping("/evento/emitir")
    public ResponseEntity<ApiResponse<?>> emitirEvento(@Valid @RequestBody TtkEvento mb, Authentication auth) {
        return evento(mb, auth, "E");
    }

    @PostMapping("/evento/volver-borrador")
    public ResponseEntity<ApiResponse<?>> volverBorrador(@Valid @RequestBody TtkEvento mb, Authentication auth) {
        return evento(mb, auth, "B");
    }

    @PostMapping("/evento/cerrar")
    public ResponseEntity<ApiResponse<?>> cerrarEvento(@Valid @RequestBody TtkEvento mb, Authentication auth) {
        return evento(mb, auth, "C");
    }

    // ------------------------------------------------------------------ tipos de ticket

    @PostMapping("/tipo/listar")
    public ResponseEntity<ApiResponse<?>> listarTipos(@RequestBody(required = false) TtkTipoTicket f,
                                                      Authentication auth) {
        // Configuración del bingo también lo usa para elegir el vale que da cartón.
        pantallas.exigirAlguna(auth, TtkPantallas.EVENTOS, TtkPantallas.CONFIG);
        return TtkRespuestas.listado(tipos.listar(f == null ? new TtkTipoTicket() : f));
    }

    /** Sin codTipoTicket es un alta (con codEvento). */
    @PostMapping("/tipo/guardar")
    public ResponseEntity<ApiResponse<?>> guardarTipo(@Valid @RequestBody TtkTipoTicket mb, Authentication auth) {
        return tipo(mb, auth, TtkRespuestas.esAlta(mb.getCodTipoTicket()) ? "I" : "U");
    }

    @PostMapping("/tipo/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarTipo(@Valid @RequestBody TtkTipoTicket mb, Authentication auth) {
        return tipo(mb, auth, "D");
    }

    // ------------------------------------------------------------------ participantes

    @PostMapping("/participante/listar")
    public ResponseEntity<ApiResponse<?>> listarParticipantes(@RequestBody(required = false) TtkParticipante f,
                                                              Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.listado(participantes.listar(f == null ? new TtkParticipante() : f));
    }

    /** Empleados activos que aún no están en el evento, para agregarlos uno a uno. */
    @PostMapping("/participante/empleados")
    public ResponseEntity<ApiResponse<?>> empleadosSinAgregar(@RequestBody(required = false) TtkParticipante f,
                                                              Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.listado(participantes.empleados(f == null ? new TtkParticipante() : f));
    }

    /** Agrega de una vez todos los empleados activos que faltan. */
    @PostMapping("/participante/generar")
    public ResponseEntity<ApiResponse<?>> generarParticipantes(@Valid @RequestBody TtkParticipante mb,
                                                               Authentication auth) {
        return participante(mb, auth, "G");
    }

    /** Sin codParticipante es un alta (empleado o invitado). */
    @PostMapping("/participante/guardar")
    public ResponseEntity<ApiResponse<?>> guardarParticipante(@Valid @RequestBody TtkParticipante mb,
                                                              Authentication auth) {
        return participante(mb, auth, TtkRespuestas.esAlta(mb.getCodParticipante()) ? "I" : "U");
    }

    @PostMapping("/participante/eliminar")
    public ResponseEntity<ApiResponse<?>> eliminarParticipante(@Valid @RequestBody TtkParticipante mb,
                                                               Authentication auth) {
        return participante(mb, auth, "D");
    }

    /** Quita el ajuste manual de categoría: vuelve a la regla de recientes. */
    @PostMapping("/participante/restablecer")
    public ResponseEntity<ApiResponse<?>> restablecerParticipante(@Valid @RequestBody TtkParticipante mb,
                                                                  Authentication auth) {
        return participante(mb, auth, "A");
    }

    // ------------------------------------------------------------------ vales

    /** Vales de un participante ({codParticipante}) o de todo el evento ({codEvento}). */
    @PostMapping("/vale/listar")
    public ResponseEntity<ApiResponse<?>> listarVales(@RequestBody(required = false) TtkParticipanteTicket f,
                                                      Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.listado(vales.listar(f == null ? new TtkParticipanteTicket() : f));
    }

    @PostMapping("/vale/cambiar")
    public ResponseEntity<ApiResponse<?>> cambiarVale(@Valid @RequestBody TtkParticipanteTicket mb, Authentication auth) {
        return vale(mb, auth, "U");
    }

    @PostMapping("/vale/restablecer")
    public ResponseEntity<ApiResponse<?>> restablecerVale(@Valid @RequestBody TtkParticipanteTicket mb,
                                                          Authentication auth) {
        return vale(mb, auth, "R");
    }

    /** Un tipo de vale para varios participantes a la vez: los elegidos, una categoría o todos. */
    @PostMapping("/vale/cambiar-lote")
    public ResponseEntity<ApiResponse<?>> cambiarValesEnLote(@Valid @RequestBody TtkValeLote lote, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.escritura(vales.cambiarLote(lote, usuario(auth)));
    }

    /** Un renglón por vale numerado, para imprimir. */
    @PostMapping("/vale/impresion")
    public ResponseEntity<ApiResponse<?>> impresionVales(@RequestBody(required = false) TtkParticipanteTicket f,
                                                         Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        return TtkRespuestas.listado(vales.impresion(f == null ? new TtkParticipanteTicket() : f));
    }

    // ------------------------------------------------------------------ apoyo

    private ResponseEntity<ApiResponse<?>> evento(TtkEvento mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(eventos.registrar(mb, accion));
    }

    private ResponseEntity<ApiResponse<?>> tipo(TtkTipoTicket mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(tipos.registrar(mb, accion));
    }

    private ResponseEntity<ApiResponse<?>> participante(TtkParticipante mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(participantes.registrar(mb, accion));
    }

    private ResponseEntity<ApiResponse<?>> vale(TtkParticipanteTicket mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.EVENTOS);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(vales.registrar(mb, accion));
    }

    private static long usuario(Authentication auth) {
        return DatosToken.codUsuarioDe(auth);
    }
}
