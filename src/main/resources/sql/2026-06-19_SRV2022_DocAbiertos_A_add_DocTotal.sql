-- ============================================================================
--  EJECUTAR EN: SRV_2022, base de datos CONEXION   (NO en BOSQUE-2_0)
--  Fecha: 2026-06-19
--
--  Objetivo: que la ACCION 'A' (documentos abiertos por EMPRESA) devuelva
--            tambien el DocTotal del documento SAP, igual que ya lo hace la
--            ACCION 'B' (por proyecto). Sin esto, montoTotalDocumento llega
--            en 0 al registrar una solicitud por el camino "por empresa".
--
--  PAREJA: este script va JUNTO con
--     2026-06-19_p_list_tpex_DetalleSolicitud_A_add_montoTotalDocumento.sql
--     (BOSQUE-2_0). El @temp de ese SP es posicional: aplicar ambos en la
--     misma ventana. Entre uno y otro, el picker "por empresa" queda
--     inconsistente (mismatch de columnas).
--
--  Solo cambia el SELECT de la ACCION 'A' (se agrega DocTotal AS montoTotal en
--  las 4 ramas del UNION). La ACCION 'B' queda intacta.
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

    -- =========   DOCUMENTOS ABIERTOS PARA PAGOS EXTRANJEROS POR EMPRESA  ===========
    IF @ACCION = 'A'
    BEGIN

        SELECT *
        FROM (
            -- IMPEXPAP - Facturas Compra
            SELECT
                1 as codEmpresa,
                'IPX'       AS Empresa,
                'Factura Compra' AS Descripcion,
                h.DocNum,
                h.DocCur         AS Moneda,
                h.DocTotal       AS montoTotal
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
                h.DocCur          AS Moneda,
                h.DocTotal        AS montoTotal
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
                h.DocCur         AS Moneda,
                h.DocTotal       AS montoTotal
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
                h.DocCur          AS Moneda,
                h.DocTotal        AS montoTotal
            FROM ESPPAPEL.dbo.OPOR h
            WHERE h.DocStatus = 'O'
              AND h.CANCELED  = 'N'

        ) AS src
        WHERE src.codEmpresa = @codEmpresa

    END

    -- =========   DOCUMENTOS ABIERTOS POR PROYECTO (LIKE)  ===========  (SIN CAMBIOS)
    IF @ACCION = 'B'
    BEGIN

        SELECT *
        FROM (
            SELECT 1 AS codEmpresa, 'IPX' AS Empresa, 'Factura Compra' AS Descripcion,
                   h.DocNum, h.DocCur AS Moneda, h.Project, h.DocTotal AS montoTotal
            FROM IMPEXPAP.dbo.OPCH h
            WHERE h.DocStatus = 'O' AND h.CANCELED = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
              AND NOT EXISTS (SELECT 1 FROM IMPEXPAP.dbo.RPC1 r
                    INNER JOIN IMPEXPAP.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry AND r.BaseType = 18 AND nc.CANCELED = 'N')
            UNION ALL
            SELECT 1, 'IPX', 'Orden de Compra', h.DocNum, h.DocCur, h.Project, h.DocTotal
            FROM IMPEXPAP.dbo.OPOR h
            WHERE h.DocStatus = 'O' AND h.CANCELED = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
            UNION ALL
            SELECT 5, 'ESP', 'Factura Compra', h.DocNum, h.DocCur, h.Project, h.DocTotal
            FROM ESPPAPEL.dbo.OPCH h
            WHERE h.DocStatus = 'O' AND h.CANCELED = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
              AND NOT EXISTS (SELECT 1 FROM ESPPAPEL.dbo.RPC1 r
                    INNER JOIN ESPPAPEL.dbo.ORPC nc ON nc.DocEntry = r.DocEntry
                    WHERE r.BaseEntry = h.DocEntry AND r.BaseType = 18 AND nc.CANCELED = 'N')
            UNION ALL
            SELECT 5, 'ESP', 'Orden de Compra', h.DocNum, h.DocCur, h.Project, h.DocTotal
            FROM ESPPAPEL.dbo.OPOR h
            WHERE h.DocStatus = 'O' AND h.CANCELED = 'N'
              AND (@project IS NULL OR h.Project LIKE '%' + @project + '%')
        ) AS src
        WHERE (src.codEmpresa = @codEmpresa OR @codEmpresa IS NULL)
        ORDER BY src.Empresa, src.Descripcion, src.Project

    END

END
GO
