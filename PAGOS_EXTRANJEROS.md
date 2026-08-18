# Módulo: Pagos Extranjeros (`tpex_*`)

> **Sistema de gestión de pagos internacionales** para una empresa boliviana.
> Cubre el ciclo completo: Solicitud → Cotización bancaria → Transacción → Confirmación.

---

## 📁 Archivos del módulo

| Capa | Archivo |
|------|---------|
| **Controller** | `controller/PagosExtranjerosController.java` |
| **Interfaces** | `dao/ICanalesPago`, `ICargoPago`, `IConfigComisionesBanco`, `ICotizaciones`, `ILogEstados`, `IMonedas`, `ISolicitudPago`, `ISolicitudProveedor`, `IDetalleSolicitud`, `ITiposCambio`, `ITiposCargo`, `ITiposTransaccion`, `ITransacciones` |
| **DAOs** | `dao/CanalesPagoDao`, `CargoPagoDao`, `ConfigComisionesBancoDao`, `CotizacionesDao`, `LogEstadosDao`, `MonedasDao`, `SolicitudPagoDao`, `SolicitudProveedorDao`, `DetalleSolicitudDao`, `TiposCambioDao`, `TiposCargoDao`, `TiposTransaccionDao`, `TransaccionesDao` |
| **Modelos** | `model/SolicitudPago`, `SolicitudProveedor`, `DetalleSolicitud`, `Cotizaciones`, `Transacciones`, `CargoPago`, `LogEstados`, `CanalesPago`, `Monedas`, `TiposCambio`, `TiposCargo`, `TiposTransaccion`, `ConfigComisionesBanco` |
| **DTOs** | `dto/SolicitudPagoDto`, `SolicitudProveedorDto`, `DetalleSolicitudDto`, `ReportePlanoDto`, `FiltroFechasDto`, `ConfirmarPagoRequest` |
| **Utilidades** | `utils/SpHelper`, `utils/RespuestaSp`, `utils/ApiResponse` |

---

## 🏛️ Arquitectura general

```
Angular Frontend
      │  POST JSON
      ▼
PagosExtranjerosController  (/pagos-extranjeros/*)
      │  Inyecta 13 DAOs vía interfaz (constructor injection)
      ▼
[Interfaz I*]  →  [*Dao implements I*]
      │  SpHelper.ejecutarAbm() / ejecutarListado()
      ▼
SQL Server  →  Stored Procedures  p_abm_tpex_* / p_list_tpex_*
```

- **Sin JPA/ORM.** Todo acceso a BD usa `JdbcTemplate` + `SimpleJdbcCall`.
- **SpHelper** serializa el modelo con Jackson, agrega el parámetro `ACCION` y ejecuta el SP.
- Los `SimpleJdbcCall` se cachean en un `ConcurrentHashMap` para evitar lecturas repetidas de metadatos.

---

## 🗄️ Modelos y relaciones

### Jerarquía principal (entidades transaccionales)

