/* =============================================================================================
   31_entregas_correcciones.sql   ·   trch_Entregas   ·   tres correcciones de integridad
   ---------------------------------------------------------------------------------------------
   >>> EJECUTAR EN SSMS CON UNA CUENTA CON PERMISOS DDL SOBRE [BOSQUE-2_0].
   >>> Correrlo tambien en BOSQUE2PRUEBA si se prueba contra esa copia.
   >>> LEER LA PARTE 3 ANTES DE EJECUTARLA: borra filas.

   Sale de la auditoria del modulo del 11/08/2026. Los tres problemas tienen dano YA
   MATERIALIZADO en produccion, medido, no teorico. Las tres partes son independientes: se
   pueden correr por separado y en cualquier orden.

     PARTE 1  El sync pisa audFecha en filas ya entregadas   -> 17.504 de 17.867 filas afectadas
     PARTE 2  El WHERE de la ACCION 'B' es demasiado ancho   -> 14 filas corruptas irreversibles
     PARTE 3  Duplicados por sincronizaciones concurrentes   -> 42 filas sobrantes

   Cada parte trae su consulta de verificacion ANTES y DESPUES. Correr la de antes y guardar el
   resultado: es la unica forma de demostrar que el arreglo hizo algo.

   ---------------------------------------------------------------------------------------------
   ADVERTENCIA DE MEDICION — leer antes de contar nada
   ---------------------------------------------------------------------------------------------
   trch_Entregas tiene 5.155 filas CENTINELA de inicio/fin de ruta, con docEntry = 0 o -1,
   docNum = 0, itemCode = '' y db = 'ALL'. No son entregas: son marcas de que un chofer arranco
   o cerro su jornada.

   TODA consulta de duplicados tiene que filtrar docEntry > 0. Sin ese filtro las 5.155
   centinelas colapsan en una sola clave y el problema se reporta 127 veces mas grande:

       sin filtrar  ->  38 claves,  9.777 filas   (falso)
       filtrando    ->  35 claves,     77 filas   (real)
   ============================================================================================= */

USE [BOSQUE-2_0];
GO
SET NOCOUNT ON;
GO


/* =============================================================================================
   PARTE 1 — El sync deja de pisar filas ya entregadas
   ---------------------------------------------------------------------------------------------
   QUE PASA HOY
   El Paso 1 de p_abm_trch_Entregas ACCION 'A' hace:

       UPDATE target SET u_chofer=..., docTime=..., openQty=..., audFecha = GETDATE()
       FROM trch_Entregas target INNER JOIN @SourceData source ON (ItemCode/docEntry/docNum/db)

   Sin filtro por fueEntregado. Como el documento sigue apareciendo en el origen SAP durante
   ~30 dias, cada sincronizacion vuelve a pisar la fila DESPUES de entregada. Medido:
   17.504 de 17.867 filas entregadas (98,0%) tienen audFecha posterior a fechaEntrega + 10 min,
   y 17.454 la tienen mas de 24 h despues. La moda de la deriva es exactamente 30 dias.

   POR QUE IMPORTA
   audFecha era el unico timestamp puesto por el SERVIDOR. El otro, fechaEntrega, lo manda el
   RELOJ DEL TELEFONO del chofer y entra como string sin validar. Al perder audFecha no queda
   contra que contrastar: si un chofer discute que marco una entrega el martes, no hay forma de
   dirimirlo. Y no hay respaldo — se verifico que los triggers dau_Entregas y dai_Entregas estan
   DESHABILITADOS, tb_bitacora no tiene ni una fila de esta tabla, y trch_EntregasEliminado esta
   vacia. No existe imagen previa en ningun lado.

   Ademas de audFecha, el mismo UPDATE pisa u_Chofer: puede mover una entrega ya hecha al
   reporte diario de otro chofer.

   EL ARREGLO
   Una fila ya entregada no tiene NADA que recibir de SAP: ni chofer, ni docTime, ni openQty.
   Se le agrega al UPDATE:  AND ISNULL(target.fueEntregado, 0) = 0
   ============================================================================================= */

-- ---- VERIFICACION ANTES (guardar el resultado) ----
SELECT
    COUNT(*)                                                                    AS entregadas,
    SUM(CASE WHEN audFecha > DATEADD(MINUTE, 10, fechaEntrega) THEN 1 ELSE 0 END) AS con_audFecha_pisada,
    SUM(CASE WHEN audFecha > DATEADD(HOUR,   24, fechaEntrega) THEN 1 ELSE 0 END) AS pisada_mas_de_24h
FROM trch_Entregas
WHERE ISNULL(fueEntregado,0) = 1 AND docEntry > 0 AND fechaEntrega IS NOT NULL;
GO

