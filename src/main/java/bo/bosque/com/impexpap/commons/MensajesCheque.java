package bo.bosque.com.impexpap.commons;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;

import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dto.BotonesChequeDto;
import bo.bosque.com.impexpap.model.Talonario;

/**
 * Los textos de error del modulo de Cheques, en un solo lugar y sin ninguna regla de negocio: describen lo que
 * {@link ReglasCheque} y {@link ChequeService} deciden, no deciden nada.
 *
 * <p>En el legacy los mensajes eran escuetos y ambiguos ("Verifique el Codigo de Talonario o el Nro de Boleta",
 * "Cheque Duplicado", "Fechas Cobro Fuera de Rango"): decian <i>que</i> fallaba pero no <i>por que</i> ni <i>que
 * hacer</i>. Aqui cada mensaje responde a las tres cosas y, cuando se conoce, trae los datos concretos (el valor
 * escrito, el rango permitido, la empresa donde si existe el talonario...). Las <b>reglas no cambian</b>.
 *
 * <p>Estilo: espanol neutro, tuteo (como el resto de la aplicacion: "Elige", "Escribe"), sin jerga interna. Se muestran
 * tal cual al usuario, uno por linea cuando son varios.
 */
public final class MensajesCheque {

    private MensajesCheque() {
    }

    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** dd/MM/yyyy de una fecha (por dia, como el resto del modulo). */
    static String dia(Date d) {
        return d == null ? "" : ReglasCheque.dia(d).format(DIA);
    }

    static String dia(LocalDate d) {
        return d.format(DIA);
    }

    // ===================================================================== //
    //                    CAMPOS: FALTAN Y NO TIENEN FORMATO                 //
    // ===================================================================== //

    /**
     * Un campo con formato invalido. Dice el valor escrito, <b>cuales caracteres sobran</b> (o el largo, si el problema es
     * ese) y que acepta el campo.
     *
     * @param campo      "El recibo manual", "«A la orden de»"...
     * @param unCaracter un caracter permitido; sirve solo para <i>describir</i> cuales sobran: la decision de validar la
     *                   sigue tomandola el patron completo de {@link ReglasCheque}
     * @param queAcepta  "letras, números y espacios"
     * @param min        largo minimo (0 = sin minimo)
     * @param max        largo maximo ({@link Integer#MAX_VALUE} = sin maximo)
     */
    public static String formato(String campo, String valor, Pattern unCaracter, String queAcepta, int min, int max) {
        if (valor == null) return campo + " es obligatorio.";
        StringBuilder m = new StringBuilder(campo).append(" «").append(valor).append("» no es válido: ");

        Set<String> sobran = new LinkedHashSet<>();
        for (int i = 0; i < valor.length(); i++) {
            String c = String.valueOf(valor.charAt(i));
            if (!unCaracter.matcher(c).matches()) sobran.add(verCaracter(valor.charAt(i)));
        }
        if (!sobran.isEmpty()) {
            m.append("tiene caracteres que no se aceptan (").append(String.join(", ", primeros(sobran, 6))).append("). ");
        } else if (valor.length() < min) {
            m.append("es demasiado corto (").append(valor.length()).append(valor.length() == 1 ? " carácter" : " caracteres").append("). ");
        } else {
            m.append("es demasiado largo (").append(valor.length()).append(" caracteres). ");
        }
        m.append("Solo acepta ").append(queAcepta).append(largo(min, max)).append('.');
        return m.toString();
    }

    private static String largo(int min, int max) {
        boolean hayMax = max < Integer.MAX_VALUE;
        if (min > 0 && hayMax) return ", de " + min + " a " + max + " caracteres";
        if (hayMax) return ", hasta " + max + " caracteres";
        if (min > 0) return ", de al menos " + min + " caracteres";
        return "";
    }

    private static List<String> primeros(Set<String> todos, int n) {
        List<String> l = new ArrayList<>(todos);
        if (l.size() <= n) return l;
        List<String> r = new ArrayList<>(l.subList(0, n));
        r.add("…");
        return r;
    }

    private static String verCaracter(char c) {
        if (c == ' ') return "espacio";
        if (c == '\t') return "tabulación";
        return "«" + c + "»";
    }

    public static final String FALTA_NRO_CHEQUE = "Falta el número de cheque. Escríbelo tal como figura en el cheque.";
    public static final String FALTA_CLIENTE = "Falta elegir el cliente. Búscalo por nombre o código y elígelo de la lista.";
    public static final String FALTA_BANCO = "Falta elegir el banco.";
    public static final String FALTA_MONTO = "Falta el monto del cheque.";
    public static final String FALTA_A_LA_ORDEN = "Falta «A la orden de»: escribe a nombre de quién está el cheque.";
    public static final String FALTA_FECHA_CHEQUE = "Falta la fecha del cheque.";
    public static final String FALTA_FECHA_COBRO = "Falta la fecha de cobro.";
    public static final String FALTA_TALONARIO =
            "Falta el talonario manual. Escribe el código del talonario, o 0 si el cheque no tiene talonario.";
    public static final String FALTA_RECIBO =
            "Falta el recibo manual. Escribe el número del recibo (si lo trajo un empleado) o 0 (si lo dejó el cliente).";
    public static final String FALTA_SUCURSAL = "Falta elegir la sucursal. Elige una en los filtros antes de continuar.";
    public static final String FALTA_EMPRESA = "Falta elegir la empresa. Elige una en los filtros antes de continuar.";
    public static final String SIN_DATOS = "No se recibieron los datos del formulario. Vuelve a intentarlo.";
    public static final String SIN_DATOS_ACCION = "No se recibieron los datos de la acción. Vuelve a intentarlo.";

