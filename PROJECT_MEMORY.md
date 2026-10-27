# StockBridge — Memoria de Proyecto

## Resumen y stack

StockBridge gestiona inventario y solicitudes mediante dos microservicios:

- `inventory-management-service`: Java 17, Spring Boot 3.2.4 y Arquitectura Hexagonal estricta.
- `supplier-availability-service`: Spring MVC ligero para simular la disponibilidad externa.
- Maven multi-módulo; PostgreSQL en producción y H2 para desarrollo; Flyway 9.
- MapStruct 1.5.5.Final, Spring Security 6 y JJWT 0.12.5.

Las cuentas de usuario y su autenticación viven exclusivamente en `inventory-management-service`. El paquete `application/` no importa Spring ni JPA; `UseCaseConfig` registra los casos de uso.

## Contrato de dominio

- `Product` incluye código, nombre, categoría, descripción, precio (`BigDecimal`) y stock. Valida los datos requeridos, precio no negativo de hasta dos decimales y stock no negativo.
- `Request` incluye producto, solicitante, cantidad, estado, fuente de cumplimiento, fecha, estimación de entrega y motivo. El dominio valida transiciones: `PENDIENTE → APROBADA → ENTREGADA`, `PENDIENTE → RECHAZADA`, y cancelación desde `PENDIENTE` o `APROBADA`.
- `Role`: `ROLE_EMPLEADO`, `ROLE_BODEGA`, `ROLE_ADMIN`.
- `RequestStatus`: `PENDIENTE`, `APROBADA`, `RECHAZADA`, `ENTREGADA`, `CANCELADA`.
- Excepciones de dominio: `DuplicateProductException`, `EmailAlreadyExistsException`, `InsufficientStockException`, `InvalidCredentialsException`, `InvalidProductDataException`, `InvalidStatusTransitionException`, `ProductNotFoundException`, `RequestNotFoundException`, `SupplierIntegrationException`, `UnauthorizedRoleException` y `UserNotFoundException`. `InvalidRequestDataException` también se usa para validar comandos.

## Implementación registrada

### Inventory Management Service

#### Aplicación (Java puro)

- Puertos de entrada: `AuthUseCase`, `ManageProductUseCase`, `ManageRequestUseCase`.
- Puertos de salida: `UserRepositoryPort`, `ProductRepositoryPort`, `RequestRepositoryPort`, `SupplierAvailabilityPort` y `PasswordHasherPort`.
- Servicios: `AuthUseCaseImpl`, `ProductUseCaseImpl` y `RequestUseCaseImpl`.
- Registro asigna `ROLE_EMPLEADO`; autenticación usa el puerto de hash de contraseñas. CRUD de productos valida precio y stock.

El flujo de solicitudes está en [RequestUseCaseImpl.java](inventory-management-service/src/main/java/org/stockbridge/inventory/application/service/RequestUseCaseImpl.java):

1. Si hay stock, lo reserva y persiste una solicitud `PENDIENTE` con fuente `INTERNAL`.
2. Si falta stock, consulta al `SupplierAvailabilityPort`; la solicitud queda `APROBADA` o `RECHAZADA` con fuente `SUPPLIER`.
3. Aprobar, rechazar, entregar y cancelar delegan la validación de transición al modelo `Request`.
4. Rechazar o cancelar libera una reserva interna. Entregar no vuelve a descontar el stock ya reservado.
5. Solo el solicitante puede cancelar su solicitud.

Se añadieron `RequestRepositoryPort`, `RequestEntity`, `SpringDataRequestRepository`, `RequestMapper` y `RequestPersistenceAdapter` para persistir ese ciclo de vida.

#### Infraestructura

