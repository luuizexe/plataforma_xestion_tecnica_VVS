# Medición de rendimiento del servidor con JETM

## Alcance y requisitos

JETM (Java Execution Time Measurement Library) registra tiempos; no detecta bugs funcionales ni sustituye JUnit, JaCoCo, SpotBugs, ArchUnit o PIT. Se usa solo en tests con `fm.void.jetm:jetm:1.2.3`; no requiere instrumentación ni cambios en `src/main`.

| Requisito no funcional | Nivel y clase | Qué se mide |
|---|---|---|
| `NFR-REND-UNIT`: tiempo de búsqueda de plantillas sobre un catálogo representativo, distinguiendo búsqueda fría y acceso en caché | Unidad: `SearchResourcePerformanceTest` | Llamada directa a `SearchResource.searchTemplates`, con DAO simulado y 2.000 plantillas |
| `NFR-REND-SYSTEM`: latencia observable de endpoints representativos con el servidor en ejecución | Integración: `ServerPerformanceIT` | Diez peticiones HTTP autenticadas por endpoint (`/templates`, `/components`, `/users`, `/search/templates`) en un servidor Spring con puerto aleatorio |

La prueba unitaria mide el método directamente con el DAO aislado; la de integración mide el recorrido HTTP real. La ubicación y convención `*IT.java` siguen la organización de `ServerArchitectureIT.java`.

No existe un SLA, umbral de latencia ni línea base acordados. Los resultados son mediciones observacionales, no criterios de aceptación ni prueba concluyente de regresión.

## Ejecución

Desde la raíz del repositorio, ejecutar las pruebas por nivel:

```bash
mvn -pl pleste-server -Dtest=SearchResourcePerformanceTest test
mvn -pl pleste-server -Dit.test=ServerPerformanceIT verify
```

La salida queda separada por nivel:

```text
target/jetm-reports/unit-measurements.txt
target/jetm-reports/integration-measurements.txt
```

Para ejecutar todo el conjunto unitario y de integración:

```bash
mvn -pl pleste-server verify
```

La verificación de referencia de `ServerPerformanceIT` terminó con 4 pruebas, 0 fallos y 0 errores; registró diez muestras para cada uno de los cuatro endpoints. `SearchResourcePerformanceTest` terminó con 2 pruebas, 0 fallos y 0 errores.

## Interpretación y protocolo ante fallos

- Cada prueba tiene un único assert para verificar la respuesta funcional mínima; la medición se recoge incluso si una llamada falla. JETM reporta muestras y estadísticos (media, mínimo, máximo y total).
- Si un assert falla por comportamiento del servidor, confirmar primero que no sea configuración o datos de prueba. Desactivar solo la prueba afectada con `@Disabled("Bug conocido: ...")` y registrar síntoma, causa, explicación y solución propuesta en el informe descargable `bugs-jetm.md`.
- No se corrige código de producción como parte de esta actividad. No desactivar pruebas por errores de instrumentación, configuración o datos de prueba; corregir el test/harness o dejar explícito el bloqueo.
- Las pruebas preexistentes con `@Disabled` no se ejecutan. Sus defectos, compilados a partir de anotaciones y resultados documentados, no se presentan como errores detectados por JETM.
- El informe `bugs-jetm.md` se entrega como archivo descargable fuera del repositorio; no se añade al stage.