    public static String tipoNoValido(List<String> nombres) {
        return "El tipo de cheque no es válido. Elige " + oLista(nombres) + ".";
    }

    public static String monedaNoValida(List<String> nombres) {
        return "La moneda no es válida. Elige " + oLista(nombres) + ".";
    }

    /** "A, B o C". */
    static String oLista(List<String> l) {
        if (l.size() <= 1) return String.join("", l);
        return String.join(", ", l.subList(0, l.size() - 1)) + " o " + l.get(l.size() - 1);
    }

    // ===================================================================== //
    //                RECIBO Y TALONARIO (coherencia y existencia)           //
    // ===================================================================== //

    public static String clienteConRecibo(String recibo) {
        return "Elegiste «- Cliente -» en «Entregado por» (lo dejó el cliente), por eso el recibo manual debe ser 0 y escribiste «"
                + recibo + "». Si en realidad lo trajo un empleado, elígelo en «Entregado por».";
    }

    public static String empleadoSinRecibo() {
        return "Elegiste un empleado en «Entregado por» (lo trajo un empleado), por eso debes escribir el número de su recibo manual: "
                + "no puede ser 0. Si lo dejó el cliente, elige «- Cliente -».";
    }

    public static String reciboSinTalonario(String recibo) {
        return "Escribiste el recibo manual «" + recibo + "», por eso también debes escribir el talonario manual al que pertenece: "
                + "no puede ser 0.";
    }

    /**
     * El par talonario/recibo no coincide con {@code tmto_talonario} para la empresa del cheque (la rama 'M' dijo que no).
     * <b>Explica el motivo</b> mirando los talonarios que tienen ese codigo en cualquier empresa:
     * <ol>
     *   <li>el recibo no es un numero (la rama 'M' lo convierte a bigint);</li>
     *   <li>no existe ningun talonario con ese codigo;</li>
     *   <li>existe, pero en OTRA empresa (el caso mas comun: se registra en una empresa con el talonario de la otra);</li>
     *   <li>existe en esta empresa pero el recibo esta fuera de su rango.</li>
     * </ol>
     *
     * @param deEseCodigo  los talonarios con ese codigo en todas las empresas (puede estar vacio)
     * @param nombreEmpresa nombre de una empresa por su codigo (vacio o null = se muestra el codigo)
     */
    public static String talonarioNoCoincide(String talonario, String recibo, int codEmpresa,
                                             List<Talonario> deEseCodigo, IntFunction<String> nombreEmpresa) {
        String empresa = nombre(codEmpresa, nombreEmpresa);
        String rec = recibo == null ? "" : recibo.trim();

        if (!rec.matches("\\d{1,18}")) {
            return "El recibo manual «" + rec + "» debe ser un número para poder comprobarlo contra el talonario «" + talonario
                    + "»: tiene letras o símbolos. Escribe solo el número impreso en el recibo.";
        }
        long numero = Long.parseLong(rec);

        List<Talonario> lista = deEseCodigo == null ? new ArrayList<Talonario>() : deEseCodigo;
        List<Talonario> aqui = new ArrayList<>();
        Map<Long, List<Talonario>> enOtras = new LinkedHashMap<>();
        for (Talonario t : lista) {
            if (t.getCodEmpresa() == codEmpresa) aqui.add(t);
            else enOtras.computeIfAbsent(t.getCodEmpresa(), k -> new ArrayList<>()).add(t);
        }

        if (lista.isEmpty()) {
            return "No existe ningún talonario con el código «" + talonario + "». Revisa el código del talonario manual: debe ser "
                    + "el que está impreso en el talonario, sin espacios de más.";
        }

        if (aqui.isEmpty()) {
            List<String> donde = new ArrayList<>();
            boolean reciboDentro = false;
            for (Map.Entry<Long, List<Talonario>> e : enOtras.entrySet()) {
                donde.add(nombre(e.getKey().intValue(), nombreEmpresa));
                for (Talonario t : e.getValue()) {
                    if (numero >= t.getNumeracionInicial() && numero <= t.getNumeracionFinal()) reciboDentro = true;
                }
            }
            Talonario ejemplo = enOtras.values().iterator().next().get(0);
            return "El talonario «" + talonario + "» no pertenece a la empresa " + empresa + ": está registrado en "
                    + (donde.size() == 1 ? "la empresa " : "las empresas ") + String.join(" y ", donde)
                    + (reciboDentro ? " (y el recibo " + rec + " sí está dentro de su numeración, "
                            + ejemplo.getNumeracionInicial() + " a " + ejemplo.getNumeracionFinal() + ")" : "")
                    + ", y este cheque se está registrando en " + empresa + ". Cambia la empresa en los filtros o usa un talonario de "
                    + empresa + ".";
        }

        boolean dentro = false;
        List<String> rangos = new ArrayList<>();
        for (Talonario t : aqui) {
            rangos.add("del " + t.getNumeracionInicial() + " al " + t.getNumeracionFinal());
            if (numero >= t.getNumeracionInicial() && numero <= t.getNumeracionFinal()) dentro = true;
        }
        if (!dentro) {
            return "El recibo " + rec + " no pertenece al talonario «" + talonario + "»: ese talonario cubre solamente los recibos "
                    + String.join(" y ", rangos) + ". Revisa el número del recibo o el código del talonario.";
        }
        // La rama 'M' dijo que no pero el diagnostico no encuentra el motivo: se dice lo mismo sin adivinar.
        return "El talonario «" + talonario + "» y el recibo " + rec + " no coinciden con ningún talonario registrado para la empresa "
                + empresa + ". Revisa los dos datos.";
    }