```
SolicitudPago                        → tabla tpex_SolicitudPago
  ├─ idSolicitud (PK)
  ├─ codEmpresa, fechaSolicitud, montoTotalSolicitud
  ├─ estado: PENDIENTE → APROBADA → PAGADA
  └─ SolicitudProveedor (1..N)        → tabla tpex_SolicitudProveedor
       ├─ idSolicitudProveedor (PK)
       ├─ cardCode, cardName (SAP)
       ├─ totalFacturasUsd, totalAmortizadoUsd, totalAPagarUsd
       └─ DetalleSolicitud (1..N)     → tabla tpex_DetalleSolicitud
            ├─ idDetalle (PK)
            ├─ tipoDocumento, numeroDocumento, facturaProvSap
            ├─ montoFacturaUsd, montoAmortizadoUsd, montoAPagarUsd
            └─ fechaFactura, fechaVencimiento, esAprobado

Cotizaciones                          → tabla tpex_Cotizaciones
  ├─ idCotizacion (PK)
  ├─ idSolicitud (FK)
  ├─ codBanco, tipoCambioOfrecido, montoCompra, nroGiros
  ├─ montoConvertido, totalBolivianos
  ├─ esGanadora, estado: VIGENTE → ACEPTADA / RECHAZADA
  └─ CargoPago (1..N)                → tabla tpex_CargoPago
       ├─ idCargo (PK)
       ├─ idCotizacion / idTransaccion  (mutuamente excluyentes por FK)
       ├─ idTipoCargo, baseCalculo, origenBase
       └─ porcentaje / valorFijo, montoCargo, idMoneda, orden

Transacciones                         → tabla tpex_Transacciones
  ├─ idTransaccion (PK)
  ├─ idSolicitud (FK), idCotizacion (FK)
  ├─ numeroTransaccion, idTipoTransaccion, codBanco, idCanal
  ├─ montoOrigen, idMonedaOrigen, tipoCambioAplicado
  ├─ montoConvertido, idMonedaDestino, totalCargos, totalFinal
  ├─ tipoCambioForward (si aplica), tipoCambioReferencia, equivalenteUsdRef
  ├─ diferenciaDeMas, porcentajeDiferencia
  ├─ nombreExportadora, tcNegociadoExportadora, comisionExportadora, metodoExportadora
  ├─ estado: PENDIENTE → PROCESADO → CONFIRMADO
  └─ CargoPago (1..N)

LogEstados                            → tabla tpex_LogEstados
  ├─ idLog (PK)
  ├─ idSolicitud / idCotizacion / idTransaccion  (uno activo por registro)
  ├─ estadoAnterior, estadoNuevo
  └─ observaciones, audUsuario
```

### Catálogos (tablas de configuración)

| Modelo | Campos clave | Descripción |
|--------|-------------|-------------|
| `CanalesPago` | `idCanal`, `nombre`, `tipo`, `contacto`, `activo` | Canales de envío (SWIFT, transferencia local, etc.) |
| `Monedas` | `idMoneda`, `codigo`, `nombre`, `simbolo`, `decimales`, `activo` | USD, BOB, EUR, etc. |
| `TiposCambio` | `idTipoCambio`, `codBanco`, `fechaVigencia`, `tasaCompra`, `tasaVenta`, `tasaPromedio`, `fuente` | Tasas por banco y fecha de vigencia |
| `TiposCargo` | `idTipoCargo`, `nombre`, `esPorcentaje`, `activo` | Tipos de comisión: porcentaje o valor fijo |
| `TiposTransaccion` | `idTipoTransaccion`, `codigo`, `nombre`, `requiereForward`, `requiereBanco` | Clasificación de la operación bancaria |
| `ConfigComisionesBanco` | `idConfig`, `codBanco`, `idTipoTransaccion`, `idTipoCargo`, `valorPorcentaje`, `valorFijo`, `fechaVigenciaDesde/Hasta` | Template de cargos preconfigurados por banco |

---

## 🔄 Flujo de negocio (5 Fases)

> Todos los endpoints de escritura están anotados con `@Transactional`.
> Si cualquier SP falla → `RuntimeException` → rollback automático de toda la fase.

### FASE 1 — Solicitud de Pago

