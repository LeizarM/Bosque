# Auditoría de seguridad y calidad — Bosque Backend

**Fecha:** 2026-08-15
**Alcance:** `D:\Proyectos\Bosque\Bosque Spring` — Spring Boot 2.6.1 / Java 8, 492 archivos en `src/main`, sin ORM, persistencia 100% por procedimientos almacenados de SQL Server 2008.
**Verificación de build:** `mvnw test` → BUILD SUCCESS, 102 tests, 0 fallos (JDK 1.8.0_231).

---

## Resumen ejecutivo

Backend Spring Boot 2.6.1 / Java 8, 492 archivos, sin ORM, todo por SPs de SQL Server. El código nuevo (módulo tpex, permisos RRHH, entregas) es de calidad alta: `SpHelper` centralizado, `@PreAuthorize`, tests de arquitectura que prohíben SQL crudo, javadoc que documenta las trampas. El código legacy arrastra defectos graves.

Cuatro problemas Critical: **clave de firma JWT hardcodeada y versionada** (cualquiera con acceso al repo forja tokens de admin), **contraseña de `sa` en claro en tres archivos versionados**, **`application.properties` commiteado en "MODO PRUEBA"** con un desvío de WhatsApp que silenciaría los avisos a todos los clientes, y **`this.jdbcTemplate = null` en 154 lugares de 73 DAOs**, que deja un DAO muerto hasta reiniciar la app.

Corrección importante al contexto de partida: **el driver NO es jTDS**. Es `mssql-jdbc 9.4.0.jre8` (verificado en `target/cp.txt`). Fuera de la matriz de soporte de SQL Server 2008 y con riesgo alto de fallo TLS en Fedora. Es el bloqueante real de la migración a Linux.

Además: 0 `@Valid` en 452 endpoints, CORS `*` en 27 controllers, rate-limiting evadible con un header, y ~30 clases muertas más `bin/` (375 archivos, incluidos `.class` y la contraseña de `sa`).

---

## Hallazgos Critical

### C1 — Clave privada RSA hardcodeada, usada como secreto HMAC de los JWT

`security/jwt/JwtConfig.java:13-40` · `security/jwt/JwtProvider.java:44,54,64`

Una clave RSA privada completa está embebida como constante y versionada en git. `JwtProvider` la usa como **secreto simétrico** de HS512:

```java
.signWith(SignatureAlgorithm.HS512, JwtConfig.RSA_PRIVATE)
```

**Impacto:** cualquiera con acceso al repo (o al jar, o al `bin/` versionado) firma tokens válidos para cualquier `login`, `codUsuario`, `codEmpresa` y `tipoUsuario`. `JwtTokenFilter` solo valida firma y carga el usuario del DAO — no hay segundo control. Autenticación completamente comprometida.

**Fix (Java 8, sin dependencias nuevas):**

```java
@Component
public class JwtProvider {
    private final byte[] claveFirma;

    public JwtProvider(@Value("${jwt.secret}") String secretoBase64) {
        byte[] k = java.util.Base64.getDecoder().decode(secretoBase64);
        if (k.length < 64) {  // HS512 pide >= 512 bits
            throw new IllegalStateException("jwt.secret debe tener al menos 64 bytes");
        }
        this.claveFirma = k;
    }
    // ...signWith(SignatureAlgorithm.HS512, claveFirma)
    // ...Jwts.parser().setSigningKey(claveFirma)
}
```

```properties
jwt.secret=${JWT_SECRET}
```

Generar: `openssl rand -base64 64`. Borrar `JwtConfig.java` entero (`RSA_PUBLIC` tampoco se usa). **Rotar la clave invalida todos los tokens vivos** — coordinar con la app Flutter y el frontend Angular, y avisar antes del despliegue.

---

### C2 — Contraseña de `sa` en claro, en tres archivos versionados

`src/main/resources/application.properties:23` · `utils/ClassGenerator.java:16-19` · `bin/src/main/resources/application.properties:4-5`

```properties
spring.datasource.password=${DB_PASSWORD:sapbus1n3ss}   # el default ES la clave real
```

```java
private static final String DATABASE_URL  = "jdbc:sqlserver://192.168.3.116:1433;databaseName=BOSQUE-2_0;?useSSL=false";
private static final String DATABASE_USER = "sa";
private static final String DATABASE_PASSWORD = "sapbus1n3ss";
```

`bin/src/main/resources/application.properties` la tiene **sin siquiera la envoltura de variable de entorno**. `sa` es sysadmin: control total del motor, no solo de `BOSQUE-2_0`.

Agravante: `.gitignore` lista `/src/main/resources/application.properties`, pero **el archivo ya está trackeado** (`git ls-files` lo confirma) — `.gitignore` no aplica a archivos ya versionados, así que la protección es ilusoria.

**Fix:**

1. Rotar la contraseña de `sa` (asumir comprometida).
2. Crear un login dedicado sin sysadmin — ver sección de DataSource.
3. Quitar los defaults: `spring.datasource.password=${DB_PASSWORD}` (sin `:valor`; así la app **no arranca** si falta la variable, que es lo correcto).
4. `git rm --cached src/main/resources/application.properties` y `git rm -r --cached bin/`; commitear el `.gitignore` que ya los lista.
5. Borrar `ClassGenerator.java` (código muerto, ver sección correspondiente).
6. El historial de git sigue conteniendo la clave: rotarla es obligatorio, limpiar el historial es opcional.

---

### C3 — `application.properties` versionado en "MODO PRUEBA", con el desvío de WhatsApp activo

`src/main/resources/application.properties:1-15,21,72,103`

El propio archivo abre con:

```
#  ATENCION: ESTE ARCHIVO ESTA EN MODO PRUEBA. NO COMMITEAR ASI.
```

y está commiteado así. Los tres valores peligrosos, tal como están hoy:

| Línea | Valor actual | Debería ser |
|---|---|---|
| 21 | `databaseName=BOSQUE-2_0` | (ya correcto — el comentario `[PRUEBA]` quedó desactualizado) |
| 51 | `openwa.url=http://181.114.119.195:2785` | `host.containers.internal:2785` |
| 72 | `openwa.entregas.telefono-prueba=78888274` | **vacío** |
| 103 | `openwa.entregas.pedir-calificacion=true` | `false` hasta registrar el webhook |

