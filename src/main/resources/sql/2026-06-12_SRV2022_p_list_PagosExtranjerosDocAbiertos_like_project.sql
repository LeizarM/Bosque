-- ============================================================================
--  EJECUTAR EN: SRV_2022, base de datos CONEXION  (NO en BOSQUE-2_0)
--  Fecha: 2026-06-12
--
--  Cambios en ACCION 'B' de p_list_PagosExtranjerosDocAbiertos:
--    1. Búsqueda de proyecto por LIKE parcial ('25-0' → 25-059-TOT, 25-060-TOT…)
--       antes era igualdad estricta (h.Project = @project).
--    2. Se agrega montoTotal (h.DocTotal): la ACCION 'C' de
--       p_list_tpex_SolicitudPago (BOSQUE-2_0) lo espera y hoy cae al
--       fallback legacy con NULL.
--  ACCION 'A' queda intacta.
-- ============================================================================
USE CONEXION;
GO

ALTER PROCEDURE [dbo].[p_list_PagosExtranjerosDocAbiertos]

     @codEmpresa     INT          = NULL   -- 1 = 'IMPEXPAP' | 5 = 'ESPPAPEL'
    ,@docNum         INT          = NULL
    ,@ACCION         VARCHAR(1)   = NULL
    ,@project       NVARCHAR(100)  = NULL

AS
SET NOCOUNT OFF;


BEGIN



     -- =========   LISTARA LOS DOCUMENTOS ABIERTOS PARA PAGOS EXTRANJEROS (PAGOS QUE NO SE PUEDEN ASOCIAR A FACTURAS DE COMPRA) POR EMPRESA  ===========
    IF @ACCION = 'A'
    BEGIN

        SELECT *
        FROM (
            -- IMPEXPAP - Facturas Compra
            SELECT
                1 as codEmpresa,
                'IPX'       AS Empresa,
                'Factura Compra' AS Descripcion,
                h.DocNum
                ,h.DocCur         AS Moneda
            FROM IMPEXPAP.dbo.OPCH h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND NOT EXISTS (
                    SELECT 1 FROM IMPEXPAP.dbo.RPC1 r
                    INNER JOIN IMPEXPAP.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry
                      AND r.BaseType  = 18       -- Object type = A/P Invoice
                      AND nc.CANCELED = 'N'
              )

            UNION ALL

            -- IMPEXPAP - Ordenes de Compra
            SELECT
                1 as codEmpresa,
                'IPX'        AS Empresa,
                'Orden de Compra' AS Descripcion,
                h.DocNum,
                h.DocCur          AS Moneda
            FROM IMPEXPAP.dbo.OPOR h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'

            UNION ALL

            -- ESPPAPEL - Facturas Compra
            SELECT
                5 as codEmpresa,
                'ESP'       AS Empresa,
                'Factura Compra' AS Descripcion,
                h.DocNum,
                h.DocCur         AS Moneda
            FROM ESPPAPEL.dbo.OPCH h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND NOT EXISTS (
                    SELECT 1 FROM ESPPAPEL.dbo.RPC1 r
                    INNER JOIN ESPPAPEL.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry
                      AND r.BaseType  = 18
                      AND nc.CANCELED = 'N'
              )

            UNION ALL

            -- ESPPAPEL - Ordenes de Compra
            SELECT
                5 as codEmpresa,
                'ESP'        AS Empresa,
                'Orden de Compra' AS Descripcion,
                h.DocNum,
                h.DocCur          AS Moneda
            FROM ESPPAPEL.dbo.OPOR h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'

        ) AS src
        WHERE src.codEmpresa = @codEmpresa

    END

    -- =========   LISTARA LOS DOCUMENTOS ABIERTOS POR PROYECTO (BUSQUEDA PARCIAL CON LIKE)  ===========
    IF @ACCION = 'B'
    BEGIN

        SELECT *
        FROM (
            -- IMPEXPAP - Facturas Compra
            SELECT
                1                AS codEmpresa,
                'IPX'            AS Empresa,
                'Factura Compra' AS Descripcion,
                h.DocNum,
                h.DocCur         AS Moneda,
                h.Project,
                h.DocTotal       AS montoTotal
            FROM IMPEXPAP.dbo.OPCH h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
              AND NOT EXISTS (
                    SELECT 1 FROM IMPEXPAP.dbo.RPC1 r
                    INNER JOIN IMPEXPAP.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry
                      AND r.BaseType  = 18       -- A/P Invoice
                      AND nc.CANCELED = 'N'
              )

            UNION ALL

            -- IMPEXPAP - Ordenes de Compra
            SELECT
                1                 AS codEmpresa,
                'IPX'             AS Empresa,
                'Orden de Compra' AS Descripcion,
                h.DocNum,
                h.DocCur          AS Moneda,
                h.Project,
                h.DocTotal        AS montoTotal
            FROM IMPEXPAP.dbo.OPOR h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')

            UNION ALL

            -- ESPPAPEL - Facturas Compra
            SELECT
                5                AS codEmpresa,
                'ESP'            AS Empresa,
                'Factura Compra' AS Descripcion,
                h.DocNum,
                h.DocCur         AS Moneda,
                h.Project,
                h.DocTotal       AS montoTotal
            FROM ESPPAPEL.dbo.OPCH h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
              AND NOT EXISTS (
                    SELECT 1 FROM ESPPAPEL.dbo.RPC1 r
                    INNER JOIN ESPPAPEL.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry
                      AND r.BaseType  = 18
                      AND nc.CANCELED = 'N'
              )

            UNION ALL

            -- ESPPAPEL - Ordenes de Compra
            SELECT
                5                 AS codEmpresa,
                'ESP'             AS Empresa,
                'Orden de Compra' AS Descripcion,
                h.DocNum,
                h.DocCur          AS Moneda,
                h.Project,
                h.DocTotal        AS montoTotal
            FROM ESPPAPEL.dbo.OPOR h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')

        ) AS src
        WHERE (src.codEmpresa = @codEmpresa OR @codEmpresa IS NULL)
        ORDER BY src.Empresa, src.Descripcion, src.Project

    END



END
GO
