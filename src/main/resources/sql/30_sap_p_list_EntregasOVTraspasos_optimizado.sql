/* =============================================================================================
   30_sap_p_list_EntregasOVTraspasos_optimizado.sql
   [CONEXION].[dbo].[p_list_EntregasOVTraspasos]   ·   reescritura ACCION 'A'
   ---------------------------------------------------------------------------------------------
   >>> OJO: ESTE SCRIPT **NO** SE CORRE EN [BOSQUE-2_0]. <<<
   >>> Este SP vive en el SERVIDOR DE SAP: SRV_2022 (192.168.3.x), base [CONEXION].
   >>> Es el mismo servidor al que BOSQUE-2_0 llega por el linked server SRV_2022.
   >>> Conectate a SRV_2022 con SSMS, poné el contexto en la base CONEXION y corré el ALTER.
   >>> Hace falta una cuenta con ALTER PROCEDURE sobre [CONEXION].[dbo].
   >>> NO se aplico nada: la cuenta con la que se investigo es de solo lectura.

   Objetivo:
     Bajar el costo de la sincronizacion con SAP que se dispara en CADA refresh de CADA chofer.
     El camino es:  POST /entregas/chofer-entrega -> EntregaChoferDao.listarEntregasXEmpleado
     -> p_list_trch_Entregas @ACCION='A' -> p_abm_trch_Entregas @ACCION='A'
     -> OPENQUERY(SRV_2022, 'EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION=''A''')
     Ese ultimo paso es el que cuesta 1.4 s y es lo unico que toca este script.

     NO cambia ni una fila del resultado: mismas columnas, mismo orden, mismos tipos, mismas
     417 filas. Verificado contra el servidor real, bloque por bloque (ver seccion EQUIVALENCIA).

   ---------------------------------------------------------------------------------------------
   RESULTADO MEDIDO (SRV_2022, con transporte real de filas, min de 5 corridas en caliente)
   ---------------------------------------------------------------------------------------------
     bloque              filas    HOY     NUEVO
     FAC  IMPEXPAP          73    260 ms     35 ms
     FAC  ESPPAPEL         168    277 ms     45 ms
     FAC  PAPIRUS            0      6 ms      8 ms
     FAC  PRODUCTIVA       107     88 ms     15 ms
     OV   (4 empresas)       6     19 ms     19 ms
     TRA  (4 empresas)      63     31 ms     32 ms
     ------------------------------------------------
     TOTAL                 417    681 ms    154 ms     (-77%)

     Los 681 ms son la suma de los 12 bloques replicados con fecha literal. El SP REAL de hoy
     tarda 1413 ms (promedio de 43 ejecuciones en sys.dm_exec_query_stats) porque ademas carga
     la penalidad de @fechaCorte como variable local, que esta reescritura elimina (punto 7).
     O sea que contra el SP real la mejora es de ~1413 ms a ~155 ms.

     Con @codEmpleado pasado (filtrando por un chofer del lado de SAP): 121 ms y 62 filas.

   =============================================================================================
   QUE SE CAMBIO Y POR QUE  (cada punto con su medicion)
   =============================================================================================

   1) [@CHOFERES] : subconsulta correlacionada x2  ->  INNER JOIN
      Hoy la misma subconsulta se evalua dos veces por fila (una en el SELECT para devolver
      U_codEmpleado y otra en el WHERE para el IS NOT NULL). Ahora es un JOIN y se resuelve
      una sola vez.
      El INNER JOIN hace innecesario el 'IS NOT NULL', y esto es DEMOSTRABLE POR ESQUEMA, no
      por datos: [@CHOFERES].U_codEmpleado es nvarchar(25) **NOT NULL** en las 4 empresas.
      Entonces la subconsulta solo podia dar NULL cuando no habia fila con ese Code, que es
      exactamente lo que descarta el INNER JOIN.
      MEDICION: por si solo NO acelera nada (Code es PK clustered unique sobre 22 filas;
      sacar la subconsulta del SELECT daba 266->264 ms). Se hace porque habilita el resto y
      porque elimina una conversion implicita (ver punto 4), no por velocidad propia.

   2) dbo.fn_TelefonoContacto (UDF ESCALAR)  ->  logica en linea
      Es LA causa del costo. El plan cacheado del servidor lo dice literalmente:
      NonParallelPlanReason="TSQLUserDefinedFunctionsNotParallelizable", y sys.sql_modules
      reporta is_inlineable = False (ni SQL 2022 puede inlinearla con Froid). Como el SP es UN
      SOLO statement, la UDF aparece en las 12 ramas y serializa el UNION COMPLETO: el SP corre
      a DOP 1 con 1364 ms de CPU en un servidor de 48 nucleos con MAXDOP 8.
      MEDICION: FAC IMPEXPAP con UDF 274 ms / sin UDF 56 ms. Y la prueba de que es perdida de
      paralelismo y no costo por fila: sin UDF pero con MAXDOP 1 da 209 ms, o sea reproduce
      casi toda la penalidad.
      El cuerpo de la UDF es simple y se pudo poner en linea completo (no hubo que dejarla).
      Definicion original obtenida con OBJECT_DEFINITION:
          p   = LTRIM(RTRIM(ISNULL(@Phone1,'')))
          c   = LTRIM(RTRIM(ISNULL(@Cellular,'')))
          seg = si p tiene un espacio, lo que va despues del primer espacio; si no, ''
          si LEFT(p,1) IN ('2','3','4'):
                si c <> ''                 -> devuelve c
                si LEFT(seg,1) IN ('6','7')-> devuelve seg
          devuelve p
      La version en linea usa dos CROSS APPLY (VALUES ...) para calcular p / cel / seg UNA sola
      vez en vez de repetir los LTRIM(RTRIM(ISNULL(...))) seis veces por rama.
      *** DETALLE DE CORRECCION QUE NO ES OBVIO ***
      El original es una SUBCONSULTA ESCALAR: si el CardCode no existe en OCRD devuelve NULL.
      Un LEFT JOIN pelado devolveria '' (cadena vacia), porque ISNULL(c.Phone1,'') sobre una
      fila NULL-extendida da ''. Por eso el CASE arranca con
          WHEN c.CardCode IS NULL THEN NULL
      Sin ese guard la columna telefono cambiaria de NULL a '' y la equivalencia se rompe.

   3) 'vendedor' (OSLP) : subconsulta correlacionada por fila  ->  LEFT JOIN
      OSLP.SlpCode es PK, asi que el LEFT JOIN no puede multiplicar filas y conserva el NULL
      cuando no hay vendedor. Mismo tratamiento para los dos subselect a OWHS de los Traspasos
      (WhsCode es PK) y para OCRD (CardCode es PK).

   4) 'valido' (ORIN/RIN1)  ->  LEFT JOIN contra un conjunto DISTINCT precalculado
      *** ACA ME APARTO DEL PEDIDO, Y LO HAGO CON NUMEROS ***
      El pedido decia "pasalo a EXISTS, que corta al primer match en vez de contar todo".
      LO PROBE Y ES MUCHISIMO PEOR. Un EXISTS correlacionado metido en un CASE de la LISTA DEL
      SELECT no se decorrelaciona: SQL Server lo ejecuta UNA VEZ POR FILA DE SALIDA, rescaneando
      ORIN join RIN1 cada vez.
          FAC IMPEXPAP  valido como EXISTS en el SELECT : 1193 ms
          FAC IMPEXPAP  valido como LEFT JOIN de conjunto:   33 ms
          FAC ESPPAPEL  valido como EXISTS en el SELECT : 1461 ms
          FAC ESPPAPEL  valido como LEFT JOIN de conjunto:   43 ms
      El EXISTS es la herramienta correcta en un WHERE (ahi si se convierte en semi-join); en
      una proyeccion no. Lo que si funciona es materializar el conjunto una sola vez:
          LEFT JOIN (SELECT DISTINCT CONVERT(int, tb.BaseRef) AS DocNumRef
                     FROM <emp>.dbo.ORIN ta JOIN <emp>.dbo.RIN1 tb ON ta.DocEntry = tb.DocEntry) nc
                 ON nc.DocNumRef = t0.DocNum
          ... CASE WHEN nc.DocNumRef IS NOT NULL THEN 'A' ELSE 'V' END
      El DISTINCT ahi es obligatorio: sin el, un DocNum referenciado por dos lineas de nota de
      credito duplicaria la fila de salida. Con DISTINCT el LEFT JOIN aporta 0 o 1 fila.
      Semantica identica al original: hoy es "0 < COUNT(tb.BaseRef)", o sea "existe al menos
      una fila con ese BaseRef". COUNT() ignora los NULL y 'BaseRef = DocNum' tampoco matchea
      NULL, asi que "existe al menos una" es exactamente lo mismo.

      *** POR QUE CONVERT(int, BaseRef) Y NO CAST(DocNum AS NVARCHAR) ***
      Es tentador dar vuelta la comparacion para hacerla sargable
      (tb.BaseRef = CAST(t0.DocNum AS NVARCHAR(32))), pero NO ES EQUIVALENTE y ademas es mas
      lento aca. RIN1.BaseRef es nvarchar(16) y hoy se compara contra un int, asi que SQL
      convierte la COLUMNA a int. Medido en PAPIRUS: 1169 filas de RIN1 tienen BaseRef = ''
      (cadena vacia), y CONVERT(int,'') = 0, mientras que '' nunca es igual a un DocNum como
      texto. Comparar como texto tambien cambiaria el resultado ante cualquier BaseRef con
      ceros a la izquierda. Se conserva la conversion a int, que es lo que hace hoy el motor.
      Y ademas midio mejor: 33 ms (int) vs 46 ms (texto) en FAC IMPEXPAP.
      RIESGO PREEXISTENTE (no lo introduce este cambio, ya estaba): si alguna fila de RIN1
      llegara con texto no numerico, la conversion revienta en runtime. HOY NO PASA: se
      verifico que TRY_CONVERT(int, BaseRef) IS NULL da 0 filas en las 4 empresas.

   5) INV1 : de JOIN a EXISTS  —  ESTE ES EL SEGUNDO GRAN AHORRO Y NO ESTABA EN EL DIAGNOSTICO
      Los bloques de Factura hacen  FROM OINV t0 JOIN INV1 t1 ON t0.DocEntry = t1.DocEntry
      y despues enganchan la derivada EN con  EN.BaseEntry = t1.DocEntry.
      Pero t1.DocEntry ES t0.DocEntry (lo dice el propio ON), y del alias t1 NO SE PROYECTA NI
      UNA COLUMNA. O sea que INV1 no aporta datos: lo unico que hace es MULTIPLICAR cada fila
      por la cantidad de lineas que tenga la factura, para que despues el SELECT DISTINCT las
      vuelva a colapsar. Ese es el origen de los 225 duplicados que el DISTINCT tiene que
      limpiar (642 filas antes del DISTINCT, 417 despues).
      Se reemplaza por un semi-join, que es equivalente EXACTO (proyectar cero columnas de una
      tabla + DISTINCT == EXISTS) y no genera el fan-out:
          FROM <emp>.dbo.OINV t0
          JOIN (...) EN ON EN.BaseEntry = t0.DocEntry AND ...
          WHERE EXISTS (SELECT 1 FROM <emp>.dbo.INV1 t1 WHERE t1.DocEntry = t0.DocEntry)
      NO se puede simplemente BORRAR INV1: el EXISTS filtra de verdad. Medido: PAPIRUS tiene
      2 facturas en OINV sin ninguna linea en INV1. Son 0 filas hoy en la ventana de 30 dias,
      pero borrar el EXISTS cambiaria el resultado el dia que una de esas caiga en ventana.

   6) DISTINCT : SE MANTIENEN TODOS. Explicacion, porque el pedido decia sacar los que sobren.
      (a) El DISTINCT externo de Factura HACE FALTA: sin el son 642 filas en vez de 417
          (IPX 73->89, ESP 168->325, PROD 107->159). Con el punto 5 el fan-out desaparece en
          origen, pero el DISTINCT se deja igual como garantia barata de equivalencia.
      (b) El DISTINCT interno de la derivada EN HACE FALTA: proyecta lineas de DLN1 sin incluir
          LineNum, asi que dos lineas del mismo remito con mismo item/almacen/cantidad colapsan.
      (c) En los 8 bloques de Orden de Venta y Traspaso la medicion previa decia "0 duplicados,
          ahi sobra". CONFIRMO los 0 duplicados de HOY pero NO lo saco, y esta es la razon:
          "0 duplicados hoy" es un hecho de datos, no una garantia del esquema. Estos bloques
          tambien proyectan lineas (RDR1 / WTR1) sin LineNum. Busque grupos de lineas que
          colapsarian (mismo DocEntry+ItemCode+WhsCode+Quantity+OpenQty+Weight1+Dscription) y
          EXISTEN EN LA BASE: IMPEXPAP.WTR1 27 grupos, ESPPAPEL.WTR1 4 grupos,
          PAPIRUS.RDR1 12 grupos. Ninguno cae hoy en la ventana de 30 dias con chofer valido,
          pero el dia que uno caiga, sin DISTINCT el chofer veria la linea repetida.
      (d) Y sobre todo: EL DISTINCT NO CUESTA NADA. Medido con transporte real de filas,
          610 ms con DISTINCT vs 623 ms sin DISTINCT. Sacarlo no compra ni un milisegundo y
          agrega un modo de falla. Se queda.

   7) @fechaCorte : SE ELIMINA LA VARIABLE LOCAL. NO se usa OPTION (RECOMPILE).
      Hoy el SP hace  Declare @fechaCorte Date = DATEADD(DAY,-30,GETDATE())  y compara contra
      esa variable. El optimizador NO puede sniffear una variable local: asume ~30% de
      selectividad en lugar del ~0.5% real, y de ahi salen las estimaciones infladas, los hash
      joins de mas y el memory grant de 281 MB por ejecucion.
      La solucion NO es RECOMPILE. Medido sobre los bloques YA optimizados:
          bloque            inline    variable   variable+RECOMPILE   parametro sniffeado
          FAC IMPEXPAP       37 ms     128 ms          153 ms                35 ms
          FAC ESPPAPEL       44 ms     147 ms          179 ms                44 ms
          FAC PRODUCTIVA     14 ms      27 ms           77 ms                18 ms
      OPTION (RECOMPILE) es PEOR que dejar la variable: el CompileTime del statement (1001 ms
      medidos en el SP actual) se paga en cada ejecucion y se come toda la ganancia.
      Lo que funciona es no tener variable: se escribe la expresion de fecha DIRECTAMENTE en
      cada predicado. GETDATE() es una runtime constant que el optimizador SI evalua al
      compilar, y @dias es un PARAMETRO, que SI se sniffea. Por eso el parametro empata con
      el literal (35/44/18 ms) sin pagar ningun compile.
      >>> NO "LIMPIES" ESTO DEVOLVIENDOLO A UNA VARIABLE. La repeticion de
      >>>    CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE)
      >>> en los 12 bloques es DELIBERADA y vale 3x de performance.

   8) Conversiones implicitas nvarchar->int corregidas donde se podia sin cambiar semantica.
      El plan de hoy lleva warnings PlanAffectingConvert ConvertIssue="Cardinality Estimate".
      - CONVERT_IMPLICIT(int, U_Chofer) x12: desaparece. Al haber JOIN con [@CHOFERES], el
        filtro pasa de  t1.U_Chofer <> 11  (que convierte la columna nvarchar(50) a int) a
        ch.Code <> N'11'  (nvarchar contra nvarchar).
        ESTO ES EQUIVALENTE Y SE PUEDE PROBAR: despues del JOIN, U_Chofer solo puede ser uno de
        los Code que existen en [@CHOFERES], y esos son exactamente '1'..'22', sin ceros a la
        izquierda ni espacios ni repetidos como enteros. Entonces
        CONVERT(int,U_Chofer) <> 11  <=>  ch.Code <> N'11'  para toda fila que sobrevive al join.
        Los valores que hoy NO son enteros canonicos (medido: IMPEXPAP.OWTR tiene 1870 filas con
        U_Chofer = '' y 2 con NULL) quedan descartados igual por el join en ambas versiones.
        BONUS: elimina un riesgo de error de conversion en runtime que hoy existe.
      - CONVERT_IMPLICIT(int, ObjType) x4: se hace explicita como CONVERT(int, t0.ObjType).
        Mismo comportamiento (int tiene precedencia sobre nvarchar, el motor ya hacia esto),
        pero queda a la vista en el codigo.
      - CONVERT_IMPLICIT(int, BaseRef) x4: se CONSERVA a proposito. Ver punto 4.

   9) Filtro de fecha empujado DENTRO de la derivada EN (antes del join), como se pidio.
      Nota honesta: por si solo esto no compra nada. Estaba medido que el optimizador ya
      empujaba el predicado y que la derivada sin filtro no era el problema (el bloque corria
      en 10-11 ms sacando UDF y valido). Se hace igual porque deja la intencion explicita y
      porque ahora el filtro viene de un parametro.
      El LEFT OUTER JOIN a EN se convirtio en INNER JOIN: no es un cambio de semantica, el
      WHERE de hoy ya lo anulaba (EN.DocDate >= @fechaCorte descarta las filas NULL-extendidas).

  10) @dias y @codEmpleado (parametros nuevos, ambos opcionales)
      @dias INT = 30                  ventana en dias. Default = comportamiento de hoy.
      @codEmpleado NVARCHAR(25) = NULL filtro opcional por chofer, del lado de SAP.
      Con @codEmpleado NULL devuelve EXACTAMENTE lo mismo que hoy (verificado, 417 filas).
      Se tipea NVARCHAR(25) para que coincida con [@CHOFERES].U_codEmpleado y no se introduzca
      otra conversion implicita.
      MEDIDO: el predicado (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado) no degrada
      el camino por defecto (157 ms con el predicado vs 154 ms sin el, dentro del ruido) porque
      filtra sobre una tabla de 22 filas.
      Pasando @codEmpleado = '174': 62 filas en 121 ms, y se verifico que da exactamente el
      mismo conjunto que filtrar la salida del SP actual por U_Chofer = '174'.
      Los parametros van DESPUES de @ACCION y con default, asi que la llamada de hoy
      (EXEC ... @ACCION='A') sigue funcionando sin tocar nada del lado de BOSQUE-2_0.

   =============================================================================================
   EQUIVALENCIA — COMO SE VERIFICO
   =============================================================================================
     El ALTER NO ESTA APLICADO (cuenta de solo lectura), asi que no se pudo ejecutar el SP nuevo
     como SP. Lo que SI se hizo, contra el servidor real y con los datos reales:

     - Se replicaron los 12 bloques de la version vieja y los 12 de la nueva, y se ejecutaron
       via OPENQUERY uno por uno.
     - La version nueva se ejecuto ADEMAS con EXEC sp_executesql pasando @dias/@codEmpleado como
       parametros de verdad, o sea que el texto de este script ya paso por parse, compilacion y
       ejecucion reales en SRV_2022. Lo que no se pudo es el ALTER.
     - Se compararon los 417 filas x 25 columnas fila por fila. La comparacion NO se hizo con
       EXCEPT (el linked server devuelve collation SQL_Latin1_General_CP850_CI_AS y choca con
       la CP1 de BOSQUE-2_0); se hizo serializando cada fila de forma ORDINAL e invariante y
       comparando los multiconjuntos, que es MAS estricto que un EXCEPT porque tambien detecta
       diferencias de mayusculas, de padding y de formato numerico.
     - Se comparo tambien la METADATA de cada bloque (nombre, tipo .NET y longitud de las 25
       columnas), porque el consumidor es un OPENQUERY y un cambio de tipo lo rompe.

     RESULTADO: los 12 bloques identicos, en las 3 corridas
       @dias=30 @codEmpleado=NULL   -> 417 filas, 0 diferencias en ambos sentidos, metadata OK
       @dias=30 @codEmpleado='174'  ->  62 filas, identico a filtrar la salida vieja por chofer

     IGUAL: DESPUES DE APLICAR EL ALTER, CORRER LA SECCION "VERIFICACION POST-ALTER" DEL FINAL
     DE ESTE ARCHIVO, que compara el SP entero (no bloque por bloque) contra la foto de hoy.

   =============================================================================================
   BOMBA DE TIEMPO PREEXISTENTE QUE **NO** SE TOCA (pero conviene que alguien la sepa)
   =============================================================================================
     La columna de salida 'factura' termina siendo **int**, no texto. Pasa porque los 4 bloques
     de Factura ponen ODLN.NumAtCard (nvarchar(200)) y los 8 de Orden de Venta y Traspaso ponen
     el literal 0 (int); en un UNION ALL el int tiene precedencia, asi que el motor CONVIERTE
     NumAtCard a int en cada fila de Factura.
     Consecuencia: si a un remito le cargan un numero de factura del cliente que no sea
     puramente numerico, ESTE SP REVIENTA ENTERO con error de conversion, y con el se cae la
     lista de entregas de TODOS los choferes.
     MEDIDO HOY: en la ventana de 30 dias hay 0 casos en las 4 empresas (no hay riesgo inmediato),
     pero en el historico de IMPEXPAP.ODLN ya existen 2 remitos con NumAtCard no numerico. O sea
     que el caso es real, solo que hoy esta fuera de ventana.
     NO SE CORRIGE ACA A PROPOSITO: arreglarlo cambia el tipo de la columna 'factura' de int a
     nvarchar, y eso es un cambio de contrato que rompe al consumidor (OPENQUERY -> @temporal ->
     trch_Entregas -> RowMapper por indice en Java). Es un cambio que hay que planificar con el
     lado de BOSQUE-2_0, no colarlo en una optimizacion que promete "mismas columnas y tipos".
     Queda anotado como deuda, separado de este trabajo.

   =============================================================================================
   LO QUE ESTE SCRIPT **NO** ARREGLA
   =============================================================================================
     El problema de fondo sigue siendo arquitectonico: un chofer que abre su lista dispara la
     sincronizacion COMPLETA de los 4 SAP para TODOS los choferes, y eso pasa DENTRO del request
     HTTP. Bajar ese trabajo de 1413 ms a ~155 ms lo hace mucho mas tolerable, pero con 15
     choferes refrescando siguen siendo 15 sincronizaciones identicas y simultaneas.
     La correccion de fondo es sacar la sincronizacion del request (job periodico o cache con
     TTL en p_abm_trch_Entregas) y eso NO se toca aca. El parametro @codEmpleado se agrega
     justamente para dejar preparado el dia que se quiera pedir solo lo de un chofer.
   ============================================================================================= */

