package bo.bosque.com.impexpap.commons;

import bo.bosque.com.impexpap.dao.ITcpeGenerarExamen;
import bo.bosque.com.impexpap.dao.ITcpeLote;
import bo.bosque.com.impexpap.dao.ITcpeResultado;
import bo.bosque.com.impexpap.dto.TcpeCredencialDto;
import bo.bosque.com.impexpap.dto.TcpeLoteAgregarDto;
import bo.bosque.com.impexpap.dto.TcpeLoteDto;
import bo.bosque.com.impexpap.dto.TcpeLoteFiltroDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGeneradoDto;
import bo.bosque.com.impexpap.dto.TcpeLoteGuardarDto;
import bo.bosque.com.impexpap.dto.TcpePersonaExamenDto;
import bo.bosque.com.impexpap.dto.TcpeResultadoExamenDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generación de lotes y exámenes (contratos 3.3, 7.2 y 10), todo o nada en una transacción. Las reglas (lote mixto,
 * máximos, preguntas disponibles) las decide {@code p_abm_tcpe_Lote 'V'}; aquí solo se orquesta y se hashean las
 * claves de los externos en línea, fuera de la transacción (BCrypt 14 tarda ~1 s cada una).
 */
@Service
public class TcpeLoteService {

    static final String ESCRITO = "E";

    private final ITcpeLote lotes;
    private final ITcpeGenerarExamen generador;
    private final ITcpeResultado resultados;
    private final TcpeClaveService claves;

    public TcpeLoteService(ITcpeLote lotes, ITcpeGenerarExamen generador, ITcpeResultado resultados,
                           TcpeClaveService claves) {
        this.lotes = lotes;
        this.generador = generador;
        this.resultados = resultados;
        this.claves = claves;
    }

    /** Generar: {@code V} con los datos del lote nuevo y los tres conteos (si falla, el SP da el mensaje). */
    public Pedido pedido(TcpeLoteGuardarDto lote) {
        Conteo c = new Conteo(lote.getCantidad(), lote.getPersonas(), lote.getCodEmpleados());
        lotes.validar(lote, c.cantidad, c.personas.size(), c.empleados.size());
        return armar(lote.getModalidad(), c);
    }

    /** Agregar: {@code V} con el lote (sus datos y lo que ya tiene los pone el SP); después se lee su modalidad. */
    public Pedido pedido(TcpeLoteAgregarDto dto) {
        Conteo c = new Conteo(dto.getCantidad(), dto.getPersonas(), dto.getCodEmpleados());
        lotes.validarAgregar(dto.getCodRolExamen(), c.cantidad, c.personas.size(), c.empleados.size());
        TcpeLoteDto l = lote(dto.getCodRolExamen());
        return armar(l == null ? null : l.getModalidad(), c);
    }

    /** El lote existente, o null si no existe. */
    public TcpeLoteDto lote(long codRolExamen) {
        TcpeLoteFiltroDto f = new TcpeLoteFiltroDto();
        f.setCodRolExamen(codRolExamen);
        List<TcpeLoteDto> filas = lotes.listar(f);
        return filas == null || filas.isEmpty() ? null : filas.get(0);
    }

    /** Crea el lote y sus exámenes. */
    @Transactional
    public TcpeLoteGeneradoDto generar(TcpeLoteGuardarDto lote, Pedido pedido, long audUsuario) {
        long codRolExamen = lotes.crear(lote, audUsuario).getIdGenerado();
        if (codRolExamen <= 0) throw new IllegalStateException("p_abm_tcpe_Lote 'I' no devolvió el codRolExamen.");
        return generarExamenes(codRolExamen, pedido, audUsuario);
    }

    /** Agrega exámenes a un lote existente. Devuelve las credenciales solo de los nuevos. */
    @Transactional
    public TcpeLoteGeneradoDto agregar(long codRolExamen, Pedido pedido, long audUsuario) {
        return generarExamenes(codRolExamen, pedido, audUsuario);
    }

    /**
     * Lote mixto: arma todos los modos que llegan (V ya validó el total). En línea, cada externo recibe una clave al
     * azar y todas se hashean en paralelo en un solo lote; los empleados y los escritos no llevan clave.
     */
    private Pedido armar(String modalidad, Conteo c) {
        boolean online = !ESCRITO.equalsIgnoreCase(modalidad == null ? "" : modalidad.trim());
        int externos = online ? c.cantidad + c.personas.size() : 0;
        List<TcpeClaveService.Clave> generadas = externos > 0 ? claves.generarConHash(externos)
                : Collections.<TcpeClaveService.Clave>emptyList();
        List<PersonaConClave> personas = new ArrayList<>(c.personas.size());
        for (int i = 0; i < c.personas.size(); i++) {
            personas.add(new PersonaConClave(c.personas.get(i), online ? generadas.get(c.cantidad + i) : null));
        }
        List<TcpeClaveService.Clave> anonimas = online ? generadas.subList(0, c.cantidad)
                : Collections.<TcpeClaveService.Clave>emptyList();
        return new Pedido(c.empleados, anonimas, personas, online ? 0 : c.cantidad, online);
    }

