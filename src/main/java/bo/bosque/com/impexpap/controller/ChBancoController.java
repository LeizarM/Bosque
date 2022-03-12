package bo.bosque.com.impexpap.controller;

import bo.bosque.com.impexpap.dao.IChBanco;
import bo.bosque.com.impexpap.model.ChBanco;

import bo.bosque.com.impexpap.security.jwt.JwtProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/banco")
public class ChBancoController {

	@Autowired()
    private IChBanco chBdao;

	
	/**
     * 
     * @return List
     */
    @Secured({ "ROLE_ADM", "ROLE_LIM" }) //que un usuario admin o limitado si tiene acceso para consumir este recurso
    @PostMapping("/bancosX")
    public List<ChBanco> listadoX( ) {
        return this.chBdao.listBancos();
    }
    
}