**Impacto (documentado en el propio archivo, líneas 10-14):** `telefono-prueba` gana sobre `notificar-cliente`. Con un número puesto, *todos* los avisos de *todos* los clientes van a ese celular y ningún cliente recibe nada. **No falla ruidoso.** Además `pedir-calificacion=true` con `webhook.habilitado=false` le pide al cliente que responda un número que nadie va a leer, y deja filas PENDIENTES para siempre.

**Fix:** los defaults del archivo versionado deben ser los de producción; el modo prueba se activa solo por variables de entorno.

```properties
openwa.entregas.telefono-prueba=${OPENWA_TEL_PRUEBA:}
openwa.entregas.pedir-calificacion=${OPENWA_PEDIR_CALIF:false}
openwa.url=${OPENWA_URL:http://host.containers.internal:2785}
```

Complemento: un check de arranque que aborte si `telefono-prueba` no está vacío y el perfil es `prod`.

---

### C4 — `this.jdbcTemplate = null` deja DAOs muertos hasta reiniciar

154 ocurrencias en 73 archivos de `dao/`. Ejemplo canónico `dao/ColorDao.java:44`:

```java
} catch (BadSqlGrammarException e) {
    System.out.println("Error: ColorDao en registrarColor, ...");
    this.jdbcTemplate = null;   // <-- anula el campo del bean singleton
    resp = 0;
}
```

Los DAOs son `@Repository` singleton con inyección por campo. Anular `jdbcTemplate` **destruye el bean para toda la aplicación**: cada llamada posterior a ese DAO tira `NullPointerException` hasta que se reinicie el proceso. Un solo error de sintaxis SQL transitorio (un SP recién alterado, un deploy a medias) apaga un módulo entero de forma permanente y silenciosa.

No es teórico — el código nuevo ya se defiende de esto:

- `dao/EntregaChoferDao.java:626-630`: `if (this.jdbcTemplate == null) { logger.error("lo anulo un metodo anterior del DAO"); return false; }`
- `commons/AccesoModuloHelper.java` (javadoc): *"`UsuarioBtnDao` se anula a sí mismo el `JdbcTemplate` en su `catch`: si alguna vez falla, el ACL devuelve lista vacía hasta que se reinicie la app."*

**Fix mecánico, aplicable en bloque:**

```java
} catch (BadSqlGrammarException e) {
    logger.error("ColorDao.registrarColor: error SQL ejecutando p_abm_Color", e);
    resp = 0;   // se borra la línea this.jdbcTemplate = null;
}
```

Buscar `jdbcTemplate = null` y eliminar las 154 líneas. No cambia ninguna semántica salvo la de no suicidar el bean. Después, agregar un test de arquitectura junto a `SinSqlCrudoTest` que falle el build si el patrón reaparece — el proyecto ya tiene ese mecanismo montado y funcionando.

---

## Hallazgos High

### H1 — El driver no es jTDS: es mssql-jdbc 9.4.0, fuera de soporte para SQL Server 2008

`pom.xml:35-39` · `application.properties:24` · verificado en `target/cp.txt`

```
mssql-jdbc\9.4.0.jre8\mssql-jdbc-9.4.0.jre8.jar
```

Dos problemas para el destino Fedora:

1. **Matriz de soporte.** La matriz oficial de mssql-jdbc 9.x arranca en SQL Server 2012. 2008/2008 R2 quedó fuera. Puede conectar hoy, pero sin garantía ni soporte.
2. **TLS (el bloqueante real).** SQL Server cifra el paquete de login **siempre**, incluso con `encrypt=false`. SQL Server 2008 sin el parche de TLS 1.2 (SP4 + KB3135244) solo ofrece TLS 1.0 con certificado autofirmado SHA1/1024 bits. Fedora aplica `crypto-policies` a nivel de sistema y Java 8 moderno deshabilita TLS 1.0/1.1 en `jdk.tls.disabledAlgorithms`. Resultado esperado en producción: `The driver could not establish a secure connection to SQL Server by using Secure Sockets Layer (SSL)`.

En Windows con Java 8u231 (el JRE que hay en la máquina de desarrollo, de octubre 2019) esto no se ve, porque la política de crypto es otra. **Es un fallo que aparece recién en el despliegue.**

Ver la sección de DataSource/jTDS para la configuración concreta y los riesgos de la migración.

---

### H2 — Rate limiting y bloqueo por IP se evaden con un header

`security/RateLimitFilter.java:78-84` · `security/jwt/JwtEntryPoint.java:107-113` · `security/SecurityFilter.java:93-99`

Los tres filtros derivan la IP igual:

```java
String xfHeader = request.getHeader("X-Forwarded-For");
if (xfHeader == null) return request.getRemoteAddr();
return xfHeader.split(",")[0];
```

`X-Forwarded-For` lo pone el cliente. Sin una lista de proxies de confianza, el atacante manda un valor distinto en cada request y:

- **Evade el rate limit de `/auth/login`** (5/min) — fuerza bruta sin límite.
- **Evade el bloqueo progresivo** de `JwtEntryPoint` (1/5/30 min).
- **Envenena los logs** de seguridad con IPs falsas.

Agravante de memoria: `JwtEntryPoint.cleanup()` (líneas 97-102) limpia solo `blockUntilTime`. **`failureCounters` no se limpia nunca** y su clave la controla el atacante → crecimiento sin techo hasta OOM. Lo mismo con `RateLimitFilter.buckets` (línea 25), que no tiene limpieza alguna.

**Fix:**

```java
// Solo confiar en XFF si el request viene de un proxy conocido.
private static final Set<String> PROXIES_CONFIABLES =
        new HashSet<>(Arrays.asList("127.0.0.1", "10.0.0.5"));  // ajustar

private String getClientIP(HttpServletRequest request) {
    String remoto = request.getRemoteAddr();
    if (!PROXIES_CONFIABLES.contains(remoto)) {
        return remoto;                       // conexión directa: XFF se ignora
    }
    String xf = request.getHeader("X-Forwarded-For");
    return (xf == null || xf.trim().isEmpty()) ? remoto : xf.split(",")[0].trim();
}
```

Si no hay proxy delante, usar `request.getRemoteAddr()` a secas. Y agregar `failureCounters.entrySet().removeIf(...)` al `cleanup()` con una marca de tiempo por entrada.