    /**
     * "Talonario ER1076 (IMPEXPAP): recibos del 3751 al 3800." para mostrar junto al campo cuando el par es correcto.
     * Null si no hay un talonario de esa empresa en la lista.
     */
    public static String detalleDelTalonario(String talonario, int codEmpresa, List<Talonario> deEseCodigo,
                                             IntFunction<String> nombreEmpresa) {
        if (deEseCodigo == null) return null;
        List<String> rangos = new ArrayList<>();
        for (Talonario t : deEseCodigo) {
            if (t.getCodEmpresa() == codEmpresa) rangos.add("del " + t.getNumeracionInicial() + " al " + t.getNumeracionFinal());
        }
        if (rangos.isEmpty()) return null;
        return "Talonario " + talonario + " (" + nombre(codEmpresa, nombreEmpresa) + "): recibos " + String.join(" y ", rangos) + ".";
    }

    public static final String SIN_PERMISO_COMPROBAR_TALONARIO =
            "Para comprobar un talonario necesitas poder registrar o editar cheques (botones btnNuevoCH o btnEditar1CH); "
                    + "pídele al administrador que te los asigne.";

    /** Cuando ni siquiera se pudo consultar los talonarios para explicar el motivo. */
    public static String talonarioNoCoincideGenerico(String talonario, String recibo, int codEmpresa, IntFunction<String> nombreEmpresa) {
        return "El talonario «" + talonario + "» y el recibo «" + recibo + "» no coinciden con ningún talonario registrado para la empresa "
                + nombre(codEmpresa, nombreEmpresa) + ". Revisa que el talonario sea de esa empresa y que el recibo esté dentro de su numeración.";
    }

    private static String nombre(int codEmpresa, IntFunction<String> nombreEmpresa) {
        String n = nombreEmpresa == null ? null : nombreEmpresa.apply(codEmpresa);
        return n == null || n.trim().isEmpty() ? "N.º " + codEmpresa : n.trim();
    }

    // ===================================================================== //
    //                              REGISTRO                                 //
    // ===================================================================== //

    public static String nroChequeSoloCeros(String original) {
        return "El número de cheque «" + (original == null ? "" : original) + "» no es válido: tiene solo ceros. "
                + "Escribe el número real del cheque.";
    }

    public static String chequeDuplicado(String nroCheque, String codCliente, Date fechaCheque) {
        return "Este cheque ya está registrado: existe otro cheque con el mismo número (" + nroCheque + "), cliente ("
                + codCliente + "), banco, fecha del cheque (" + dia(fechaCheque) + ") y sucursal. No se puede registrar dos veces.";
    }

    public static String cobroFueraDeRango(Date cobro, Date cheque) {
        return "La fecha de cobro (" + dia(cobro) + ") está fuera de lo permitido: " + reglaDelRango(cheque);
    }

    public static String nuevaFechaCobroFueraDeRango(Date cobro, Date cheque) {
        return "La nueva fecha de cobro (" + dia(cobro) + ") está fuera de lo permitido: " + reglaDelRango(cheque)
                + " Si necesitas una fecha más lejana, pídele a un administrador que la cambie.";
    }

    private static String reglaDelRango(Date cheque) {
        LocalDate f = ReglasCheque.dia(cheque);
        return "debe estar a no más de " + ReglasCheque.DIAS_TOLERANCIA_COBRO + " días antes o después de la fecha del cheque ("
                + dia(f) + "), es decir, entre el " + dia(f.minusDays(ReglasCheque.DIAS_TOLERANCIA_COBRO)) + " y el "
                + dia(f.plusDays(ReglasCheque.DIAS_TOLERANCIA_COBRO)) + ".";
    }

    public static String empresaNoHabilitada(List<String> habilitadas) {
        return "La empresa elegida no está habilitada para cheques. Elige " + oLista(habilitadas) + ".";
    }

    public static String sucursalNoEsDeLaEmpresa(String empresa) {
        return "La sucursal elegida no corresponde a la empresa " + empresa + " o tu usuario no puede usarla. "
                + "Elige una de las sucursales que ofrece la lista de esa empresa.";
    }

    public static final String SUCURSAL_AJENA =
            "Tu usuario no tiene permiso para modificar los datos de otra sucursal. Cambia a tu propia sucursal para continuar.";

    public static final String TALONARIO_EN_ALTA =
            "Un cheque nuevo no se puede registrar con el formulario de talonario. Usa el formulario de registro.";

    public static String chequeNoEncontrado(Integer codCheque) {
        return "No se encontró el cheque " + codCheque + ". Es posible que ya no exista: actualiza la pantalla.";
    }