USE [CONEXION];
GO

/* Chequeo de que estas en el servidor correcto: si esto falla, estas en el server equivocado. */
IF DB_NAME() <> 'CONEXION'
BEGIN
    RAISERROR('ABORTADO: este script va en la base CONEXION de SRV_2022, no en %s.', 16, 1, DB_NAME());
    SET NOEXEC ON;
END
GO

IF OBJECT_ID('[dbo].[p_list_EntregasOVTraspasos]', 'P') IS NULL
BEGIN
    RAISERROR('ABORTADO: no existe [CONEXION].[dbo].[p_list_EntregasOVTraspasos]. Servidor equivocado?', 16, 1);
    SET NOEXEC ON;
END
GO

-- ---------------------------------------------------------------------------------------------
-- RESPALDO: guardate el texto actual antes de pisarlo.
--   SELECT OBJECT_DEFINITION(OBJECT_ID('[dbo].[p_list_EntregasOVTraspasos]'));
-- y pegalo en un .sql aparte. El ALTER de abajo no se puede deshacer solo.
-- ---------------------------------------------------------------------------------------------

ALTER PROCEDURE [dbo].[p_list_EntregasOVTraspasos]
	@ACCION VARCHAR(2) = NULL,
	@dias INT = 30,                      -- ventana en dias. 30 = comportamiento historico.
	@codEmpleado NVARCHAR(25) = NULL     -- filtro opcional por chofer. NULL = todos (default).
