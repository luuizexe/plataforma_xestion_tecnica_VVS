# Pruebas de mutación (PIT)

## Configuración

PIT 1.17.0 está configurado en `pleste-server/pom.xml`, con `pitest-junit5-plugin` 1.2.1 y `junit-platform-launcher` para lanzar las pruebas de JUnit 5. No está ligado al ciclo de vida de Maven: hay que ejecutarlo a mano porque tarda varios minutos.

```bash
cd pleste-server
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

El informe queda en `target/pit-reports/index.html`, y el detalle de cada mutante en `target/pit-reports/mutations.csv`.

- **Clases mutadas:** todo `gal.udc.fic.prperez.pleste.service.*` excepto las clases que genera ANTLR (`Find*`) y `Application`, por los mismos motivos que en la cobertura ([cobertura.md](cobertura.md)).
- **Pruebas:** solo las unitarias (`*Test`). Las de integración (`*IT`) quedan fuera porque levantan Spring y el servidor HTTP para cada mutante, y el análisis pasaría de minutos a horas. Por eso el código que solo ejecutan las pruebas de integración aparece aquí como "sin cobertura".
- **Pruebas desactivadas:** PIT no ejecuta las pruebas marcadas con `@Disabled`, las que detectan errores todavía sin corregir. El código que solo recorren ellas aparece también como "sin cobertura".
- **Mutadores:** el conjunto por defecto de PIT. Incluye, entre otros:
  - Negar condiciones y cambiar los límites de las comparaciones.
  - Eliminar llamadas a métodos `void`.
  - Devolver `null`, vacío, `true`, `false` o `0` en lugar del valor real.

## Cómo leer los resultados

Para cada mutante (un pequeño cambio en el código), PIT ejecuta las pruebas que pasan por esa línea:

- **Muerto:** alguna prueba falla. Las pruebas detectan ese cambio.
- **Superviviente:** todas las pruebas siguen pasando. La línea se ejecuta, pero ningún assert comprueba su efecto.
- **Sin cobertura:** ninguna prueba ejecuta la línea.

Se usan dos indicadores:

- **Puntuación de mutación** = muertos / generados.
- **Fuerza de las pruebas** = muertos / (generados − sin cobertura). Mide lo buenas que son las pruebas que existen, sin penalizar el código sin probar, que ya se refleja en la cobertura.

## Resultados globales

Estado medido en el commit `4d8c8e7` de la rama `test/usuarios-auth-cobertura-mutacion` (23/11/2026), después de fusionar `main`. Incluye las pruebas de usuarios (Javi), componentes (Luis) y plantillas (Mario). **Las de búsqueda (Nico) todavía no están integradas.** El código de producción es el original, sin corregir los errores encontrados.

| Ejecución | Generados | Muertos | Supervivientes | Sin cobertura | Puntuación | Fuerza |
|---|---|---|---|---|---|---|
| Primera ejecución (26/10, con las correcciones y sin plantillas) | 415 | 200 | 72 | 143 | 48 % | 74 % |
| Tras añadir pruebas (26/10, con las correcciones y sin plantillas) | 415 | 222 | 51 | 142 | 53 % | 81 % |
| **Actual (código original, con plantillas)** | **397** | **229** | **54** | **114** | **58 %** | **81 %** |

Las dos primeras filas se midieron cuando la rama todavía tenía las correcciones de usuarios y autenticación, que se han retirado. Por eso hay menos mutantes generados: el código de esas correcciones ya no existe.

Por área, en la ejecución actual:

| Área | Generados | Muertos | Supervivientes | Sin cobertura | Puntuación | Fuerza |
|---|---|---|---|---|---|---|
| Usuarios y autenticación (Javi) | 59 | 57 | 1 | 1 | **96,6 %** | **98,3 %** |
| Componentes (Luis) | 95 | 72 | 10 | 13 | 75,8 % | 87,8 % |
| Plantillas (Mario) | 68 | 50 | 5 | 13 | 73,5 % | 90,9 % |
| Búsqueda (Nico) | 143 | 19 | 38 | 86 | 13,3 % | 33,3 % |
| Código común | 32 | 31 | 0 | 1 | 96,9 % | 100 % |

## Usuarios, autenticación y código común

### Hallazgos y pruebas nuevas

En la primera ejecución sobrevivieron 22 mutantes en estas clases. Se añadieron pruebas para todos los que se podían matar:

| Clase | Antes | Después | Qué revelaba el mutante | Prueba añadida |
|---|---|---|---|---|
| `UsersResource` | 44/45 | 45/45 | Con `setUser` devolviendo `null`, ninguna prueba fallaba: no se comprobaba el valor devuelto. | `UsersResourceTest.testSetUserReturnsSavedUser` |
| `DataLoader` | 0/17 | 17/17 | Las pruebas de Spring ejecutaban la carga inicial al arrancar, pero ninguna comprobaba qué se cargaba. Se podían quitar las condiciones `if` o las llamadas a `setUsername`, `setPassword`, `setRole` o `getFields().add(...)` sin que nada fallara. | `DataLoaderTest` (con mocks): usuario `root` (nombre, email, contraseña y rol), las tres plantillas con su descripción y sus campos, y que no se cree nada si ya existe. |
| `SQLDaoFactoryUtil` | 4/6 | 6/6 | `getSqlTemplateFieldDao` y `getSqlUserDao` podían devolver `null`: ninguna prueba unitaria los usaba. | `SQLDaoFactoryUtilTest` |

Las cifras de `UsersResource` de esta tabla son de cuando tenía las correcciones. Con el código original, PIT genera 30 mutantes y las pruebas activas los matan todos. `DataLoader` (17/17), `SQLDaoFactoryUtil` (6/6), `User` (5/5) y `RESTExceptionMapper` (4/4) siguen igual.

### Mutantes que quedan y su justificación

| Clase | Mutante | Estado | Justificación |
|---|---|---|---|
| `Token.hashCode` (línea 60) | Devolver `0` en lugar de `Objects.hashCode(id)` | Superviviente | **Mutante equivalente para cualquier prueba de comportamiento.** Con una constante como `0` se sigue cumpliendo el contrato con `equals`: dos tokens iguales tienen el mismo hash. El contrato no obliga a que dos tokens distintos tengan hashes distintos, así que una prueba que lo exigiera estaría probando la implementación y no el comportamiento. |
| `AuthorizationManager.filter` (línea 39) | Negar `token == null` al construir el mensaje de error | Sin cobertura | Es la excepción de una petición sin token a una ruta protegida. Solo la ejecutan `testRequestWithoutToken` y `testRequestWithEmptyToken`, desactivadas porque el código responde 404 en lugar de 401. Se cubrirá al corregir el error y reactivarlas. |
| `OpenApiResource.getOpenApi` (línea 26) | Devolver `null` | Sin cobertura | Solo se ejecuta sirviendo el documento OpenAPI por HTTP. Necesita el `ServletConfig` y la `Application` de Jersey, que se inyectan en tiempo de ejecución. Lo prueba `AuthorizationIT.testOpenApiWithoutToken` (responde 200 sin token), que PIT no ejecuta por ser de integración. |

## Supervivientes en otras áreas

Se dejan anotados para que cada responsable añada las pruebas. **Hay que volver a ejecutar PIT cuando estén integradas las pruebas de búsqueda**, porque las cifras de esa área cambiarán por completo.

**Componentes (Luis): 10 supervivientes y 13 sin cobertura.**

| Clase y línea | Mutante | Qué falta comprobar |
|---|---|---|
| `ComponentResource.addComponent` (99) | Negar `getTemplate() == null` al construir el mensaje | El id de la plantilla en el mensaje de `TemplateNotFoundException`. |
| `ComponentResource.addComponent` (120) | Negar `isFieldMissing(field)` | Qué campos obligatorios aparecen en el mensaje de `ComponentFieldIsMandatoryException`. |
| `ComponentResource.getComponent` (167) | Eliminar `setTemplate(new Template(id))` | Que la plantilla del componente devuelto solo lleve el id. |
| `ComponentResource.getFieldsComponent` (268) | Devolver `null` en la conversión de cada campo | El contenido de la lista de campos devuelta, no solo su tamaño. |
| `ComponentResource.modifyComponent` (185, 189, 191) | Eliminar `setDescription`, eliminar `setContent` de los campos `TEXT` y que el filtro de campos devuelva siempre `true` | Que la modificación guarde la descripción y el contenido de cada campo en su campo correspondiente. |
| `Component.hashCode` (86), `Field.hashCode` (75) | Devolver `0` | Mutantes equivalentes por el mismo motivo que `Token.hashCode`. |
| `ComponentExceptionTemplate` (9) | Negar `name != null` | El mensaje de error cuando el componente no tiene nombre. |

Los 13 sin cobertura son los casos `DATETIME`, `LINK` y `NUMBER` de `modifyComponent`, la rama de `equals` con un objeto de otro tipo en `Component` y `Field`, y los getters de `FieldDeserializerJacksonException`.

**Plantillas (Mario): 5 supervivientes y 13 sin cobertura.**

| Clase y línea | Mutante | Qué falta comprobar |
|---|---|---|
| `TemplateResource.getAllTemplates` (104) | Negar `skip == null` | Una página con `skip` distinto de 0: la prueba actual usa `skip = 0`, que da el mismo resultado con y sin el mutante. |
| `TemplateResource.getTemplateComponents` (195) | Eliminar `setTemplate(new Template(id))` | Que la plantilla de cada componente devuelto solo lleve el id. |
| `TemplateResource.templateDoesntContainsField` (207) | Que `anyMatch` devuelva siempre `true` | Que un campo de otra plantilla no se considere parte de esta. |
| `Template.hashCode` (73), `TemplateField.hashCode` (70) | Devolver `0` | Mutantes equivalentes por el mismo motivo que `Token.hashCode`. |

De los 13 sin cobertura, 11 son de `modifyFieldTemplate` y `removeTemplateField`, que solo ejecutan pruebas desactivadas por la condición invertida de `templateDoesntContainsField`. Los otros 2 son la rama de `equals` con un objeto de otro tipo en `Template` y `TemplateField`.

**Búsqueda (Nico): 38 supervivientes y 86 sin cobertura.**

- **`search.FindListener` (10 supervivientes):** se pueden eliminar las llamadas que añaden nodos al árbol (`appendNode`, `enterExpAnd`, `enterExpOr`, `enterTestIn`…) sin que falle ninguna prueba. Las pruebas actuales no comprueban la estructura del árbol que se genera.
- **`search.TestIn`, `TestString`, `TestNumber.Operator` (11 supervivientes):** se pueden negar las comparaciones de `matches` y `getOperator`.
- **`search.AndNode`, `OrNode`, `RootNode`, `Node` (7 supervivientes):** se pueden cambiar `&&`/`||` o devolver siempre `true`/`false` en `matches`.
- **`SearchResource` (8 supervivientes):** los límites de la paginación de resultados (`<` por `<=`) y las listas vacías.
- **`JSONDatetime` (2 supervivientes):** `parse` y `now` pueden devolver `null`.

Los 86 sin cobertura son sobre todo `EntityFactory` y `TestNumber`, que ninguna prueba unitaria ejecuta.

## Conclusión

- En usuarios y autenticación, todos los mutantes que ejecutan las pruebas activas están muertos salvo uno, que es equivalente (57 de 59). El único sin cobertura está en código que solo recorren pruebas desactivadas por un error conocido. La cobertura de estas clases se apoya en asserts que comprueban el comportamiento, no solo en ejecutar las líneas.
- PIT encontró en el código común algo que la cobertura no mostraba: `DataLoader` aparecía al 100 % de líneas en JaCoCo, pero ninguna prueba unitaria comprobaba lo que hacía (0 de 17 mutantes muertos). Ahora está a 17/17.
- Con las pruebas de plantillas integradas, esa área pasa de 24 a 50 mutantes muertos, y lo que queda sin cobertura es casi todo código bloqueado por un error conocido.
- En búsqueda, la diferencia entre puntuación y fuerza muestra que la mayor parte de lo que falta es código todavía sin pruebas. Los supervivientes listados arriba indican qué asserts añadir cuando se escriban.