    public static String accionNoEncontrada(int codAccion) {
        return "No se encontró la acción " + codAccion + ". Es posible que ya la hayan eliminado: actualiza la pantalla.";
    }

    // ===================================================================== //
    //                           ACCIONES DEL DETALLE                        //
    // ===================================================================== //

    /** Las cuatro acciones que habilita la rama K. */
    public enum AccionDelDetalle {
        FECHA_COBRO("cambiar la fecha de cobro"),
        DEVOLVER("devolver el cheque"),
        CERRAR_CON_VERIFICACION("cerrar el cheque con verificación"),
        CERRAR_SIN_VERIFICACION("cerrar el cheque sin verificación");

        final String texto;

        AccionDelDetalle(String texto) {
            this.texto = texto;
        }
    }

    /**
     * Por que una accion no esta habilitada. Lee las cuatro banderas de la rama K y el estado del cheque; cada combinacion
     * conocida de la K (ver CLAUDE.md) tiene su explicacion.
     */
    public static String accionNoHabilitada(AccionDelDetalle accion, boolean cerrado, BotonesChequeDto k) {
        String base = "No se puede " + accion.texto + " ahora: ";
        if (cerrado) return base + "el cheque está cerrado y ya no admite más acciones.";

        boolean f = k.isFechaCobro(), d = k.isDevolver(), cc = k.isCerrarConVerificacion(), cs = k.isCerrarSinVerificacion();
        if (!f && !d && !cc && !cs) {
            return base + "el cheque todavía no salió de caja. Primero hay que hacer el traspaso a cobranza (botón «Traspaso»).";
        }
        if (f && !d && !cc && !cs) {   // 1000: en la oficina
            return base + "el cheque está en la oficina, no en cobranza. Primero entrégalo a un cobrador con «A Custodio».";
        }
        if (!f && d && !cc && cs) {    // 0101: en cobranza, sin verificacion
            return accion == AccionDelDetalle.FECHA_COBRO
                    ? base + "el cheque está en cobranza. Para cambiar su fecha de cobro primero debe volver a la oficina (botón «Devolver»)."
                    : base + "el depósito de este cheque no tiene una verificación válida. Verifícalo primero o ciérralo sin verificación.";
        }
        if (!f && !d && cc && !cs) {   // 0010: verificado
            return base + "el depósito de este cheque ya fue verificado, así que solo puede cerrarse con verificación.";
        }
        if (!f && d && !cc && !cs) {   // 0100: dato inconsistente
            return base + "los datos del cheque son inconsistentes (tiene más devoluciones que entregas a cobranza). Avisa a sistemas.";
        }
        return base + "la acción no está habilitada para el estado actual del cheque.";
    }

    public static String sinTraspaso(int traspasos) {
        return traspasos == 0
                ? "No se puede cambiar la fecha de cobro: este cheque todavía no tiene el traspaso a cobranza (no salió de caja)."
                : "No se puede cambiar la fecha de cobro: este cheque tiene " + traspasos
                        + " traspasos registrados y debe tener exactamente uno. Revisa su historial.";
    }

    public static final String NO_ESTA_EN_LA_OFICINA =
            "No se puede cambiar la fecha de cobro: el cheque tiene que estar en la oficina y fue entregado a cobranza más veces de las "
                    + "que se devolvió. Primero usa «Devolver».";

    public static String pocasAcciones(int tiene, int necesita) {
        return "Para devolver o cerrar, el cheque debe haber salido de caja y haberse entregado a cobranza: su historial tiene solo "
                + tiene + (tiene == 1 ? " acción" : " acciones") + " y se necesitan al menos " + necesita
                + " (RECIBIDO, TRASPASO y A COBRANZA).";
    }

    public static final String FALTA_NUEVA_FECHA_COBRO = "Falta la nueva fecha de cobro.";

    public static final String FALTA_NRO_SAP = "Para cerrar el cheque debes escribir el número de SAP: es obligatorio al finalizar.";

    public static final String ESTADO_RESERVADO =
            "Los estados RECIBIDO, TRASPASO y A COBRANZA no se guardan desde aquí: los crea el sistema (al registrar el cheque, con "
                    + "«Traspaso» y con «A Custodio»). Elige otro estado.";

    public static String estadoDeAccionNoValido(List<String> permitidos) {
        return "El estado de la acción no es válido para esta operación. Elige " + oLista(permitidos) + ".";
    }

    public static String fechaDeAccionPasada(Date fecha, LocalDate hoy) {
        return "La fecha de la acción (" + dia(fecha) + ") no puede ser anterior a hoy (" + dia(hoy) + ").";
    }

    // ===================================================================== //
    //                         TRASPASO Y CUSTODIA                           //
    // ===================================================================== //

    public static final String SIN_PENDIENTES_DE_TRASPASO =
            "No hay cheques para traspasar en esta sucursal. Solo se traspasan los cheques recién recibidos que todavía no se entregaron "
                    + "(los que tienen una única acción: RECIBIDO).";

    public static final String FALTA_RESPONSABLE =
            "Falta elegir el responsable (jefe de cobranzas o cobrador) que recibe los cheques.";

    public static final String FALTA_CHEQUE_PARA_CUSTODIA = "Marca al menos un cheque para entregar en custodia.";

