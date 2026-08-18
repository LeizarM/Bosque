/* ============================================================================
   CARTAS CITE — SPs nuevos para el backend Spring / app Flutter
   Fecha: 2026-08-16

   Migración del módulo JSF `web/Bosque/tcrDocumento` (Bosque v2) al backend
   Spring Boot + Flutter. Trabaja sobre las MISMAS tablas tcr_* que el módulo
   viejo.

   NO TOCA NINGÚN SP EXISTENTE. `p_abm_Documento`, `p_list_documento`,
   `p_list_registroDoc`, `p_abm_copiaArch`, `p_abm_copiaEncab`,
   `p_abm_remitente` quedan intactos para que el JSF siga funcionando en
   paralelo. Los reportes Jasper también siguen usando `p_list_registroDoc`.

   Objetos que crea:
     tcr_documentoAnulado    tabla nueva (baja lógica fuera de tcr_documento)
     p_abm_tcr_Documento     I / U / D / X (exportado) / G (rollover gestión)
     p_abm_tcr_CopiaArch     I / U / D
     p_abm_tcr_CopiaEncab    I / U / D
     p_abm_tcr_Remitente     I / U / D
     p_list_tcr_Documento    L R A T E M C U G H I J
     UX_tcr_documento_correlativo   índice único: dos documentos no pueden
                                    compartir CITE, los inserte quien los inserte

   Convención de la casa (tpex): @error / @errormsg / @idGenerado OUTPUT, que
   es lo que `SpHelper.ejecutarAbm` lee. Sin esos OUTPUT el helper no funciona
   — es la razón por la que hacen falta SPs nuevos y no alcanzaba con los
   viejos.

   SQL Server 2008 (10.0.1600). Nada de TRY_CONVERT / IIF / CONCAT / FORMAT /
   LAG / LEAD / OFFSET-FETCH: no existen hasta 2012.
   ============================================================================ */

USE [BOSQUE-2_0];
GO
SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO


/* ============================================================================
   0) tcr_documentoAnulado — la baja lógica, en su propia tabla
   ============================================================================

   La primera versión de esto marcaba la anulación con `tcr_documento.estado='E'`.
   Estaba mal, y el motivo es el módulo viejo:

   `p_list_documento` ACCION 'A' —la previsualización del JSF— cuenta
   `COUNT(*) ... WHERE estado='V'`. Si un documento pasaba a 'E', esa cuenta
   bajaba en uno y el JSF entregaba un número que ya estaba emitido. O sea que
   anular desde la app nueva le rompía la numeración al sistema viejo.

   Sacando la marca de anulación fuera de `tcr_documento`, el JSF sigue
   contando exactamente las mismas filas que antes y su numeración no se entera
   de nada. La tabla no se toca: ni una columna nueva, ni un valor nuevo en
   `estado`. Todo lo que el sistema viejo lee sigue igual.

   De paso queda el rastro de quién anuló y cuándo, que con una letra en
   `estado` no existía.
   ============================================================================ */
