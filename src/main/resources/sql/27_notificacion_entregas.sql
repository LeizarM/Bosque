/* =============================================================================================
   27_notificacion_entregas.sql   ·   p_list_trch_Entregas   ·   ACCION nueva 'G'
   ---------------------------------------------------------------------------------------------
   >>> EJECUTAR EN SSMS CON UNA CUENTA QUE TENGA PERMISOS DDL (db_owner o ALTER en el
   >>> procedimiento) SOBRE LA BASE [BOSQUE-2_0]. La cuenta de solo lectura NO puede correrlo.

   Objetivo:
     Alimentar el aviso por WhatsApp al cliente. Se agrega UNA accion nueva al
     procedimiento existente, sin tocar ninguna de las ya desplegadas (L, A, B, C, D, E, F)
     y sin cambiar la firma de parametros (se reusan @docEntry y @db, que ya existian).

       ACCION 'G'  -> UNA sola fila con los datos de UNA entrega, identificada por (@docEntry, @db).
                      En trch_Entregas un documento tiene N filas (una por itemCode), asi que se
                      agrupa y se expone cantidadItems = COUNT(*) de esas filas.
                      La usa NotificacionEntregaService al terminar el registro de la entrega.

     Devuelve 14 columnas EN ESTE ORDEN, porque el RowMapper de EntregaChoferDao las lee
     POR INDICE (rs.getXxx(1..14)), no por nombre:

        1 docEntry        bigint
        2 db              varchar
        3 docNum          int
        4 factura         int
        5 cardCode        varchar
        6 cardName        varchar
        7 vendedor        varchar
        8 uChofer         int
        9 nombreChofer    varchar    (resuelto con el mismo join de la ACCION 'B')
       10 direccionEntrega varchar
       11 fechaEntrega    varchar    'dd/MM/yyyy HH:mm'  (string ya formateado, no datetime)
       12 obs             varchar
       13 cantidadItems   int
       14 tipo            varchar    'Factura' | 'Orden Venta' | 'Traspaso'

     SI SE AGREGA, QUITA O REORDENA UNA COLUMNA HAY QUE TOCAR TAMBIEN
     EntregaChoferDao.obtenerDatosNotificacion (el RowMapper lee por indice).

   Compatibilidad:
     SQL Server 2008. Nada de STRING_AGG, OFFSET/FETCH, TRY_CONVERT, IIF ni CONCAT.
     Se usa CONVERT(VARCHAR(10), fecha, 103) + ' ' + CONVERT(VARCHAR(5), fecha, 108) para
     armar 'dd/MM/yyyy HH:mm' (el estilo 108 da 'HH:mm:ss' y VARCHAR(5) lo trunca a 'HH:mm').

   Notas de filtrado (deliberadas, no las "arregles" sin pensarlo):
     - Los marcadores de inicio/fin de ruta usan docEntry = -1 (y el cierre usa docEntry = 0);
       la accion filtra docEntry > 0, asi que quedan siempre excluidos.
     - 'G' NO filtra por fueEntregado ni por valido: se invoca inmediatamente despues de que el
       SP p_abm_trch_Entregas ACCION 'B' marco el documento, y lo unico que se necesita son los
       datos de cabecera. Filtrar de mas haria que la notificacion se pierda en silencio.
     - La columna 14 'tipo' se devuelve para que el backend decida a quien avisar. NO se filtra
       aca a proposito: si el SP no devolviera fila para un Traspaso, el DAO no podria distinguir
       "es un traspaso, correcto no avisar" de "el documento no existe, algo anda mal", y las dos
       cosas quedarian en el mismo WARN. Filtrar en Java deja el motivo escrito en el log.
       Datos reales: Factura 11.660 documentos, Orden Venta 409, Traspaso 1.108 (estos ultimos
       SIEMPRE con cardCode vacio, porque son movimientos entre almacenes y no tienen cliente).

     - Se agrupa por (docEntry, db, cardCode) y NO solo por docEntry. Agrupar solo por documento
       y sacar cada columna con un MAX() independiente mezcla filas de clientes distintos: hay
       18 pares (docEntry, db) con mas de un cardCode y en 15 el nombre no le corresponde al
       codigo. Como el telefono se resuelve por cardCode, mezclar = avisarle al cliente
       equivocado. Un documento ambiguo devuelve N filas y el DAO se niega a notificar.

   El script es idempotente: es un ALTER PROCEDURE completo, se puede correr las veces que haga
   falta. El cuerpo de las acciones L, A, B, C, D, E y F esta copiado LITERAL de la definicion
   viva en el servidor (OBJECT_DEFINITION), solo se agrego lo nuevo al final.

   Verificacion rapida despues de correrlo (cambia los valores por unos reales):

       EXEC p_list_trch_Entregas @docEntry = 29552, @db = 'ESP', @ACCION = 'G';   -- Factura, 1 fila
       EXEC p_list_trch_Entregas @docEntry = 23818, @db = 'IPX', @ACCION = 'G';   -- Traspaso, no se avisa

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


END
GO
