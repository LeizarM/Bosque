/* =============================================================================================
   29_entregas_sync_desacoplada.sql   ·   p_list_trch_Entregas ACCION 'Y'  +  p_abm_trch_EntregasSync
   ---------------------------------------------------------------------------------------------
   >>> EJECUTAR EN SSMS CON UNA CUENTA QUE TENGA PERMISOS DDL (db_owner, o ALTER sobre
   >>> p_list_trch_Entregas mas CREATE PROCEDURE) SOBRE LA BASE [BOSQUE-2_0]. La cuenta de solo
   >>> lectura NO puede correrlo. Correrlo tambien en BOSQUE2PRUEBA si se va a probar contra
   >>> esa copia.

   Objetivo:
     Sacar la sincronizacion con SAP del camino del request del chofer, SIN cambiar una sola
     fila de lo que el chofer ve y SIN tocar nada de lo que ya esta desplegado.

   ---------------------------------------------------------------------------------------------
   EL PROBLEMA (medido contra el servidor real, no estimado)
   ---------------------------------------------------------------------------------------------
     Hoy, cada vez que un chofer abre su lista -y en CADA refresh- pasa esto:

        POST /entregas/chofer-entrega
         -> EntregaChoferDao.listarEntregasXEmpleado(codEmpleado)
         -> execute p_list_trch_Entregas @u_Chofer=?, @ACCION='A'
              ACCION 'A':  1. execute p_abm_trch_Entregas @audUsuario=-1, @ACCION='A'
                              <-- SINCRONIZA TODO CON SAP: OPENQUERY contra el linked server
                                  SRV_2022 a [CONEXION].[dbo].[p_list_EntregasOVTraspasos] 'A'
                           2. SELECT de los pendientes de ESE chofer

     El paso 2 es barato (devuelve los pendientes de un solo chofer). El paso 1 es el costo
     entero y no depende del chofer: sincroniza los documentos de los 30 dias de las 4 empresas
     SAP para TODOS los choferes.

        - la consulta remota tarda 1424..1539 ms en caliente (mediana 1432 ms) y devuelve
          417 filas, siempre. Del lado remoto: 1413 ms de elapsed, 1364 ms de CPU, 490.427
          lecturas logicas y 281 MB de memory grant POR EJECUCION, a DOP 1.
        - 15 choferes abriendo la app a la misma hora = 15 sincronizaciones IDENTICAS y
          SIMULTANEAS, ~4,2 GB de memory grants concurrentes, para devolverle a cada uno una
          lista chica.

     Este script NO optimiza el SP remoto (eso va aparte, vive en SRV_2022). Lo que hace es
     permitir que la sincronizacion deje de correr dentro del request: el backend nuevo lee con
     una accion que NO sincroniza, y sincroniza por su cuenta, una sola vez, fuera del request.

   ---------------------------------------------------------------------------------------------
   QUE CREA / MODIFICA ESTE SCRIPT
   ---------------------------------------------------------------------------------------------
     1) p_list_trch_Entregas   ACCION NUEVA 'Y'   (el resto del SP queda IGUAL, ver abajo)
     2) p_abm_trch_EntregasSync  ACCION 'S'       (procedimiento nuevo, no existe hoy)

     No se toca: trch_Entregas, p_abm_trch_Entregas, ni ninguna de las acciones ya desplegadas
     de p_list_trch_Entregas (L, A, B, C, D, E, F, G).

   ---------------------------------------------------------------------------------------------
   1) ACCION 'Y' = LEER SIN SINCRONIZAR
   ---------------------------------------------------------------------------------------------
     'Y' es un clon EXACTO de la ACCION 'A' menos UNA linea:

         execute p_abm_trch_Entregas @audUsuario = -1, @ACCION='A'

     Todo lo demas es identico: las mismas 28 columnas, en el mismo orden, con las mismas
     expresiones isnull(), el mismo WHERE y el mismo ORDER BY. No es una reescritura "equivalente":
     el bloque se derivo mecanicamente del texto vivo del SP borrando esa unica linea, y se
     verifico que volviendo a insertarla se reproduce byte a byte el bloque 'A' original.

     Columnas, EN ESTE ORDEN, porque el RowMapper de EntregaChoferDao.listarEntregasXEmpleado
     las lee POR INDICE (rs.getXxx(1..28)) y no por nombre:

        1 idEntrega          8 cardName            15 whsCode             22 latitud
        2 docEntry           9 addressEntregaFac   16 quantity            23 longitud
        3 docNum            10 addressEntregaMat   17 openQty             24 direccionEntrega
        4 factura           11 vendedor            18 db                  25 obs
        5 docDate           12 u_Chofer            19 valido              26 tipo
        6 docTime           13 itemCode            20 fueEntregado        27 obsF
        7 cardCode          14 dscription          21 fechaEntrega        28 audUsuario

     O sea: 'Y' es intercambiable con 'A' cambiando UN parametro en el DAO. No hay que tocar el
     RowMapper, ni el DTO, ni el Angular.

   ---------------------------------------------------------------------------------------------
   POR QUE LA ACCION 'A' SE DEJA EXACTAMENTE COMO ESTA
   ---------------------------------------------------------------------------------------------
     Porque hoy hay clientes que dependen de que 'A' sincronice: el Angular, la version vieja de
     la app movil que ya esta instalada en los telefonos, y cualquier consulta manual. Si a 'A'
     le sacaramos la sincronizacion, esos clientes empezarian a ver datos congelados sin ningun
     error visible -la peor clase de falla-. Agregando 'Y' en vez de mutar 'A', el cambio es
     puramente ADITIVO: lo que existe hoy sigue funcionando exactamente igual, y el backend nuevo
     opta por el camino desacoplado cuando esta listo. La migracion se puede hacer y deshacer
     cambiando un string en el DAO.

   ---------------------------------------------------------------------------------------------
   2) p_abm_trch_EntregasSync  ACCION 'S'  =  SINCRONIZAR Y DECIR CUANTO SINCRONIZO
   ---------------------------------------------------------------------------------------------
     Devuelve UNA fila, dos columnas:

        1 filasSincronizadas  int        filas de trch_Entregas que la sincronizacion toco
        2 fechaSync           datetime   momento en que TERMINO la sincronizacion

     DECISION: DELEGA, NO DUPLICA.
     El SP hace 'execute p_abm_trch_Entregas @audUsuario = -1, @ACCION = ''A''' y despues mide
     el efecto desde afuera. No se copio el cuerpo del ABM. Razones:

       a) Una sola fuente de verdad. El bloque 'A' del ABM tiene la tabla @temporal, el GROUP BY
          por (DocEntry, DocNum, ItemCode, db) y el par UPDATE + INSERT. Duplicarlo significa que
          el dia que alguien lo corrija -por ejemplo, para que el INSERT deje de duplicar filas-
          tenga que acordarse de corregirlo en dos lugares. La copia que se olvide es la que
          rompe produccion en silencio.
       b) El objetivo de este script es DESACOPLAR CUANDO corre la sincronizacion, no cambiar QUE
          hace. Delegando, 'S' y la ACCION 'A' de p_list hacen literalmente el mismo trabajo, con
          los mismos bugs y las mismas virtudes. Eso hace que el cambio sea reversible y que se
          pueda comparar el resultado de los dos caminos.
       c) Si manana se optimiza el SP remoto o el bloque 'A', 'S' hereda la mejora gratis.

     El precio de delegar es que no se puede leer el @@ROWCOUNT interno del ABM, asi que
     filasSincronizadas se MIDE DESDE AFUERA (ver mas abajo). Es un numero de observabilidad,
     no un dato de negocio: sirve para saber que la sincronizacion efectivamente corrio y movio
     algo, no para conciliar nada.

   ---------------------------------------------------------------------------------------------
   COMO SE MIDE filasSincronizadas (y que ruido tiene)
   ---------------------------------------------------------------------------------------------
     El bloque 'A' del ABM hace dos cosas sobre trch_Entregas:

        UPDATE ... SET u_chofer, docTime, openQty, audFecha = GETDATE()   <-- deja huella temporal
        INSERT INTO trch_Entregas (docEntry, ..., obsF)                    <-- NO escribe audFecha

     Verificado en sys.columns: trch_Entregas.audFecha es NULLable y NO tiene DEFAULT. O sea que
     las filas que la sincronizacion INSERTA quedan con audFecha NULL y no se pueden detectar por
     fecha. Por eso se usan dos criterios distintos y se suman con un OR:

        - filas NUEVAS      -> idEntrega > (MAX(idEntrega) capturado antes de sincronizar).
                               idEntrega es IDENTITY, asi que esto es un rango sobre la PK
                               clustered: cuesta nada.
        - filas ACTUALIZADAS -> audFecha >= (GETDATE() capturado antes de sincronizar).

     Ruido conocido, asumido a proposito:
        - Si mientras corre la sincronizacion (~1,5 s) un chofer marca una entrega, el ABM
          ACCION 'B' tambien pone audFecha = getdate() en esa fila y suma al conteo. Son unidades
          sobre ~400. No vale la pena complicar el SP para evitarlo.
        - trch_Entregas tiene 27.674 filas y un solo indice (la PK clustered sobre idEntrega),
          asi que el conteo por audFecha es un scan completo. Sobre 27 mil filas son pocos ms,
          despreciable al lado de los ~1.400 ms de la sincronizacion.

     Valor esperado: la sincronizacion trae 417 filas del SP remoto y el UPDATE toca TODAS las
     que ya existen (pone audFecha = GETDATE() aunque el dato no haya cambiado), asi que
     filasSincronizadas va a dar del orden de 400 a 430 y bastante estable -medido: en las
     ultimas 24 h el conjunto de filas con audFecha fresca fue de 429-. Un 0 no significa
     "no habia nada que hacer": significa que la consulta remota no devolvio filas, y eso hay
     que mirarlo.

   ---------------------------------------------------------------------------------------------
   ERRORES: SE LOGUEAN Y SE RELANZAN (a proposito)
   ---------------------------------------------------------------------------------------------
     Si la sincronizacion falla -el linked server caido, SAP no responde, un timeout- el SP:
        1. escribe una fila en trch_log (procedimiento = 'p_abm_trch_EntregasSync', estado
           'ERROR'), que es la misma tabla y el mismo patron que ya usa la ACCION 'C' del ABM;
        2. RELANZA el error con RAISERROR para que el que llamo se entere.

     NO se traga el error devolviendo filasSincronizadas = 0. Si lo hiciera, el scheduler creeria
     que sincronizo bien, actualizaria su marca de "ultima sincronizacion OK", y los choferes
     verian datos viejos indefinidamente convencidos de que estan al dia. Es exactamente la falla
     silenciosa que hay que evitar. Como este SP corre FUERA del request del chofer, que tire
     excepcion no rompe nada del lado del chofer: el chofer lee con 'Y', que no depende de esto.

     El INSERT en trch_log va guardado con IF (XACT_STATE() <> -1) para no agregar un segundo
     error encima del primero si la transaccion quedo condenada (puede pasar con errores de
     linked server dentro de una transaccion distribuida).

   ---------------------------------------------------------------------------------------------
   LO QUE EL LADO JAVA TIENE QUE GARANTIZAR (no lo garantiza este script)
   ---------------------------------------------------------------------------------------------
     a) UNA SOLA SINCRONIZACION A LA VEZ.
        El par UPDATE + INSERT del ABM no es atomico: el INSERT decide que insertar con un
        LEFT JOIN ... WHERE target.ItemCode IS NULL, asi que dos sincronizaciones que se pisan
        pueden insertar la misma fila dos veces. NO es teorico: hoy, en produccion, hay
        35 claves (docEntry, docNum, itemCode, db) repetidas, 77 filas involucradas, una de
        ellas repetida 5 veces. Es la marca que dejan las 15 sincronizaciones simultaneas que
        dispara el diseno actual.
        El diseno nuevo elimina la carrera por construccion -un solo llamador programado en vez
        de uno por request-, pero eso hay que sostenerlo del lado Java: el scheduler de Spring
        con fixedDelay ya serializa los ticks dentro de una JVM, y cualquier disparo manual tiene
        que pasar por un tryLock/AtomicBoolean que descarte el pedido si ya hay uno corriendo
        (descartar, NO encolar: encolar solo agenda una sincronizacion redundante).
        NO se puso un sp_getapplock adentro del SP a proposito: un applock con @LockOwner =
        'Session' sobrevive a un timeout de cliente y queda pegado en la conexion del pool, y el
        modo de falla resultante -ninguna sincronizacion vuelve a correr nunca, choferes con
        datos congelados- es peor que el que evita. Si algun dia la app corre en mas de una JVM,
        ahi si conviene agregarlo, con una estrategia explicita de liberacion.
     b) LLAMARLO FUERA DE CUALQUIER TRANSACCION (sin @Transactional).
        El SP no abre transaccion, igual que hoy. Envolverlo en una transaccion mantendria los
        locks de trch_Entregas tomados durante todo el viaje al linked server (~1,5 s) y le
        pegaria justo a las lecturas de los choferes, que es lo que este cambio viene a arreglar.
     c) QUE UNA SINCRONIZACION LENTA O CAIDA NO ROMPA EL REQUEST DEL CHOFER.
        El chofer lee con 'Y'. 'Y' no llama a nada remoto. Si la sincronizacion esta caida, el
        chofer ve datos de la ultima sincronizacion buena en vez de ver un error.

   ---------------------------------------------------------------------------------------------
   COMO VOLVER ATRAS
   ---------------------------------------------------------------------------------------------
     El script es puramente aditivo, asi que no hay nada urgente que revertir: mientras el DAO
     siga mandando @ACCION = 'A' el comportamiento es identico al de antes de correrlo.
        - Para volver el backend al comportamiento viejo: cambiar 'Y' por 'A' en el DAO. Nada mas.
        - Para sacar los objetos: DROP PROCEDURE dbo.p_abm_trch_EntregasSync; y volver a correr
          el ALTER de p_list_trch_Entregas sin el bloque 'Y'.

   ---------------------------------------------------------------------------------------------
   COMPATIBILIDAD Y FIDELIDAD DEL TEXTO
   ---------------------------------------------------------------------------------------------
     SQL Server 2008 (la base local BOSQUE-2_0 es 2008; el linked server SRV_2022 es otra cosa y
     no se toca aca). Nada de CREATE OR ALTER, THROW, IIF, CONCAT ni OFFSET/FETCH.

     El ALTER de mas abajo reproduce INTEGRA la definicion viva de p_list_trch_Entregas leida con
     OBJECT_DEFINITION (23.782 caracteres, incluidas las acciones L, A, B, C, D, E, F y G y el
     bloque comentado que quedo dentro de la ACCION 'B'), y le agrega SOLO el bloque 'Y' al final.
     Se verifico programaticamente: deshaciendo los dos unicos cambios -CREATE -> ALTER y el
     bloque 'Y'- el texto vuelve a ser byte a byte el que esta desplegado. El script es
     idempotente, se puede correr las veces que haga falta.

   ---------------------------------------------------------------------------------------------
   VERIFICACION DESPUES DE CORRERLO (al final del archivo estan las consultas listas para copiar)
   ---------------------------------------------------------------------------------------------
       EXEC p_list_trch_Entregas @u_Chofer = 104, @ACCION = 'Y';   -- rapido, no sincroniza
       EXEC p_abm_trch_EntregasSync @ACCION = 'S';                 -- ~1,5 s, devuelve 1 fila

   ============================================================================================= */

