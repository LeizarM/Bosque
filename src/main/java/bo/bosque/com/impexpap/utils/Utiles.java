package bo.bosque.com.impexpap.utils;

import lombok.ToString;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@ToString
public class Utiles implements Serializable{


    private static final long serialVersionUID = 1L;
    
    /***********************************************
     *  Constructor por Defecto
     ***********************************************/
    
    public Utiles() {
    }
	
    /***********************************************
     *  Funciones 
     ***********************************************/
    
    
    /*
     * @Funcion  a la que se le ingresa fecha en formato Java y retorna  en formato fecha de SQL
     * @param fecJava
     * @return sqlDate 
     */
    public java.sql.Date fechaJ_a_Sql(java.util.Date fecJava) {
        java.sql.Date sqlDate = null;
        
        if( fecJava != null  ) {
            try {
                sqlDate = new java.sql.Date(fecJava.getTime());
            } catch (Exception e) {
                System.out.println( " Error en la conversion fechaJ_a_Sql = " + e.getMessage());
            }
        }
        return sqlDate;
    }
    
    /*
     * @Funcion  a la que se le ingresa fecha en formato SQL y retorna  en formato fecha de JAVA
     */
    public java.util.Date fechaSQL_a_Java(java.sql.Date fecSql) {
        java.util.Date fecJava = null;
        
        if( fecSql != null  ) {
            try {
                fecJava = new java.util.Date(fecSql.getTime());
            } catch (Exception e) {
                System.out.print( "Error en la conversion fechaSql_a_Java ="+ e.getMessage());
            }
        }
        return fecJava;
    }

    
    /*
     * @Funcion  a la que se le ingresa fecha en formato String  y retorna  en formato fecha de SQL
     */
    public java.sql.Date string_a_fechaSQL(String fecha) {

        java.sql.Date sqlDate = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sqlDate = new java.sql.Date((sdf.parse(fecha)).getTime());
        } catch (Exception e) {
            System.out.print(e.getMessage());
        }

