-- ============================================================================
--  TPEX — Cierre de brechas detectadas vs. planillas Excel operativas
--  Fecha: 2026-06-12
--  Autor: Claude (revisado por MJAIMES)
--
--  Contenido (ejecutar en orden, BD BOSQUE-2_0, SQL Server 2008):
--    PARTE 1: DDL  — cardCode nullable, idTransaccionOrigen,
--                    tabla tpex_TransaccionParticipantes
--    PARTE 2: Seeds — moneda USDT, tipo transacción DEVOLUCION
--    PARTE 3: ALTER p_abm_tpex_Transacciones  (reglas por tipo de transacción)
--    PARTE 4: ALTER p_list_tpex_Transacciones (LEFT JOIN proveedor)
--    PARTE 5: CREATE p_abm_tpex_TransaccionParticipantes
--    PARTE 6: CREATE p_list_tpex_TransaccionParticipantes
--
--  Reglas nuevas en p_abm_tpex_Transacciones:
--    - cardCode e idSolicitud obligatorios SOLO para tipos de pago a proveedor
--      (TC_DIRECTO, TC_NEGOCIADO, FORWARD, EXPORTADORA).
--      FONDEO_MERCURY / TRASPASO_MERCURY / COMPRA_USDT / DEVOLUCION no los exigen.
--    - TRASPASO_MERCURY y DEVOLUCION pueden tener monedaOrigen = monedaDestino.
--    - DEVOLUCION exige idTransaccionOrigen (vínculo a la operación que se devuelve).
-- ============================================================================

-- ============================================================================
-- PARTE 1: DDL
-- ============================================================================

-- 1.1 cardCode pasa a NULL (fondeos/traspasos/devoluciones no tienen proveedor)
ALTER TABLE dbo.tpex_Transacciones ALTER COLUMN cardCode VARCHAR(20) NULL;
GO

-- 1.2 Vínculo devolución → transacción origen
IF NOT EXISTS (SELECT 1 FROM sys.columns
               WHERE object_id = OBJECT_ID('dbo.tpex_Transacciones')
                 AND name = 'idTransaccionOrigen')
BEGIN
    ALTER TABLE dbo.tpex_Transacciones
        ADD idTransaccionOrigen BIGINT NULL
            CONSTRAINT FK_tpexTrans_TransOrigen
            REFERENCES dbo.tpex_Transacciones (idTransaccion);
END
GO

-- 1.3 Participantes de una transacción (split IPX / MONRROY / NEMER, etc.)
IF OBJECT_ID('dbo.tpex_TransaccionParticipantes') IS NULL
BEGIN
    CREATE TABLE dbo.tpex_TransaccionParticipantes (
        idParticipante   BIGINT IDENTITY (1, 1) NOT NULL
            CONSTRAINT PK_tpex_TransaccionParticipantes PRIMARY KEY,
        idTransaccion    BIGINT        NOT NULL
            CONSTRAINT FK_tpexPart_Transaccion
            REFERENCES dbo.tpex_Transacciones (idTransaccion),
        tipoParticipante VARCHAR(10)   NOT NULL,  -- EMPRESA / TERCERO
        nombre           VARCHAR(100)  NOT NULL,  -- IPX, MONRROY RODRIGO, NEMER...
        porcentaje       DECIMAL(8, 4) NULL,      -- participación s/ montoConvertido
        montoUs          DECIMAL(18, 4) NULL,
        montoBs          DECIMAL(18, 4) NULL,
        itfUs            DECIMAL(18, 4) NULL,     -- ITF propio del participante
        itfBs            DECIMAL(18, 4) NULL,
        observaciones    VARCHAR(500)  NULL,
        audUsuario       BIGINT        NULL,
        audFecha         DATETIME      NOT NULL DEFAULT GETDATE()
    );

    CREATE INDEX IX_tpexPart_idTransaccion
        ON dbo.tpex_TransaccionParticipantes (idTransaccion);
END
GO

-- ============================================================================
-- PARTE 2: SEEDS
-- ============================================================================

IF NOT EXISTS (SELECT 1 FROM dbo.tpex_Monedas WHERE codigo = 'USDT')
BEGIN
    INSERT INTO dbo.tpex_Monedas (codigo, nombre, simbolo, decimales, activo, audUsuario, audFecha)
    VALUES ('USDT', 'Tether USD', 'USDT', 2, 1, NULL, GETDATE());
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.tpex_TiposTransaccion WHERE codigo = 'DEVOLUCION')
BEGIN
    INSERT INTO dbo.tpex_TiposTransaccion (codigo, nombre, descripcion, requiereForward, requiereBanco, activo, audUsuario, audFecha)
    VALUES ('DEVOLUCION', 'Devolución de fondos',
            'Devolución de un tercero o canal, vinculada a una transacción origen mediante idTransaccionOrigen.',
            0, 0, 1, NULL, GETDATE());