- Entidades JPA: `UserEntity`, `ProductEntity`, `RequestEntity`.
- Repositorios Spring Data: `SpringDataUserRepository`, `SpringDataProductRepository`, `SpringDataRequestRepository`.
- Mappers MapStruct: `UserMapper`, `ProductMapper`, `RequestMapper`.
- Adaptadores: `UserPersistenceAdapter`, `ProductPersistenceAdapter`, `RequestPersistenceAdapter`, `SupplierExternalAdapter` y `PasswordHasherAdapter`.
- Configuración: `UseCaseConfig` construye los servicios y provee `Clock.systemUTC()`; `SecurityConfig` configura BCrypt, JWT stateless y autoridades con los nombres `ROLE_*`.
- Seguridad JWT: `JwtProvider` emite y valida tokens; `JwtAuthenticationFilter` configura el principal y autoridad desde el Bearer token.
- Controladores: `AuthController` (`/auth/register`, `/auth/login`), `ProductController` (`/api/products`) y `RequestController` (`/api/requests`).
- `GlobalExceptionHandler` mapea validación a 400, credenciales a 401, rol a 403, entidades ausentes a 404, conflictos a 409 y fallos inesperados/de proveedor a 500.
- Los mensajes de excepciones, validación y respuestas de error de ambos servicios están en español. Las validaciones HTTP declaran textos explícitos; los manejadores globales traducen las etiquetas HTTP, errores de JSON, errores inesperados y datos de integridad. Security responde JSON en español para 401 y 403.
- `Request` informa cantidades y transiciones inválidas mediante excepciones de dominio en español. `Product` también informa datos incompletos o reglas de precio/stock con excepciones de dominio.
- `ProductMapper` y `RequestMapper` ignoran explícitamente los métodos de dominio `withStock` y `reject` durante la conversión a dominio para evitar propiedades falsas de MapStruct.
- `MetricsConfig` y `StockBridgeMetrics` registran `stockbridge.requests.created` y `stockbridge.supplier.availability.checks`.
- `application.yml` define puerto 8081, datasource H2 por defecto, opciones PostgreSQL por variables, Flyway, JWT y Actuator (`health`, `info`, `metrics`).
- `V1__Initial_schema.sql` crea usuarios, productos y solicitudes; agrega claves foráneas, restricciones e índices. Productos usan `NUMERIC(12,2)` para precio y `stock` entero.
- El POST externo predeterminado es `http://localhost:8082/api/suppliers/check-availability`, configurable mediante `SUPPLIER_AVAILABILITY_URL`.

### Supplier Availability Service

- `AvailabilityRequest` y `AvailabilityResponse` son DTOs; `SupplierController` publica `POST /api/suppliers/check-availability`.
- Disponibilidad determinista: `discontinued` ofrece 0 unidades; `food` y `groceries`, 100 unidades/2 días; `electronics`, 25 unidades/7 días; otras categorías, 50 unidades/4 días.
- `exception/GlobalExceptionHandler` convierte errores de validación, cuerpo JSON inválido, recursos ausentes y fallos inesperados en respuestas JSON con mensajes en español.
- `application.yml` configura el puerto 8082.

### Recursos y cambios de soporte

- Se reemplazaron los `application.properties` de ambos módulos por `application.yml`.
- Se completaron los modelos de dominio necesarios y se alinearon los nombres de estados, roles, precio y stock con este contrato.
- Se agregaron `FulfillmentSource` y `InvalidRequestDataException` como soporte del flujo de solicitudes.
- Se agregaron los archivos de persistencia de solicitudes que no estaban en la lista inicial, porque el caso de uso requiere guardar y recuperar solicitudes.
- Se agregaron los dos Dockerfiles multi-etapa (Maven + JRE Java 17, ejecución con usuario sin privilegios), el `.dockerignore` y un `compose.yaml` con PostgreSQL 15, volumen persistente, healthcheck, los dos microservicios, puertos publicados y red bridge propia.
- Se reemplazaron en `HELP.md` las advertencias generadas que afirmaban que Compose no tenía servicios por instrucciones de ejecución y detención del entorno.

## Configuración de ejecución

