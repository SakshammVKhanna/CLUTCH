# ============================================================
#  NexVoice - Phone (USB) setup
# ------------------------------------------------------------
#  Kya karta hai:
#    1. USB se jude phone ko dhundhta hai
#    2. adb reverse tcp:5000 tcp:5000 lagata hai
#       (phone ka 127.0.0.1:5000  ->  laptop ka :5000)
#    3. Phone se backend tak pahunch ka test karta hai
#
#  Kab chalana hai:
#    - Har baar USB cable lagane ke baad
#    - Phone reboot / adb restart ke baad
#    - Jab app me "Cannot connect to server" aaye
#
#  Pehle ye chalu hona chahiye (alag terminal me):
#    cd D:\work\parallel-voice\backend
#    npm run dev
# ============================================================

$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $AdbPath)) {
    $AdbPath = "C:\Users\Hp\AppData\Local\Android\Sdk\platform-tools\adb.exe"
}
if (-not (Test-Path $AdbPath)) {
    Write-Host "adb nahi mila - Android SDK ka path check karo." -ForegroundColor Red
    exit 1
}

Write-Host "adb: $AdbPath" -ForegroundColor DarkGray
& $AdbPath start-server | Out-Null

# Sirf real phone (emulator-5554 ko chhod ke)
$phone = $null
foreach ($l in (& $AdbPath devices)) {
    if ($l -match '^(\S+)\s+device$') {
        if ($Matches[1] -notlike 'emulator-*') { $phone = $Matches[1] }
    }
}

if (-not $phone) {
    Write-Host ""
    Write-Host "Koi USB phone nahi mila. Ye 3 cheezein check karo:" -ForegroundColor Yellow
    Write-Host "  1. Data cable lagi ho (sirf charging wali cable nahi)"
    Write-Host "  2. Phone me Developer Options > USB debugging = ON"
    Write-Host "  3. Phone ki screen pe 'Allow USB debugging' aaye to Allow dabao"
    Write-Host ""
    & $AdbPath devices -l
    exit 1
}

Write-Host "Phone mila: $phone" -ForegroundColor Green

& $AdbPath -s $phone reverse tcp:5000 tcp:5000 | Out-Null
$rev = (& $AdbPath -s $phone reverse --list) -join " "
Write-Host "reverse mapping: $rev" -ForegroundColor DarkGray

Write-Host "Backend reachability test..."
$reply = (& $AdbPath -s $phone shell "curl -s -m 6 http://127.0.0.1:5000/" 2>$null) -join " "

if ($reply -match 'NexVoice API is running') {
    Write-Host ""
    Write-Host "OK - phone se backend reachable hai. App use kar sakte ho." -ForegroundColor Green
} elseif ($reply -match 'inaccessible|not found') {
    Write-Host ""
    Write-Host "reverse lag gaya (phone me curl nahi hai, isliye verify skip). App me test karo." -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "Backend se reply nahi aaya. Laptop pe backend chalao:" -ForegroundColor Red
    Write-Host "   cd D:\work\parallel-voice\backend" -ForegroundColor DarkGray
    Write-Host "   npm run dev" -ForegroundColor DarkGray
    exit 1
}