AS
SET NOCOUNT ON;
BEGIN

-- =================== DESPLEGARA EL EXTRACTO ENTRE FECHAS ======================================
	IF( @ACCION = 'A' )
	BEGIN

		/* ---------------------------------------------------------------------------------
		   NO declarar @fechaCorte como variable. La expresion de fecha va INLINE en cada
		   bloque a proposito: una variable local no se puede sniffear y cuesta 3x.
		   Ver punto 7 del encabezado, con las mediciones.
		   --------------------------------------------------------------------------------- */

		/*=========================================================================================
		=========================== ENTREGAS GENERADAS POR FACTURAS ===============================
		=========================================================================================*/
		--- ***** IMPEXPAP
		SELECT DISTINCT
		 EN.DocEntry
		,EN.DocNum
		,t0.DocNum as DocNumF
		,EN.factura
		,EN.DocDate
		,EN.DocTime
		,EN.CardCode
		,EN.CardName
		,EN.addressEntregaFac
		,EN.addressEntregaMat
		,EN.vendedor
		,EN.U_ChoferEmp as U_Chofer
		,EN.ItemCode
		,EN.Dscription
		,EN.WhsCode
		,EN.Quantity
		,EN.OpenQty
		,'IPX' as db
		,(CASE WHEN nc.DocNumRef IS NOT NULL THEN 'A' ELSE 'V' END) as valido
		,EN.Weight1
		,EN.U_Coche
		,EN.U_Prioridad
		,'Factura' as tipo
		,EN.Comments
		,EN.telefono
		FROM IMPEXPAP.dbo.OINV t0
		JOIN (SELECT DISTINCT
		  t1.DocEntry
		 ,t1.DocNum
		 ,t1.NumAtCard as factura
		 ,t1.DocDate
		 ,t1.DocTime
		 ,t1.CardCode
		 ,t1.CardName
		 ,isnull(t1.Address, '') as addressEntregaFac
		 ,isnull(t1.Address2, '') as addressEntregaMat
		 ,sl.SlpName as vendedor
		 ,t1.U_Chofer
		 ,ch.U_codEmpleado as U_ChoferEmp
		 ,t2.ItemCode
		 ,t2.Dscription
		 ,t2.WhsCode
		 ,t2.Quantity
		 ,t2.OpenQty
		 ,t2.BaseEntry
		 ,t2.BaseType
		 ,t2.Weight1
		 ,t1.U_Coche
		 ,t1.U_Prioridad
		 ,t1.Comments
		 ,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) as telefono
		 FROM IMPEXPAP.dbo.ODLN t1
		 JOIN IMPEXPAP.dbo.DLN1 t2 ON t1.DocEntry = t2.DocEntry
		 JOIN IMPEXPAP.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		 LEFT JOIN IMPEXPAP.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		 LEFT JOIN IMPEXPAP.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		 WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)) EN
		  ON EN.BaseEntry = t0.DocEntry AND EN.BaseType = CONVERT(int, t0.ObjType)
		LEFT JOIN (SELECT DISTINCT CONVERT(int, tb.BaseRef) AS DocNumRef
		           FROM IMPEXPAP.dbo.ORIN ta JOIN IMPEXPAP.dbo.RIN1 tb ON ta.DocEntry = tb.DocEntry) nc
		  ON nc.DocNumRef = t0.DocNum
		WHERE EXISTS (SELECT 1 FROM IMPEXPAP.dbo.INV1 t1 WHERE t1.DocEntry = t0.DocEntry)
		UNION ALL

		--- ***** ESPPAPEL
		SELECT DISTINCT
		 EN.DocEntry
		,EN.DocNum
		,t0.DocNum as DocNumF
		,EN.factura
		,EN.DocDate
		,EN.DocTime
		,EN.CardCode
		,EN.CardName
		,EN.addressEntregaFac
		,EN.addressEntregaMat
		,EN.vendedor
		,EN.U_ChoferEmp as U_Chofer
		,EN.ItemCode
		,EN.Dscription
		,EN.WhsCode
		,EN.Quantity
		,EN.OpenQty
		,'ESP' as db
		,(CASE WHEN nc.DocNumRef IS NOT NULL THEN 'A' ELSE 'V' END) as valido
		,EN.Weight1
		,EN.U_Coche
		,EN.U_Prioridad
		,'Factura' as tipo
		,EN.Comments
		,EN.telefono
		FROM ESPPAPEL.dbo.OINV t0
		JOIN (SELECT DISTINCT
		  t1.DocEntry
		 ,t1.DocNum
		 ,t1.NumAtCard as factura
		 ,t1.DocDate
		 ,t1.DocTime
		 ,t1.CardCode
		 ,t1.CardName
		 ,isnull(t1.Address, '') as addressEntregaFac
		 ,isnull(t1.Address2, '') as addressEntregaMat
		 ,sl.SlpName as vendedor
		 ,t1.U_Chofer
		 ,ch.U_codEmpleado as U_ChoferEmp
		 ,t2.ItemCode
		 ,t2.Dscription
		 ,t2.WhsCode
		 ,t2.Quantity
		 ,t2.OpenQty
		 ,t2.BaseEntry
		 ,t2.BaseType
		 ,t2.Weight1
		 ,t1.U_Coche
		 ,t1.U_Prioridad
		 ,t1.Comments
		 ,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) as telefono
		 FROM ESPPAPEL.dbo.ODLN t1
		 JOIN ESPPAPEL.dbo.DLN1 t2 ON t1.DocEntry = t2.DocEntry
		 JOIN ESPPAPEL.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		 LEFT JOIN ESPPAPEL.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		 LEFT JOIN ESPPAPEL.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		 WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)) EN
		  ON EN.BaseEntry = t0.DocEntry AND EN.BaseType = CONVERT(int, t0.ObjType)
		LEFT JOIN (SELECT DISTINCT CONVERT(int, tb.BaseRef) AS DocNumRef
		           FROM ESPPAPEL.dbo.ORIN ta JOIN ESPPAPEL.dbo.RIN1 tb ON ta.DocEntry = tb.DocEntry) nc
		  ON nc.DocNumRef = t0.DocNum
		WHERE EXISTS (SELECT 1 FROM ESPPAPEL.dbo.INV1 t1 WHERE t1.DocEntry = t0.DocEntry)
		UNION ALL

		--- ***** PAPIRUS
		SELECT DISTINCT
		 EN.DocEntry
		,EN.DocNum
		,t0.DocNum as DocNumF
		,EN.factura
		,EN.DocDate
		,EN.DocTime
		,EN.CardCode
		,EN.CardName
		,EN.addressEntregaFac
		,EN.addressEntregaMat
		,EN.vendedor
		,EN.U_ChoferEmp as U_Chofer
		,EN.ItemCode
		,EN.Dscription
		,EN.WhsCode
		,EN.Quantity
		,EN.OpenQty
		,'PAP' as db
		,(CASE WHEN nc.DocNumRef IS NOT NULL THEN 'A' ELSE 'V' END) as valido
		,EN.Weight1
		,EN.U_Coche
		,EN.U_Prioridad
		,'Factura' as tipo
		,EN.Comments
		,EN.telefono
		FROM PAPIRUS.dbo.OINV t0
		JOIN (SELECT DISTINCT
		  t1.DocEntry
		 ,t1.DocNum
		 ,isnull(t1.NumAtCard,0) as factura
		 ,t1.DocDate
		 ,t1.DocTime
		 ,t1.CardCode
		 ,t1.CardName
		 ,isnull(t1.Address, '') as addressEntregaFac
		 ,isnull(t1.Address2, '') as addressEntregaMat
		 ,sl.SlpName as vendedor
		 ,t1.U_Chofer
		 ,ch.U_codEmpleado as U_ChoferEmp
		 ,t2.ItemCode
		 ,t2.Dscription
		 ,t2.WhsCode
		 ,t2.Quantity
		 ,t2.OpenQty
		 ,t2.BaseEntry
		 ,t2.BaseType
		 ,t2.Weight1
		 ,t1.U_Coche
		 ,t1.U_Prioridad
		 ,t1.Comments
		 ,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) as telefono
		 FROM PAPIRUS.dbo.ODLN t1
		 JOIN PAPIRUS.dbo.DLN1 t2 ON t1.DocEntry = t2.DocEntry
		 JOIN PAPIRUS.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		 LEFT JOIN PAPIRUS.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		 LEFT JOIN PAPIRUS.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		 WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)) EN
		  ON EN.BaseEntry = t0.DocEntry AND EN.BaseType = CONVERT(int, t0.ObjType)
		LEFT JOIN (SELECT DISTINCT CONVERT(int, tb.BaseRef) AS DocNumRef
		           FROM PAPIRUS.dbo.ORIN ta JOIN PAPIRUS.dbo.RIN1 tb ON ta.DocEntry = tb.DocEntry) nc
		  ON nc.DocNumRef = t0.DocNum
		WHERE EXISTS (SELECT 1 FROM PAPIRUS.dbo.INV1 t1 WHERE t1.DocEntry = t0.DocEntry)
		UNION ALL

		--- ***** PRODUCTIVA PAPEL
		SELECT DISTINCT
		 EN.DocEntry
		,EN.DocNum
		,t0.DocNum as DocNumF
		,EN.factura
		,EN.DocDate
		,EN.DocTime
		,EN.CardCode
		,EN.CardName
		,EN.addressEntregaFac
		,EN.addressEntregaMat
		,EN.vendedor
		,EN.U_ChoferEmp as U_Chofer
		,EN.ItemCode
		,EN.Dscription
		,EN.WhsCode
		,EN.Quantity
		,EN.OpenQty
		,'PRODPAP' as db
		,(CASE WHEN nc.DocNumRef IS NOT NULL THEN 'A' ELSE 'V' END) as valido
		,EN.Weight1
		,EN.U_Coche
		,EN.U_Prioridad
		,'Factura' as tipo
		,EN.Comments
		,EN.telefono
		FROM [PRODUCTIVA PAPEL].dbo.OINV t0
		JOIN (SELECT DISTINCT
		  t1.DocEntry
		 ,t1.DocNum
		 ,t1.NumAtCard as factura
		 ,t1.DocDate
		 ,t1.DocTime
		 ,t1.CardCode
		 ,t1.CardName
		 ,isnull(t1.Address, '') as addressEntregaFac
		 ,isnull(t1.Address2, '') as addressEntregaMat
		 ,sl.SlpName as vendedor
		 ,t1.U_Chofer
		 ,ch.U_codEmpleado as U_ChoferEmp
		 ,t2.ItemCode
		 ,t2.Dscription
		 ,t2.WhsCode
		 ,t2.Quantity
		 ,t2.OpenQty
		 ,t2.BaseEntry
		 ,t2.BaseType
		 ,t2.Weight1
		 ,t1.U_Coche
		 ,t1.U_Prioridad
		 ,t1.Comments
		 ,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) as telefono
		 FROM [PRODUCTIVA PAPEL].dbo.ODLN t1
		 JOIN [PRODUCTIVA PAPEL].dbo.DLN1 t2 ON t1.DocEntry = t2.DocEntry
		 JOIN [PRODUCTIVA PAPEL].dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		 LEFT JOIN [PRODUCTIVA PAPEL].dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		 LEFT JOIN [PRODUCTIVA PAPEL].dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		 WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)) EN
		  ON EN.BaseEntry = t0.DocEntry AND EN.BaseType = CONVERT(int, t0.ObjType)
		LEFT JOIN (SELECT DISTINCT CONVERT(int, tb.BaseRef) AS DocNumRef
		           FROM [PRODUCTIVA PAPEL].dbo.ORIN ta JOIN [PRODUCTIVA PAPEL].dbo.RIN1 tb ON ta.DocEntry = tb.DocEntry) nc
		  ON nc.DocNumRef = t0.DocNum
		WHERE EXISTS (SELECT 1 FROM [PRODUCTIVA PAPEL].dbo.INV1 t1 WHERE t1.DocEntry = t0.DocEntry)

		/*=========================================================================================
		=========================== ORDENES DE VENTA ==============================================
		=========================================================================================*/
		UNION ALL

		--- ***** IMPEXPAP
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as DocNumF
		,0 as Factura
		,t1.DocDate
		,t1.DocTime
		,t1.CardCode
		,t1.CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'IPX' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Orden Venta' as tipo
		,t1.Comments
		,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) AS telefono
		FROM IMPEXPAP.dbo.ORDR t1
		JOIN IMPEXPAP.dbo.RDR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN IMPEXPAP.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN IMPEXPAP.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN IMPEXPAP.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** ESPPAPEL
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as DocNumF
		,0 as Factura
		,t1.DocDate
		,t1.DocTime
		,t1.CardCode
		,t1.CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'ESP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Orden Venta' as tipo
		,t1.Comments
		,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) AS telefono
		FROM ESPPAPEL.dbo.ORDR t1
		JOIN ESPPAPEL.dbo.RDR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN ESPPAPEL.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN ESPPAPEL.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN ESPPAPEL.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** PAPIRUS
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as DocNumF
		,0 as Factura
		,t1.DocDate
		,t1.DocTime
		,t1.CardCode
		,t1.CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'PAP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Orden Venta' as tipo
		,t1.Comments
		,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) AS telefono
		FROM PAPIRUS.dbo.ORDR t1
		JOIN PAPIRUS.dbo.RDR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN PAPIRUS.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN PAPIRUS.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN PAPIRUS.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** PRODUCTIVA PAPEL
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as DocNumF
		,0 as Factura
		,t1.DocDate
		,t1.DocTime
		,t1.CardCode
		,t1.CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'PRODPAP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Orden Venta' as tipo
		,t1.Comments
		,CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)) AS telefono
		FROM [PRODUCTIVA PAPEL].dbo.ORDR t1
		JOIN [PRODUCTIVA PAPEL].dbo.RDR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN [PRODUCTIVA PAPEL].dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)

		/*=========================================================================================
		=========================== TRASPASOS =====================================================
		=========================================================================================*/
		UNION ALL

		--- ***** IMPEXPAP
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as docNumF
		,0 as factura
		,t1.DocDate
		,t1.DocTime
		,isnull(t1.CardCode,'') as CardCode
		,'Traspaso -> de '+ wo.WhsName + ' a '+ wd.WhsName as CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'IPX' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Traspaso' as tipo
		,t1.JrnlMemo
		,ISNULL(CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)), '') AS telefono
		FROM IMPEXPAP.dbo.OWTR t1
		JOIN IMPEXPAP.dbo.WTR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN IMPEXPAP.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN IMPEXPAP.dbo.OWHS wo ON wo.WhsCode = t1.Filler
		LEFT JOIN IMPEXPAP.dbo.OWHS wd ON wd.WhsCode = t1.ToWhsCode
		LEFT JOIN IMPEXPAP.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN IMPEXPAP.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.Filler NOT IN ('F','ALM-IMP','TPA') AND t1.toWhscode NOT IN ('F','ALM-IMP','TPA')
		AND t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'13' AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** ESPPAPEL
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as docNumF
		,0 as factura
		,t1.DocDate
		,t1.DocTime
		,isnull(t1.CardCode,'') as CardCode
		,'Traspaso -> de '+ wo.WhsName + ' a '+ wd.WhsName as CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'ESP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Traspaso' as tipo
		,t1.JrnlMemo
		,ISNULL(CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)), '') AS telefono
		FROM ESPPAPEL.dbo.OWTR t1
		JOIN ESPPAPEL.dbo.WTR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN ESPPAPEL.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN ESPPAPEL.dbo.OWHS wo ON wo.WhsCode = t1.Filler
		LEFT JOIN ESPPAPEL.dbo.OWHS wd ON wd.WhsCode = t1.ToWhsCode
		LEFT JOIN ESPPAPEL.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN ESPPAPEL.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.Filler NOT IN ('F','ALM-IMP','TPA') AND t1.toWhscode NOT IN ('F','ALM-IMP','TPA')
		AND t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'13' AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** PAPIRUS
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as docNumF
		,0 as factura
		,t1.DocDate
		,t1.DocTime
		,isnull(t1.CardCode,'') as CardCode
		,'Traspaso -> de '+ wo.WhsName + ' a '+ wd.WhsName as CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'PAP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Traspaso' as tipo
		,t1.JrnlMemo
		,ISNULL(CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)), '') AS telefono
		FROM PAPIRUS.dbo.OWTR t1
		JOIN PAPIRUS.dbo.WTR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN PAPIRUS.dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN PAPIRUS.dbo.OWHS wo ON wo.WhsCode = t1.Filler
		LEFT JOIN PAPIRUS.dbo.OWHS wd ON wd.WhsCode = t1.ToWhsCode
		LEFT JOIN PAPIRUS.dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN PAPIRUS.dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.Filler NOT IN ('F','ALM-IMP','TPA','AI','AI04','AI6') AND t1.toWhscode NOT IN ('F','ALM-IMP','TPA','AI','AI04','AI6')
		AND t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'13' AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)
		UNION ALL

		--- ***** PRODUCTIVA PAPEL
		SELECT DISTINCT
		 t1.DocEntry
		,t1.DocNum
		,0 as docNumF
		,0 as factura
		,t1.DocDate
		,t1.DocTime
		,isnull(t1.CardCode,'') as CardCode
		,'Traspaso -> de '+ wo.WhsName + ' a '+ wd.WhsName as CardName
		,t1.Address
		,t1.Address2
		,sl.SlpName as vendedor
		,ch.U_codEmpleado as U_Chofer
		,t2.ItemCode
		,t2.Dscription
		,t2.WhsCode
		,t2.Quantity
		,t2.OpenQty
		,'PRODPAP' as db
		,t1.CANCELED
		,t2.Weight1
		,t1.U_Coche
		,t1.U_Prioridad
		,'Traspaso' as tipo
		,t1.JrnlMemo
		,ISNULL(CAST(CASE
		  WHEN c.CardCode IS NULL THEN NULL
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND tp.cel <> N'' THEN tp.cel
		  WHEN LEFT(tp.p, 1) IN (N'2', N'3', N'4') AND LEFT(ts.seg, 1) IN (N'6', N'7') THEN ts.seg
		  ELSE tp.p
		 END AS NVARCHAR(50)), '') AS telefono
		FROM [PRODUCTIVA PAPEL].dbo.OWTR t1
		JOIN [PRODUCTIVA PAPEL].dbo.WTR1 t2 ON t1.DocEntry = t2.DocEntry
		JOIN [PRODUCTIVA PAPEL].dbo.[@CHOFERES] ch ON ch.Code = t1.U_Chofer
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OWHS wo ON wo.WhsCode = t1.Filler
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OWHS wd ON wd.WhsCode = t1.ToWhsCode
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OSLP sl ON sl.SlpCode = t1.SlpCode
		LEFT JOIN [PRODUCTIVA PAPEL].dbo.OCRD c ON c.CardCode = t1.CardCode
		 CROSS APPLY (VALUES (LTRIM(RTRIM(ISNULL(c.Phone1, N''))), LTRIM(RTRIM(ISNULL(c.Cellular, N''))))) tp(p, cel)
		 CROSS APPLY (VALUES (CASE WHEN CHARINDEX(N' ', tp.p) > 0 THEN LTRIM(SUBSTRING(tp.p, CHARINDEX(N' ', tp.p) + 1, 50)) ELSE N'' END)) ts(seg)
		WHERE t1.Filler NOT IN ('F','ALM-IMP','TPA') AND t1.toWhscode NOT IN ('F','ALM-IMP','TPA')
		AND t1.DocDate >= CAST(DATEADD(DAY, -@dias, GETDATE()) AS DATE) AND ch.Code <> N'13' AND ch.Code <> N'11' AND (@codEmpleado IS NULL OR ch.U_codEmpleado = @codEmpleado)


	END

