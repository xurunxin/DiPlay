param(
    [ValidateSet('baseline', 'low_resource')][string]$Profile = 'baseline',
    [Parameter(Mandatory=$true)][string]$AvdHome,
    [Parameter(Mandatory=$true)][string]$LogDirectory,
    [switch]$LimitHostCpu
)
$ErrorActionPreference = 'Stop'
$taskSdk = $env:ANDROID_HOME
if (-not $taskSdk) { throw 'ANDROID_HOME must name an existing SDK' }
$taskEmulator = Join-Path $taskSdk 'emulator\emulator.exe'
if (-not (Test-Path -LiteralPath $taskEmulator)) { throw 'Install Emulator with owner approval first' }
$taskAvdConfig = Join-Path $AvdHome 'DiPlay_G0_API34.avd\config.ini'
if (-not (Test-Path -LiteralPath $taskAvdConfig)) { throw 'Create the isolated DiPlay_G0_API34 AVD first' }
$env:ANDROID_AVD_HOME = (Resolve-Path -LiteralPath $AvdHome).Path
New-Item -ItemType Directory -Path $LogDirectory -Force | Out-Null
$taskCores = 4
$taskMemory = 2048
if ($Profile -eq 'low_resource') { $taskCores = 2; $taskMemory = 1024 }
# Never stop another emulator or modify global CPU/power/virtualization settings.
$taskAdb = Join-Path $taskSdk 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $taskAdb)) { throw 'Existing SDK platform-tools are required' }
$taskDevices = & $taskAdb devices
if ($taskDevices -match 'emulator-5580\s') { throw 'Port 5580 is in use; stop the owned test AVD explicitly first' }
$taskArguments = @('-avd','DiPlay_G0_API34','-port','5580','-cores',"$taskCores",'-memory',"$taskMemory",'-no-window','-no-audio','-no-snapshot','-no-boot-anim','-gpu','swiftshader')
$taskProcess = Start-Process -FilePath $taskEmulator -ArgumentList $taskArguments -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $LogDirectory "$Profile.stdout.log") -RedirectStandardError (Join-Path $LogDirectory "$Profile.stderr.log")
if ($LimitHostCpu) { $taskProcess.ProcessorAffinity = [IntPtr]3 }
[pscustomobject]@{ HostAffinityMask=($taskProcess.ProcessorAffinity.ToInt64()); EffectiveRamMustBeVerified=$true; Profile=$Profile; ProcessId=$taskProcess.Id; Vcpu=$taskCores; RequestedRamMB=$taskMemory; Renderer='SwiftShader'; EquivalentToSnapdragon625=$false } | ConvertTo-Json