    private TcpeLoteGeneradoDto generarExamenes(long codRolExamen, Pedido pedido, long audUsuario) {
        // Las credenciales son solo de los externos: el empleado entra con su usuario de Bosque.
        List<Long> externos = new ArrayList<>();
        Map<Long, String> clavePorExamen = new HashMap<>();
        Map<Long, PersonaConClave> personaPorExamen = new HashMap<>();
        for (Long codEmpleado : pedido.empleados) {
            generador.generar(codRolExamen, codEmpleado, null, audUsuario);
        }
        for (TcpeClaveService.Clave c : pedido.claves) {
            long codExamen = generador.generar(codRolExamen, null, c.getHash(), audUsuario);
            externos.add(codExamen);
            clavePorExamen.put(codExamen, c.getPlano());
        }
        for (PersonaConClave pc : pedido.personas) {
            String hash = pc.clave == null ? null : pc.clave.getHash();
            long codExamen = generador.generarParaPersona(codRolExamen, pc.persona, hash, audUsuario);
            externos.add(codExamen);
            if (pc.clave != null) clavePorExamen.put(codExamen, pc.clave.getPlano());
            personaPorExamen.put(codExamen, pc);
        }
        for (int i = 0; i < pedido.sinUsuario; i++) {
            generador.generar(codRolExamen, null, null, audUsuario);
        }

        if (!pedido.online || externos.isEmpty()) {
            return new TcpeLoteGeneradoDto(codRolExamen, Collections.<TcpeCredencialDto>emptyList());
        }

        // El login sale de p_list_tcpe_Resultado 'L', cruzado por codExamen (una persona reutilizada conserva el suyo).
        Map<Long, TcpeResultadoExamenDto> porExamen = new LinkedHashMap<>();
        for (TcpeResultadoExamenDto r : resultados.listar(codRolExamen)) {
            if (r.getCodExamen() != null) porExamen.put(r.getCodExamen(), r);
        }
        List<TcpeCredencialDto> credenciales = new ArrayList<>(externos.size());
        for (Long codExamen : externos) {
            TcpeResultadoExamenDto r = porExamen.get(codExamen);
            PersonaConClave pc = personaPorExamen.get(codExamen);
            String ci = r != null && r.getCi() != null ? r.getCi() : pc != null ? pc.persona.getCi() : null;
            String nombre = r != null && r.getNombreCompleto() != null ? r.getNombreCompleto()
                    : pc != null ? nombreCompleto(pc.persona.getApellidoPaterno(), pc.persona.getApellidoMaterno(),
                    pc.persona.getNombres()) : null;
            credenciales.add(new TcpeCredencialDto(
                    codExamen,
                    r != null ? r.getLogin() : null,
                    clavePorExamen.get(codExamen),
                    null,
                    nombre,
                    ci));
        }
        return new TcpeLoteGeneradoDto(codRolExamen, Collections.unmodifiableList(credenciales));
    }

    /** Mismo formato que {@code f_tcpe_datosUsuario}: apellidos y después nombres; null si no hay ninguno. */
    public static String nombreCompleto(String apellidoPaterno, String apellidoMaterno, String nombres) {
        StringBuilder sb = new StringBuilder();
        for (String s : new String[] { apellidoPaterno, apellidoMaterno, nombres }) {
            if (s != null && !s.trim().isEmpty()) sb.append(sb.length() > 0 ? " " : "").append(s.trim());
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Los tres conteos de un pedido, con los nulos en 0 o en lista vacía. */
    private static final class Conteo {
        private final int cantidad;
        private final List<TcpePersonaExamenDto> personas;
        private final List<Long> empleados;

        Conteo(Integer cantidad, List<TcpePersonaExamenDto> personas, List<Long> empleados) {
            this.cantidad = cantidad == null ? 0 : cantidad;
            this.personas = personas == null ? Collections.<TcpePersonaExamenDto>emptyList() : personas;
            this.empleados = empleados == null ? Collections.<Long>emptyList() : empleados;
        }
    }

    /** Una persona del pedido con la clave que se le muestra y su hash (null en un escrito). */
    static final class PersonaConClave {
        private final TcpePersonaExamenDto persona;
        private final TcpeClaveService.Clave clave;

        PersonaConClave(TcpePersonaExamenDto persona, TcpeClaveService.Clave clave) {
            this.persona = persona;
            this.clave = clave;
        }
    }

    /** Lo que hay que generar, ya validado por el SP y con los hash calculados. Se arma con {@link #pedido}. */
    public static final class Pedido {
        private final List<Long> empleados;
        private final List<TcpeClaveService.Clave> claves;
        private final List<PersonaConClave> personas;
        private final int sinUsuario;
        private final boolean online;

        Pedido(List<Long> empleados, List<TcpeClaveService.Clave> claves, List<PersonaConClave> personas,
               int sinUsuario, boolean online) {
            this.empleados = empleados;
            this.claves = claves;
            this.personas = personas;
            this.sinUsuario = sinUsuario;
            this.online = online;
        }

        public boolean isOnline() { return online; }

        /** Cuántos exámenes va a crear. */
        public int cantidad() {
            return empleados.size() + claves.size() + personas.size() + sinUsuario;
        }
    }
}
