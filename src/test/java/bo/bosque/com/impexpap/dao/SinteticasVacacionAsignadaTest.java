package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.model.VacacionAsignada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las filas «Sin registrar» que el SP inventa de más.
 *
 * <p>El caso está tomado de la base, no armado a mano: <b>empleado 213, relación 457</b>, beneficio
 * desde el 01/06/2022, con tres asignaciones reales de 15 días fechadas 31/05/2023, 31/05/2024 y
 * 31/05/2025. La ACCION 'B' de {@code p_list_vacacionAsignada} prueba los aniversarios 01/06/2022,
 * 01/06/2023, 01/06/2024 y 01/06/2025 con igualdad exacta de fecha, no acierta ninguno, y emite
 * cuatro sintéticas: tres pegadas a filas que sí existen y una legítima.
 *
 * <p>Lo que se mide es que sobrevivan exactamente las legítimas. Si esto se rompe hay dos formas de
 * romperlo y las dos son caras: dejar las falsas le dice a RR.HH. que no se asignó nada en años que
 * sí se asignaron, y comerse la verdadera esconde el único punto de alta de la pantalla.
 */
class SinteticasVacacionAsignadaTest {

    @Test
    @DisplayName("saca la sintética cuando el año ya tiene su fila real, aunque la fecha no coincida")
    void sacaLasFalsasYDejaLaVerdadera() {
        List<VacacionAsignada> filas = new ArrayList<>();
        filas.add(sintetica("2022-06-01"));
        filas.add(real(1317, "2023-05-31"));
        filas.add(sintetica("2023-06-01"));
        filas.add(real(1394, "2024-05-31"));
        filas.add(sintetica("2024-06-01"));
        filas.add(real(1493, "2025-05-31"));
        filas.add(sintetica("2025-06-01"));   // este año NO tiene asignación: es un hueco real

        VacacionAsignadaDao.quitarSinteticasYaRegistradas(filas);

        assertThat(fechas(filas)).containsExactly(
                "2023-05-31", "2024-05-31", "2025-05-31", "2025-06-01");
    }

    @Test
    @DisplayName("sin ninguna fila real no borra nada: son todos huecos de verdad")
    void empleadoNuevoConservaSusHuecos() {
        List<VacacionAsignada> filas = new ArrayList<>();
        filas.add(sintetica("2024-06-01"));
        filas.add(sintetica("2025-06-01"));

        VacacionAsignadaDao.quitarSinteticasYaRegistradas(filas);

        assertThat(fechas(filas)).containsExactly("2024-06-01", "2025-06-01");
    }

    @Test
    @DisplayName("una fila real tapa un solo aniversario, no dos")
    void laVentanaEsAbiertaALaIzquierda() {
        // Fecha rara pero posible: 74 de las 1.424 filas de la tabla caen en día 1. Si la ventana
        // fuera cerrada de los dos lados, esta real pertenecería a los años que abren el 01/06/2023
        // y el 01/06/2024 a la vez, y se llevaría puesto un hueco que nadie cargó.
        List<VacacionAsignada> filas = new ArrayList<>();
        filas.add(sintetica("2023-06-01"));
        filas.add(real(9001, "2024-06-01"));
        filas.add(sintetica("2024-06-01"));

        VacacionAsignadaDao.quitarSinteticasYaRegistradas(filas);

        assertThat(fechas(filas)).containsExactly("2024-06-01", "2024-06-01");
    }

    // ── fixture ───────────────────────────────────────────────────────────────

    private static VacacionAsignada real(long cod, String iso) {
        VacacionAsignada v = new VacacionAsignada();
        v.setCodVacacionAsignada(cod);
        v.setFecha(fecha(iso));
        v.setDiasAsignados(15);
        return v;
    }

    /** Lo que inventa el SP: {@code codVacacionAsignada = 0} y cero días. */
    private static VacacionAsignada sintetica(String iso) {
        VacacionAsignada v = new VacacionAsignada();
        v.setCodVacacionAsignada(0);
        v.setFecha(fecha(iso));
        return v;
    }

    private static Date fecha(String iso) {
        String[] p = iso.split("-");
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        return c.getTime();
    }

    private static List<String> fechas(List<VacacionAsignada> filas) {
        List<String> out = new ArrayList<>();
        for (VacacionAsignada v : filas) {
            Calendar c = Calendar.getInstance();
            c.setTime(v.getFecha());
            out.add(String.format("%04d-%02d-%02d",
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)));
        }
        return out;
    }
}