END
GO

SET NOEXEC OFF;
GO


/* =============================================================================================
   VERIFICACION POST-ALTER   —   CORRER ESTO DESPUES DE APLICAR EL ALTER
   ---------------------------------------------------------------------------------------------
   El paso 1 se corre ANTES del ALTER y el resto DESPUES. Si no sacaste la foto antes, igual
   podes usar el paso 3 (que no necesita foto) y el paso 4 (que compara contra la definicion
   vieja que respaldaste).
   ============================================================================================= */

/* ---------------------------------------------------------------------------------------------
   PASO 1  ·  ANTES DEL ALTER, EN SRV_2022, BASE CONEXION
   Saca una foto del resultado de hoy. Es tempdb, no toca nada de SAP.
   --------------------------------------------------------------------------------------------- */
/*
IF OBJECT_ID('tempdb..##entregas_antes') IS NOT NULL DROP TABLE ##entregas_antes;
SELECT * INTO ##entregas_antes
FROM OPENQUERY([<TU_LINKED_SERVER_A_SI_MISMO>], 'EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION=''A''');
-- Si no hay loopback linked server, la alternativa sin linked server es:
--   INSERT INTO ##entregas_antes EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION='A';
-- pero requiere crear la tabla con las 25 columnas a mano. Lo mas comodo es correr el paso 1
-- DESDE BOSQUE-2_0, que ya tiene el linked server SRV_2022 configurado:
--
--   -- (en BOSQUE-2_0, ANTES del ALTER)
--   IF OBJECT_ID('tempdb..##entregas_antes') IS NOT NULL DROP TABLE ##entregas_antes;
--   SELECT * INTO ##entregas_antes
--   FROM OPENQUERY(SRV_2022,'EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION=''A''');
--   SELECT COUNT(*) AS filas_antes FROM ##entregas_antes;   -- hoy da 417
*/