IF OBJECT_ID('dbo.tcr_documentoAnulado', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.tcr_documentoAnulado (
         idDocumento BIGINT       NOT NULL PRIMARY KEY
        ,motivo      VARCHAR(300) NULL
        ,audUsuario  BIGINT       NULL
        ,audFecha    DATETIME     NOT NULL DEFAULT (GETDATE())
    );
END
GO


/* ============================================================================
   1) p_abm_tcr_Documento
   ============================================================================

   Correcciones respecto de `p_abm_Documento`:

   a) NUMERACIÓN DEL CITE. El viejo usa COUNT(*)+1. Con eso, borrar un
      documento hace que el siguiente repita un número ya emitido, y dos
      usuarios guardando a la vez obtienen el mismo. Acá es MAX(nroCite)+1
      calculado DENTRO de la transacción con (UPDLOCK, HOLDLOCK), que serializa
      el rango: dos sesiones simultáneas no pueden sacar el mismo número.

   b) El viejo recalcula el número sólo para idTipoDoc=1 y para el resto confía
      en el que mandó el cliente, que lo leyó de la pantalla hace minutos. Acá
      se asigna siempre en el servidor, en el mismo instante del INSERT. El
      @nroCite que manda el cliente se ignora en la 'I' (es sólo previsualización).

   c) El viejo recupera el id recién insertado con
      `SELECT TOP 1 idDocumento FROM tcr_documento ORDER BY idDocumento DESC`,
      que bajo concurrencia devuelve el documento de OTRO usuario y le cuelga
      el registroDoc / las copias al documento equivocado. Acá: SCOPE_IDENTITY().

   d) El viejo mezclaba previsualización y guardado: la vista previa filtra
      estado='V' y el INSERT no, así que el número mostrado y el guardado podían
      diferir. Acá los dos usan exactamente el mismo criterio.

   e) La baja es lógica y vive en `tcr_documentoAnulado`, no en la fila. Un
      correlativo emitido no se reutiliza nunca: si se borrara la fila, MAX+1
      volvería a entregar ese número y habría dos cartas distintas con el mismo
      CITE en el archivo. Y va fuera de `tcr_documento` para no moverle la
      cuenta al módulo viejo (ver la sección 0).
*/
IF OBJECT_ID('dbo.p_abm_tcr_Documento', 'P') IS NOT NULL
    DROP PROCEDURE dbo.p_abm_tcr_Documento;
GO
CREATE PROCEDURE [dbo].[p_abm_tcr_Documento]
     @ACCION        VARCHAR(1)
    ,@idDocumento   BIGINT        = NULL
    ,@idTipoDoc     BIGINT        = NULL
    ,@idGestion     BIGINT        = NULL
    ,@codEmpresa    BIGINT        = NULL
    ,@codUsuario    BIGINT        = NULL
    ,@codEmpleado   BIGINT        = NULL
    ,@empleadoDe    VARCHAR(250)  = NULL
    ,@cargoDe       VARCHAR(250)  = NULL
    ,@ciudad        VARCHAR(30)   = NULL
    ,@area          VARCHAR(30)   = NULL
    ,@nroCite       INT           = NULL
    ,@fechaDoc      DATE          = NULL
    ,@dirigido      VARCHAR(200)  = NULL
    ,@cargoDirigido VARCHAR(200)  = NULL
    ,@referencia    VARCHAR(MAX)  = NULL
    ,@via           VARCHAR(200)  = NULL
    ,@cargoVia      VARCHAR(200)  = NULL
    ,@asunto        VARCHAR(200)  = NULL
    ,@cuerpo        VARCHAR(MAX)  = NULL
    ,@estado        VARCHAR(5)    = NULL
    ,@motivo        VARCHAR(300)  = NULL   -- sólo ACCION 'D'
    ,@audUsuario    BIGINT        = NULL
    ,@error         INT           = 0  OUTPUT
    ,@errormsg      NVARCHAR(500) = '' OUTPUT
    ,@idGenerado    BIGINT        = 0  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @gestionActual  INT;
    DECLARE @estadoActual   VARCHAR(5);
    DECLARE @tipoNombre     VARCHAR(50);

    /* ---------------------------------------------------------------------
       G — Rollover de gestión.
       El módulo viejo hacía esto en cada apertura del formulario (ACCION 'B'
       de p_abm_Documento). Se mantiene, porque la numeración del CITE cuelga
       de la gestión activa: sin rollover, en enero se sigue numerando sobre
       el año anterior.
       Deja EXACTAMENTE una gestión activa: la del año en curso.
       --------------------------------------------------------------------- */
    IF @ACCION = 'G'
    BEGIN
        SET @gestionActual = YEAR(GETDATE());

        BEGIN TRY
            BEGIN TRAN;

                IF NOT EXISTS (SELECT 1 FROM tcr_gestion WITH (UPDLOCK, HOLDLOCK)
                               WHERE gestion = @gestionActual)
                BEGIN
                    INSERT INTO tcr_gestion (gestion, activo, audUsuario, audFecha)
                    VALUES (@gestionActual, 'NO', @audUsuario, GETDATE());
                END

                /* Un solo UPDATE en vez de la cascada de IFs del SP viejo, que
                   dejaba dos gestiones activas si se lo llamaba concurrente. */
                UPDATE tcr_gestion
                   SET activo = CASE WHEN gestion = @gestionActual THEN 'SI' ELSE 'NO' END
                 WHERE activo <> CASE WHEN gestion = @gestionActual THEN 'SI' ELSE 'NO' END
                    OR activo IS NULL;

            COMMIT TRAN;

            SELECT @idGenerado = idGestion FROM tcr_gestion WHERE gestion = @gestionActual;
            SET @errormsg = 'Gestión vigente: ' + CAST(@gestionActual AS VARCHAR(4));
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRAN;
            SET @error = 90; SET @errormsg = 'Error actualizando la gestión: ' + ERROR_MESSAGE();
        END CATCH

        RETURN;
    END

    /* ---------------------------------------------------------------------
       I — Alta
       --------------------------------------------------------------------- */
    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idTipoDoc, 0) = 0
        BEGIN SET @error = 10; SET @errormsg = 'Debe seleccionar el tipo de documento.'; RETURN; END

        SELECT @tipoNombre = tipo FROM tcr_tipoDocumento WHERE idTipoDoc = @idTipoDoc;
        IF @tipoNombre IS NULL
        BEGIN SET @error = 11; SET @errormsg = 'El tipo de documento no existe.'; RETURN; END

        IF ISNULL(@codEmpresa, 0) = 0
        BEGIN SET @error = 12; SET @errormsg = 'Debe seleccionar la empresa.'; RETURN; END

        IF NOT EXISTS (SELECT 1 FROM tb_empresa WHERE codEmpresa = @codEmpresa)
        BEGIN SET @error = 13; SET @errormsg = 'La empresa seleccionada no existe.'; RETURN; END

        IF ISNULL(@codUsuario, 0) = 0
        BEGIN SET @error = 14; SET @errormsg = 'No se pudo determinar el usuario que redacta.'; RETURN; END

        IF LEN(LTRIM(RTRIM(ISNULL(@cuerpo, '')))) = 0
        BEGIN SET @error = 15; SET @errormsg = 'El cuerpo del documento no puede estar vacío.'; RETURN; END

        /* 'A' es el placeholder "-Seleccione Area-" del combo. El SP viejo lo
           dejaba pasar y grababa cartas con área 'A', que después imprimían un
           CITE "A/007/2025". Acá se rechaza. El certificado de trabajo (6) es
           la excepción: su área la fija el sistema. */
        IF @idTipoDoc <> 6 AND (LEN(LTRIM(RTRIM(ISNULL(@area, '')))) = 0 OR LTRIM(RTRIM(@area)) = 'A')
        BEGIN SET @error = 16; SET @errormsg = 'Debe seleccionar el área que emite el documento.'; RETURN; END

        IF @fechaDoc IS NULL SET @fechaDoc = CAST(GETDATE() AS DATE);

        /* Defaults por tipo — mismas reglas que el SP viejo, para que los
           reportes Jasper (que ya esperan 'N/A') no cambien de aspecto. */
        IF @idTipoDoc = 1
        BEGIN
            SET @asunto = 'N/A';
        END
        IF @idTipoDoc = 2 OR @idTipoDoc = 7
        BEGIN
            SET @referencia = 'N/A';
        END
        IF @idTipoDoc = 6
        BEGIN
            SET @dirigido      = 'N/A';
            SET @referencia    = 'N/A';
            SET @asunto        = 'N/A';
            SET @area          = 'G.A.';   -- Gerencia Administrativa, pedido de RRHH
            SET @cargoDirigido = 'N/A';
        END
        IF @idTipoDoc = 8
        BEGIN
            SET @referencia = 'N/A';
        END
        IF @idTipoDoc = 9
        BEGIN
            /* COM. CI: lo que el usuario cargó como destinatario es en realidad
               el "DE:" del formato. Mismo intercambio que hacía el viejo. */
            SET @empleadoDe    = @dirigido;
            SET @cargoDe       = @cargoDirigido;
            SET @dirigido      = 'N/A';
            SET @cargoDirigido = 'N/A';
            SET @asunto        = 'N/A';
        END

        /* Rollover antes de numerar, siempre.
           La gestión activa es la que define el correlativo, y el 1 de enero
           la activa sigue siendo la del año pasado hasta que alguien la mueva.
           El módulo JSF lo resolvía llamando al rollover cada vez que se abría
           el formulario; acá se llama desde el alta misma, para que no dependa
           de que el cliente se haya acordado de pedirlo antes. Es una llamada
           recursiva a la ACCION 'G' de este mismo SP: una sola implementación
           del rollover en vez de dos que se pueden desincronizar. */
        DECLARE @gErr INT, @gMsg NVARCHAR(500), @gId BIGINT;
        EXEC dbo.p_abm_tcr_Documento
             @ACCION = 'G'
            ,@audUsuario = @audUsuario
            ,@error = @gErr OUTPUT
            ,@errormsg = @gMsg OUTPUT
            ,@idGenerado = @gId OUTPUT;

        IF ISNULL(@gErr, 0) <> 0
        BEGIN SET @error = @gErr; SET @errormsg = @gMsg; RETURN; END

        BEGIN TRY
            BEGIN TRAN;

                /* Gestión activa, bajo el mismo lock que la numeración. */
                SELECT TOP 1 @idGestion = idGestion
                  FROM tcr_gestion WITH (UPDLOCK, HOLDLOCK)
                 WHERE activo = 'SI'
                 ORDER BY gestion DESC;

                IF @idGestion IS NULL
                BEGIN
                    ROLLBACK TRAN;
                    SET @error = 17;
                    SET @errormsg = 'No hay una gestión activa. Ejecute la acción G antes de registrar.';
                    RETURN;
                END

                /* Correlativo por (tipo, empresa, gestión).
                   UPDLOCK + HOLDLOCK sobre el rango: otra sesión que quiera el
                   mismo correlativo espera acá en vez de llevarse un duplicado. */
                SELECT @nroCite = ISNULL(MAX(nroCite), 0) + 1
                  FROM tcr_documento WITH (UPDLOCK, HOLDLOCK)
                 WHERE idTipoDoc  = @idTipoDoc
                   AND codEmpresa = @codEmpresa
                   AND idGestion  = @idGestion;

                INSERT INTO tcr_documento (
                     idTipoDoc, idGestion, codEmpresa, codUsuario, codEmpleado
                    ,empleadoDe, cargoDe, ciudad, area, nroCite, fechaDoc
                    ,dirigido, cargoDirigido, referencia, via, cargoVia
                    ,asunto, cuerpo, estado, audUsuario, audFecha)
                VALUES (
                     @idTipoDoc, @idGestion, @codEmpresa, @codUsuario, @codEmpleado
                    ,@empleadoDe, @cargoDe, @ciudad, @area, @nroCite, @fechaDoc
                    ,@dirigido, @cargoDirigido, @referencia, @via, @cargoVia
                    ,@asunto, @cuerpo, 'V', @audUsuario, GETDATE());

                SET @idGenerado = SCOPE_IDENTITY();

                INSERT INTO tcr_registroDoc (idDocumento, nroCite, exportado, audUsuario, audFecha)
                VALUES (@idGenerado, @nroCite, 'NO', @audUsuario, GETDATE());

            COMMIT TRAN;

            SET @errormsg = 'Documento registrado con el CITE Nº ' +
                            RIGHT('000' + CAST(@nroCite AS VARCHAR(10)), 3) + '.';
        END TRY
        BEGIN CATCH
            IF @@TRANCOUNT > 0 ROLLBACK TRAN;
            SET @idGenerado = 0;

            /* 2601/2627 = violación del índice único del correlativo. Sólo puede
               pasar si otra sesión —típicamente el módulo JSF, que numera sin
               tomar lock— insertó el mismo número mientras esta transacción
               estaba abierta. Reintentar alcanza: la fila de la otra sesión ya
               está y el MAX de acá va a dar el siguiente. Se distingue del
               resto de los errores para poder decirlo en esos términos y no
               con el texto crudo del motor. */
            IF ERROR_NUMBER() IN (2601, 2627)
            BEGIN
                SET @error = 19;
                SET @errormsg = 'Otro usuario tomó ese número de CITE en este mismo instante. '
                              + 'Volvé a guardar: el documento va a quedar con el número siguiente.';
            END
            ELSE
            BEGIN
                SET @error = 18;
                SET @errormsg = 'Error registrando el documento: ' + ERROR_MESSAGE();
            END
        END CATCH

        RETURN;
    END

    /* ---------------------------------------------------------------------
       U — Modificación
       El nroCite y la gestión NO se tocan: ya fueron emitidos y el documento
       pudo haber salido en papel. Sólo cambia el contenido.
       --------------------------------------------------------------------- */
    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 20; SET @errormsg = 'Falta el identificador del documento a modificar.'; RETURN; END

        SELECT @estadoActual = estado, @idTipoDoc = idTipoDoc
          FROM tcr_documento WHERE idDocumento = @idDocumento;

        IF @estadoActual IS NULL
        BEGIN SET @error = 21; SET @errormsg = 'El documento no existe.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tcr_documentoAnulado WHERE idDocumento = @idDocumento)
        BEGIN SET @error = 22; SET @errormsg = 'El documento fue anulado y no puede modificarse.'; RETURN; END

        IF LEN(LTRIM(RTRIM(ISNULL(@cuerpo, '')))) = 0
        BEGIN SET @error = 23; SET @errormsg = 'El cuerpo del documento no puede estar vacío.'; RETURN; END

        IF @idTipoDoc <> 6 AND (LEN(LTRIM(RTRIM(ISNULL(@area, '')))) = 0 OR LTRIM(RTRIM(@area)) = 'A')
        BEGIN SET @error = 24; SET @errormsg = 'Debe seleccionar el área que emite el documento.'; RETURN; END

        /* Los mismos defaults por tipo que en el alta.
           No es redundante: el formulario no muestra los campos que el tipo no
           usa, así que al editar vuelven vacíos. Sin esto, un memorando que se
           corrige pasa de tener referencia 'N/A' a tenerla en blanco, y
           RptMemo —que imprime la etiqueta y el valor concatenados— saca un
           "REF.:" suelto que antes no estaba. Los reportes esperan 'N/A'. */
        IF @idTipoDoc = 1
        BEGIN
            SET @asunto = 'N/A';
        END
        IF @idTipoDoc = 2 OR @idTipoDoc = 7
        BEGIN
            SET @referencia = 'N/A';
        END
        IF @idTipoDoc = 6
        BEGIN
            SET @dirigido      = 'N/A';
            SET @referencia    = 'N/A';
            SET @asunto        = 'N/A';
            SET @area          = 'G.A.';
            SET @cargoDirigido = 'N/A';
        END
        IF @idTipoDoc = 8
        BEGIN
            SET @referencia = 'N/A';
        END
        IF @idTipoDoc = 9
        BEGIN
            SET @empleadoDe    = @dirigido;
            SET @cargoDe       = @cargoDirigido;
            SET @dirigido      = 'N/A';
            SET @cargoDirigido = 'N/A';
            SET @asunto        = 'N/A';
        END

        BEGIN TRY
            UPDATE tcr_documento
               SET codEmpleado   = @codEmpleado
                  ,empleadoDe    = @empleadoDe
                  ,cargoDe       = @cargoDe
                  ,ciudad        = @ciudad
                  ,area          = @area
                  ,fechaDoc      = ISNULL(@fechaDoc, fechaDoc)
                  ,dirigido      = @dirigido
                  ,cargoDirigido = @cargoDirigido
                  ,referencia    = @referencia
                  ,via           = @via
                  ,cargoVia      = @cargoVia
                  ,asunto        = @asunto
                  ,cuerpo        = @cuerpo
                  ,audUsuario    = @audUsuario
                  ,audFecha      = GETDATE()
             WHERE idDocumento = @idDocumento;

            SET @idGenerado = @idDocumento;
            SET @errormsg = 'Documento actualizado.';
        END TRY
        BEGIN CATCH
            SET @error = 25; SET @errormsg = 'Error actualizando el documento: ' + ERROR_MESSAGE();
        END CATCH

        RETURN;
    END

    /* ---------------------------------------------------------------------
       D — Anulación lógica.

       El SP viejo hacía DELETE físico, que además de reciclar el correlativo
       dejaba huérfanas las filas de tcr_registroDoc / copias / remitentes
       (no hay FK con cascade).

       Acá la fila de `tcr_documento` NO SE TOCA: la anulación se registra en
       `tcr_documentoAnulado`. Así el documento desaparece de los listados de
       la app nueva, el número queda consumido, y el módulo viejo —que numera
       contando filas con estado='V'— sigue viendo exactamente lo mismo que
       antes y no reutiliza el número. Ver la sección 0.
       --------------------------------------------------------------------- */
    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 30; SET @errormsg = 'Falta el identificador del documento a anular.'; RETURN; END

        SELECT @estadoActual = estado FROM tcr_documento WHERE idDocumento = @idDocumento;

        IF @estadoActual IS NULL
        BEGIN SET @error = 31; SET @errormsg = 'El documento no existe.'; RETURN; END

        IF EXISTS (SELECT 1 FROM tcr_documentoAnulado WHERE idDocumento = @idDocumento)
        BEGIN SET @error = 32; SET @errormsg = 'El documento ya estaba anulado.'; RETURN; END

        BEGIN TRY
            INSERT INTO tcr_documentoAnulado (idDocumento, motivo, audUsuario, audFecha)
            VALUES (@idDocumento, @motivo, @audUsuario, GETDATE());

            SET @idGenerado = @idDocumento;
            SET @errormsg = 'Documento anulado. El número de CITE queda consumido y no se reutiliza.';
        END TRY
        BEGIN CATCH
            SET @error = 33; SET @errormsg = 'Error anulando el documento: ' + ERROR_MESSAGE();
        END CATCH

        RETURN;
    END

    /* ---------------------------------------------------------------------
       X — Marcar como exportado (se generó el PDF).
       --------------------------------------------------------------------- */
    IF @ACCION = 'X'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 40; SET @errormsg = 'Falta el identificador del documento.'; RETURN; END

        BEGIN TRY
            UPDATE tcr_registroDoc
               SET exportado = 'SI', audUsuario = @audUsuario, audFecha = GETDATE()
             WHERE idDocumento = @idDocumento;

            /* El JSF creaba una fila nueva de registroDoc en cada exportación
               (llamaba al ABM con idRegDoc=0), duplicando filas por documento.
               Acá sólo se actualiza; si por lo que sea no existe, se crea una. */
            IF @@ROWCOUNT = 0
            BEGIN
                INSERT INTO tcr_registroDoc (idDocumento, nroCite, exportado, audUsuario, audFecha)
                SELECT idDocumento, nroCite, 'SI', @audUsuario, GETDATE()
                  FROM tcr_documento WHERE idDocumento = @idDocumento;
            END

            SET @idGenerado = @idDocumento;
            SET @errormsg = 'Documento marcado como exportado.';
        END TRY
        BEGIN CATCH
            SET @error = 41; SET @errormsg = 'Error marcando la exportación: ' + ERROR_MESSAGE();
        END CATCH

        RETURN;
    END

    SET @error = 99;
    SET @errormsg = 'ACCION no reconocida: ' + ISNULL(@ACCION, '(null)');
