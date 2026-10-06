package bo.bosque.com.impexpap.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import bo.bosque.com.impexpap.commons.MensajesCheque.AccionDelDetalle;
import bo.bosque.com.impexpap.config.SinPermisoException;
import bo.bosque.com.impexpap.dto.BotonesChequeDto;
import bo.bosque.com.impexpap.model.Talonario;

/**
 * Los textos de error del modulo: que digan <b>por que</b> falla algo, con los datos concretos, y que no se
 * contradigan con las reglas. Sin base de datos ni Spring.
 */
class MensajesChequeTest {

    private static final IntFunction<String> EMPRESAS = nombres(1, "IMPEXPAP", 5, "ESPPAPEL");

    private static IntFunction<String> nombres(Object... paresCodigoNombre) {
        Map<Integer, String> m = new HashMap<>();
        for (int i = 0; i < paresCodigoNombre.length; i += 2) m.put((Integer) paresCodigoNombre[i], (String) paresCodigoNombre[i + 1]);
        return m::get;
    }

    private static Talonario talonario(String nro, int empresa, int inicial, int fin) {
        Talonario t = new Talonario();
        t.setNroTalonario(nro);
        t.setCodEmpresa(empresa);
        t.setNumeracionInicial(inicial);
        t.setNumeracionFinal(fin);
        return t;
    }

    private static Date d(int a, int m, int dia) {
        return java.sql.Date.valueOf(LocalDate.of(a, m, dia));
    }

    // ------------------------------------------------------------------ talonario y recibo