    public static String chequeNoDisponibleParaCustodia(int codCheque) {
        return "El cheque " + codCheque + " no está disponible para entregar en custodia: solo se entregan los cheques que ya fueron "
                + "traspasados y todavía no están en cobranza. Actualiza la lista.";
    }

    public static final String FALTA_SUCURSAL_Y_FECHA = "Falta elegir la sucursal y la fecha.";
    public static final String FALTA_CHEQUE_Y_HORA =
            "Falta elegir el cheque y la entrega (fecha y hora) cuya custodia se va a copiar.";
    public static final String ENTREGA_NO_ES_CUSTODIA =
            "La acción elegida no es una entrega a cobranza: solo se pueden copiar las entregas «A COBRANZA».";
    public static final String CHEQUE_DE_OTRA_SUCURSAL = "El cheque elegido no es de la sucursal indicada. Actualiza la lista.";
    public static final String ENTREGA_DE_OTRA_SUCURSAL = "La entrega elegida no es de la sucursal indicada. Actualiza la lista.";
    public static final String RANGO_DE_RECEPCION_INVERTIDO =
            "La fecha de recepción inicial no puede ser posterior a la final. Corrige el rango de fechas.";
    public static final String SIN_EMPRESAS = "No hay empresas habilitadas para cheques. Avisa a sistemas.";

    // ===================================================================== //
    //                       REPORTES Y BANCOS (los mismos criterios)        //
    // ===================================================================== //

    public static String estadoDeChequeNoValido(String estado, List<String> validos) {
        return "El estado «" + estado + "» no es válido para el reporte. Elige " + oLista(validos) + ", o deja «Todos».";
    }

    public static final String SIN_CHEQUE_PROPIO =
            "Todavía no registraste ningún cheque en esta sucursal, por eso no hay un «último cheque» cuyo recibo imprimir.";

    public static String reciboSinRecepcion(int codCheque) {
        return "No se pudo armar el recibo del cheque " + codCheque
                + ": le falta la acción de recepción (RECIBIDO) en su historial.";
    }

    public static final String ELEGIR_TRASPASO = "Elige el traspaso a imprimir (fecha y hora).";

    public static final String FALTA_NOMBRE_BANCO = "Falta el nombre del banco. Escríbelo (de 3 a 50 caracteres).";
    public static final String FALTA_BANCO_A_ELIMINAR = "No se indicó qué banco eliminar. Elige un banco de la lista.";

    public static String bancoNoEncontrado(Integer codBanco) {
        return "No se encontró el banco " + codBanco + ". Es posible que ya lo hayan eliminado: actualiza la pantalla.";
    }

    // ===================================================================== //
    //                     DOCUMENTO PDF DEL CHEQUE                          //
    // ===================================================================== //

    public static final String FALTA_CHEQUE_PDF =
            "Falta indicar de qué cheque es el PDF. Elige un cheque de la lista y vuelve a intentarlo.";

    /** El codigo interno del cheque no es un entero mayor que 0 (es lo que da nombre al archivo: nunca se usa texto del cliente). */
    public static String codChequePdfNoValido(String recibido) {
        return "El código de cheque «" + (recibido == null ? "" : recibido) + "» no es válido: debe ser un número entero mayor que 0. "
                + "Actualiza la pantalla y vuelve a elegir el cheque.";
    }

    public static final String FALTA_ARCHIVO_PDF =
            "No se recibió ningún archivo. Elige el PDF del cheque (de menos de 2 MegaBytes) y vuelve a intentarlo.";

    /** Palabras del legacy ("Solo se permiten pdf"), con el nombre del archivo elegido y que hacer. */
    public static String archivoNoEsPdf(String nombre) {
        return "Solo se permiten pdf: el archivo «" + (nombre == null ? "" : nombre) + "» no tiene la extensión .pdf. "
                + "Elige el documento del cheque en formato PDF.";
    }

    public static String archivoPdfVacio(String nombre) {
        return "El archivo «" + (nombre == null ? "" : nombre) + "» está vacío (0 bytes). Elige el PDF correcto del cheque.";
    }

    /** Palabras del legacy ("menos de 2 MegaByte"), con el peso recibido y el limite exactos. */
    public static String archivoPdfMuyGrande(long bytes, long maximo) {
        return "El archivo pesa " + bytes(bytes) + " (" + megas(bytes) + " MB) y debe ser de menos de 2 MegaBytes (máximo "
                + bytes(maximo) + "). Elige un PDF más liviano o comprímelo antes de subirlo.";
    }

    public static String archivoPdfSinContenidoPdf(String nombre) {
        return "El archivo «" + (nombre == null ? "" : nombre) + "» tiene la extensión .pdf pero su contenido no es un PDF "
                + "(no empieza con la marca %PDF-). Puede estar dañado o ser otro tipo de archivo con el nombre cambiado: "
                + "ábrelo para comprobarlo o genera el PDF de nuevo.";
    }

    public static final String CARPETA_PDF_NO_CONFIGURADA =
            "La carpeta de los PDF de cheques está configurada en blanco en el servidor (propiedad cheques.pdf.dir, variable de entorno "
                    + "CHEQUES_PDF_DIR). Avisa a sistemas: por defecto es la subcarpeta cheques de la carpeta de archivos de la aplicación.";

