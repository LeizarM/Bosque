-- ============================================================
-- FIX: Agrega la acción 'C' al SP p_list_tpex_Transacciones
--
-- Problema: TransaccionesDao.obtenerTransaccionesPorCotizacion
-- llama al SP con @ACCION = 'C', pero el SP no tenía ese bloque.
-- Resultado previo: siempre retornaba lista vacía (HTTP 204).
-- ============================================================
USE [BOSQUE-2_0];
GO

ALTER PROCEDURE [dbo].[p_list_tpex_Transacciones]
-- ... (mantener todos los parámetros existentes sin cambios) ...
AS
BEGIN
    SET NOCOUNT ON;

    -- ================================================================
    -- Declarar y poblar @tempProv (compartido por todas las acciones)
    -- ================================================================
    DECLARE @sqlProv  NVARCHAR(MAX);
    DECLARE @tempProv TABLE (
        cardCode  VARCHAR(10),
        cardName  VARCHAR(300),
        moneda    VARCHAR(5)
    );

    IF @codEmpresa IS NOT NULL
    BEGIN
        SET @sqlProv = N'SELECT * FROM OPENQUERY(SRV_2022,
            ''EXEC [CONEXION].[dbo].[p_list_Proveedores]
            @codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',
            @ACCION = ''''A'''''')';

        INSERT INTO @tempProv
        EXEC sp_executesql @sqlProv;
    END

    -- ================================================================
    -- L: Transacciones de una solicitud (grilla)
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        -- ... (sin cambios) ...
    END

    -- ================================================================
    -- R: Un registro completo con todos los campos (formulario)
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        -- ... (sin cambios) ...
    END

    -- ================================================================
    -- C: Transacciones vinculadas a una cotización específica
    -- NUEVO BLOQUE — requerido por TransaccionesDao.obtenerTransaccionesPorCotizacion
    -- ================================================================
    IF @ACCION = 'C'
    BEGIN
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,t.idSolicitud
            ,t.idCotizacion
            ,t.idTipoTransaccion
            ,tt.nombre              AS tipoTransaccion
            ,t.codBanco
            ,b.nombre               AS banco
            ,t.cardCode
            ,p.cardName             AS proveedor
            ,t.fechaTransaccion
            ,t.fechaValor
            ,t.montoOrigen
            ,mo.codigo              AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido
            ,md.codigo              AS monedaDestino
            ,t.totalCargos
            ,t.totalFinal
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN tpex_TiposTransaccion  tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN tpex_Monedas           mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN tpex_Monedas           md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.idCotizacion = @idCotizacion
        ORDER BY t.fechaTransaccion DESC;
    END

    -- ================================================================
    -- B: Reporte entre fechas
    -- ================================================================
    IF @ACCION = 'B'
    BEGIN
        -- ... (sin cambios) ...
    END

END
GO

