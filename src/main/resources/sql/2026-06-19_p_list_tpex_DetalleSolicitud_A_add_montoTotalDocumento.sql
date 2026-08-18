-- ============================================================================
--  EJECUTAR EN: BOSQUE-2_0
--  Fecha: 2026-06-19
--
--  Objetivo: que el picker de facturas "por empresa" (ACCION 'A', que hace
--            OPENQUERY a SAP) devuelva el DocTotal del documento, mapeado al
--            nombre montoTotalDocumento que espera el modelo del backend.
--            Sin esto, al registrar por el camino "por empresa" la cuota
--            queda con montoTotalDocumento = 0.
--
--  ⚠ PAREJA: requiere haber aplicado tambien (en SRV_2022 / CONEXION)
--     2026-06-19_SRV2022_DocAbiertos_A_add_DocTotal.sql
--     El INSERT ... EXEC al @temp es POSICIONAL: las columnas de @temp deben
--     coincidir con las que devuelve la SAP. Aplicar ambos en la misma ventana.
--
--  Cambios respecto del original:
--    1. @temp gana la columna montoTotal DECIMAL(18,2) (6ta, al final).
--    2. El SELECT final agrega t1.montoTotal AS montoTotalDocumento.
--  La ACCION 'L' queda intacta.
-- ============================================================================
USE [BOSQUE-2_0];
GO

ALTER PROCEDURE [dbo].[p_list_tpex_DetalleSolicitud]

 @idDetalle bigint = NULL
, @idSolicitudProveedor bigint = NULL
, @tipoDocumento varchar(50) = NULL
, @numeroDocumento varchar(250) = NULL
, @facturaProvSap int = NULL
, @codigoImportacion varchar(50) = NULL
, @montoFacturaUsd decimal(18, 2) = NULL
, @montoAmortizadoUsd decimal(18, 2) = NULL
, @montoAPagarUsd decimal(19, 2) = NULL
, @fechaFactura datetime = NULL
, @fechaVencimiento datetime = NULL
, @concepto varchar(255) = NULL
, @obs varchar(255) = NULL
, @esAprobado bit = NULL
, @audUsuario int = NULL

, @ACCION VARCHAR(1) = NULL
, @codEmpresa int = NULL
AS
BEGIN
     -- =====LISTAR (SIN CAMBIOS) =====================================
     IF(@ACCION = 'L')
     BEGIN
         SELECT
             idDetalle,
             idSolicitudProveedor,
             tipoDocumento,
             numeroDocumento,
             facturaProvSap,
             codigoImportacion,
             montoFacturaUsd,
             montoAmortizadoUsd,
             montoAPagarUsd,
             fechaFactura,
             fechaVencimiento,
             concepto,
             obs,
             esAprobado,
             audUsuario,
             audFecha
         FROM tpex_DetalleSolicitud
         WHERE eliminado = 0 AND (@idDetalle IS NULL OR @idDetalle = idDetalle)
            AND (@idSolicitudProveedor IS NULL OR @idSolicitudProveedor = idSolicitudProveedor)
            AND (@tipoDocumento IS NULL OR @tipoDocumento = tipoDocumento)
            AND (@numeroDocumento IS NULL OR @numeroDocumento = numeroDocumento)
            AND (@facturaProvSap IS NULL OR @facturaProvSap = facturaProvSap)
            AND (@codigoImportacion IS NULL OR @codigoImportacion = codigoImportacion)
            AND (@montoFacturaUsd IS NULL OR @montoFacturaUsd = montoFacturaUsd)
            AND (@montoAmortizadoUsd IS NULL OR @montoAmortizadoUsd = montoAmortizadoUsd)
            AND (@montoAPagarUsd IS NULL OR @montoAPagarUsd = montoAPagarUsd)
            AND (@fechaFactura IS NULL OR @fechaFactura = fechaFactura)
            AND (@fechaVencimiento IS NULL OR @fechaVencimiento = fechaVencimiento)
            AND (@concepto IS NULL OR @concepto = concepto)
            AND (@obs IS NULL OR @obs = obs)
            AND (@esAprobado IS NULL OR @esAprobado = esAprobado)
            AND (@audUsuario IS NULL OR @audUsuario = audUsuario)
    END

    --- ===== FACTURAS PROV. Y ORDENES DE COMPRA POR EMPRESA (OPENQUERY a SAP) =====
     IF( @ACCION = 'A' )
     BEGIN

            DECLARE @sql    NVARCHAR(MAX);
            DECLARE @temp   TABLE (
                codEmpresa  INT,
                empresa     VARCHAR(15),
                descripcion VARCHAR(50),
                docNum      INT,
                moneda      VARCHAR(10),
                montoTotal  DECIMAL(18, 2)   -- ← NUEVA (DocTotal de SAP)
            );

            SET @sql = N'SELECT * FROM OPENQUERY(SRV_2022,
                ''EXEC [CONEXION].[dbo].[p_list_PagosExtranjerosDocAbiertos]
                    @codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',
                    @ACCION = ''''A'''''')';

            INSERT INTO @temp
            EXEC sp_executesql @sql

            SELECT
                t1.docNum      AS facturaProvSap,
                t1.descripcion AS tipoDocumento,
                t1.montoTotal  AS montoTotalDocumento   -- ← NUEVA
            FROM @temp t1

     END

END
GO
