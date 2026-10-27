package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.commons.AccesoPantalla;
import bo.bosque.com.impexpap.commons.TtkBingoEnVivoService;
import bo.bosque.com.impexpap.commons.TtkPantallas;
import bo.bosque.com.impexpap.dao.ITtkBingo;
import bo.bosque.com.impexpap.dao.ITtkBingoAviso;
import bo.bosque.com.impexpap.dao.ITtkBingoCarton;
import bo.bosque.com.impexpap.dao.ITtkBingoCelda;
import bo.bosque.com.impexpap.dao.ITtkBingoGanador;
import bo.bosque.com.impexpap.dao.ITtkBingoJuego;
import bo.bosque.com.impexpap.dao.ITtkBingoSorteo;
import bo.bosque.com.impexpap.model.TtkBingo;
import bo.bosque.com.impexpap.model.TtkBingoAviso;
import bo.bosque.com.impexpap.model.TtkBingoCarton;
import bo.bosque.com.impexpap.model.TtkBingoCelda;
import bo.bosque.com.impexpap.model.TtkBingoGanador;
import bo.bosque.com.impexpap.model.TtkBingoJuego;
import bo.bosque.com.impexpap.model.TtkBingoSorteo;
import bo.bosque.com.impexpap.security.jwt.DatosToken;
import bo.bosque.com.impexpap.utils.ApiResponse;
import bo.bosque.com.impexpap.utils.RespuestaSp;
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
import java.util.List;
import java.util.function.Function;

/**
 * Bingo: configuración y cartones (Configuracion), panel en vivo (Panel), cartón propio (MiCarton) y estado
 * público (EnVivo). Cada mutación del juego refresca una vez el estado en vivo y lo publica por STOMP;
 * los estados se leen de la caché, nunca de la base por espectador. audUsuario sale del token.
 */
@RestController
@CrossOrigin(originPatterns = "${ttk.cors.origenes:*}", methods = {RequestMethod.POST})
@RequestMapping("/bingo")
@PreAuthorize("hasAnyRole('ROLE_ADM', 'ROLE_LIM')")
public class TtkBingoController {

    private final ITtkBingo bingos;
    private final ITtkBingoJuego juegos;
    private final ITtkBingoCarton cartones;
    private final ITtkBingoCelda celdas;
    private final ITtkBingoSorteo sorteos;
    private final ITtkBingoGanador ganadores;
    private final ITtkBingoAviso avisos;
    private final TtkBingoEnVivoService enVivo;
    private final AccesoPantalla pantallas;

    public TtkBingoController(ITtkBingo bingos, ITtkBingoJuego juegos, ITtkBingoCarton cartones, ITtkBingoCelda celdas,
                              ITtkBingoSorteo sorteos, ITtkBingoGanador ganadores, ITtkBingoAviso avisos,
                              TtkBingoEnVivoService enVivo, AccesoPantalla pantallas) {
        this.bingos = bingos;
        this.juegos = juegos;
        this.cartones = cartones;
        this.celdas = celdas;
        this.sorteos = sorteos;
        this.ganadores = ganadores;
        this.avisos = avisos;
        this.enVivo = enVivo;
        this.pantallas = pantallas;
    }

    // ------------------------------------------------------------------ encargado: configuración