    /**
     * La carpeta esta configurada pero no sirve. Se dice la ruta y el motivo. La carpeta se crea sola al subir el primer PDF, asi
     * que este error solo sale si no se pudo crear, si esa ruta no es una carpeta o si no se puede leer o escribir en ella.
     *
     * @param motivo "no se pudo crear", "no es una carpeta", "no se puede leer", "no se puede escribir en ella", "la ruta no es válida"
     */
    public static String carpetaPdfNoDisponible(String ruta, String motivo) {
        return "La carpeta de los PDF de cheques no está disponible en el servidor («" + ruta + "»): " + motivo
                + ". Avisa a sistemas para que revisen esa ruta (propiedad cheques.pdf.dir) y los permisos del servidor sobre ella.";
    }

    public static String pdfNoEncontrado(int codCheque) {
        return "No se encontró el archivo PDF del cheque " + codCheque + ". Todavía no se cargó ninguno, o el archivo ya no está en la "
                + "carpeta del servidor. Usa «Cargar Documento PDF» para subirlo.";
    }

    public static String noSePudoGuardarPdf(int codCheque) {
        return "No se pudo guardar el PDF del cheque " + codCheque + " en la carpeta del servidor y el documento anterior no se tocó. "
                + "Vuelve a intentarlo; si se repite, avisa a sistemas (puede faltar espacio o permiso de escritura).";
    }

    public static String noSePudoLeerPdf(int codCheque) {
        return "No se pudo leer el PDF del cheque " + codCheque + " desde la carpeta del servidor. "
                + "Vuelve a intentarlo; si se repite, avisa a sistemas.";
    }

    /** 2010000 -> "2.010.000 bytes" (punto como separador de miles, como se escriben las cifras en el resto del sistema). */
    private static String bytes(long n) {
        java.text.DecimalFormatSymbols s = new java.text.DecimalFormatSymbols(java.util.Locale.ROOT);
        s.setGroupingSeparator('.');
        return new java.text.DecimalFormat("#,##0", s).format(n) + " bytes";
    }

    /** Megas decimales con dos decimales y coma: 2350000 -> "2,35". */
    private static String megas(long n) {
        return String.format(java.util.Locale.ROOT, "%.2f", n / 1_000_000.0).replace('.', ',');
    }

    // ===================================================================== //
    //     PANELES DEL DETALLE: NOTAS DE REMISION, TRANSACCIONES Y           //
    //     POSTERGACIONES (y el PDF de la postergacion)                      //
    // ===================================================================== //

    public static final String FALTA_CHEQUE_DEL_PANEL =
            "Falta indicar de qué cheque son estos datos. Actualiza la pantalla y vuelve a entrar al detalle del cheque.";

    // ---- notas de remision

    public static final String FALTA_NOTA_REMISION = "Falta la nota de remisión. Escribe su número (de 5 a 10 dígitos).";

    /** @param unCaracter un caracter permitido ({@code [0-9 ]}); solo sirve para decir cuales sobran, decide el patron completo. */
    public static String notaRemisionNoValida(String valor, Pattern unCaracter) {
        return formato("El número de la nota de remisión", valor, unCaracter, "números y espacios", 5, 10);
    }

    public static final String FALTA_NRO_FACTURA =
            "Falta el número de factura de SAP. Escríbelo (un número entero de 1 a 6 dígitos).";

    public static String nroFacturaNoPositivo(int valor) {
        return "El número de factura debe ser mayor a cero y escribiste " + valor + ". Escribe el número real de la factura de SAP.";
    }

    public static String nroFacturaMuyLargo(int valor, int maximo) {
        return "El número de factura «" + valor + "» es demasiado largo: tiene " + String.valueOf(valor).length()
                + " dígitos y el campo acepta hasta " + String.valueOf(maximo).length() + " (como máximo " + maximo
                + "). Revisa el número de la factura de SAP.";
    }

    public static final String FALTA_FECHA_FACTURA =
            "Falta la fecha de la factura. Elige la fecha que figura en la factura de SAP.";

    public static final String FALTA_NOTA_A_ELIMINAR =
            "No se indicó qué nota de remisión eliminar. Elígela de la lista del cheque y vuelve a intentarlo.";

    public static String notaNoEncontrada(int codCheque, String nota) {
        return "El cheque " + codCheque + " no tiene la nota de remisión «" + (nota == null ? "" : nota.trim())
                + "». Es posible que ya la hayan eliminado: actualiza la pantalla.";
    }

    /** Si habia repetidas (mismo cheque y mismo numero) el sistema borra todas, como el legacy: se dice cuantas. */
    public static String notaEliminada(int filas, String nota) {
        if (filas <= 1) return "Nota de remisión eliminada.";
        return "Se eliminaron las " + filas + " notas de remisión «" + (nota == null ? "" : nota.trim())
                + "» que estaban repetidas en el cheque: el sistema borra todas las que tienen el mismo número.";
    }

    // ---- transacciones bancarias

    public static final String FALTA_NRO_TRANSACCION =
            "Falta el número de transacción. Escribe el número que figura en el comprobante del banco (más de 4 caracteres).";

    public static String nroTransaccionCorto(String valor, int largo) {
        return "El número de transacción «" + valor + "» es demasiado corto: tiene " + largo
                + (largo == 1 ? " carácter" : " caracteres") + " y debe tener más de 4. "
                + "Revisa que lo escribiste completo, tal como figura en el comprobante del banco.";
    }

