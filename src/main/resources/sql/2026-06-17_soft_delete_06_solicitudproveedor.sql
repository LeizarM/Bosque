/* SOFT DELETE - Entidad 4/9: SOLICITUD PROVEEDOR
   Fase 2 (ABM): accion D -> UPDATE eliminado=1 (mantiene guardas).
   Fase 3 (p_list_tpex_SolicitudProveedor): filtra eliminado=0 en L (sin alias).
   Accion A es OPENQUERY a SAP (proveedores por empresa), no se filtra.
   REQUISITO: 01_schema. Ejecutar con usuario de ESCRITURA. */
ALTER PROCEDURE [dbo].[p_abm_tpex_SolicitudProveedor]
    @ACCION VARCHAR(1)
    , @idSolicitudProveedor BIGINT = NULL
    , @idSolicitud BIGINT = NULL
    , @cardCode VARCHAR(50) = NULL
    , @cardName VARCHAR(255) = NULL
    , @totalFacturasUsd DECIMAL(18, 2) = NULL
    , @totalAmortizadoUsd DECIMAL(18, 2) = NULL
    , @totalAPagarUsd DECIMAL(18, 2) = NULL
    , @obs VARCHAR(255) = NULL
    , @estado VARCHAR(20) = NULL
    , @obsAprobacion VARCHAR(500) = NULL
    , @audUsuario INT = NULL
    , @error INT = 0 OUTPUT
    , @errormsg NVARCHAR(500) = '' OUTPUT
    , @idGenerado BIGINT = 0 OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @estadoSolicitud  VARCHAR(20);
    DECLARE @estadoActual     VARCHAR(20);
    DECLARE @idSolicitudProv  BIGINT;
    DECLARE @countCuotasNoAprob INT;

    -- ================================================================
    --  INSERT
    -- ================================================================
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idSolicitud, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'El campo idSolicitud es obligatorio.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tpex_SolicitudPago WHERE idSolicitud = @idSolicitud)
        BEGIN SET @error = 21; SET @errormsg = 'No existe la solicitud con idSolicitud = ' + CAST(@idSolicitud AS VARCHAR) + '.'; RETURN; END

        IF ISNULL(@cardCode, '') = ''
        BEGIN SET @error = 22; SET @errormsg = 'El campo cardCode es obligatorio.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tpex_SolicitudProveedor WHERE idSolicitud = @idSolicitud AND cardCode = @cardCode)
        BEGIN SET @error = 23; SET @errormsg = 'El proveedor ' + @cardCode + ' ya existe en esta solicitud.'; RETURN; END

        IF ISNULL(@totalAPagarUsd, 0) < 0
        BEGIN SET @error = 24; SET @errormsg = 'El totalAPagarUsd no puede ser negativo.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            INSERT INTO tpex_SolicitudProveedor (idSolicitud, cardCode, cardName,
                                                 totalFacturasUsd, totalAmortizadoUsd, totalAPagarUsd,
                                                 obs, estado, audUsuario, audFecha)
            VALUES (@idSolicitud, @cardCode, @cardName,
                    ISNULL(@totalFacturasUsd, 0),
                    ISNULL(@totalAmortizadoUsd, 0),
                    ISNULL(@totalAPagarUsd, 0),
                    @obs, 'PENDIENTE', @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitud, @idGenerado, NULL, 'PENDIENTE', 'Proveedor agregado a solicitud.', @audUsuario, GETDATE());

            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor insertado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al insertar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
            SET @idGenerado = 0;
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- UPDATE (datos basicos del proveedor)
    -- ================================================================
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'El campo idSolicitudProveedor es obligatorio para actualizar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'No existe el registro con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual IN ('APROBADO','RECHAZADO')
        BEGIN SET @error = 32; SET @errormsg = 'El proveedor esta ' + @estadoActual + '. Para modificar use ACCION A/R.'; RETURN; END

        IF ISNULL(@cardCode, '') = ''
        BEGIN SET @error = 33; SET @errormsg = 'El campo cardCode es obligatorio para actualizar.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor
            SET cardCode          = @cardCode,
                cardName          = @cardName,
                totalFacturasUsd  = ISNULL(@totalFacturasUsd, totalFacturasUsd),
                totalAmortizadoUsd= ISNULL(@totalAmortizadoUsd, totalAmortizadoUsd),
                totalAPagarUsd    = ISNULL(@totalAPagarUsd, totalAPagarUsd),
                obs               = @obs,
                audUsuario        = @audUsuario,
                audFecha          = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;
            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor actualizado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al actualizar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- DELETE
    -- ================================================================
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'El campo idSolicitudProveedor es obligatorio para eliminar.'; RETURN; END

        SELECT @estadoActual = estado FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;
        IF @estadoActual IS NULL
        BEGIN SET @error = 41; SET @errormsg = 'No existe el registro con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'APROBADO'
        BEGIN SET @error = 42; SET @errormsg = 'No se puede eliminar un proveedor APROBADO. Rechacelo primero.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor)
        BEGIN SET @error = 43; SET @errormsg = 'El proveedor tiene facturas asociadas. Elimine primero las lineas de detalle.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor SET eliminado = 1, audUsuario = @audUsuario, audFecha = GETDATE() WHERE idSolicitudProveedor = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor eliminado correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al eliminar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'A' : Aprobar proveedor (manual)
    -- ================================================================
    IF @ACCION = 'A'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'idSolicitudProveedor es obligatorio para aprobar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 51; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'APROBADO'
        BEGIN SET @error = 52; SET @errormsg = 'El proveedor ya esta APROBADO.'; RETURN; END

        IF @estadoActual = 'RECHAZADO'
        BEGIN SET @error = 53; SET @errormsg = 'El proveedor esta RECHAZADO y no se puede aprobar.'; RETURN; END

        -- Validar que tenga al menos 1 cuota
        IF NOT EXISTS (SELECT 1 FROM tpex_DetalleSolicitud WHERE idSolicitudProveedor = @idSolicitudProveedor)
        BEGIN SET @error = 54; SET @errormsg = 'No se puede aprobar un proveedor sin cuotas registradas.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            -- Aprobar todas las cuotas del proveedor (al aprobar manual)
            UPDATE tpex_DetalleSolicitud
            SET esAprobado = 1, audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor AND esAprobado = 0;

            UPDATE tpex_SolicitudProveedor
            SET estado = 'APROBADO',
                fechaAprobacion = GETDATE(),
                usuarioAprobador = @audUsuario,
                obsAprobacion = @obsAprobacion,
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudProv, @idSolicitudProveedor, @estadoActual, 'APROBADO',
                    ISNULL(@obsAprobacion, 'Aprobacion manual del proveedor.'), @audUsuario, GETDATE());

            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor APROBADO correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al aprobar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END

    -- ================================================================
    -- ACCION = 'R' : Rechazar proveedor
    -- ================================================================
    IF @ACCION = 'R'
    BEGIN
        IF ISNULL(@idSolicitudProveedor, 0) = 0
        BEGIN SET @error = 60; SET @errormsg = 'idSolicitudProveedor es obligatorio para rechazar.'; RETURN; END

        SELECT @estadoActual = estado, @idSolicitudProv = idSolicitud
        FROM tpex_SolicitudProveedor WHERE idSolicitudProveedor = @idSolicitudProveedor;

        IF @estadoActual IS NULL
        BEGIN SET @error = 61; SET @errormsg = 'No existe el proveedor con idSolicitudProveedor = ' + CAST(@idSolicitudProveedor AS VARCHAR) + '.'; RETURN; END

        IF @estadoActual = 'RECHAZADO'
        BEGIN SET @error = 62; SET @errormsg = 'El proveedor ya esta RECHAZADO.'; RETURN; END

        BEGIN TRY
            BEGIN TRANSACTION;
            UPDATE tpex_SolicitudProveedor
            SET estado = 'RECHAZADO',
                fechaAprobacion = GETDATE(),
                usuarioAprobador = @audUsuario,
                obsAprobacion = @obsAprobacion,
                audUsuario = @audUsuario, audFecha = GETDATE()
            WHERE idSolicitudProveedor = @idSolicitudProveedor;

            INSERT INTO tpex_LogEstados (idSolicitud, idSolicitudProveedor, estadoAnterior, estadoNuevo, observaciones, audUsuario, audFecha)
            VALUES (@idSolicitudProv, @idSolicitudProveedor, @estadoActual, 'RECHAZADO',
                    ISNULL(@obsAprobacion, 'Proveedor rechazado.'), @audUsuario, GETDATE());

            SET @idGenerado = @idSolicitudProveedor;
            COMMIT TRANSACTION;
            SET @errormsg = 'Proveedor RECHAZADO correctamente.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
            SET @error = 99;
            SET @errormsg = 'Error al rechazar: ' + ERROR_MESSAGE() + ' | Linea: ' + CAST(ERROR_LINE() AS VARCHAR);
        END CATCH;
        RETURN;
    END
