# Análisis Completo del Flujo de Información — `PagosExtranjerosController`

## 1. Visión General

El `PagosExtranjerosController` es un controlador REST Spring Boot que gestiona el ciclo completo de **pagos internacionales** para una empresa boliviana de importación/exportación. El flujo cubre desde la solicitud de pago hasta la confirmación final, pasando por cotizaciones bancarias y transacciones.

**Ruta base:** `POST /pagos-extranjeros/**`  
**Seguridad:** JWT + roles `ROLE_ADM` o `ROLE_LIM`  
**Arquitectura:** Sin JPA/ORM — usa `JdbcTemplate` + `SimpleJdbcCall` → Stored Procedures SQL Server  
**Patrón ABM:** Todos los SPs reciben un parámetro `ACCION` (`"I"`=Insert, `"U"`=Update, `"D"`=Delete)  
**Patrón Listado:** SPs de listado reciben `ACCION` (`"L"`=por ID, `"B"`=por rango/banco, `"S"`=por solicitud, `"C"`=por cotización, `"T"`=por transacción)

---

## 2. Arquitectura de Capas

```
Angular Frontend
      │  POST JSON (todos los endpoints son POST)
      ▼
PagosExtranjerosController  (@Transactional en escrituras)
      │  13 DAOs inyectados vía constructor (interfaces)
      ▼
[I*Dao interface] → [*Dao implements I*]
      │  SpHelper.ejecutarAbm() / ejecutarListado()
      │  Serializa modelo → Map<String,Object> vía Jackson + agrega "ACCION"
      ▼
SQL Server → Stored Procedures (p_abm_tpex_* / p_list_tpex_*)
      │  Retornan: error (0=OK), errormsg, idGenerado
      ▼
RespuestaSp → ApiResponse → ResponseEntity<ApiResponse<?>>
```

---

## 3. Modelo de Datos y Relaciones

### 3.1 Catálogos (6 tablas maestras)

| Tabla | PK | Propósito | FKs relevantes |
|-------|----|-----------|----------------|
| `tpex_Monedas` | `idMoneda` | Monedas (USD, BOB, EUR, USDT) | — |
| `tpex_CanalesPago` | `idCanal` | Canales de pago (BANCARIO, EXPORTADORA, CRIPTO) | — |
| `tpex_TiposCargo` | `idTipoCargo` | Tipos de cargo (por % o fijo) | — |
| `tpex_TiposTransaccion` | `idTipoTransaccion` | Tipos de operación cambiaria | — |
| `tpex_TiposCambio` | `idTipoCambio` | Tasas de cambio por banco/fecha | `codBanco`, `idMonedaOrigen`, `idMonedaDestino` |
| `tpex_ConfigComisionesBanco` | `idConfig` | Comisiones configuradas por banco/tipo | `codBanco`, `idTipoTransaccion`, `idTipoCargo`, `idMoneda` |

### 3.2 Entidades Transaccionales (5 tablas + 1 de log)

```
tpex_SolicitudPago (1)
  ├── idSolicitud (PK)
  ├── codEmpresa (FK → tb_empresa)
  ├── estado: PENDIENTE → APROBADA → PAGADA
  │
  ├── tpex_SolicitudProveedor (1..N)
  │     ├── idSolicitudProveedor (PK)
  │     ├── idSolicitud (FK)
  │     ├── cardCode / cardName (SAP)
  │     │
  │     └── tpex_DetalleSolicitud (1..N)
  │           ├── idDetalle (PK)
  │           ├── idSolicitudProveedor (FK)
  │           └── facturas individuales (monto, amortización, etc.)
  │
  ├── tpex_Cotizaciones (1..N, ofertadas por bancos)
  │     ├── idCotizacion (PK)
  │     ├── idSolicitud (FK)
  │     ├── codBanco (FK → tch_banco)
  │     ├── esGanadora (1 = cotización aceptada)
  │     ├── estado: VIGENTE → ACEPTADA / RECHAZADA
  │     │
  │     └── tpex_Cargos (1..N, sobre cotización)
  │           ├── idCargo (PK)
  │           ├── idCotizacion (FK) ← 0 si pertenece a transacción
  │           ├── idTransaccion (FK) ← 0 si pertenece a cotización
  │           └── (mutuamente excluyentes: solo uno != 0)
  │
  └── tpex_Transacciones (1, ejecución del pago)
        ├── idTransaccion (PK)
        ├── idSolicitud (FK)
        ├── idCotizacion (FK, la ganadora)
        ├── estado: PENDIENTE → PROCESADO → CONFIRMADO
        │
        └── tpex_Cargos (1..N, sobre transacción)

tpex_LogEstados (auditoría transversal)
  ├── idLog (PK)
  ├── idSolicitud / idCotizacion / idTransaccion (exactamente uno != 0)
  ├── estadoAnterior → estadoNuevo
  └── Trazabilidad completa de todos los cambios de estado
```

