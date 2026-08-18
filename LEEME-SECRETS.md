# Secrets — cómo se configuran

Los secretos salieron de `application.properties`. Ahora se pasan por **variable
de entorno**, que es el único mecanismo que funciona igual en Windows y en Linux,
no necesita ninguna dependencia nueva y es compatible con Java 8. Spring Boot ya
lo resuelve solo: `${DB_PASSWORD}` en el `.properties` lee la variable de entorno
`DB_PASSWORD` sin configurar nada.

No hace falta Vault, ni Spring Cloud Config, ni jasypt. Para este caso serían
tres piezas más que mantener y una más que puede fallar de madrugada.

---

## Compilar no necesita ninguna variable

Vale la pena decirlo primero porque es la confusión natural: las variables son de
**ejecución**, no de compilación. `mvn package` no las mira.

```bash
./mvnw clean package     # sin exportar nada -> target/bosque-1.0.0.jar
```

El jar sale igual en cualquier máquina. Los secretos no entran nunca en el
artefacto: dentro del jar, `application.properties` dice `${DB_PASSWORD}`, y ese
texto se resuelve recién cuando la JVM arranca. **El mismo jar sirve para
desarrollo y para producción**; lo único que cambia es el entorno donde lo
levantás.

(La única prueba que necesitaría la base, `BosqueApplicationTests`, se saltea
sola si no hay `DB_PASSWORD`. Por eso `package` pasa en limpio.)

---

## Las variables

| Variable | ¿Obligatoria? | Qué es |
|---|---|---|
| `DB_USERNAME` | **Sí** | Usuario de SQL Server |
| `DB_PASSWORD` | **Sí** | Su contraseña |
| `JWT_SECRET` | **Sí** | Clave de firma de los tokens, Base64, mínimo 64 bytes |
| `DB_HOST` | No — default `192.168.3.116` | Host del motor |
| `DB_PORT` | No — default `1433` | Puerto |
| `DB_NAME` | No — default `BOSQUE-2_0` | Base |
| `JWT_EXPIRATION` | No — default `36000` | Vida del token, en segundos |
| `PROXIES_CONFIABLES` | No — default vacío | IPs de las que se acepta `X-Forwarded-For` |
| `OPENWA_API_KEY` | No — default vacío | Clave de openWA |
| `OPENWA_SESSION_ID` | No — default vacío | Sesión de openWA |
| `OPENWA_WEBHOOK_SECRET` | No — default vacío | Secreto HMAC del webhook |

**Las tres primeras no tienen valor por defecto y eso es deliberado.** Si faltan,
la aplicación **no arranca** y dice cuál falta. Es preferible a la alternativa:
así fue exactamente como la contraseña de `sa` terminó dentro del repositorio,
escrita como "default por si acaso" en `${DB_PASSWORD:<CLAVE-ROTADA>}`.

Las de openWA sí arrancan vacías, porque WhatsApp es accesorio: sin clave los
envíos fallan y quedan en el log, pero el ERP sigue funcionando. Voltear el
arranque entero del ERP porque no está configurado WhatsApp sería peor.

---

## Generar el `JWT_SECRET`

```bash
openssl rand -base64 64
```

En Windows sin openssl, con PowerShell:

```powershell
$b = New-Object byte[] 64
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

`JwtProvider` valida al arrancar que sea Base64 y que tenga al menos 64 bytes
decodificados, que es lo que pide HS512. Si no, no arranca.

> **Cambiar `JWT_SECRET` invalida todos los tokens vivos.** Los usuarios quedan
> deslogueados y la app Flutter también. Es un cambio para anunciar antes, no
> para hacer un martes a las 11.

---

## Windows (desarrollo)

### Opción A — desde IntelliJ

`Run > Edit Configurations… > Environment variables`:

```
DB_USERNAME=bosque_app;DB_PASSWORD=...;JWT_SECRET=...
```

Queda guardado en el workspace del IDE, que no se versiona.

### Opción B — desde la terminal

PowerShell, sólo para esa sesión:

```powershell
$env:DB_USERNAME = "bosque_app"
$env:DB_PASSWORD = "..."
$env:JWT_SECRET  = "..."
.\mvnw.cmd spring-boot:run
```

Para que sobrevivan a cerrar la terminal, una sola vez por máquina:

```powershell
[Environment]::SetEnvironmentVariable("DB_USERNAME", "bosque_app", "User")
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "...", "User")
[Environment]::SetEnvironmentVariable("JWT_SECRET",  "...", "User")
```

(Hay que abrir una terminal nueva para que las tome.)

### Opción C — archivo fuera del proyecto

Si preferís un archivo antes que variables, poné un
`application-local.properties` **fuera del repositorio** y arrancá con:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.config.additional-location=file:C:/bosque/"
```

o, sobre el jar ya construido:

```powershell
java "-Dspring.config.additional-location=file:C:/bosque/" -jar target\bosque-1.0.0.jar
```

