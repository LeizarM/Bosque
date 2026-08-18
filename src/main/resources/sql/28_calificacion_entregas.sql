/* =============================================================================================
   28_calificacion_entregas.sql   ·   trch_EntregaCalificacion   ·   tabla + p_abm + p_list nuevos
   ---------------------------------------------------------------------------------------------
   >>> EJECUTAR EN SSMS CON UNA CUENTA QUE TENGA PERMISOS DDL (db_owner o CREATE TABLE +
   >>> CREATE PROCEDURE) SOBRE LA BASE [BOSQUE-2_0]. La cuenta de solo lectura NO puede correrlo.
   >>> Correrlo tambien en BOSQUE2PRUEBA si se va a probar contra esa copia.

   Objetivo:
     Soportar la calificacion del servicio de entrega. Cuando se le avisa por WhatsApp a un
     cliente que su pedido fue entregado, se le pide que responda con un puntaje del 1 al 5.
     Este script crea la tabla donde se guarda ese envio y la respuesta, y los dos SPs del
     modulo (escritura y lectura). NO toca nada de lo ya desplegado: ni trch_Entregas, ni
     p_abm_trch_Entregas, ni p_list_trch_Entregas (la ACCION 'G' del script 27 sigue igual).

     Objetos que crea (los tres son nuevos, no existe ninguno hoy en la base):

       trch_EntregaCalificacion          tabla
       p_abm_trch_EntregaCalificacion    ACCION 'I' (registrar el envio) y 'U' (guardar la respuesta)
       p_list_trch_EntregaCalificacion   ACCION 'P' (pendiente por chat), 'L' (listado), 'R' (resumen)

   ---------------------------------------------------------------------------------------------
   MODELO DE DATOS: una fila = un pedido de calificacion
   ---------------------------------------------------------------------------------------------
     La fila NACE cuando se manda el aviso de entrega (puntaje NULL, fechaRespuesta NULL) y se
     COMPLETA cuando el cliente contesta por el webhook. O sea, "pendiente" no es un estado
     aparte: es simplemente puntaje IS NULL. No hay columna de estado a proposito, para que no
     puedan desincronizarse el estado y el dato.

     Clave del negocio: (docEntry, db, chatId). Es UNIQUE. Si el mismo aviso se manda dos veces
     al mismo numero no se crea una segunda fila (la ACCION 'I' es un no-op silencioso), asi que
     la respuesta del cliente siempre cae sobre el pedido original y no se inflan las estadisticas
     del chofer con envios repetidos.

     u_Chofer se guarda DESNORMALIZADO (copiado del documento al momento del envio) y el nombre
     NO se guarda: se resuelve al leer, con el mismo join que la ACCION 'G'. El codigo se congela
     porque la calificacion es de ese chofer en esa entrega aunque despues cambie de puesto; el
     nombre se resuelve en vivo porque si a alguien le corrigen el apellido en RRHH tiene que
     verse corregido tambien en el historico.

   ---------------------------------------------------------------------------------------------
   ACCIONES DE p_abm_trch_EntregaCalificacion
   ---------------------------------------------------------------------------------------------
     ACCION 'I'  -> Inserta el pedido de calificacion recien enviado. Lo llama
                    CalificacionService.registrarEnvio despues de que WhatsApp acepto el mensaje.
                    Se implementa como un unico INSERT ... SELECT ... WHERE NOT EXISTS, no como
                    IF + INSERT: asi un reenvio del mismo aviso devuelve 0 filas afectadas en vez
                    de reventar contra el UNIQUE, y el SP emite UN SOLO conteo de filas (ver la
                    nota de NOCOUNT mas abajo, de eso depende el boolean del DAO).

                    OJO con @fechaEntrega: aca es un datetime de verdad, pero
                    EntregaNotificacionDTO.fechaEntrega es un STRING ya formateado 'dd/MM/yyyy
                    HH:mm' (la ACCION 'G' lo devuelve asi para esquivar JacksonConfig). No se lo
                    puede pasar tal cual confiando en la conversion implicita: el login de la base
                    tiene DATEFORMAT dmy y eso lo haria depender del idioma de la conexion. O se
                    parsea en Java a java.util.Date, o directamente se manda NULL y el SP pone
                    GETDATE(), que es lo mas fiel: el aviso sale inmediatamente despues de que se
                    marco la entrega.

     ACCION 'U'  -> Guarda la respuesta del cliente: puntaje, comentario, mensajeCrudo y
                    fechaRespuesta, buscando por @idCalificacion. El WHERE lleva ademas
                    "AND puntaje IS NULL": gana la PRIMERA respuesta. openWA reintenta un webhook
                    hasta 5 veces (retryCount) y puede entregar el mismo mensaje mas de una vez;
                    con este filtro el reintento afecta 0 filas en lugar de pisar la calificacion.
                    No toca audUsuario (la respuesta no la genera un usuario del sistema) ni
                    fechaEntrega. Si no se manda @fechaRespuesta se usa GETDATE() del servidor.

   ---------------------------------------------------------------------------------------------
   ACCIONES DE p_list_trch_EntregaCalificacion
   ---------------------------------------------------------------------------------------------
     ACCION 'P'  -> LA CONSULTA CALIENTE. Devuelve 0 o 1 fila: la calificacion pendiente mas
                    reciente de @chatId dentro de las ultimas @horas
                    (puntaje IS NULL  AND  fechaEntrega >= DATEADD(hour, -@horas, GETDATE())),
                    TOP 1 ORDER BY fechaEntrega DESC. Es lo que corre en cada mensaje entrante,
                    por eso el indice IX_trchEntCal_chat_fecha esta hecho a su medida.
                    Con @chatId nulo o vacio devuelve vacio SIEMPRE: un chatId en blanco no puede
                    hacer de comodin y engancharse a la entrega de otro cliente.

     ACCION 'L'  -> Listado por rango de fechas y chofer. @u_Chofer = 0 (o NULL) = todos.
                    Devuelve respondidas y pendientes, ordenado por fechaEntrega DESC.

     ACCION 'R'  -> Resumen agregado por chofer para el reporte: cuantas se enviaron, cuantas
                    contestaron, el promedio y la distribucion c1..c5. Mismo rango que 'L'.

     Las tres devuelven las columnas EN EL ORDEN DE LOS DTO, porque los RowMapper leen POR
     INDICE (rs.getXxx(1..N)), no por nombre.

     'P' y 'L' -> CalificacionEntrega, 14 columnas:

        1 idCalificacion   bigint
        2 docEntry         bigint
        3 db               varchar
        4 cardCode         varchar
        5 cardName         varchar
        6 uChofer          int
        7 nombreChofer     varchar    (resuelto con el mismo join de la ACCION 'G')
        8 puntaje          int NULL   <-- OJO, ver la nota de abajo
        9 comentario       varchar
       10 chatId           varchar
       11 mensajeCrudo     varchar
       12 fechaEntrega     datetime   (datetime de verdad, NO string como en la ACCION 'G')
       13 fechaRespuesta   datetime NULL
       14 audFecha         datetime

     'R' -> ResumenCalificacionDTO, 10 columnas:

        1 uChofer          int
        2 nombreChofer     varchar
        3 totalEnviadas    int
        4 totalRespondidas int
        5 promedio         float      (0 cuando todavia no contesto nadie)
        6 c1               int
        7 c2               int
        8 c3               int
        9 c4               int
       10 c5               int

     SI SE AGREGA, QUITA O REORDENA UNA COLUMNA HAY QUE TOCAR TAMBIEN CalificacionEntregaDao
     (los RowMapper leen por indice) Y EL DTO CORRESPONDIENTE.

     OJO CON puntaje (columna 8): se devuelve NULL cuando el cliente todavia no contesto, a
     proposito. rs.getInt(8) sobre un NULL devuelve 0, que es un puntaje que no existe y que en
     un promedio hecho en Java arruinaria el numero. El RowMapper TIENE que hacer
     rs.getInt(8) + rs.wasNull() (o rs.getObject) y dejar el Integer en null. Lo mismo aplica a
     fechaRespuesta (13), que si es getTimestamp devuelve null solo.
     audUsuario NO se devuelve: no esta en el DTO del contrato. No agregarlo sin correr los indices.

   ---------------------------------------------------------------------------------------------
   Compatibilidad y decisiones tecnicas
   ---------------------------------------------------------------------------------------------
     SQL Server 2008 (el servidor es 10.0.1600, 2008 RTM sin service pack). Nada de STRING_AGG,
     OFFSET/FETCH, TRY_CONVERT, IIF, CONCAT ni CREATE OR ALTER. Por eso los SPs se crean con el
     patron "stub vacio + ALTER": es la unica forma de que el script sea idempotente en 2008.

     NOCOUNT, y por que esta puesto en un SP y en el otro no. NO es un descuido:
       - p_abm NO lleva SET NOCOUNT ON, igual que p_abm_trch_Entregas. El DAO del modulo hace
         "return resp != 0;" sobre lo que devuelve jdbcTemplate.update (ver
         EntregaChoferDao.registrarEntregaChofer): si se apaga el conteo, update() devuelve
         siempre 0 y el metodo devolveria siempre false. Por eso ademas cada ACCION ejecuta UNA
         SOLA sentencia que reporte filas, para que el conteo que le llega a Java no sea ambiguo.
       - p_list SI lleva SET NOCOUNT ON, igual que p_list_trch_Entregas: ahi se usa
         jdbcTemplate.query y los conteos sueltos solo confunden al driver.

     Largos de texto: @comentario y @mensajeCrudo estan declarados con el MISMO largo que su
     columna (500 y 1000). Un texto mas largo se corta al asignarse al parametro, que en SQL
     Server trunca en silencio, en vez de llegar al INSERT y reventar con el error 8152 ("String
     or binary data would be truncated"). Un mensaje de WhatsApp puede tener miles de caracteres
     y lo escribe el cliente, o sea que es entrada no confiable y de largo arbitrario: el SP no se
     cae por eso. Igual conviene truncar en Java para que quede explicito de donde sale el corte.

     Fechas: los parametros de fecha son DATETIME y los manda el driver ya tipados, asi que no se
     parsea ningun string. Cuidado al probar A MANO en SSMS: el login de la base tiene idioma
     Espanol y DATEFORMAT dmy, con lo cual el literal '2026-12-31' se interpreta como dia 2026 y
     tira "valor fuera de intervalo". En los ejemplos de abajo se usa el formato sin separadores
     ('20261231'), que es el unico que no depende del idioma de la sesion.

   Notas de filtrado (deliberadas, no las "arregles" sin pensarlo):
     - El rango de 'L' y 'R' filtra por fechaEntrega (cuando se PIDIO la calificacion), no por
       fechaRespuesta. El reporte tiene que poder mostrar "de las 40 entregas de esta semana
       contestaron 12": si filtrara por la fecha de respuesta, las pendientes desaparecerian del
       total y la tasa de respuesta daria siempre 100%.
     - El rango se escribe  >= inicio del dia @fechaIni  AND  < dia siguiente a @fechaFin, en vez
       del CAST(fecha AS DATE) BETWEEN que usan las acciones viejas de p_list_trch_Entregas. Hace
       exactamente lo mismo (incluye el ultimo dia completo sin importar la hora) pero deja usar
       el indice por fechaEntrega, porque no aplica una funcion sobre la columna.
     - 'R' agrupa por u_Chofer y no por nombre: dos personas pueden llamarse igual, y agrupar por
       el nombre resuelto mezclaria sus calificaciones. El nombre se resuelve DESPUES de agrupar.
     - 'R' no excluye a los choferes sin ninguna respuesta: aparecen con totalRespondidas = 0 y
       promedio 0. Que un chofer no reciba respuestas es justamente un dato del reporte.
     - promedio se calcula con AVG(CAST(puntaje AS FLOAT)) sobre los NO nulos y se redondea a 2
       decimales. Sin el CAST, AVG de una columna int hace division entera y 4 y 3 promediarian 3.
       El ISNULL(...,0) del final es para que la columna nunca venga NULL cuando no hay respuestas.
     - La ACCION 'R' emite el aviso "Advertencia: valor NULL eliminado por el agregado u otra
       operacion SET" (warning 8153) cuando hay calificaciones pendientes. Es NORMAL y es la
       prueba de que el promedio ignora las que no contestaron. No hay que taparlo con
       SET ANSI_WARNINGS OFF. En Java no molesta: JdbcTemplate viene con ignoreWarnings = true.

   El script es idempotente: se puede correr las veces que haga falta. La tabla y los indices se
   crean solo si faltan (nunca se borra ni se altera una tabla que ya exista, asi que no hay forma
   de que se pierdan datos), y los SPs son un ALTER completo sobre un stub que se crea al vuelo.
   Los SPs existentes del modulo no tienen GRANT explicito (la aplicacion conecta con una cuenta
   elevada), asi que no hace falta otorgar EXECUTE sobre los nuevos.

   Verificacion rapida despues de correrlo (cambia los valores por unos reales):

       -- estructura
       EXEC sp_help 'trch_EntregaCalificacion';

       -- alta de un envio de prueba y su respuesta
       EXEC p_abm_trch_EntregaCalificacion @docEntry = 29552, @db = 'ESP', @cardCode = 'C0001',
            @cardName = 'CLIENTE DE PRUEBA', @u_Chofer = 104, @chatId = '59178888274@c.us',
            @fechaEntrega = '20260810 10:30', @audUsuario = -1, @ACCION = 'I';

       EXEC p_list_trch_EntregaCalificacion @chatId = '59178888274@c.us', @horas = 48, @ACCION = 'P';
       -- tomar el idCalificacion de arriba y guardarle la respuesta
       EXEC p_abm_trch_EntregaCalificacion @idCalificacion = 1, @puntaje = 5,
            @comentario = 'Todo bien', @mensajeCrudo = '5 todo bien gracias', @ACCION = 'U';

       -- la misma 'P' ya no la devuelve (dejo de estar pendiente)
       EXEC p_list_trch_EntregaCalificacion @chatId = '59178888274@c.us', @horas = 48, @ACCION = 'P';

       -- reportes
       EXEC p_list_trch_EntregaCalificacion @fechaIni = '20260801', @fechaFin = '20260831',
            @u_Chofer = 0, @ACCION = 'L';
       EXEC p_list_trch_EntregaCalificacion @fechaIni = '20260801', @fechaFin = '20260831',
            @ACCION = 'R';

============================================================================================= */