---

## 4. Flujo del Proceso (5 Fases) — Detalle Completo

### FASE 1: Solicitud de Pago

**Endpoint:** `POST /guardar-solicitud-completa`  
**Body:** `SolicitudPago` con `proveedores[]` anidados (cada uno con `detalles[]`) y listas de IDs a eliminar.  
**Transaccional:** Sí — si cualquier paso falla, rollback total.

```
Flujo interno:
1. DELETE detalles marcados en proveedores[].detallesAEliminar[]
   → detalleSolicitudDao.registrarDetalleSolicitud(det, "D") × N
   
2. DELETE proveedores marcados en proveedoresAEliminar[]
   → solicitudProveedorDao.registrarSolicitudProveedor(prov, "D") × N

3. INSERT/UPDATE cabecera SolicitudPago
   → solicitudPagoDao.registrarSolicitudPago(payload, "I"|"U")
   → Obtiene idSolicitud desde resSol.getIdGenerado()

4. Para cada proveedor en proveedores[]:
   a. INSERT/UPDATE SolicitudProveedor
      → solicitudProveedorDao.registrarSolicitudProveedor(prov, "I"|"U")
      → Obtiene idProveedor desde resProv.getIdGenerado()
   
   b. Para cada detalle en proveedor.detalles[]:
      INSERT/UPDATE DetalleSolicitud
      → detalleSolicitudDao.registrarDetalleSolicitud(det, "I"|"U")

5. Retorna 201 Created con idSolicitud
```

**Aprobación:** `POST /aprobar-solicitud`  
- Carga la solicitud actual → cambia `estado` → UPDATE via SP `"U"`  
- El SP internamente registra LogEstados (PENDIENTE → APROBADA)

---

### FASE 2: Cotización Bancaria

**Endpoint:** `POST /guardar-cotizacion-completa`  
**Body:** `Cotizaciones` con `cargos[]` anidados.  
**Transaccional:** Sí.

```
Flujo interno:
1. INSERT/UPDATE Cotizaciones
   → cotizacionesDao.registrarCotizaciones(payload, "I"|"U")
   → Obtiene idCotizacion desde res.getIdGenerado()

2. Solo si es INSERT (idCotizacion era 0):
   Para cada cargo en cargos[]:
   a. Asigna cargo.idCotizacion = idCotizacion
   b. Asigna cargo.idTransaccion = 0 (exclusividad FK)
   c. Auto-numera cargo.orden si es 0
   d. INSERT CargoPago → cargoPagoDao.registrarCargoPago(cargo, "I")

3. Retorna 201 Created con idCotizacion
```

**Nota:** Los cargos solo se insertan en cotizaciones nuevas. En UPDATE de cotización, los cargos no se modifican desde este endpoint.

---

### FASE 3: Aceptar Cotización Ganadora

**Endpoint:** `POST /aceptar-cotizacion`  
**Body:** `Cotizaciones { idCotizacion, estado: "ACEPTADA", audUsuario }`  
**Transaccional:** Sí.

```
Flujo interno:
1. UPDATE Cotizaciones → cotizacionesDao.registrarCotizaciones(payload, "U")
   → El SP internamente:
     - Marca esta cotización como esGanadora=1, estado="ACEPTADA"
     - Rechaza las demás cotizaciones de la misma solicitud (estado="RECHAZADA")
     - Registra LogEstados (VIGENTE → ACEPTADA / RECHAZADA)

2. Retorna 201 Created con idCotizacion
```

---

### FASE 4: Transacción de Pago

**Endpoint:** `POST /guardar-transaccion-completa`  
**Body:** `Transacciones` con `cargos[]` anidados.  
**Transaccional:** Sí.