`additional-location` **suma** al `application.properties` de adentro del jar y
pisa lo que repita, así que ese archivo lleva solamente los secretos. Que esté
fuera del árbol del proyecto es el punto: no hay forma de commitearlo sin querer.

---

## Linux Fedora (producción)

### 1. El archivo de entorno

```bash
sudo mkdir -p /etc/bosque
sudo tee /etc/bosque/bosque.env > /dev/null <<'EOF'
DB_HOST=192.168.3.116
DB_PORT=1433
DB_NAME=BOSQUE-2_0
DB_USERNAME=bosque_app
DB_PASSWORD=<la clave>
JWT_SECRET=<openssl rand -base64 64>
JWT_EXPIRATION=36000
PROXIES_CONFIABLES=
OPENWA_API_KEY=<la clave>
OPENWA_SESSION_ID=<el uuid>
OPENWA_WEBHOOK_SECRET=<openssl rand -hex 32>
TZ=America/La_Paz
EOF

sudo chown root:bosque /etc/bosque/bosque.env
sudo chmod 640 /etc/bosque/bosque.env
```

`640` con grupo `bosque`: lo lee el servicio y root, nadie más.

Sobre el formato del archivo: systemd lo parsea "como un shell", así que las
comillas se admiten y se sacan. Sin comillas funciona para los valores normales
—incluidas las claves en Base64, que llevan `+`, `/` y `=`; sólo el primer `=`
separa nombre de valor—. **Poné comillas si el valor tiene espacios o un `#`**,
porque `#` sin comillas abre un comentario y te trunca la clave en silencio:

```
DB_PASSWORD="clave con espacios y # adentro"
```

Nada de `export` delante: no es un script, es una lista de `CLAVE=valor`.

### 2. La unidad systemd

`/etc/systemd/system/bosque.service`:

```ini
[Unit]
Description=Bosque ERP Backend
After=network-online.target

[Service]
Type=simple
User=bosque
WorkingDirectory=/opt/bosque
EnvironmentFile=/etc/bosque/bosque.env
ExecStart=/usr/bin/java -Xms512m -Xmx2g -jar /opt/bosque/bosque-1.0.0.jar
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now bosque
sudo journalctl -u bosque -f
```

`WorkingDirectory=/opt/bosque` no es decorativo: `uploads/` se resuelve como
ruta relativa al directorio de trabajo del proceso. Sin esa línea, systemd
arranca en `/` y los archivos subidos irían a `/uploads/`.

### 3. Nunca pasar secretos por `-D` en el `ExecStart`

```ini
# MAL: cualquier usuario del servidor los ve con `ps aux`
ExecStart=/usr/bin/java -Djwt.secret=... -Dspring.datasource.password=... -jar ...
```

La línea de comandos de un proceso es pública en Linux (`/proc/<pid>/cmdline`).
El entorno no: `/proc/<pid>/environ` sólo lo lee el dueño del proceso y root. Por
eso van por `EnvironmentFile` y no por `-D`.

### 4. Comprobar que las variables llegaron

```bash
sudo systemctl show bosque -p Environment
```

Si la aplicación no arranca, el mensaje dice exactamente qué falta:

```
Could not resolve placeholder 'DB_PASSWORD' in value "${DB_PASSWORD}"
jwt.secret tiene 32 bytes y HS512 necesita al menos 64
```

---

## Qué hacer ahora, una sola vez

Los valores que estaban en el repositorio hay que darlos por comprometidos:
estuvieron versionados, y siguen estando en el historial de git.

1. **Rotar la contraseña de `sa`** — estaba en `application.properties`, en
   `ClassGenerator.java` y en `bin/src/main/resources/application.properties`.
2. **Crear un login propio** para la aplicación y dejar de usar `sa`:

   ```sql
   CREATE LOGIN bosque_app WITH PASSWORD = '<clave fuerte>',
       CHECK_POLICY = ON, DEFAULT_DATABASE = [BOSQUE-2_0];
   GO
   USE [BOSQUE-2_0];
   CREATE USER bosque_app FOR LOGIN bosque_app;
   GRANT EXECUTE ON SCHEMA::dbo TO bosque_app;
   -- SpHelper.ejecutarAbm usa SimpleJdbcCall, que lee los metadatos de los
   -- parámetros del procedimiento. Sin esto falla con "Unable to determine the
   -- correct call signature".
   GRANT VIEW DEFINITION ON SCHEMA::dbo TO bosque_app;
   GO
   ```

   Sin `db_owner` ni `db_datareader`: la aplicación sólo ejecuta procedimientos,
   nunca toca tablas directamente.
3. **Generar un `JWT_SECRET` nuevo.** La clave anterior estaba escrita en
   `JwtConfig.java`, versionada: cualquiera con acceso al repo podía firmarse un
   token de administrador. Al cambiarla, todos los tokens vivos se invalidan.
4. **Rotar la clave de openWA** (`<OPENWA_API_KEY>` estaba en el `.properties` y en
   los ejemplos del javadoc de `WhatsAppWebhookController`).
