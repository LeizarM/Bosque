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
| `PROXIES_CONFIABLES` | No — default vacío | IPs de las que se acepta `X-Forwarded-For`. En producción, las de Caddy: `181.114.119.195,127.0.0.1` (ver «Caddy delante») |
| `OPENWA_URL` | No — default `http://host.containers.internal:2785` | URL del servicio openWA. El default vale para producción (backend y openWA en contenedores del mismo host); en una PC de desarrollo hay que apuntarla a la IP o al `localhost` que corresponda |
| `OPENWA_API_KEY` | No — default vacío | Clave de openWA |
| `OPENWA_SESSION_ID` | No — default vacío | Sesión de openWA |
| `OPENWA_WEBHOOK_SECRET` | No — default vacío | Secreto HMAC del webhook |
| `OPENWA_TELEFONO_PRUEBA` | No — default vacío | Si se setea, todos los avisos de entrega van a este número en vez de al cliente real. Solo para desarrollo — dejarlo vacío en producción |
| `CONFIG_CIFRADO_CLAVE` | No — default vacío | Clave que cifra la contraseña SMTP de la recuperación de acceso dentro de `trec_configuracion`. Base64 de 32 bytes o más (la clave AES-256 es el SHA-256 de lo que se ponga, así que 64 no da más seguridad que 32): `openssl rand -base64 32`, que imprime una sola línea. Con 64 bytes `openssl` la parte en dos líneas y hay que pegar las dos juntas, o generarla con `openssl rand 64 | base64 -w0`. Que no sea el mismo valor que `JWT_SECRET`. Sin ella la app arranca, pero esa contraseña no se puede guardar ni usar. **No cambiarla después**: lo ya cifrado deja de poder leerse y hay que volver a cargar la contraseña desde la pantalla |

El servidor SMTP, la casilla, su contraseña, el remitente, los dominios corporativos y los límites
de la recuperación de acceso **no son variables de entorno**: viven en `trec_configuracion` y los
cambia un administrador desde la pantalla «Recuperación de acceso» del menú Administración, sin
compilar ni reiniciar.

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

En el servidor, Bosque corre en un **contenedor podman rootless** del usuario
`newimpexpap`, con una unidad systemd **de usuario**. Delante está el **Caddy**
del sistema, que atiende el HTTPS y renueva el certificado solo. La unidad real
(`systemctl --user cat container-bosque-app.service`) y `/etc/caddy/Caddyfile`
son la fuente de verdad: si algo de esto cambió, mandan ellos y no este documento.

```
App Flutter ──https──► :8443 ┐
Navegador   ──https──► :443  ┴► Caddy ──http──► 127.0.0.1:8008 ──► contenedor :8443 (Spring)
```

### 1. Las variables: un archivo normal y uno cifrado

Las dos viven en `~/.config/bosque/`, en el `HOME` de `newimpexpap`:

| Archivo | Qué lleva |
|---|---|
| `bosque.env` | Lo que no es secreto. Texto plano, `chmod 600`. |
| `secretos.env.cred` | Las claves: `DB_PASSWORD`, `JWT_SECRET`, `CONFIG_CIFRADO_CLAVE` y las de openWA si se usan. Cifradas con `systemd-creds` y atadas a este servidor. |

`bosque.env`:

```bash
mkdir -p ~/.config/bosque
tee ~/.config/bosque/bosque.env > /dev/null <<'EOF'
SPRING_PROFILES_ACTIVE=prod
DB_HOST=192.168.3.116
DB_PORT=1433
DB_NAME=BOSQUE-2_0
DB_USERNAME=bosque_app
JWT_EXPIRATION=36000
PROXIES_CONFIABLES=181.114.119.195,127.0.0.1
EOF
chmod 600 ~/.config/bosque/bosque.env
```

Una variable por línea, `CLAVE=valor`, sin `export` y sin comillas.

`secretos.env.cred` se escribe en `/run/user/<uid>`, que está en memoria, se
cifra y se borra el texto plano. Así la clave no pasa por el disco ni por el
historial de bash:

```bash
cd ~/.config/bosque
umask 077
nano /run/user/$(id -u)/s.env      # DB_PASSWORD=..., JWT_SECRET=..., CONFIG_CIFRADO_CLAVE=...
systemd-creds --user encrypt --name=secretos.env /run/user/$(id -u)/s.env secretos.env.cred
shred -u /run/user/$(id -u)/s.env
systemd-creds --user decrypt --name=secretos.env secretos.env.cred - | cut -d= -f1   # solo los nombres
```