        return sqlDate;
    }

    
    /*
     * @Funcion  a la que se le ingresa fecha en formato String  y retorna  en formato fecha de JAVA
     */
    public java.util.Date string_a_fechaJava(String fecha) {

        java.util.Date javaDate = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            javaDate = new java.util.Date((sdf.parse(fecha)).getTime());
        } catch (Exception e) {
            System.out.print("error en la conversion = " + e.getMessage());
        }

        return javaDate;
    }
    
    /**
     * Función  a la que se le ingresa fecha en formato Java  y retorna  en formato String dd/mm/yyyy
     * @param fecJava
     * @return  String
     */
    public String  fechaJ_a_String(java.util.Date fecJava) {
        String  javaDateString = null;
        try{
            	DateFormat fecha = new SimpleDateFormat("dd/MM/yyyy");
                javaDateString = fecha.format(fecJava);
        } catch (Exception e) {
            System.out.print("Error en la conversion fj_a_s = " + e.getMessage());
        }
        return javaDateString;
    }
    
    /**
     * Función  a la que se le ingresa fecha en formato Java  y retorna  en formato String dd/mm/yyyy - HH:mm
     * @param fecJava
     * @return  String
     */
    public String  fechaJ_a_StringB(java.util.Date fecJava) {
        String  javaDateString = null;
        try{
            	DateFormat fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                javaDateString = fecha.format(fecJava);
        } catch (Exception e) {
            System.out.print("Error en la conversion fjB_a_s = " + e.getMessage());
        }
        
        return javaDateString;
    }
    
    /**
     * Función  a la que se le ingresa fecha en formato Java  y retorna  en formato String pero solo MES/AÑO
     * @param fecJava
     * @return  String
     */
    public String  fechaJ_a_String_MY(java.util.Date fecJava) {
        String  javaDateString = null;
        try{
            	DateFormat fecha = new SimpleDateFormat("MM/yyyy");
                javaDateString = fecha.format(fecJava);
        } catch (Exception e) {
            System.out.print("Error en la conversion fj_a_s_my = " + e.getMessage());
        }
        return javaDateString;
    }
    
    /**
     * Función  a la que se le ingresa fecha en formato SQL  y retorna  en formato String
     * @param fecSql
     * @return  String
     */
    public String  fechaSQL_a_String(java.sql.Date fecSql) {
        String  sqlDateString = null;
        try{
            	DateFormat fecha = new SimpleDateFormat("dd/MM/yyyy");
                sqlDateString = fecha.format(fecSql);
        } catch (Exception e) {
            System.out.print("Error en la conversion fsql_a_s = " + e.getMessage());
        }
        return sqlDateString;
    }
    
    /**
     * Para cambiar de formato java a  TimeStamp
     * @param fecJava
     * @return 
     */
    public java.sql.Timestamp fechaJ_a_SQLTIMESAMP(java.util.Date fecJava) {
        java.sql.Timestamp  varSqlTime = null;
        try{
               varSqlTime = new java.sql.Timestamp( fecJava.getTime() );
        } catch (Exception e) {
            System.out.print("Error en la conversion fechaJ_a_SQLTIMESAMP = " + e.getMessage());
        }
        return varSqlTime;
    }
    
    
    /**
     * Para cambiar de formato fecha TimeStamp a Java
     * @param fecTS
     * @return 
     */
    public java.util.Date fechaSQLTIMESTAMP_a_fechaJ(java.sql.Timestamp fecTS){
        java.util.Date  fecJava = null;
        try{
               fecJava = new java.util.Date( fecTS.getTime() );
        } catch (Exception e) {
            System.out.print("Error en la conversion fechaSQLTIMESTAMP_a_fechaJ = " + e.getMessage());
        }
        return fecJava;
    }
    
    /**
     * Para cambiar de formato String con formato (dd/mm/yyyy)  a java
     * @param stringFecha
     * @return date
     */
    public java.util.Date String_a_fechaJ( String stringFecha  ) {
        SimpleDateFormat formatoDelTexto = new SimpleDateFormat("dd/MM/yyyy");
        java.util.Date fecJava=null;
        try {
            fecJava = formatoDelTexto.parse(stringFecha);
        } catch (ParseException ex) {
            System.out.print("Error en la conversion String_a_fechaJ = " + ex.getMessage() );
        }
        return fecJava;
    }
    
    
    /**
     * Para Completar una cadena con algun valor
     * Caso .- codigo = 8 y deseamos q tenga el formato = 008
     * Si lado = 0 es para aumentar por la derecha, caso contrario lo hara por la izquierda
     * @param lado
     * @param total
     * @param codigo
     * @param caracter
     * @return 
     */
    public String completarCodigo( int lado, int total, int codigo, String caracter)
    {
        String elCodigo = String.valueOf(codigo);
        
        while( elCodigo.length() < total ){
            if(lado==0)
            {
                elCodigo +=caracter;
            }
            else
            {
                elCodigo = caracter+elCodigo;
            }
        }
        return elCodigo;
    }
    
    /**
     * Funcion para redondear los valores de un numero para que tenga el formato o tamaño deseado
     * @param nroVal
     * @param nroDecimales
     * @return 
     */
    public double redondear(double nroVal, int nroDecimales){
        long factor = (long)Math.pow(10,nroDecimales);
        nroVal = nroVal * factor;
        long tempVal = Math.round(nroVal);
        return (double) tempVal / factor;
    }
    
    /**
     * Funcion que Adiciona o resta dias a partir de una fecha  dada
     * @param fecha
     * @param diasARecorrer
     * @return  Fecha en formato Java
     */
    public java.util.Date sumResDias(java.util.Date fecha, int diasARecorrer){
        Calendar fechaCalendar = Calendar.getInstance();
        fechaCalendar.setTime(fecha);
        fechaCalendar.add(Calendar.DAY_OF_YEAR, diasARecorrer);
        java.util.Date  fechaNew = fechaCalendar.getTime();
        
        return fechaNew;
    }
    
    /**
     * Funcion que Adiciona o resta meses a partir de una fecha  dada
     * @param fecha
     * @param mesesARecorrer
     * @return  Fecha en formato Java
     */
    public java.util.Date sumResMeses(java.util.Date fecha, int mesesARecorrer){
        Calendar fechaCalendar = Calendar.getInstance();
        fechaCalendar.setTime(fecha);
        fechaCalendar.add(Calendar.MONTH, mesesARecorrer );
        java.util.Date  fechaNew = fechaCalendar.getTime();
        
        return fechaNew;
    }
    
    /**
     * Funcion que genera una fecha en base a los datos enviados 
     * @param anio
     * @param mes
     * @param dia
     * @return  Fecha en formato Java
     */
    public java.util.Date nuevaFecha( int anio, int mes , int dia ){
        Calendar fechaCalendar = Calendar.getInstance();
        fechaCalendar.set(anio+1900, mes, dia);
        java.util.Date  fechaNew = fechaCalendar.getTime();
        
        return fechaNew;
    }
    
    /**
     * Funcion que genera una fecha en base a los datos enviados 
     * @param fecha
     * @return  Fecha en formato Java
     */
    public java.util.Date fechaUltimoDiaMes( java.util.Date fecha ){
        fecha = this.sumResMeses(fecha, 1);
        fecha = this.nuevaFecha(fecha.getYear(), fecha.getMonth(), 1);
        fecha = this.sumResDias(fecha, -1);
        return fecha;
    }
    
    
    /**
     * Función  a la que se le ingresa un numero en formato INT  y retorna  en formato String
     * @param numero
     * @return  String
     */
    public String  int_a_String(int numero) {
        String  sqlIntString = null;
        try{
            sqlIntString = String.valueOf(numero);
        } catch (Exception e) {
            System.out.print("Error en la conversion fint_a_s = " + e.getMessage());
        }
        return sqlIntString;
    }
    
    /**
     * Función  a la que se le ingresa un numero en formato INT  y retorna  en formato String
     * @param numero
     * @return  String
     */
    public int String_a_Int( String  numero) {
        int sqlStringInt = 0;
        try{
            sqlStringInt = Integer.valueOf( numero) ;
        } catch (Exception e) {
            sqlStringInt = 0;
            System.out.print("Error en la conversion s_a_i = " + e.getMessage());
        }
        return sqlStringInt;
    }
    /**
     * Procedimiento para generar tokens por usuario y una llave (considerar enviar el password)
     * @param usuario
     * @param key
     * @return 
     */
    public String genJWTToken(String usuario, String key) {
        long tiempo = System.currentTimeMillis();
        String token = Jwts.builder()
                    .setId("ipxJwt")//le agregamos un id en comun a los tokens
                    .signWith(SignatureAlgorithm.HS512, key.getBytes())//generamos con que algoritmo deseamos encriptar
                    .setSubject("impexpap "+usuario)//procedencia del token
                    .setIssuedAt(new Date(tiempo))//cuando fue creado el token
                    .setExpiration(new Date(tiempo + 900000))//tiempo limite de expiracion del token, en este caso 15 min
                    .claim("bosque", "ipx")//firmas adicionales
                    .compact();
        return token;
    }
    
}
