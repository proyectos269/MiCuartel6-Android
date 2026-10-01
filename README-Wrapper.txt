Mi Cuartel 6 Android — Gradle Wrapper
======================================

Archivos para agregar al proyecto:
- gradlew
- gradlew.bat
- gradle/wrapper/gradle-wrapper.properties

El launcher descarga, si hace falta, el gradle-wrapper.jar oficial de Gradle 8.9 desde el repositorio oficial de Gradle.

Después de agregar estos archivos al repositorio, Codemagic puede ejecutar:
    ./gradlew assembleDebug

No modificar el resto del proyecto.
