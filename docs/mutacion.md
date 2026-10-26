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

Estado medido en la rama `test/usuarios-auth-cobertura-mutacion`, con las pruebas de usuarios (Javi) y componentes (Luis). **Las de plantillas (Mario) y búsqueda (Nico) todavía no están integradas.**

| Ejecución | Generados | Muertos | Supervivientes | Sin cobertura | Puntuación | Fuerza |
|---|---|---|---|---|---|---|
| Primera ejecución | 415 | 200 | 72 | 143 | 48 % | 74 % |
| Tras añadir pruebas | 415 | 222 | 51 | 142 | **53 %** | **81 %** |

Por área, tras añadir las pruebas:

| Área | Generados | Muertos | Supervivientes | Sin cobertura | Puntuación | Fuerza |
|---|---|---|---|---|---|---|
| Usuarios y autenticación (Javi) | 74 | 73 | 1 | 0 | **98,6 %** | **98,6 %** |
| Componentes (Luis) | 98 | 75 | 10 | 13 | 76,5 % | 88,2 % |
| Plantillas (Mario) | 68 | 24 | 2 | 42 | 35,3 % | 92,3 % |
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

`AuthorizationManager` (13/13), `User` (5/5), `RESTExceptionMapper` (4/4) y las excepciones mataban ya todos sus mutantes en la primera ejecución.

### Mutantes que quedan y su justificación

| Clase | Mutante | Estado | Justificación |
|---|---|---|---|
| `Token.hashCode` (línea 61) | Devolver `0` en lugar de `getClass().hashCode()` | Superviviente | **Mutante equivalente.** `hashCode` devuelve un valor constante a propósito (patrón recomendado para entidades JPA, [#16](https://github.com/luuizexe/plataforma_xestion_tecnica_VVS/issues/16)). Con cualquier otra constante, como `0`, se sigue cumpliendo el contrato con `equals`: dos tokens iguales tienen el mismo hash. Ninguna prueba de comportamiento puede distinguirlos; solo una que fijase el valor concreto, que estaría probando la implementación y no el comportamiento. |
| `OpenApiResource.getOpenApi` (línea 26) | Devolver `null` | Sin cobertura | Solo se ejecuta sirviendo el documento OpenAPI por HTTP. Necesita el `ServletConfig` y la `Application` de Jersey, que se inyectan en tiempo de ejecución. Lo prueba `AuthorizationIT.testOpenApiWithoutToken` (responde 200 sin token), que PIT no ejecuta por ser de integración. |

## Supervivientes en otras áreas

Se dejan anotados para que cada responsable añada las pruebas. **Hay que volver a ejecutar PIT cuando estén integradas las pruebas de plantillas y búsqueda**, porque las cifras de esas áreas cambiarán por completo.

**Componentes (Luis): 10 supervivientes.**

| Clase y línea | Mutante | Qué falta comprobar |
|---|---|---|
| `ComponentResource.addComponent` (100) | Negar `getId() == null` | El mensaje de `TemplateNotFoundException` cuando la plantilla no tiene id. |
| `ComponentResource.addComponent` (122) | Negar `isFieldMissing(field)` | Qué campos obligatorios aparecen en el mensaje de `ComponentFieldIsMandatoryException`. |
| `ComponentResource.getComponent` (169) | Eliminar `setTemplate(new Template(id))` | Que la plantilla del componente devuelto solo lleve el id. |
| `ComponentResource.getFieldsComponent` (270) | Devolver `null` en la conversión de cada campo | El contenido de la lista de campos devuelta, no solo su tamaño. |
| `ComponentResource.modifyComponent` (187, 191, 193) | Eliminar `setDescription`, eliminar `setContent` de los campos `TEXT` y que el filtro de campos devuelva siempre `true` | Que la modificación guarde la descripción y el contenido de cada campo en su campo correspondiente. |
| `Component.hashCode` (86), `Field.hashCode` (75) | Devolver `0` | Mutantes equivalentes por el mismo motivo que `Token.hashCode`. |
| `ComponentExceptionTemplate` (9) | Negar `name != null` | El mensaje de error cuando el componente no tiene nombre. |

**Plantillas (Mario): 2 supervivientes y 42 sin cobertura.**

| Clase y línea | Mutante | Qué falta comprobar |
|---|---|---|
| `Template.equals` (66), `TemplateField.equals` (63) | Negar `this == o` | `equals` de una plantilla o un campo consigo mismo. |
| `TemplateField.getName` (38) | Devolver `""` | El nombre de los campos de plantilla. |

Los 42 sin cobertura son casi todos de `TemplateResource`, que todavía no tiene pruebas unitarias.

**Búsqueda (Nico): 38 supervivientes y 86 sin cobertura.**

- **`search.FindListener` (10 supervivientes):** se pueden eliminar las llamadas que añaden nodos al árbol (`appendNode`, `enterExpAnd`, `enterExpOr`, `enterTestIn`…) sin que falle ninguna prueba. Las pruebas actuales no comprueban la estructura del árbol que se genera.
- **`search.TestIn`, `TestString`, `TestNumber.Operator` (11 supervivientes):** se pueden negar las comparaciones de `matches` y `getOperator`.
- **`search.AndNode`, `OrNode`, `RootNode`, `Node` (7 supervivientes):** se pueden cambiar `&&`/`||` o devolver siempre `true`/`false` en `matches`.
- **`SearchResource` (8 supervivientes):** los límites de la paginación de resultados (`<` por `<=`) y las listas vacías.
- **`JSONDatetime` (2 supervivientes):** `parse` y `now` pueden devolver `null`.

Los 86 sin cobertura son sobre todo `EntityFactory` y `TestNumber`, que ninguna prueba unitaria ejecuta.

## Conclusión

- En usuarios y autenticación, todos los mutantes que se pueden matar están muertos (73 de 74). El único que sobrevive es equivalente. La cobertura del 100 % de estas clases se apoya en asserts que comprueban el comportamiento, no solo en ejecutar las líneas.
- PIT encontró en el código común algo que la cobertura no mostraba: `DataLoader` aparecía al 100 % de líneas en JaCoCo, pero ninguna prueba unitaria comprobaba lo que hacía (0 de 17 mutantes muertos). Ahora está a 17/17.
- En el resto de áreas, la diferencia entre puntuación y fuerza muestra que la mayor parte de lo que falta es código todavía sin pruebas. Los supervivientes listados arriba indican qué asserts añadir cuando se escriban.