```
Flujo interno:
1. INSERT/UPDATE Transacciones
   → transaccionesDao.registrarTransacciones(payload, "I"|"U")
   → Obtiene idTransaccion desde res.getIdGenerado()

2. Solo si es INSERT (idTransaccion era 0):
   Para cada cargo en cargos[]:
   a. Asigna cargo.idTransaccion = idTransaccion
   b. Asigna cargo.idCotizacion = 0 (exclusividad FK)
   c. Auto-numera cargo.orden si es 0
   d. INSERT CargoPago → cargoPagoDao.registrarCargoPago(cargo, "I")

3. Retorna 201 Created con idTransaccion
```

**Cambio de estado:** `POST /cambiar-estado-transaccion`  
- Carga transacción actual → actualiza `estado`, `numeroTransaccion`, `fechaValor` → UPDATE via SP `"U"`

---

### FASE 5: Confirmar Pago ⚠️ Operación más crítica

**Endpoint:** `POST /confirmar-pago`  
**Body:** `ConfirmarPagoRequest { idTransaccion, idSolicitud, numeroTransaccion, fechaValor, audUsuario, codEmpresa, montoTotalSolicitud }`  
**Transaccional:** Sí — ACID completo, cierra DOS entidades en una sola transacción Java.

```
Flujo interno:
1. Carga Transacción actual → verifica existencia
2. SET trx.estado = "CONFIRMADO"
3. UPDATE Transacciones → transaccionesDao.registrarTransacciones(trx, "U")
   → El SP registra LogEstados (PROCESADO → CONFIRMADO)

4. Carga SolicitudPago actual → verifica existencia
5. SET sol.estado = "PAGADA"
6. Si montoTotalSolicitud > 0 → actualiza monto
7. UPDATE SolicitudPago → solicitudPagoDao.registrarSolicitudPago(sol, "U")
   → El SP registra LogEstados (APROBADA → PAGADA)

8. Retorna 201 Created con idSolicitud
```

**Si cualquier paso falla → RuntimeException → rollback de AMBAS entidades.**

---

## 5. Endpoints de Consulta (sin @Transactional)

| Endpoint | Parámetro de filtro | Retorna | SP subyacente |
|----------|-------------------|---------|---------------|
| `/obtener-solicitudes` | `idSolicitud` (0=todos) | `List<SolicitudPago>` | `p_list_tpex_SolicitudPago` "L" |
| `/obtener-solicitud-proveedor` | `idSolicitudProveedor` | `List<SolicitudProveedor>` | `p_list_tpex_SolicitudProveedor` "L" |
| `/obtener-detalle-solicitud` | `idDetalle` | `List<DetalleSolicitud>` | `p_list_tpex_DetalleSolicitud` "L" |
| `/obtener-proveedores-empresa` | `codEmpresa` | `List<SolicitudProveedor>` | `p_list_tpex_SolicitudProveedor` "B" |
| `/obtener-docnum-empresa` | `codEmpresa` | `List<DetalleSolicitud>` | `p_list_tpex_DetalleSolicitud` "B" |
| `/reporte-solicitudes-fechas` | `FiltroFechasDto` | `List<SolicitudPagoDto>` (árbol) | `p_list_tpex_SolicitudPago` "B" |
| `/obtener-cotizaciones-solicitud` | `idSolicitud` | `List<Cotizaciones>` | `p_list_tpex_Cotizaciones` "S" |
| `/obtener-transacciones-solicitud` | `idSolicitud` + filtros | `List<Transacciones>` | `p_list_tpex_Transacciones` "S" |
| `/obtener-transacciones-cotizacion` | `idCotizacion` | `List<Transacciones>` | `p_list_tpex_Transacciones` "C" |
| `/obtener-transaccion` | `idTransaccion` + `codEmpresa` | `Transacciones` (objeto único) | `p_list_tpex_Transacciones` "L" |
| `/reporte-transacciones-fechas` | fechas + filtros | `List<Transacciones>` | `p_list_tpex_Transacciones` "B" |
| `/obtener-cargos-cotizacion` | `idCotizacion` | `List<CargoPago>` | `p_list_tpex_Cargos` "C" |
| `/obtener-cargos-transaccion` | `idTransaccion` | `List<CargoPago>` | `p_list_tpex_Cargos` "T" |
| `/obtener-log-solicitud` | `idSolicitud` | `List<LogEstados>` | `p_list_tpex_LogEstados` "S" |
| `/obtener-log-transaccion` | `idTransaccion` | `List<LogEstados>` | `p_list_tpex_LogEstados` "T" |
| `/obtener-timeline-solicitud` | `idSolicitud` | `List<LogEstados>` | `p_list_tpex_LogEstados` "B" |

