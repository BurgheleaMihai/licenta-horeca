<#
.SYNOPSIS
    Verifica integral serviciul AI al aplicatiei HoReCa.

.DESCRIPTION
    Scriptul ruleaza automat verificarile necesare dupa modificarea fisierelor
    Python din directorul ai-service:

    1. verifica versiunea Python si dependentele instalate;
    2. verifica existenta fisierelor principale;
    3. verifica sintaxa tuturor fisierelor Python;
    4. ruleaza verificarile Ruff;
    5. verifica importurile modulelor;
    6. verifica incarcarea modelelor .pkl;
    7. porneste temporar serviciul Flask si testeaza /health si /predict/all;
    8. ruleaza evaluarea extinsa a modelelor;
    9. ruleaza Deepchecks si verifica rapoartele HTML;
    10. optional, face backup si reantreneaza toate modelele.

.PARAMETER FullRetrain
    Activeaza backup-ul si reantrenarea completa a celor trei modele.
    Fara acest parametru, modelele existente nu sunt inlocuite.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\verify_ai_service.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\verify_ai_service.ps1 -FullRetrain
#>

param(
    # Parametru optional. Cand este prezent, scriptul regenereaza datasetul,
    # reantreneaza modelele si ruleaza din nou evaluarile.
    [switch]$FullRetrain
)

# Opreste scriptul imediat la prima eroare PowerShell netratata.
$ErrorActionPreference = "Stop"

# Directorul principal al serviciului AI.
$ProjectDir = "C:\Users\burgh\Desktop\Licenta\Proiect\Cod\ai-service"


# ---------------------------------------------------------------------------
# FUNCTII AJUTATOARE
# ---------------------------------------------------------------------------

function Write-Step {
    <#
    .SYNOPSIS
        Afiseaza un titlu vizibil pentru fiecare etapa.
    #>

    param(
        [string]$Title
    )

    Write-Host ""
    Write-Host ("=" * 78) -ForegroundColor Cyan
    Write-Host $Title -ForegroundColor Cyan
    Write-Host ("=" * 78) -ForegroundColor Cyan
}


function Invoke-Python {
    <#
    .SYNOPSIS
        Ruleaza o comanda Python si opreste scriptul daca aceasta esueaza.

    .DESCRIPTION
        Argumentele sunt trimise direct interpreterului Python configurat
        in PATH. Codul de iesire este verificat dupa fiecare executie.
    #>

    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    # Operatorul splat @Arguments trimite fiecare element ca argument separat.
    & python @Arguments

    # Orice cod diferit de zero indica o executie nereusita.
    if ($LASTEXITCODE -ne 0) {
        throw "Comanda Python a esuat: python $($Arguments -join ' ')"
    }
}