/* ---------------------------------------------------------------------------------------------
   PASO 2  ·  DESPUES DEL ALTER, DESDE BOSQUE-2_0
   Compara la foto contra el resultado nuevo. Las 4 cifras tienen que dar:
        filas_antes = filas_despues     y     solo_antes = 0     y     solo_despues = 0
   OJO CON LA COLLATION: si tira "No se puede resolver el conflicto de intercalacion", corré
   el PASO 3 en su lugar, que hace la misma comparacion pero del lado de SRV_2022.
   --------------------------------------------------------------------------------------------- */
/*
IF OBJECT_ID('tempdb..##entregas_despues') IS NOT NULL DROP TABLE ##entregas_despues;
SELECT * INTO ##entregas_despues
FROM OPENQUERY(SRV_2022,'EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION=''A''');

SELECT
    (SELECT COUNT(*) FROM ##entregas_antes)   AS filas_antes,
    (SELECT COUNT(*) FROM ##entregas_despues) AS filas_despues,
    (SELECT COUNT(*) FROM (SELECT * FROM ##entregas_antes   EXCEPT SELECT * FROM ##entregas_despues) x) AS solo_antes,
    (SELECT COUNT(*) FROM (SELECT * FROM ##entregas_despues EXCEPT SELECT * FROM ##entregas_antes)   y) AS solo_despues;
*/

