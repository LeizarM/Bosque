package bo.bosque.com.impexpap.dto;

import lombok.*;

import java.io.Serializable;

/** Filtro generico por identificador. 0 significa "todos". */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FiltroIdDto implements Serializable {

    private long id;
}