---

## 6. Endpoints de Catálogos (CRUD simple)

Cada catálogo sigue el mismo patrón:
- **Registrar:** `idXxx == 0` → `"I"`, `idXxx > 0` → `"U"`
- **Eliminar:** siempre `"D"`
- **Obtener:** por ID (0 = todos), algunos tienen endpoint extra por banco

| Entidad | Registrar | Eliminar | Obtener | Extra |
|---------|----------|---------|---------|-------|
| `CanalesPago` | `/registrar-canal-pago` | `/eliminar-canal-pago` | `/obtener-canales-pago` | — |
| `Monedas` | `/registrar-moneda` | `/eliminar-moneda` | `/obtener-monedas` | — |
| `TiposCambio` | `/registrar-tipo-cambio` | `/eliminar-tipo-cambio` | `/obtener-tipos-cambio` | `/obtener-tipos-cambio-banco` |
| `TiposCargo` | `/registrar-tipo-cargo` | `/eliminar-tipo-cargo` | `/obtener-tipos-cargo` | — |
| `TiposTransaccion` | `/registrar-tipo-transaccion` | `/eliminar-tipo-transaccion` | `/obtener-tipos-transaccion` | — |
| `ConfigComisionesBanco` | `/registrar-config-comisiones` | `/eliminar-config-comisiones` | `/obtener-config-comisiones` | `/obtener-config-comisiones-banco` |

---

## 7. Gestión de Vouchers (Archivos)

### Subir voucher: `POST /transacciones/{idTransaccion}/voucher`
- **Content-Type:** `multipart/form-data`
- **Parámetros:** `file` (MultipartFile) + `audUsuario`
- **Extensiones permitidas:** PDF, JPG, JPEG, PNG
- **Ruta de almacenamiento:** `pagos-extranjeros/vouchers/{idTransaccion}_{timestamp}.{ext}`
- **Flujo:**
  1. Validar archivo no vacío y extensión permitida
  2. `fileStorageService.guardarVoucher(file, rutaRelativa)` → guarda en filesystem
  3. `transaccionesDao.actualizarVoucher(idTransaccion, rutaRelativa, audUsuario)` → actualiza BD
  4. Si el SP falla → `fileStorageService.eliminarVoucher(rutaRelativa)` → compensación manual

### Descargar voucher: `GET /transacciones/{idTransaccion}/voucher`
- **Parámetros:** `idTransaccion` (path) + `codEmpresa` (query, default 0)
- **Flujo:**
  1. Carga transacción → verifica que tenga `rutaVoucher` y `tieneVoucher = true`
  2. `fileStorageService.obtenerVoucher(ruta)` → carga archivo como `Resource`
  3. Determina Content-Type según extensión (pdf/jpg/jpeg/png)
  4. Retorna archivo inline con header `Content-Disposition: inline`

---

## 8. Máquina de Estados

### SolicitudPago
```
PENDIENTE ──(aprobar-solicitud)──→ APROBADA ──(confirmar-pago)──→ PAGADA
```

### Cotizaciones
```
VIGENTE ──(aceptar-cotizacion)──→ ACEPTADA  (esGanadora = 1)
VIGENTE ──(aceptar otra)────────→ RECHAZADA (automático por SP)
```

### Transacciones
```
PENDIENTE ──(cambiar-estado)──→ PROCESADO ──(confirmar-pago)──→ CONFIRMADO
```

---

## 9. Respuestas HTTP y Manejo de Errores

| Código | Cuándo | Body |
|--------|--------|------|
| `201 Created` | Escritura exitosa | `ApiResponse { message, data: idGenerado }` |
| `200 OK` | Lista con resultados | `ApiResponse { message, data: List<T> }` |
| `204 No Content` | Lista vacía | `ApiResponse { message: "No se encontraron..." }` |
| `400 Bad Request` | SP devuelve `error != 0` → `SpBusinessException` | Error del SP |
| `403 Forbidden` | Sin rol requerido | — |
| `500 Internal Server Error` | Fallo inesperado | — |

**Mecanismo de error:** El método auxiliar `ejecutar(RespuestaSp, contexto)` lanza `SpBusinessException` si `res.getError() != 0`, lo que dentro de `@Transactional` provoca rollback automático.

---

## 10. Convenciones Clave para un Agente de IA