/* ---------------------------------------------------------------------------------------------
   PASO 3  ·  DESPUES DEL ALTER, EN SRV_2022 (BASE CONEXION)
   Compara el SP nuevo contra el SP viejo sin necesitar foto previa: para eso hay que dejar
   una copia del SP viejo con otro nombre ANTES del ALTER.
       -- ANTES del ALTER, en CONEXION:
       -- tomá OBJECT_DEFINITION(...) y creá p_list_EntregasOVTraspasos_OLD con el mismo cuerpo.
   Despues:
   --------------------------------------------------------------------------------------------- */
/*
IF OBJECT_ID('tempdb..#viejo') IS NOT NULL DROP TABLE #viejo;
IF OBJECT_ID('tempdb..#nuevo') IS NOT NULL DROP TABLE #nuevo;

-- Forma EXACTA del result set de hoy, obtenida de
-- sys.dm_exec_describe_first_result_set contra el SP actual (25 columnas):
CREATE TABLE #viejo (
   DocEntry          int,
   DocNum            int,
   DocNumF           int,
   factura           int,             -- OJO: int, no nvarchar. Ver nota (*) abajo.
   DocDate           datetime,
   DocTime           smallint,
   CardCode          nvarchar(15),
   CardName          nvarchar(218),
   addressEntregaFac nvarchar(254),
   addressEntregaMat nvarchar(254),
   vendedor          nvarchar(155),
   U_Chofer          nvarchar(25),
   ItemCode          nvarchar(50),
   Dscription        nvarchar(200),
   WhsCode           nvarchar(8),
   Quantity          numeric(19,6),
   OpenQty           numeric(19,6),
   db                varchar(7),
   valido            varchar(1),
   Weight1           numeric(19,6),
   U_Coche           nvarchar(50),
   U_Prioridad       nvarchar(25),
   tipo              varchar(11),
   Comments          nvarchar(254),
   telefono          nvarchar(50)
);

SELECT * INTO #nuevo FROM #viejo WHERE 1 = 0;

INSERT INTO #viejo EXEC [dbo].[p_list_EntregasOVTraspasos_OLD] @ACCION = 'A';
INSERT INTO #nuevo EXEC [dbo].[p_list_EntregasOVTraspasos]     @ACCION = 'A';

SELECT
    (SELECT COUNT(*) FROM #viejo) AS filas_viejo,
    (SELECT COUNT(*) FROM #nuevo) AS filas_nuevo,
    (SELECT COUNT(*) FROM (SELECT * FROM #viejo EXCEPT SELECT * FROM #nuevo) x) AS solo_viejo,
    (SELECT COUNT(*) FROM (SELECT * FROM #nuevo EXCEPT SELECT * FROM #viejo) y) AS solo_nuevo;
-- Tiene que dar  filas_viejo = filas_nuevo  y  solo_viejo = solo_nuevo = 0.
*/