END
GO

-- ============================================================================
-- PARTE 3: p_abm_tpex_Transacciones — reglas por tipo + idTransaccionOrigen
-- ============================================================================
ALTER PROCEDURE [dbo].[p_abm_tpex_Transacciones]

    @ACCION VARCHAR(1)
    , @idTransaccion BIGINT = NULL
    , @numeroTransaccion VARCHAR(50) = NULL
    , @idSolicitud BIGINT = NULL
    , @idCotizacion BIGINT = NULL
    , @idTipoTransaccion BIGINT = NULL
    , @codBanco BIGINT = NULL
    , @idCanal BIGINT = NULL
    , @codEmpresa BIGINT = NULL
    , @cardCode VARCHAR(20) = NULL
    , @fechaTransaccion DATE = NULL
    , @fechaValor DATE = NULL
    , @montoOrigen DECIMAL(18, 2) = NULL
    , @idMonedaOrigen BIGINT = NULL
    , @tipoCambioAplicado DECIMAL(10, 6) = NULL
    , @montoConvertido DECIMAL(18, 2) = NULL
    , @idMonedaDestino BIGINT = NULL
    , @totalCargos DECIMAL(18, 2) = NULL
    , @totalFinal DECIMAL(18, 2) = NULL
    , @numeroContrato VARCHAR(50) = NULL
    , @fechaPactado DATE = NULL
    , @fechaVencimiento DATE = NULL
    , @tipoCambioForward DECIMAL(10, 6) = NULL
    , @tipoCambioReferencia DECIMAL(10, 6) = NULL
    , @equivalenteUsdRef DECIMAL(18, 2) = NULL
    , @diferenciaDeMas DECIMAL(18, 2) = NULL
    , @porcentajeDiferencia DECIMAL(10, 4) = NULL
    , @nombreExportadora VARCHAR(100) = NULL
    , @tcNegociadoExportadora DECIMAL(10, 6) = NULL
    , @comisionExportadora DECIMAL(18, 2) = NULL
    , @metodoExportadora VARCHAR(50) = NULL
    , @estado VARCHAR(20) = NULL
    , @observaciones TEXT = NULL
    , @rutaVoucher VARCHAR(500) = NULL
    , @idTransaccionOrigen BIGINT = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0;
    SET @errormsg = '';
    SET @idGenerado = 0;

    DECLARE @estadoSolicitud VARCHAR(20);
    DECLARE @estadoActual VARCHAR(20);
    DECLARE @requiereForward BIT;
    DECLARE @requiereBanco BIT;
    DECLARE @codigoTipo VARCHAR(30);
    DECLARE @estadoCotizacion VARCHAR(20);
    DECLARE @idMonedaProveedor BIGINT;
    DECLARE @esPagoProveedor BIT;

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
        BEGIN
            -- El tipo se valida primero: define qué campos son obligatorios.
            IF ISNULL(@idTipoTransaccion, 0) = 0
                BEGIN SET @error = 23; SET @errormsg = 'idTipoTransaccion es obligatorio.'; RETURN; END

            SELECT @requiereForward = requiereForward, @requiereBanco = requiereBanco, @codigoTipo = codigo
            FROM tpex_TiposTransaccion WHERE idTipoTransaccion = @idTipoTransaccion AND activo = 1;
            IF @codigoTipo IS NULL
                BEGIN SET @error = 24; SET @errormsg = 'Tipo de transacción inexistente o inactivo.'; RETURN; END

            SET @esPagoProveedor = CASE WHEN @codigoTipo IN ('TC_DIRECTO', 'TC_NEGOCIADO', 'FORWARD', 'EXPORTADORA') THEN 1 ELSE 0 END;

            -- Solicitud: obligatoria solo para pagos a proveedor; si llega, se valida siempre.
            IF @esPagoProveedor = 1 AND ISNULL(@idSolicitud, 0) = 0
                BEGIN SET @error = 20; SET @errormsg = 'idSolicitud es obligatorio para el tipo ' + @codigoTipo + '.'; RETURN; END

            IF ISNULL(@idSolicitud, 0) != 0
                BEGIN
                    SELECT @estadoSolicitud = estado FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud;
                    IF @estadoSolicitud IS NULL
                        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END
                    IF @estadoSolicitud != 'APROBADA'
                        BEGIN SET @error = 22; SET @errormsg = 'Solo se pueden crear transacciones sobre solicitudes APROBADAS. Estado actual: ' + @estadoSolicitud + '.'; RETURN; END
                END

            IF @requiereBanco = 1 AND ISNULL(@codBanco, 0) = 0
                BEGIN SET @error = 25; SET @errormsg = 'El tipo ' + @codigoTipo + ' requiere codBanco.'; RETURN; END
            IF ISNULL(@codBanco, 0) != 0 AND NOT EXISTS (SELECT 1 FROM tch_banco WHERE codBanco = @codBanco)
                BEGIN SET @error = 26; SET @errormsg = 'No existe el banco ' + CAST(@codBanco AS VARCHAR) + '.'; RETURN; END

            IF ISNULL(@idCotizacion, 0) != 0
                BEGIN
                    SELECT @estadoCotizacion = estado FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
                    IF @estadoCotizacion IS NULL
                        BEGIN SET @error = 27; SET @errormsg = 'No existe la cotización ' + CAST(@idCotizacion AS VARCHAR) + '.'; RETURN; END
                    IF @estadoCotizacion != 'ACEPTADA'
                        BEGIN SET @error = 28; SET @errormsg = 'La cotización debe estar ACEPTADA. Estado actual: ' + @estadoCotizacion + '.'; RETURN; END
                END

            IF ISNULL(@codEmpresa, 0) = 0
                BEGIN SET @error = 29; SET @errormsg = 'codEmpresa es obligatorio.'; RETURN; END

            -- cardCode: obligatorio solo para pagos a proveedor.
            IF @esPagoProveedor = 1 AND ISNULL(@cardCode, '') = ''
                BEGIN SET @error = 30; SET @errormsg = 'cardCode es obligatorio para el tipo ' + @codigoTipo + '.'; RETURN; END

            -- DEVOLUCION exige el vínculo a la transacción que se devuelve.
            IF @codigoTipo = 'DEVOLUCION' AND ISNULL(@idTransaccionOrigen, 0) = 0
                BEGIN SET @error = 45; SET @errormsg = 'DEVOLUCION requiere idTransaccionOrigen (transacción que se devuelve).'; RETURN; END
            IF ISNULL(@idTransaccionOrigen, 0) != 0
               AND NOT EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccion = @idTransaccionOrigen)
                BEGIN SET @error = 46; SET @errormsg = 'No existe la transacción origen ' + CAST(@idTransaccionOrigen AS VARCHAR) + '.'; RETURN; END

            IF @fechaTransaccion IS NULL
                BEGIN SET @error = 33; SET @errormsg = 'Fecha Transaccion es obligatorio.'; RETURN; END
            IF ISNULL(@montoOrigen, 0) <= 0
                BEGIN SET @error = 34; SET @errormsg = 'Monto Origen debe ser mayor a cero.'; RETURN; END
            IF ISNULL(@idMonedaOrigen, 0) = 0 OR NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen AND activo = 1)
                BEGIN SET @error = 35; SET @errormsg = 'idMonedaOrigen inválida o inactiva.'; RETURN; END
            IF ISNULL(@tipoCambioAplicado, 0) <= 0
                BEGIN SET @error = 36; SET @errormsg = 'Tipo Cambio Aplicado debe ser mayor a cero.'; RETURN; END
            IF ISNULL(@idMonedaDestino, 0) = 0 OR NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMonedaDestino AND activo = 1)
                BEGIN SET @error = 37; SET @errormsg = 'idMonedaDestino inválida o inactiva.'; RETURN; END

            -- Misma moneda permitida en traspasos internos y devoluciones (p.ej. USD→USD).
            IF @idMonedaOrigen = @idMonedaDestino AND @codigoTipo NOT IN ('TRASPASO_MERCURY', 'DEVOLUCION')
                BEGIN
                    DECLARE @codigoMonedaIgual VARCHAR(5);
                    SELECT @codigoMonedaIgual = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen;
                    SET @error = 38;
                    SET @errormsg = 'idMonedaOrigen e idMonedaDestino no pueden ser la misma moneda (' + ISNULL(@codigoMonedaIgual, '?') + ') para el tipo ' + @codigoTipo + '.';
                    RETURN;
                END

            -- moneda origen = BOB, destino = divisa
            IF @codigoTipo IN ('TC_DIRECTO', 'TC_NEGOCIADO', 'FORWARD')
                BEGIN
                    DECLARE @codigoOrigenDir VARCHAR(5);
                    DECLARE @codigoDestinoDir VARCHAR(5);
                    SELECT @codigoOrigenDir  = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaOrigen;
                    SELECT @codigoDestinoDir = codigo FROM tpex_Monedas WHERE idMoneda = @idMonedaDestino;
                    IF @codigoOrigenDir != 'BOB'
                        BEGIN SET @error = 39; SET @errormsg = 'Para el tipo ' + @codigoTipo + ' la moneda origen debe ser BOB. Recibido: ' + ISNULL(@codigoOrigenDir,'?') + '.'; RETURN; END
                    IF @codigoDestinoDir = 'BOB'
                        BEGIN SET @error = 40; SET @errormsg = 'Para el tipo ' + @codigoTipo + ' la moneda destino no puede ser BOB.'; RETURN; END
                END

            IF @requiereForward = 1
                BEGIN
                    IF ISNULL(@tipoCambioForward, 0) = 0
                        BEGIN SET @error = 41; SET @errormsg = 'FORWARD requiere Tipo CambioForward.'; RETURN; END
                    IF @fechaVencimiento IS NULL
                        BEGIN SET @error = 42; SET @errormsg = 'FORWARD requiere Fecha Vencimiento del contrato.'; RETURN; END
                END

            IF @codigoTipo = 'EXPORTADORA'
                BEGIN
                    IF ISNULL(@nombreExportadora, '') = ''
                        BEGIN SET @error = 43; SET @errormsg = 'EXPORTADORA requiere nombreExportadora.'; RETURN; END
                    IF ISNULL(@tcNegociadoExportadora, 0) = 0
                        BEGIN SET @error = 44; SET @errormsg = 'EXPORTADORA requiere tcNegociadoExportadora.'; RETURN; END
                END

            -- BOB / TC = USD (división, no multiplicación)
            SET @montoConvertido = ROUND(@montoOrigen / @tipoCambioAplicado, 2);
            SET @totalFinal      = ROUND(@montoConvertido + ISNULL(@totalCargos, 0), 2);

            IF ISNULL(@tipoCambioReferencia, 0) = 0
                BEGIN
                    SELECT TOP 1 @tipoCambioReferencia = tasaVenta
                    FROM tpex_TiposCambio
                    WHERE codBanco IS NULL
                      AND idMonedaOrigen  = @idMonedaOrigen
                      AND idMonedaDestino = @idMonedaDestino
                    ORDER BY fechaVigencia DESC;
                END

            IF ISNULL(@tipoCambioReferencia, 0) > 0
                BEGIN
                    SET @equivalenteUsdRef    = ROUND(@totalFinal / @tipoCambioReferencia, 2);
                    SET @diferenciaDeMas      = ROUND(@equivalenteUsdRef - @montoOrigen, 2);
                    SET @porcentajeDiferencia = ROUND((@diferenciaDeMas / @montoOrigen) * 100, 4);
                END
            ELSE
                BEGIN
                    SET @equivalenteUsdRef    = 0;
                    SET @diferenciaDeMas      = 0;
                    SET @porcentajeDiferencia = 0;
                END

            BEGIN TRY
                BEGIN TRANSACTION;

                INSERT INTO tpex_Transacciones (
                    numeroTransaccion, idSolicitud, idCotizacion, idTipoTransaccion,
                    codBanco, idCanal, codEmpresa, cardCode,
                    fechaTransaccion, fechaValor,
                    montoOrigen, idMonedaOrigen, tipoCambioAplicado, montoConvertido, idMonedaDestino,
                    totalCargos, totalFinal,
                    numeroContrato, fechaPactado, fechaVencimiento,
                    tipoCambioForward, tipoCambioReferencia,
                    equivalenteUsdRef, diferenciaDeMas, porcentajeDiferencia,
                    nombreExportadora, tcNegociadoExportadora, comisionExportadora, metodoExportadora,
                    rutaVoucher, idTransaccionOrigen,
                    estado, observaciones, audUsuario, audFecha
                )
                VALUES (
                    NULL, NULLIF(@idSolicitud, 0), @idCotizacion, @idTipoTransaccion,
                    @codBanco, @idCanal, @codEmpresa, NULLIF(@cardCode, ''),
                    @fechaTransaccion, NULL,
                    @montoOrigen, @idMonedaOrigen, @tipoCambioAplicado, @montoConvertido, @idMonedaDestino,
                    ISNULL(@totalCargos, 0), @totalFinal,
                    @numeroContrato, @fechaPactado, @fechaVencimiento,
                    @tipoCambioForward, @tipoCambioReferencia,
                    @equivalenteUsdRef, @diferenciaDeMas, @porcentajeDiferencia,
                    @nombreExportadora, @tcNegociadoExportadora, @comisionExportadora, @metodoExportadora,
                    NULL,  -- voucher siempre NULL al crear
                    NULLIF(@idTransaccionOrigen, 0),
                    'PENDIENTE', @observaciones, @audUsuario, GETDATE()
                );

                SET @idGenerado = SCOPE_IDENTITY();

                INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                VALUES (NULL, NULL, @idGenerado, NULL, 'PENDIENTE', 'Transacción creada.', @audUsuario, GETDATE());

                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción registrada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al insertar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
                SET @idGenerado = 0;
            END CATCH;
            RETURN;
        END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
        BEGIN
            IF ISNULL(@idTransaccion, 0) = 0
                BEGIN SET @error = 50; SET @errormsg = 'idTransaccion es obligatorio para actualizar.'; RETURN; END

            SELECT @estadoActual = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
            IF @estadoActual IS NULL
                BEGIN SET @error = 51; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END
            IF @estadoActual IN ('CONFIRMADO', 'CANCELADO')
                BEGIN SET @error = 52; SET @errormsg = 'No se puede modificar una transacción en estado ' + @estadoActual + '.'; RETURN; END

            IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
                BEGIN
                    IF NOT (
                        (@estadoActual = 'PENDIENTE'  AND @estado IN ('PROCESADO', 'CANCELADO'))
                        OR (@estadoActual = 'PROCESADO' AND @estado IN ('CONFIRMADO', 'CANCELADO'))
                    )
                    BEGIN
                        SET @error = 53;
                        SET @errormsg = 'Transición no permitida: ' + @estadoActual + ' → ' + @estado +
                                        '. Permitidas: PENDIENTE→PROCESADO, PENDIENTE→CANCELADO, ' +
                                        'PROCESADO→CONFIRMADO, PROCESADO→CANCELADO.';
                        RETURN;
                    END
                END

            DECLARE @tcRefActual       DECIMAL(10, 6);
            DECLARE @totalFinalActual  DECIMAL(18, 2);
            DECLARE @montoOrigenActual DECIMAL(18, 2);

            SELECT @tcRefActual       = tipoCambioReferencia,
                   @totalFinalActual  = totalFinal,
                   @montoOrigenActual = montoOrigen
            FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;

            DECLARE @tcRefFinal DECIMAL(10, 6) = ISNULL(@tipoCambioReferencia, @tcRefActual);
            DECLARE @tfFinal    DECIMAL(18, 2) = ISNULL(@totalFinal, @totalFinalActual);

            IF ISNULL(@tcRefFinal, 0) > 0 AND @tfFinal > 0
                BEGIN
                    SET @equivalenteUsdRef    = ROUND(@tfFinal / @tcRefFinal, 2);
                    SET @diferenciaDeMas      = ROUND(@equivalenteUsdRef - @montoOrigenActual, 2);
                    SET @porcentajeDiferencia = ROUND((@diferenciaDeMas / @montoOrigenActual) * 100, 4);
                END

            BEGIN TRY
                BEGIN TRANSACTION;

                UPDATE tpex_Transacciones
                SET fechaValor           = ISNULL(@fechaValor,           fechaValor),
                    numeroTransaccion    = ISNULL(@numeroTransaccion,    numeroTransaccion),
                    totalCargos          = ISNULL(@totalCargos,          totalCargos),
                    totalFinal           = ISNULL(@totalFinal,           totalFinal),
                    tipoCambioReferencia = ISNULL(@tipoCambioReferencia, tipoCambioReferencia),
                    equivalenteUsdRef    = ISNULL(@equivalenteUsdRef,    equivalenteUsdRef),
                    diferenciaDeMas      = ISNULL(@diferenciaDeMas,      diferenciaDeMas),
                    porcentajeDiferencia = ISNULL(@porcentajeDiferencia, porcentajeDiferencia),
                    comisionExportadora  = ISNULL(@comisionExportadora,  comisionExportadora),
                    rutaVoucher          = ISNULL(@rutaVoucher,          rutaVoucher),
                    idTransaccionOrigen  = ISNULL(NULLIF(@idTransaccionOrigen, 0), idTransaccionOrigen),
                    estado               = ISNULL(@estado,               estado),
                    observaciones        = ISNULL(@observaciones,        observaciones),
                    audUsuario           = @audUsuario,
                    audFecha             = GETDATE()
                WHERE idTransaccion = @idTransaccion;

                IF ISNULL(@estado, '') != '' AND @estado != @estadoActual
                    BEGIN
                        INSERT INTO dbo.tpex_LogEstados (idSolicitud, idCotizacion, idTransaccion, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
                        VALUES (NULL, NULL, @idTransaccion, @estadoActual, @estado, NULL, @audUsuario, GETDATE());
                    END

                SET @idGenerado = @idTransaccion;
                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción actualizada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al actualizar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            END CATCH;
            RETURN;
        END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
        BEGIN
            IF ISNULL(@idTransaccion, 0) = 0
                BEGIN SET @error = 60; SET @errormsg = 'idTransaccion es obligatorio para eliminar.'; RETURN; END

            SELECT @estadoActual = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
            IF @estadoActual IS NULL
                BEGIN SET @error = 61; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END
            IF @estadoActual != 'PENDIENTE'
                BEGIN SET @error = 62; SET @errormsg = 'Solo se pueden eliminar transacciones PENDIENTES. Estado actual: ' + @estadoActual + '.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_Cargos WHERE idTransaccion = @idTransaccion)
                BEGIN SET @error = 63; SET @errormsg = 'La transacción tiene cargos. Elimine primero los cargos.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_TransaccionParticipantes WHERE idTransaccion = @idTransaccion)
                BEGIN SET @error = 64; SET @errormsg = 'La transacción tiene participantes. Elimine primero los participantes.'; RETURN; END
            IF EXISTS (SELECT 1 FROM tpex_Transacciones WHERE idTransaccionOrigen = @idTransaccion)
                BEGIN SET @error = 65; SET @errormsg = 'La transacción tiene devoluciones vinculadas. Elimine primero las devoluciones.'; RETURN; END

            BEGIN TRY
                BEGIN TRANSACTION;
                DELETE FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
                COMMIT TRANSACTION;
                SET @error    = 0;
                SET @errormsg = 'Transacción eliminada correctamente.';
            END TRY
            BEGIN CATCH
                IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
                SET @error    = 99;
                SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            END CATCH;
            RETURN;
        END
END
GO

-- ============================================================================
-- PARTE 4: p_list_tpex_Transacciones — LEFT JOIN proveedor (cardCode ahora NULL)
--          Sin esto, fondeos/traspasos/devoluciones desaparecen de la grilla.
-- ============================================================================
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

-- ============================================================================
-- PARTE 5: p_abm_tpex_TransaccionParticipantes
-- ============================================================================
IF OBJECT_ID('dbo.p_abm_tpex_TransaccionParticipantes') IS NOT NULL
    DROP PROCEDURE dbo.p_abm_tpex_TransaccionParticipantes;
GO

CREATE PROCEDURE [dbo].[p_abm_tpex_TransaccionParticipantes]

      @ACCION           VARCHAR(1)
    , @idParticipante   BIGINT         = NULL
    , @idTransaccion    BIGINT         = NULL
    , @tipoParticipante VARCHAR(10)    = NULL   -- EMPRESA / TERCERO
    , @nombre           VARCHAR(100)   = NULL
    , @porcentaje       DECIMAL(8, 4)  = NULL
    , @montoUs          DECIMAL(18, 4) = NULL
    , @montoBs          DECIMAL(18, 4) = NULL
    , @itfUs            DECIMAL(18, 4) = NULL
    , @itfBs            DECIMAL(18, 4) = NULL
    , @observaciones    VARCHAR(500)   = NULL
    , @audUsuario       BIGINT         = NULL
    , @error            INT            = 0   OUTPUT
    , @errormsg         NVARCHAR(500)  = ''  OUTPUT
    , @idGenerado       BIGINT         = 0   OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SET @error      = 0;
    SET @errormsg   = '';
    SET @idGenerado = 0;

    DECLARE @estadoTrans     VARCHAR(20);
    DECLARE @tcTrans         DECIMAL(10, 6);
    DECLARE @montoConvertido DECIMAL(18, 2);

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idTransaccion, 0) = 0
        BEGIN SET @error = 10; SET @errormsg = 'idTransaccion es obligatorio.'; RETURN; END

        SELECT @estadoTrans = estado, @tcTrans = tipoCambioAplicado
        FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;

        IF @estadoTrans IS NULL
        BEGIN SET @error = 11; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END

        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 12; SET @errormsg = 'No se pueden agregar participantes a una transacción CANCELADA.'; RETURN; END

        IF ISNULL(@tipoParticipante, '') NOT IN ('EMPRESA', 'TERCERO')
        BEGIN SET @error = 13; SET @errormsg = 'tipoParticipante debe ser EMPRESA o TERCERO. Recibido: ' + ISNULL(@tipoParticipante, 'NULL') + '.'; RETURN; END

        IF ISNULL(@nombre, '') = ''
        BEGIN SET @error = 14; SET @errormsg = 'nombre del participante es obligatorio.'; RETURN; END

        IF ISNULL(@montoUs, 0) = 0 AND ISNULL(@montoBs, 0) = 0
        BEGIN SET @error = 15; SET @errormsg = 'Debe proporcionar montoUs o montoBs.'; RETURN; END

        IF ISNULL(@porcentaje, 0) < 0 OR ISNULL(@porcentaje, 0) > 100
        BEGIN SET @error = 16; SET @errormsg = 'porcentaje debe estar entre 0 y 100.'; RETURN; END

        -- Derivar el lado faltante con el TC de la transacción (mismo criterio que asientos)
        IF ISNULL(@tcTrans, 0) > 0
        BEGIN
            IF @montoUs IS NOT NULL AND @montoBs IS NULL SET @montoBs = ROUND(@montoUs * @tcTrans, 4);
            IF @montoBs IS NOT NULL AND @montoUs IS NULL SET @montoUs = ROUND(@montoBs / @tcTrans, 4);
            IF @itfUs   IS NOT NULL AND @itfBs   IS NULL SET @itfBs   = ROUND(@itfUs   * @tcTrans, 4);
            IF @itfBs   IS NOT NULL AND @itfUs   IS NULL SET @itfUs   = ROUND(@itfBs   / @tcTrans, 4);
        END

        -- Porcentaje automático respecto al monto convertido (USD) de la transacción
        IF @porcentaje IS NULL
        BEGIN
            SELECT @montoConvertido = montoConvertido FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
            IF ISNULL(@montoConvertido, 0) > 0 AND ISNULL(@montoUs, 0) > 0
                SET @porcentaje = ROUND((@montoUs / @montoConvertido) * 100, 4);
        END

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_TransaccionParticipantes
                (idTransaccion, tipoParticipante, nombre, porcentaje,
                 montoUs, montoBs, itfUs, itfBs,
                 observaciones, audUsuario, audFecha)
            VALUES
                (@idTransaccion, @tipoParticipante, @nombre, @porcentaje,
                 @montoUs, @montoBs, @itfUs, @itfBs,
                 @observaciones, @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante registrado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error      = 99;
            SET @errormsg   = 'Error al insertar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idParticipante, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'idParticipante es obligatorio para actualizar.'; RETURN; END

        DECLARE @idTransActual BIGINT;
        SELECT @idTransActual = idTransaccion FROM tpex_TransaccionParticipantes WHERE idParticipante = @idParticipante;
        IF @idTransActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el participante ' + CAST(@idParticipante AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado, @tcTrans = tipoCambioAplicado
        FROM tpex_Transacciones WHERE idTransaccion = @idTransActual;

        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 32; SET @errormsg = 'No se puede modificar un participante de una transacción CANCELADA.'; RETURN; END

        IF @tipoParticipante IS NOT NULL AND @tipoParticipante NOT IN ('EMPRESA', 'TERCERO')
        BEGIN SET @error = 33; SET @errormsg = 'tipoParticipante debe ser EMPRESA o TERCERO.'; RETURN; END

        IF ISNULL(@tcTrans, 0) > 0
        BEGIN
            IF @montoUs IS NOT NULL AND @montoBs IS NULL SET @montoBs = ROUND(@montoUs * @tcTrans, 4);
            IF @montoBs IS NOT NULL AND @montoUs IS NULL SET @montoUs = ROUND(@montoBs / @tcTrans, 4);
            IF @itfUs   IS NOT NULL AND @itfBs   IS NULL SET @itfBs   = ROUND(@itfUs   * @tcTrans, 4);
            IF @itfBs   IS NOT NULL AND @itfUs   IS NULL SET @itfUs   = ROUND(@itfBs   / @tcTrans, 4);
        END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_TransaccionParticipantes
            SET tipoParticipante = ISNULL(@tipoParticipante, tipoParticipante),
                nombre           = ISNULL(@nombre,           nombre),
                porcentaje       = ISNULL(@porcentaje,       porcentaje),
                montoUs          = ISNULL(@montoUs,          montoUs),
                montoBs          = ISNULL(@montoBs,          montoBs),
                itfUs            = ISNULL(@itfUs,            itfUs),
                itfBs            = ISNULL(@itfBs,            itfBs),
                observaciones    = ISNULL(@observaciones,    observaciones),
                audUsuario       = @audUsuario,
                audFecha         = GETDATE()
            WHERE idParticipante = @idParticipante;
            SET @idGenerado = @idParticipante;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante actualizado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al actualizar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idParticipante, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idParticipante es obligatorio para eliminar.'; RETURN; END

        DECLARE @idTransDel BIGINT;
        SELECT @idTransDel = idTransaccion FROM tpex_TransaccionParticipantes WHERE idParticipante = @idParticipante;
        IF @idTransDel IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el participante ' + CAST(@idParticipante AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransDel;
        IF @estadoTrans = 'CANCELADO'
        BEGIN SET @error = 52; SET @errormsg = 'No se puede eliminar un participante de una transacción CANCELADA.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            DELETE FROM tpex_TransaccionParticipantes WHERE idParticipante = @idParticipante;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Participante eliminado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al eliminar participante: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    SET @error    = 99;
    SET @errormsg = 'ACCION no válida.';
END
GO

-- ============================================================================
-- PARTE 6: p_list_tpex_TransaccionParticipantes
--   T = participantes de una transacción
--   V = resumen de cuadre (Σ montoUs vs montoConvertido de la transacción)
-- ============================================================================
IF OBJECT_ID('dbo.p_list_tpex_TransaccionParticipantes') IS NOT NULL
    DROP PROCEDURE dbo.p_list_tpex_TransaccionParticipantes;
GO

CREATE PROCEDURE [dbo].[p_list_tpex_TransaccionParticipantes]

      @ACCION           VARCHAR(1)  = NULL
    , @idParticipante   BIGINT      = NULL
    , @idTransaccion    BIGINT      = NULL
    , @tipoParticipante VARCHAR(10) = NULL
    , @audUsuario       BIGINT      = NULL
AS
BEGIN
    SET NOCOUNT ON;

    -- ================================================================
    -- T: Participantes de una transacción
    -- ================================================================
    IF @ACCION = 'T'
    BEGIN
        SELECT
             pa.idParticipante
            ,pa.idTransaccion
            ,pa.tipoParticipante
            ,pa.nombre
            ,pa.porcentaje
            ,pa.montoUs
            ,pa.montoBs
            ,pa.itfUs
            ,pa.itfBs
            ,pa.observaciones
            ,t.estado               AS estadoTransaccion
            ,t.fechaTransaccion
            ,t.montoOrigen
            ,t.montoConvertido
            ,pa.audUsuario
            ,pa.audFecha
        FROM tpex_TransaccionParticipantes pa
        JOIN tpex_Transacciones            t ON t.idTransaccion = pa.idTransaccion
        WHERE pa.idTransaccion = ISNULL(@idTransaccion, 0)
          AND (@tipoParticipante IS NULL OR pa.tipoParticipante = @tipoParticipante)
        ORDER BY pa.idParticipante;
        RETURN;
    END

    -- ================================================================
    -- V: Resumen de cuadre del split
    --    CUADRADO si Σ montoUs = montoConvertido de la transacción
    --    (tolerancia 0.01) y Σ porcentaje ≈ 100 cuando hay porcentajes.
    -- ================================================================
    IF @ACCION = 'V'
    BEGIN
        SELECT
             pa.idTransaccion
            ,t.fechaTransaccion
            ,t.estado                                      AS estadoTransaccion
            ,COUNT(*)                                      AS cantidadParticipantes
            ,ISNULL(SUM(pa.porcentaje), 0)                 AS totalPorcentaje
            ,ISNULL(SUM(pa.montoUs), 0)                    AS totalMontoUs
            ,ISNULL(SUM(pa.montoBs), 0)                    AS totalMontoBs
            ,ISNULL(SUM(pa.itfUs),  0)                     AS totalItfUs
            ,ISNULL(SUM(pa.itfBs),  0)                     AS totalItfBs
            ,t.montoConvertido
            ,ISNULL(SUM(pa.montoUs), 0) - t.montoConvertido AS diferenciaUs
            ,CASE
                WHEN ABS(ISNULL(SUM(pa.montoUs), 0) - t.montoConvertido) < 0.01
                THEN 'CUADRADO'
                ELSE 'DESCUADRADO'
             END                                           AS estadoCuadre
        FROM tpex_TransaccionParticipantes pa
        JOIN tpex_Transacciones            t ON t.idTransaccion = pa.idTransaccion
        WHERE pa.idTransaccion = ISNULL(@idTransaccion, 0)
        GROUP BY pa.idTransaccion, t.fechaTransaccion, t.estado, t.montoConvertido;
        RETURN;
    END

    -- ACCION no reconocida: vacío, consistente con el resto de SPs de listado
    SELECT TOP 0
         CAST(0 AS BIGINT)    AS idParticipante
        ,CAST('' AS VARCHAR(10)) AS tipoParticipante;
END
GO
