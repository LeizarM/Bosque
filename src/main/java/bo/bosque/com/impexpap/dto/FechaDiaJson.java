package bo.bosque.com.impexpap.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

/**
 * Un {@link java.time.LocalDate} que viaja como {@code yyyy-MM-dd}: un dia, sin hora ni zona.
 *
 * <p>El serializador y el deserializador se indican <b>en el campo</b> y no se confia en que el
 * {@code ObjectMapper} tenga registrado el modulo de {@code java.time}: {@code JacksonConfig} arma el suyo con
 * {@code builder.modules(...)}, que apaga el descubrimiento automatico de modulos. Asi la fecha sale igual con el
 * mapper de la aplicacion y con uno pelado (las pruebas), y nunca como el arreglo {@code [2026,10,3]}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@JacksonAnnotationsInside
@JsonFormat(pattern = "yyyy-MM-dd")
@JsonSerialize(using = LocalDateSerializer.class)
@JsonDeserialize(using = LocalDateDeserializer.class)
public @interface FechaDiaJson {
}
