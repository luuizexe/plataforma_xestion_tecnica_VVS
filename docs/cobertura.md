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

Estado medido en el commit `4d8c8e7` de la rama `test/usuarios-auth-cobertura-mutacion` (23/11/2026), después de fusionar `main`. Incluye las pruebas de componentes (Luis), usuarios y autenticación (Javi) y plantillas (Mario). **Las pruebas de búsqueda (Nico) todavía no están integradas**, así que este documento se tiene que regenerar cuando lleguen.

En las tres áreas integradas, los errores encontrados **no se han corregido**: el código de producción es el original y las pruebas que los detectan están desactivadas con `@Disabled`, con el motivo del fallo. Se ejecutan 551 pruebas (447 unitarias y 104 de integración), de las que 62 están desactivadas por esa razón (41 unitarias y 21 de integración). Las líneas que solo recorren esas pruebas aparecen como no cubiertas.

## Resumen

| Ejecución | Líneas | Ramas | Instrucciones |
|---|---|---|---|
| Solo unidad | 78,4 % (809/1032) | 55,1 % (205/372) | 72,6 % |
| Solo integración | 48,1 % (496/1032) | 31,5 % (117/372) | 43,1 % |
| **Total** | **79,9 % (825/1032)** | **56,2 % (209/372)** | **73,9 %** |

Por área de trabajo:

| Área | Responsable | Líneas | Ramas |
|---|---|---|---|
| Usuarios y autenticación | Javi | 98,6 % (139/141) | 91,9 % (57/62) |
| Componentes y campos | Luis | 94,3 % (266/282) | 89,5 % (68/76) |
| Plantillas | Mario | 84,0 % (137/163) | 65,4 % (34/52) |
| Búsqueda y caché | Nico | 55,3 % (197/356) | 22,4 % (38/170) |
| Código común (arranque, errores, DAO factory) | — | 95,6 % (86/90) | 100 % (12/12) |

## Qué significan estos números

