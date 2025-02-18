// En utils/ApiResponse.java (archivo existente en estructura)
package bo.bosque.com.impexpap.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {

    private String message;
    private T data;
    private int status;

}