/* ---------------------------------------------------------------------------------------------
   PASO 4  ·  METADATA (que no cambien nombres ni tipos de las 25 columnas)
   El consumidor es un OPENQUERY: si cambia un tipo, se rompe del lado de BOSQUE-2_0.
   Compará la salida de esto contra la que guardaste antes del ALTER.
   --------------------------------------------------------------------------------------------- */
/*
EXEC sp_describe_first_result_set
     N'EXEC [CONEXION].[dbo].[p_list_EntregasOVTraspasos] @ACCION=''A''';
*/

/* ---------------------------------------------------------------------------------------------
   PASO 5  ·  QUE EFECTIVAMENTE PARALELICE (era el objetivo del punto 2)
   Despues de unas cuantas ejecuciones, esto tiene que mostrar dop > 1 y elapsed << cpu.
   Hoy, con la UDF escalar, da dop = 1 y cpu ~= elapsed (1364 ms de CPU / 1413 ms de elapsed).
   --------------------------------------------------------------------------------------------- */
/*
SELECT TOP 5
       qs.execution_count,
       cpu_ms_prom     = qs.total_worker_time  / qs.execution_count / 1000,
       elapsed_ms_prom = qs.total_elapsed_time / qs.execution_count / 1000,
       dop_prom        = qs.total_dop          / qs.execution_count,
       grant_kb_prom   = qs.total_grant_kb     / qs.execution_count,
       lecturas_prom   = qs.total_logical_reads/ qs.execution_count,
       SUBSTRING(st.text, 1, 120) AS inicio_texto
FROM sys.dm_exec_query_stats qs
CROSS APPLY sys.dm_exec_sql_text(qs.sql_handle) st
WHERE st.objectid = OBJECT_ID('[CONEXION].[dbo].[p_list_EntregasOVTraspasos]')
ORDER BY qs.last_execution_time DESC;
*/