```
POST /guardar-solicitud-completa
Body: SolicitudPago {
  proveedoresAEliminar: [ids],
  proveedores: [
    SolicitudProveedor {
      detallesAEliminar: [ids],
      detalles: [ DetalleSolicitud ]
    }
  ]
}

Pasos:
  1. DELETE detalles marcados en detallesAEliminar    → p_abm_tpex_DetalleSolicitud   "D"
  2. DELETE proveedores marcados en proveedoresAEliminar → p_abm_tpex_SolicitudProveedor "D"
  3. INSERT/UPDATE SolicitudPago (cabecera)           → p_abm_tpex_SolicitudPago      "I"/"U"
     └─ Obtiene idSolicitud desde idGenerado del SP
  4. Si INSERT → LogEstados NULL → "PENDIENTE"        → p_abm_tpex_LogEstados         "I"
  5. Para cada proveedor:
     INSERT/UPDATE SolicitudProveedor                 → p_abm_tpex_SolicitudProveedor "I"/"U"
     └─ Para cada detalle:
        INSERT/UPDATE DetalleSolicitud                → p_abm_tpex_DetalleSolicitud   "I"/"U"

POST /aprobar-solicitud
Body: SolicitudPago { idSolicitud, estado: "APROBADA", audUsuario }
  1. UPDATE estado de la solicitud                    → p_abm_tpex_SolicitudPago      "U"
  2. LogEstados → "APROBADA"                          → p_abm_tpex_LogEstados         "I"
```

### FASE 2 — Cotización Bancaria

```
POST /guardar-cotizacion-completa
Body: Cotizaciones {
  idCotizacion: 0,   ← 0 = INSERT, >0 = UPDATE
  idSolicitud: 7,
  codBanco: 3,
  montoCompra: 50000.00,
  tipoCambioOfrecido: 6.97,
  cargos: [ CargoPago ]
}

Pasos:
  1. INSERT/UPDATE Cotizaciones (cabecera)            → p_abm_tpex_Cotizaciones       "I"/"U"
  2. Si INSERT → INSERT cada CargoPago                → p_abm_tpex_CargoPago          "I"
     └─ cargo.idCotizacion = idCotizacion generado
     └─ cargo.idTransaccion = 0  (exclusividad FK)
```

### FASE 3 — Aceptar Cotización Ganadora

```
POST /aceptar-cotizacion
Body: Cotizaciones { idCotizacion, estado: "ACEPTADA", audUsuario }

Pasos:
  1. UPDATE cotización                                → p_abm_tpex_Cotizaciones       "U"
     └─ El SP internamente rechaza las demás cotizaciones de la misma solicitud
  2. LogEstados VIGENTE → "ACEPTADA"                  → p_abm_tpex_LogEstados         "I"
```

### FASE 4 — Transacción de Pago

```
POST /guardar-transaccion-completa
Body: Transacciones {
  idTransaccion: 0,
  idSolicitud: 7,
  idCotizacion: 12,
  montoOrigen: 50000.00,
  tipoCambioAplicado: 6.97,
  cargos: [ CargoPago ]
}

Pasos:
  1. INSERT/UPDATE Transacciones (cabecera)           → p_abm_tpex_Transacciones      "I"/"U"
  2. Si INSERT → INSERT cada CargoPago                → p_abm_tpex_CargoPago          "I"
     └─ cargo.idTransaccion = idTransaccion generado
     └─ cargo.idCotizacion = 0  (exclusividad FK)
  3. Si INSERT → LogEstados NULL → "PENDIENTE"        → p_abm_tpex_LogEstados         "I"

POST /cambiar-estado-transaccion
Body: Transacciones { idTransaccion, estado, audUsuario }
  1. UPDATE estado de la transacción                  → p_abm_tpex_Transacciones      "U"
  2. LogEstados → nuevo estado                        → p_abm_tpex_LogEstados         "I"
```

### FASE 5 — Confirmar y Cerrar ⚠️ Operación más crítica

```
POST /confirmar-pago
Body: ConfirmarPagoRequest { idTransaccion, idSolicitud, numeroTransaccion, fechaValor, audUsuario }

Pasos (todos en una sola transacción Java / ACID):
  1. Transacciones → estado "CONFIRMADO"              → p_abm_tpex_Transacciones      "U"
  2. LogEstados PROCESADO → "CONFIRMADO"              → p_abm_tpex_LogEstados         "I"
  3. SolicitudPago → estado "PAGADA"                  → p_abm_tpex_SolicitudPago      "U"
  4. LogEstados APROBADA → "PAGADA"                   → p_abm_tpex_LogEstados         "I"
```