    public static String nroTransaccionLargo(String valor, int largo, int maximo) {
        return "El número de transacción «" + valor + "» es demasiado largo: tiene " + largo + " caracteres y el máximo es "
                + maximo + ". Revisa que sea el número de la transacción y no otro texto del comprobante.";
    }

    public static final String FALTA_BANCO_DE_TRANSACCION = "Falta elegir el banco de la transacción. Elige uno de la lista.";

    public static final String FALTA_FECHA_TRANSACCION =
            "Falta la fecha de la transacción. Elige la fecha que figura en el comprobante del banco.";

    public static final String FALTA_TRANSACCION_A_ELIMINAR =
            "No se indicó qué transacción bancaria eliminar. Elígela de la lista del cheque y vuelve a intentarlo.";

    public static String transaccionNoEncontrada(int codCheque, String nro) {
        return "El cheque " + codCheque + " no tiene la transacción bancaria «" + (nro == null ? "" : nro.trim())
                + "». Es posible que ya la hayan eliminado: actualiza la pantalla.";
    }

    public static String transaccionEliminada(int filas, String nro) {
        if (filas <= 1) return "Transacción bancaria eliminada.";
        return "Se eliminaron las " + filas + " transacciones bancarias «" + (nro == null ? "" : nro.trim())
                + "» que estaban repetidas en el cheque: el sistema borra todas las que tienen el mismo número.";
    }

    // ---- postergaciones

    public static final String FALTA_FECHA_POSTERGACION =
            "Falta la fecha de la postergación. Elige la fecha en que se pidió o se concedió la postergación.";

    public static final String FALTA_OBSERVACION_POSTERGACION =
            "Falta la observación. Escribe el motivo de la postergación (más de 2 caracteres).";

    public static String observacionPostergacionCorta(String valor, int largo) {
        return "La observación «" + valor + "» es demasiado corta: tiene " + largo + (largo == 1 ? " carácter" : " caracteres")
                + " y debe tener más de 2. Escribe el motivo de la postergación: quién la pidió y hasta cuándo.";
    }

    public static String observacionPostergacionLarga(int largo, int maximo) {
        return "La observación es demasiado larga: tiene " + largo + " caracteres y el máximo es " + maximo + ". Acórtala.";
    }

    public static final String FALTA_POSTERGACION =
            "No se indicó qué postergación usar. Elígela de la lista del cheque y vuelve a intentarlo.";

    public static String postergacionNoEncontrada(Integer codPostergacion) {
        return "No se encontró la postergación " + codPostergacion
                + ". Es posible que ya la hayan eliminado: actualiza la pantalla.";
    }

    public static String postergacionDeOtroCheque(int codPostergacion, int codCheque) {
        return "La postergación " + codPostergacion + " no es del cheque " + codCheque
                + " (es de otro cheque), por eso no se puede eliminar desde aquí. Actualiza la pantalla y vuelve a elegirla.";
    }

    // ---- documento PDF de la postergacion

    public static final String FALTA_POSTERGACION_PDF =
            "Falta indicar de qué postergación es el PDF. Elige una postergación de la lista y vuelve a intentarlo.";

    /** El codigo de la postergacion no es un entero mayor que 0 (da nombre al archivo: nunca se usa texto del cliente). */
    public static String codPostergacionPdfNoValido(String recibido) {
        return "El código de postergación «" + (recibido == null ? "" : recibido) + "» no es válido: debe ser un número entero "
                + "mayor que 0. Actualiza la pantalla y vuelve a elegir la postergación.";
    }

    public static final String FALTA_ARCHIVO_PDF_POSTERGACION =
            "No se recibió ningún archivo. Elige el PDF de la postergación (de menos de 2 MegaBytes) y vuelve a intentarlo.";

    public static String archivoNoEsPdfPostergacion(String nombre) {
        return "Solo se permiten pdf: el archivo «" + (nombre == null ? "" : nombre) + "» no tiene la extensión .pdf. "
                + "Elige el documento de la postergación en formato PDF.";
    }

    public static String archivoPdfPostergacionVacio(String nombre) {
        return "El archivo «" + (nombre == null ? "" : nombre) + "» está vacío (0 bytes). Elige el PDF correcto de la postergación.";
    }

    public static final String CARPETA_PDF_POSTERGACION_NO_CONFIGURADA =
            "La carpeta de los PDF de postergaciones está configurada en blanco en el servidor (propiedad cheques.postergacion.pdf.dir, "
                    + "variable de entorno POSTERGACIONES_PDF_DIR). Avisa a sistemas: por defecto es la subcarpeta postergacionCheques "
                    + "de la carpeta de archivos de la aplicación.";

    /** Igual que {@link #carpetaPdfNoDisponible}: se dice la ruta y el motivo; la carpeta se crea sola al subir el primer PDF. */
    public static String carpetaPdfPostergacionNoDisponible(String ruta, String motivo) {
        return "La carpeta de los PDF de postergaciones no está disponible en el servidor («" + ruta + "»): " + motivo
                + ". Avisa a sistemas para que revisen esa ruta (propiedad cheques.postergacion.pdf.dir) y los permisos del servidor sobre ella.";
    }

    public static String pdfPostergacionNoEncontrado(int codPostergacion) {
        return "No se encontró el archivo PDF de la postergación " + codPostergacion + ". Todavía no se cargó ninguno, o el "
                + "archivo ya no está en la carpeta del servidor. Usa «Cargar PDF» para subirlo.";
    }

