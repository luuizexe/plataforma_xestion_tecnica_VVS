# Análisis de cobertura (JaCoCo)

## Cómo se mide

La cobertura se mide con JaCoCo 0.8.15, configurado en `pleste-server/pom.xml`. Al ejecutar `mvn verify` en `pleste-server` se generan tres informes:

| Informe | Pruebas que incluye | Ubicación |
|---|---|---|
| Unidad | `*Test.java`, ejecutadas por Surefire | `target/site/jacoco/index.html` |
| Integración | `*IT.java`, ejecutadas por Failsafe | `target/site/jacoco-it/index.html` |
| Total | Unión de las dos ejecuciones | `target/site/jacoco-merged/index.html` |

Salvo que se diga lo contrario, las cifras de este documento son del informe **total**.

Fuera de la medición quedan:

- **Las clases que genera ANTLR a partir de `Find.g4`** (`FindLexer`, `FindParser`, `FindBaseListener`…), excluidas en el `pom.xml` con el patrón `gal/udc/fic/prperez/pleste/service/Find*`. Es código generado: no lo escribimos ni lo mantenemos nosotros, y probarlo sería probar ANTLR. La clase propia `search.FindListener` sí se mide.
- **Las interfaces de los DAO** (`SQLUserDao`, `SQLTokenDao`…), que no aparecen porque no tienen código propio: Spring Data JPA genera su implementación en tiempo de ejecución.

Estado medido en el commit `37b7c15` de la rama `test/usuarios-auth-cobertura-mutacion` (26/10/2026). En ese momento, en `main` solo estaban las pruebas de componentes (Luis) y en esta rama las de usuarios y autenticación (Javi). **Las pruebas de plantillas (Mario) y de búsqueda (Nico) todavía no estaban integradas**, así que este documento se tiene que regenerar cuando lleguen.

## Resumen

| Ejecución | Líneas | Ramas | Instrucciones |
|---|---|---|---|
| Solo unidad | 73,8 % (783/1061) | 51,8 % (205/396) | 67,0 % |
| Solo integración | 47,0 % (499/1061) | 31,6 % (125/396) | 41,6 % |
| **Total** | **76,5 % (812/1061)** | **55,1 % (218/396)** | **69,7 %** |

Por área de trabajo:

| Área | Responsable | Líneas | Ramas |
|---|---|---|---|
| Usuarios y autenticación | Javi | **100 %** (163/163) | **100 %** (80/80) |
| Componentes y campos | Luis | 94,4 % (271/287) | 91,5 % (75/82) |
| Plantillas | Mario | 57,1 % (93/163) | 25,0 % (13/52) |
| Búsqueda y caché | Nico | 55,3 % (197/356) | 22,4 % (38/170) |
| Código común (arranque, errores, DAO factory) | — | 95,7 % (88/92) | 100 % (12/12) |

## Qué significan estos números

