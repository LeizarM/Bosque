/* ============================================================================
   2026-06-15  ·  p_list_tpex_Transacciones  ·  acción L (y C): campos de referencia
   ----------------------------------------------------------------------------
   Problema:
     La pestaña "Transacciones" del detalle usa la ACCIÓN L (grilla por solicitud).
     Su SELECT NO incluía los campos de referencia/diferencia/forward/exportadora,
     así que la tarjeta los recibía en 0 y el bloque de porcentajes no se mostraba
     (aunque en la tabla SÍ existen: tipoCambioReferencia, equivalenteUsdRef,
     diferenciaDeMas, porcentajeDiferencia, etc.).

   Fix:
     Agregar esas columnas al SELECT de las acciones L y C (misma forma de grilla).
     El modelo Flutter ya las parsea; solo faltaba que el SP las devolviera.
   ============================================================================ */

ALTER PROCEDURE [dbo].[p_list_tpex_Transacciones]

      @ACCION               VARCHAR(1)    = NULL
    , @idTransaccion        BIGINT        = NULL
    , @numeroTransaccion    VARCHAR(50)   = NULL
    , @idSolicitud          BIGINT        = NULL
    , @idCotizacion         BIGINT        = NULL
    , @idTipoTransaccion    BIGINT        = NULL
    , @codBanco             BIGINT        = NULL
    , @idCanal              BIGINT        = NULL
    , @codEmpresa           INT           = NULL
    , @cardCode             VARCHAR(20)   = NULL
    , @fechaTransaccion     DATE          = NULL
    , @fechaValor           DATE          = NULL
    , @montoOrigen          DECIMAL(18,2) = NULL
    , @idMonedaOrigen       BIGINT        = NULL
    , @tipoCambioAplicado   DECIMAL(18,6) = NULL
    , @montoConvertido      DECIMAL(18,2) = NULL
    , @idMonedaDestino      BIGINT        = NULL
    , @totalCargos          DECIMAL(18,2) = NULL
    , @totalFinal           DECIMAL(18,2) = NULL
    , @numeroContrato       VARCHAR(50)   = NULL
    , @fechaPactado         DATE          = NULL
    , @fechaVencimiento     DATE          = NULL
    , @tipoCambioForward    DECIMAL(18,6) = NULL
    , @tipoCambioReferencia DECIMAL(18,6) = NULL
    , @equivalenteUsdRef    DECIMAL(18,6) = NULL
    , @diferenciaDeMas      DECIMAL(18,2) = NULL
    , @porcentajeDiferencia DECIMAL(5,2)  = NULL
    , @nombreExportadora    VARCHAR(300)  = NULL
    , @tcNegociadoExportadora DECIMAL(18,6) = NULL
    , @comisionExportadora  DECIMAL(18,2) = NULL
    , @metodoExportadora    VARCHAR(100)  = NULL
    , @estado               VARCHAR(30)   = NULL
    , @observaciones        VARCHAR(400)  = NULL
    , @audUsuario           BIGINT        = NULL
    , @fechaInicio          DATE          = NULL
    , @fechaFin             DATE          = NULL
