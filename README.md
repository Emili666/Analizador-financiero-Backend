# Analizador Financiero — Backend

API REST en **Spring Boot** que descarga datos históricos de activos financieros (acciones, criptomonedas, etc.) desde Yahoo Finance, aplica algoritmos de análisis de series de tiempo implementados manualmente, y genera reportes comparativos en PDF.

## Funcionalidades

- **ETL de datos financieros**: descarga y parseo del histórico diario de un activo directamente desde la API pública de Yahoo Finance (sin token), con limpieza y unificación de series de distintos activos para que sean comparables.
- **Análisis de similitud entre series**: comparación de dos activos mediante distancia euclidiana, correlación de Pearson, *Dynamic Time Warping* (DTW) y similitud coseno — todos implementados a mano, sin librerías externas de ML.
- **Análisis de riesgo y patrones**: cálculo de volatilidad, clasificación de riesgo, media móvil simple (SMA), conteo de tendencias alcistas consecutivas y detección de patrones tipo *bullish engulfing*.
- **Matriz de correlación** entre múltiples activos a la vez.
- **Benchmark de algoritmos de ordenamiento**: implementación manual de 12 algoritmos (selection, quick, heap, gnome, comb, tim, tree, pigeonhole, bucket, bitonic, binary insertion y radix sort) con medición de tiempos de ejecución para comparar su desempeño sobre los mismos datos.
- **Reportes en PDF**: generación de reportes individuales y comparativos con iText, incluyendo tablas y gráficos.

## Stack técnico

| Categoría | Tecnología |
|---|---|
| Lenguaje / Framework | Java 17, Spring Boot 3.2 |
| Generación de PDF | iText 7 (kernel, layout, io) |
| Gráficos | JFreeChart |
| Descarga de datos | `java.net.http.HttpClient` (sin dependencias externas) contra la API pública de Yahoo Finance |
| Build | Maven |

## Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/assets` | Lista de activos disponibles / descargados |
| GET | `/api/similarity` | Similitud entre dos series (Euclidiana, Pearson, DTW, coseno) |
| GET | `/api/patterns/{symbol}` | Patrones detectados para un activo |
| GET | `/api/correlation-matrix` | Matriz de correlación entre activos |
| GET | `/api/chart/{symbol}` | Datos para graficar un activo |
| GET | `/api/benchmark` | Benchmark de los algoritmos de ordenamiento |
| GET | `/api/benchmark/similarity` | Benchmark de los algoritmos de similitud |
| GET | `/api/report/pdf` | Reporte PDF individual |
| GET | `/api/report/comparative/pdf` | Reporte PDF comparativo entre activos |

## Cómo correrlo localmente

### Requisitos
- Java 17+
- Maven

### Pasos

```bash
mvn clean install
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

### Tests

```bash
mvn test
```

Incluye pruebas unitarias de `AlgorithmService` (algoritmos de ordenamiento y similitud) y `EtlService` (parseo y limpieza de datos).

## Proyecto relacionado

Este backend es consumido por [`Analizador-financiero-Fronted`](https://github.com/Emili666/Analizador-financiero-Fronted), construido en React.