SET NOCOUNT ON;
GO


/* =============================================================================================
   1) TABLA trch_EntregaCalificacion
   ============================================================================================= */

IF OBJECT_ID('dbo.trch_EntregaCalificacion', 'U') IS NULL
BEGIN

	CREATE TABLE dbo.trch_EntregaCalificacion
	(
		 idCalificacion		bigint			IDENTITY(1,1)	NOT NULL
		,docEntry			bigint							NOT NULL	-- docEntry del documento SAP
		,db					varchar(10)						NOT NULL	-- IPX / ESP / PAP / PRODPAP
		,cardCode			varchar(10)						NOT NULL	-- mismo largo que trch_Entregas.cardCode
		,cardName			varchar(250)					NOT NULL	-- copiado al momento del envio
		,u_Chofer			int								NOT NULL	-- = tb_empleado.codEmpleado
		,puntaje			int								NULL		-- 1..5. NULL = todavia no respondio
		,comentario			varchar(500)					NULL		-- texto util extraido de la respuesta
		,chatId				varchar(50)						NOT NULL	-- '591XXXXXXXX@c.us'
		,mensajeCrudo		varchar(1000)					NULL		-- respuesta tal cual, NO CONFIABLE
		,fechaEntrega		datetime						NOT NULL	-- cuando se entrego / se pidio la calificacion
		,fechaRespuesta		datetime						NULL		-- cuando contesto el cliente
		,audUsuario			bigint							NOT NULL
		,audFecha			datetime						NOT NULL
			CONSTRAINT DF_trchEntCal_audFecha DEFAULT (GETDATE())

		,CONSTRAINT PK_trch_EntregaCalificacion PRIMARY KEY CLUSTERED (idCalificacion)

		-- El CHECK acepta NULL: en SQL Server un CHECK solo rechaza cuando da FALSE, y con NULL
		-- da UNKNOWN. El "puntaje IS NULL OR" esta escrito igual para que la regla se lea sola.
		,CONSTRAINT CK_trchEntCal_puntaje
			CHECK (puntaje IS NULL OR (puntaje >= 1 AND puntaje <= 5))

		-- Un aviso por (documento, base, numero). Si el aviso se reenvia no se duplica la fila,
		-- asi la respuesta cae sobre el pedido original y el reporte no cuenta dos envios.
		,CONSTRAINT UQ_trchEntCal_doc_db_chat UNIQUE (docEntry, db, chatId)
	);

	PRINT 'OK: tabla trch_EntregaCalificacion creada.';

