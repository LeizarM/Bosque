package bo.bosque.com.impexpap.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formateo de magnitudes para mostrar. Hoy sólo días de vacación/permiso.
 *
 * <p><b>Por qué existe.</b> Los saldos de vacación son {@code float} de SQL Server y salen
 * con basura decimal ({@code 12.500000000000002}). Pintarlos crudos en la app da un número
 * que nadie reconoce como suyo; redondearlos en Dart los deja divergiendo entre plataformas.
 * Se formatean una vez, del lado del servidor, y viajan ya listos en los campos {@code *Txt}.
 *
 * <h3>ATENCIÓN — hay DOS formatos de días conviviendo en el producto</h3>
 * Este ({@code "12,5 días"}) es el que fija el plan de migración §6.3 para la consola de
 * RR.HH. El flujo del EMPLEADO usa otro: {@code dbo.f_formatearDiasYHorasPermiso} convierte a
 * octavos y devuelve {@code "12 días y 4 hrs"} — es lo que hoy ve el empleado en
 * {@code p_list_SolicitudVacacion} (columnas {@code diasDisponiblesTxt} y compañía, que las
 * arma el SP, no Java).
 *
 * <p><b>Consecuencia a mirar en la revisión con RR.HH.:</b> la misma magnitud se va a leer
 * distinta en dos pantallas del mismo sistema, y eso se parece a un bug aunque no lo sea. Si
 * RR.HH. prefiere una sola forma, se cambia SÓLO acá (15 líneas: pasar a octavos de día) y
 * los tres DTO se enteran solos. No se toca el SP.
 */
public final class Fmt {

    private Fmt() { }

    /**
     * Días para mostrar: hasta 2 decimales, coma decimal, sin ceros a la derecha.
     *
     * <pre>
     *   0     -> "0 días"      1     -> "1 día"       1.5  -> "1,5 días"
     *   12.5  -> "12,5 días"   30    -> "30 días"     -16  -> "-16 días"
     *   8.0   -> "8 días"      12.345-> "12,35 días"  -1   -> "-1 día"
     * </pre>
     *
     * <p>Los negativos se muestran con signo y NO se maquillan: un saldo negativo es un dato
     * real del ERP —alguien tomó más días de los que tenía— y esconderlo detrás de un 0
     * borraría justamente el caso que RR.HH. necesita ver.
     */
    public static String dias(double dias) {
        return conUnidad(dias, "día", "días");
    }

    /**
     * Horas para mostrar, con la misma regla que {@link #dias}: {@code 10 -> "10 horas"},
     * {@code 1 -> "1 hora"}, {@code 4.5 -> "4,5 horas"}.
     *
     * <p>Existe porque la nómina de permisos y el modal muestran el MISMO número en las dos
     * unidades ("En Día(s)" / "En Hr(s)") y el redondeo tiene que ser el mismo en las dos: si el
     * cliente formatea una y el servidor la otra, la misma fila dice 1,13 días y 9 horas.
     */
    public static String horas(double horas) {
        return conUnidad(horas, "hora", "horas");
    }

    private static String conUnidad(double valor, String singular, String plural) {
        // BigDecimal.valueOf y no new BigDecimal(double): valueOf usa la representación
        // decimal corta (12.9), el constructor arrastra el ruido binario (12.9000000000000003).
        BigDecimal n = BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        // Java 8 ya devuelve 0 para "0.00".stripTrailingZeros(), pero el caso vale explícito:
        // es el valor más frecuente de toda la pantalla.
        if (n.signum() == 0) {
            return "0 " + plural;
        }

        String numero = n.toPlainString().replace('.', ',');
        return numero + (n.abs().compareTo(BigDecimal.ONE) == 0 ? " " + singular : " " + plural);
    }
}