---

## 📐 DTOs y su propósito

| DTO | Usado en | Descripción |
|-----|---------|-------------|
| `SolicitudPagoDto` | `reporteSolicitudesXFecha` (response) | Vista agregada con `nombre` de empresa (JOIN) + lista anidada de proveedores |
| `SolicitudProveedorDto` | Anidado en `SolicitudPagoDto` | Usa `BigDecimal` para montos financieros; contiene lista de `DetalleSolicitudDto` |
| `DetalleSolicitudDto` | Anidado en `SolicitudProveedorDto` | Fila de factura/detalle en el reporte |
| `ReportePlanoDto` | `SolicitudPagoDao` interno | Fila plana devuelta por SP acción `"B"`, se reconstruye en árbol en memoria |
| `FiltroFechasDto` | `POST /reporte-solicitudes-fechas` (request) | Filtro de rango de fechas `fechaInicio` / `fechaFin` |
| `ConfirmarPagoRequest` | `POST /confirmar-pago` (request) | Agrupa datos de Transaccion + SolicitudPago en un solo body para la fase crítica |

### Reconstrucción de árbol en `SolicitudPagoDao.reporteSolicitudesXFecha`

```
SP p_list_tpex_SolicitudPago acción "B"
  └─ Devuelve List<ReportePlanoDto>  (fila plana por cada detalle)
       │
       ▼  agrupado por idSolicitud  (LinkedHashMap → preserva orden de inserción)
  SolicitudPagoDto
    │  BeanUtils.copyProperties(fila, solicitud)  ← copia campos con mismo nombre
    ▼  agrupado por idSolicitudProveedor  (HashMap)
  SolicitudProveedorDto
    │  BeanUtils.copyProperties(fila, proveedor)
    ▼  por cada fila con idDetalle > 0
  DetalleSolicitudDto
       BeanUtils.copyProperties(fila, detalle)
```

---

## ⚙️ SpHelper — Núcleo de acceso a datos

```java
// ABM (Insert / Update / Delete)
spHelper.ejecutarAbm("p_abm_tpex_Xxx", modelObject, "I" | "U" | "D")
  // 1. Serializa el objeto → Map<String, Object> vía Jackson ObjectMapper
  // 2. Agrega clave "ACCION" al map
  // 3. Obtiene/crea SimpleJdbcCall del caché (ConcurrentHashMap por nombre SP)
  // 4. Ejecuta el SP con MapSqlParameterSource
  // 5. Si error != 0 → lanza SpBusinessException → GlobalExceptionHandler → HTTP 400
  // 6. Retorna RespuestaSp { error, errormsg, idGenerado }

// Listados (SELECT)
spHelper.ejecutarListado("p_list_tpex_Xxx", filtro, "ACCION", Clase.class)
  // Igual, pero usa BeanPropertyRowMapper → devuelve List<T>
  // Clave de caché = spName + "_list_" + clazz.getName()
```

### Códigos de acción usados en este módulo

| Código | Significado en SP | DAOs que lo usan |
|--------|------------------|-----------------|
| `"I"` | INSERT | Todos los DAOs ABM |
| `"U"` | UPDATE | Todos los DAOs ABM |
| `"D"` | DELETE | Todos los DAOs ABM |
| `"L"` | SELECT por ID (o todos si id = 0) | Todos los DAOs listado |
| `"B"` | SELECT por banco / por solicitud / por rango fechas | `CotizacionesDao`, `TiposCambioDao` no, `ConfigComisionesBancoDao`, `SolicitudPagoDao` |
| `"S"` | SELECT por `idSolicitud` | `TransaccionesDao`, `LogEstadosDao` |
| `"C"` | SELECT por `idCotizacion` | `CargoPagoDao`, `TransaccionesDao` |
| `"T"` | SELECT por `idTransaccion` | `CargoPagoDao`, `LogEstadosDao` |
| `"R"` | SELECT por banco (tipos cambio) | `TiposCambioDao` |