---

### H3 — Cero validación de entrada: 0 `@Valid` en 452 endpoints

`controller/` (todos)

Ningún `@Valid` ni `@Validated` en todo el paquete, pese a que `spring-boot-starter-validation` está en el `pom.xml:90-94`. En `LoginController.java:69` hay un `BindingResult` que **nunca puede tener errores** porque el `@RequestBody` que lo precede no está anotado con `@Valid`:

```java
public ResponseEntity<?> login(@RequestBody Login login, BindingResult bindingResult) {
    if (bindingResult.hasErrors()) { ... }   // rama muerta
```

Toda validación queda delegada al SP. Los strings sin límite de longitud llegan al driver y fallan como `DataAccessException` → 500 genérico en vez de 400 con mensaje útil.

**Fix incremental** (no hace falta tocar los 452 de una vez): empezar por los modelos del módulo tpex y de RRHH.

```java
public class SolicitudPago implements Serializable {
    @NotNull private Long codEmpresa;
    @Size(max = 200) private String glosa;
    @DecimalMin("0.01") private BigDecimal monto;
}
```

```java
@PostMapping("/registrar-solicitud")
public ResponseEntity<ApiResponse<?>> registrar(@Valid @RequestBody SolicitudPago sp) { ... }
```

Y un handler en `GlobalExceptionHandler` para `MethodArgumentNotValidException` → 400 con los campos que fallaron.

---

### H4 — CORS abierto a `*` en 27 controllers

27 archivos con `@CrossOrigin(origins = "*")`; `LoginController.java:38` además con `allowedHeaders = "*"`.

`MainSecurity.java:79` hace `http.cors()` **sin declarar ningún `CorsConfigurationSource`**, así que Spring cae en la fuente basada en las anotaciones `@CrossOrigin`. Es decir: la política efectiva es "cualquier origen".

Cualquier sitio web puede invocar la API desde el navegador de un usuario y **leer la respuesta**. Como el JWT viaja en header y no en cookie, no hay CSRF clásico, pero sí exfiltración desde cualquier página que consiga un token (XSS en el SPA, extensión maliciosa, token pegado en un sitio de terceros).

