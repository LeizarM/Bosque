package bo.bosque.com.impexpap.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    /**
     * Formatos de fecha aceptados desde el frontend.
     * Se prueban en orden; el primero que parsee correctamente gana.
     */
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",  // "2026-03-19 00:00:00"  (con hora)
            "yyyy-MM-dd",           // "2026-03-19"            (solo fecha)
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",  // ISO 8601 con zona
            "yyyy-MM-dd'T'HH:mm:ss"        // ISO 8601 sin zona
    };

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonDateCustomizer() {
        return builder -> {
            // Serialización de respuestas: siempre con hora
            builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            builder.timeZone(TimeZone.getTimeZone("America/La_Paz"));

            // Deserialización: acepta cualquiera de los formatos definidos arriba
            SimpleModule module = new SimpleModule("FlexibleDateModule");
            module.addDeserializer(Date.class, new FlexibleDateDeserializer());
            builder.modules(module);
        };
    }

    /**
     * Deserializador de Date que acepta múltiples formatos de fecha.
     * Evita el error "Unparseable date" cuando el frontend envía fechas
     * con o sin componente de hora.
     */
    static class FlexibleDateDeserializer extends StdDeserializer<Date> {

        FlexibleDateDeserializer() {
            super(Date.class);
        }

        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();

            // Si es un número (epoch ms), lo manejamos directamente
            try {
                return new Date(Long.parseLong(text));
            } catch (NumberFormatException ignored) {
                // no es epoch, continuar con formatos string
            }

            for (String format : DATE_FORMATS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setLenient(false);
                    sdf.setTimeZone(TimeZone.getTimeZone("America/La_Paz"));
                    return sdf.parse(text);
                } catch (ParseException ignored) {
                    // probar el siguiente formato
                }
            }

            throw ctxt.weirdStringException(text, Date.class,
                    "No se pudo parsear la fecha '" + text + "'. Formatos aceptados: " +
                    "\"yyyy-MM-dd HH:mm:ss\" o \"yyyy-MM-dd\"");
        }
    }
}