Para cambiar una clave: `decrypt` al mismo archivo de `/run/user`, editarlo,
`encrypt` otra vez, `shred -u` y reiniciar la unidad.

- El `.cred` solo se abre en este servidor: queda atado a su clave de máquina
  (y al TPM, si hay). Los valores tienen que estar también en un gestor de
  contraseñas fuera del servidor; si se reinstala, el `.cred` ya no sirve.
- `systemd-creds --user` necesita systemd 256 o más nuevo (Fedora 41 trae 256).
- **No sirve `LoadCredentialEncrypted=`.** En una unidad de usuario con systemd
  256 falla con «Failed to determine local credential key: Permission denied»,
  porque intenta leer la clave de la máquina, que es solo de root.
  `systemd-creds --user` sí funciona porque se la pide a un servicio del
  sistema. Por eso la unidad lo llama en `ExecStartPre`.

### 2. La unidad systemd (de usuario)

No es `/etc/systemd/system/bosque.service`. Es
`~/.config/systemd/user/container-bosque-app.service`, y se administra con
`systemctl --user`, sin `sudo`:

```ini
[Unit]
Description=Podman container-bosque-app.service
Wants=network-online.target
After=network-online.target
RequiresMountsFor=%t/containers

[Service]
ExecStartPre=/bin/sh -c 'umask 077 && systemd-creds --user decrypt --name=secretos.env %h/.config/bosque/secretos.env.cred %t/bosque-secretos.env'
Environment=PODMAN_SYSTEMD_UNIT=%n
Restart=always
TimeoutStopSec=70
ExecStart=/usr/bin/podman run \
        --cidfile=%t/%n.ctr-id \
        --cgroups=no-conmon \
        --rm \
        --sdnotify=conmon \
        --replace \
        -d \
        --name bosque-app \
        --env-file=%h/.config/bosque/bosque.env \
        --env-file=%t/bosque-secretos.env \
        -p 127.0.0.1:8008:8443 \
        -v bosque-uploads:/app/uploads:Z \
        --security-opt label=type:container_t \
        --cap-drop=ALL \
        --tz America/La_Paz localhost/bosque-app:latest
ExecStartPost=/usr/bin/rm -f %t/bosque-secretos.env
ExecStop=/usr/bin/podman stop --ignore -t 10 --cidfile=%t/%n.ctr-id
ExecStopPost=/usr/bin/podman rm -f --ignore -t 10 --cidfile=%t/%n.ctr-id
ExecStopPost=/usr/bin/rm -f %t/bosque-secretos.env
Type=notify
NotifyAccess=all

[Install]
WantedBy=default.target
```

```bash
systemctl --user daemon-reload
systemctl --user enable --now container-bosque-app.service
loginctl enable-linger newimpexpap   # para que siga corriendo sin sesion SSH abierta
journalctl --user -u container-bosque-app.service -f
```

- `%h` y `%t` los resuelve systemd: el `HOME` del usuario y su directorio de
  runtime (`/run/user/<uid>`, en memoria).
- El archivo descifrado existe unos segundos: lo crea `ExecStartPre`, lo lee
  `podman run` y lo borra `ExecStartPost`. Si el arranque falla, lo borra
  `ExecStopPost`.
- `-p 127.0.0.1:8008:8443`: Spring solo se alcanza desde el propio servidor;
  de afuera todo entra por Caddy. No se monta ningún keystore.
- `uploads/` vive en el volumen nombrado `bosque-uploads`, montado en
  `/app/uploads`, así que sobrevive a que se recree el contenedor.
- `loginctl enable-linger` es imprescindible: sin él, la unidad de usuario se
  apaga sola en cuanto se cierra la última sesión SSH de `newimpexpap`.

`./deploy.sh` reconstruye la imagen (`podman build`) y hace `systemctl --user
restart container-bosque-app.service`, pero **no** regenera la unidad ni los
archivos de variables: son provisión manual, una sola vez.

### 3. Caddy delante

`/etc/caddy/Caddyfile` es del servicio del sistema (con `sudo`). Atiende
`app.esppapel.com` en el 443 (navegador) y en el 8443 (la app Flutter,
`AppConstants.baseUrl`), saca y renueva el certificado de Let's Encrypt, y
reenvía en HTTP a Spring:

```caddyfile
{
	servers {
		timeouts {
			read_header 10s
		}
	}
}

app.esppapel.com, app.esppapel.com:8443 {
	request_body {
		max_size 20MB
	}

	reverse_proxy http://127.0.0.1:8008 {
		header_up X-Forwarded-For {http.request.remote.host}
		transport http {
			dial_timeout 10s
		}
	}

	header {
		Referrer-Policy "strict-origin-when-cross-origin"
		Strict-Transport-Security "max-age=31536000; includeSubDomains"
	}

	log {
		output file /var/log/caddy/access.log {
			roll_disabled
		}
	}
}
```

- **La IP del cliente.** `header_up X-Forwarded-For` *reemplaza* la cabecera,
  así que nadie puede inventar su IP. Spring la acepta solo si la conexión
  viene de `PROXIES_CONFIABLES`. Caddy le llega como `181.114.119.195` (la IP
  del servidor); `127.0.0.1` está por si cambia el camino. Si la lista no
  coincide, todos los usuarios comparten el mismo cupo de intentos de login.
  Para comprobarlo, desde afuera:
  `curl -A python-requests -H "X-Forwarded-For: 1.2.3.4" https://app.esppapel.com:8443/`.
  En el journal, «Suspicious User-Agent detected from IP» tiene que mostrar la
  IP real, no `1.2.3.4`.
- **HSTS va en Caddy.** Spring Security solo lo pone en peticiones HTTPS, y a
  Spring le llegan en HTTP.
- **SELinux.** Caddy corre como `httpd_t`, y su HTTP/3 en el 8443 usa UDP, que
  `http_port_t` no traía. Se habilita una sola vez:
  `sudo semanage port -a -t http_port_t -p udp 8443`. Sin eso, Caddy rechaza la
  configuración con «listen udp :8443: bind: permission denied».
- **Cambiar la configuración:** `sudo caddy validate --config
  /etc/caddy/Caddyfile --adapter caddyfile` y después `sudo systemctl reload
  caddy`. Si la recarga falla, el servicio queda trabado en «reloading»; se
  destraba con `sudo systemctl restart caddy`.
- **El puerto 80 es de Caddy**, que lo usa para validar con Let's Encrypt. No
  usar certbot en modo standalone para este dominio: para renovar, detiene a
  Caddy.
- **Vencimiento.** Caddy renueva el certificado cuando le queda un tercio de
  vida (unos 30 días antes), y Let's Encrypt ya no avisa por correo. Para verlo:

  ```bash
  echo | openssl s_client -connect app.esppapel.com:8443 -servername app.esppapel.com 2>/dev/null | openssl x509 -noout -issuer -startdate -enddate
  ```

### 4. Nunca pasar secretos por `-e` ni por el `Dockerfile`

```ini
# MAL: cualquier usuario del servidor los ve con `ps aux`
ExecStart=/usr/bin/podman run -e JWT_SECRET=... ...
```

La línea de comandos de `podman run` es pública (`ps aux`), igual que en
cualquier proceso Linux. Por eso la unidad pasa la RUTA a un archivo
(`--env-file=...`) y no valores sueltos con `-e`. Tampoco va nunca un secreto
como `ENV` en el `Dockerfile`: queda grabado en la imagen para siempre, visible
con `podman history` para cualquiera que pueda hacer `podman pull` de ella.

### 5. Comprobar que las variables llegaron

```bash
podman exec bosque-app printenv | grep -E 'DB_|JWT_|CONFIG_|PROXIES_' | sed 's/=.*/=.../'
```

Si la aplicación no arranca, el journal de la unidad dice exactamente qué
falta:

```bash
journalctl --user -u container-bosque-app.service -n 200 --no-pager
```

```
Could not resolve placeholder 'DB_PASSWORD' in value "${DB_PASSWORD}"
jwt.secret tiene 32 bytes y HS512 necesita al menos 64
```

---

## La cuenta de la base de datos

La aplicación debe entrar con un login propio, `bosque_app`, y nunca con `sa`.
Para crearlo:

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

Sin `db_owner` ni `db_datareader`: la aplicación solo ejecuta procedimientos y
nunca toca tablas directamente. Si algún SP usa el linked server `SRV_2022`
(contactos SAP, sincronización de entregas), el mapeo de logins del linked
server también tiene que incluir a `bosque_app`.