- **El total no llega al 100 % sobre todo porque faltan las pruebas de búsqueda.** Esa área suma 159 de las 207 líneas sin cubrir. Solo se ejecuta con las tres pruebas antiguas de `SearchResourceTest`.
- **Casi todo lo que falta en las otras áreas es código que solo recorren pruebas desactivadas por errores conocidos.** En usuarios y autenticación, las 2 líneas y las 5 ramas sin cubrir de `AuthorizationManager` son exactamente los casos de las pruebas desactivadas. En plantillas, `modifyFieldTemplate` y `removeTemplateField` no se ejecutan en ninguna prueba activa. Cuando se corrijan los errores y se reactiven las pruebas, esas líneas quedarán cubiertas sin escribir pruebas nuevas.
- **La cobertura de ramas está bastante por debajo de la de líneas (56 % frente a 80 %).** Las clases de búsqueda (`TestIn`, `TestString`, `TestNumber.Operator`) son casi todo `switch` y condiciones encadenadas: con una sola prueba se ejecutan muchas líneas, pero solo una de las ramas.
- **Las pruebas de integración aportan poco a la cobertura, pero mucho a la detección de errores.** Ejecutadas solas cubren el 48 %, y sobre las unitarias solo añaden 16 líneas (`ComponentResource`, `OpenApiResource` y `JSONString`). Aun así, encontraron dos fallos ([#17](https://github.com/luuizexe/plataforma_xestion_tecnica_VVS/issues/17) y [#18](https://github.com/luuizexe/plataforma_xestion_tecnica_VVS/issues/18)) **en líneas que las pruebas unitarias ya cubrían**. El error de clave foránea al borrar un usuario solo aparece con una base de datos real. El login con un token caducado solo falla al combinar dos condiciones que las pruebas unitarias comprobaban por separado.
- **El 100 % de cobertura no garantiza que el código esté bien probado.** Una línea cuenta como cubierta aunque ningún assert compruebe su resultado. La calidad de las pruebas se mide aparte con pruebas de mutación ([mutacion.md](mutacion.md)).

## Justificación clase por clase

### Clases al 100 % de líneas y ramas

- **Usuarios y autenticación:** `UsersResource`, `User`, `Token`, `Roles`, `IllegalActionForUserException`, `UserAlreadyExistsException`, `UserNotFoundException`.
- **Componentes:** `DatetimeField`, `LinkField`, `NumberField`, `TextField`, `ComponentFieldInvalidValueException`, `ComponentFieldIsMandatoryException`, `ComponentFieldNotFoundException`, `ComponentNotFoundException`.
- **Plantillas:** `FieldTypes`, `TemplateAlreadyExistsException`, `TemplateExceptionTemplate`, `TemplateNotFoundException`, `TemplateStillInUseException`, `TemplateFieldAlreadyExistsException`, `TemplateFieldNotFoundException`.
- **Búsqueda:** `JSONString`, `ParseSearchException`, `search.Properties`.
- **Común:** `DataLoader`, `OpenApiResource`, `SQLDaoFactoryUtil`, `RESTException`, `RESTExceptionSerializable`.

### Clases que no llegan al 100 % por motivos justificados

Esto es lo que queda sin cubrir y no se va a cubrir:

| Clase | Cobertura | Qué falta | Por qué no se cubre |
|---|---|---|---|
| `Application` | 78 % líneas (7/9) | `main()`, líneas 25-26 | Es el punto de entrada que llama la JVM al arrancar el `.jar`. Las pruebas levantan el contexto con `@SpringBootTest`, que no pasa por `main`. Ejecutarlo desde una prueba arrancaría un segundo servidor en el puerto 80 con la configuración de PostgreSQL de producción. Lo que hace `main` (crear el contexto y registrar el servlet de Jersey) sí se ejecuta en todas las pruebas de integración. |
| `RESTExceptionMapper` | 82 % líneas (9/11), 100 % ramas | `catch (IOException)`, líneas 22-23 | Código defensivo que no se puede alcanzar. Solo se ejecutaría si Jackson no pudiera serializar `RESTExceptionSerializable`, que tiene dos campos (`Integer` y `String`) con sus getters, sin ciclos ni tipos especiales. El resto de ramas (excepción REST, excepción de JAX-RS y cualquier otra excepción) se prueban en `RESTExceptionMapperTest`. |

Para no dejar en esta lista cosas que sí se podían probar, se añadieron dos pruebas a las clases comunes, que no pertenecen a ningún área:

- `RESTExceptionMapperTest`, que cubre las tres ramas del mapper.
- `DataLoaderIT`, que vuelve a ejecutar la carga inicial y comprueba que no duplica el usuario `root` ni las plantillas por defecto. Así se cubren las ramas que solo se dan en producción, donde la base de datos conserva los datos entre reinicios (`ddl-auto=update`). En las pruebas la base de datos se crea vacía en cada arranque.

### Clases sin cubrir por pruebas desactivadas

Estas líneas tienen pruebas escritas, pero están desactivadas porque el código tiene un error. Se cubrirán en cuanto se corrija el error y se quite el `@Disabled`.

**Usuarios y autenticación (Javi), 98,6 % de líneas:**

| Clase | Qué falta | Pruebas desactivadas que lo cubren |
|---|---|---|
| `AuthorizationManager` (90 % líneas, 82 % ramas) | La excepción de una petición sin token a una ruta protegida (línea 39) y la de un token desconocido (línea 57), con sus ramas en las líneas 38 y 42. En la línea 45, la rama de un usuario sin rol. | `testRequestWithoutToken`, `testRequestWithEmptyToken` y `testRequestWithUnknownToken` (responde 404 en lugar de 401), y las pruebas `testNoRole*` (`NullPointerException` con usuarios sin rol). |

El resto de pruebas desactivadas de esta área no dejan líneas sin cubrir, porque el código que prueban se ejecuta también en otras pruebas: fallan por lo que comprueban sus asserts, no por no llegar a la línea.

**Plantillas (Mario), 84,0 % de líneas:**

| Clase | Qué falta | Pruebas desactivadas que lo cubren |
|---|---|---|
| `TemplateResource` (72 % líneas, 64 % ramas) | `modifyFieldTemplate` (líneas 271-286) y `removeTemplateField` (líneas 315-334) enteros, y las ramas de `templateDoesntContainsField` (líneas 205 y 207). | Las pruebas de modificar y borrar un campo de `TemplateResourceTest`: la condición invertida de `templateDoesntContainsField` hace que se lance `TemplateFieldNotFoundException` aunque el campo exista. |

**Componentes (Luis):**

| Clase | Qué falta | Pruebas desactivadas que lo cubren |
|---|---|---|
| `ComponentResource` | En `addComponent`, la rama de una plantilla nula (líneas 97 y 99). | ISSUE #1 de `ComponentResourceTest` (`NullPointerException` al construir el mensaje). |
| `ComponentResource` | En `modifyFieldComponent`, los `catch (EntityNotFoundException)` de `LINK` (líneas 310-311) y de `NUMBER` (líneas 318-319). Con el código actual no se pueden alcanzar: `getReferenceById` no comprueba que el componente exista y `new BigDecimal` lanza `NumberFormatException`. | ISSUE #3 e ISSUE #4 de `ComponentResourceTest`. |

### Clases pendientes de las pruebas de su área

Ninguna de estas faltas es código inalcanzable. Se cubrirán con las pruebas de cada área, y entonces habrá que regenerar este documento.

**Componentes (Luis), 94,3 % de líneas:**

| Clase | Qué falta |
|---|---|
| `ComponentResource` (91 % líneas, 91 % ramas) | Además de lo anterior: en `modifyComponent`, el caso `NUMBER` del `switch` (líneas 207-212). En `removeComponentField`, el `catch (EntityNotFoundException)` (líneas 364-365). |
| `FieldDeserializer` (94 % líneas) | El `catch (IllegalArgumentException)` de contenido con formato incorrecto (líneas 83-84). |
| `FieldDeserializer.FieldDeserializerJacksonException` (75 % líneas) | Los getters `getLocation()` y `getProcessor()` (líneas 31 y 41), que solo llama Jackson al construir mensajes de error. |
| `Component`, `Field` (75 % ramas) | En `equals`, la rama en la que se compara con un objeto de otro tipo. |
| `ComponentExceptionTemplate` (50 % ramas) | El mensaje cuando el componente no tiene nombre (`name == null`). |

**Plantillas (Mario):**

| Clase | Qué falta |
|---|---|
| `TemplateResource` | En `getAllTemplates`, la rama de `skip` nulo con paginación (línea 104). En `addFieldTemplate`, un nombre de campo que ya existe pero en otra plantilla (línea 245). |
| `Template`, `TemplateField` (75 % ramas) | En `equals`, la rama en la que se compara con un objeto de otro tipo. |

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
