// Destino final: D:\Proyectos\Bosque\Bosque Spring\src\main\java\bo\bosque\com\impexpap\dao\IArqueoRegistro.java
package bo.bosque.com.impexpap.dao;

import bo.bosque.com.impexpap.dto.RegistrarArqueoRequest;
import bo.bosque.com.impexpap.utils.RespuestaSp;

public interface IArqueoRegistro {
    RespuestaSp registrar(RegistrarArqueoRequest req, long codEmpleadoEncargado, long audUsuario);
}