SET NOCOUNT ON;

/* =============================================================================================
   1) p_list_trch_Entregas   ·   ACCION NUEVA 'Y'  (leer sin sincronizar)
   ---------------------------------------------------------------------------------------------
   Cuerpo copiado LITERAL de la definicion viva; lo unico agregado es el bloque 'Y' del final.
   ============================================================================================= */
GO

/* =============================================================================================
   Author:        Marcelo Jaimes
   Date:          10/08/2026

   ============================================================================================= */

ALTER PROCEDURE [dbo].[p_list_trch_Entregas]

 @idEntrega bigint = NULL
, @docEntry bigint = NULL
, @docNum int = NULL
, @docNumF int = NULL
, @factura int = NULL
, @docDate date = NULL
, @docTime varchar(6) = NULL
, @cardCode varchar(10) = NULL
, @cardName varchar(250) = NULL
, @addressEntregaFac varchar(500) = NULL
, @addressEntregaMat varchar(500) = NULL
, @vendedor varchar(50) = NULL
, @u_Chofer int = NULL
, @itemCode varchar(50) = NULL
, @dscription varchar(250) = NULL
, @whsCode int = NULL
, @quantity varchar(10) = NULL
, @openQty int = NULL
, @db varchar(10) = NULL
, @valido varchar(10) = NULL
, @peso decimal(19,6) = NULL
, @cochePlaca varchar(25) = NULL
, @prioridad varchar(25) = NULL
, @tipo varchar(30) = NULL
, @obsF varchar(500) = NULL
, @fueEntregado int = NULL
, @fechaEntrega varchar(25) = NULL
, @latitud decimal(19, 6) = NULL
, @longitud decimal(19, 6) = NULL
, @direccionEntrega varchar(500) = NULL
, @obs varchar(500) = NULL
, @codSucursalChofer int = NULL
, @codCiudadChofer int = NULL

