# GMM-Java · Gestor Multiproceso Multiplataforma

Proyecto de la UD01 de Programación de Servicios y Procesos (2º DAM, ILERNA).

Aplicación de consola en Java que agrupa en un menú las operaciones de creación,
comunicación, supervisión y planificación de procesos. Funciona en Windows y Linux
sin modificar el código fuente.

## Requisitos

- Java 17 o superior (JDK)
- Maven 3.x

## Compilación

```bash
mvn compile
```

## Ejecución

Tras compilar, desde la carpeta del proyecto:

```bash
java -cp target/classes es.ilerna.psp.gmm.Main
```

## Estructura

```
src/main/java/es/ilerna/psp/gmm/
├── Main.java          Menú principal
├── entorno/           Módulo 01: monitor de entorno y propiedades del sistema
├── procesos/          Módulo 02: ejecutor multiplataforma
│                      Módulo 03: ciclo de vida, timeout e historial de ejecuciones
├── comunicacion/      Módulo 04: entrada, salida, ficheros y comunicación padre-hijo
├── worker/            Programas auxiliares lanzados como procesos hijos
│                      (ProgramaAuxiliar, ProcesoLento, ProgramaConErrores, EcoLector)
├── bernstein/         Módulo 06: condiciones de Bernstein
├── planificacion/     Módulo 07: simulador de planificación de CPU
└── util/              Utilidades comunes (teclado, SO, lanzador de procesos)
datos/                 Ficheros de entrada
resultados/            Ficheros generados por los procesos (Módulo 04)
logs/                  Registros de ejecución (historial_ejecuciones.log, errores_procesos.log)
evidencias/            Pruebas de funcionamiento
docs/                  Memoria técnica
```

## Estado

| Opción | Módulo | Estado |
|---|---|---|
| 1 | Información del sistema | Hecho |
| 2 | Ejecutor multiplataforma de comandos | Hecho |
| 3 | Control y supervisión de procesos | Hecho |
| 4 | Gestión de entrada/salida y logs | Hecho |
| 5 | Procesamiento paralelo de ficheros | Pendiente |
| 6 | Análisis de dependencias de Bernstein | Pendiente |
| 7 | Simulador de planificación de CPU | Pendiente |
| 8 | Consultar registros de ejecución | Hecho |