- **El total no llega al 100 % sobre todo porque faltan las pruebas de dos áreas.** Plantillas y búsqueda suman 229 de las 249 líneas sin cubrir. Solo se ejecutan de forma indirecta: las plantillas al crear componentes, y la búsqueda con las tres pruebas antiguas de `SearchResourceTest`. En las áreas que ya tienen sus pruebas, la cobertura es del 100 % (usuarios), del 94 % (componentes) y del 96 % (código común).
- **La cobertura de ramas está bastante por debajo de la de líneas (55 % frente a 76 %).** Las clases de búsqueda (`TestIn`, `TestString`, `TestNumber.Operator`) son casi todo `switch` y condiciones encadenadas: con una sola prueba se ejecutan muchas líneas, pero solo una de las ramas.
- **Las pruebas de integración aportan poco a la cobertura, pero mucho a la detección de errores.** Ejecutadas solas cubren el 47 %, y sobre las unitarias solo añaden 29 líneas (`ComponentResource`, `TemplateResource`, `OpenApiResource`…). Aun así, encontraron dos fallos ([#17](https://github.com/luuizexe/plataforma_xestion_tecnica_VVS/issues/17) y [#18](https://github.com/luuizexe/plataforma_xestion_tecnica_VVS/issues/18)) **en líneas que las pruebas unitarias ya cubrían al 100 %**. El error de clave foránea al borrar un usuario solo aparece con una base de datos real. El login con un token caducado solo falla al combinar dos condiciones que las pruebas unitarias comprobaban por separado.
- **El 100 % de cobertura no garantiza que el código esté bien probado.** Una línea cuenta como cubierta aunque ningún assert compruebe su resultado. La calidad de las pruebas se mide aparte con pruebas de mutación (PIT).

## Justificación clase por clase

### Clases al 100 % de líneas y ramas

- **Usuarios y autenticación:** `UsersResource`, `AuthorizationManager`, `User`, `Token`, `Roles`, `IllegalActionForUserException`, `InvalidUserDataException`, `UnauthenticatedUserException`, `UserAlreadyExistsException`, `UserNotFoundException`.
- **Componentes:** `DatetimeField`, `LinkField`, `NumberField`, `TextField`, `ComponentFieldInvalidValueException`, `ComponentFieldIsMandatoryException`, `ComponentFieldNotFoundException`, `ComponentNotFoundException`.
- **Plantillas:** `FieldTypes`, `TemplateExceptionTemplate`, `TemplateNotFoundException`, `TemplateStillInUseException`.
- **Búsqueda:** `JSONString`, `ParseSearchException`, `search.Properties`.
- **Común:** `DataLoader`, `OpenApiResource`, `SQLDaoFactoryUtil`, `RESTException`, `RESTExceptionSerializable`, `InvalidPaginationException`.

### Clases que no llegan al 100 % por motivos justificados

Esto es lo que queda sin cubrir y no se va a cubrir:

| Clase | Cobertura | Qué falta | Por qué no se cubre |
|---|---|---|---|
| `Application` | 78 % líneas (7/9) | `main()`, líneas 25-26 | Es el punto de entrada que llama la JVM al arrancar el `.jar`. Las pruebas levantan el contexto con `@SpringBootTest`, que no pasa por `main`. Ejecutarlo desde una prueba arrancaría un segundo servidor en el puerto 80 con la configuración de PostgreSQL de producción. Lo que hace `main` (crear el contexto y registrar el servlet de Jersey) sí se ejecuta en todas las pruebas de integración. |
| `RESTExceptionMapper` | 82 % líneas (9/11), 100 % ramas | `catch (IOException)`, líneas 22-23 | Código defensivo que no se puede alcanzar. Solo se ejecutaría si Jackson no pudiera serializar `RESTExceptionSerializable`, que tiene dos campos (`Integer` y `String`) con sus getters, sin ciclos ni tipos especiales. El resto de ramas (excepción REST, excepción de JAX-RS y cualquier otra excepción) se prueban en `RESTExceptionMapperTest`. |

Para no dejar en esta lista cosas que sí se podían probar, se añadieron dos pruebas a las clases comunes, que no pertenecen a ningún área:

- `RESTExceptionMapperTest`, que cubre las tres ramas del mapper.
- `DataLoaderIT`, que vuelve a ejecutar la carga inicial y comprueba que no duplica el usuario `root` ni las plantillas por defecto. Así se cubren las ramas que solo se dan en producción, donde la base de datos conserva los datos entre reinicios (`ddl-auto=update`). En las pruebas la base de datos se crea vacía en cada arranque.

### Clases pendientes de las pruebas de su área

Ninguna de estas faltas es código inalcanzable. Se cubrirán con las pruebas de cada área, y entonces habrá que regenerar este documento.

**Componentes (Luis), 94,4 % de líneas:**

| Clase | Qué falta |
|---|---|
| `ComponentResource` (91 % líneas, 94 % ramas) | En `modifyComponent`, el caso `NUMBER` del `switch` (líneas 209-214). En `modifyFieldComponent`, los `catch` de valores no válidos para `LINK` (líneas 316-317: id que no es un número o componente que no existe) y para `NUMBER` (líneas 324-325). En `removeComponentField`, el `catch (EntityNotFoundException)` (líneas 370-371). En `addComponent`, la plantilla con id nulo (línea 100). |
| `FieldDeserializer` (94 % líneas) | El `catch (IllegalArgumentException)` de contenido con formato incorrecto (líneas 83-84). |
| `FieldDeserializer.FieldDeserializerJacksonException` (75 % líneas) | Los getters `getLocation()` y `getProcessor()`, que solo llama Jackson al construir mensajes de error. |
| `Component`, `Field` (75 % ramas) | En `equals`, la rama en la que se compara con un objeto de otro tipo. |
| `ComponentExceptionTemplate` (50 % ramas) | El mensaje cuando el componente no tiene nombre (`name == null`). |

**Plantillas (Mario), 57,1 % de líneas:**

| Clase | Qué falta |
|---|---|
| `TemplateResource` (39 % líneas, 20 % ramas) | La mayoría de endpoints solo se ejecutan desde `DataLoader` y desde las pruebas de componentes: paginación, modificación, borrado de una plantilla en uso, y alta, modificación y borrado de sus campos. |
| `Template` (88 % líneas, 25 % ramas), `TemplateField` (85 % líneas, 75 % ramas) | `equals` y `hashCode` (y el constructor `TemplateField(Long)`) no se ejecutan en ninguna prueba. |
| `TemplateAlreadyExistsException`, `TemplateFieldAlreadyExistsException`, `TemplateFieldNotFoundException` (0 %) | Ninguna prueba provoca estos errores. |

**Búsqueda y caché (Nico), 55,3 % de líneas:**

| Clase | Qué falta |
|---|---|
| `SearchResource` (56 % líneas, 42 % ramas) | La paginación de los resultados de componentes y usuarios, las respuestas que se sirven desde la caché LRU, los `catch (IndexOutOfBoundsException)` y el vaciado de las cachés (`DELETE /search/*/cache`). |
| `search.EntityFactory` (0 %) | Ninguna prueba la ejecuta. |
| `search.TestIn`, `TestString`, `TestNumber`, `TestNumber.Operator` (34-45 % líneas, 0-19 % ramas) | La mayoría de operadores de comparación y de los tipos de campo. |
| `search.AndNode`, `OrNode`, `BinaryNode`, `RootNode`, `Node` (40-67 % líneas) | Ninguna prueba usa consultas con `and` u `or`, así que esos nodos ni se construyen ni se evalúan (`matches`). Tampoco se ejecutan los `toString` ni el `catch (NullPointerException)` de `RootNode`. |
| `search.FindListener` (87 % líneas) | Los tres `catch (IndexOutOfBoundsException)` que convierten una consulta mal formada en `ParseSearchException`. |
| `JSONDatetime` (92 % líneas) | `getDatetime()` (línea 27). |

## Cómo regenerar las cifras

```bash
cd pleste-server
mvn clean verify
```

Después hay que abrir `target/site/jacoco-merged/index.html`. Para las tablas de este documento se usó `target/site/jacoco-merged/jacoco.csv`.
