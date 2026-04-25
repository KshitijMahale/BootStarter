# BootStarter Community (IntelliJ IDEA Community plugin)

Generates a Spring Boot project directly into the currently opened project directory using Spring Initializr API.

## Features
- Menu action: `File -> New -> Spring Boot Project (Community)`
- Form for project metadata and dependencies
- Background generation with progress
- ZIP download + extraction
- IntelliJ API-based file copy into project root (`VirtualFile`, `WriteCommandAction`)
- Refresh project after generation

## Build and run

```powershell
./gradlew.bat clean test
./gradlew.bat runIde
```

## Notes
- If the current project directory is non-empty, plugin asks before overwriting conflicts.
- Spring Initializr endpoint used: `https://start.spring.io/starter.zip`.

