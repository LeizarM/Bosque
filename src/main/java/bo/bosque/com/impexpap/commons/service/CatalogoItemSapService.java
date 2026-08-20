package bo.bosque.com.impexpap.commons.service;

import bo.bosque.com.impexpap.dao.IItemSap;
import bo.bosque.com.impexpap.model.ItemSap;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * El catalogo de items SAP que se pueden cortar, buscable.
 *
 * <h3>Por que existe</h3>
 * El catalogo tiene ~1.500 items. Mandarlo entero al front cada vez que alguien
 * abria el formulario de una solicitud eran unos 550 KB de JSON —el texto pesa
 * 165 KB, el resto son las 14 claves repetidas 1.500 veces— y el buscador
 * tardaba varios segundos en estar disponible.
 *
 * <p>Ahora el filtrado pasa a este lado: el front manda lo que se escribio y
 * recibe a lo sumo unas decenas de items. La busqueda es sobre una copia en
 * memoria, asi que no golpea la base en cada tecla.
 *
 * <h3>La cache</h3>
 * {@code text_ItemSAP} la refresca un proceso aparte contra SAP, no esta
 * aplicacion, y cambia pocas veces al dia. Diez minutos de TTL es un cambio
 * invisible para quien carga una solicitud y evita releer 1.500 filas por cada
 * tecla escrita.
 *
 * <p><b>Una lista vacia no se cachea.</b> El DAO devuelve lista vacia cuando la
 * consulta falla, no una excepcion; guardarla dejaria el buscador mudo hasta que
 * venciera el TTL.
 */
@Service
public class CatalogoItemSapService {

    /** Diez minutos. */
    private static final long TTL_MS = 10L * 60L * 1000L;

    /** Tope duro de resultados, por si el front pide un limite absurdo. */
    private static final int MAX_RESULTADOS = 200;

    private final IItemSap itemSapDao;

    private volatile List<ItemSap> cache = Collections.emptyList();
    private volatile long cargadoEn = 0L;

    public CatalogoItemSapService(IItemSap itemSapDao) {
        this.itemSapDao = itemSapDao;
    }

    /**
     * Los items cuyo codigo o descripcion contienen {@code texto}.
     *
     * <p>Con el texto vacio devuelve los primeros {@code limite} del catalogo,
     * que ya viene ordenado por descripcion: sirve para que el desplegable no
     * aparezca en blanco al enfocar el campo.
     *
     * @param texto  lo que se escribio; puede venir null o vacio
     * @param limite cuantos devolver como maximo
     * @return los items que coinciden, nunca null
     */
    public List<ItemSap> buscar( String texto, int limite ) {

        int tope = limite <= 0 ? 50 : Math.min( limite, MAX_RESULTADOS );
        List<ItemSap> catalogo = this.catalogo();
        String q = texto == null ? "" : texto.trim().toLowerCase();

        List<ItemSap> encontrados = new ArrayList<>();

        for( ItemSap i : catalogo ){
            if( encontrados.size() >= tope ) break;

            if( q.isEmpty() ){
                encontrados.add( i );
                continue;
            }

            String cod = i.getCodItem() == null ? "" : i.getCodItem().toLowerCase();
            String dato = i.getDatoItem() == null ? "" : i.getDatoItem().toLowerCase();

            if( cod.contains( q ) || dato.contains( q ) ) encontrados.add( i );
        }

        return encontrados;
    }

    /**
     * Cuantos items tiene el catalogo, para que el front pueda decir entre
     * cuantos se esta buscando.
     * @return
     */
    public int total() {
        return this.catalogo().size();
    }

    /**
     * El catalogo, releido cuando vencio el TTL.
     */
    private List<ItemSap> catalogo() {

        long ahora = System.currentTimeMillis();

        if( this.cache.isEmpty() || ahora - this.cargadoEn > TTL_MS ){
            List<ItemSap> frescos = this.itemSapDao.obtenerItemsSap();
            if( !frescos.isEmpty() ){
                this.cache = frescos;
                this.cargadoEn = ahora;
            }
        }

        return this.cache;
    }
}
