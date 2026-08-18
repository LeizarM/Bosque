/* ============================================================================
   2026-06-15  ·  p_abm_tpex_Cargos  ·  FIX cálculo de montoCargo (porcentaje)
   ----------------------------------------------------------------------------
   Problema:
     El SP calculaba  montoCargo = ROUND(baseCalculo * porcentaje, 2)
     tratando @porcentaje como una TASA (0.003). Pero la convención real del
     sistema (tabla tpex_ConfigComisionesBanco y el frontend) guarda el
     porcentaje como NÚMERO-PORCENTAJE: ITF 0,30%  =>  valorPorcentaje = 0.30.

     Con la fórmula vieja, un ITF de 0,30% sobre Bs 100.000 daba
        100000 * 0.30 = Bs 30.000   (¡100× de más!)
     cuando debe ser
        100000 * 0.30 / 100 = Bs 300.

     El getter del frontend ya divide entre 100 (base * porcentaje / 100), así
     que el SP quedaba inconsistente con la vista (la previsualización mostraba
     Bs 300 pero se guardaba Bs 30.000).

   Fix: dividir entre 100 en el cálculo del INSERT y del UPDATE.
        (tpex_Cargos está vacío, no hay datos que migrar.)
   ============================================================================ */

ALTER PROCEDURE [dbo].[p_abm_tpex_Cargos]
      @ACCION          VARCHAR(1)
    , @idCargo         BIGINT          = NULL
    , @idCotizacion    BIGINT          = NULL
    , @idTransaccion   BIGINT          = NULL
    , @idTipoCargo     BIGINT          = NULL
    , @baseCalculo     DECIMAL(18,2)   = NULL
    , @origenBase      VARCHAR(50)     = NULL   -- MONTO_ORIGEN / MONTO_CONVERTIDO / VALOR_FIJO
    , @porcentaje      DECIMAL(10,6)   = NULL
    , @valorFijo       DECIMAL(18,2)   = NULL
    , @idMoneda        BIGINT          = NULL
    , @orden           INT             = NULL
    , @descripcion     VARCHAR(200)    = NULL
    , @audUsuario      BIGINT          = NULL
    , @error           INT             = 0   OUTPUT
    , @errormsg        NVARCHAR(500)   = ''  OUTPUT
    , @idGenerado      BIGINT          = 0   OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error     = 0;
    SET @errormsg  = '';
    SET @idGenerado = 0;

    DECLARE @montoCargo       DECIMAL(18,2);
    DECLARE @estadoTrans      VARCHAR(20);
    DECLARE @estadoCot        VARCHAR(20);
    DECLARE @esPorcentaje     BIT;
    DECLARE @idTransRef       BIGINT;   -- transaccion a recalcular

    -- ================================================================
    -- INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idCotizacion, 0) = 0 AND ISNULL(@idTransaccion, 0) = 0
        BEGIN
            SET @error = 10; SET @errormsg = 'Debe indicar idCotizacion o idTransaccion.'; RETURN;
        END

        IF ISNULL(@idCotizacion, 0) != 0
        BEGIN
            SELECT @estadoCot = estado FROM tpex_Cotizaciones WHERE idCotizacion = @idCotizacion;
            IF @estadoCot IS NULL
            BEGIN SET @error = 11; SET @errormsg = 'No existe la cotización ' + CAST(@idCotizacion AS VARCHAR) + '.'; RETURN; END
        END

        IF ISNULL(@idTransaccion, 0) != 0
        BEGIN
            SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransaccion;
            IF @estadoTrans IS NULL
            BEGIN SET @error = 12; SET @errormsg = 'No existe la transacción ' + CAST(@idTransaccion AS VARCHAR) + '.'; RETURN; END
            IF @estadoTrans IN ('CONFIRMADO', 'CANCELADO')
            BEGIN SET @error = 13; SET @errormsg = 'No se pueden agregar cargos a una transacción en estado ' + @estadoTrans + '.'; RETURN; END
            SET @idTransRef = @idTransaccion;
        END

        IF ISNULL(@idTipoCargo, 0) = 0
        BEGIN SET @error = 14; SET @errormsg = 'idTipoCargo es obligatorio.'; RETURN; END

        SELECT @esPorcentaje = esPorcentaje
        FROM tpex_TiposCargo WHERE idTipoCargo = @idTipoCargo AND activo = 1;
        IF @esPorcentaje IS NULL
        BEGIN SET @error = 15; SET @errormsg = 'Tipo de cargo inexistente o inactivo.'; RETURN; END

        IF ISNULL(@idMoneda, 0) = 0 OR NOT EXISTS (SELECT 1 FROM tpex_Monedas WHERE idMoneda = @idMoneda AND activo = 1)
        BEGIN SET @error = 16; SET @errormsg = 'idMoneda inválida o inactiva.'; RETURN; END

        IF ISNULL(@baseCalculo, 0) <= 0
        BEGIN SET @error = 17; SET @errormsg = 'baseCalculo debe ser mayor a cero.'; RETURN; END

        -- Calcular montoCargo
        IF @esPorcentaje = 1
        BEGIN
            IF ISNULL(@porcentaje, 0) = 0
            BEGIN SET @error = 18; SET @errormsg = 'El tipo de cargo es porcentual — debe enviar @porcentaje.'; RETURN; END
            -- FIX: @porcentaje es número-porcentaje (0.30 = 0,30%), dividir entre 100
            SET @montoCargo = ROUND(@baseCalculo * @porcentaje / 100.0, 2);
        END
        ELSE
        BEGIN
            IF ISNULL(@valorFijo, 0) = 0
            BEGIN SET @error = 19; SET @errormsg = 'El tipo de cargo es valor fijo — debe enviar @valorFijo.'; RETURN; END
            SET @montoCargo = @valorFijo;
        END

        IF ISNULL(@orden, 0) = 0
        BEGIN
            SELECT @orden = ISNULL(MAX(orden), 0) + 1
            FROM tpex_Cargos
            WHERE (idTransaccion = @idTransaccion OR idCotizacion = @idCotizacion);
        END

        BEGIN TRY
            BEGIN TRANSACTION;

            INSERT INTO tpex_Cargos
                (idCotizacion, idTransaccion, idTipoCargo, baseCalculo, origenBase,
                 porcentaje, valorFijo, montoCargo, idMoneda, orden, descripcion,
                 audUsuario, audFecha)
            VALUES
                (@idCotizacion, @idTransaccion, @idTipoCargo, @baseCalculo,
                 ISNULL(@origenBase, 'MONTO_CONVERTIDO'),
                 @porcentaje, @valorFijo, @montoCargo,
                 @idMoneda, @orden, @descripcion,
                 @audUsuario, GETDATE());

            SET @idGenerado = SCOPE_IDENTITY();

            IF @idTransRef IS NOT NULL
            BEGIN
                UPDATE tpex_Transacciones
                SET totalCargos = (
                        SELECT ISNULL(SUM(montoCargo), 0)
                        FROM tpex_Cargos
                        WHERE idTransaccion = @idTransRef
                    ),
                    totalFinal = montoConvertido + (
                        SELECT ISNULL(SUM(montoCargo), 0)
                        FROM tpex_Cargos
                        WHERE idTransaccion = @idTransRef
                    ),
                    audUsuario = @audUsuario,
                    audFecha   = GETDATE()
                WHERE idTransaccion = @idTransRef;
            END

            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Cargo registrado correctamente. montoCargo = ' + CAST(@montoCargo AS VARCHAR(30));
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al insertar cargo: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idCargo, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'idCargo es obligatorio para actualizar.'; RETURN; END

        SELECT @idTransRef = idTransaccion
        FROM tpex_Cargos WHERE idCargo = @idCargo;
        IF @idTransRef IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el cargo ' + CAST(@idCargo AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransRef;
        IF @estadoTrans IN ('CONFIRMADO', 'CANCELADO')
        BEGIN SET @error = 32; SET @errormsg = 'No se pueden modificar cargos de una transacción en estado ' + @estadoTrans + '.'; RETURN; END

        IF @porcentaje IS NOT NULL OR @valorFijo IS NOT NULL OR @baseCalculo IS NOT NULL
        BEGIN
            DECLARE @baseFinal    DECIMAL(18,2);
            DECLARE @pctFinal     DECIMAL(10,6);
            DECLARE @fijoFinal    DECIMAL(18,2);
            DECLARE @esPctActual  BIT;

            SELECT @baseFinal   = ISNULL(@baseCalculo, baseCalculo),
                   @pctFinal    = ISNULL(@porcentaje,  porcentaje),
                   @fijoFinal   = ISNULL(@valorFijo,   valorFijo),
                   @esPctActual = tc.esPorcentaje
            FROM tpex_Cargos c
            JOIN tpex_TiposCargo tc ON tc.idTipoCargo = c.idTipoCargo
            WHERE c.idCargo = @idCargo;

            IF @esPctActual = 1
                -- FIX: número-porcentaje, dividir entre 100
                SET @montoCargo = ROUND(@baseFinal * @pctFinal / 100.0, 2);
            ELSE
                SET @montoCargo = @fijoFinal;
        END
        ELSE
        BEGIN
            SELECT @montoCargo = montoCargo FROM tpex_Cargos WHERE idCargo = @idCargo;
        END

        BEGIN TRY
            BEGIN TRANSACTION;

            UPDATE tpex_Cargos
            SET baseCalculo  = ISNULL(@baseCalculo,  baseCalculo),
                origenBase   = ISNULL(@origenBase,   origenBase),
                porcentaje   = ISNULL(@porcentaje,   porcentaje),
                valorFijo    = ISNULL(@valorFijo,    valorFijo),
                montoCargo   = @montoCargo,
                idMoneda     = ISNULL(@idMoneda,     idMoneda),
                orden        = ISNULL(@orden,        orden),
                descripcion  = ISNULL(@descripcion,  descripcion),
                audUsuario   = @audUsuario,
                audFecha     = GETDATE()
            WHERE idCargo = @idCargo;

            UPDATE tpex_Transacciones
            SET totalCargos = (
                    SELECT ISNULL(SUM(montoCargo), 0)
                    FROM tpex_Cargos WHERE idTransaccion = @idTransRef
                ),
                totalFinal = montoConvertido + (
                    SELECT ISNULL(SUM(montoCargo), 0)
                    FROM tpex_Cargos WHERE idTransaccion = @idTransRef
                ),
                audUsuario = @audUsuario,
                audFecha   = GETDATE()
            WHERE idTransaccion = @idTransRef;

            SET @idGenerado = @idCargo;
            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Cargo actualizado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al actualizar cargo: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idCargo, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idCargo es obligatorio para eliminar.'; RETURN; END

        SELECT @idTransRef = idTransaccion
        FROM tpex_Cargos WHERE idCargo = @idCargo;
        IF @idTransRef IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el cargo ' + CAST(@idCargo AS VARCHAR) + '.'; RETURN; END

        SELECT @estadoTrans = estado FROM tpex_Transacciones WHERE idTransaccion = @idTransRef;
        IF @estadoTrans IN ('CONFIRMADO', 'CANCELADO')
        BEGIN SET @error = 52; SET @errormsg = 'No se pueden eliminar cargos de una transacción en estado ' + @estadoTrans + '.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;

            DELETE FROM tpex_Cargos WHERE idCargo = @idCargo;

            UPDATE tpex_Transacciones
            SET totalCargos = (
                    SELECT ISNULL(SUM(montoCargo), 0)
                    FROM tpex_Cargos WHERE idTransaccion = @idTransRef
                ),
                totalFinal = montoConvertido + (
                    SELECT ISNULL(SUM(montoCargo), 0)
                    FROM tpex_Cargos WHERE idTransaccion = @idTransRef
                ),
                audUsuario = @audUsuario,
                audFecha   = GETDATE()
            WHERE idTransaccion = @idTransRef;

            COMMIT TRANSACTION;
            SET @error    = 0;
            SET @errormsg = 'Cargo eliminado y totales recalculados.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error    = 99;
            SET @errormsg = 'Error al eliminar cargo: ' + ERROR_MESSAGE() + ' | Línea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    SET @error = 99;
    SET @errormsg = 'ACCION no válida.';
END
GO