**Fix:** un solo bean centralizado, y borrar las 27 anotaciones.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource(
        @Value("${cors.origenes}") String origenes) {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(Arrays.asList(origenes.split(",")));
    c.setAllowedMethods(Arrays.asList("GET", "POST"));
    c.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", c);
    return src;
}
```

```properties
cors.origenes=${CORS_ORIGENES:http://192.168.3.x,https://erp.bosque.com.bo}
```

---

### H5 — IDOR: `/view/vistaBtn` lee los permisos de cualquier usuario

`controller/VistaController.java:86-96`

```java
@Secured({ "ROLE_ADM", "ROLE_LIM" })
@PostMapping("/vistaBtn")
public List<UsuarioBtn> obtenerPermisosBotones(@RequestBody Login obj) {
    return this.uDao.botonesXUsuario(obj.getCodUsuario());   // codUsuario del BODY
}
```

El `codUsuario` viene del JSON, no del token. Cualquier usuario autenticado (incluido `ROLE_LIM`) enumera el ACL completo de los 134 usuarios pasando códigos. Es reconocimiento previo a una escalada de privilegios.

Ya está identificado en el javadoc de `commons/AccesoModuloHelper.java:150-152` como *"el precedente malo"*.

**Fix:** sacar el `codUsuario` del `Authentication`, igual que hace `AccesoModuloHelper`:

```java
@PostMapping("/vistaBtn")
public List<UsuarioBtn> obtenerPermisosBotones(Authentication auth) {
    return this.uDao.botonesXUsuario(AccesoModuloHelper.codUsuarioDe(auth));
}
```

Coordinar con el frontend: el body deja de usarse.

---

### H6 — IDOR en vouchers + `audUsuario` provisto por el cliente

`controller/PagosExtranjerosController.java:1001,1066-1077`

Dos problemas en el mismo par de endpoints:

**a) Descarga sin control de pertenencia.** `GET /pagos-extranjeros/transacciones/{idTransaccion}/voucher` solo comprueba que la transacción tenga voucher. `codEmpresa` es un `@RequestParam` opcional con default `0` — lo elige el cliente. Cualquier usuario autenticado itera `idTransaccion` y baja todos los comprobantes bancarios de todas las empresas.

**b) `audUsuario` viene del request:**

```java
public ResponseEntity<ApiResponse<?>> subirVoucher(@PathVariable long idTransaccion,
        @RequestParam("file") MultipartFile file,
        @RequestParam("audUsuario") int audUsuario) {
```

Igual en `TigoController.java:101`. Y por `@RequestBody` el patrón se repite en casi todos los ABM, porque los modelos llevan `audUsuario` como campo. **La pista de auditoría es falsificable**: cualquiera registra una operación a nombre de otro. En un módulo de pagos al exterior eso invalida la trazabilidad completa.

Solo 3 lugares en todo `controller/` consultan al usuario autenticado.

**Fix:** tomar `audUsuario` del token y pisar lo que venga del cliente.

```java
@PostMapping(value = "/transacciones/{idTransaccion}/voucher", consumes = MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<ApiResponse<?>> subirVoucher(@PathVariable long idTransaccion,
        @RequestParam("file") MultipartFile file,
        Authentication auth) {
    int audUsuario = AccesoModuloHelper.codUsuarioDe(auth);   // ya no se recibe
    ...
}
```

Para (a): agregar una ACCION de validación al SP de transacciones que confirme que la transacción pertenece a una empresa del usuario, o filtrar por el `codEmpresa` del token.

---

### H7 — La contraseña en claro viaja al SP de login

`dao/LoginDaoImpl.java:80-87`

```java
public Login verifyUser(String login, String password2, String ip) {
    ... this.jdbcTemplate.query(
            "execute p_list_Usuario @login=?, @password2=?, @ip=?, @ACCION=?",
            new Object[] { login, password2, ip, "C" }, ...
```

La contraseña en texto plano se manda al motor. Consecuencias:

- Queda expuesta en trazas de SQL Profiler, en el plan cache y en cualquier auditoría del motor.
- Con TLS 1.0 / login-packet-only (ver H1), viaja mal protegida en la red.
- Implica que el SP la compara contra algo — si `tb_usuario` guarda la contraseña en claro o con un hash débil de SQL (`PWDCOMPARE`, `HASHBYTES('MD5')`), es un problema mayor. **Verificar el SP `p_list_Usuario @ACCION='C'` y el esquema de `tb_usuario`.**

Además hay **dos caminos de autenticación** conviviendo: este SP y el `authenticationManager` con `BCryptPasswordEncoder` (`MainSecurity.java:61-68`, `LoginController.java:120-123`). Si el SP acepta credenciales que BCrypt rechazaría (o al revés), la política real de contraseñas es la más laxa de las dos.

**Fix:** que `verifyUser` reciba solo `login` e `ip` (bloqueos, bitácora, estado de cuenta) y que la verificación de la contraseña quede exclusivamente en `authenticationManager` contra el hash BCrypt. Si `tb_usuario` no tiene hashes BCrypt, migrar con un campo nuevo y doble verificación transitoria.

---

### H8 — Rutas relativas de `uploads/`: rompen o se desvían en Linux

`commons/service/FileStorageService.java:25,31-39,105,119` · `commons/config/FileUploadConfig.java`

```java
private final Path root = Paths.get("uploads/depositos");

public void init() {
    if (!Files.exists(root)) {
        Files.createDirectory(root);      // NO createDirectories
    }
}
```

Dos fallos concretos en Fedora:

1. **`createDirectory` (singular) falla si `uploads/` no existe.** En un despliegue limpio tira `NoSuchFileException` → `RuntimeException` en el constructor del bean → **la aplicación no arranca**.
2. **Todas las rutas son relativas al directorio de trabajo del proceso.** En Windows se corre desde la raíz del proyecto y funciona. Bajo systemd, el `WorkingDirectory` por defecto es `/`, así que los archivos irían a `/uploads/...` (sin permisos) o a un lugar distinto en cada arranque. Los vouchers de pagos al exterior y las fotos de la ficha del trabajador quedan colgados.

Nota relacionada: `commons/JasperReportExport.java:251-253` ya tiene un default `/app/uploads/` hardcodeado para cuando la propiedad no está inyectada — señal de que el problema ya apareció.

**Fix:**

```java
@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${app.uploads.dir}") String base) throws IOException {
        this.root = Paths.get(base).toAbsolutePath().normalize();
        Files.createDirectories(this.root.resolve("depositos"));
    }
}
```

```properties
app.uploads.dir=${UPLOADS_DIR:./uploads}
```

Y en la unidad systemd: `WorkingDirectory=/opt/bosque` + `Environment=UPLOADS_DIR=/var/lib/bosque/uploads`.

Complemento: `guardarVoucher(archivo, rutaRelativa)` hace `Paths.get("uploads").resolve(rutaRelativa)` sin normalizar. Hoy no es explotable porque los dos llamadores construyen la ruta en el servidor (`PagosExtranjerosController.java:1016`), pero conviene blindarlo:

```java
Path destino = root.resolve(rutaRelativa).normalize();
if (!destino.startsWith(root)) {
    throw new IOException("Ruta fuera de uploads: " + rutaRelativa);
}
```

---

### H9 — Los cron corren en la zona horaria del sistema operativo

`scheduler/DatabaseTaskScheduler.java:53,68` y demás `@Scheduled`

```java
@Scheduled(cron = "59 58 23 * * *")   // cierre diario de entregas
@Scheduled(cron = "0 0 8 * * *")      // aviso de cumpleaños
```

Sin `zone`, Spring usa la zona por defecto de la JVM. `application.properties:27` configura `spring.jackson.time-zone=America/La_Paz`, pero eso solo afecta a la serialización JSON — **no al scheduler**.

Fedora servidor suele estar en UTC. Bolivia es UTC-4, así que el cierre de las 23:58:59 se ejecutaría a las **19:58:59 hora local**: cierra entregas del día en curso, y el aviso de cumpleaños sale a las 04:00.

**Fix:**

```java
@Scheduled(cron = "59 58 23 * * *", zone = "America/La_Paz")
@Scheduled(cron = "0 0 8 * * *",   zone = "America/La_Paz")
```

Y en la unidad systemd, `Environment=TZ=America/La_Paz` como red de seguridad. Ojo también con `LocalDate.now()` en `notificarCumpleaniosDelDia` (línea 78) — depende de la misma zona por defecto.

---

## Hallazgos Medium

### M1 — El WAF casero genera falsos positivos y consume el body

`security/SecurityFilter.java:30-34,58`

```java
Pattern.compile(".*\\b(union|select|from|where|drop|--|--)\\b.*", CASE_INSENSITIVE),
Pattern.compile(".*['\"].*", CASE_INSENSITIVE),
```

- El segundo patrón rechaza **cualquier parámetro que contenga un apóstrofo o comilla**: apellidos como `D'Angelo`, `O'Connor`, glosas con comillas. Devuelve 403 sin explicación.
- El primero bloquea palabras comunes en español/inglés: `from`, `where`, y `drop` aparecen en texto libre.
- `--` está duplicado en la alternancia.
- Línea 58, `request.getParameterMap()`: en un POST `application/x-www-form-urlencoded` esto **consume el body** de Tomcat, y el `@RequestBody` del controller llega vacío. El propio `WhatsAppWebhookController.java:145-148` documenta esta trampa.
- Como toda la persistencia usa `PreparedStatement` con parámetros, este filtro no aporta defensa real contra SQLi. Da falsa sensación de seguridad a costa de romper entradas legítimas.

**Fix:** eliminar los patrones de contenido y quedarse solo con la lista de User-Agents. La protección efectiva contra SQLi ya existe: parámetros vinculados y nombres de SP literales.

### M2 — Sin configuración del pool ni timeouts

HikariCP 4.0.3 con todo por defecto: 10 conexiones, sin `max-lifetime` ajustado, sin `connection-test-query`, sin `queryTimeout` global. Un SP lento cuelga un hilo de Tomcat indefinidamente. Solo `SincronizacionEntregasService` acota su propio timeout (`EntregaChoferDao.java:637`), y lo hace creando un `JdbcTemplate` aparte — señal de que falta política global. Configuración concreta en la sección de DataSource.

### M3 — 295 `System.out.println` y stack traces a stdout

En `src/main/java`, incluidos caminos de error de `LoginDaoImpl.java:125,130,245,249`. No pasan por SLF4J: sin nivel, sin timestamp, sin rotación, invisibles para `logging.level`. Bajo systemd van todos a journald como INFO. Además `application.properties:43` deja `logging.level.bo.bosque.com.impexpap=DEBUG`, lo que en producción incluye `SpHelper.java:81` (nombres de SP y claves de parámetros en cada request). Reemplazar por `logger.error(msg, e)` y bajar a INFO en producción.

### M4 — Dependencias sin usar, algunas EOL

`pom.xml` — verificado por ausencia de imports en `src/main/java`:

| Dependencia | Líneas | Estado |
|---|---|---|
| `spring-security-oauth2` 2.5.1.RELEASE | 50-54 | 0 usos. Proyecto EOL desde 2022. |
| `spring-security-jwt` 1.1.1.RELEASE | 62-66 | 0 usos. EOL. Se usa jjwt. |
| `commons-lang3` 3.9 | 77-81 | 0 usos. |
| `jaxb-api` / `jaxb-core` / `jaxb-impl` | 114-128 | 0 usos. JAXB viene en el JDK 8; esto es un workaround de Java 11. |
| `barcode4j` 2.1 | 145-149 | 0 usos, y ningún `.jrxml` usa componentes Barcode4J. |
| `batik-all` 1.14 | **150-159, declarada dos veces** | Duplicado literal. Ningún reporte usa SVG. |
| `spring-boot-starter-data-jdbc` | 20-23 | Arrastra Spring Data JDBC (mapeo objeto-relacional) cuando solo se usa `JdbcTemplate`. Contradice la regla de "sin ORM". |

**No tocar** `zxing`: `reports/RptFichaTrabajador.jrxml:700` tiene un `<jr:QRCode>`, que JasperReports renderiza con zxing en tiempo de ejecución aunque no haya import en Java.

Cambio sugerido: reemplazar `spring-boot-starter-data-jdbc` por `spring-boot-starter-jdbc` y borrar las siete filas restantes de la tabla. Verificar con `mvn dependency:analyze` antes de aplicar.

### M5 — Enumeración de usuarios en el login

`controller/LoginController.java:105-116`: respuestas distinguibles entre "El usuario ingresado no existe" (401), "Su cuenta ha sido bloqueada" (403) y credenciales inválidas. Permite enumerar los 134 usuarios. Unificar a un único 401 con mensaje genérico; el detalle, al log.

### M6 — `jjwt` 0.9.1 y Java 8u231, ambos de 2018-2019

`pom.xml:67-71` y el JRE instalado (`1.8.0_231`, octubre 2019). jjwt 0.9.1 es la última de la rama vieja; la API `signWith(SignatureAlgorithm, String)` que usa `JwtProvider` está deprecada precisamente porque acepta secretos débiles. Actualizar el JDK al último 8u disponible es especialmente relevante para la migración a Fedora (correcciones de TLS y de política criptográfica). No implica pasar a Java 9+.

### M7 — `header.replace("Bearer ", "")` reemplaza todas las ocurrencias

`security/jwt/JwtTokenFilter.java:55`. Un token que contuviera la subcadena se corrompería, y `"Bearer"` sin espacio pasa el `startsWith` y no se limpia. Usar `header.substring(7).trim()` tras verificar `startsWith("Bearer ")`.

### M8 — `ClassGenerator` abre conexiones fuera del pool

`utils/ClassGenerator.java:126` usa `DriverManager.getConnection` directo con credenciales propias. Es una herramienta de desarrollo que quedó en `src/main`, se empaqueta en el jar de producción y `.gitignore:37-38` la re-admite explícitamente al repo. Ver código muerto.

---

## Hallazgos Low

- **L1** — `.svn/` y `.git/` conviven en la raíz. Dos sistemas de control de versiones sobre el mismo árbol; el `.svn/pristine` duplica el contenido completo del proyecto en disco.
- **L2** — `.github/copilot-instructions.md` está vacío (0 bytes).
- **L3** — `pom.txt` (copia del `pom.xml`) y `controller/LoginControllerBackup.txt` son respaldos manuales; para eso está git.
- **L4** — 10 archivos versionados bajo `uploads/`: `172.jpg`, `20.jpg`, `32.jpg`, `95.jpg`, `deposito_2627.jpg`, `deposito_56.png`, `australia.JPG`. Los numerados coinciden con el patrón `codEmpleado.jpg` de la ficha del trabajador: posibles fotos de empleados reales en el repo. Revisar y sacar del control de versiones. `australia.JPG` en mayúsculas es además un riesgo de case-sensitivity en Linux.
- **L5** — `security/jwt/JwtEntryPoint.java:49`: `res.setStatus(SC_UNAUTHORIZED); // 429`. El comentario dice 429 y el código manda 401, con `Retry-After`. Debería ser `SC_TOO_MANY_REQUESTS`.
- **L6** — `JwtEntryPoint.java:26`: el campo `buckets` nunca se usa (junto con el import de bucket4j).
- **L7** — `itext7-core` 7.2.5 es AGPL: en un producto interno cerrado exige licencia comercial. Verificar con el área legal.
- **L8** — `dao/ColorDao.java:33-40`: la sentencia declara 4 marcadores (`@idColor, @color, @estado, @audUsuario`) pero el lambda hace `ps.setString(5, acc)`. Falta `@ACCION=?`. El método falla siempre. Es código muerto, así que nunca se notó — pero es señal de que ese DAO nunca se ejecutó.
- **L9** — Sin `.gitattributes`. Con Windows en desarrollo y Linux en producción, los finales de línea de `mvnw` (script sh) pueden llegar con CRLF y fallar con `bad interpreter`. Agregar `mvnw text eol=lf`.

---

## Código muerto / archivos basura detectados

### Directorio `bin/` — 375 archivos, prioridad máxima

Árbol completo del proyecto duplicado y versionado: `.class` compilados, `maven-wrapper.jar`, `mvnw`, `pom.xml`, `.idea/` y **`bin/src/main/resources/application.properties` con `sa`/`sapbus1n3ss` en claro**. Confunde cualquier búsqueda de código y filtra credenciales.

```bash
git rm -r --cached bin/
rm -rf bin/
echo "bin/" >> .gitignore
```

### DAOs sin ningún punto de inyección (12 pares interfaz + implementación)

Verificado: cada interfaz aparece solo en su propio archivo y en su implementación; ninguna se inyecta en controllers, commons, security, scheduler ni tests.

| Interfaz | Implementación |
|---|---|
| `IArticulo` | `ArticuloDao` |
| `IChAccion` | `ChAccionDAO` |
| `IClasificacionPrecio` | `ClasificacionPrecioDao` |
| `IColor` | `ColorDao` |
| `IGrupoFamiliaSap` | `GrupoFamiliaDao` |
| `IGrupoFamTipoRangoGram` | `GrupoFamTipoRangoGramDao` |
| `IPorcentaje` | `PorcentajeDao` |
| `IPresentacion` | `PresentacionDao` |
| `IProveedorExtSap` | `ProveedorExtSapDao` |
| `IRangoGramaje` | `RangoGramajeDao` |
| `ITipo` | `TipoDao` |
| `IDocumentoDao` | `DocumentoDaoImpl` (clase vacía) |

**Antes de borrar `ColorDao`:** está citado con `{@link ColorDao}` en `dao/VacacionAsignadaDao.java:30` y mencionado en `utils/SpEscritura.java:19` como el ejemplo canónico del patrón legacy. El `{@link}` rompe el javadoc si la clase desaparece. Cambiar esas dos referencias a texto plano o apuntarlas a otro DAO legacy que se conserve.

`FeriadoDao` / `IDiaNoLaborable` **sí se usa** (`VacacionController`) — no borrar, pese a que la interfaz parezca huérfana en una primera pasada.

### Clases stub vacías (cuerpo `{ }`)

`controller/ChChequeController`, `dao/ChChequeDAO`, `dao/IChCheque`, `dao/ArticuloPrecioDisponibleEPPDao`, `dao/IArticuloPrecioDisponibleEPP`, `dao/IKilometraje`, `dao/DocumentoDaoImpl`, `dao/IDocumentoDao`.

### Modelos sin ninguna referencia

`ArticuloPrecioDisponibleEPP`, `ChCheque`, `Horometro`, `Kilometraje`, `MovCajaAxa`, `MovCajaAxaEgresos`, `MovCajaAxaIngresos`, `TarRuXCargo`, `VistaBtn`.

*Posible código muerto — verificar con búsqueda de referencias*: un modelo podría usarse solo como `Clase.class` en `ejecutarListado`, aunque la búsqueda por palabra completa ya cubriría ese caso.

### Otros

- `utils/ClassGenerator.java` + `utils/Column.java` — generador de POJOs, herramienta de desarrollo con credenciales embebidas, 0 referencias. Si se quiere conservar, mover a `src/test/java` o a un módulo aparte para que no se empaquete en el jar de producción, y sacarle las credenciales.
- `security/SecurityAlertService.java` — 0 referencias.
- `pom.txt`, `controller/LoginControllerBackup.txt`, `.github/copilot-instructions.md` (vacío), `.idea2020/`, `.svn/`.
- Código comentado grande: `dao/ChAccionDAO.java` tiene bloques comentados con `ps.executeQuery()` en las líneas 270, 311, 348, 382, 456, 492, 533 — y el DAO entero es código muerto.

**Efecto estimado:** eliminando `bin/` y los 12 pares de DAOs se van unos 400 archivos, cerca del 45% de los que sigue git.

---

## Secrets y configuración sensible

| Archivo | Línea | Secreto | Acción |
|---|---|---|---|
| `security/jwt/JwtConfig.java` | 13-40 | Clave privada RSA, usada como secreto HMAC | Borrar la clase. `jwt.secret` por env. **Rotar.** |
| `src/main/resources/application.properties` | 23 | `DB_PASSWORD:sapbus1n3ss` (default = clave real de `sa`) | Quitar el default. **Rotar.** |
| `src/main/resources/application.properties` | 52 | `OPENWA_SESSION_ID:58812227-…` | Solo por env. |
| `src/main/resources/application.properties` | 53 | `OPENWA_API_KEY:dev-admin-key` | Solo por env. **Rotar.** |
| `src/main/resources/application.properties` | 21 | IP y nombre de la base de producción | Externalizar la URL entera. |
| `utils/ClassGenerator.java` | 16-19 | `sa` / `sapbus1n3ss` en literales | Borrar la clase. |
| `bin/src/main/resources/application.properties` | 4-5 | `sa` / `sapbus1n3ss` **sin envoltura** | Borrar `bin/` completo. |
| `controller/WhatsAppWebhookController.java` | 64-77 | `X-API-Key: dev-admin-key` y session-id en ejemplos de javadoc | Sustituir por marcadores. |

**El `.gitignore` no está protegiendo nada.** Lista `/pom.xml` y `/src/main/resources/application.properties` (líneas 36-38), pero ambos ya están trackeados y `.gitignore` no aplica a archivos versionados. Hay que hacer `git rm --cached` explícito.

### Cómo externalizar sin herramientas incompatibles con Java 8

Nada de Vault ni Spring Cloud Config. Con Spring Boot 2.6.1 y systemd alcanza:

**1. Archivo de entorno fuera del repo:**

```bash
# /etc/bosque/bosque.env   (chown root:bosque, chmod 640)
DB_USERNAME=bosque_app
DB_PASSWORD=<clave rotada>
JWT_SECRET=<openssl rand -base64 64>
OPENWA_API_KEY=<clave rotada>
OPENWA_SESSION_ID=<uuid>
OPENWA_WEBHOOK_SECRET=<openssl rand -hex 32>
UPLOADS_DIR=/var/lib/bosque/uploads
CORS_ORIGENES=https://erp.bosque.com.bo
TZ=America/La_Paz
```

**2. Unidad systemd:**

```ini
[Unit]
Description=Bosque ERP Backend
After=network-online.target

[Service]
Type=simple
User=bosque
WorkingDirectory=/opt/bosque
EnvironmentFile=/etc/bosque/bosque.env
ExecStart=/usr/lib/jvm/jre-1.8.0/bin/java \
  -Djava.security.properties=/etc/bosque/java.security.override \
  -Duser.timezone=America/La_Paz \
  -Xms512m -Xmx2g \
  -jar /opt/bosque/bosque-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**3. Sin defaults en el `.properties`.** `${DB_PASSWORD}` sin `:valor` hace que la app falle al arrancar si la variable no está: mejor un arranque fallido y visible que uno silencioso contra la base equivocada.

**4. Perfiles.** `application.properties` con lo común, `application-dev.properties` (fuera del repo o con valores inocuos) y `application-prod.properties` solo con referencias a variables. Activar con `SPRING_PROFILES_ACTIVE=prod`.

---

## Recomendaciones de DataSource / jTDS para producción Linux + SQL Server 2008

Punto de partida: hoy el proyecto usa **mssql-jdbc 9.4.0.jre8**, no jTDS. Este es el cambio.

### 1. Reemplazo de la dependencia

```xml
<!-- QUITAR -->
<!--
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
-->

<!-- AGREGAR -->
<dependency>
    <groupId>net.sourceforge.jtds</groupId>
    <artifactId>jtds</artifactId>
    <version>1.3.1</version>
</dependency>
```

jTDS 1.3.1 es la última versión (2013), compatible con Java 8 y con SQL Server 2008. No recibe mantenimiento: es una decisión de compatibilidad, no de modernización. Está bien para este escenario.

### 2. Configuración del DataSource

```properties
# ── DataSource (jTDS) ────────────────────────────────────────────────────────
# Ojo con la sintaxis: jTDS usa /BASE, no ;databaseName=
spring.datasource.driver-class-name=net.sourceforge.jtds.jdbc.Driver
spring.datasource.url=jdbc:jtds:sqlserver://${DB_HOST}:${DB_PORT:1433}/${DB_NAME};\
loginTimeout=10;\
socketTimeout=120;\
prepareSQL=1;\
sendStringParametersAsUnicode=true;\
useCursors=false;\
appName=BosqueBackend
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# ── Pool (hoy todo por defecto) ──────────────────────────────────────────────
spring.datasource.hikari.pool-name=bosque-pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=900000
# jTDS 1.3.1 no implementa Connection.isValid() de forma confiable:
# sin esto Hikari avisa y valida mal.
spring.datasource.hikari.connection-test-query=SELECT 1
```

Notas sobre cada propiedad que importa:

- **`prepareSQL=1`** (`sp_executesql`) en lugar del default `3` (`sp_prepare` con procedimientos temporales). Es lo relevante para `SpHelper.ejecutarAbmMap`, que manda un lote multi-sentencia `DECLARE @__error INT … EXEC … SELECT`. Con `prepareSQL=3` ese lote puede fallar. Si aun así falla, probar `prepareSQL=0` (SQL plano, sin preparación).
- **`socketTimeout=120`** (segundos): corta consultas colgadas a nivel socket. Hoy no existe ningún timeout global — solo `SincronizacionEntregasService` pone el suyo.
- **`sendStringParametersAsUnicode=true`** es el default y hay que dejarlo así. Ponerlo en `false` mejora el uso de índices sobre columnas `varchar`, pero rompe los acentos. No cambiarlo sin medir.
- **`charset`: no la configures.** Sin esa propiedad, jTDS usa la colación del servidor, que es lo correcto. Forzar `charset=UTF-8` contra una base con colación `SQL_Latin1_General` corrompe los acentos.
- **`useCursors=false`**: el default; los listados de este proyecto son de tamaño acotado y traerlos completos es más rápido.

### 3. TLS: lo que realmente va a fallar en Fedora

SQL Server cifra el paquete de login **siempre**, aunque `encrypt=false`. SQL Server 2008 sin parchear solo ofrece TLS 1.0 con certificado autofirmado SHA1/1024 bits. Fedora aplica `crypto-policies` a todo el sistema, y Java 8 moderno deshabilita TLS 1.0/1.1 en `jdk.tls.disabledAlgorithms`. Síntoma típico: `SSL handshake failed` o `algorithm constraints check failed`.

Tres caminos, en orden de preferencia:

**a) Parchear el motor (lo correcto).** SQL Server 2008 SP4 (o 2008 R2 SP3) + la actualización de TLS 1.2 (KB3135244). Elimina el problema de raíz y no requiere debilitar nada del cliente.

**b) Excepción acotada a esta aplicación** (si el motor no se puede tocar):

```properties
# /etc/bosque/java.security.override
jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, DH keySize < 768
jdk.certpath.disabledAlgorithms=MD2, MD5, RSA keySize < 1024
```

Se aplica con `-Djava.security.properties=/etc/bosque/java.security.override` (ya está en la unidad systemd de más arriba). **Un solo `=` sobrescribe propiedades puntuales; `==` reemplazaría el archivo entero.** Ventaja sobre editar `java.security` del JDK: no se pierde en una actualización y no afecta a otras aplicaciones Java del servidor.

**c) `update-crypto-policies --set LEGACY`** — funciona, pero debilita TLS en **todo** el sistema (ssh, httpd, curl). Último recurso.

Comprobar el estado con `update-crypto-policies --show` antes de desplegar.

### 4. Cuenta de base de datos: dejar de usar `sa`

```sql
CREATE LOGIN bosque_app WITH PASSWORD = '<clave fuerte>',
    CHECK_POLICY = ON, DEFAULT_DATABASE = [BOSQUE-2_0];
GO
USE [BOSQUE-2_0];
CREATE USER bosque_app FOR LOGIN bosque_app;
GO

-- Ejecutar los SPs es todo lo que necesita la aplicación
GRANT EXECUTE ON SCHEMA::dbo TO bosque_app;

-- IMPRESCINDIBLE: SpHelper.ejecutarAbm usa SimpleJdbcCall, que lee los metadatos
-- de los parámetros del procedimiento. Sin VIEW DEFINITION falla con
-- "Unable to determine the correct call signature".
GRANT VIEW DEFINITION ON SCHEMA::dbo TO bosque_app;

-- Nada de db_owner, db_datareader ni db_datawriter: no se accede a tablas
-- directamente, y así una inyección hipotética no llega a ninguna tabla.
GO
```

Si algún SP usa el linked server `SRV_2022` (contactos SAP, sincronización de entregas), verificar que el mapeo de login del linked server incluya a `bosque_app`.

### 5. Riesgos concretos del cambio de driver — probar antes de desplegar

Este proyecto depende de comportamientos específicos del driver en cuatro lugares. Cada uno merece una prueba explícita contra la base real:

1. **`SpHelper.ejecutarAbm` (`SpHelper.java:134-171`)** usa `SimpleJdbcCall`, que lee metadatos vía `DatabaseMetaData.getProcedureColumns`. jTDS y mssql-jdbc difieren en cómo devuelven los nombres de parámetro. Probar un ABM de cada era (`p_abm_tpex_Transacciones` y uno legacy).
2. **`SpHelper.ejecutarAbmMap` (`SpHelper.java:61-129`)** hace `ps.setObject(i+1, valor)` — **y esos mapas llevan `null` con frecuencia**. jTDS es menos tolerante que mssql-jdbc con `setObject(idx, null)`. Si aparece un error de tipo, la solución es explícita:

   ```java
   Object v = inputValues.get(i);
   if (v == null) { ps.setNull(i + 1, java.sql.Types.NULL); }
   else           { ps.setObject(i + 1, v); }
   ```

3. **Recorrido de múltiples ResultSets.** Tanto `ejecutarAbmMap` (líneas 91-98) como `EntregaChoferDao.sincronizarConSap` (líneas 644-670) iteran con `getMoreResults()` / `getUpdateCount()`. Ese bucle es sensible al driver. Los dos ya están escritos defensivamente, lo que ayuda, pero hay que ejecutarlos.
4. **Tipos de fecha.** El proyecto **ya tiene un test para esto**: `dao/FechaDelDriverTest` existe porque `tb_relEmplEmpr.fechaIni` es `date` (no `datetime`) y unos drivers devuelven `java.sql.Date` y otros el texto crudo — lo que provocó un `ClassCastException` en producción. `PermisoDao.aFecha` ya cubre ambos casos, y los 5 tests pasan. Es exactamente la clase de diferencia que hay que revalidar con jTDS.

**Plan de validación sugerido:** cambiar el driver en una rama, apuntar a `BOSQUE2PRUEBA`, y recorrer un ABM de cada era, un listado con `BeanPropertyRowMapper`, un listado dinámico, la sincronización con SAP, y un reporte Jasper (que toma su propia `Connection` del pool en `JasperReportExport.java:270,329`).

---

## Plan de acción priorizado

### Bloque 0 — Contención inmediata (horas, sin desplegar código)

1. **Rotar tres secretos**: contraseña de `sa`, `OPENWA_API_KEY`, y planificar la rotación de la clave JWT.
2. **Revertir el modo prueba** de `application.properties`: `telefono-prueba` vacío, `pedir-calificacion=false`. Verificar si el estado actual ya llegó a producción y, si es así, cuántos avisos se desviaron.
3. **Sacar del repo** `bin/`, `ClassGenerator.java` y `application.properties` (`git rm --cached`).

### Bloque 1 — Critical (1-2 semanas)

4. **C1** — Secreto JWT por variable de entorno; borrar `JwtConfig`. Coordinar la invalidación de tokens con frontend y app Flutter.
5. **C2** — Login `bosque_app` sin sysadmin; todos los secretos por `EnvironmentFile`; sin defaults en el `.properties`.
6. **C4** — Eliminar las 154 líneas `jdbcTemplate = null` y agregar el test de arquitectura que impida su regreso (el andamiaje de `SinSqlCrudoTest` ya existe).
7. **C3** — Perfiles `dev`/`prod` y un check de arranque que aborte si en `prod` hay valores de prueba activos.

### Bloque 2 — Bloqueantes de la migración a Linux (2-3 semanas, en paralelo)

8. **H1** — Migrar a jTDS 1.3.1 con la configuración de arriba, y ejecutar las cuatro pruebas de la sección 5 contra `BOSQUE2PRUEBA`.
9. **H8** — `uploads/` por ruta absoluta configurable + `createDirectories`.
10. **H9** — `zone = "America/La_Paz"` en todos los `@Scheduled`, y `TZ` en la unidad systemd.
11. **M2** — Configuración explícita del pool y de los timeouts.
12. Decidir la vía de TLS: parchear SQL Server 2008 (preferido) o el `java.security.override` acotado.

### Bloque 3 — High de seguridad (3-4 semanas)

13. **H2** — IP real solo desde proxies de confianza; limpiar `failureCounters` y `buckets`.
14. **H5**, **H6** — Cerrar los dos IDOR; `audUsuario` desde el token en todos los ABM.
15. **H4** — `CorsConfigurationSource` único; borrar las 27 anotaciones `@CrossOrigin`.
16. **H7** — Sacar la contraseña del SP; **antes**, auditar cómo guarda `tb_usuario` las contraseñas.
17. **H3** — `@Valid` progresivo: primero tpex y RRHH, después el resto.

### Bloque 4 — Limpieza y calidad (continuo)

18. Borrar los 12 pares de DAOs muertos, los stubs vacíos y los 9 modelos huérfanos (ajustar antes los dos `{@link ColorDao}`).
19. **M4** — Podar el `pom.xml`; validar con `mvn dependency:analyze`. No tocar zxing.
20. **M1** — Simplificar `SecurityFilter` a la lista de User-Agents.
21. **M3** — Reemplazar los 295 `System.out.println` por SLF4J; bajar a INFO en producción.
22. **M6** — Actualizar al último Java 8u.
23. **L1-L9** — Basura del repo, `.gitattributes`, licencia de iText.

---

## Notas de cierre

**Verificado durante la auditoría:** `mvnw test` → BUILD SUCCESS, 102 tests, 0 fallos (JDK 1.8.0_231).

**Dato de entorno:** `JAVA_HOME` no está configurado en la máquina de desarrollo — el JDK está en `C:\Program Files\Java\jdk1.8.0_231` y hay que exportarlo para que `mvnw` funcione desde una shell limpia.

**Nota positiva, para no perderla en la lista:** el código de los últimos meses (`SpHelper`, `AccesoModuloHelper`, `WhatsAppWebhookController`, `SincronizacionEntregasService`, los tests de arquitectura) está por encima del promedio del sector — HMAC en tiempo constante, defensa explícita contra los defectos del legacy, javadoc que explica el porqué y no el qué. Los hallazgos Critical están todos en el estrato viejo o en la configuración, no en ese trabajo.