---

## 🔗 Mapa completo: Interfaz ↔ DAO ↔ SP

| Interfaz | DAO | SP ABM | SP Listado |
|----------|-----|--------|-----------|
| `ICanalesPago` | `CanalesPagoDao` | `p_abm_tpex_CanalesPago` | `p_list_tpex_CanalesPago` |
| `ICargoPago` | `CargoPagoDao` | `p_abm_tpex_CargoPago` | `p_list_tpex_CargoPago` |
| `IConfigComisionesBanco` | `ConfigComisionesBancoDao` | `p_abm_tpex_ConfigComisionesBanco` | `p_list_tpex_ConfigComisionesBanco` |
| `ICotizaciones` | `CotizacionesDao` | `p_abm_tpex_Cotizaciones` | `p_list_tpex_Cotizaciones` |
| `IDetalleSolicitud` | `DetalleSolicitudDao` | `p_abm_tpex_DetalleSolicitud` | `p_list_tpex_DetalleSolicitud` |
| `ILogEstados` | `LogEstadosDao` | `p_abm_tpex_LogEstados` | `p_list_tpex_LogEstados` |
| `IMonedas` | `MonedasDao` | `p_abm_tpex_Monedas` | `p_list_tpex_Monedas` |
| `ISolicitudPago` | `SolicitudPagoDao` | `p_abm_tpex_SolicitudPago` | `p_list_tpex_SolicitudPago` |
| `ISolicitudProveedor` | `SolicitudProveedorDao` | `p_abm_tpex_SolicitudProveedor` | `p_list_tpex_SolicitudProveedor` |
| `ITiposCambio` | `TiposCambioDao` | `p_abm_tpex_TiposCambio` | `p_list_tpex_TiposCambio` |
| `ITiposCargo` | `TiposCargoDao` | `p_abm_tpex_TiposCargo` | `p_list_tpex_TiposCargo` |
| `ITiposTransaccion` | `TiposTransaccionDao` | `p_abm_tpex_TiposTransaccion` | `p_list_tpex_TiposTransaccion` |
| `ITransacciones` | `TransaccionesDao` | `p_abm_tpex_Transacciones` | `p_list_tpex_Transacciones` |

---

## 📋 Endpoints completos

### Escrituras transaccionales (`@Transactional`)

| Endpoint | Body principal | Descripción |
|---------|---------------|-------------|
| `POST /guardar-solicitud-completa` | `SolicitudPago` | Crea/actualiza solicitud + proveedores + facturas (ACID) |
| `POST /aprobar-solicitud` | `SolicitudPago` | Cambia estado + log (ACID) |
| `POST /guardar-cotizacion-completa` | `Cotizaciones` | Crea cotización + cargos (ACID) |
| `POST /aceptar-cotizacion` | `Cotizaciones` | Acepta ganadora, rechaza demás + log (ACID) |
| `POST /guardar-transaccion-completa` | `Transacciones` | Crea transacción + cargos + log (ACID) |
| `POST /cambiar-estado-transaccion` | `Transacciones` | Cambia estado + log (ACID) |
| `POST /confirmar-pago` | `ConfirmarPagoRequest` | Confirma transacción + cierra solicitud como PAGADA (ACID) |

### Lecturas (sin `@Transactional`)