END
ELSE
	PRINT 'ya existe: trch_EntregaCalificacion (no se toca).';
GO


-- Indice de la busqueda caliente: "la ultima entrega avisada a este numero". Lo usa la ACCION 'P'
-- en CADA mensaje entrante del webhook. chatId primero porque es igualdad, fechaEntrega DESC
-- despues para que el TOP 1 salga del primer registro leido sin ordenar nada.
-- puntaje va como INCLUDE para que el filtro "pendiente" se resuelva dentro del indice y no haya
-- que ir a buscar la fila entera solo para descubrir que ya estaba respondida.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_trchEntCal_chat_fecha'
                 AND object_id = OBJECT_ID('dbo.trch_EntregaCalificacion'))
BEGIN
	CREATE NONCLUSTERED INDEX IX_trchEntCal_chat_fecha
		ON dbo.trch_EntregaCalificacion (chatId ASC, fechaEntrega DESC)
		INCLUDE (puntaje);
	PRINT 'OK: indice IX_trchEntCal_chat_fecha creado.';
END
ELSE
	PRINT 'ya existe: IX_trchEntCal_chat_fecha.';
GO


-- Indice del reporte por rango (ACCIONES 'L' y 'R'). u_Chofer va como segunda clave porque 'L'
-- puede filtrar por chofer dentro del rango, y 'R' agrupa por el.
IF NOT EXISTS (SELECT 1 FROM sys.indexes
               WHERE name = 'IX_trchEntCal_fecha'
                 AND object_id = OBJECT_ID('dbo.trch_EntregaCalificacion'))