END
GO


/* ============================================================================
   2) p_abm_tcr_CopiaArch — "cc/Arch" al pie del documento
   ============================================================================ */
IF OBJECT_ID('dbo.p_abm_tcr_CopiaArch', 'P') IS NOT NULL
    DROP PROCEDURE dbo.p_abm_tcr_CopiaArch;
GO
CREATE PROCEDURE [dbo].[p_abm_tcr_CopiaArch]
     @ACCION      VARCHAR(1)
    ,@idCopiaArch BIGINT        = NULL
    ,@idDocumento BIGINT        = NULL
    ,@nroCite     INT           = NULL
    ,@copiaArch   VARCHAR(25)   = NULL
    ,@audUsuario  BIGINT        = NULL
    ,@error       INT           = 0  OUTPUT
    ,@errormsg    NVARCHAR(500) = '' OUTPUT
    ,@idGenerado  BIGINT        = 0  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 50; SET @errormsg = 'Falta el documento al que pertenece la copia de archivo.'; RETURN; END

        IF LEN(LTRIM(RTRIM(ISNULL(@copiaArch, '')))) = 0
        BEGIN SET @error = 51; SET @errormsg = 'La copia de archivo no puede estar vacía.'; RETURN; END

        /* El nroCite se toma del documento, no del cliente: el viejo lo recibía
           por parámetro y en el duplicado de cartas grababa el CITE del
           documento original. */
        SELECT @nroCite = nroCite FROM tcr_documento WHERE idDocumento = @idDocumento;

        BEGIN TRY
            INSERT INTO tcr_copiaArch (idDocumento, nroCite, copiaArch, audUsuario, audFecha)
            VALUES (@idDocumento, @nroCite, LTRIM(RTRIM(@copiaArch)), @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
        END TRY
        BEGIN CATCH
            SET @error = 52; SET @errormsg = 'Error registrando la copia de archivo: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idCopiaArch, 0) = 0
        BEGIN SET @error = 53; SET @errormsg = 'Falta el identificador de la copia de archivo.'; RETURN; END

        BEGIN TRY
            UPDATE tcr_copiaArch
               SET copiaArch = LTRIM(RTRIM(@copiaArch)), audUsuario = @audUsuario, audFecha = GETDATE()
             WHERE idCopiaArch = @idCopiaArch;
            SET @idGenerado = @idCopiaArch;
        END TRY
        BEGIN CATCH
            SET @error = 54; SET @errormsg = 'Error actualizando la copia de archivo: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idCopiaArch, 0) = 0
        BEGIN SET @error = 55; SET @errormsg = 'Falta el identificador de la copia de archivo.'; RETURN; END

        BEGIN TRY
            DELETE FROM tcr_copiaArch WHERE idCopiaArch = @idCopiaArch;
            SET @idGenerado = @idCopiaArch;
        END TRY
        BEGIN CATCH
            SET @error = 56; SET @errormsg = 'Error eliminando la copia de archivo: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    SET @error = 99; SET @errormsg = 'ACCION no reconocida: ' + ISNULL(@ACCION, '(null)');
END
GO


/* ============================================================================
   3) p_abm_tcr_CopiaEncab — "Copia a:" del encabezado (más destinatarios)
   ============================================================================ */
IF OBJECT_ID('dbo.p_abm_tcr_CopiaEncab', 'P') IS NOT NULL
    DROP PROCEDURE dbo.p_abm_tcr_CopiaEncab;
GO
CREATE PROCEDURE [dbo].[p_abm_tcr_CopiaEncab]
     @ACCION       VARCHAR(1)
    ,@idCopiaEncab BIGINT        = NULL
    ,@idDocumento  BIGINT        = NULL
    ,@nroCite      INT           = NULL
    ,@copiaEnca    VARCHAR(180)  = NULL
    ,@cargoCopia   VARCHAR(180)  = NULL
    ,@audUsuario   BIGINT        = NULL
    ,@error        INT           = 0  OUTPUT
    ,@errormsg     NVARCHAR(500) = '' OUTPUT
    ,@idGenerado   BIGINT        = 0  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 60; SET @errormsg = 'Falta el documento al que pertenece el destinatario.'; RETURN; END

        IF LEN(LTRIM(RTRIM(ISNULL(@copiaEnca, '')))) = 0
        BEGIN SET @error = 61; SET @errormsg = 'El nombre del destinatario no puede estar vacío.'; RETURN; END

        SELECT @nroCite = nroCite FROM tcr_documento WHERE idDocumento = @idDocumento;

        BEGIN TRY
            INSERT INTO tcr_copiaEncabezado (idDocumento, nroCite, copiaEnca, cargoCopia, audUsuario, audFecha)
            VALUES (@idDocumento, @nroCite, LTRIM(RTRIM(@copiaEnca)), LTRIM(RTRIM(@cargoCopia)), @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
        END TRY
        BEGIN CATCH
            SET @error = 62; SET @errormsg = 'Error registrando el destinatario: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idCopiaEncab, 0) = 0
        BEGIN SET @error = 63; SET @errormsg = 'Falta el identificador del destinatario.'; RETURN; END

        BEGIN TRY
            UPDATE tcr_copiaEncabezado
               SET copiaEnca = LTRIM(RTRIM(@copiaEnca)), cargoCopia = LTRIM(RTRIM(@cargoCopia))
                  ,audUsuario = @audUsuario, audFecha = GETDATE()
             WHERE idCopiaEncab = @idCopiaEncab;
            SET @idGenerado = @idCopiaEncab;
        END TRY
        BEGIN CATCH
            SET @error = 64; SET @errormsg = 'Error actualizando el destinatario: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idCopiaEncab, 0) = 0
        BEGIN SET @error = 65; SET @errormsg = 'Falta el identificador del destinatario.'; RETURN; END

        BEGIN TRY
            DELETE FROM tcr_copiaEncabezado WHERE idCopiaEncab = @idCopiaEncab;
            SET @idGenerado = @idCopiaEncab;
        END TRY
        BEGIN CATCH
            SET @error = 66; SET @errormsg = 'Error eliminando el destinatario: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    SET @error = 99; SET @errormsg = 'ACCION no reconocida: ' + ISNULL(@ACCION, '(null)');
END
GO


/* ============================================================================
   4) p_abm_tcr_Remitente — quién firma (máximo 2, igual que el módulo viejo)
   ============================================================================ */
IF OBJECT_ID('dbo.p_abm_tcr_Remitente', 'P') IS NOT NULL
    DROP PROCEDURE dbo.p_abm_tcr_Remitente;
GO
CREATE PROCEDURE [dbo].[p_abm_tcr_Remitente]
     @ACCION         VARCHAR(1)
    ,@idRemitente    BIGINT        = NULL
    ,@idDocumento    BIGINT        = NULL
    ,@nroCite        INT           = NULL
    ,@remitente      VARCHAR(150)  = NULL
    ,@cargoRemitente VARCHAR(200)  = NULL
    ,@audUsuario     BIGINT        = NULL
    ,@error          INT           = 0  OUTPUT
    ,@errormsg       NVARCHAR(500) = '' OUTPUT
    ,@idGenerado     BIGINT        = 0  OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    SET @error = 0; SET @errormsg = ''; SET @idGenerado = 0;

    DECLARE @cantidad INT;

    IF @ACCION = 'I'
    BEGIN
        IF ISNULL(@idDocumento, 0) = 0
        BEGIN SET @error = 70; SET @errormsg = 'Falta el documento al que pertenece el remitente.'; RETURN; END

        IF LEN(LTRIM(RTRIM(ISNULL(@remitente, '')))) = 0
        BEGIN SET @error = 71; SET @errormsg = 'El nombre del remitente no puede estar vacío.'; RETURN; END

        /* El tope de 2 estaba sólo en el managed bean del JSF: cualquier otro
           cliente podía grabar 5 remitentes y el reporte los cortaba. Ahora la
           regla vive donde no se puede esquivar. */
        SELECT @cantidad = COUNT(*) FROM tcr_remitente WHERE idDocumento = @idDocumento;
        IF @cantidad >= 2
        BEGIN SET @error = 72; SET @errormsg = 'Sólo se permiten 2 remitentes como máximo.'; RETURN; END

        SELECT @nroCite = nroCite FROM tcr_documento WHERE idDocumento = @idDocumento;

        BEGIN TRY
            INSERT INTO tcr_remitente (idDocumento, nroCite, remitente, cargoRemitente, audUsuario, audFecha)
            VALUES (@idDocumento, @nroCite, LTRIM(RTRIM(@remitente)), LTRIM(RTRIM(@cargoRemitente)), @audUsuario, GETDATE());
            SET @idGenerado = SCOPE_IDENTITY();
        END TRY
        BEGIN CATCH
            SET @error = 73; SET @errormsg = 'Error registrando el remitente: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'U'
    BEGIN
        IF ISNULL(@idRemitente, 0) = 0
        BEGIN SET @error = 74; SET @errormsg = 'Falta el identificador del remitente.'; RETURN; END

        BEGIN TRY
            UPDATE tcr_remitente
               SET remitente = LTRIM(RTRIM(@remitente)), cargoRemitente = LTRIM(RTRIM(@cargoRemitente))
                  ,audUsuario = @audUsuario, audFecha = GETDATE()
             WHERE idRemitente = @idRemitente;
            SET @idGenerado = @idRemitente;
        END TRY
        BEGIN CATCH
            SET @error = 75; SET @errormsg = 'Error actualizando el remitente: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    IF @ACCION = 'D'
    BEGIN
        IF ISNULL(@idRemitente, 0) = 0
        BEGIN SET @error = 76; SET @errormsg = 'Falta el identificador del remitente.'; RETURN; END

        BEGIN TRY
            DELETE FROM tcr_remitente WHERE idRemitente = @idRemitente;
            SET @idGenerado = @idRemitente;
        END TRY
        BEGIN CATCH
            SET @error = 77; SET @errormsg = 'Error eliminando el remitente: ' + ERROR_MESSAGE();
        END CATCH
        RETURN;
    END

    SET @error = 99; SET @errormsg = 'ACCION no reconocida: ' + ISNULL(@ACCION, '(null)');
END
GO


/* ============================================================================
   5) p_list_tcr_Documento — todas las lecturas del módulo
   ============================================================================
   ACCIONes:
     L  Listado paginado de documentos (pantalla principal)
     R  Un documento por id (para editar / duplicar)
     A  Siguiente nroCite disponible (previsualización del formulario)
     T  Catálogo de tipos de documento
     E  Áreas de una empresa
     M  Empleados activos (destinatarios de memorando / com. interna)
     C  Un empleado por código (para traer su cargo)
     U  Nombre y cargo del usuario logueado (firma por defecto)
     G  Gestiones
     H  Copias de archivo de un documento
     I  Remitentes de un documento
     J  Destinatarios "Copia a:" de un documento

   Correcciones respecto de `p_list_registroDoc` / `p_list_documento`:

   a) `IF(@idTipoDoc=null OR @idTipoDoc=0)` nunca es cierto para NULL con
      ANSI_NULLS ON: comparar con `= null` da UNKNOWN. El SP viejo caía siempre
      en el ELSE y filtraba `idTipoDoc = NULL`, que no devuelve nada. Acá:
      ISNULL(@idTipoDoc,0) = 0 → todos los tipos.

   b) La ACCION 'G' vieja tenía `INNER JOIN tcr_tipoDocumento AS tDoc
      ON tDoc.idTipoDoc = tDoc.idTipoDoc` — la tabla contra sí misma, condición
      siempre verdadera: producto cartesiano con los 6 tipos, disimulado con
      DISTINCT. Acá el join es contra doc.idTipoDoc.

   c) La ACCION 'B' vieja unía documento y registroDoc por `nroCite`, que se
      repite entre tipos, empresas y gestiones. Acá siempre por idDocumento.

   d) Los dos brazos del listado viejo usaban ventanas distintas
      (GETDATE() vs GETDATE()+10) y `permisoEditar` devolvía 0 en ambas ramas
      del CASE (el 1 estaba comentado), así que cualquiera podía editar
      cualquier carta al filtrar por tipo. Acá hay una sola consulta y el
      permiso se calcula igual siempre.

   e) `AND(@codEmpleado IS NULL OR @codEmpleado=codUsuario)` comparaba empleado
      contra usuario. Eliminado.

   f) Los documentos anulados no aparecen en el listado. La marca vive en
      `tcr_documentoAnulado` (sección 0), no en la fila, así que se excluyen
      con NOT EXISTS. La ACCION 'R' sí los devuelve, con `esAnulado` en 1, para
      que se puedan abrir en modo consulta.
   ============================================================================ */
IF OBJECT_ID('dbo.p_list_tcr_Documento', 'P') IS NOT NULL
    DROP PROCEDURE dbo.p_list_tcr_Documento;
GO
CREATE PROCEDURE [dbo].[p_list_tcr_Documento]
     @ACCION       VARCHAR(1)
    ,@idDocumento  BIGINT       = NULL
    ,@idTipoDoc    BIGINT       = NULL
    ,@codEmpresa   BIGINT       = NULL
    ,@codUsuario   BIGINT       = NULL
    ,@codEmpleado  BIGINT       = NULL
    ,@fechaDesde   DATE         = NULL
    ,@fechaHasta   DATE         = NULL
    ,@buscar       VARCHAR(200) = NULL
    ,@pagina       INT          = 1
    ,@tamanoPagina INT          = 20
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @desde INT, @hasta INT;

    /* ---------------------------------------------------------------------
       L — Listado paginado
       --------------------------------------------------------------------- */
    IF @ACCION = 'L'
    BEGIN
        IF ISNULL(@pagina, 0)       < 1 SET @pagina = 1;
        IF ISNULL(@tamanoPagina, 0) < 1 SET @tamanoPagina = 20;
        IF @tamanoPagina > 200          SET @tamanoPagina = 200;

        SET @desde = ((@pagina - 1) * @tamanoPagina) + 1;
        SET @hasta = @pagina * @tamanoPagina;

        IF @fechaHasta IS NULL SET @fechaHasta = CAST(GETDATE() AS DATE);
        IF @fechaDesde IS NULL SET @fechaDesde = DATEADD(MONTH, -3, @fechaHasta);

        IF LEN(LTRIM(RTRIM(ISNULL(@buscar, '')))) = 0 SET @buscar = NULL;

        ;WITH datos AS (
            SELECT
                 doc.idDocumento
                ,rDoc.idRegDoc
                ,doc.idTipoDoc
                ,tDoc.tipo
                ,doc.nroCite
                ,RTRIM(doc.area) + '/' + RIGHT('000' + CAST(doc.nroCite AS VARCHAR(10)), 3)
                    + '/' + CAST(ges.gestion AS VARCHAR(5))            AS cite
                ,doc.area
                ,doc.fechaDoc
                ,doc.dirigido
                ,doc.cargoDirigido
                ,doc.referencia
                ,doc.asunto
                ,ISNULL(rDoc.exportado, 'NO')                          AS exportado
                ,doc.codEmpresa
                ,emp.nombre                                            AS empresa
                ,doc.codUsuario
                ,ges.gestion
                ,doc.idGestion
                ,doc.estado
                ,LTRIM(RTRIM(ISNULL(per.nombres, '') + ' ' + ISNULL(per.apPaterno, '')
                        + ' ' + ISNULL(per.apMaterno, '')))            AS redactadoPor
                /* 1 = es el autor, puede editar sin permiso especial */
                ,CASE WHEN doc.codUsuario = @codUsuario THEN 1 ELSE 0 END AS esAutor
                ,ROW_NUMBER() OVER (ORDER BY doc.fechaDoc DESC, doc.idDocumento DESC) AS rn
                ,COUNT(*) OVER ()                                      AS totalRegistros
            FROM tcr_documento AS doc
                 INNER JOIN tcr_tipoDocumento AS tDoc ON tDoc.idTipoDoc = doc.idTipoDoc
                 INNER JOIN tcr_gestion       AS ges  ON ges.idGestion  = doc.idGestion
                 INNER JOIN tb_empresa        AS emp  ON emp.codEmpresa = doc.codEmpresa
                 LEFT  JOIN tcr_registroDoc   AS rDoc ON rDoc.idDocumento = doc.idDocumento
                 LEFT  JOIN tb_usuario        AS usu  ON usu.codUsuario  = doc.codUsuario
                 LEFT  JOIN tb_empleado       AS empl ON empl.codEmpleado = usu.codEmpleado
                 LEFT  JOIN trh_persona       AS per  ON per.codPersona  = empl.codPersona
            WHERE NOT EXISTS (SELECT 1 FROM tcr_documentoAnulado an
                               WHERE an.idDocumento = doc.idDocumento)
              AND doc.fechaDoc BETWEEN @fechaDesde AND @fechaHasta
              AND (ISNULL(@codEmpresa, 0) = 0 OR doc.codEmpresa = @codEmpresa)
              AND (ISNULL(@idTipoDoc, 0)  = 0 OR doc.idTipoDoc  = @idTipoDoc)
              AND (@buscar IS NULL
                   OR doc.dirigido   LIKE '%' + @buscar + '%'
                   OR doc.referencia LIKE '%' + @buscar + '%'
                   OR doc.asunto     LIKE '%' + @buscar + '%'
                   OR CAST(doc.nroCite AS VARCHAR(10)) = @buscar)
        )
        SELECT idDocumento, idRegDoc, idTipoDoc, tipo, nroCite, cite, area, fechaDoc
              ,dirigido, cargoDirigido, referencia, asunto, exportado
              ,codEmpresa, empresa, codUsuario, redactadoPor, esAutor
              ,gestion, idGestion, estado, totalRegistros
          FROM datos
         WHERE rn BETWEEN @desde AND @hasta
         ORDER BY rn;

        RETURN;
    END

    /* ---------------------------------------------------------------------
       R — Un documento por id
       --------------------------------------------------------------------- */
    IF @ACCION = 'R'
    BEGIN
        SELECT
             doc.idDocumento
            ,doc.idTipoDoc
            ,tDoc.tipo
            ,doc.idGestion
            ,ges.gestion
            ,doc.codEmpresa
            ,emp.nombre        AS empresa
            ,doc.codUsuario
            ,doc.codEmpleado
            ,doc.empleadoDe
            ,doc.cargoDe
            ,doc.ciudad
            ,doc.area
            ,doc.nroCite
            ,RTRIM(doc.area) + '/' + RIGHT('000' + CAST(doc.nroCite AS VARCHAR(10)), 3)
                + '/' + CAST(ges.gestion AS VARCHAR(5))  AS cite
            ,doc.fechaDoc
            ,doc.dirigido
            ,doc.cargoDirigido
            ,doc.referencia
            ,doc.via
            ,doc.cargoVia
            ,doc.asunto
            ,doc.cuerpo
            ,doc.estado
            ,CASE WHEN an.idDocumento IS NULL THEN 0 ELSE 1 END AS esAnulado
            ,ISNULL(rDoc.exportado, 'NO')                AS exportado
            ,doc.audUsuario
        FROM tcr_documento AS doc
             INNER JOIN tcr_tipoDocumento AS tDoc ON tDoc.idTipoDoc = doc.idTipoDoc
             INNER JOIN tcr_gestion       AS ges  ON ges.idGestion  = doc.idGestion
             INNER JOIN tb_empresa        AS emp  ON emp.codEmpresa = doc.codEmpresa
             LEFT  JOIN tcr_registroDoc   AS rDoc ON rDoc.idDocumento = doc.idDocumento
             LEFT  JOIN tcr_documentoAnulado AS an ON an.idDocumento = doc.idDocumento
        WHERE doc.idDocumento = @idDocumento;

        RETURN;
    END

    /* ---------------------------------------------------------------------
       A — Siguiente nroCite (previsualización).
       Mismo criterio que usa el INSERT: MAX+1 sobre (tipo, empresa, gestión
       activa), contando también los anulados para no reciclar números.
       Es orientativo: el número definitivo lo asigna el ABM al guardar.
       --------------------------------------------------------------------- */
    IF @ACCION = 'A'
    BEGIN
        SELECT TOP 1
             ges.idGestion
            ,ges.gestion
            ,ISNULL((SELECT MAX(d.nroCite)
                       FROM tcr_documento d
                      WHERE d.idTipoDoc  = @idTipoDoc
                        AND d.codEmpresa = @codEmpresa
                        AND d.idGestion  = ges.idGestion), 0) + 1  AS nroCite
        FROM tcr_gestion AS ges
        WHERE ges.activo = 'SI'
        ORDER BY ges.gestion DESC;

        RETURN;
    END

    /* ---------------------------------------------------------------------
       T — Tipos de documento
       --------------------------------------------------------------------- */
    IF @ACCION = 'T'
    BEGIN
        SELECT idTipoDoc, tipo
          FROM tcr_tipoDocumento
         ORDER BY idTipoDoc;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       E — Áreas de una empresa.
       tcr_Area tiene siglas duplicadas por empresa (ESPPAPEL tiene dos
       'G.A.', idArea 16 y 30). El combo del JSF las mostraba repetidas.
       El GROUP BY las colapsa: lo que se graba es la sigla, no el idArea.
       --------------------------------------------------------------------- */
    IF @ACCION = 'E'
    BEGIN
        SELECT RTRIM(siglas)     AS siglas
              ,MIN(descripcion)  AS descripcion
          FROM tcr_Area
         WHERE codEmpresa = @codEmpresa
           AND ISNULL(estado, 1) = 1
         GROUP BY RTRIM(siglas)
         ORDER BY siglas;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       M — Empleados activos con su cargo.
       Consulta portada tal cual del `p_list_documento` ACCION 'C': es lógica
       de RRHH (relación empleado-empresa vigente, último cargo) que funciona
       y no conviene reescribir de memoria.
       --------------------------------------------------------------------- */
    IF @ACCION = 'M'
    BEGIN
        SELECT Emp.codEmpleado
              ,RTRIM(LTRIM(P.apPaterno + ' ' + P.apMaterno + ' ' + P.nombres)) AS nombreCompleto
              ,(SELECT (SELECT TOP(1) c.descripcion
                          FROM trh_empleadoCargo ec, tb_relEmplEmpr ree, trh_cargo c, tb_cargo_sucursal cs
                         WHERE ec.codEmpleado = E.codEmpleado
                           AND ree.codRelEmplEmpr = E.codRelBeneficios
                           AND ec.codEmpleado = ree.codEmpleado
                           AND ((ree.fechaFin IS NOT NULL AND ec.fechaInicio >= ree.fechaIni)
                             OR (ree.fechaFin IS NULL     AND ec.fechaInicio >= ree.fechaIni))
                           AND cs.codCargoSucursal = ec.codCargoSucursal
                           AND c.codCargo = cs.codCargo
                         ORDER BY ree.fechaIni DESC, ec.fechaInicio DESC)
                  FROM tb_empleado E
                       LEFT JOIN trh_persona P ON E.codPersona = P.codPersona
                 WHERE E.codEmpleado = Emp.codEmpleado) AS cargo
        FROM tb_empleado Emp
            ,trh_persona P
            ,(SELECT R.codRelEmplEmpr, R.codEmpleado, R.esActivo
                FROM (SELECT codEmpleado, MAX(fechaIni) ini
                        FROM tb_relEmplEmpr
                       GROUP BY codEmpleado) L
                     INNER JOIN tb_relEmplEmpr R
                        ON L.codEmpleado = R.codEmpleado AND L.ini = R.fechaIni) R
        WHERE P.codPersona = Emp.codPersona
          AND R.codEmpleado = Emp.codEmpleado
          AND R.esActivo = 1
        ORDER BY nombreCompleto;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       C — Un empleado por código
       --------------------------------------------------------------------- */
    IF @ACCION = 'C'
    BEGIN
        SELECT DISTINCT
               Emp.codEmpleado
              ,RTRIM(LTRIM(P.apPaterno + ' ' + P.apMaterno + ' ' + P.nombres)) AS nombreCompleto
              ,(SELECT (SELECT TOP(1) c.descripcion
                          FROM trh_empleadoCargo ec, tb_relEmplEmpr ree, trh_cargo c, tb_cargo_sucursal cs
                         WHERE ec.codEmpleado = E.codEmpleado
                           AND ree.codRelEmplEmpr = E.codRelBeneficios
                           AND ec.codEmpleado = ree.codEmpleado
                           AND ((ree.fechaFin IS NOT NULL AND ec.fechaInicio >= ree.fechaIni)
                             OR (ree.fechaFin IS NULL     AND ec.fechaInicio >= ree.fechaIni))
                           AND cs.codCargoSucursal = ec.codCargoSucursal
                           AND c.codCargo = cs.codCargo
                         ORDER BY ree.fechaIni DESC, ec.fechaInicio DESC)
                  FROM tb_empleado E
                       LEFT JOIN trh_persona P ON E.codPersona = P.codPersona
                 WHERE E.codEmpleado = Emp.codEmpleado) AS cargo
        FROM tb_empleado Emp
            ,trh_persona P
            ,(SELECT R.codRelEmplEmpr, R.codEmpleado, R.esActivo
                FROM (SELECT codEmpleado, MAX(fechaIni) ini
                        FROM tb_relEmplEmpr
                       GROUP BY codEmpleado) L
                     INNER JOIN tb_relEmplEmpr R
                        ON L.codEmpleado = R.codEmpleado AND L.ini = R.fechaIni) R
        WHERE P.codPersona = Emp.codPersona
          AND R.codEmpleado = Emp.codEmpleado
          AND R.esActivo = 1
          AND Emp.codEmpleado = @codEmpleado;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       U — Nombre y cargo del usuario logueado (firma por defecto).
       Portada de `p_list_documento` ACCION 'B'.
       --------------------------------------------------------------------- */
    IF @ACCION = 'U'
    BEGIN
        SELECT TOP 1
               (p.nombres + ' ' + p.apPaterno + ' ' + p.apMaterno) AS nombreCompleto
              ,c.descripcion                                       AS cargo
        FROM trh_empleadoCargo ec
             JOIN tb_cargo_sucursal cs ON ec.codCargoSucursal = cs.codCargoSucursal
             JOIN tb_sucursal        s ON s.codSucursal       = cs.codSucursal
             JOIN trh_cargo          c ON c.codCargo          = cs.codCargo
             JOIN tb_empleado     empl ON empl.codEmpleado    = ec.codEmpleado
             JOIN trh_persona        p ON p.codPersona        = empl.codPersona
             JOIN tb_relEmplEmpr   ree ON ree.codEmpleado     = empl.codEmpleado
             LEFT JOIN tb_usuario    u ON u.codEmpleado       = empl.codEmpleado
        WHERE ec.fechaInicio = (SELECT MAX(fechaInicio) FROM trh_empleadoCargo
                                 WHERE codEmpleado = ec.codEmpleado)
          AND ree.esActivo = 1
          AND u.codUsuario = @codUsuario
        ORDER BY ec.fechaInicio DESC;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       G — Gestiones
       --------------------------------------------------------------------- */
    IF @ACCION = 'G'
    BEGIN
        SELECT idGestion, gestion, activo
          FROM tcr_gestion
         ORDER BY gestion DESC;
        RETURN;
    END

    /* ---------------------------------------------------------------------
       H / I / J — Hijos de un documento
       --------------------------------------------------------------------- */
    IF @ACCION = 'H'
    BEGIN
        SELECT idCopiaArch, idDocumento, nroCite, copiaArch
          FROM tcr_copiaArch
         WHERE idDocumento = @idDocumento
         ORDER BY idCopiaArch;
        RETURN;
    END

    IF @ACCION = 'I'
    BEGIN
        SELECT idRemitente, idDocumento, nroCite, remitente, cargoRemitente
          FROM tcr_remitente
         WHERE idDocumento = @idDocumento
         ORDER BY idRemitente;
        RETURN;
    END

    IF @ACCION = 'J'
    BEGIN
        SELECT idCopiaEncab, idDocumento, nroCite, copiaEnca, cargoCopia
          FROM tcr_copiaEncabezado
         WHERE idDocumento = @idDocumento
         ORDER BY idCopiaEncab;
        RETURN;
    END
END
GO


/* ============================================================================
   6) Permisos para la cuenta con la que entra el backend
   ============================================================================

   Se otorgan a la cuenta que exista, no a una escrita a mano. Al 2026-08-17 la
   base tiene el usuario `bosque`; `bosque_app` —el login dedicado que propone
   LEEME-SECRETS.md— todavía no se creó. Un GRANT a un usuario inexistente
   corta el script con "Cannot find the user", así que en vez de elegir uno se
   recorren los candidatos y se le da permiso al que esté.

   Si mañana se crea `bosque_app`, alcanza con volver a correr este bloque.
   Si el GRANT a nivel de esquema ya cubre todo (GRANT EXECUTE ON SCHEMA::dbo),
   esto es redundante y no hace daño.
   ============================================================================ */
DECLARE @permisos NVARCHAR(MAX) = N'';

SELECT @permisos = @permisos
     + 'GRANT EXECUTE ON dbo.p_abm_tcr_Documento  TO ' + QUOTENAME(name) + '; '
     + 'GRANT EXECUTE ON dbo.p_abm_tcr_CopiaArch  TO ' + QUOTENAME(name) + '; '
     + 'GRANT EXECUTE ON dbo.p_abm_tcr_CopiaEncab TO ' + QUOTENAME(name) + '; '
     + 'GRANT EXECUTE ON dbo.p_abm_tcr_Remitente  TO ' + QUOTENAME(name) + '; '
     + 'GRANT EXECUTE ON dbo.p_list_tcr_Documento TO ' + QUOTENAME(name) + '; '
     + 'GRANT SELECT, INSERT ON dbo.tcr_documentoAnulado TO ' + QUOTENAME(name) + '; '
  FROM sys.database_principals
 WHERE name IN ('bosque_app', 'bosque')
   AND type IN ('S', 'U', 'G');

IF LEN(@permisos) > 0
    EXEC sp_executesql @permisos;
ELSE
    PRINT 'AVISO: no se encontró ni bosque_app ni bosque en esta base. '
        + 'Otorgá EXECUTE sobre los SPs nuevos a la cuenta que use el backend.';
GO


/* ============================================================================
   7) Índices de apoyo
   ============================================================================
   El listado filtra por fecha + empresa + tipo. Sin índice es scan de tabla
   completa; son ~780 filas hoy, así que el impacto es chico, pero el índice de
   la sección 8 —el único, sobre (tipo, empresa, gestión, nroCite)— además
   reduce el rango que bloquea el HOLDLOCK al numerar: sin él el lock es sobre
   toda la tabla y dos usuarios de empresas distintas se esperan entre sí al
   guardar.

   Por eso acá no se crea un índice de numeración aparte: el de la sección 8
   tiene exactamente esas columnas y sirve para las dos cosas.
   ============================================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_tcr_documento_listado'
               AND object_id = OBJECT_ID('dbo.tcr_documento'))
BEGIN
    CREATE INDEX IX_tcr_documento_listado
        ON dbo.tcr_documento (fechaDoc, codEmpresa, idTipoDoc)
        INCLUDE (estado, nroCite, area, dirigido, asunto, referencia);
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'IX_tcr_registroDoc_idDocumento'
               AND object_id = OBJECT_ID('dbo.tcr_registroDoc'))
BEGIN
    CREATE INDEX IX_tcr_registroDoc_idDocumento
        ON dbo.tcr_registroDoc (idDocumento) INCLUDE (exportado, nroCite);
END
GO


/* ============================================================================
   8) El candado: dos documentos no pueden compartir CITE
   ============================================================================

   Todo lo anterior hace que la app nueva numere bien. Esto hace que **nadie**
   pueda numerar mal, venga de donde venga el INSERT.

   Queda un hueco que los SPs nuevos no pueden tapar solos porque está del otro
   lado: `p_abm_Documento` numera con COUNT(*)+1 sin ningún lock. Si dos
   sesiones —una del JSF y otra de la app nueva— guardan en el mismo instante,
   las dos pueden calcular el mismo número antes de que la otra haya insertado.
   El SP nuevo se protege con UPDLOCK/HOLDLOCK, pero no puede obligar al viejo
   a tomar ese lock.

   El índice único convierte esa carrera en un error en vez de en un documento
   duplicado, y del lado del JSF eso ya está contemplado: `DocumentoDao`
   atrapa la SQLException, devuelve false y la pantalla muestra "Ah ocurrido un
   problema al ingresar el registro". El usuario reintenta y esa vez sí anda,
   porque para entonces la fila de la otra sesión ya está y COUNT(*)+1 da el
   número siguiente. Falla ruidosa y recuperable, en lugar de dos cartas con el
   mismo número en el archivo, que no se arregla nunca.

   Se aplica ahora y no queda como opcional. Lo que lo habilita es que se
   verificó el estado real de los datos:

     · 781 documentos, ninguna combinación (tipo, empresa, gestión, nroCite)
       repetida — el índice se crea sin pelear con datos existentes;
     · las 23 combinaciones están numeradas 1..N sin huecos, así que COUNT+1 y
       MAX+1 dan hoy el mismo número: el índice no va a estar saltando por una
       divergencia estructural entre los dos sistemas, sólo por carreras reales;
     · `DocumentoManagedBean` sólo llama al ABM con 'I', 'U' y 'B'. El JSF
       nunca borra, así que la numeración no puede volverse hacia atrás.

   Si el CREATE falla por duplicados, hay datos que revisar antes de insistir:

     SELECT g.gestion, d.codEmpresa, d.idTipoDoc, d.nroCite, COUNT(*)
     FROM tcr_documento d JOIN tcr_gestion g ON g.idGestion = d.idGestion
     GROUP BY g.gestion, d.codEmpresa, d.idTipoDoc, d.nroCite
     HAVING COUNT(*) > 1;
   ============================================================================ */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'UX_tcr_documento_correlativo'
               AND object_id = OBJECT_ID('dbo.tcr_documento'))
BEGIN
    CREATE UNIQUE INDEX UX_tcr_documento_correlativo
        ON dbo.tcr_documento (idTipoDoc, codEmpresa, idGestion, nroCite);
END
GO


/* ============================================================================
   9) Verificación final
   ============================================================================
   Si esto devuelve algo distinto de 6 objetos y 'OK' en todo, algo no se creó.
   ============================================================================ */
SELECT 'tcr_documentoAnulado' AS objeto, CASE WHEN OBJECT_ID('dbo.tcr_documentoAnulado','U')  IS NULL THEN 'FALTA' ELSE 'OK' END AS estado
UNION ALL SELECT 'p_abm_tcr_Documento',  CASE WHEN OBJECT_ID('dbo.p_abm_tcr_Documento','P')   IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'p_abm_tcr_CopiaArch',  CASE WHEN OBJECT_ID('dbo.p_abm_tcr_CopiaArch','P')   IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'p_abm_tcr_CopiaEncab', CASE WHEN OBJECT_ID('dbo.p_abm_tcr_CopiaEncab','P')  IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'p_abm_tcr_Remitente',  CASE WHEN OBJECT_ID('dbo.p_abm_tcr_Remitente','P')   IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'p_list_tcr_Documento', CASE WHEN OBJECT_ID('dbo.p_list_tcr_Documento','P')  IS NULL THEN 'FALTA' ELSE 'OK' END
UNION ALL SELECT 'UX_tcr_documento_correlativo',
       CASE WHEN EXISTS (SELECT 1 FROM sys.indexes
                          WHERE name='UX_tcr_documento_correlativo'
                            AND object_id=OBJECT_ID('dbo.tcr_documento'))
            THEN 'OK' ELSE 'FALTA' END;
GO

