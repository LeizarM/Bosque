package bo.bosque.com.impexpap.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tipos {

    private String codTipos;
    private String nombre;
    private int codGrupo;
    private List<Tipos> listTipos;

    /**
     * Constructores
     */
    public Tipos(String codTipos, String nombre, int codGrupo) {
        this.codTipos = codTipos;
        this.nombre = nombre;
        this.codGrupo = codGrupo;
    }

    public Tipos(String codTipos, String nombre) {
        this.codTipos = codTipos;
        this.nombre = nombre;
    }




    /**
     * =====================================================
     * ================= FUNCIONES Y PROCEDIMIENTOS ========
     * =====================================================
     */

    /**
     * Cargara los Grupos dados = Sexo= 1; EstadoCivil = 2; Nivel Educacion = 3
     * ; Formacion = 4; Parentesco = 5 ; ExContrato-relacion = 6 ; Duracion = 7
     * Seguro = 8 ; extension del Ci = 9 ; ExContrato-ConContrato=10 ;
     * RelEmplEmpr => esActivo , para Descuentos y Estado de los Bancos = 11,
     * garante referencia = 12; Tipos Permisos/Vacaciones = 13 Estado de usuario
     * = 14; Tipo de usuario = 15; Tipos de Licencias = 17 ; Meses = 18 tipoBono
     * = 19; estado bono = 20 ; Tipo de Cheque = 21 ; Tipo acion cheque =22;
     * Tipo estado chek = 23 ; Monedas = 24 ; Tipo Prestamos = 25 ; estado
     * Prestamos = 26; Postergado = 27 ; Tipo de garantia = 28 ; estados
     * Garantia Gobranz = 29 Pedido = 30, Usuario Pedido = 14 ; Tipos Usuarios
     * Pedidos = 31 ; Tipo Personal cobranza = 32 InfoCenter tipo garante = 33 ;
     * InfoCenter documentato = 34 ; Factura Manual Suc Tipo = 35 Factura manual
     * Suc estado = 36 ; Talonario estado = 37; Estado de las autorizaciones =
     * 39 = 40 ; reconciliacion = 41; peridos=42; estadoActividades=43; Posicion cargo estructura organizacional=44,
     * Estado mod Pedidos =45;
     *
     * @param elIdGrupo
     * @return LinkedList
     */
    public List<Tipos> cargarListXGrupo( int elIdGrupo ) {
        List<Tipos> listGrupTipos = new ArrayList<Tipos>();
        for ( Tipos parametroTemp : listTipos ) {
            if ( elIdGrupo == parametroTemp.getCodGrupo() ) {
                listGrupTipos.add( parametroTemp );
            }
        }
        return listGrupTipos;
    }

    /**
     * ====================== PARA EL MODULO DE PRECIOS ==============================
     */
    /**
     * Devolvera una lista de los estados de las propuestas
     * @return
     */
    public List<Tipos> lstEstadoPropuesta() {
        List<Tipos> listaTemp = new ArrayList<Tipos>();
        listaTemp.add(new Tipos("0", "Pendiente", 38));
        listaTemp.add(new Tipos("1", "Aprobada", 38));
        listaTemp.add(new Tipos("2", "No Aprobada", 38));
        //listaTemp.add(new Tipos("3", "En Espera", 38));
        return listaTemp;
    }

    /**
     * Devolvera una lista de tipos de factura
     * @return
     */
    public List<Tipos> lstTipoFactura() {
        List<Tipos> listaTemp = new ArrayList<Tipos>();
        listaTemp.add(new Tipos("1", "Factura Electronica", 60));
        listaTemp.add(new Tipos("2", "Factura Computarizada", 60));
        listaTemp.add(new Tipos("3", "Recibo", 60));

        //listaTemp.add(new Tipos("3", "En Espera", 38));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos de genero
     * @return
     */
    public List<Tipos>lstSexo(){
        List<Tipos>listaTemp = new ArrayList<Tipos>();
        listaTemp.add(new Tipos("F","Femenino",1));
        listaTemp.add(new Tipos("M","Masculino",1));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos de estado civil
     * @return
     */
    public List <Tipos>lstEstadoCivil(){
        List<Tipos>listaTemp = new ArrayList<Tipos>();
        listaTemp.add (new Tipos("sol","soltero(a)",2));
        listaTemp.add (new Tipos("cas","Casado(a)",2));
        listaTemp.add (new Tipos("con","Concubino(a)",2));
        listaTemp.add (new Tipos("div","Divorciado(a)",2));
        listaTemp.add (new Tipos("viu","Viudo(a)",2));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos de ci expedido
     * @return
     */
    public List<Tipos>lstCiExp(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("lp","LP",9));
        listaTemp.add(new Tipos("sc","SC",9));
        listaTemp.add(new Tipos("or","OR",9));
        listaTemp.add(new Tipos("tj","TJ",9));
        listaTemp.add(new Tipos("cbba","CBBA",9));
        listaTemp.add(new Tipos("be","BE",9));
        listaTemp.add(new Tipos("pt","PT",9));
        listaTemp.add(new Tipos("pd","PD",9));
        listaTemp.add(new Tipos("ch","CH",9));
        listaTemp.add(new Tipos("nn","Sin Carnet",9));
        listaTemp.add(new Tipos("ext","Extranjero",9));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos de formacion
     * @return
     */
    public List<Tipos>lstTipoFormacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("cur","Curso",4));
        listaTemp.add(new Tipos("dip","Diplomado",4));
        listaTemp.add(new Tipos("esp","Especializacion",4));
        listaTemp.add(new Tipos("mae","Maestria",4));
        listaTemp.add(new Tipos("doc","Doctorado",4));
        listaTemp.add(new Tipos("licen","Licenciatura",4));
        listaTemp.add(new Tipos("tecSup","Técnico Superior",4));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos de duracionformacion
     * @return
     */
    public List<Tipos>lstTipoDuracionFormacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("hrs","Horas",7));
        listaTemp.add(new Tipos("dia","Dias",7));
        listaTemp.add(new Tipos("mes","Meses",7));
        listaTemp.add(new Tipos("sem","Semanas",7));
        listaTemp.add(new Tipos("ani","Años",7));
        return listaTemp;
    }
    /**
     * Devolvera una lista de garante-referencia
     * @return
     */
    public List<Tipos>lstTipoGarRef(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("gar","Garante",12));
        listaTemp.add(new Tipos("ref","Referencia",12));
        return listaTemp;
    }
    /**
     * Devolvera una lista de tipos parentesco
     * @return
     */
    public List<Tipos>lstTipoDependiente(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("hij","Hijo(a)",5));
        listaTemp.add(new Tipos("pad","Padre",5));
        listaTemp.add(new Tipos("mad","Madre",5));
        listaTemp.add(new Tipos("ben","Beneficiario(a)",5));
        listaTemp.add(new Tipos("cony","Cónyuge",5));
        return listaTemp;
    }
    /**
     * Devolvera una lista para  esActivo SI/NO
     * @return
     */
    public List<Tipos>lstTipoActivo(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("1","SI",10));
        listaTemp.add(new Tipos("0","NO",10));

        return listaTemp;
    }


}