BEGIN
	CREATE NONCLUSTERED INDEX IX_trchEntCal_fecha
		ON dbo.trch_EntregaCalificacion (fechaEntrega ASC, u_Chofer ASC)
		INCLUDE (puntaje);
	PRINT 'OK: indice IX_trchEntCal_fecha creado.';
END
ELSE
	PRINT 'ya existe: IX_trchEntCal_fecha.';
GO


/* =============================================================================================
   2) p_abm_trch_EntregaCalificacion   ·   ACCION 'I' (envio) y 'U' (respuesta)
   ---------------------------------------------------------------------------------------------
   Stub + ALTER: en SQL Server 2008 no existe CREATE OR ALTER, y CREATE PROCEDURE tiene que ser
   la primera sentencia del lote, asi que no se puede meter dentro de un IF.
   ============================================================================================= */

IF OBJECT_ID('dbo.p_abm_trch_EntregaCalificacion', 'P') IS NULL
	EXEC('CREATE PROCEDURE dbo.p_abm_trch_EntregaCalificacion AS BEGIN SELECT 1; END');
GO

ALTER PROCEDURE [dbo].[p_abm_trch_EntregaCalificacion]

 @idCalificacion bigint = NULL
, @docEntry bigint = NULL
, @db varchar(10) = NULL
, @cardCode varchar(10) = NULL
, @cardName varchar(250) = NULL
, @u_Chofer int = NULL
, @puntaje int = NULL
, @comentario varchar(500) = NULL
, @chatId varchar(50) = NULL
, @mensajeCrudo varchar(1000) = NULL
, @fechaEntrega datetime = NULL
, @fechaRespuesta datetime = NULL

