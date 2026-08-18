/* ============================================================================
   TPEX · LIMPIEZA DEFINITIVA de datos de prueba + reset de Primary Keys
   Fecha: 2026-06-23 (Z) — EJECUTAR CON USUARIO DE ESCRITURA

   ⚠⚠ DESTRUCTIVO E IRREVERSIBLE ⚠⚠
   Borra FÍSICAMENTE (no es soft-delete) TODOS los datos transaccionales del
   módulo TPEX y reinicia los identity (las PK arrancan de nuevo en 1).
   Pensado para correrlo UNA vez antes de pasar a producción y arrancar limpio.

   >>> HACÉ UN BACKUP ANTES <<<
       BACKUP DATABASE [BOSQUE-2_0] TO DISK = 'C:\temp\BOSQUE-2_0_pre_limpieza_tpex.bak';

   QUÉ BORRA (9 tablas transaccionales):
     tpex_Asientos, tpex_Cargos, tpex_TransaccionParticipantes, tpex_LogEstados,
     tpex_Transacciones, tpex_Cotizaciones, tpex_DetalleSolicitud,
     tpex_SolicitudProveedor, tpex_SolicitudPago

   QUÉ NO TOCA (catálogos / configuración — los necesita producción):
     tpex_Monedas, tpex_TiposTransaccion, tpex_TiposCambio (TC referencia),
     tpex_CanalesPago, tpex_TiposCargo, tpex_ConfigComisionesBanco, tpex_Proveedores
     (tampoco toca tch_banco, tb_empresa, etc.)

   El borrado va en orden hijo → padre para respetar las FKs. Se verificó que
   ninguna tabla fuera de tpex_ referencia a estas 9.
   ============================================================================ */

SET NOCOUNT ON;
SET XACT_ABORT ON;   -- ante cualquier error, ROLLBACK automático de todo

-- (Opcional) Conteo ANTES
SELECT 'ANTES' AS momento,
       (SELECT COUNT(*) FROM tpex_SolicitudPago)            AS solicitudes,
       (SELECT COUNT(*) FROM tpex_SolicitudProveedor)       AS proveedores,
       (SELECT COUNT(*) FROM tpex_DetalleSolicitud)         AS cuotas,
       (SELECT COUNT(*) FROM tpex_Cotizaciones)             AS cotizaciones,
       (SELECT COUNT(*) FROM tpex_Transacciones)            AS transacciones,
       (SELECT COUNT(*) FROM tpex_Cargos)                   AS cargos,
       (SELECT COUNT(*) FROM tpex_Asientos)                 AS asientos,
       (SELECT COUNT(*) FROM tpex_TransaccionParticipantes) AS participantes,
       (SELECT COUNT(*) FROM tpex_LogEstados)               AS logEstados;

BEGIN TRANSACTION;

    -- ── 1) Borrado físico en orden hijo → padre ─────────────────────────────
    DELETE FROM tpex_Asientos;                  -- hijo de Transacciones
    DELETE FROM tpex_Cargos;                    -- hijo de Transacciones / Cotizaciones
    DELETE FROM tpex_TransaccionParticipantes;  -- hijo de Transacciones
    DELETE FROM tpex_LogEstados;                -- hijo de Transacciones / Cotizaciones / SolicitudPago
    DELETE FROM tpex_Transacciones;             -- hijo de Cotizaciones / SolicitudPago (y self-ref devolución)
    DELETE FROM tpex_Cotizaciones;              -- hijo de SolicitudPago
    DELETE FROM tpex_DetalleSolicitud;          -- hijo de SolicitudProveedor
    DELETE FROM tpex_SolicitudProveedor;        -- hijo de SolicitudPago
    DELETE FROM tpex_SolicitudPago;             -- raíz

COMMIT TRANSACTION;

-- ── 2) Reset de identity (las PK arrancan de nuevo en 1) ────────────────────
--    RESEED a 0 ⇒ el próximo INSERT toma 1 (en tablas que ya tuvieron filas).
DBCC CHECKIDENT ('tpex_Asientos',                 RESEED, 0);
DBCC CHECKIDENT ('tpex_Cargos',                   RESEED, 0);
DBCC CHECKIDENT ('tpex_TransaccionParticipantes', RESEED, 0);
DBCC CHECKIDENT ('tpex_LogEstados',               RESEED, 0);
DBCC CHECKIDENT ('tpex_Transacciones',            RESEED, 0);
DBCC CHECKIDENT ('tpex_Cotizaciones',             RESEED, 0);
DBCC CHECKIDENT ('tpex_DetalleSolicitud',         RESEED, 0);
DBCC CHECKIDENT ('tpex_SolicitudProveedor',       RESEED, 0);
DBCC CHECKIDENT ('tpex_SolicitudPago',            RESEED, 0);

-- ── 3) Verificación: todo en 0 y próximo id = 1 ─────────────────────────────
SELECT 'DESPUES' AS momento,
       (SELECT COUNT(*) FROM tpex_SolicitudPago)            AS solicitudes,
       (SELECT COUNT(*) FROM tpex_SolicitudProveedor)       AS proveedores,
       (SELECT COUNT(*) FROM tpex_DetalleSolicitud)         AS cuotas,
       (SELECT COUNT(*) FROM tpex_Cotizaciones)             AS cotizaciones,
       (SELECT COUNT(*) FROM tpex_Transacciones)            AS transacciones,
       (SELECT COUNT(*) FROM tpex_Cargos)                   AS cargos,
       (SELECT COUNT(*) FROM tpex_Asientos)                 AS asientos,
       (SELECT COUNT(*) FROM tpex_TransaccionParticipantes) AS participantes,
       (SELECT COUNT(*) FROM tpex_LogEstados)               AS logEstados;
