/* ============================================================================
   SOFT DELETE — Fase 1: ESQUEMA
   Agrega la columna 'eliminado BIT NOT NULL DEFAULT 0' a las 9 tablas del núcleo
   transaccional del módulo TPEX. Idempotente (solo agrega si falta). Seguro:
   solo agrega columnas con default 0; no rompe nada existente.
   (El quién/cuándo del borrado lógico queda en audUsuario/audFecha, que el UPDATE
   de la acción 'D' va a setear en la Fase 2.)
   Ejecutar con un usuario de ESCRITURA.
   ============================================================================ */

SET NOCOUNT ON;

DECLARE @tablas TABLE (nombre SYSNAME);
INSERT INTO @tablas (nombre) VALUES
   ('tpex_Transacciones')
  ,('tpex_Cotizaciones')
  ,('tpex_SolicitudPago')
  ,('tpex_SolicitudProveedor')
  ,('tpex_DetalleSolicitud')
  ,('tpex_Asientos')
  ,('tpex_Cargos')
  ,('tpex_TransaccionParticipantes')
  ,('tpex_LogEstados');

DECLARE @t SYSNAME, @sql NVARCHAR(MAX);
DECLARE cur CURSOR LOCAL FAST_FORWARD FOR SELECT nombre FROM @tablas;
OPEN cur; FETCH NEXT FROM cur INTO @t;
WHILE @@FETCH_STATUS = 0
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sys.columns
                   WHERE object_id = OBJECT_ID('dbo.' + @t) AND name = 'eliminado')
    BEGIN
        SET @sql = 'ALTER TABLE dbo.' + QUOTENAME(@t) +
                   ' ADD eliminado BIT NOT NULL CONSTRAINT DF_' + @t + '_eliminado DEFAULT 0;';
        EXEC sys.sp_executesql @sql;
        PRINT 'OK: eliminado agregado a ' + @t;
    END
    ELSE
        PRINT 'ya existe en ' + @t;
    FETCH NEXT FROM cur INTO @t;
END
CLOSE cur; DEALLOCATE cur;

-- Verificación: las 9 deben decir OK
SELECT v.name AS tabla,
       CASE WHEN c.name IS NOT NULL THEN 'OK' ELSE 'FALTA' END AS eliminado
FROM (VALUES
   ('tpex_Transacciones'),('tpex_Cotizaciones'),('tpex_SolicitudPago'),('tpex_SolicitudProveedor'),
   ('tpex_DetalleSolicitud'),('tpex_Asientos'),('tpex_Cargos'),('tpex_TransaccionParticipantes'),('tpex_LogEstados')
) v(name)
JOIN sys.tables t ON t.name = v.name
LEFT JOIN sys.columns c ON c.object_id = t.object_id AND c.name = 'eliminado'
ORDER BY v.name;
