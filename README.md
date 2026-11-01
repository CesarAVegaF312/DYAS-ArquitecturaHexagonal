# Taller de Arquitectura Hexagonal y Arquitectura Limpia

## Objetivo General

Entender y aplicar la **arquitectura hexagonal** (puertos y adaptadores) y su relación con la **arquitectura limpia**, cambiando la tecnología de persistencia y agregando una integración nueva a un servicio REST real **sin modificar sus reglas de negocio**, y comprobando las fronteras entre capas con pruebas automáticas.

## Índice

- [Conceptos clave](#conceptos-clave)
- [CONOCE EL TALLER](#conoce-el-taller)
  - [Requisitos](#requisitos)
  - [Estructura del proyecto](#estructura-del-proyecto)
- [PASO A PASO](#paso-a-paso)
  - [Paso 1: Levantar el ambiente](#paso-1-levantar-el-ambiente)
  - [Paso 2: Recorrer el hexágono](#paso-2-recorrer-el-hexágono)
  - [Paso 3: Adaptador de PostgreSQL](#paso-3-adaptador-de-postgresql)
  - [Paso 4: Puerto de notificaciones](#paso-4-puerto-de-notificaciones)
  - [Paso 5: Pruebas](#paso-5-pruebas)
- [Retos opcionales](#retos-opcionales)
- [Buenas prácticas](#buenas-prácticas)
- [Para entregar](#para-entregar-con-este-taller)
- [Resumen del taller](#hagamos-un-resumen)
- [Recursos recomendados](#recursos-recomendados)
- [Créditos y uso académico](#créditos-y-uso-académico)
- [Licencia](#licencia-de-uso)

---

## Conceptos clave

| Concepto | Qué es |
|---|---|
| **Arquitectura hexagonal** | Estilo propuesto por Alistair Cockburn: la aplicación queda en el centro y se comunica con el exterior solo a través de **puertos** (interfaces). Las tecnologías concretas (HTTP, bases de datos, correo) son **adaptadores** intercambiables. |
| **Puerto de entrada** | Interfaz con los casos de uso que la aplicación ofrece. Aquí, `GestionarReservas`. |
| **Puerto de salida** | Interfaz con lo que la aplicación necesita del exterior, sin decir cómo se implementa. Aquí, `RepositorioReservas`. |
| **Adaptador** | Traduce entre una tecnología y un puerto. El de entrada (REST) llama al puerto de entrada; el de salida (memoria, PostgreSQL) implementa el puerto de salida. |
| **Regla de dependencia** | Las dependencias del código apuntan siempre hacia adentro: los adaptadores conocen la aplicación y el dominio; el dominio no conoce a nadie. |
| **Raíz de composición** | El único lugar donde se conectan los casos de uso con sus adaptadores concretos. Aquí, el paquete `configuracion`. |
| **Prueba de contrato** | Las mismas pruebas aplicadas a todos los adaptadores de un puerto, para garantizar que son intercambiables. |
| **Función de aptitud** | Una regla de arquitectura escrita como prueba automática. Aquí se usan con [ArchUnit](https://www.archunit.org/). |

### Arquitectura limpia y hexagonal

La arquitectura limpia de Robert C. Martin organiza el sistema en círculos concéntricos con la misma regla de dependencia. En este proyecto, sus círculos corresponden a estos paquetes:

| Círculo de la arquitectura limpia | Paquete del proyecto | Contenido |
|---|---|---|
| Entidades | `dominio` | `Reserva` y sus reglas de negocio. Java puro. |
| Casos de uso | `aplicacion` | `ServicioReservas` y los puertos. |
| Adaptadores de interfaz | `adaptadores` | Controlador REST, repositorios, notificadores. |
| Frameworks y controladores | `configuracion` y Spring Boot | Arranque, perfiles y conexión de las piezas. |

---

## CONOCE EL TALLER

La biblioteca necesita un servicio para reservar salas de estudio. Las reglas de negocio ya están implementadas y probadas en el dominio:

- Las salas tienen un código con el formato `L-NNN`, por ejemplo `B-201`.
- El responsable debe tener un correo `@unisabana.edu.co`.
- Solo se reserva entre las 07:00 y las 21:00, por máximo 3 horas, y no en fechas pasadas.
- Una sala no puede tener dos reservas activas que se crucen.

Hoy el servicio guarda las reservas **en memoria**: se pierden cada vez que se reinicia. Su trabajo es conectarlo a **PostgreSQL** y agregar **avisos al responsable**, sin tocar las reglas de negocio.

El servicio expone estos endpoints:

| Método y ruta | Qué hace | Respuestas |
|---|---|---|
| `POST /api/reservas` | Crea una reserva | `201`, `400` si los datos no cumplen las reglas, `409` si la sala está ocupada |
| `GET /api/reservas/{id}` | Consulta una reserva | `200`, `404` |
| `GET /api/reservas?fecha=AAAA-MM-DD` | Lista las reservas de una fecha, por sala y hora | `200` |
| `POST /api/reservas/{id}/cancelacion` | Cancela una reserva | `200`, `400` si ya estaba cancelada, `404` |

Este servicio es la base del taller de [DevSecOps](https://github.com/CesarAVegaF312/DYAS-DevSecOps), donde se le agregan seguridad y despliegue continuo.

### Requisitos

- JDK 21 y Maven 3.9 o superior (o el wrapper `./mvnw` incluido).
- Docker Desktop o Docker Engine con Docker Compose v2.
- Git y un IDE. El repositorio trae un contenedor de desarrollo (`.devcontainer/`) para abrirlo listo en GitHub Codespaces o en VS Code.
- Haber hecho los talleres de [Docker](https://github.com/CesarAVegaF312/DYAS-Docker) (partes 1 y 2) y de [TDD](https://github.com/CesarAVegaF312/DYAS-TDD).

Verifique el ambiente antes de empezar:

```bash
java -version          # debe mostrar 21
./mvnw -version
docker compose version
```

### Estructura del proyecto

```text
.
├── compose.yaml                         # aplicación + PostgreSQL
├── .env.example                         # variables de entorno; cópielo como .env
├── Dockerfile
├── src/main/java/edu/unisabana/dyas/reservas/
│   ├── dominio/                         # Reserva y excepciones de negocio. NO se modifica
│   ├── aplicacion/
│   │   ├── ServicioReservas.java        # casos de uso: lo completa en el Paso 4
│   │   └── puertos/
│   │       ├── entrada/                 # GestionarReservas, NuevaReserva
│   │       └── salida/                  # RepositorioReservas (y su puerto nuevo del Paso 4)
│   ├── adaptadores/
│   │   ├── entrada/web/                 # controlador REST. NO se modifica
│   │   └── salida/
│   │       ├── memoria/                 # adaptador en memoria (perfil memoria)
│   │       ├── postgres/                # lo implementa en el Paso 3 (perfil postgres)
│   │       └── notificaciones/          # lo crea en el Paso 4
│   └── configuracion/                   # raíz de composición
├── src/main/resources/
│   ├── application.properties
│   ├── application-memoria.properties   # perfil por defecto, sin base de datos
│   ├── application-postgres.properties  # conexión a PostgreSQL
│   └── schema.sql                       # tabla reservas
└── src/test/java/edu/unisabana/dyas/reservas/
    ├── ArquitecturaTest.java            # reglas de arquitectura (ArchUnit)
    ├── aplicacion/puertos/salida/
    │   └── RepositorioReservasContrato.java   # prueba de contrato del puerto
    └── ...                              # pruebas del dominio, casos de uso y adaptadores
```

---

## PASO A PASO

### Paso 1: Levantar el ambiente

1. Haga un *fork* del repositorio y clónelo:

   ```bash
   git clone https://github.com/<tu-usuario>/DYAS-ArquitecturaHexagonal.git
   cd DYAS-ArquitecturaHexagonal
   ```

2. Ejecute las pruebas. Deben pasar todas, incluidas las reglas de arquitectura:

   ```bash
   ./mvnw test
   ```

3. Cree su archivo de variables y levante el sistema:

   ```bash
   cp .env.example .env
   docker compose up --build -d
   docker compose ps          # app y postgres deben aparecer como "healthy"
   ```

4. Cree una reserva para mañana (cambie la fecha) y consúltela:

   ```bash
   curl -i -X POST http://localhost:8080/api/reservas \
        -H "Content-Type: application/json" \
        -d '{"sala":"B-201","responsable":"ana.perez@unisabana.edu.co","fecha":"2026-11-02","inicio":"10:00","fin":"12:00"}'
   curl "http://localhost:8080/api/reservas?fecha=2026-11-02"
   ```

5. Reinicie la aplicación y vuelva a consultar:

   ```bash
   docker compose restart app
   curl "http://localhost:8080/api/reservas?fecha=2026-11-02"
   ```

   La lista está vacía: con el perfil `memoria` las reservas se pierden. PostgreSQL está corriendo, pero la aplicación todavía no lo usa.

> Si el puerto 8080 o 5432 está ocupado, cambie `APP_PORT` o `DB_PORT` en el archivo `.env`.

### Paso 2: Recorrer el hexágono

Recorra el código desde afuera hacia adentro: `ReservaController` → `GestionarReservas` → `ServicioReservas` → `Reserva` y `RepositorioReservas` → `RepositorioReservasEnMemoria`. Responda en su informe:

1. ¿Qué clases son el puerto de entrada, el puerto de salida, el adaptador de entrada y el adaptador de salida?
2. ¿Por qué `ServicioReservas` no tiene anotaciones de Spring? ¿Dónde se crea y quién le entrega el repositorio?
3. ¿Por qué `Reserva.nueva(...)` recibe la fecha de hoy como parámetro en lugar de usar `LocalDate.now()`?
4. ¿Por qué el controlador recibe `SolicitudReserva` y devuelve `ReservaRespuesta` en lugar de usar `Reserva` directamente?
5. ¿Cómo decide Spring qué adaptador de persistencia usar? (Revise `@Profile` y los archivos `application-*.properties`.)

Luego **rompa una regla a propósito**: en `ServicioReservas`, agregue un atributo de tipo `org.springframework.jdbc.core.JdbcTemplate`, ejecute `./mvnw test -Dtest=ArquitecturaTest` y copie en el informe qué regla falló y por qué. Deshaga el cambio.

Dibuje el diagrama del hexágono con los puertos y adaptadores actuales y los que va a agregar.

### Paso 3: Adaptador de PostgreSQL

Implemente `RepositorioReservasPostgres` en el paquete `adaptadores.salida.postgres`:

- Debe implementar `RepositorioReservas` con `JdbcTemplate` y **sentencias parametrizadas** sobre la tabla `reservas` (ver `schema.sql`).
- Debe registrarse como *bean* solo para el perfil `postgres`, igual que el de memoria lo hace para `memoria`.
- La traducción entre filas y objetos `Reserva` ocurre en el adaptador. Use `Reserva.reconstruir(...)`, que no vuelve a validar las reglas de una reserva ya guardada.
- Guardar una reserva que ya existe la actualiza (por ejemplo, al cancelarla). Pista: `INSERT ... ON CONFLICT (id) DO UPDATE` en PostgreSQL.
- **No modifique** el dominio, los casos de uso, los puertos existentes ni el controlador.

Cree `RepositorioReservasPostgresTest` en `src/test/java/.../adaptadores/salida/postgres/` como subclase de `RepositorioReservasContrato`, igual que `RepositorioReservasEnMemoriaTest`, pero creando el adaptador contra un PostgreSQL real con [Testcontainers](https://java.testcontainers.org/modules/databases/postgres/) (las dependencias ya están en el `pom.xml`). Las cinco pruebas del contrato deben pasar sin cambiarlas.

Active el adaptador y compruebe que las reservas sobreviven a un reinicio:

```bash
# en el .env: PERSISTENCIA=postgres
docker compose up --build -d
# cree una reserva, reinicie la aplicación y vuelva a consultar
docker compose exec postgres psql -U reservas -d reservas -c "SELECT sala, fecha, inicio, estado FROM reservas;"
```

> Antes de implementar el adaptador, pruebe a levantar con `PERSISTENCIA=postgres`: la aplicación no arranca y el log dice que falta un *bean* de tipo `RepositorioReservas`. Explique en el informe por qué es un buen síntoma.

### Paso 4: Puerto de notificaciones

La biblioteca quiere avisar al responsable cada vez que se crea o se cancela una reserva. Hoy el aviso será un mensaje en el log; mañana puede ser un correo o un mensaje en una cola.

1. Cree el puerto de salida `NotificadorReservas` en `aplicacion.puertos.salida`, con los métodos `reservaCreada(Reserva)` y `reservaCancelada(Reserva)`.
2. Haga que `ServicioReservas` lo use: avisa después de guardar una reserva nueva o cancelada, y no avisa si la operación falla.
3. Cree el adaptador `NotificadorPorLog` en `adaptadores.salida.notificaciones`, que escribe el aviso en el log.
4. Conéctelo en la raíz de composición (`ConfiguracionAplicacion`).

`ServicioReservas` no puede importar nada del paquete `adaptadores` ni de Spring: las reglas de `ArquitecturaTest` lo verifican.

### Paso 5: Pruebas

1. En `ServicioReservasTest`, use un notificador de prueba (por ejemplo, una clase anónima que guarde los avisos en una lista) y verifique que se avisa al crear y al cancelar, y que no se avisa si la sala está ocupada.
2. Verifique que `RepositorioReservasPostgresTest` pasa el contrato completo.
3. Ejecute todo:

   ```bash
   ./mvnw test
   ```

   Todas las pruebas, incluidas las de `ArquitecturaTest`, deben pasar. No debilite las reglas de arquitectura para que el código pase: corrija el código.

---

## Retos opcionales

1. **Correo real.** Cree un segundo adaptador de notificaciones que envíe correos con `spring-boot-starter-mail` a un servidor de prueba [Mailpit](https://mailpit.axllent.org/) agregado a `compose.yaml`. Elija el adaptador por perfil, sin cambiar `ServicioReservas`.
2. **Otro adaptador de entrada.** Exponga los mismos casos de uso por otro medio (por ejemplo, una línea de comandos o una cola de RabbitMQ) usando el mismo puerto `GestionarReservas`. ¿Qué tuvo que cambiar en la aplicación?
3. **Reservas simultáneas.** Dos peticiones al mismo tiempo pueden crear reservas que se cruzan, porque la validación y el guardado no ocurren en una transacción. Demuéstrelo y corríjalo en el adaptador de PostgreSQL con una restricción de exclusión (`EXCLUDE USING gist`). ¿En qué capa debe vivir esa protección y por qué?
4. **Comparar con JPA.** El taller de DevSecOps implementa el mismo puerto con JPA. Compare los dos adaptadores: ¿qué gana y qué pierde cada uno? ¿Cambió algo fuera del adaptador?

---

## Buenas prácticas

- Los puertos se definen con el vocabulario del negocio (`buscarPorSalaYFecha`), no con el de la tecnología (`selectWhere`).
- Los objetos del dominio no cruzan la frontera hacia afuera: el adaptador web usa sus propios DTO y el de persistencia sus propias filas.
- Un adaptador nuevo pasa la misma prueba de contrato que los anteriores antes de usarse.
- Las reglas de arquitectura corren en cada build, igual que cualquier otra prueba.
- El archivo `.env` está en `.gitignore`. Solo se versiona `.env.example`.

---

## PARA ENTREGAR CON ESTE TALLER

Entregue el enlace a su *fork* con el código y un informe breve (`INFORME.md` en la raíz) con las respuestas del Paso 2, la regla que falló al romperla, el diagrama del hexágono y la evidencia de cada paso (comandos y salidas).

### Criterios de evaluación

| Criterio | Qué se revisa | Peso |
|---|---|---|
| **Ambiente** | `docker compose up --build` levanta la aplicación y PostgreSQL desde un clon limpio, con `PERSISTENCIA=memoria` y con `PERSISTENCIA=postgres`. | 10 % |
| **Arquitectura** | El dominio, los casos de uso existentes, los puertos originales y el controlador no se modificaron (salvo agregar el notificador a `ServicioReservas` y a la raíz de composición). Las cuatro reglas de `ArquitecturaTest` pasan sin haberlas cambiado. `ServicioReservas` no conoce adaptadores ni Spring. | 30 % |
| **Adaptador de PostgreSQL** | Implementa el puerto con sentencias parametrizadas, se activa solo con el perfil `postgres`, traduce filas a `Reserva` sin lógica de negocio y pasa las cinco pruebas del contrato contra PostgreSQL real. Las reservas sobreviven a un reinicio. | 25 % |
| **Puerto de notificaciones** | El puerto está en `aplicacion.puertos.salida`; el servicio avisa al crear y al cancelar, y no avisa si la operación falla; el adaptador está en `adaptadores.salida.notificaciones`. | 20 % |
| **Pruebas e informe** | `./mvnw test` pasa completo; el informe responde el Paso 2 e incluye el diagrama y la evidencia. | 15 % |

---

## Hagamos un resumen

- El dominio y los casos de uso no saben si los llaman por HTTP ni dónde se guardan los datos.
- Cambiar de memoria a PostgreSQL fue escribir un adaptador nuevo y cambiar un perfil, no reescribir la aplicación.
- Una integración nueva empieza por un puerto con el lenguaje del negocio y luego un adaptador con la tecnología.
- Las pruebas de contrato garantizan que los adaptadores son intercambiables, y las reglas de ArchUnit evitan que las fronteras se rompan con el tiempo.

---

## Recursos recomendados

- Alistair Cockburn: [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/)
- Robert C. Martin: [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [ArchUnit: guía de usuario](https://www.archunit.org/userguide/html/000_Index.html)
- [Spring Framework: acceso a datos con JDBC](https://docs.spring.io/spring-framework/reference/data-access/jdbc.html)
- [Spring Boot: perfiles](https://docs.spring.io/spring-boot/reference/features/profiles.html)
- [Testcontainers: módulo de PostgreSQL](https://java.testcontainers.org/modules/databases/postgres/)

---

## Créditos y uso académico

**Autor:** César Augusto Vega Fernández
**Curso:** Diseño y Arquitectura de Software, Universidad de La Sabana

## Licencia de uso

Este material se distribuye bajo la licencia [Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional (CC BY-NC-SA 4.0)](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es).