END

GO

ALTER PROCEDURE [dbo].[p_list_tpex_SolicitudProveedor] @idSolicitudProveedor bigint = NULL, @idSolicitud bigint = NULL, @cardCode varchar(50) = NULL, @cardName varchar(255) = NULL, @totalFacturasUsd decimal(18, 2) = NULL, @totalAmortizadoUsd decimal(18, 2) = NULL, @totalAPagarUsd decimal(18, 2) = NULL, @obs varchar(255) = NULL, @audUsuario int = NULL, @ACCION VARCHAR(1) = NULL, @codEmpresa int = NULLASBEGIN	 -- =====LISTAR==================================	 IF(@ACCION = 'L')	 BEGIN		 SELECT			 idSolicitudProveedor,			 idSolicitud,			 cardCode,			 cardName,			 totalFacturasUsd,			 totalAmortizadoUsd,			 totalAPagarUsd,			 obs,			 audUsuario					FROM tpex_SolicitudProveedor		WHERE eliminado = 0 AND (@idSolicitudProveedor IS NULL OR @idSolicitudProveedor = idSolicitudProveedor) 		AND (@idSolicitud IS NULL OR @idSolicitud = idSolicitud) 		AND (@cardCode IS NULL OR @cardCode = cardCode) 		AND (@cardName IS NULL OR @cardName = cardName) 		AND (@totalFacturasUsd IS NULL OR @totalFacturasUsd = totalFacturasUsd) 		AND (@totalAmortizadoUsd IS NULL OR @totalAmortizadoUsd = totalAmortizadoUsd) 		AND (@totalAPagarUsd IS NULL OR @totalAPagarUsd = totalAPagarUsd) 		AND (@obs IS NULL OR @obs = obs) 		AND (@audUsuario IS NULL OR @audUsuario = audUsuario) 			 END	 	  --- ============== PARA LISTAR LOS PROVEEDORES POR EMPRESA ======================	 IF( @ACCION = 'A' )	 BEGIN		 			DECLARE @sqlProv    NVARCHAR(MAX);  
			DECLARE @tempProv   TABLE (  
				cardCode	VARCHAR(10),  
				cardName    VARCHAR(300),
				moneda 		VARCHAR(5)
			);  
  
			 
			SET @sqlProv = N'SELECT * FROM OPENQUERY(SRV_2022,   
				''EXEC [CONEXION].[dbo].[p_list_Proveedores]   
					@codEmpresa = ' + CONVERT(VARCHAR(5), @codEmpresa) + ',   
					@ACCION = ''''A'''''')';  
  
			INSERT INTO @tempProv 
			EXEC sp_executesql @sqlProv			SELECT 				t1.* 			FROM @tempProv t1			ORDER BY t1.cardName	 END	END

GO

SELECT
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_SolicitudProveedor')  AND definition LIKE '%UPDATE tpex_SolicitudProveedor SET eliminado = 1%') AS abm_soft_ok,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_abm_tpex_SolicitudProveedor')  AND definition LIKE '%DELETE FROM tpex_SolicitudProveedor%')                AS abm_delete_residual,
 (SELECT COUNT(*) FROM sys.sql_modules WHERE object_id=OBJECT_ID('dbo.p_list_tpex_SolicitudProveedor') AND definition LIKE '%eliminado = 0 AND%')                                   AS list_filtra_ok;