, @audUsuario bigint = NULL
, @ACCION VARCHAR(1) = NULL
, @codEmpleado INT = NULL
, @fechaIni DATE = NULL
, @fechaFin DATE = NULL
, @codSucursal INT = NULL



AS
BEGIN

	SET NOCOUNT ON;

	 -- =====LISTAR==================================
	 IF(@ACCION = 'L')
	 BEGIN
		 SELECT
			 idEntrega,
			 docEntry,
			 docNum,
			 factura,
			 docDate,
			 docTime,
			 cardCode,
			 cardName,
			 addressEntregaFac,
			 addressEntregaMat,
			 vendedor,
			 u_Chofer,
			 itemCode,
			 dscription,
			 whsCode,
			 quantity,
			 openQty,
			 db,
			 valido, 
			 fueEntregado,
			 fechaEntrega,
			 latitud,
			 longitud,
			 direccionEntrega,
			 obs,
			 codSucursalChofer,
			 codCiudadChofer,
			 audUsuario,
			 audFecha
		 FROM trch_Entregas
		 WHERE (@idEntrega IS NULL OR @idEntrega = idEntrega) 
		AND (@docEntry IS NULL OR @docEntry = docEntry) 
		AND (@docNum IS NULL OR @docNum = docNum) 
		AND (@factura IS NULL OR @factura = factura) 
		AND (@docDate IS NULL OR @docDate = docDate) 
		AND (@docTime IS NULL OR @docTime = docTime) 
		AND (@cardCode IS NULL OR @cardCode = cardCode) 
		AND (@cardName IS NULL OR @cardName = cardName) 
		AND (@addressEntregaFac IS NULL OR @addressEntregaFac = addressEntregaFac) 
		AND (@addressEntregaMat IS NULL OR @addressEntregaMat = addressEntregaMat) 
		AND (@vendedor IS NULL OR @vendedor = vendedor) 
		AND (@u_Chofer IS NULL OR @u_Chofer = u_Chofer) 
		AND (@itemCode IS NULL OR @itemCode = itemCode) 
		AND (@dscription IS NULL OR @dscription = dscription) 
		AND (@whsCode IS NULL OR @whsCode = whsCode) 
		AND (@quantity IS NULL OR @quantity = quantity) 
		AND (@openQty IS NULL OR @openQty = openQty) 
		AND (@db IS NULL OR @db = db) 
		AND (@fueEntregado IS NULL OR @fueEntregado = fueEntregado) 
		AND (@fechaEntrega IS NULL OR @fechaEntrega = fechaEntrega) 
		AND (@latitud IS NULL OR @latitud = latitud) 
		AND (@longitud IS NULL OR @longitud = longitud) 
		AND (@direccionEntrega IS NULL OR @direccionEntrega = direccionEntrega) 
		AND (@audUsuario IS NULL OR @audUsuario = audUsuario) 
		

	 END


	 -- ===================== LISTARA LAS ENTREGAS POR EMPLEADO =============================
	
	IF( @ACCION = 'A' )
	BEGIN
		
		set nocount on;
		execute p_abm_trch_Entregas @audUsuario = -1, @ACCION='A'

		SELECT
			 t1.idEntrega,
			 t1.docEntry,
			 t1.docNum,
			 isnull(t1.factura,0) as factura,
			 t1.docDate,
			 isnull(t1.docTime,0) as docTime,
			 t1.cardCode,
			 t1.cardName,
			 t1.addressEntregaFac,
			 t1.addressEntregaMat,
			 isnull(t1.vendedor,''),
			 t1.u_Chofer,
			 isnull(t1.itemCode,''),
			 isnull(t1.dscription,''),
			 isnull(t1.whsCode,''),
			 t1.quantity,
			 t1.openQty,
			 t1.db,
			 t1.valido, 
			 isnull(t1.fueEntregado,0) as fueEntregado,
			 isnull(t1.fechaEntrega, getdate()) as fechaEntrega,
			 isnull(t1.latitud,0)as latitud,
			 isnull(t1.longitud,0)as logitud,
			 isnull(t1.direccionEntrega,''),
			 t1.obs,
			 isnull(t1.tipo,'Inicio/Fin'),
			 isnull(t1.obsF,''),
			 t1.audUsuario
		 FROM trch_Entregas t1
		 WHERE t1.u_Chofer = @u_Chofer --codEmpleado
		 AND t1.valido IN( 'V', 'N') --validos y NO cancelados
		 AND isnull(t1.fueEntregado,0) = 0 AND t1.docEntry  > 0
		 ORDER BY t1.docDate desc

	END


	-- =================== PARA LISTAR LAS ENTREGAS EN UNA DETERMINADA FECHA ======================
	IF( @ACCION = 'B' )
	BEGIN
		
		/*SELECT DISTINCT
			t1.docEntry,
			ISNULL(t1.factura, 0) AS factura,
			t1.cardCode,
			t1.cardName,
    
			-- Formatear fechaNota como 'dd/MM/yyyy HH:mm:ss' para visualización
			CONVERT(VARCHAR(10), DATEADD(
				MINUTE, 
				(t1.docTime / 100) * 60 + (t1.docTime % 100), 
				CAST(t1.docDate AS DATETIME)
			), 103) + ' ' + 
			CONVERT(VARCHAR(8), DATEADD(
				MINUTE, 
				(t1.docTime / 100) * 60 + (t1.docTime % 100), 
				CAST(t1.docDate AS DATETIME)
			), 108) AS fechaNota,
    
			-- Formatear fechaEntrega como 'dd/MM/yyyy HH:mm:ss'
			CONVERT(VARCHAR, CAST(t1.fechaEntrega AS DATETIME), 103) + ' ' + 
			CONVERT(VARCHAR, CAST(t1.fechaEntrega AS DATETIME), 108) as fechaEntrega,
    
			-- Calcular la diferencia en minutos usando tipos datetime
			/*DATEDIFF(
				MINUTE, 
				DATEADD(
					MINUTE, 
					(t1.docTime / 100) * 60 + (t1.docTime % 100), 
					CAST(t1.docDate AS DATETIME)
				), 
				t1.fechaEntrega
			) AS DiferenciaEnMinutos,*/
			DATEDIFF(MINUTE, 
				(SELECT TOP 1 fechaEntrega 
				 FROM trch_Entregas t2 
				 WHERE t2.fechaEntrega < t1.fechaEntrega 
				 ORDER BY t2.fechaEntrega DESC), 
				t1.fechaEntrega) as diferenciaMinutos,
    
			t1.direccionEntrega,
			t1.addressEntregaFac,
			t1.addressEntregaMat,
			t1.vendedor,
			t1.db,
			t1.u_Chofer,
    
			-- Obtener el nombre completo del chofer
			(
				SELECT 
					tx.nombres + ' ' + tx.apPaterno + ' ' + tx.apMaterno 
				FROM trh_persona tx 
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona 
				WHERE ty.codEmpleado = t1.u_Chofer 
			) AS nombreCompleto
			,t1.latitud
			,t1.longitud
			,t1.obs
			,t1.DocNumF
			,Sum(t1.peso) as peso
			,t2.marca +' '+ t2.clase +', ' +t1.cochePlaca as coche
			--,t1.cochePlaca
			,Case When t1.prioridad = 'N' Then 'Normal' When t1.prioridad ='A' Then 'Alta' else '-Proridad Desconocida-' End as prioridad
			,t1.tipo
			--( select top 1  DATEADD(MINUTE,(tx.docTime / 100) * 60 + (tx.docTime % 100),CAST(tx.docDate AS DATETIME)) from trch_Entregas tx where cast(tx.fechaEntrega as date) = '2024-10-05' and tx.u_Chofer = t1.u_Chofer and tx.docEntry > 0 ) as t
		FROM 
			trch_Entregas t1 left join tac_coche t2 on t1.cochePlaca = t2.placa
		WHERE CAST(t1.fechaEntrega as DATE) = @fechaEntrega
		AND t1.valido in( 'V', 'N')
		AND t1.u_Chofer =  @u_Chofer
		AND isnull(t1.fueEntregado,0) = 1
		Group by t1.docEntry, t1.factura, t1.cardCode, t1.cardName,t1.docTime, t1.docDate, t1.fechaEntrega, t1.direccionEntrega, t1.addressEntregaFac, t1.addressEntregaMat, t1.vendedor
				 ,t1.db, t1.u_Chofer, t1.latitud, t1.longitud, t1.obs, t1.DocNumF, t1.cochePlaca, t1.prioridad, t1.tipo, t2.marca, t2.clase
		order by fechaEntrega*/


		SELECT 
			t1.docEntry,
			ISNULL(t1.factura, 0) AS factura,
			t1.cardCode,
			t1.cardName,

			-- Formatear fechaNota como 'dd/MM/yyyy HH:mm:ss' para visualización
			CONVERT(VARCHAR(10), DATEADD(
				MINUTE, 
				(t1.docTime / 100) * 60 + (t1.docTime % 100), 
				CAST(t1.docDate AS DATETIME)
			), 103) + ' ' + 
			CONVERT(VARCHAR(8), DATEADD(
				MINUTE, 
				(t1.docTime / 100) * 60 + (t1.docTime % 100), 
				CAST(t1.docDate AS DATETIME)
			), 108) AS fechaNota,

			-- Formatear fechaEntrega como 'dd/MM/yyyy HH:mm:ss'
			CONVERT(VARCHAR, CAST(t1.fechaEntrega AS DATETIME), 103) + ' ' + 
			CONVERT(VARCHAR, CAST(t1.fechaEntrega AS DATETIME), 108) AS fechaEntrega,

			-- Calcular la diferencia en minutos según las reglas especificadas
			CASE 
				WHEN t1.docEntry = -1 THEN 0
				WHEN t1.docEntry = 0 THEN DATEDIFF(MINUTE,
					(
						SELECT TOP 1 tInicio.fechaEntrega
						FROM trch_Entregas tInicio
						WHERE tInicio.u_Chofer = t1.u_Chofer
						  AND tInicio.docEntry = -1
						  AND tInicio.fechaEntrega <= t1.fechaEntrega
						  AND CAST(tInicio.fechaEntrega AS DATE) = CAST(t1.fechaEntrega AS DATE)
						ORDER BY tInicio.fechaEntrega DESC
					),
					t1.fechaEntrega
				)
				ELSE DATEDIFF(MINUTE,
					(
						SELECT MAX(tPrev.fechaEntrega)
						FROM trch_Entregas tPrev
						WHERE tPrev.u_Chofer = t1.u_Chofer
						  AND tPrev.fechaEntrega < t1.fechaEntrega
						  AND CAST(tPrev.fechaEntrega AS DATE) = CAST(t1.fechaEntrega AS DATE)
					),
					t1.fechaEntrega
				)
			END AS diferenciaMinutos,

			t1.direccionEntrega,
			t1.addressEntregaFac,
			t1.addressEntregaMat,
			t1.vendedor,
			t1.db,
			t1.u_Chofer,

			-- Obtener el nombre completo del chofer
			(
				SELECT 
					tx.nombres + ' ' + tx.apPaterno + ' ' + tx.apMaterno 
				FROM trh_persona tx 
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona 
				WHERE ty.codEmpleado = t1.u_Chofer 
			) AS nombreCompleto,

			t1.latitud,
			t1.longitud,
			t1.obs,
			t1.DocNumF,
			SUM(t1.peso) AS peso,
			t2.marca + ' ' + t2.clase + ', ' + t1.cochePlaca AS coche,

			-- Determinar la prioridad
			CASE 
				WHEN t1.prioridad = 'N' THEN 'Normal' 
				WHEN t1.prioridad = 'A' THEN 'Alta' 
				ELSE '-Prioridad Desconocida-' 
			END AS prioridad,

			t1.tipo

		FROM 
			trch_Entregas t1 
			LEFT JOIN tac_coche t2 ON t1.cochePlaca = t2.placa

		WHERE 
			CAST(t1.fechaEntrega AS DATE) = @fechaEntrega
			AND t1.valido IN ('V', 'N')
			AND t1.u_Chofer = @u_Chofer
			AND ISNULL(t1.fueEntregado, 0) = 1

		GROUP BY 
			t1.docEntry, 
			t1.factura, 
			t1.cardCode, 
			t1.cardName,
			t1.docTime, 
			t1.docDate, 
			t1.fechaEntrega, 
			t1.direccionEntrega, 
			t1.addressEntregaFac, 
			t1.addressEntregaMat, 
			t1.vendedor,
			t1.db, 
			t1.u_Chofer, 
			t1.latitud, 
			t1.longitud, 
			t1.obs, 
			t1.DocNumF, 
			t1.cochePlaca, 
			t1.prioridad, 
			t1.tipo, 
			t2.marca, 
			t2.clase

		ORDER BY CAST(t1.fechaEntrega AS DATETIME)




	END


	--===================== OBTENER LA LOCALIZACION DE UN CHOFER EN DETERMINADO DIA ========================
	IF( @ACCION = 'C' )
	BEGIN
		SELECT DISTINCT
			t1.latitud
			,t1.longitud
			,t1.direccionEntrega
		FROM
		trch_Entregas t1
		Where t1.u_Chofer = @u_Chofer
		And t1.fechaEntrega = @fechaEntrega
	END


	--================= LISTARA A LOS EMPLEADOS ACTIVOS QUE SON SOLO CHOFERES ===============================
	IF(@ACCION='D')
	BEGIN
		SELECT  DISTINCT	
			ec.codEmpleado
			,(p.apPaterno + ' '+ p.apMaterno+' '+p.nombres  ) as nombreCompleto
			,c.descripcion  as cargo
	     FROM trh_empleadoCargo ec join tb_cargo_sucursal cs on ec.codCargoSucursal = cs.codCargoSucursal
							  join tb_sucursal s on s.codSucursal = cs.codSucursal
							  join tb_empresa empr on empr.codEmpresa = s.codEmpresa
							  join trh_cargo c on c.codCargo = cs.codCargo
							  join tb_nivelJerarquico nj on nj.codNivel = c.codNivel
							  join tb_cargo_sucursal csb on ec.codCargoSucPlanilla = csb.codCargoSucursal
							  join tb_sucursal sb on sb.codSucursal = csb.codSucursal
							  join tb_empresa emprb on emprb.codEmpresa = sb.codEmpresa
							  join trh_cargo cb on cb.codCargo = csb.codCargo
							  join tb_nivelJerarquico njb on njb.codNivel = cb.codNivel
							  join tb_empleado empl on empl.codEmpleado = ec.codEmpleado
							  join trh_persona p on p.codPersona = empl.codPersona
							  join tb_relEmplEmpr ree on ree.codEmpleado=empl.codEmpleado
							  left join tb_usuario u on u.codEmpleado=empl.codEmpleado
		WHERE ec.fechaInicio = (Select MAX(fechaInicio) From trh_empleadoCargo Where codEmpleado=ec.codEmpleado)
		AND ree.esActivo=1 --Solo los empleados activos
		AND c.descripcion Like '%CHOFER%'
		--AND s.codSucursal = 23
		--AND ec.codEmpleado in (select distinct codEmpleado from tac_organigrama)
		ORDER BY  nombreCompleto 
	END


	-- =========== PARA LISTAR A LOS CHOFERES Y SUS RUTAS SI FUERON COMPLETADAS O NO ENTRE FECHAS Y POR SUCURSAL ==================
	IF( @ACCION = 'E' )
	BEGIN
		

			
			IF( @codEmpleado not in( 65, 32, 172) )-- alvaro aguilar, erick almendras, marcelo jaimes
			BEGIN
			
				;WITH RouteCTE AS (
				SELECT 
					t1.u_Chofer,
					t1.fechaEntrega,
					t1.docEntry,
					t1.obs,
					t1.CardCode,  -- Agregamos CardCode
					CAST(t1.fechaEntrega AS DATE) as RouteDate,
					ROW_NUMBER() OVER (PARTITION BY t1.u_Chofer, CAST(t1.fechaEntrega AS DATE) ORDER BY t1.fechaEntrega) as EventOrder,
					CASE 
						WHEN t1.docEntry = -1  THEN 1
						WHEN t1.docEntry = 0  THEN 2
					END as EventType
				FROM trch_Entregas t1
				WHERE YEAR(t1.docDate) > 2024
				AND CAST(t1.fechaEntrega as date) between @fechaIni and @fechaFin
				AND t1.docEntry IN (-1, 0)
			),
			RouteNumbers AS (
				SELECT 
					*,
					(SELECT COUNT(*) 
					 FROM RouteCTE r2 
					 WHERE r2.u_Chofer = r1.u_Chofer 
					 AND r2.EventType = 1 
					 AND r2.RouteDate = r1.RouteDate
					 AND r2.fechaEntrega <= r1.fechaEntrega) as DailyRouteNumber
				FROM RouteCTE r1
			)
			SELECT 
				0 as ord,
				r.u_Chofer as Codchofer,
				tp.nombres +' '+ tp.apPaterno+' '+ tp.apMaterno as nombreCompleto,
				CONVERT(VARCHAR, CAST(r.RouteDate AS DATETIME), 103) as Fecha,
				r.DailyRouteNumber as RutaDiaria,
				MIN(CASE WHEN r.EventType = 1 THEN CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 103) + ' ' + CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 108) END) as InicioRuta,
				MAX(CASE WHEN r.EventType = 2 THEN CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 103) + ' ' + CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 108) END) as FinRuta,
				CASE 
					WHEN MAX(CASE WHEN r.EventType = 2 THEN 1 ELSE 0 END) = 0 THEN 'Incompleto o en ruta'
					WHEN MAX(CASE WHEN r.CardCode = 'SB' THEN 1 ELSE 0 END) = 1 THEN 'Completado Por Sistema'
					ELSE 'Completo'
				END as EstatusRuta,
				CASE 
					WHEN MAX(CASE WHEN r.EventType = 2 THEN 1 ELSE 0 END) = 0 THEN 0
					WHEN MAX(CASE WHEN r.CardCode = 'SB' THEN 1 ELSE 0 END) = 1 THEN 2
					ELSE 1
				END as flag
			FROM RouteNumbers r 
			JOIN tb_empleado te on te.codEmpleado = r.u_Chofer
			JOIN trh_persona tp on tp.codPersona = te.codPersona
			GROUP BY r.u_Chofer, r.RouteDate, r.DailyRouteNumber, tp.nombres, tp.apPaterno, tp.apMaterno


			UNION ALL

			SELECT 
				-1 as ord,
				r.u_Chofer,
				'Total Rutas del Periodo de '+ tp.nombres +' '+ tp.apPaterno+' '+ tp.apMaterno,
				NULL as RouteDate,
				COUNT(DISTINCT CAST(r.RouteDate AS VARCHAR(10)) + CAST(r.DailyRouteNumber AS VARCHAR(2))) as TotalRutas,
				NULL as InicioRuta,
				NULL as FinRuta,
				'' as EstatusRuta,
				-1 as flag
			FROM RouteNumbers r 
			JOIN tb_empleado te on te.codEmpleado = r.u_Chofer
			JOIN trh_persona tp on tp.codPersona = te.codPersona
			
			GROUP BY r.u_Chofer, tp.nombres, tp.apPaterno, tp.apMaterno
			ORDER BY Codchofer, ord desc, Fecha;
			
			END
			
			ELSE
			BEGIN
				;WITH RouteCTE AS (
				SELECT 
					t1.u_Chofer,
					t1.fechaEntrega,
					t1.docEntry,
					t1.obs,
					t1.CardCode,  -- Agregamos CardCode
					CAST(t1.fechaEntrega AS DATE) as RouteDate,
					ROW_NUMBER() OVER (PARTITION BY t1.u_Chofer, CAST(t1.fechaEntrega AS DATE) ORDER BY t1.fechaEntrega) as EventOrder,
					CASE 
						WHEN t1.docEntry = -1  THEN 1
						WHEN t1.docEntry = 0  THEN 2
					END as EventType
				FROM trch_Entregas t1
				WHERE YEAR(t1.docDate) > 2024
				AND CAST(t1.fechaEntrega as date) between @fechaIni and @fechaFin
				AND t1.docEntry IN (-1, 0)
			),
			RouteNumbers AS (
				SELECT 
					*,
					(SELECT COUNT(*) 
					 FROM RouteCTE r2 
					 WHERE r2.u_Chofer = r1.u_Chofer 
					 AND r2.EventType = 1 
					 AND r2.RouteDate = r1.RouteDate
					 AND r2.fechaEntrega <= r1.fechaEntrega) as DailyRouteNumber
				FROM RouteCTE r1
			)
			SELECT 
				0 as ord,
				r.u_Chofer as Codchofer,
				tp.nombres +' '+ tp.apPaterno+' '+ tp.apMaterno as nombreCompleto,
				CONVERT(VARCHAR, CAST(r.RouteDate AS DATETIME), 103)as Fecha,
				r.DailyRouteNumber as RutaDiaria,
				MIN(CASE WHEN r.EventType = 1 THEN CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 103) + ' ' + CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 108) END) as InicioRuta,
				MAX(CASE WHEN r.EventType = 2 THEN CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 103) + ' ' + CONVERT(VARCHAR, CAST(r.fechaEntrega AS DATETIME), 108) END) as FinRuta,
				CASE 
					WHEN MAX(CASE WHEN r.EventType = 2 THEN 1 ELSE 0 END) = 0 THEN 'Incompleto o en ruta'
					WHEN MAX(CASE WHEN r.CardCode = 'SB' THEN 1 ELSE 0 END) = 1 THEN 'Completado Por Sistema'
					ELSE 'Completo'
				END as EstatusRuta,
				CASE 
					WHEN MAX(CASE WHEN r.EventType = 2 THEN 1 ELSE 0 END) = 0 THEN 0
					WHEN MAX(CASE WHEN r.CardCode = 'SB' THEN 1 ELSE 0 END) = 1 THEN 2
					ELSE 1
				END as flag
			FROM RouteNumbers r 
			JOIN tb_empleado te on te.codEmpleado = r.u_Chofer
			JOIN trh_persona tp on tp.codPersona = te.codPersona
			WHERE (SELECT Case when codCiudad = 3 then 1 else codCiudad end from tb_sucursal where codSucursal = @codSucursal) = (
				SELECT DISTINCT Case when s.codCiudad = 3 then 1 else s.codCiudad end
				FROM trh_empleadoCargo ec 
				JOIN tb_cargo_sucursal cs on ec.codCargoSucursal = cs.codCargoSucursal
				JOIN tb_sucursal s on s.codSucursal = cs.codSucursal
				JOIN tb_empleado empl on empl.codEmpleado = ec.codEmpleado		 
				WHERE ec.fechaInicio = (Select MAX(fechaInicio) From trh_empleadoCargo Where codEmpleado=ec.codEmpleado) 
				AND empl.codEmpleado = r.u_Chofer 
			)
			GROUP BY r.u_Chofer, r.RouteDate, r.DailyRouteNumber, tp.nombres, tp.apPaterno, tp.apMaterno

			UNION ALL

			SELECT 
				-1 as ord,
				r.u_Chofer,
				'Total Rutas del Periodo de '+ tp.nombres +' '+ tp.apPaterno+' '+ tp.apMaterno,
				NULL as RouteDate,
				COUNT(DISTINCT CAST(r.RouteDate AS VARCHAR(10)) + CAST(r.DailyRouteNumber AS VARCHAR(2))) as TotalRutas,
				NULL as InicioRuta,
				NULL as FinRuta,
				'' as EstatusRuta,
				-1 as flag
			FROM RouteNumbers r 
			JOIN tb_empleado te on te.codEmpleado = r.u_Chofer
			JOIN trh_persona tp on tp.codPersona = te.codPersona
			WHERE (SELECT Case when codCiudad = 3 then 1 else codCiudad end from tb_sucursal where codSucursal = @codSucursal) = (
				SELECT Case when s.codCiudad = 3 then 1 else s.codCiudad end
				FROM trh_empleadoCargo ec 
				JOIN tb_cargo_sucursal cs on ec.codCargoSucursal = cs.codCargoSucursal
				JOIN tb_sucursal s on s.codSucursal = cs.codSucursal
				JOIN tb_empleado empl on empl.codEmpleado = ec.codEmpleado		 
				WHERE ec.fechaInicio = (Select MAX(fechaInicio) From trh_empleadoCargo Where codEmpleado=ec.codEmpleado) 
				AND empl.codEmpleado = r.u_Chofer 
			)
			GROUP BY r.u_Chofer, tp.nombres, tp.apPaterno, tp.apMaterno
			ORDER BY Codchofer, ord desc, Fecha;

			END
			

	END


	-- =================== LISTARA LOS PENDIENTES DE ENTREGA DEL SAP DE IPX, ESP Y PROD PAP ==============================
	IF( @ACCION = 'F' )
	BEGIN

			DECLARE @sqlPE    NVARCHAR(MAX);  
			DECLARE @tempPE   TABLE (  
				empresa	VARCHAR(15),  
				docEntry BIGINT,
				cardName VARCHAR(250),
				docDate DATE,
				horaCreacion VARCHAR(10),
				weight DECIMAL(19,6),
				cantidad DECIMAL(19,6),
				comments VARCHAR(500),
				direccionEntrega VARCHAR(500),
				vendedor VARCHAR(250),
				sistema VARCHAR(50),
				docNum VARCHAR(50),
				seriesName VARCHAR(25),
				tipoEntrega VARCHAR(50)
			);  
  
			 
			SET @sqlPE = N'SELECT * FROM OPENQUERY(SRV_2022,   
				''EXEC [CONEXION].[dbo].[p_list_PendientesEntrega] @ACCION = ''''A'''''')';  
  
			INSERT INTO @tempPE
			EXEC sp_executesql @sqlPE

			SELECT 
				t1.* 
			FROM @tempPE t1
			
	END


	-- ============ DATOS DE UNA ENTREGA PARA EL AVISO POR WHATSAPP (docEntry + db) ================
	-- Devuelve UNA sola fila. "Una entrega" = un (docEntry, db); en trch_Entregas ese documento
	-- tiene N filas, una por itemCode, por eso se agrupa y se informa cantidadItems = COUNT(*).
	-- No filtra fueEntregado ni valido a proposito: se llama justo despues de marcar la entrega.
	-- Excluye los marcadores de ruta (docEntry = -1) y el cierre de ruta (docEntry = 0).
	IF( @ACCION = 'G' )
	BEGIN
		SELECT
			x.docEntry,
			x.db,
			x.docNum,
			x.factura,
			x.cardCode,
			x.cardName,
			x.vendedor,
			x.uChofer,

			-- Nombre del chofer: mismo join que usa la ACCION 'B' (u_Chofer = tb_empleado.codEmpleado)
			ISNULL((
				SELECT ISNULL(tx.nombres, '') + ' ' + ISNULL(tx.apPaterno, '') + ' ' + ISNULL(tx.apMaterno, '')
				FROM trh_persona tx
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona
				WHERE ty.codEmpleado = x.uChofer
			), '') AS nombreChofer,

			x.direccionEntrega,

			-- 'dd/MM/yyyy HH:mm' (el estilo 108 da HH:mm:ss y VARCHAR(5) lo corta a HH:mm)
			CASE
				WHEN x.fechaEntregaRaw IS NULL THEN ''
				ELSE CONVERT(VARCHAR(10), x.fechaEntregaRaw, 103) + ' ' + CONVERT(VARCHAR(5), x.fechaEntregaRaw, 108)
			END AS fechaEntrega,

			x.obs,
			x.cantidadItems,
			x.tipo
		FROM (
			SELECT
				t1.docEntry                                                          AS docEntry,
				MAX(t1.db)                                                           AS db,
				MAX(ISNULL(t1.docNum, 0))                                            AS docNum,
				MAX(ISNULL(t1.factura, 0))                                           AS factura,
				MAX(ISNULL(t1.cardCode, ''))                                         AS cardCode,
				MAX(ISNULL(t1.cardName, ''))                                         AS cardName,
				MAX(ISNULL(t1.vendedor, ''))                                         AS vendedor,
				MAX(ISNULL(t1.u_Chofer, 0))                                          AS uChofer,
				MAX(ISNULL(t1.direccionEntrega,
				     ISNULL(t1.addressEntregaMat,
				     ISNULL(t1.addressEntregaFac, ''))))                             AS direccionEntrega,
				MAX(t1.fechaEntrega)                                                 AS fechaEntregaRaw,
				MAX(ISNULL(t1.obs, ''))                                              AS obs,
				COUNT(*)                                                             AS cantidadItems,
				MAX(ISNULL(t1.tipo, ''))                                             AS tipo
			FROM trch_Entregas t1
			WHERE t1.docEntry = @docEntry
			  AND t1.db = @db
			  AND t1.docEntry > 0
			-- Se agrupa por CLIENTE, no solo por documento. Agrupar solo por docEntry y sacar
			-- cada columna con un MAX() independiente mezcla filas: el cardCode puede venir de
			-- una fila y el cardName/direccion de otra. Hay 18 pares (docEntry, db) con mas de
			-- un cardCode en datos reales, y en 15 de ellos el nombre no le corresponde al
			-- codigo. Como el telefono se resuelve por cardCode, mezclar significa mandarle a
			-- un cliente los datos de otro. Agrupando asi, un documento ambiguo devuelve N
			-- filas y el DAO se niega a notificar (ver obtenerDatosNotificacion).
			GROUP BY t1.docEntry, t1.db, ISNULL(t1.cardCode, '')
		) x
	END

	-- ========== LISTAR LAS ENTREGAS POR EMPLEADO **SIN SINCRONIZAR CON SAP** =====================
	-- Clon EXACTO de la ACCION 'A' de mas arriba, menos la unica linea que sincroniza:
	--     execute p_abm_trch_Entregas ... @ACCION='A'   (el llamado al ABM que va al SAP)
	-- Mismas 28 columnas, mismo orden, mismas expresiones isnull(), mismo WHERE, mismo ORDER BY.
	-- El RowMapper de EntregaChoferDao.listarEntregasXEmpleado lee por INDICE (rs.getXxx(1..28)),
	-- asi que 'Y' es intercambiable con 'A' sin tocar una linea de Java.
	-- Quien llama a 'Y' se hace cargo de que alguien mas corra la sincronizacion
	-- (p_abm_trch_EntregasSync @ACCION='S', mas abajo en este mismo script).
	IF( @ACCION = 'Y' )
	BEGIN
		
		set nocount on;

		SELECT
			 t1.idEntrega,
			 t1.docEntry,
			 t1.docNum,
			 isnull(t1.factura,0) as factura,
			 t1.docDate,
			 isnull(t1.docTime,0) as docTime,
			 t1.cardCode,
			 t1.cardName,
			 t1.addressEntregaFac,
			 t1.addressEntregaMat,
			 isnull(t1.vendedor,''),
			 t1.u_Chofer,
			 isnull(t1.itemCode,''),
			 isnull(t1.dscription,''),
			 isnull(t1.whsCode,''),
			 t1.quantity,
			 t1.openQty,
			 t1.db,
			 t1.valido, 
			 isnull(t1.fueEntregado,0) as fueEntregado,
			 isnull(t1.fechaEntrega, getdate()) as fechaEntrega,
			 isnull(t1.latitud,0)as latitud,
			 isnull(t1.longitud,0)as logitud,
			 isnull(t1.direccionEntrega,''),
			 t1.obs,
			 isnull(t1.tipo,'Inicio/Fin'),
			 isnull(t1.obsF,''),
			 t1.audUsuario
		 FROM trch_Entregas t1
		 WHERE t1.u_Chofer = @u_Chofer --codEmpleado
		 AND t1.valido IN( 'V', 'N') --validos y NO cancelados
		 AND isnull(t1.fueEntregado,0) = 0 AND t1.docEntry  > 0
		 ORDER BY t1.docDate desc

	END


END
GO


/* =============================================================================================
   2) p_abm_trch_EntregasSync   ·   ACCION 'S'  (sincronizar con SAP, fuera del request)
   ---------------------------------------------------------------------------------------------
   Stub + ALTER: en SQL Server 2008 no existe CREATE OR ALTER, y CREATE PROCEDURE tiene que ser
   la primera sentencia del lote, asi que no se puede meter dentro de un IF.

   No lleva @audUsuario. El bloque 'A' de p_abm_trch_Entregas no usa ese parametro para nada
   (el UPDATE solo toca audFecha y el INSERT no escribe audUsuario), asi que un parametro que no
   se usa seria solo ruido. Se le pasa -1 fijo, exactamente el mismo valor que le pasa hoy la
   ACCION 'A' de p_list_trch_Entregas.

   Tampoco lleva GRANT: los SPs de este modulo no tienen GRANT explicito porque la aplicacion
   conecta con una cuenta que ya puede ejecutarlos.
   ============================================================================================= */

IF OBJECT_ID('dbo.p_abm_trch_EntregasSync', 'P') IS NULL
	EXEC('CREATE PROCEDURE dbo.p_abm_trch_EntregasSync AS BEGIN SELECT 1; END');
GO

ALTER PROCEDURE [dbo].[p_abm_trch_EntregasSync]

  @ACCION VARCHAR(1) = NULL

AS
BEGIN

	-- Obligatorio. El bloque 'A' del ABM hace un UPDATE y un INSERT sobre trch_Entregas; sin
	-- NOCOUNT ON esos dos "N rows affected" viajan al cliente como update counts ANTES del
	-- SELECT final y le arruinan el ResultSet a jdbcTemplate.
	SET NOCOUNT ON;

	-- Accion desconocida = error explicito, no un ResultSet vacio. Si devolviera cero filas,
	-- un queryForObject del lado Java reventaria con EmptyResultDataAccessException y el motivo
	-- real (se mando la accion equivocada) no apareceria en ningun lado.
	IF (@ACCION IS NULL OR @ACCION <> 'S')
	BEGIN
		RAISERROR(N'p_abm_trch_EntregasSync: @ACCION invalida. La unica accion soportada es ''S''.', 16, 1);
		RETURN;
	END


	-- ===================== SINCRONIZAR LA TABLA CON EL SAP ======================================
	IF (@ACCION = 'S')
	BEGIN

		DECLARE @maxIdAntes   bigint;
		DECLARE @inicioSync   datetime;
		DECLARE @filas        int;
		DECLARE @errMsg       nvarchar(2000);

		BEGIN TRY

			-- Marcas tomadas ANTES de sincronizar. Son las dos unicas formas de medir el efecto
			-- del ABM desde afuera: idEntrega es IDENTITY (detecta lo insertado, que queda con
			-- audFecha NULL porque el INSERT del ABM no escribe esa columna y la columna no
			-- tiene DEFAULT) y audFecha detecta lo actualizado (el UPDATE del ABM le pone
			-- GETDATE() a toda fila que empareja, haya cambiado o no).
			SELECT @maxIdAntes = ISNULL(MAX(idEntrega), 0) FROM trch_Entregas;
			SET @inicioSync = GETDATE();

			-- LA sincronizacion. Se delega, no se duplica: este es el mismo llamado, con los
			-- mismos parametros, que hoy hace la ACCION 'A' de p_list_trch_Entregas adentro del
			-- request del chofer. La unica diferencia es QUIEN y CUANDO lo llama.
			-- Dura ~1,5 s en caliente (417 filas del linked server SRV_2022).
			EXECUTE p_abm_trch_Entregas @audUsuario = -1, @ACCION = 'A';

			SELECT @filas = COUNT(*)
			FROM trch_Entregas
			WHERE idEntrega > @maxIdAntes		-- filas nuevas
			   OR audFecha  >= @inicioSync;		-- filas actualizadas

		END TRY
		BEGIN CATCH

			SET @errMsg = N'p_abm_trch_EntregasSync: ' + ERROR_MESSAGE()
						+ N' (nro '        + CAST(ERROR_NUMBER()   AS nvarchar(12))
						+ N', linea '      + CAST(ERROR_LINE()     AS nvarchar(12))
						+ N', severidad '  + CAST(ERROR_SEVERITY() AS nvarchar(12)) + N')';

			-- Guardado con XACT_STATE: si la transaccion quedo condenada (pasa con errores de
			-- linked server dentro de una transaccion distribuida) cualquier INSERT falla y
			-- taparia el error original con uno nuevo y menos util.
			IF (XACT_STATE() <> -1)
			BEGIN
				INSERT INTO trch_log (procedimiento, registros_insertados, estado, mensaje, error_detalle)
				VALUES ('p_abm_trch_EntregasSync', 0, 'ERROR', 'Fallo la sincronizacion con SAP', @errMsg);
			END

			-- Se RELANZA a proposito. Devolver filasSincronizadas = 0 haria que el scheduler
			-- creyera que sincronizo bien y marcara los datos como frescos: los choferes verian
			-- datos viejos sin que nadie se entere. Que tire excepcion no afecta al chofer, que
			-- lee con la ACCION 'Y' y no depende de esto.
			-- SQL Server 2008 no tiene THROW; se usa el formato '%s' en vez de pasar @errMsg
			-- como formato para que un '%' dentro del mensaje del error original no lo rompa.
			RAISERROR(N'%s', 16, 1, @errMsg);
			RETURN;

		END CATCH

		SELECT
			 ISNULL(@filas, 0)	AS filasSincronizadas
			,GETDATE()			AS fechaSync;

	END

END
GO


/* =============================================================================================
   VERIFICACION  (copiar y pegar en SSMS despues de correr el script; nada de esto se ejecuta)
   ---------------------------------------------------------------------------------------------

   1) La ACCION 'Y' quedo desplegada y la 'A' sigue intacta:

        SELECT
             CASE WHEN OBJECT_DEFINITION(OBJECT_ID('dbo.p_list_trch_Entregas')) LIKE '%@ACCION = ''Y''%'
                  THEN 'OK: existe la Y' ELSE 'FALTA la Y' END                       AS accionY,
             CASE WHEN OBJECT_DEFINITION(OBJECT_ID('dbo.p_list_trch_Entregas'))
                       LIKE '%execute p_abm_trch_Entregas @audUsuario = -1, @ACCION=''A''%'
                  THEN 'OK: la A sigue sincronizando' ELSE 'SE ROMPIO LA A' END      AS accionA,
             LEN(OBJECT_DEFINITION(OBJECT_ID('dbo.p_list_trch_Entregas')))           AS largo,
             CASE WHEN OBJECT_ID('dbo.p_abm_trch_EntregasSync','P') IS NULL
                  THEN 'FALTA el SP de sync' ELSE 'OK: existe el SP de sync' END     AS spSync;

        largo tiene que dar alrededor de 25.505 (antes eran 23.782). El numero exacto puede
        moverse un par de caracteres segun como el cliente SQL recorte los saltos de linea del
        lote; lo que importa es que crecio y que las dos primeras columnas digan OK.

   2) 'A' y 'Y' devuelven las mismas filas. Cambiar 104 por un chofer con pendientes:

        SET STATISTICS TIME ON;
        EXEC p_list_trch_Entregas @u_Chofer = 104, @ACCION = 'A';   -- ~1.500 ms (sincroniza)
        EXEC p_list_trch_Entregas @u_Chofer = 104, @ACCION = 'Y';   -- pocos ms (no sincroniza)
        SET STATISTICS TIME OFF;

        Las dos tienen que devolver la MISMA cantidad de filas y las mismas 28 columnas.
        OJO CON LA COLUMNA 21 (fechaEntrega): la expresion es isnull(fechaEntrega, getdate()) y
        estas filas son justamente las que todavia NO se entregaron (fueEntregado = 0), asi que
        fechaEntrega es NULL y la columna devuelve la hora de AHORA. Entre una corrida y otra va
        a dar distinto SIEMPRE, en las dos acciones por igual. No es un sintoma de nada.

        Cantidad esperada, calculada aparte con el mismo filtro que usan las dos acciones:

          SELECT COUNT(*) FROM trch_Entregas t1
          WHERE t1.u_Chofer = 104 AND t1.valido IN ('V','N')
            AND ISNULL(t1.fueEntregado,0) = 0 AND t1.docEntry > 0;

   3) El SP de sincronizacion:

        EXEC p_abm_trch_EntregasSync @ACCION = 'S';

        Una fila: filasSincronizadas del orden de 400, fechaSync = ahora. Tarda ~1,5 s.
        Y que la accion invalida falle claro en vez de devolver vacio:

          EXEC p_abm_trch_EntregasSync @ACCION = 'X';   -- tiene que dar error, no 0 filas

   4) Errores de sincronizacion registrados (tiene que estar vacio si todo va bien):

        SELECT TOP 50 * FROM trch_log
        WHERE procedimiento = 'p_abm_trch_EntregasSync'
        ORDER BY fecha_ejecucion DESC;

   5) DUPLICADOS: el termometro del cambio.
      La carrera entre sincronizaciones simultaneas ya dejo 35 claves repetidas / 77 filas
      (una repetida 5 veces). Con un solo llamador programado ese numero tiene que quedar
      CONGELADO. Si sigue creciendo despues de migrar el DAO a 'Y', quedo algun camino que
      sincroniza en paralelo (revisar quien mas llama a la ACCION 'A'):

        SELECT COUNT(*) AS clavesDuplicadas, SUM(n) AS filasEnClavesDup, MAX(n) AS maxRepeticiones
        FROM (
            SELECT docEntry, docNum, itemCode, db, COUNT(*) AS n
            FROM trch_Entregas
            WHERE docEntry > 0
            GROUP BY docEntry, docNum, itemCode, db
            HAVING COUNT(*) > 1
        ) d;

      (La limpieza de esas 77 filas es un trabajo aparte y destructivo: no va en este script.)

   ============================================================================================= */