/*  APLICAR A MANO sobre p_abm_trch_Entregas, bloque IF(@ACCION='A'), Paso 1.

    Buscar el UPDATE que empieza con "UPDATE target SET u_chofer" y agregarle la ultima linea:

        UPDATE target
           SET u_chofer = source.uchof, docTime = source.docT, openQty = source.oqty,
               audFecha = GETDATE()
        FROM trch_Entregas target
        INNER JOIN @SourceData source
                ON target.ItemCode = source.icod AND target.docEntry = source.dce
               AND target.docNum   = source.dcn  AND target.db       = source.dbo
        WHERE ISNULL(target.fueEntregado, 0) = 0;    -- <<<<<< AGREGAR ESTA LINEA

    No se automatiza el ALTER acá a proposito: ese SP tiene 15.126 caracteres y varias ACCIONes
    mas; reproducirlo entero en este script para cambiar una linea es mas riesgoso que el cambio.
*/

-- ---- VERIFICACION DESPUES ----
-- Correr la misma consulta de arriba pasadas 24 h. 'con_audFecha_pisada' no puede CRECER.
-- Las 17.504 filas ya afectadas NO se recuperan: el dato viejo no existe en ningun lado.


/* =============================================================================================
   PARTE 2 — La ACCION 'B' deja de pisar documentos ajenos
   ---------------------------------------------------------------------------------------------
   QUE PASA HOY
       UPDATE trch_Entregas SET latitud, longitud, direccionEntrega, fechaEntrega,
              fueEntregado = 1, obs, codSucursalChofer, codCiudadChofer, audUsuario,
              audFecha = getdate()
       WHERE docEntry = @docEntry AND db = @db

   El problema no es una carrera entre dos choferes (eso es inalcanzable: el listado filtra
   ISNULL(fueEntregado,0)=0). El problema es que **(docEntry, db) NO identifica un documento**.

   En SAP las facturas/OV viven en ODLN/ORDR y los traspasos en OWTR, cada tabla con su PROPIA
   secuencia de DocEntry. En PRODPAP las dos estan todavia en valores bajos (1..112), asi que
   chocan. Caso real verificado:

     1. Enero: se entrega la factura docNum 263850016 (docEntry 92, PRODPAP), queda cerrada.
     2. 26/06: el sync inserta el traspaso docNum 260860006, que REUSA docEntry 92 en PRODPAP.
     3. El chofer 27 marca ese traspaso.
     4. El UPDATE alcanza tambien la factura de enero: le cambia fechaEntrega, coordenadas y
        audUsuario. Los dos POST devuelven 201. Sin error, sin log.

   Resultado medido: 14 filas corruptas entre 03/03/2026 y 08/07/2026, 6 en los ultimos 45 dias.
   El reporte por fecha las reatribuye al chofer y al dia equivocados. Es irreversible.

   EFECTO SECUNDARIO YA CUBIERTO: obtenerDatosNotificacion (ACCION 'G') se niega a avisar cuando
   el par devuelve mas de un cliente. Correcto — pero significa que al encender el aviso al
   cliente, cada colision deja al cliente REAL sin su mensaje, en silencio.

   EL ARREGLO
   Agregar docNum al WHERE (es lo minimo que separa una factura de un traspaso) y exigir que la
   fila este pendiente. Idealmente tambien u_Chofer, pero eso requiere que el backend lo mande
   desde el JWT y no desde el body: se deja para el cambio de Java, no se fuerza acá.
   ============================================================================================= */

-- ---- VERIFICACION ANTES: cuantos pares (docEntry, db) tienen mas de un docNum ----
SELECT COUNT(*) AS pares_ambiguos, SUM(docs) AS documentos_involucrados
FROM (SELECT docEntry, db, COUNT(DISTINCT docNum) AS docs
      FROM trch_Entregas
      WHERE docEntry > 0
      GROUP BY docEntry, db
      HAVING COUNT(DISTINCT docNum) > 1) x;
GO

-- ---- Las filas ya corrompidas, para tenerlas identificadas antes de tapar el agujero ----
SELECT t.idEntrega, t.docEntry, t.docNum, t.db, t.cardCode, t.cardName,
       t.u_Chofer, t.fechaEntrega, t.audUsuario, t.tipo
FROM trch_Entregas t
JOIN (SELECT docEntry, db FROM trch_Entregas
      WHERE docEntry > 0
      GROUP BY docEntry, db HAVING COUNT(DISTINCT docNum) > 1) d
  ON d.docEntry = t.docEntry AND d.db = t.db
WHERE ISNULL(t.fueEntregado,0) = 1
ORDER BY t.db, t.docEntry, t.docNum;
GO

