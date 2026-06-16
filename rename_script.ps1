$ErrorActionPreference = "Stop"
$projectRoot = "c:\Users\GuyGuy\AndroidStudioProjects\ThePigFarmer"

# 1. Update text in all .kt and .xml files in app/src
Write-Host "Updating text in .kt and .xml files..."
$files = Get-ChildItem -Path "$projectRoot\app\src" -Include *.kt,*.xml -Recurse

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $newContent = $content -replace 'com\.example\.thepigfarmer', 'com.example.smartswine'
    $newContent = $newContent -replace 'ThePigFarmerTheme', 'SmartSwineTheme'
    
    if ($content -cne $newContent) {
        Write-Host "Updating $($file.FullName)"
        Set-Content -Path $file.FullName -Value $newContent -NoNewline
    }
}

# 2. Rename directories
Write-Host "Renaming directories..."
$dirsToRename = @(
    "$projectRoot\app\src\main\java\com\example\thepigfarmer",
    "$projectRoot\app\src\androidTest\java\com\example\thepigfarmer",
    "$projectRoot\app\src\test\java\com\example\thepigfarmer"
)

foreach ($dir in $dirsToRename) {
    if (Test-Path $dir) {
        Write-Host "Renaming $dir to smartswine"
        Rename-Item -Path $dir -NewName "smartswine"
    } else {
        Write-Host "Directory not found: $dir"
    }
}

Write-Host "Done."