- Inventario: puerto 8081. H2 en memoria por defecto. Para PostgreSQL configurar `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, `DB_PASSWORD` y `JPA_DIALECT`.
- JWT: reemplazar el secreto de desarrollo con `JWT_SECRET`; expiración configurable mediante `JWT_EXPIRATION_MS`.
- URL de proveedores configurable mediante `SUPPLIER_AVAILABILITY_URL`.
- Simulador: puerto 8082.
- Compose: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `JWT_SECRET` y `JWT_EXPIRATION_MS` pueden sobreescribirse desde el entorno o un archivo `.env`; los valores predeterminados son solo para desarrollo local.

## Validación y pendientes conocidos

- Se ejecutó `mvnw.cmd -B -ntp clean package` con el wrapper del repositorio: `BUILD SUCCESS`. Resultado de Surefire: inventario, 12 pruebas reportadas (11 pasaron y 1 Testcontainers se omitió); proveedor, 3 pruebas pasaron. No hubo fallos ni errores. La ejecución requirió permitir la descarga de Maven/dependencias y fijar `MAVEN_USER_HOME` y el repositorio local en `%TEMP%` por las restricciones del perfil Windows.
- Las pruebas unitarias JUnit 5/Mockito cubren alta de producto válido, normalización del código, rechazo de duplicados, cantidades inválidas, reserva de stock interno, respuesta del proveedor y rechazo cuando no hay disponibilidad suficiente.
- Las pruebas Spring Boot Test/MockMvc cubren registro, login, persistencia H2, endpoints, autorización por roles, flujo de solicitud y mensajes HTTP de validación en español. El servicio de proveedores prueba la respuesta determinista y su validación HTTP.
- `PostgreSqlPersistenceContainerTest` usa Testcontainers con PostgreSQL 15-alpine y Flyway para probar persistencia de usuarios, productos y solicitudes. En esta ejecución se omitió porque Testcontainers no encontró un daemon Docker válido: la estrategia de cliente fijada en `~/.testcontainers.properties` contactó el pipe `docker_cli`, que respondió con un identificador de motor vacío. Se debe ejecutar nuevamente con Docker disponible y una estrategia/pipe que coincida con el contexto activo; la omisión está habilitada explícitamente con `disabledWithoutDocker = true`.
- `docker compose config --quiet` pasó y `docker compose up --build -d` construyó ambas imágenes y dejó los tres servicios arriba. `postgres` figura `healthy`; inventario migró PostgreSQL con Flyway V1.
- Smoke tests dentro de `stockbridge-network`: `/actuator/health` respondió `UP`; el proveedor devolvió disponibilidad para `food` (100 unidades, 2 días); una cantidad inválida produjo HTTP 400 con mensaje en español; un endpoint protegido produjo HTTP 401 con JSON en español.
- Prueba completa inventory → supplier con producto sin stock: la solicitud terminó `APROBADA`, origen `SUPPLIER`, entrega estimada 2 días. Los datos temporales de esta prueba se eliminaron.
- Los puertos 8081, 8082 y 5432 están publicados en Compose. La solicitud HTTP desde el proceso host de este entorno se reinició; la verificación desde la red Docker funcionó, por lo que conviene confirmar acceso host desde la máquina local si se cambia el contexto Docker.
- Los archivos `mvnw`, `mvnw.cmd` y `.mvn/wrapper/maven-wrapper.properties` no necesitaron cambios: el wrapper 3.3.4 apunta a la distribución Apache Maven 3.10.0 y ejecutó correctamente al tener conectividad de descarga. El primer intento falló antes de Maven por restricción de red, no por un defecto del script.
- Permanecen avisos no bloqueantes de compatibilidad declarada por Flyway 9.22.3 con H2 2.2.224, y avisos de carga dinámica de Byte Buddy con el JDK 21 local; la compilación usa `--release 17`, y los tests y contenedores usan Java 17 en Docker.

## Índice por archivo

Este índice resume los archivos de código, recursos, pruebas y configuración del repositorio. Las entradas marcadas como existentes no fueron modificadas durante esta tarea.

### `inventory-management-service/src/main/java/org/stockbridge/inventory/`

| Archivo | Resumen |
|---|---|
| `InventoryManagementApplication.java` | Punto de entrada Spring Boot existente. |
| `domain/model/User.java` | Modelo de usuario con id, nombre, email normalizado, hash y rol; mensajes de invariantes en español. |
| `domain/model/Role.java` | Enum de autoridades `ROLE_EMPLEADO`, `ROLE_BODEGA`, `ROLE_ADMIN`. |
| `domain/model/Product.java` | Producto inmutable; valida datos, precio y stock con excepciones y mensajes en español; permite cambiar stock. |
| `domain/model/Request.java` | Solicitud inmutable; valida campos/cantidad con `InvalidRequestDataException` y transiciones con mensajes en español. |
| `domain/model/RequestStatus.java` | Estados de solicitud en español. |
| `domain/model/FulfillmentSource.java` | Distingue reservas `INTERNAL` de abastecimiento `SUPPLIER`. |
| `domain/exception/DuplicateProductException.java` | Error de código de producto duplicado. |
| `domain/exception/EmailAlreadyExistsException.java` | Error de email de usuario duplicado. |
| `domain/exception/InsufficientStockException.java` | Error de inventario insuficiente. |
| `domain/exception/InvalidCredentialsException.java` | Error de credenciales inválidas durante login. |
| `domain/exception/InvalidProductDataException.java` | Error de datos o reglas inválidas de producto. |
| `domain/exception/InvalidRequestDataException.java` | Error de comandos o datos de solicitud inválidos. |
| `domain/exception/InvalidStatusTransitionException.java` | Rechaza cambios de estado que el dominio no permite. |
| `domain/exception/ProductNotFoundException.java` | Error al no encontrar un producto. |
| `domain/exception/RequestNotFoundException.java` | Error al no encontrar una solicitud. |
| `domain/exception/SupplierIntegrationException.java` | Error de comunicación o respuesta inválida del proveedor. |
| `domain/exception/UnauthorizedRoleException.java` | Error de autorización de negocio/propiedad de solicitud. |
| `domain/exception/UserNotFoundException.java` | Error al no encontrar un usuario. |
| `application/port/in/AuthUseCase.java` | Contrato de registro y login, con comandos y resumen de usuario. |
| `application/port/in/ManageProductUseCase.java` | Contrato CRUD de productos; comandos incluyen precio y stock. |
| `application/port/in/ManageRequestUseCase.java` | Contrato para crear, consultar, aprobar, rechazar, entregar y cancelar. |
| `application/port/out/UserRepositoryPort.java` | Puerto de persistencia de usuario. |
| `application/port/out/ProductRepositoryPort.java` | Puerto de persistencia y eliminación de productos. |
| `application/port/out/RequestRepositoryPort.java` | Puerto de persistencia y consulta de solicitudes. |
| `application/port/out/SupplierAvailabilityPort.java` | Puerto para consultar disponibilidad externa. |
| `application/port/out/PasswordHasherPort.java` | Puerto Java puro para crear y verificar hashes de contraseña. |
| `application/service/AuthUseCaseImpl.java` | Valida registro/login, asigna rol de empleado y usa los puertos; mensajes de validación, duplicidad y credenciales en español. |
| `application/service/ProductUseCaseImpl.java` | Valida y coordina CRUD, duplicados, precio y stock; mensajes de dominio en español. |
| `application/service/RequestUseCaseImpl.java` | Coordina reserva interna, proveedor, persistencia y transiciones; errores y motivos de decisión en español. |
| `infrastructure/adapters/in/web/AuthController.java` | Endpoints de registro/login con textos Bean Validation en español; devuelve token JWT y resumen sin hash. |
| `infrastructure/adapters/in/web/ProductController.java` | Endpoints CRUD REST; textos de validación de producto en español. |
| `infrastructure/adapters/in/web/RequestController.java` | Endpoints REST; textos de validación de cantidad/código/motivo en español e incrementa métrica de creación. |
| `infrastructure/adapters/out/external/SupplierExternalAdapter.java` | POST con RestClient al simulador; adapta DTO y errores de integración. |
| `infrastructure/adapters/out/security/PasswordHasherAdapter.java` | Adapta BCrypt/PasswordEncoder al puerto de hash. |
| `infrastructure/adapters/out/persistence/entity/UserEntity.java` | Entidad JPA de la tabla `users`. |
| `infrastructure/adapters/out/persistence/entity/ProductEntity.java` | Entidad JPA de `products`, con precio decimal y stock entero. |
| `infrastructure/adapters/out/persistence/entity/RequestEntity.java` | Entidad JPA de la tabla `requests`. |
| `infrastructure/adapters/out/persistence/repository/SpringDataUserRepository.java` | JpaRepository con consultas por email. |
| `infrastructure/adapters/out/persistence/repository/SpringDataProductRepository.java` | JpaRepository con consulta por código. |
| `infrastructure/adapters/out/persistence/repository/SpringDataRequestRepository.java` | JpaRepository de solicitudes. |
| `infrastructure/adapters/out/persistence/mapper/UserMapper.java` | MapStruct entre usuario y entidad. |
| `infrastructure/adapters/out/persistence/mapper/ProductMapper.java` | MapStruct entre producto y entidad; ignora el método de dominio `withStock` como propiedad. |
| `infrastructure/adapters/out/persistence/mapper/RequestMapper.java` | MapStruct entre solicitud y entidad; ignora el método de dominio `reject` como propiedad. |
| `infrastructure/adapters/out/persistence/UserPersistenceAdapter.java` | Implementa el puerto de usuario sobre Spring Data. |
| `infrastructure/adapters/out/persistence/ProductPersistenceAdapter.java` | Implementa el puerto de producto sobre Spring Data. |
| `infrastructure/adapters/out/persistence/RequestPersistenceAdapter.java` | Implementa el puerto de solicitud sobre Spring Data. |
| `infrastructure/config/UseCaseConfig.java` | Declara beans de casos de uso e inyecta puertos y Clock. |
| `infrastructure/config/SecurityConfig.java` | Configura seguridad stateless, BCrypt y permisos por autoridad; las respuestas 401/403 están en español. |
| `infrastructure/security/JwtProvider.java` | Emite y valida JWT firmados con secreto configurable. |
| `infrastructure/security/JwtAuthenticationFilter.java` | Lee Bearer token y crea autenticación con email y rol. |
| `infrastructure/exception/GlobalExceptionHandler.java` | Convierte excepciones de dominio, validación, JSON, integridad y errores inesperados a HTTP con etiquetas y mensajes en español. |
| `infrastructure/observability/MetricsConfig.java` | Registra contadores Micrometer. |
| `infrastructure/observability/StockBridgeMetrics.java` | Expone incrementos para solicitudes creadas y consultas al proveedor. |

### Recursos de `inventory-management-service`

| Archivo | Resumen |
|---|---|
| `src/main/resources/application.yml` | Puerto 8081, H2 por defecto, configuración por entorno para PostgreSQL, JWT, proveedor, Flyway y Actuator. Reemplaza el `application.properties` existente. |
| `src/main/resources/db/migration/V1__Initial_schema.sql` | DDL de usuarios, productos y solicitudes, con claves foráneas, checks e índices; estados y roles coinciden con los enums. |

### `supplier-availability-service/src/main/java/org/stockbridge/supplier/`

| Archivo | Resumen |
|---|---|
| `SupplierAvailabilityApplication.java` | Punto de entrada Spring Boot existente. |
| `dto/AvailabilityRequest.java` | Record de entrada con `productCode`, `quantityRequested` y `category`; valida campos, tamaños y cantidad mínima con mensajes en español. |
| `dto/AvailabilityResponse.java` | Record de salida con `available`, `availableQuantity` y `estimatedDeliveryDays`. |
| `controller/SupplierController.java` | Endpoint POST con reglas de disponibilidad deterministas por categoría. |
| `exception/GlobalExceptionHandler.java` | Formatea errores HTTP de validación, JSON, 404 y 500 en español. |

### Recursos de `supplier-availability-service`

| Archivo | Resumen |
|---|---|
| `src/main/resources/application.yml` | Nombre de aplicación y puerto 8082. Reemplaza el `application.properties` existente. |

### Archivos existentes de build, pruebas y soporte

| Archivo | Estado y resumen |
|---|---|
| `pom.xml` | Existente, no modificado; declara Spring Boot 3.2.4, Java 17 y los dos módulos. |
| `inventory-management-service/pom.xml` | Existente, no modificado; dependencias de web, JPA, Flyway, seguridad, JWT, MapStruct, Lombok, OpenAPI y pruebas. |
| `supplier-availability-service/pom.xml` | Existente, no modificado; dependencias MVC, validación y pruebas. |
| `inventory-management-service/src/test/java/org/stockbridge/inventory/application/service/ProductUseCaseImplTest.java` | Pruebas unitarias JUnit 5/Mockito: creación válida de producto y rechazo de código duplicado. Pasaron 2 pruebas. |
| `inventory-management-service/src/test/java/org/stockbridge/inventory/application/service/RequestUseCaseImplTest.java` | Pruebas unitarias JUnit 5/Mockito: cantidad inválida, reserva interna, aprobación del proveedor y rechazo por falta de stock. Pasaron 4 pruebas. |
| `inventory-management-service/src/test/java/org/stockbridge/inventory/infrastructure/InventoryManagementWebIntegrationTest.java` | Integración Spring Boot Test/MockMvc con H2: autenticación, persistencia, endpoints, roles y flujo de solicitud. Pasaron 4 pruebas. |
| `inventory-management-service/src/test/java/org/stockbridge/inventory/infrastructure/PostgreSqlPersistenceContainerTest.java` | Integración con PostgreSQL 15-alpine mediante Testcontainers; verifica Flyway y persistencia de usuario, producto y solicitud. Compiló, pero esta ejecución omitió la prueba porque Testcontainers no detectó Docker. |
| `inventory-management-service/src/test/java/org/stockbridge/inventory/InventoryManagementApplicationTests.java` | Prueba existente de carga del contexto Spring; pasó con Maven (`1 prueba, 0 fallos`). |
| `supplier-availability-service/src/test/java/org/stockbridge/supplier/SupplierControllerWebTest.java` | Pruebas Spring MVC/MockMvc para disponibilidad determinista y respuesta HTTP de validación en español. Pasaron 2 pruebas. |
| `supplier-availability-service/src/test/java/org/stockbridge/supplier/SupplierAvailabilityApplicationTests.java` | Prueba existente de carga del contexto Spring; pasó con Maven (`1 prueba, 0 fallos`). |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Wrapper Maven 3.3.4 con distribución Apache Maven 3.10.0; revisado y ejecutado sin cambios. |
| `compose.yaml` | Orquesta PostgreSQL 15, inventario y proveedores, con variables de entorno, puertos, volumen, healthcheck y red propia. `docker compose up --build -d` verificado. |
| `inventory-management-service/Dockerfile` | Build multi-etapa Maven y runtime JRE 17; usuario sin privilegios; expone 8081. |
| `supplier-availability-service/Dockerfile` | Build multi-etapa Maven y runtime JRE 17; usuario sin privilegios; expone 8082. |
| `.dockerignore` | Excluye artefactos `target`, configuración de IDE, Git y archivos `.env` del contexto Docker. |
| `HELP.md` | Guía generada actualizada con el comando Compose y descripción del entorno desplegado. |
| `.gitignore`, `.gitattributes` | Existentes, no modificados; reglas de exclusión y finales de línea. |
