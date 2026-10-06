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
     * ================= PARA EL MODULO DE CHEQUES (tch) =================
     *
     * Copia de {@code v_tipos} grupos 21 a 24 (la vista esta hecha con literales dentro del CREATE
     * VIEW) y de los combos por boton de {@code Tipos.java} de Bosque v2. Se conservan los codigos
     * TAL COMO los escribe el legacy: la moneda se guarda {@code BS} / {@code SUS} (la vista dice
     * {@code Bs}; el motor compara sin distinguir mayusculas).
     *
     * <p>El grupo 22 de la VISTA tiene 12 estados; el combo de edicion del legacy
     * ({@code listChkAccsCompleto}) agrega {@code VER}. Aqui van los 13, que es lo que hace falta para
     * poner nombre a cualquier fila del historial.
     */
    public List<Tipos> lstTipoCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("PAG", "PAGO", 21));
        l.add(new Tipos("RES", "RESPALDO", 21));
        return l;
    }

    public List<Tipos> lstMonedaCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("BS", "Bs", 24));
        l.add(new Tipos("SUS", "$us", 24));
        return l;
    }

    public List<Tipos> lstEstadoCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("PEN", "PENDIENTE", 23));
        l.add(new Tipos("CER", "CERRADO", 23));
        return l;
    }

    /** Todos los estados de accion de un cheque (grupo 22 mas VER). */
    public List<Tipos> lstEstadoAccionCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("REC", "RECIBIDO", 22));
        l.add(new Tipos("TRASP", "TRASPASO", 22));
        l.add(new Tipos("CUS", "A COBRANZA", 22));
        l.add(new Tipos("DEV", "DEVUELTO", 22));
        l.add(new Tipos("COB", "COBRADO", 22));
        l.add(new Tipos("VEN", "VENCIDO - POSTERGADO", 22));
        l.add(new Tipos("DPB", "DEPOSITADO BANCO", 22));
        l.add(new Tipos("DPR", "DEPOSITADO - RECHAZADO", 22));
        l.add(new Tipos("CEF", "CANJEADO EFECTIVO", 22));
        l.add(new Tipos("CCH", "CANJEADO CHEQUE", 22));
        l.add(new Tipos("ADE", "ADELANTADO", 22));
        l.add(new Tipos("PAP", "PAGO PARCIAL", 22));
        l.add(new Tipos("VER", "VERIFICADO", 22));
        return l;
    }

    /** Boton "Fecha Cobro": {@code listChkAccsFechaCobro}. */
    public List<Tipos> lstAccionFechaCobroCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("VEN", "VENCIDO - POSTERGADO", 22));
        l.add(new Tipos("ADE", "ADELANTADO", 22));
        return l;
    }

    /** Boton "Cerrar con verificacion": {@code listChkAccsCierre}. */
    public List<Tipos> lstAccionCierreConVerificacionCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("COB", "COBRADO", 22));
        return l;
    }

    /** Boton "Cerrar sin verificacion": {@code listChkAccsCierreOtros}. */
    public List<Tipos> lstAccionCierreSinVerificacionCheque() {
        List<Tipos> l = new ArrayList<>();
        l.add(new Tipos("CEF", "CANJEADO EFECTIVO", 22));
        l.add(new Tipos("CCH", "CANJEADO CHEQUE", 22));
        l.add(new Tipos("PAP", "PAGO PARCIAL", 22));
        l.add(new Tipos("DPR", "DEPOSITADO BANCO - RECHAZADO", 22));
        return l;
    }

    /**
     * ================= PARA EL MODULO DE GARANTIAS DE COBRANZA (tcbr) =================
     *
     * Copia literal de v_tipos grupos 28 y 29 (la vista esta hecha con literales dentro del
     * CREATE VIEW). Los procedimientos p_abm_CbrDetalle y p_abm_AccionCbr validan el codigo
     * contra v_tipos: si aqui aparece un codigo que la vista no tiene, el alta devuelve un
     * error de negocio claro, no se graba basura. Mantener las dos fuentes iguales.
     */
    /**
     * Tipos de garantia (v_tipos grupo 28): lo que respalda cada detalle.
     */
    public List<Tipos> lstTipoGarantiaCbr() {
        List<Tipos> listaTemp = new ArrayList<Tipos>();
        listaTemp.add(new Tipos("CDS", "CONTRATO DE SUMINISTRO", 28));
        listaTemp.add(new Tipos("FMI", "FORMULARIO INTERNO", 28));
        listaTemp.add(new Tipos("INM", "INMUEBLE", 28));
        listaTemp.add(new Tipos("LDC", "LETRA DE CAMBIO", 28));
        listaTemp.add(new Tipos("MAQ", "MAQUINARIA", 28));
        listaTemp.add(new Tipos("PAG", "PAGARE", 28));
        listaTemp.add(new Tipos("QRG", "QUIROGRAFARIO", 28));
        listaTemp.add(new Tipos("RDD", "RECONOCIMIENTO DE DEUDA", 28));
        listaTemp.add(new Tipos("VEH", "VEHICULO", 28));
        return listaTemp;
    }

    /**
     * Estados de las acciones de una garantia (v_tipos grupo 29). Es el catalogo completo,
     * para mostrar etiquetas: a mano solo se registran NOT y CER (REG y TRASP los crean los
     * procedimientos, EXT la extension).
     */
    public List<Tipos> lstEstadoAccionCbr() {
        List<Tipos> listaTemp = new ArrayList<Tipos>();
        listaTemp.add(new Tipos("REG", "REGISTRADO BOSQUE", 29));
        listaTemp.add(new Tipos("TRASP", "TRASPASO", 29));
        listaTemp.add(new Tipos("EXT", "EXTENSION", 29));
        listaTemp.add(new Tipos("NOT", "NOTA", 29));
        listaTemp.add(new Tipos("CER", "CERRADO", 29));
        return listaTemp;
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
    /***************************MODULO EMPLEADOS RRHH**********************/
    /**
     * Devolvera una lista para  esActivo SI/NO
     * @return
     */
    public List<Tipos>lstEducacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("nin","Ninguno",3));
        listaTemp.add(new Tipos("pri","Primaria",3));
        listaTemp.add(new Tipos("sec","Secundaria",3));
        listaTemp.add(new Tipos("bac","Bachiller",3));
        listaTemp.add(new Tipos("tmed","Técnico Medio",3));
        listaTemp.add(new Tipos("tsup","Técnico Superior",3));
        listaTemp.add(new Tipos("uni","Universitario",3));
        listaTemp.add(new Tipos("egr","Universitario/Egresado",3));
        listaTemp.add(new Tipos("lic","Licenciatura",3));

        return listaTemp;
    }
    /**
     * Devolvera una lista para  tipo de relacion
     * @return
     */
    public List<Tipos>lstTipoRelacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("inde","Indefinido",6));
        listaTemp.add(new Tipos("pFijo","Plazo Fijo",6));
        listaTemp.add(new Tipos("pasa","Pasante",6));

        return listaTemp;
    }
    /**
     * Devolvera una lista para  tipo de licencia
     * @return
     */
    public List<Tipos>listTipoLicencia(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("M","M - Motociclista;  Motocicletas, triciclos y cuadriciclos (cuadra tracks)",17));
        listaTemp.add(new Tipos("P","P - Particular ;  Automóviles, camionetas, jeeps y vagonetas de uso particular, hasta 7 ocupantes ",17));
        listaTemp.add(new Tipos("A","A - Profesional ; Incluye Cat. P. Vehiculos de carga con capacidad de hasta de 2 1/2 toneladas.",17));
        listaTemp.add(new Tipos("B","B - Profesional ; Incluye Cat. Prof. A. Vehículos de carga con capacidad de hasta 6 toneladas. Hasta 22 pasajeros",17));
        listaTemp.add(new Tipos("C","C - Profesional ; Incluye Cat. Prof. B. Vehiculos de carga superior a 6 toneladas, con y sin acople, volquetas y cisternas. Sup. a 22 pasajeros",17));
        listaTemp.add(new Tipos("CI","C Indefinido - Profesional ; Incluye Cat. P, Profesionales A, B y C.",17));
        listaTemp.add(new Tipos("T","T - Motorista ; Maquinaria motorizada pesada, como montacargas, tractores, moto-niveladoras, retro-excavadoras, grúas y otras similares",17));
        return listaTemp;
    }
    /**
     * Devolvera una lista para  tipo de seguro
     * @return
     */
    public List<Tipos>listTipoSeguro(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("acc","Accidentes",8));
        listaTemp.add(new Tipos("sal","Salud",8));
        return listaTemp;
    }
    /**
     * Devolvera una lista para tipo renovacion chip tigo
     * @return
     */
    public List<Tipos>listTipoRenovacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("PERD","PERDIDA EQUIPO",62));
        listaTemp.add(new Tipos("CDET","CHIP DETERIORADO",62));
        listaTemp.add(new Tipos("CSIM","CAMBIO eSIM",62));
        listaTemp.add(new Tipos("SP","SALIDA PERSONAL",62));
        listaTemp.add(new Tipos("RL","RECUPERACION LINEA",62));
        return listaTemp;
    }
    /**
     * tipos asignacion anticipos
     * @return
     */
    public List<Tipos>listTipoAsignacion(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("ASIG","ASIGNADO",63));
        listaTemp.add(new Tipos("NOASIG","NO ASIGNADO",63));
        listaTemp.add(new Tipos("ANUL","ANULADO",63));
        return listaTemp;
    }
    /**
     * Devolvera una lista para tipos de permiso-vacacion
     * @return
     */
    public List<Tipos>listTipoPermiso(){
        List<Tipos>listaTemp= new ArrayList<Tipos>();
        listaTemp.add(new Tipos("vac","Vacación",13));
        listaTemp.add(new Tipos("baja","Baja por Enfermedad",13));
        listaTemp.add(new Tipos("sinsuel","Sin Sueldo",13));
        listaTemp.add(new Tipos("libre","Día Libre",13));
        listaTemp.add(new Tipos("def","Por Defunción",13));
        //listaTemp.add(new Tipos("pva","Pago Vacación",13));
        listaTemp.add(new Tipos("clb","Comision Laboral",13));
        listaTemp.add(new Tipos("pcr","Permisos con reposicion",13));
        listaTemp.add(new Tipos("otro","Otros",13));
        return listaTemp;
    }



}