, @audUsuario bigint = NULL
, @ACCION VARCHAR(1) = NULL

AS
BEGIN

	-- SIN "SET NOCOUNT ON" A PROPOSITO: el DAO del modulo devuelve
	-- "jdbcTemplate.update(...) != 0", asi que necesita que el conteo de filas llegue a Java.
	-- Cada ACCION ejecuta UNA SOLA sentencia que reporte filas, para que ese conteo sea el suyo.

	-- ============ REGISTRAR EL PEDIDO DE CALIFICACION RECIEN ENVIADO ============================
	-- Nace pendiente: puntaje y fechaRespuesta quedan NULL hasta que el cliente conteste.
	-- Es un INSERT ... SELECT ... WHERE NOT EXISTS y no un IF + INSERT por dos razones: emite un
	-- unico conteo de filas (1 = se registro, 0 = ya estaba) y no revienta contra
	-- UQ_trchEntCal_doc_db_chat cuando el mismo aviso se manda de nuevo.
	-- El UNIQUE sigue siendo la garantia real: si dos hilos entran a la vez, uno recibe el
	-- error 2627 y el DAO lo loguea. En la practica no pasa (un @Async por entrega marcada).
	IF(@ACCION = 'I')
	BEGIN

		INSERT INTO trch_EntregaCalificacion
		(
			 docEntry
			,db
			,cardCode
			,cardName
			,u_Chofer
			,puntaje
			,comentario
			,chatId
			,mensajeCrudo
			,fechaEntrega
			,fechaRespuesta
			,audUsuario
			,audFecha
		)
		SELECT
			 ISNULL(@docEntry, 0)
			,ISNULL(@db, '')
			,ISNULL(@cardCode, '')
			,ISNULL(@cardName, '')
			,ISNULL(@u_Chofer, 0)
			,NULL						-- puntaje: todavia no respondio
			,NULL						-- comentario
			,@chatId
			,NULL						-- mensajeCrudo
			,ISNULL(@fechaEntrega, GETDATE())
			,NULL						-- fechaRespuesta
			,ISNULL(@audUsuario, -1)
			,GETDATE()
		WHERE ISNULL(@chatId, '') <> ''		-- sin destinatario no hay calificacion que pedir
		  AND NOT EXISTS (
				SELECT 1
				FROM trch_EntregaCalificacion t1
				WHERE t1.docEntry = ISNULL(@docEntry, 0)
				  AND t1.db = ISNULL(@db, '')
				  AND t1.chatId = @chatId
		      );

	END


	-- ============ GUARDAR LA RESPUESTA DEL CLIENTE ==============================================
	-- Busca por idCalificacion, que sale de la ACCION 'P' de p_list. El "AND puntaje IS NULL" hace
	-- que gane la PRIMERA respuesta: openWA reintenta un webhook hasta 5 veces y puede entregar el
	-- mismo mensaje mas de una vez; con ese filtro el reintento afecta 0 filas en lugar de pisar
	-- la calificacion ya guardada.
	-- No toca audUsuario (la respuesta no la genera un usuario del sistema) ni fechaEntrega.
	IF(@ACCION = 'U')
	BEGIN

		UPDATE trch_EntregaCalificacion
		SET  puntaje = @puntaje
			,comentario = @comentario
			,mensajeCrudo = @mensajeCrudo
			,fechaRespuesta = ISNULL(@fechaRespuesta, GETDATE())
			,audFecha = GETDATE()
		WHERE idCalificacion = @idCalificacion
		  AND puntaje IS NULL
		  AND @puntaje BETWEEN 1 AND 5;		-- espejo del CHECK: un puntaje invalido no borra el pendiente

	END