AS
BEGIN
    SET NOCOUNT ON;

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
        INSERT INTO @tempProv EXEC sp_executesql @sqlProv;
    END

    -- ================================================================
    -- L: Grilla de transacciones por solicitud
    -- ================================================================
    IF @ACCION = 'L'
    BEGIN
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,t.idSolicitud
            ,t.idTipoTransaccion
            ,tt.nombre              AS tipoTransaccion
            ,t.codBanco
            ,b.nombre               AS banco
            ,t.cardCode
            ,ISNULL(p.cardName, '') AS proveedor     -- ► LEFT: cardCode puede ser NULL
            ,t.fechaTransaccion
            ,t.fechaValor
            ,t.montoOrigen
            ,mo.codigo              AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido
            ,md.codigo              AS monedaDestino
            ,t.totalCargos
            ,t.totalFinal
            -- ► NUEVO: info relevante para la tarjeta de transacción
            ,t.numeroContrato
            ,t.tipoCambioForward
            ,t.tipoCambioReferencia
            ,t.equivalenteUsdRef
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.nombreExportadora
            ,t.tcNegociadoExportadora
            ,t.comisionExportadora
            ,t.metodoExportadora
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE (@idSolicitud       IS NULL OR t.idSolicitud       = @idSolicitud)
          AND (@cardCode          IS NULL OR t.cardCode          = @cardCode)
          AND (@codBanco          IS NULL OR t.codBanco          = @codBanco)
          AND (@estado            IS NULL OR t.estado            = @estado)
          AND (@idTipoTransaccion IS NULL OR t.idTipoTransaccion = @idTipoTransaccion)
        ORDER BY t.fechaTransaccion DESC;
    END

    -- ================================================================
    -- R: Registro completo (formulario)
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        SELECT
             t.*
            ,tt.nombre              AS tipoTransaccion
            ,tt.requiereForward
            ,tt.requiereBanco
            ,b.nombre               AS banco
            ,ISNULL(p.cardName, '') AS proveedor     -- ► LEFT: cardCode puede ser NULL
            ,mo.codigo              AS monedaOrigen
            ,md.codigo              AS monedaDestino
            ,e.nombre               AS empresa
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         p  ON p.cardCode           = t.cardCode
        JOIN  tb_empresa            e  ON e.codEmpresa         = t.codEmpresa
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE t.idTransaccion = @idTransaccion;
    END

    -- ================================================================
    -- C: Transacciones por cotización
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
            ,ISNULL(p.cardName, '') AS proveedor
            ,t.fechaTransaccion
            ,t.fechaValor
            ,t.montoOrigen
            ,mo.codigo              AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.montoConvertido
            ,md.codigo              AS monedaDestino
            ,t.totalCargos
            ,t.totalFinal
            -- ► NUEVO: info relevante (igual que L)
            ,t.numeroContrato
            ,t.tipoCambioForward
            ,t.tipoCambioReferencia
            ,t.equivalenteUsdRef
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.nombreExportadora
            ,t.tcNegociadoExportadora
            ,t.comisionExportadora
            ,t.metodoExportadora
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
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
        SELECT
             t.idTransaccion
            ,t.numeroTransaccion
            ,e.nombre                           AS empresa
            ,t.cardCode
            ,t.codBanco
            ,ISNULL(tp.cardName, ISNULL(t.cardCode, '')) AS proveedor
            ,tt.nombre                          AS tipoTransaccion
            ,b.nombre                           AS banco
            ,t.fechaTransaccion
            ,t.montoOrigen
            ,mo.codigo                          AS monedaOrigen
            ,t.tipoCambioAplicado
            ,t.totalCargos
            ,t.totalFinal
            ,md.codigo                          AS monedaDestino
            ,t.diferenciaDeMas
            ,t.porcentajeDiferencia
            ,t.tieneVoucher
            ,t.idTransaccionOrigen
            ,t.estado
        FROM tpex_Transacciones     t
        JOIN  tpex_TiposTransaccion tt ON tt.idTipoTransaccion = t.idTipoTransaccion
        JOIN  tpex_Monedas          mo ON mo.idMoneda          = t.idMonedaOrigen
        JOIN  tpex_Monedas          md ON md.idMoneda          = t.idMonedaDestino
        LEFT JOIN @tempProv         tp ON tp.cardCode          = t.cardCode
        JOIN  tb_empresa            e  ON e.codEmpresa         = t.codEmpresa
        LEFT JOIN tch_banco         b  ON b.codBanco           = t.codBanco
        WHERE (@fechaInicio       IS NULL OR t.fechaTransaccion  >= @fechaInicio)
          AND (@fechaFin          IS NULL OR t.fechaTransaccion  <= @fechaFin)
          AND (@estado            IS NULL OR t.estado            = @estado)
          AND (@cardCode          IS NULL OR t.cardCode          = @cardCode)
          AND (@idTipoTransaccion IS NULL OR t.idTipoTransaccion = @idTipoTransaccion)
        ORDER BY t.fechaTransaccion DESC;
    END
END
GO