/*  APLICAR A MANO sobre p_abm_trch_Entregas, bloque IF(@ACCION = 'B'):

        WHERE docEntry = @docEntry
          AND db       = @db
          AND docNum   = @docNum                    -- <<<<<< AGREGAR
          AND ISNULL(fueEntregado, 0) = 0           -- <<<<<< AGREGAR

    OJO CON EL SEGUNDO AGREGADO: vuelve la operacion idempotente, o sea que re-marcar una
    entrega ya marcada afecta 0 filas. Hoy EntregaChoferDao.registrarEntregaChofer devuelve
    "resp != 0", asi que 0 filas se traduce en false y el controller responde 500 sobre una
    entrega que estaba bien registrada. HAY QUE TOCAR EL JAVA JUNTO CON ESTO, o la guarda
    convierte una perdida silenciosa en una falsa alarma roja.

    Y el frontend tiene que empezar a mandar docNum en el body de /registro-entrega-chofer:
    hoy manda docNum y docEntry, asi que el dato ya viaja — solo falta que el SP lo use.
*/


/* =============================================================================================
   PARTE 3 — Duplicados por sincronizaciones concurrentes    (BORRA FILAS: leer todo)
   ---------------------------------------------------------------------------------------------
   QUE PASA HOY
   La ACCION 'A' hace Paso 1 (UPDATE de lo existente) y Paso 2 (INSERT de lo que falta, con
   LEFT JOIN ... WHERE target.ItemCode IS NULL) SIN transaccion y SIN HOLDLOCK. Dos ejecuciones
   simultaneas ven las dos "target ausente" e insertan las dos.

   La firma esta a la vista en los IDENTITY: bloques contiguos e intercalados (30540-30544,
   30546-30550, 30551-30555, 30556-30557) del mismo dia. Como @SourceData se arma con
   GROUP BY, una sola corrida NO puede duplicar: hacen falta corridas concurrentes.

   Medido: 35 claves, 77 filas, 42 sobrantes, entre 20/03/2025 y 10/07/2026.
   Efecto visible: 24 documentos entregados reportan SUM(peso) inflado 2x a 5x en
   POST /entregas/entregas-fecha.

   DATO QUE TRANQUILIZA: filas duplicadas AUN PENDIENTES hoy = 0. Ningun chofer ve un item
   repetido en su lista. El dano vivo es el reporte de peso, no la operacion diaria.

   ---------------------------------------------------------------------------------------------
   POR QUE NO SE CREA EL INDICE UNICO QUE PARECIA OBVIO
   ---------------------------------------------------------------------------------------------
   La recomendacion natural es UNIQUE sobre (docEntry, docNum, itemCode, db). NO SE HACE, y el
   motivo es un dato concreto de la tabla:

       idEntrega 9769 · docEntry 20487 · IPX · PBB056065085ASZ · quantity 218 · openQty 298
       idEntrega 9770 · docEntry 20487 · IPX · PBB056065085ASZ · quantity  80 · openQty 298

   218 + 80 = 298 = openQty. Esas dos filas NO son un duplicado: son dos cantidades distintas
   del mismo item en el mismo documento. O sea que la clave (docEntry, docNum, itemCode, db)
   **no es unica en el origen**: le falta el numero de linea de SAP (LineNum), que la
   sincronizacion no trae a esta tabla.

   Un indice unico ahi funcionaria hoy —despues de limpiar— pero el dia que SAP vuelva a mandar
   un documento con el mismo item en dos lineas, el Paso 2 fallaria y la sincronizacion se
   caeria para TODOS los choferes. Cambiar 42 filas sucias por una caida total es mal negocio.

   LO QUE SI SE HACE, en orden de menor a mayor compromiso:
     3.a  Limpiar las 42 filas sobrantes (abajo). Arregla los 24 reportes de peso inflado.
     3.b  Envolver Paso 1 + Paso 2 en una transaccion con HOLDLOCK. Es LA solucion de fondo y
          no depende de que la clave sea unica.
     3.c  Traer LineNum de SAP y recien entonces poder poner un UNIQUE de verdad. Requiere
          tocar el SP remoto p_list_EntregasOVTraspasos y agregar la columna acá.
   ============================================================================================= */

-- ---- 3.a  VERIFICACION ANTES ----
SELECT COUNT(*) AS claves_duplicadas, SUM(n) AS filas, SUM(n) - COUNT(*) AS sobrantes
FROM (SELECT docEntry, docNum, itemCode, db, COUNT(*) AS n
      FROM trch_Entregas
      WHERE docEntry > 0                       -- imprescindible: ver la advertencia del inicio
      GROUP BY docEntry, docNum, itemCode, db
      HAVING COUNT(*) > 1) x;
GO