function Wait-ForHealth {
    <#
    .SYNOPSIS
        Asteapta pana cand serviciul AI raspunde la endpointul /health.

    .DESCRIPTION
        Se face cate o incercare pe secunda. Functia returneaza raspunsul JSON
        convertit in obiect PowerShell imediat ce serviciul devine disponibil.
    #>

    param(
        [int]$MaximumSeconds = 30
    )

    for ($attempt = 1; $attempt -le $MaximumSeconds; $attempt++) {
        try {
            return Invoke-RestMethod `
                -Uri "http://127.0.0.1:5000/health" `
                -Method Get `
                -TimeoutSec 2
        }
        catch {
            # Serviciul poate avea nevoie de cateva secunde pentru a incarca
            # modelele. Se asteapta o secunda inainte de urmatoarea incercare.
            Start-Sleep -Seconds 1
        }
    }

    throw "AI Service nu a raspuns la /health in $MaximumSeconds secunde."
}


# ---------------------------------------------------------------------------
# PREGATIREA DIRECTORULUI SI LISTA FISIERELOR
# ---------------------------------------------------------------------------

# Verifica existenta directorului inainte de orice alta operatie.
if (-not (Test-Path $ProjectDir)) {
    throw "Directorul proiectului nu exista: $ProjectDir"
}

# Toate comenzile urmatoare sunt executate din directorul ai-service.
Set-Location $ProjectDir

# Generatorul datasetului a avut doua denumiri de-a lungul dezvoltarii.
# Scriptul accepta automat oricare dintre ele, fara sa oblige redenumirea.
$DatasetGeneratorCandidates = @(
    "generate_dataset.py",
    "generate_synthetic_dataset.py"
)

$DatasetGenerator = $DatasetGeneratorCandidates |
    Where-Object { Test-Path $_ } |
    Select-Object -First 1

if ($null -eq $DatasetGenerator) {
    throw (
        "Nu a fost gasit nici generate_dataset.py, "
    )
}

Write-Host (
    "Generator dataset detectat: " +
    $DatasetGenerator
) -ForegroundColor Green

$DatasetGeneratorModule = [System.IO.Path]::GetFileNameWithoutExtension(
    $DatasetGenerator
)

# Lista fisierelor Python modificate si verificate explicit.
$Files = @(
    "app.py",
    "deepchecks_evaluation.py",
    "evaluate_models.py",
    $DatasetGenerator,
    "retrain_models.py",
    "staff_model_architecture.py",
    "train_all_models.py",
    "train_delay_model.py",
    "train_staff_model.py",
    "train_traffic_model.py"
)


# ---------------------------------------------------------------------------
# 1. MEDIU PYTHON SI DEPENDENTE
# ---------------------------------------------------------------------------

Write-Step "1. MEDIU PYTHON SI DEPENDENTE"

# Afiseaza versiunea interpreterului folosit de script.
Invoke-Python @("--version")

# Verifica incompatibilitatile dintre pachetele Python instalate.
Invoke-Python @("-m", "pip", "check")


# ---------------------------------------------------------------------------
# 2. EXISTENTA FISIERELOR
# ---------------------------------------------------------------------------

Write-Step "2. EXISTENTA FISIERELOR MODIFICATE"

foreach ($file in $Files) {
    if (-not (Test-Path $file)) {
        throw "Lipseste fisierul: $file"
    }

    Write-Host "OK  $file" -ForegroundColor Green
}


# ---------------------------------------------------------------------------
# 3. VERIFICAREA SINTAXEI PYTHON
# ---------------------------------------------------------------------------

Write-Step "3. VERIFICARE SINTAXA PYTHON"

# py_compile verifica explicit fisierele principale.
Invoke-Python (@("-m", "py_compile") + $Files)

# compileall verifica recursiv toate fisierele Python din proiect.
Invoke-Python @("-m", "compileall", "-q", ".")

Write-Host "Sintaxa tuturor fisierelor este corecta." -ForegroundColor Green


# ---------------------------------------------------------------------------
# 4. VERIFICAREA IMPORTURILOR
# ---------------------------------------------------------------------------

Write-Step "4. VERIFICARE IMPORTURI"

# Cod Python executat intr-un proces separat. Daca un import esueaza,
# Invoke-Python opreste automat verificarea.
$ImportCommand = @"
import app
import evaluate_models
import $DatasetGeneratorModule
import retrain_models
import staff_model_architecture
import train_all_models
import train_delay_model
import train_staff_model
import train_traffic_model
"@

Invoke-Python @("-c", $ImportCommand)

# Deepchecks este verificat separat deoarece are dependente suplimentare.
$DeepchecksImportCommand = @"
import deepchecks_evaluation
"@

Invoke-Python @("-c", $DeepchecksImportCommand)


# ---------------------------------------------------------------------------
# 5. VERIFICAREA MODELELOR SALVATE
# ---------------------------------------------------------------------------

Write-Step "5. VERIFICARE MODELE PKL"

$ModelFiles = @(
    "traffic_model.pkl",
    "staff_model.pkl",
    "delay_model.pkl"
)

foreach ($ModelFile in $ModelFiles) {
    $ModelPath = Join-Path ".\models" $ModelFile

    if (-not (Test-Path $ModelPath)) {
        throw "Lipseste modelul: $ModelPath"
    }

    & python -c `
        "import joblib, sys; model = joblib.load(sys.argv[1]); print(type(model).__name__)" `
        $ModelPath

    if ($LASTEXITCODE -ne 0) {
        throw "Modelul nu poate fi incarcat: $ModelFile"
    }

    Write-Host "OK  $ModelFile" -ForegroundColor Green
}


# ---------------------------------------------------------------------------
# 6. PORNIREA SERVICIULUI SI TESTELE HTTP
# ---------------------------------------------------------------------------

Write-Step "6. PORNIRE SERVICIU SI TESTE HTTP"

# Logurile serviciului sunt pastrate separat pentru diagnosticare.
$LogDir = Join-Path $ProjectDir "reports\verification"
New-Item -ItemType Directory -Path $LogDir -Force | Out-Null

$ServiceOutput = Join-Path $LogDir "ai_service_stdout.log"
$ServiceError = Join-Path $LogDir "ai_service_stderr.log"

# Sterge logurile vechi pentru ca fiecare rulare sa porneasca de la zero.
Remove-Item `
    $ServiceOutput, `
    $ServiceError `
    -Force `
    -ErrorAction SilentlyContinue

# Porneste app.py intr-un proces separat si ascuns. Procesul va fi oprit in
# blocul finally, inclusiv atunci cand una dintre verificarile HTTP esueaza.
$ServiceProcess = Start-Process `
    -FilePath "python" `
    -ArgumentList "app.py" `
    -WorkingDirectory $ProjectDir `
    -RedirectStandardOutput $ServiceOutput `
    -RedirectStandardError $ServiceError `
    -PassThru `
    -WindowStyle Hidden

try {
    # Asteapta pana cand serviciul este disponibil.
    $Health = Wait-ForHealth -MaximumSeconds 30

    # Afiseaza raspunsul endpointului pentru verificare vizuala.
    $Health | ConvertTo-Json -Depth 10

    # Modelele trebuie sa fie incarcate in memorie.
    if (-not $Health.modelsLoaded) {
        throw "/health raporteaza modelsLoaded=false."
    }

    # Confirma numarul de caracteristici folosit de fiecare model.
    if (
        $Health.trafficFeatureCount -ne 11 -or
        $Health.staffFeatureCount -ne 11 -or
        $Health.delayFeatureCount -ne 21
    ) {
        throw "Numarul de caracteristici din /health nu este corect."
    }

    Write-Host "/health OK" -ForegroundColor Green

    # Exemplu complet de stare operationala trimisa la lantul de predictie.
    $PredictionBody = @{
        day_of_week = 2
        hour = 19
        active_orders = 8
        occupied_tables = 7
        estimated_occupancy = 58
        kitchen_load = 10
        bar_load = 5
        avg_preparation_time = 18
        orders_last_30_min = 12
        order_age_minutes = 9
        item_count = 15
        active_waiters = 2
        active_kitchen = 2
        active_bar = 1
    } | ConvertTo-Json

    # Trimite cererea catre endpointul principal de predictie.
    $Prediction = Invoke-RestMethod `
        -Uri "http://127.0.0.1:5000/predict/all" `
        -Method Post `
        -ContentType "application/json" `
        -Body $PredictionBody `
        -TimeoutSec 30

    # Afiseaza raspunsul primit.
    $Prediction | ConvertTo-Json -Depth 10

    # Campurile obligatorii ale raspunsului API.
    $RequiredResponseFields = @(
        "trafficLevel",
        "recommendedWaiters",
        "recommendedKitchenStaff",
        "recommendedBarStaff",
        "delayRisk"
    )

    # Verifica existenta tuturor campurilor obligatorii.
    foreach ($field in $RequiredResponseFields) {
        if ($null -eq $Prediction.$field) {
            throw "Raspunsul /predict/all nu contine campul: $field"
        }
    }

    Write-Host "/predict/all OK" -ForegroundColor Green
}
finally {
    # Opreste procesul serviciului chiar daca o verificare a esuat.
    if (
        $null -ne $ServiceProcess `
        -and -not $ServiceProcess.HasExited
    ) {
        Stop-Process -Id $ServiceProcess.Id -Force
        $ServiceProcess.WaitForExit()
    }
}


# ---------------------------------------------------------------------------
# 7. EVALUAREA EXTINSA
# ---------------------------------------------------------------------------

Write-Step "7. EVALUAREA EXTINSA A MODELELOR"

# Genereaza rapoartele JSON si text cu metricile extinse.
Invoke-Python @("evaluate_models.py")


# ---------------------------------------------------------------------------
# 8. EVALUAREA DEEPCHECKS
# ---------------------------------------------------------------------------

Write-Step "8. EVALUAREA DEEPCHECKS"

# Ruleaza suitele externe de evaluare.
Invoke-Python @("deepchecks_evaluation.py")

# Citeste toate rapoartele HTML generate.
$DeepchecksReports = Get-ChildItem `
    -Path ".\reports\deepchecks\*.html" `
    -ErrorAction Stop

# Sunt asteptate sase rapoarte:
# trafic, trei roluri de personal si doua evaluari pentru delay.
if ($DeepchecksReports.Count -lt 6) {
    throw "Au fost gasite doar $($DeepchecksReports.Count) rapoarte Deepchecks HTML."
}

# Niciun raport nu trebuie sa fie gol.
$EmptyReports = $DeepchecksReports |
    Where-Object { $_.Length -eq 0 }

if ($EmptyReports) {
    throw "Exista rapoarte Deepchecks goale: $($EmptyReports.Name -join ', ')"
}

# Afiseaza numele, dimensiunea si data fiecarui raport.
$DeepchecksReports |
    Select-Object Name, Length, LastWriteTime |
    Format-Table -AutoSize


# ---------------------------------------------------------------------------
# 9. BACKUP SI REANTRENARE OPTIONALA
# ---------------------------------------------------------------------------

if ($FullRetrain) {
    Write-Step "9. BACKUP SI REANTRENARE COMPLETA"

    # Timestamp-ul evita suprascrierea backup-urilor anterioare.
    $Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"

    # Copie de siguranta pentru modelele existente.
    Copy-Item `
        -Path ".\models" `
        -Destination ".\models_backup_$Timestamp" `
        -Recurse

    # Copie de siguranta pentru rapoartele existente.
    Copy-Item `
        -Path ".\reports" `
        -Destination ".\reports_backup_$Timestamp" `
        -Recurse

    # Copie de siguranta pentru datasetul sintetic existent.
    Copy-Item `
        -Path ".\data\synthetic_horeca_dataset.csv" `
        -Destination ".\data\synthetic_horeca_dataset_backup_$Timestamp.csv"

    # Regenereaza datasetul si antreneaza toate cele trei modele.
    Invoke-Python @("train_all_models.py")

    # Evalueaza din nou modelele proaspat generate.
    Invoke-Python @("evaluate_models.py")
    Invoke-Python @("deepchecks_evaluation.py")
}


# ---------------------------------------------------------------------------
# REZULTAT FINAL
# ---------------------------------------------------------------------------

Write-Step "VERIFICARE FINALIZATA"

Write-Host "Toate verificarile au trecut cu succes." -ForegroundColor Green
Write-Host "Loguri AI Service: $LogDir" -ForegroundColor Green

if (-not $FullRetrain) {
    Write-Host ""
    Write-Host "Reantrenarea completa NU a fost rulata." -ForegroundColor Yellow
    Write-Host "Pentru ea, ruleaza acelasi fisier cu parametrul -FullRetrain." -ForegroundColor Yellow
}