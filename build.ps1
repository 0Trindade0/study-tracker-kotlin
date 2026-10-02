param([string]$Kotlinc = 'kotlinc')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Path build -Force | Out-Null
    & $Kotlinc src/main/kotlin/StudyTracker.kt -jvm-target 17 -include-runtime -d build/study-tracker.jar
    if ($LASTEXITCODE -ne 0) { throw 'Application compilation failed.' }
    & $Kotlinc src/main/kotlin/StudyTracker.kt src/test/kotlin/StudyTrackerTest.kt -jvm-target 17 -include-runtime -d build/tests.jar
    if ($LASTEXITCODE -ne 0) { throw 'Test compilation failed.' }
    java -cp build/tests.jar studytracker.StudyTrackerTestKt
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed.' }
} finally { Pop-Location }