-- ---- 3.a  El grupo AMBIGUO, que NO se borra automaticamente ----
-- Difiere en quantity, asi que borrar "el sobrante" cambiaria el dato. Decision de negocio:
-- o se conserva la suma (218 + 80 = 298 = openQty) o se corrige a mano.
SELECT idEntrega, docEntry, docNum, db, itemCode, quantity, openQty, peso, u_Chofer, fechaEntrega
FROM trch_Entregas
WHERE docEntry = 20487 AND db = 'IPX' AND itemCode = 'PBB056065085ASZ'
ORDER BY idEntrega;
GO

/* ---- 3.a  LIMPIEZA ----
   Conserva el idEntrega MAS BAJO de cada grupo y borra el resto.
   Es seguro porque se verifico que dentro de cada grupo las filas son IDENTICAS en
   fechaEntrega, latitud, longitud, audUsuario y u_Chofer. La UNICA excepcion es el grupo de
   arriba, que queda EXCLUIDO por su condicion de quantity.

   Correr primero el SELECT. Si el conteo coincide con 'sobrantes' menos el grupo ambiguo,
   recien ahi descomentar el DELETE.
*/

-- PASO 1 — mirar exactamente que se borraria
SELECT t.idEntrega, t.docEntry, t.docNum, t.db, t.itemCode, t.quantity, t.u_Chofer, t.fechaEntrega
FROM trch_Entregas t
JOIN (SELECT docEntry, docNum, itemCode, db, MIN(idEntrega) AS conservar
      FROM trch_Entregas
      WHERE docEntry > 0
      GROUP BY docEntry, docNum, itemCode, db
      HAVING COUNT(*) > 1
         AND COUNT(DISTINCT CAST(quantity AS varchar(30))) = 1)  -- excluye el grupo ambiguo
     d ON d.docEntry = t.docEntry AND d.docNum = t.docNum
      AND d.itemCode = t.itemCode AND d.db = t.db
WHERE t.idEntrega > d.conservar
ORDER BY t.docEntry, t.itemCode, t.idEntrega;
GO

-- PASO 2 — el borrado. DESCOMENTAR SOLO DESPUES DE REVISAR EL PASO 1.
/*
BEGIN TRANSACTION;

    DELETE t
    FROM trch_Entregas t
    JOIN (SELECT docEntry, docNum, itemCode, db, MIN(idEntrega) AS conservar
          FROM trch_Entregas
          WHERE docEntry > 0
          GROUP BY docEntry, docNum, itemCode, db
          HAVING COUNT(*) > 1
             AND COUNT(DISTINCT CAST(quantity AS varchar(30))) = 1)
         d ON d.docEntry = t.docEntry AND d.docNum = t.docNum
          AND d.itemCode = t.itemCode AND d.db = t.db
    WHERE t.idEntrega > d.conservar;

    -- Tiene que decir 41 (los 42 sobrantes menos el del grupo ambiguo).
    SELECT @@ROWCOUNT AS filas_borradas;

-- Si el numero es el esperado:  COMMIT TRANSACTION;
-- Si no:                        ROLLBACK TRANSACTION;
ROLLBACK TRANSACTION;   -- <<<< por defecto NO commitea. Cambiar a COMMIT a conciencia.
*/
GO

-- ---- 3.a  VERIFICACION DESPUES ----
-- Tiene que devolver 1 clave (la ambigua) en vez de 35.
-- Y el peso por documento vuelve a su valor real:
--   SELECT docEntry, db, SUM(peso) FROM trch_Entregas
--   WHERE docEntry = 962 AND db = 'PRODPAP' GROUP BY docEntry, db;   -- esperado ~246,22


/* ---- 3.b  LA SOLUCION DE FONDO (aplicar a mano en p_abm_trch_Entregas, ACCION 'A') ----

   Envolver los dos pasos para que ninguna otra ejecucion pueda colarse entre el UPDATE y el
   INSERT. El HOLDLOCK sobre el LEFT JOIN del Paso 2 es lo que impide que dos corridas vean
   las dos "target ausente":

       BEGIN TRANSACTION;

           -- Paso 1: UPDATE de lo existente  (con el AND fueEntregado=0 de la PARTE 1)

           -- Paso 2: INSERT de lo que falta
           INSERT INTO trch_Entregas (...)
           SELECT ...
           FROM @SourceData source
           LEFT JOIN trch_Entregas target WITH (HOLDLOCK)     -- <<<<<< AGREGAR
                  ON target.ItemCode = source.icod AND target.docEntry = source.dce
                 AND target.docNum   = source.dcn  AND target.db       = source.dbo
           WHERE target.ItemCode IS NULL;

       COMMIT TRANSACTION;

   Esto no depende de que la clave sea unica, asi que funciona hoy y sigue funcionando el dia
   que SAP mande dos lineas del mismo item.
*/
GO