1. **Todos los endpoints son POST** (incluso las consultas) — el body siempre es JSON.
2. **El patrón ABM es uniforme:** Un único SP por tabla maneja Insert/Update/Delete según el parámetro `ACCION`.
3. **Los IDs generados** se obtienen de `RespuestaSp.getIdGenerado()` — si es 0, se usa el ID del payload (caso UPDATE).
4. **Los cargos son polimórficos:** `CargoPago` pertenece a una cotización (`idCotizacion != 0, idTransaccion = 0`) O a una transacción (`idCotizacion = 0, idTransaccion != 0`), nunca a ambas.
5. **Los SPs manejan lógica de negocio:** La aceptación de cotización, el rechazo automático de las demás, y el registro de LogEstados ocurren dentro de los SPs, no en Java.
6. **Los modelos son POJOs planos** (sin JPA/Hibernate) — la persistencia es 100% via SPs.
7. **SpHelper serializa con Jackson** el modelo completo a `Map<String,Object>` y lo pasa como parámetros al SP.
8. **El reporte de solicitudes por fecha** usa un patrón especial: el SP devuelve filas planas (`ReportePlanoDto`) y el DAO reconstruye el árbol (Solicitud→Proveedor→Detalle) en memoria usando `LinkedHashMap`.
9. **`ConfirmarPagoRequest`** es un DTO especial que agrupa datos de Transacción + Solicitud porque la Fase 5 cierra ambas entidades en una sola transacción ACID.
10. **Campos de filtro** como `fechaInicio`, `fechaFin`, `codEmpresa` existen en los modelos solo para ser serializados y pasados al SP como parámetros de búsqueda — no se persisten.

---

## 11. Mapa Completo: Interfaz ↔ DAO ↔ SP

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

## 12. Códigos de Acción Usados en SPs de Listado

| Código | Significado | DAOs que lo usan |
|--------|------------|-----------------|
| `"L"` | SELECT por ID (o todos si id = 0) | Todos los DAOs listado |
| `"B"` | SELECT por banco / por solicitud / rango fechas | `CotizacionesDao`, `ConfigComisionesBancoDao`, `SolicitudPagoDao`, `TransaccionesDao` |
| `"S"` | SELECT por `idSolicitud` | `TransaccionesDao`, `LogEstadosDao` |
| `"C"` | SELECT por `idCotizacion` | `CargoPagoDao`, `TransaccionesDao` |
| `"T"` | SELECT por `idTransaccion` | `CargoPagoDao`, `LogEstadosDao` |
| `"R"` | SELECT por banco (tipos de cambio) | `TiposCambioDao` |

---

## 13. SpHelper — Núcleo de Acceso a Datos

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

---

## 14. DTOs y su Propósito

| DTO | Usado en | Descripción |
|-----|---------|-------------|
| `SolicitudPagoDto` | `reporteSolicitudesXFecha` (response) | Vista agregada con `nombre` de empresa + lista anidada de proveedores |
| `SolicitudProveedorDto` | Anidado en `SolicitudPagoDto` | Usa `BigDecimal` para montos; contiene lista de `DetalleSolicitudDto` |
| `DetalleSolicitudDto` | Anidado en `SolicitudProveedorDto` | Fila de factura/detalle en el reporte |
| `ReportePlanoDto` | `SolicitudPagoDao` interno | Fila plana devuelta por SP acción `"B"`, reconstruida en árbol en memoria |
| `FiltroFechasDto` | `POST /reporte-solicitudes-fechas` (request) | Filtro de rango `fechaInicio` / `fechaFin` |
| `ConfirmarPagoRequest` | `POST /confirmar-pago` (request) | Agrupa datos de Transaccion + SolicitudPago para la fase crítica |

### Reconstrucción de árbol en `SolicitudPagoDao.reporteSolicitudesXFecha`

```
SP p_list_tpex_SolicitudPago acción "B"
  └─ Devuelve List<ReportePlanoDto>  (fila plana por cada detalle)
       │
       ▼  agrupado por idSolicitud  (LinkedHashMap → preserva orden)
  SolicitudPagoDto
    │  BeanUtils.copyProperties(fila, solicitud)
    ▼  agrupado por idSolicitudProveedor  (HashMap)
  SolicitudProveedorDto
    │  BeanUtils.copyProperties(fila, proveedor)
    ▼  por cada fila con idDetalle > 0
  DetalleSolicitudDto
       BeanUtils.copyProperties(fila, detalle)