/* ---------------------------------------------------------------------------------------------
   PASO 6  ·  QUE NO QUEDEN WARNINGS DE CONVERSION EN EL PLAN
   Con la reescritura tienen que quedar SOLO los de BaseRef (que se conservan a proposito).
   Ya no deberian aparecer los de U_Chofer.
   --------------------------------------------------------------------------------------------- */
/*
WITH XMLNAMESPACES (DEFAULT 'http://schemas.microsoft.com/sqlserver/2004/07/showplan')
SELECT cv.value('@Expression','nvarchar(400)') AS conversion_que_afecta_el_plan
FROM sys.dm_exec_cached_plans cp
CROSS APPLY sys.dm_exec_query_plan(cp.plan_handle) qp
CROSS APPLY qp.query_plan.nodes('//PlanAffectingConvert') AS t(cv)
WHERE qp.objectid = OBJECT_ID('[CONEXION].[dbo].[p_list_EntregasOVTraspasos]');
*/

/* ---------------------------------------------------------------------------------------------
   PASO 7  ·  ROLLBACK
   Si algo sale mal, volvé a aplicar el CREATE/ALTER original que respaldaste al principio.
   El cambio es solo de codigo: no hay DDL de tablas, ni indices, ni datos tocados.
   --------------------------------------------------------------------------------------------- */


/* =============================================================================================
   INDICES QUE CONVENDRIA CREAR   —   **NO SE CREA NINGUNO ACA, ES SOLO ANALISIS**
   ---------------------------------------------------------------------------------------------
   ADVERTENCIA PREVIA: estas son tablas ESTANDAR DE SAP Business One. Crear indices por DDL
   directo sobre tablas de SAP no esta soportado y puede complicar un upgrade. Si alguno de
   estos se decide hacer, lo correcto es darlo de alta como User-Defined Index desde el propio
   SAP (Herramientas > Personalizacion) y consultarlo con el partner. Por eso van comentados.
   ============================================================================================= */

/* ---------------------------------------------------------------------------------------------
   (A) [@CHOFERES].Code  ->  NO HACE FALTA. YA EXISTE Y NO ERA EL CUELLO.
   ---------------------------------------------------------------------------------------------
   El pedido lo mencionaba como candidato, pero la medicion dice que no.
   Las 4 empresas ya tienen  KCHOFERES_PR  CLUSTERED UNIQUE PRIMARY KEY sobre Code
   (22 filas en IMPEXPAP / ESPPAPEL / PRODUCTIVA PAPEL, 21 en PAPIRUS), y ademas
   KCHOFERES_NAME nonclustered unique sobre Name.
   Medido: sacar del SELECT la subconsulta a [@CHOFERES] no cambia nada
   (IPX 266->264 ms, ESP 277->291 ms, PROD 87->89 ms). Es un seek de PK sobre 22 filas.
   Que se evaluara dos veces por fila era feo pero salia gratis.
   >>> NO CREAR NADA ACA.
   --------------------------------------------------------------------------------------------- */

/* ---------------------------------------------------------------------------------------------
   (B) RIN1(BaseRef)  ->  NO SIRVE COMO INDICE COMUN. Requeriria columna computada.
   ---------------------------------------------------------------------------------------------
   RIN1.BaseRef es nvarchar(16) y se compara contra un int, asi que el predicado real es
   CONVERT(int, BaseRef) = DocNum. Un indice sobre la columna cruda NUNCA se va a poder usar
   para ese predicado, porque la conversion esta del lado de la columna.
   Hoy no hay indice sobre BaseRef en ninguna de las 4 empresas (solo RIN1_PRIMARY sobre
   DocEntry+LineNum y RIN1_VIS_ORDER sobre DocEntry+VisOrder).
   Para que sirviera habria que hacer:
       ALTER TABLE <emp>.dbo.RIN1 ADD BaseRefInt AS TRY_CONVERT(int, BaseRef) PERSISTED;
       CREATE NONCLUSTERED INDEX IX_RIN1_BaseRefInt ON <emp>.dbo.RIN1 (BaseRefInt);
   ...que es agregar una columna a una tabla de SAP: NO recomendado.
   Y ADEMAS YA NO HACE FALTA: la reescritura del punto 4 pasa de "escanear ORIN join RIN1 una
   vez por fila de salida" a "escanearlo UNA sola vez y hacer hash join". Las tablas son chicas
   (RIN1: 4.576 / 2.632 / 13.943 / 61 filas segun empresa), asi que una pasada completa es
   barata. El bloque quedo en 33 ms con este approach.
   >>> NO CREAR. La reescritura ya resolvio el problema sin DDL.
   --------------------------------------------------------------------------------------------- */

/* ---------------------------------------------------------------------------------------------
   (C) ODLN / OINV / ORDR / OWTR por DocDate  ->  YA EXISTEN (los trae SAP de fabrica).
   ---------------------------------------------------------------------------------------------
   Verificado en IMPEXPAP: ODLN_DATE_PIND, OINV_DATE_PIND, ORDR_DATE_PIND, OWTR_DATE_PIND,
   ORIN_DATE_PIND, todos NONCLUSTERED sobre (DocDate, PIndicator).
   Son justo los que necesita el filtro de ventana, y con el punto 7 (fecha sniffeable en vez de
   variable) el optimizador por fin tiene la estimacion correcta para decidir usarlos.
   >>> NO CREAR NADA. El punto 7 es lo que los pone en juego.
   --------------------------------------------------------------------------------------------- */

/* ---------------------------------------------------------------------------------------------
   (D) UNICO CANDIDATO CON SENTIDO, Y ES DE BAJA PRIORIDAD:
       cobertura de la derivada EN sobre ODLN.
   ---------------------------------------------------------------------------------------------
       CREATE NONCLUSTERED INDEX IX_ODLN_DocDate_Chofer
           ON <emp>.dbo.ODLN (DocDate)
           INCLUDE (U_Chofer, CardCode, SlpCode, DocNum, NumAtCard, DocTime, CardName,
                    Address, Address2, U_Coche, U_Prioridad, Comments);
   Evitaria el lookup a la clustered por cada cabecera dentro de la ventana.
   NO SE MIDIO (requiere DDL, que no tengo permiso de ejecutar), asi que no puedo afirmar cuanto
   compra. Con los bloques de Factura ya en 35/45/15/8 ms, el margen que queda es chico.
   RECOMENDACION: aplicar primero el ALTER, medir de nuevo, y solo evaluar esto si hace falta
   seguir bajando. Es el clasico indice ancho que cuesta en cada INSERT de remito y que
   probablemente no se justifique.
   --------------------------------------------------------------------------------------------- */

/* =============================================================================================
   FIN
   ============================================================================================= */
