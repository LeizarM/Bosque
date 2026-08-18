# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# BOSQUE — Backend Spring Boot

ERP backend for Bosque (paper im/export, RRHH, fleet, treasury). Spring Boot monolith, Angular SPA frontend (separate repo). **No JPA/ORM** — all persistence is SQL Server Stored Procedures.

## Commands
Maven wrapper (`mvnw.cmd` on Windows / `./mvnw` on *nix):
```
.\mvnw.cmd spring-boot:run          # run app (port 9223)
.\mvnw.cmd clean package            # build jar → target/bosque-1.0.0.jar
.\mvnw.cmd test                     # all tests
.\mvnw.cmd test -Dtest=ClassName#method   # single test
java -jar target/bosque-1.0.0.jar   # run built jar
```
- Java 8 (`java.version=1.8`), Spring Boot 2.6.1.
- DB creds via env: `DB_USERNAME`/`DB_PASSWORD` (default `sa`/in properties). JWT lifetime `JWT_EXPIRATION` (sec). `spring-boot-devtools` gives hot reload on `spring-boot:run`.

## Architecture

### Package layout (`bo.bosque.com.impexpap`)
- `controller/` — `@RestController`, one per domain (RRHH, Tigo, Gasolina, PagosExtranjeros, ChCheque, Resmado, Multas, Anticipo…). **Almost all endpoints are POST.**
- `dao/` + `model/` — DAO (`<Entidad>Dao implements I<Entidad>`) wraps SP calls; model = plain POJO mirroring table columns + audit fields (`audUsuario`, `audFecha`).
- `dto/` — request/response shapes distinct from models.
- `security/` + `security/jwt/` — auth chain.
- `config/` — exception handler, Jackson, business exception.
- `utils/` — `SpHelper`, `ApiResponse`, `RespuestaSp`, `ClassGenerator`.
- `commons/` — cross-cutting services: `FileStorageService`, `PdfGeneratorService`, `JasperReportExport`, `WhatsAppService`.

### Two DAO eras — match the one already used in the module you touch
1. **Legacy** (`ColorDao` etc.): raw `jdbcTemplate.update("execute p_abm_Xxx @p=?, ...")`, returns `boolean`, swallows `BadSqlGrammarException`. Older non-tpex modules.
2. **New / `SpHelper`** (tpex module + newer code): centralized, typed errors, auto Jackson serialization. **Prefer this for new code.**

### SpHelper (`utils/SpHelper.java`) — the persistence core
```java
spHelper.ejecutarAbm(spName, modelObject, "I"|"U"|"D")   // serializes model→Map via Jackson, calls SP, returns RespuestaSp
spHelper.ejecutarAbmMap(spName, map, accion)             // send EXACTLY these params (rest stay NULL/default in SP)
spHelper.ejecutarListado(spName, model|map, "ACCION", Clase.class)  // BeanPropertyRowMapper → List<T>
```
- SP error code `!= 0` → throws `SpBusinessException` → HTTP 400.
- `RespuestaSp { error, errormsg, idGenerado }`.
- **model vs Map overload matters:** `ejecutarListado(model, …)` strips nulls/collections but **keeps numeric 0** — fine for SPs treating 0 as "return all", WRONG for SPs using NULL as "no filter". Use the **Map overload** to load-by-ID so default 0s don't corrupt filtering.
- SPs are cached per name (`spCache`).

### HTTP response contract
All responses wrap `ApiResponse<T> { message, data, status }`.
- 201 Created — write OK, `data` = generated id.
- 200 OK — non-empty list. 204 No Content — empty list.
- 400 — `SpBusinessException` (SP business error). 403 — `AccessDeniedException`. 415 — bad Content-Type on upload. 500 — uncaught.
- Centralized in `config/GlobalExceptionHandler.java` (`@RestControllerAdvice`).

### Security (`security/MainSecurity.java`)
Stateless JWT. Filter order (all before `UsernamePasswordAuthenticationFilter`):
1. `SecurityFilter` — blocks suspicious User-Agents (sqlmap, nikto…) and SQLi-ish patterns in params/query (skips body for multipart uploads).
2. `RateLimitFilter` (bucket4j).
3. `JwtTokenFilter` — validates token, sets auth context.

Roles: `ROLE_ADM`, `ROLE_LIM`. Method-level `@PreAuthorize("hasAnyRole(...)")` + `@EnableGlobalMethodSecurity`. `permitAll` paths: `/auth/**`, `/pagos-extranjeros/**`, several `/fichaTrabajador/uploads/**` and `/tigo/uploads/**` static paths — but controllers may still enforce roles via `@PreAuthorize`, so a valid token is generally still needed.

### Reports & files
- JasperReports (`src/main/resources/reports/*.jrxml`) via `JasperReportExport` / `PdfGeneratorService`; also iText7 + Apache POI. Used heavily by RRHH (ficha trabajador) and Tigo.
- Uploads land in `uploads/` (e.g. `uploads/pagos-extranjeros/`) via `FileStorageService`; multipart limit 10MB.

## tpex module — Pagos al Exterior (`/pagos-extranjeros/**`)
Most actively developed module. Strict uniform pattern per entity:
- `I<Entidad>` interface + `<Entidad>Dao` + SP `p_abm_tpex_<Entidad>` (ACCION I/U/D) + SP `p_list_tpex_<Entidad>` (ACCION L=by id, B=range/bank, S=by solicitud, C=by cotización, T=by transacción, V=validate).

| DAO | ABM SP | List SP |
|-----|--------|---------|
| Transacciones / Cargos / Asientos / Cotizaciones | `p_abm_tpex_*` | `p_list_tpex_*` |
| SolicitudPago / SolicitudProveedor / DetalleSolicitud / LogEstados | `p_abm_tpex_*` | `p_list_tpex_*` |

Lifecycle (FASE 1 solicitud → 2 cotización → 3 aceptación → 4 transacción → 5 confirmación). State machines:
```
SolicitudPago:  PENDIENTE → APROBADA → PAGADA
Cotizaciones:   VIGENTE → ACEPTADA / RECHAZADA
Transacciones:  PENDIENTE → PROCESADO → CONFIRMADO
```
Reference impl to copy: `TransaccionesDao` / `CargosDao` (DAO), `PagosExtranjerosController` (endpoints).

### Pending: Asientos contables (partially specced)
Table `tpex_Asientos` exists. Model `Asientos.java` fields: idAsiento, idTransaccion, numero, tipoAsiento(PR/PE/MP), codBancoRef(FK tch_banco), cuentaDebe/Haber, descripcion, debito/credito Us/Bs, tcAplicado, audUsuario/Fecha; read-only joins (banco, estadoTransaccion, fechaTransaccion) and cuadre fields (totalDebito/Credito Us/Bs, diferenciaBs, estadoCuadre CUADRADO/DESCUADRADO) only on ACCION "V". Endpoints to add: `registrar-asiento` (ABM I/U/D), `obtener-asientos-transaccion` (list T), `validar-cuadre-asientos` (list V).

## Database
SQL Server 2008, BD `BOSQUE-2_0`. Runtime DataSource in `application.properties` (`192.168.3.116:1433`). A readonly account `claude_readonly` exists at `181.114.119.194:8800` for inspection. `schema.sql` / `src/main/resources/sql/` hold reference DDL/fixes. All business logic lives in SPs — to understand behavior, read the SP, not Java.