| Endpoint | Filtro | Devuelve |
|---------|--------|---------|
| `POST /obtener-solicitudes` | `{ idSolicitud }` | `List<SolicitudPago>` |
| `POST /obtener-solicitud-proveedor` | `{ idSolicitudProveedor }` | `List<SolicitudProveedor>` |
| `POST /obtener-detalle-solicitud` | `{ idDetalle }` | `List<DetalleSolicitud>` |
| `POST /obtener-proveedores-empresa` | `{ codEmpresa }` | `List<SolicitudProveedor>` |
| `POST /obtener-docnum-empresa` | `{ codEmpresa }` | `List<DetalleSolicitud>` |
| `POST /reporte-solicitudes-fechas` | `FiltroFechasDto` | `List<SolicitudPagoDto>` (árbol) |
| `POST /obtener-cotizaciones-solicitud` | `{ idSolicitud }` | `List<Cotizaciones>` |
| `POST /obtener-transacciones-solicitud` | `{ idSolicitud }` | `List<Transacciones>` |
| `POST /obtener-transacciones-cotizacion` | `{ idCotizacion }` | `List<Transacciones>` |
| `POST /obtener-cargos-cotizacion` | `{ idCotizacion }` | `List<CargoPago>` |
| `POST /obtener-cargos-transaccion` | `{ idTransaccion }` | `List<CargoPago>` |
| `POST /obtener-log-solicitud` | `{ idSolicitud }` | `List<LogEstados>` |
| `POST /obtener-log-transaccion` | `{ idTransaccion }` | `List<LogEstados>` |

### Catálogos (CRUD simple, atómico)

| Entidad | Registrar | Eliminar | Obtener por ID | Obtener extra |
|---------|----------|---------|---------------|--------------|
| `CanalesPago` | `/registrar-canal-pago` | `/eliminar-canal-pago` | `/obtener-canales-pago` | — |
| `Monedas` | `/registrar-moneda` | `/eliminar-moneda` | `/obtener-monedas` | — |
| `TiposCambio` | `/registrar-tipo-cambio` | `/eliminar-tipo-cambio` | `/obtener-tipos-cambio` | `/obtener-tipos-cambio-banco` |
| `TiposCargo` | `/registrar-tipo-cargo` | `/eliminar-tipo-cargo` | `/obtener-tipos-cargo` | — |
| `TiposTransaccion` | `/registrar-tipo-transaccion` | `/eliminar-tipo-transaccion` | `/obtener-tipos-transaccion` | — |
| `ConfigComisionesBanco` | `/registrar-config-comisiones` | `/eliminar-config-comisiones` | `/obtener-config-comisiones` | `/obtener-config-comisiones-banco` |

---

## 🛡️ Seguridad y Respuestas HTTP

### Acceso
- Ruta base: `POST /pagos-extranjeros/**`
- Requiere JWT válido + rol `ROLE_ADM` o `ROLE_LIM`
- `@CrossOrigin(origins = "*")` habilitado para el frontend Angular

### Códigos de respuesta

| Código | Cuándo | Body |
|--------|--------|------|
| `201 Created` | Escritura exitosa | `ApiResponse { message: "Operación realizada exitosamente", data: idGenerado }` |
| `200 OK` | Lista con resultados | `ApiResponse { data: List<T> }` |
| `204 No Content` | Lista vacía | `ApiResponse { message: "No se encontraron..." }` |
| `400 Bad Request` | SP devuelve `error != 0` → `SpBusinessException` | `ApiResponse { message: errormsg del SP }` |
| `403 Forbidden` | Sin rol requerido | — |
| `500 Internal Server Error` | Fallo de BD o excepción inesperada | — |

### Método auxiliar `ejecutar()` en el controller

```java
// Dentro de un @Transactional, lanzar RuntimeException dispara rollback completo
private void ejecutar(RespuestaSp res, String contexto) {
    if (res.getError() != 0) {
        throw new RuntimeException(contexto + ": " + res.getErrormsg());
    }
}
```

### Método auxiliar `registrarLog()` en el controller

```java
// Exactamente uno de los tres IDs debe ser != null
private void registrarLog(Long idSolicitud, Long idCotizacion, Long idTransaccion,
                          String estadoNuevo, int audUsuario) {
    // Construye LogEstados y llama a logEstadosDao.registrarLogEstados(log, "I")
    // Si falla → RuntimeException → rollback de toda la transacción padre
}
```