    @PostMapping("/listar")
    public ResponseEntity<ApiResponse<?>> listar(@RequestBody(required = false) TtkBingo f, Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.encargado());
        return TtkRespuestas.listado(bingos.listar(f == null ? new TtkBingo() : f));
    }

    /**
     * Sin codBingo es un alta (con codEvento). Una edición refresca el estado en vivo: un cambio de
     * tamRanking en pleno juego se ve al instante. Un alta no lo hace, porque nadie mira un bingo nuevo.
     */
    @PostMapping("/guardar")
    public ResponseEntity<ApiResponse<?>> guardar(@Valid @RequestBody TtkBingo mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.CONFIG);
        mb.setAudUsuario(usuario(auth));
        boolean alta = TtkRespuestas.esAlta(mb.getCodBingo());
        RespuestaSp res = bingos.registrar(mb, alta ? "I" : "U");
        if (!alta) refrescar(mb.getCodBingo(), res);
        return TtkRespuestas.escritura(res);
    }

    @PostMapping("/abrir-eleccion")
    public ResponseEntity<ApiResponse<?>> abrirEleccion(@Valid @RequestBody TtkBingo mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.CONFIG);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(bingos.registrar(mb, "E"));
    }

    @PostMapping("/iniciar")
    public ResponseEntity<ApiResponse<?>> iniciar(@Valid @RequestBody TtkBingo mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.CONFIG);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = bingos.registrar(mb, "J");
        refrescar(mb.getCodBingo(), res);
        return TtkRespuestas.escritura(res);
    }

    /**
     * Nombre, figura (máscara 0/1) y premio de un juego pendiente; con el juego en curso solo cambia el premio.
     * Refresca el estado en vivo para que «Se está jugando por …» cambie al instante en la sala.
     */
    @PostMapping("/juego/guardar")
    public ResponseEntity<ApiResponse<?>> guardarJuego(@Valid @RequestBody TtkBingoJuego mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.CONFIG);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = juegos.registrar(mb, "U");
        refrescar(mb.getCodBingo() != null ? mb.getCodBingo() : bingoDeJuego(mb.getCodJuego()), res);
        return TtkRespuestas.escritura(res);
    }

    /**
     * Genera o completa cartones (el SP decide si se puede: también en elección o en juego sin juego en curso).
     * Siempre refresca el estado en vivo: es una acción rara y así Java no repite la regla de estados.
     */
    @PostMapping("/carton/generar")
    public ResponseEntity<ApiResponse<?>> generarCartones(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.CONFIG);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = cartones.registrar(mb, "G");
        refrescar(mb.getCodBingo(), res);
        return TtkRespuestas.escritura(res);
    }

    /** Cartones con su dueño: solo el encargado sabe quién tiene cuál. */
    @PostMapping("/carton/listar")
    public ResponseEntity<ApiResponse<?>> listarCartones(@RequestBody(required = false) TtkBingoCarton f,
                                                         Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.cartones());
        return TtkRespuestas.listado(cartones.listar(f == null ? new TtkBingoCarton() : f));
    }

    @PostMapping("/carton/empleados")
    public ResponseEntity<ApiResponse<?>> empleadosParaAsignar(@RequestBody(required = false) TtkBingoCarton f,
                                                               Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.cartones());
        return TtkRespuestas.listado(cartones.empleados(f == null ? new TtkBingoCarton() : f));
    }

    @PostMapping("/carton/asignar")
    public ResponseEntity<ApiResponse<?>> asignarCarton(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        return cartonDelEncargado(mb, auth, "A");
    }

    @PostMapping("/carton/liberar")
    public ResponseEntity<ApiResponse<?>> liberarCarton(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        return cartonDelEncargado(mb, auth, "L");
    }

    /** Con modalidad «ambos», si el cartón se entrega impreso o se juega en pantalla (el SP decide si se puede). */
    @PostMapping("/carton/forma")
    public ResponseEntity<ApiResponse<?>> formaCarton(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        return cartonDelEncargado(mb, auth, "F");
    }

    /** Casillas de cualquier cartón, con su dueño. */
    @PostMapping("/carton/celdas")
    public ResponseEntity<ApiResponse<?>> celdasDeCarton(@RequestBody(required = false) TtkBingoCelda f,
                                                         Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.cartones());
        if (f == null || f.getCodCarton() == null) return TtkRespuestas.sinDatos();
        return TtkRespuestas.listado(celdas.delCarton(f.getCodCarton()));
    }

    @PostMapping("/carton/impresion")
    public ResponseEntity<ApiResponse<?>> impresionCartones(@RequestBody(required = false) TtkBingoCelda f,
                                                            Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.cartones());
        return TtkRespuestas.listado(celdas.impresion(f == null ? new TtkBingoCelda() : f));
    }

    // ------------------------------------------------------------------ encargado: panel en vivo

    @PostMapping("/juego/comenzar")
    public ResponseEntity<ApiResponse<?>> comenzarJuego(@Valid @RequestBody TtkBingoJuego mb, Authentication auth) {
        return juegoDelPanel(mb, auth, "C");
    }

    @PostMapping("/juego/finalizar")
    public ResponseEntity<ApiResponse<?>> finalizarJuego(@Valid @RequestBody TtkBingoJuego mb, Authentication auth) {
        return juegoDelPanel(mb, auth, "F");
    }

    @PostMapping("/finalizar")
    public ResponseEntity<ApiResponse<?>> finalizar(@Valid @RequestBody TtkBingo mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = bingos.registrar(mb, "F");
        refrescar(mb.getCodBingo(), res);
        return TtkRespuestas.escritura(res);
    }

    /** El message es el aviso del SP: incluye «¡BINGO…» si algún cartón completó la figura. */
    @PostMapping("/sorteo/marcar")
    public ResponseEntity<ApiResponse<?>> marcarBolillo(@Valid @RequestBody TtkBingoSorteo mb, Authentication auth) {
        return bolillo(mb, auth, "I");
    }

    @PostMapping("/sorteo/quitar")
    public ResponseEntity<ApiResponse<?>> quitarBolillo(@Valid @RequestBody TtkBingoSorteo mb, Authentication auth) {
        return bolillo(mb, auth, "D");
    }

    /** Verifica el cartón que gritó BINGO: el SP siempre devuelve una fila con el mensaje para mostrar. */
    @PostMapping("/carton/validar")
    public ResponseEntity<ApiResponse<?>> validarCarton(@RequestBody(required = false) TtkBingoCarton f,
                                                        Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        return TtkRespuestas.uno(cartones.validar(f == null ? new TtkBingoCarton() : f));
    }

    @PostMapping("/ganador/confirmar")
    public ResponseEntity<ApiResponse<?>> confirmarGanador(@Valid @RequestBody TtkBingoGanador mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = ganadores.registrar(mb, "I");
        refrescar(mb.getCodBingo() != null ? mb.getCodBingo() : bingoDeCarton(mb.getCodCarton()), res);
        return TtkRespuestas.escritura(res);
    }

    /** El bingo se busca antes de anular: después la confirmación ya no existe. */
    @PostMapping("/ganador/anular")
    public ResponseEntity<ApiResponse<?>> anularGanador(@Valid @RequestBody TtkBingoGanador mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        Long codBingo = bingoDeGanador(mb.getCodGanador());
        RespuestaSp res = ganadores.registrar(mb, "D");
        refrescar(codBingo, res);
        return TtkRespuestas.escritura(res);
    }

    @PostMapping("/ganador/listar")
    public ResponseEntity<ApiResponse<?>> listarGanadores(@RequestBody(required = false) TtkBingoGanador f,
                                                          Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        return TtkRespuestas.listado(ganadores.listar(f == null ? new TtkBingoGanador() : f));
    }

    /** El bingo se busca por el aviso para refrescar el panel de todos los encargados. */
    @PostMapping("/aviso/atender")
    public ResponseEntity<ApiResponse<?>> atenderAviso(@Valid @RequestBody TtkBingoAviso mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        Long codBingo = bingoDeAviso(mb.getCodAviso());
        RespuestaSp res = avisos.registrar(mb, "A");
        refrescar(codBingo, res);
        return TtkRespuestas.escritura(res);
    }

    /** Desde la caché en memoria: con ranking y aviso. */
    @PostMapping("/estado-encargado")
    public ResponseEntity<ApiResponse<?>> estadoEncargado(@RequestBody(required = false) TtkBingo f,
                                                          Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        if (f == null || f.getCodBingo() == null) return TtkRespuestas.sinDatos();
        return TtkRespuestas.uno(enVivo.obtenerEncargado(f.getCodBingo()));
    }

    // ------------------------------------------------------------------ participante

    @PostMapping("/mis-bingos")
    public ResponseEntity<ApiResponse<?>> misBingos(Authentication auth) {
        // En vivo también lo usa para encontrar el bingo en juego (p. ej. la cuenta del proyector).
        pantallas.exigirAlguna(auth, TtkPantallas.MI_CARTON, TtkPantallas.EN_VIVO);
        return TtkRespuestas.listado(bingos.misBingos(usuario(auth)));
    }

    /** Cartones libres para elegir, sin dueños. */
    @PostMapping("/carton/disponibles")
    public ResponseEntity<ApiResponse<?>> cartonesDisponibles(@RequestBody(required = false) TtkBingoCarton f,
                                                              Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        if (f == null || f.getCodBingo() == null) return TtkRespuestas.sinDatos();
        return TtkRespuestas.listado(cartones.disponibles(f.getCodBingo()));
    }

    @PostMapping("/carton/mios")
    public ResponseEntity<ApiResponse<?>> misCartones(@RequestBody(required = false) TtkBingoCarton f,
                                                      Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        return TtkRespuestas.listado(cartones.mios(f == null ? null : f.getCodBingo(), usuario(auth)));
    }

    /** Casillas de un cartón libre o propio (el SP no devuelve los ajenos). */
    @PostMapping("/carton/ver")
    public ResponseEntity<ApiResponse<?>> verCarton(@RequestBody(required = false) TtkBingoCelda f,
                                                    Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        if (f == null || f.getCodCarton() == null) return TtkRespuestas.sinDatos();
        return TtkRespuestas.listado(celdas.verCarton(f.getCodCarton(), usuario(auth)));
    }

    /** Casillas de todos los cartones propios, para imprimirlos. */
    @PostMapping("/carton/mis-celdas")
    public ResponseEntity<ApiResponse<?>> misCeldas(@RequestBody(required = false) TtkBingoCelda f,
                                                    Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        return TtkRespuestas.listado(celdas.misCeldas(f == null ? null : f.getCodBingo(), usuario(auth)));
    }

    @PostMapping("/carton/elegir")
    public ResponseEntity<ApiResponse<?>> elegirCarton(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(cartones.registrar(mb, "E"));
    }

    @PostMapping("/carton/soltar")
    public ResponseEntity<ApiResponse<?>> soltarCarton(@Valid @RequestBody TtkBingoCarton mb, Authentication auth) {
        pantallas.exigir(auth, TtkPantallas.MI_CARTON);
        mb.setAudUsuario(usuario(auth));
        return TtkRespuestas.escritura(cartones.registrar(mb, "X"));
    }

    /**
     * «¡Bingo!» del participante: solo avisa al encargado (el SP exige cartón propio y nunca dice si ganó).
     * El panel recibe el mensaje del aviso para el encargado; el participante, el suyo.
     */
    @PostMapping("/aviso/cantar")
    public ResponseEntity<ApiResponse<?>> cantarBingo(@Valid @RequestBody TtkBingoAviso mb, Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.MI_CARTON, TtkPantallas.EN_VIVO);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = avisos.registrar(mb, "I");
        TtkBingoAviso nuevo = avisos.buscar(res.getIdGenerado());
        if (nuevo != null && nuevo.getCodBingo() != null) enVivo.refrescar(nuevo.getCodBingo(), nuevo.getMensaje());
        return TtkRespuestas.escritura(res);
    }

    // ------------------------------------------------------------------ compartidos

    /** Juegos con su figura, sin ganadores. */
    @PostMapping("/juego/listar")
    public ResponseEntity<ApiResponse<?>> listarJuegos(@RequestBody(required = false) TtkBingoJuego f,
                                                       Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.publico());
        return TtkRespuestas.listado(juegos.listar(f == null ? new TtkBingoJuego() : f));
    }

    /** Desde la caché en memoria: sin ranking, nombres ni avisos. */
    @PostMapping("/estado-publico")
    public ResponseEntity<ApiResponse<?>> estadoPublico(@RequestBody(required = false) TtkBingo f,
                                                        Authentication auth) {
        pantallas.exigirAlguna(auth, TtkPantallas.publico());
        if (f == null || f.getCodBingo() == null) return TtkRespuestas.sinDatos();
        return TtkRespuestas.uno(enVivo.obtenerPublico(f.getCodBingo()));
    }

    // ------------------------------------------------------------------ apoyo

    private ResponseEntity<ApiResponse<?>> cartonDelEncargado(TtkBingoCarton mb, Authentication auth, String accion) {
        pantallas.exigirAlguna(auth, TtkPantallas.cartones());
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = cartones.registrar(mb, accion);
        refrescar(bingoDeCarton(mb.getCodCarton()), res);
        return TtkRespuestas.escritura(res);
    }

    private ResponseEntity<ApiResponse<?>> juegoDelPanel(TtkBingoJuego mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = juegos.registrar(mb, accion);
        refrescar(bingoDeJuego(mb.getCodJuego()), res);
        return TtkRespuestas.escritura(res);
    }

    private ResponseEntity<ApiResponse<?>> bolillo(TtkBingoSorteo mb, Authentication auth, String accion) {
        pantallas.exigir(auth, TtkPantallas.PANEL);
        mb.setAudUsuario(usuario(auth));
        RespuestaSp res = sorteos.registrar(mb, accion);
        refrescar(mb.getCodBingo(), res);
        return TtkRespuestas.escritura(res);
    }

    /** Una sola recarga del estado en vivo por mutación; el aviso es el mensaje del SP. */
    private void refrescar(Long codBingo, RespuestaSp res) {
        if (codBingo != null) enVivo.refrescar(codBingo, res.getErrormsg());
    }

    private Long bingoDeCarton(Long codCarton) {
        if (codCarton == null) return null;
        TtkBingoCarton f = new TtkBingoCarton();
        f.setCodCarton(codCarton);
        return primero(cartones.listar(f), TtkBingoCarton::getCodBingo);
    }

    private Long bingoDeJuego(Long codJuego) {
        if (codJuego == null) return null;
        TtkBingoJuego f = new TtkBingoJuego();
        f.setCodJuego(codJuego);
        return primero(juegos.listar(f), TtkBingoJuego::getCodBingo);
    }

    private Long bingoDeAviso(Long codAviso) {
        if (codAviso == null) return null;
        TtkBingoAviso a = avisos.buscar(codAviso);
        return a == null ? null : a.getCodBingo();
    }

    private Long bingoDeGanador(Long codGanador) {
        if (codGanador == null) return null;
        TtkBingoGanador f = new TtkBingoGanador();
        f.setCodGanador(codGanador);
        return primero(ganadores.listar(f), TtkBingoGanador::getCodBingo);
    }

    private static <T> Long primero(List<T> filas, Function<T, Long> codBingo) {
        return filas == null || filas.isEmpty() ? null : codBingo.apply(filas.get(0));
    }

    private static long usuario(Authentication auth) {
        return DatosToken.codUsuarioDe(auth);
    }
}