    @Test
    @DisplayName("EL CASO REPORTADO: el talonario ER1076 es de IMPEXPAP y el cheque se registra en ESPPAPEL: el mensaje lo dice")
    void talonarioDeOtraEmpresa() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "3752", 5,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);

        assertTrue(m.contains("«ER1076»"), m);
        assertTrue(m.contains("no pertenece a la empresa ESPPAPEL"), m);
        assertTrue(m.contains("está registrado en la empresa IMPEXPAP"), m);
        assertTrue(m.contains("el recibo 3752 sí está dentro de su numeración, 3751 a 3800"), m);
        assertTrue(m.contains("Cambia la empresa en los filtros"), "dice que hacer: " + m);
    }

    @Test
    @DisplayName("talonario de otra empresa y el recibo tampoco entra en su rango: no dice que el recibo sí entra")
    void talonarioDeOtraEmpresaConReciboFueraDeRango() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "9999", 5,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);
        assertTrue(m.contains("no pertenece a la empresa ESPPAPEL"), m);
        assertFalse(m.contains("sí está dentro"), m);
    }

    @Test
    @DisplayName("el talonario no existe en ninguna empresa")
    void talonarioInexistente() {
        String m = MensajesCheque.talonarioNoCoincide("XX9", "10", 1, Collections.<Talonario>emptyList(), EMPRESAS);
        assertTrue(m.contains("No existe ningún talonario con el código «XX9»"), m);
        assertTrue(m.contains("Revisa el código"), m);
    }

    @Test
    @DisplayName("el talonario es de la empresa pero el recibo esta fuera de su numeracion: dice el rango")
    void reciboFueraDelRango() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "3801", 1,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);
        assertTrue(m.contains("El recibo 3801 no pertenece al talonario «ER1076»"), m);
        assertTrue(m.contains("del 3751 al 3800"), m);
    }

    @Test
    @DisplayName("un recibo que no es un numero no se puede comprobar: se dice eso, no 'verifique'")
    void reciboNoNumerico() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "37A2", 1,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);
        assertTrue(m.contains("debe ser un número"), m);
        assertTrue(m.contains("«37A2»"), m);
    }

    @Test
    @DisplayName("si el diagnostico no encuentra el motivo (la regla dijo que no, pero todo cuadra) lo dice sin adivinar")
    void sinMotivoVisible() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "3752", 1,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);
        assertTrue(m.contains("no coinciden con ningún talonario registrado para la empresa IMPEXPAP"), m);
    }

    @Test
    @DisplayName("una empresa sin nombre conocido se muestra por su codigo; el generico tambien nombra la empresa")
    void empresaSinNombre() {
        String m = MensajesCheque.talonarioNoCoincide("ER1076", "3752", 7,
                Collections.singletonList(talonario("ER1076", 1, 3751, 3800)), EMPRESAS);
        assertTrue(m.contains("la empresa N.º 7"), m);
        String g = MensajesCheque.talonarioNoCoincideGenerico("ER1076", "3752", 5, EMPRESAS);
        assertTrue(g.contains("ESPPAPEL") && g.contains("«ER1076»"), g);
    }

    @Test
    @DisplayName("coherencia entre quien lo dejo, el recibo y el talonario: los tres mensajes dicen que se eligio y que cambiar")
    void coherencia() {
        List<String> cliente = ReglasCheque.erroresReciboTalonario(0, "55", "ER1");
        assertEquals(1, cliente.size());
        assertTrue(cliente.get(0).contains("«- Cliente -»") && cliente.get(0).contains("«55»")
                && cliente.get(0).contains("elígelo en «Entregado por»"), cliente.get(0));

        List<String> empleado = ReglasCheque.erroresReciboTalonario(7, "0", "ER1");
        assertTrue(empleado.get(0).contains("un empleado") && empleado.get(0).contains("no puede ser 0")
                && empleado.get(0).contains("elige «- Cliente -»"), empleado.get(0));

        List<String> sinTalonario = ReglasCheque.erroresReciboTalonario(7, "3752", "0");
        assertTrue(sinTalonario.get(0).contains("recibo manual «3752»") && sinTalonario.get(0).contains("talonario manual"),
                sinTalonario.get(0));
    }

    @Test
    @DisplayName("detalle de un par correcto: talonario, empresa y rango; sin talonario de esa empresa, nada")
    void detalleDelTalonario() {
        List<Talonario> uno = Collections.singletonList(talonario("ER1076", 1, 3751, 3800));
        assertEquals("Talonario ER1076 (IMPEXPAP): recibos del 3751 al 3800.",
                MensajesCheque.detalleDelTalonario("ER1076", 1, uno, EMPRESAS));
        assertEquals(null, MensajesCheque.detalleDelTalonario("ER1076", 5, uno, EMPRESAS), "es de otra empresa");
        assertEquals(null, MensajesCheque.detalleDelTalonario("ER1076", 1, null, EMPRESAS));
        assertEquals(null, MensajesCheque.detalleDelTalonario("ER1076", 1, Collections.<Talonario>emptyList(), EMPRESAS));
    }

    // ------------------------------------------------------------------ formato

    @Test
    @DisplayName("formato: nombra los caracteres que sobran, el valor escrito y que acepta el campo")
    void formatoNombraLoQueSobra() {
        String m = ReglasCheque.errorReciboManual("ab-12");
        assertTrue(m.startsWith("El recibo manual «ab-12» no es válido"), m);
        assertTrue(m.contains("caracteres que no se aceptan («-»)"), m);
        assertTrue(m.contains("Solo acepta letras, números, espacios y apóstrofe, de 1 a 25 caracteres"), m);

        assertTrue(ReglasCheque.errorAOrdenDe("SPC IMPRESORES S.A.").contains("(«.»)"));
        assertTrue(ReglasCheque.errorNroTalonario("ER-123").contains("(«-»)"), "el guion NO se acepta en el talonario");
        assertTrue(ReglasCheque.errorNombreBanco("Banco Union S.A.").contains("«.»"));
    }

    @Test
    @DisplayName("formato: el espacio se nombra como 'espacio' y se resumen los muchos caracteres malos")
    void formatoEspacioYResumen() {
        assertTrue(ReglasCheque.errorNroCheque("12 34").contains("(espacio)"));
        String muchos = ReglasCheque.errorNroCheque("a.b,c;d:e!f@g#h");
        assertTrue(muchos.contains("…"), "con mas de 6 caracteres distintos se corta con puntos suspensivos: " + muchos);
    }

    @Test
    @DisplayName("formato: si el problema es el largo, dice el largo (talonario de 26 caracteres)")
    void formatoPorLargo() {
        String largo = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";   // 26 letras validas
        String m = ReglasCheque.errorNroTalonario(largo);
        assertTrue(m.contains("es demasiado largo (26 caracteres)"), m);
        assertTrue(m.contains("de 1 a 25 caracteres"), m);
    }

    @Test
    @DisplayName("formato: el numero de cheque NO promete un largo que el sistema no impone (la regla del legacy no lo limita)")
    void nroChequeNoPrometeLargo() {
        String m = ReglasCheque.errorNroCheque("12A5");
        assertTrue(m.contains("números y guion medio (-)"), m);
        assertFalse(m.contains("caracteres."), "no debe decir 'entre 2 y 25 caracteres': " + m);
        assertFalse(m.contains("2 a 25"), m);
    }

    // ------------------------------------------------------------------ fechas

    @Test
    @DisplayName("fecha de cobro fuera de rango: dice la fecha, el cheque y el rango exacto permitido")
    void cobroFueraDeRango() {
        String m = MensajesCheque.cobroFueraDeRango(d(2026, 12, 31), d(2026, 10, 3));
        assertTrue(m.contains("(31/12/2026)") && m.contains("(03/10/2026)"), m);
        assertTrue(m.contains("28 días antes o después"), m);
        assertTrue(m.contains("entre el 05/09/2026 y el 31/10/2026"), m);
    }

    @Test
    @DisplayName("nueva fecha de cobro fuera de rango: ademas dice a quien pedir una fecha mas lejana")
    void nuevaFechaCobroFueraDeRango() {
        String m = MensajesCheque.nuevaFechaCobroFueraDeRango(d(2027, 1, 15), d(2026, 10, 3));
        assertTrue(m.startsWith("La nueva fecha de cobro (15/01/2027)"), m);
        assertTrue(m.contains("pídele a un administrador"), m);
    }

    // ------------------------------------------------------------------ acciones del detalle (rama K)

    private static String razon(AccionDelDetalle a, boolean cerrado, String k) {
        return MensajesCheque.accionNoHabilitada(a, cerrado, BotonesChequeDto.desde(k));
    }

    @Test
    @DisplayName("accion no habilitada: cada combinacion de la rama K tiene su explicacion")
    void accionNoHabilitadaPorK() {
        assertTrue(razon(AccionDelDetalle.DEVOLVER, true, "0000").contains("está cerrado y ya no admite más acciones"));
        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "0000").contains("todavía no salió de caja"));
        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "1000").contains("está en la oficina, no en cobranza"));
        assertTrue(razon(AccionDelDetalle.CERRAR_SIN_VERIFICACION, false, "1000").contains("«A Custodio»"));

        assertTrue(razon(AccionDelDetalle.FECHA_COBRO, false, "0101").contains("está en cobranza")
                && razon(AccionDelDetalle.FECHA_COBRO, false, "0101").contains("«Devolver»"));
        assertTrue(razon(AccionDelDetalle.CERRAR_CON_VERIFICACION, false, "0101").contains("no tiene una verificación válida"));

        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "0010").contains("ya fue verificado"));
        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "0100").contains("más devoluciones que entregas"));
        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "1111").contains("no está habilitada"), "combinacion desconocida: generico");
    }

    @Test
    @DisplayName("accion no habilitada: el texto empieza diciendo QUE accion no se puede hacer")
    void accionNoHabilitadaNombraLaAccion() {
        assertTrue(razon(AccionDelDetalle.FECHA_COBRO, false, "0000").startsWith("No se puede cambiar la fecha de cobro ahora: "));
        assertTrue(razon(AccionDelDetalle.DEVOLVER, false, "0000").startsWith("No se puede devolver el cheque ahora: "));
        assertTrue(razon(AccionDelDetalle.CERRAR_CON_VERIFICACION, false, "0000").startsWith("No se puede cerrar el cheque con verificación ahora: "));
        assertTrue(razon(AccionDelDetalle.CERRAR_SIN_VERIFICACION, false, "0000").startsWith("No se puede cerrar el cheque sin verificación ahora: "));
    }

    @Test
    @DisplayName("traspasos y acciones: los mensajes traen los numeros reales")
    void conNumeros() {
        assertTrue(MensajesCheque.sinTraspaso(0).contains("todavía no tiene el traspaso"));
        assertTrue(MensajesCheque.sinTraspaso(2).contains("tiene 2 traspasos registrados y debe tener exactamente uno"));
        assertTrue(MensajesCheque.pocasAcciones(2, 3).contains("solo 2 acciones y se necesitan al menos 3"));
        assertTrue(MensajesCheque.pocasAcciones(1, 3).contains("solo 1 acción "));
        assertTrue(MensajesCheque.fechaDeAccionPasada(d(2026, 10, 1), LocalDate.of(2026, 10, 3)).contains("(01/10/2026)")
                && MensajesCheque.fechaDeAccionPasada(d(2026, 10, 1), LocalDate.of(2026, 10, 3)).contains("hoy (03/10/2026)"));
        assertTrue(MensajesCheque.estadoDeAccionNoValido(Arrays.asList("VENCIDO", "ADELANTADO")).contains("VENCIDO o ADELANTADO"));
        assertTrue(MensajesCheque.chequeDuplicado("123", "C001", d(2026, 10, 1)).contains("número (123), cliente (C001)"));
    }

    // ------------------------------------------------------------------ permisos

    @Test
    @DisplayName("sin permiso: dice QUE no puedes hacer, cual boton falta y que hacer")
    void sinPermiso() {
        String m = MensajesCheque.sinPermiso("btnNuevoCH");
        assertEquals("No tienes permiso para registrar cheques. Tu usuario no tiene asignado el botón btnNuevoCH; "
                + "pídele al administrador que te lo asigne.", m);
        assertTrue(MensajesCheque.sinPermiso("btnEliminarSegCH").contains("eliminar acciones del historial"));
        assertTrue(MensajesCheque.sinPermiso("btnRpt3CH").contains("reporte de cheques en custodia"));
        assertTrue(MensajesCheque.sinPermiso("btnEliminarB").contains("eliminar bancos"));
        assertTrue(MensajesCheque.sinPermiso("btnQueNoExiste").contains("hacer esto") && MensajesCheque.sinPermiso("btnQueNoExiste").contains("btnQueNoExiste"));
    }

    @Test
    @DisplayName("exigirBoton: convierte el 403 generico en uno con motivo (SinPermisoException) y deja pasar al que tiene el boton")
    void exigirBotonConMotivo() {
        AccesoModuloHelper acceso = mock(AccesoModuloHelper.class);
        Authentication auth = new UsernamePasswordAuthenticationToken("u", null);

        MensajesCheque.exigirBoton(acceso, auth, 42, "btnNuevoCH");   // con el boton: no tira nada
        verify(acceso).exigirBoton(auth, 42, "btnNuevoCH");

        doThrow(new AccessDeniedException("sin boton")).when(acceso).exigirBoton(any(), anyInt(), anyString());
        SinPermisoException e = assertThrows(SinPermisoException.class,
                () -> MensajesCheque.exigirBoton(acceso, auth, 42, "btnEditar1CH"));
        assertTrue(e.getMessage().contains("editar cheques") && e.getMessage().contains("btnEditar1CH"), e.getMessage());
        assertTrue(e instanceof AccessDeniedException, "sigue siendo un 403");
    }

    // ------------------------------------------------------------------ nada de jerga ni de textos del legacy

    @Test
    @DisplayName("los textos nuevos hablan de tu a tu y no conservan las frases escuetas del legacy")
    void sinFrasesDelLegacy() {
        List<String> todos = Arrays.asList(MensajesCheque.FALTA_NRO_CHEQUE, MensajesCheque.FALTA_CLIENTE, MensajesCheque.FALTA_TALONARIO,
                MensajesCheque.FALTA_RECIBO, MensajesCheque.SUCURSAL_AJENA, MensajesCheque.SOLO_SU_SUCURSAL,
                MensajesCheque.SIN_PENDIENTES_DE_TRASPASO, MensajesCheque.ESTADO_RESERVADO, MensajesCheque.FALTA_NRO_SAP,
                MensajesCheque.chequeNoEncontrado(7), MensajesCheque.accionNoEncontrada(9), MensajesCheque.bancoNoEncontrado(3));
        for (String m : todos) {
            for (String vieja : new String[] {"Verifique", "Ingrese", "Faltan Parametros", "Parametro Incorrecto", "Llenado Requerido",
                    "NO fue Hallado", "Favor", "Selecciono"}) {
                assertFalse(m.contains(vieja), "«" + vieja + "» sigue en: " + m);
            }
            assertTrue(m.endsWith(".") || m.endsWith("»."), "cada mensaje es una frase completa: " + m);
        }
    }

    // ------------------------------------------------------------------ documento PDF del cheque

    @Test
    @DisplayName("PDF demasiado grande: dice el tamano recibido y el limite exactos, con las palabras del legacy ('menos de 2 MegaBytes')")
    void pdfMuyGrande() {
        String m = MensajesCheque.archivoPdfMuyGrande(2_350_000L, 2_010_000L);

        assertTrue(m.contains("2.350.000 bytes"), m);
        assertTrue(m.contains("2,35 MB"), m);
        assertTrue(m.contains("menos de 2 MegaBytes"), m);
        assertTrue(m.contains("máximo 2.010.000 bytes"), m);
        assertTrue(m.contains("Elige un PDF más liviano"), "dice que hacer: " + m);
    }

    @Test
    @DisplayName("PDF que no es PDF: por extension (palabras del legacy), por estar vacio y por contenido; cada uno nombra el archivo y que hacer")
    void pdfQueNoEsPdf() {
        String ext = MensajesCheque.archivoNoEsPdf("foto.png");
        assertTrue(ext.startsWith("Solo se permiten pdf"), ext);
        assertTrue(ext.contains("«foto.png»") && ext.contains(".pdf") && ext.contains("Elige"), ext);

        String vacio = MensajesCheque.archivoPdfVacio("a.pdf");
        assertTrue(vacio.contains("«a.pdf»") && vacio.contains("0 bytes") && vacio.contains("Elige"), vacio);

        String contenido = MensajesCheque.archivoPdfSinContenidoPdf("falso.pdf");
        assertTrue(contenido.contains("«falso.pdf»") && contenido.contains("%PDF-"), contenido);
        assertTrue(contenido.contains("dañado") && contenido.contains("genera el PDF de nuevo"), "dice que hacer: " + contenido);
    }

    @Test
    @DisplayName("carpeta de los PDF: en blanco nombra la propiedad y la variable; no disponible dice la ruta, el motivo y que revisen ruta y permisos")
    void pdfCarpeta() {
        assertTrue(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA.contains("está configurada en blanco en el servidor"));
        assertTrue(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA.contains("cheques.pdf.dir"));
        assertTrue(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA.contains("CHEQUES_PDF_DIR"));
        assertTrue(MensajesCheque.CARPETA_PDF_NO_CONFIGURADA.contains("Avisa a sistemas"));

        String m = MensajesCheque.carpetaPdfNoDisponible("/app/uploads/cheques", "no se pudo crear");
        assertTrue(m.contains("no está disponible en el servidor"), m);
        assertTrue(m.contains("«/app/uploads/cheques»") && m.contains("no se pudo crear"), m);
        assertTrue(m.contains("cheques.pdf.dir") && m.contains("permisos"), m);
        assertFalse(m.contains("no la crea por su cuenta"), "ya se crea sola al subir: " + m);
    }

    @Test
    @DisplayName("PDF sin archivo, sin cheque o con error de disco: cada mensaje dice el cheque y que hacer, y conserva las palabras del legacy donde sirven")
    void pdfOtros() {
        String falta = MensajesCheque.pdfNoEncontrado(18129);
        assertTrue(falta.startsWith("No se encontró el archivo PDF del cheque 18129"), falta);
        assertTrue(falta.contains("Cargar Documento PDF"), falta);

        assertTrue(MensajesCheque.noSePudoGuardarPdf(18129).contains("el documento anterior no se tocó"));
        assertTrue(MensajesCheque.noSePudoGuardarPdf(18129).contains("avisa a sistemas"));
        assertTrue(MensajesCheque.noSePudoLeerPdf(18129).contains("cheque 18129"));

        assertTrue(MensajesCheque.codChequePdfNoValido("../1").contains("«../1»"));
        assertTrue(MensajesCheque.codChequePdfNoValido(null).contains("«»"));
        assertTrue(MensajesCheque.FALTA_ARCHIVO_PDF.contains("Elige el PDF"));
        assertTrue(MensajesCheque.FALTA_CHEQUE_PDF.contains("Elige un cheque"));
    }

    @Test
    @DisplayName("PDF: los textos hablan de tu, son frases completas y no conservan las frases escuetas del legacy")
    void pdfSinFrasesDelLegacy() {
        List<String> todos = Arrays.asList(MensajesCheque.FALTA_CHEQUE_PDF, MensajesCheque.codChequePdfNoValido("x"),
                MensajesCheque.FALTA_ARCHIVO_PDF, MensajesCheque.archivoNoEsPdf("a.png"), MensajesCheque.archivoPdfVacio("a.pdf"),
                MensajesCheque.archivoPdfMuyGrande(3_000_000L, 2_010_000L), MensajesCheque.archivoPdfSinContenidoPdf("a.pdf"),
                MensajesCheque.CARPETA_PDF_NO_CONFIGURADA, MensajesCheque.carpetaPdfNoDisponible("/x", "no se pudo crear"),
                MensajesCheque.pdfNoEncontrado(7), MensajesCheque.noSePudoGuardarPdf(7), MensajesCheque.noSePudoLeerPdf(7));
        for (String m : todos) {
            for (String vieja : new String[] {"Verifique", "Ingrese", "Favor", "No se Almaceno", "Accion Invalida", "Selecciono"}) {
                assertFalse(m.contains(vieja), "«" + vieja + "» sigue en: " + m);
            }
            assertTrue(m.endsWith("."), "cada mensaje es una frase completa: " + m);
        }
    }

    // ------------------------------------------------------------------ los patrones de caracter coinciden con las reglas

    /**
     * El mensaje lista los caracteres que SOBRAN usando un patron de un solo caracter; la decision de validar la toma
     * el patron completo. Si alguien cambia uno y no el otro, el mensaje mentiria: aqui se comprueba, caracter por
     * caracter sobre un alfabeto amplio, que los dos dicen lo mismo.
     */
    @Test
    @DisplayName("el mensaje nombra como 'sobrante' exactamente los caracteres que la regla rechaza (todos los campos)")
    void patronesDeCaracterCoincidenConLaRegla() {
        String alfabeto = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~ñÑáéíóúÁÉÍÓÚüÜ¿¡€";
        Object[][] campos = {
                {ReglasCheque.NRO_CHEQUE, ReglasCheque.NRO_CHEQUE_CAR, "nroCheque"},
                {ReglasCheque.A_ORDEN_DE, ReglasCheque.A_ORDEN_DE_CAR, "aOrdenDe"},
                {ReglasCheque.RECIBO_MANUAL, ReglasCheque.RECIBO_MANUAL_CAR, "recibo"},
                {ReglasCheque.NRO_TALONARIO, ReglasCheque.NRO_TALONARIO_CAR, "talonario"},
                {ReglasCheque.OBSERVACION, ReglasCheque.OBSERVACION_CAR, "observacion"},
                {ReglasCheque.NRO_SAP, ReglasCheque.NRO_SAP_CAR, "sap"},
                {ReglasCheque.NOMBRE_BANCO, ReglasCheque.NOMBRE_BANCO_CAR, "banco"},
        };
        for (Object[] c : campos) {
            Pattern completo = (Pattern) c[0];
            Pattern unCaracter = (Pattern) c[1];
            for (char ch : alfabeto.toCharArray()) {
                // Un valor de 3 caracteres iguales cumple cualquier minimo (1 a 3) y maximo; lo unico que puede fallar es el caracter.
                String valor = "" + ch + ch + ch;
                assertEquals(completo.matcher(valor).matches(), unCaracter.matcher(String.valueOf(ch)).matches(),
                        "campo " + c[2] + ", caracter «" + ch + "» (" + (int) ch + "): el mensaje y la regla no coinciden");
            }
        }
    }
}