    public static String noSePudoGuardarPdfPostergacion(int codPostergacion) {
        return "No se pudo guardar el PDF de la postergación " + codPostergacion + " en la carpeta del servidor y el documento "
                + "anterior no se tocó. Vuelve a intentarlo; si se repite, avisa a sistemas (puede faltar espacio o permiso de escritura).";
    }

    public static String noSePudoLeerPdfPostergacion(int codPostergacion) {
        return "No se pudo leer el PDF de la postergación " + codPostergacion + " desde la carpeta del servidor. "
                + "Vuelve a intentarlo; si se repite, avisa a sistemas.";
    }

    // ===================================================================== //
    //                               PERMISOS                                //
    // ===================================================================== //

    private static final Map<String, String> QUE_PERMITE_EL_BOTON = new LinkedHashMap<>();

    static {
        QUE_PERMITE_EL_BOTON.put("btnNuevoCH", "registrar cheques");
        QUE_PERMITE_EL_BOTON.put("btnNuevo2CH", "registrar cheques como administrador");
        QUE_PERMITE_EL_BOTON.put("btnEditar1CH", "editar cheques");
        QUE_PERMITE_EL_BOTON.put("btnEditar2CH", "cambiar la fecha de cobro de un cheque");
        QUE_PERMITE_EL_BOTON.put("btnEditar3CH", "editar cheques como administrador");
        QUE_PERMITE_EL_BOTON.put("btnDetalleCH", "ver el detalle de un cheque y hacer sus acciones");
        QUE_PERMITE_EL_BOTON.put("btnEliminarSegCH", "eliminar acciones del historial de un cheque");
        QUE_PERMITE_EL_BOTON.put("btnNuevoNRCH", "registrar notas de remisión, transacciones bancarias y postergaciones de un cheque");
        QUE_PERMITE_EL_BOTON.put("btnEliminarNRCH", "eliminar notas de remisión de un cheque");
        QUE_PERMITE_EL_BOTON.put("btnTraspasoCH", "hacer el traspaso de cheques a cobranza");
        QUE_PERMITE_EL_BOTON.put("btnCustodiaCH", "entregar cheques a custodia");
        QUE_PERMITE_EL_BOTON.put("btnCustodia2CH", "dar custodia como administrador");
        QUE_PERMITE_EL_BOTON.put("btnChqSucrs", "ver los cheques de otras sucursales");
        QUE_PERMITE_EL_BOTON.put("btnRpt1CH", "generar el reporte de cheques recibidos");
        QUE_PERMITE_EL_BOTON.put("btnRpt2CH", "generar el reporte de cheques de cobranza");
        QUE_PERMITE_EL_BOTON.put("btnRpt3CH", "generar el reporte de cheques en custodia");
        QUE_PERMITE_EL_BOTON.put("btnRpt4CH", "imprimir el recibo del último cheque");
        QUE_PERMITE_EL_BOTON.put("btnRpt5CH", "reimprimir un traspaso");
        QUE_PERMITE_EL_BOTON.put("btnNuevoB", "registrar bancos");
        QUE_PERMITE_EL_BOTON.put("btnEditarB", "editar bancos");
        QUE_PERMITE_EL_BOTON.put("btnEliminarB", "eliminar bancos");
    }

    /** "No tienes permiso para registrar cheques. ... botón btnNuevoCH ...": que falta y que hacer. */
    public static String sinPermiso(String boton) {
        String que = QUE_PERMITE_EL_BOTON.get(boton);
        if (que == null) {
            return "No tienes permiso para hacer esto. Tu usuario no tiene asignado el botón " + boton
                    + "; pídele al administrador que te lo asigne.";
        }
        return "No tienes permiso para " + que + ". Tu usuario no tiene asignado el botón " + boton
                + "; pídele al administrador que te lo asigne.";
    }

    public static final String SOLO_SU_SUCURSAL =
            "Solo puedes ver los cheques de tu propia sucursal. Para ver otras sucursales tu usuario necesita el permiso "
                    + "«ver los cheques de otras sucursales» (botón btnChqSucrs); pídeselo al administrador.";

    public static final String SOLO_SU_SUCURSAL_ESCRIBIR =
            "Solo puedes registrar o modificar cheques de tu propia sucursal. Para trabajar en otras sucursales tu usuario necesita el permiso "
                    + "«ver los cheques de otras sucursales» (botón btnChqSucrs); pídeselo al administrador.";

    public static String editarUnCerrado(String boton) {
        return "Este cheque está cerrado: no se puede editar con ese formulario. En un cheque cerrado solo se pueden corregir el "
                + "talonario y el recibo (permiso " + boton + ").";
    }

    public static final String TALONARIO_SOLO_CERRADOS =
            "El formulario de talonario es solo para cheques cerrados y este cheque está abierto. Usa «Editar».";

    /**
     * Como {@code acceso.exigirBoton}, pero el 403 trae el motivo y que hacer (ver {@link SinPermisoException}) en vez del texto
     * fijo del manejador global.
     */
    public static void exigirBoton(AccesoModuloHelper acceso, Authentication auth, int codVista, String boton) {
        try {
            acceso.exigirBoton(auth, codVista, boton);
        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw new SinPermisoException(sinPermiso(boton));
        }
    }
}