END
GO


/* =============================================================================================
   3) p_list_trch_EntregaCalificacion   ·   ACCION 'P' (pendiente), 'L' (listado), 'R' (resumen)
   ============================================================================================= */

IF OBJECT_ID('dbo.p_list_trch_EntregaCalificacion', 'P') IS NULL
	EXEC('CREATE PROCEDURE dbo.p_list_trch_EntregaCalificacion AS BEGIN SELECT 1; END');
GO

ALTER PROCEDURE [dbo].[p_list_trch_EntregaCalificacion]

 @idCalificacion bigint = NULL
, @docEntry bigint = NULL
, @db varchar(10) = NULL
, @cardCode varchar(10) = NULL
, @u_Chofer int = NULL
, @puntaje int = NULL
, @chatId varchar(50) = NULL
, @horas int = NULL
, @fechaIni datetime = NULL
, @fechaFin datetime = NULL

, @audUsuario bigint = NULL
, @ACCION VARCHAR(1) = NULL

AS
BEGIN

	SET NOCOUNT ON;

	-- ============ CALIFICACION PENDIENTE MAS RECIENTE DE UN CHAT ================================
	-- La consulta caliente: corre en CADA mensaje entrante del webhook. Devuelve 0 o 1 fila.
	-- "Pendiente" = puntaje IS NULL. La ventana evita que un "gracias" suelto de la semana pasada
	-- se cuente como calificacion de una entrega vieja.
	-- Con @chatId nulo o vacio NO devuelve nada: un chatId en blanco no puede hacer de comodin.
	IF(@ACCION = 'P')
	BEGIN

		-- Ventana por defecto y piso de 1 hora. Con @horas = 0 la ventana quedaria vacia y el
		-- webhook no encontraria nunca nada, que es un sintoma dificil de rastrear.
		SET @horas = ISNULL(@horas, 48);
		IF(@horas < 1) SET @horas = 1;

		SELECT TOP 1
			 t1.idCalificacion
			,t1.docEntry
			,t1.db
			,t1.cardCode
			,t1.cardName
			,t1.u_Chofer				AS uChofer

			-- Nombre del chofer: mismo join que la ACCION 'G' de p_list_trch_Entregas
			-- (u_Chofer = tb_empleado.codEmpleado). Se resuelve al leer, no se guarda.
			,ISNULL((
				SELECT ISNULL(tx.nombres, '') + ' ' + ISNULL(tx.apPaterno, '') + ' ' + ISNULL(tx.apMaterno, '')
				FROM trh_persona tx
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona
				WHERE ty.codEmpleado = t1.u_Chofer
			 ), '')						AS nombreChofer

			,t1.puntaje					-- NULL aca siempre (es la definicion de pendiente)
			,t1.comentario
			,t1.chatId
			,t1.mensajeCrudo
			,t1.fechaEntrega
			,t1.fechaRespuesta
			,t1.audFecha
		FROM trch_EntregaCalificacion t1
		WHERE ISNULL(@chatId, '') <> ''
		  AND t1.chatId = @chatId
		  AND t1.puntaje IS NULL
		  AND t1.fechaEntrega >= DATEADD(HOUR, -@horas, GETDATE())
		ORDER BY t1.fechaEntrega DESC;

	END


	-- ============ LISTADO POR RANGO DE FECHAS Y CHOFER ==========================================
	-- Devuelve respondidas Y pendientes: el pendiente tambien es informacion para el reporte.
	-- @u_Chofer = 0 o NULL significa "todos".
	-- El rango va sobre fechaEntrega (cuando se pidio la calificacion), no sobre fechaRespuesta.
	IF(@ACCION = 'L')
	BEGIN

		SELECT
			 t1.idCalificacion
			,t1.docEntry
			,t1.db
			,t1.cardCode
			,t1.cardName
			,t1.u_Chofer				AS uChofer

			,ISNULL((
				SELECT ISNULL(tx.nombres, '') + ' ' + ISNULL(tx.apPaterno, '') + ' ' + ISNULL(tx.apMaterno, '')
				FROM trh_persona tx
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona
				WHERE ty.codEmpleado = t1.u_Chofer
			 ), '')						AS nombreChofer

			,t1.puntaje
			,t1.comentario
			,t1.chatId
			,t1.mensajeCrudo
			,t1.fechaEntrega
			,t1.fechaRespuesta
			,t1.audFecha
		FROM trch_EntregaCalificacion t1
		-- Rango escrito para que use el indice: sin CAST sobre la columna. Toma el dia @fechaIni
		-- completo desde las 00:00 y el dia @fechaFin completo hasta las 23:59:59.997.
		WHERE (@fechaIni IS NULL OR t1.fechaEntrega >= CAST(CAST(@fechaIni AS DATE) AS DATETIME))
		  AND (@fechaFin IS NULL OR t1.fechaEntrega <  DATEADD(DAY, 1, CAST(CAST(@fechaFin AS DATE) AS DATETIME)))
		  AND (@u_Chofer IS NULL OR @u_Chofer = 0 OR t1.u_Chofer = @u_Chofer)
		ORDER BY t1.fechaEntrega DESC;

	END


	-- ============ RESUMEN POR CHOFER ============================================================
	-- Una fila por chofer con envios en el rango. Los que no recibieron ninguna respuesta
	-- aparecen igual, con totalRespondidas = 0 y promedio 0: que a un chofer no le contesten es
	-- justamente un dato del reporte.
	-- Se agrupa por u_Chofer (el codigo) y el nombre se resuelve DESPUES de agrupar: dos personas
	-- pueden llamarse igual y agrupar por nombre mezclaria sus calificaciones.
	IF(@ACCION = 'R')
	BEGIN

		SELECT
			 x.uChofer

			,ISNULL((
				SELECT ISNULL(tx.nombres, '') + ' ' + ISNULL(tx.apPaterno, '') + ' ' + ISNULL(tx.apMaterno, '')
				FROM trh_persona tx
				JOIN tb_empleado ty ON ty.codPersona = tx.codPersona
				WHERE ty.codEmpleado = x.uChofer
			 ), '')						AS nombreChofer

			,x.totalEnviadas
			,x.totalRespondidas
			,x.promedio
			,x.c1
			,x.c2
			,x.c3
			,x.c4
			,x.c5
		FROM (
			SELECT
				 t1.u_Chofer											AS uChofer
				,COUNT(*)												AS totalEnviadas
				-- COUNT de una columna ignora los NULL, o sea cuenta solo las que contestaron
				,COUNT(t1.puntaje)										AS totalRespondidas
				-- CAST a FLOAT obligatorio: AVG sobre int hace division entera (4 y 3 darian 3).
				-- ISNULL para que nunca venga NULL cuando no hubo ninguna respuesta.
				,ISNULL(CAST(ROUND(AVG(CAST(t1.puntaje AS FLOAT)), 2) AS FLOAT), 0)	AS promedio
				,SUM(CASE WHEN t1.puntaje = 1 THEN 1 ELSE 0 END)		AS c1
				,SUM(CASE WHEN t1.puntaje = 2 THEN 1 ELSE 0 END)		AS c2
				,SUM(CASE WHEN t1.puntaje = 3 THEN 1 ELSE 0 END)		AS c3
				,SUM(CASE WHEN t1.puntaje = 4 THEN 1 ELSE 0 END)		AS c4
				,SUM(CASE WHEN t1.puntaje = 5 THEN 1 ELSE 0 END)		AS c5
			FROM trch_EntregaCalificacion t1
			WHERE (@fechaIni IS NULL OR t1.fechaEntrega >= CAST(CAST(@fechaIni AS DATE) AS DATETIME))
			  AND (@fechaFin IS NULL OR t1.fechaEntrega <  DATEADD(DAY, 1, CAST(CAST(@fechaFin AS DATE) AS DATETIME)))
			  AND (@u_Chofer IS NULL OR @u_Chofer = 0 OR t1.u_Chofer = @u_Chofer)
			GROUP BY t1.u_Chofer
		) x
		ORDER BY nombreChofer;

	END

END
GO
