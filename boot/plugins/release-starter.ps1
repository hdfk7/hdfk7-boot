$init_runtime_path = Get-Location
$maven_config_path = Join-Path (Resolve-Path "$PSScriptRoot\..\..") ".mvn\maven.config"
$revision_line = Get-Content $maven_config_path | Where-Object { $_ -like "-Drevision=*" } | Select-Object -First 1
$version_name = $revision_line -replace "^-Drevision=", ""
$project_list = @("hdfk7-boot-starter-code-generator", "hdfk7-boot-starter-common", "hdfk7-boot-starter-discovery")

function Signature-File
{
    param(
        [string]$FileSource
    )
    if (Test-Path -Path $FileSource -PathType Leaf)
    {
        gpg --armor --output "$FileSource.asc" --detach-sig $FileSource
        $algorithms = @("MD5", "SHA1", "SHA256", "SHA512")
        foreach ($alg in $algorithms)
        {
            $lowerAlg = $alg.ToLower()
            Get-FileHash $FileSource -Algorithm $alg | ForEach-Object { $_.Hash } | Set-Content "$FileSource.$lowerAlg"
        }
    }
}

function Copy-File
{
    param(
        [string]$FileSource,
        [string]$FileDestination
    )
    if (Test-Path -Path $FileSource -PathType Leaf)
    {
        Copy-Item -Path $FileSource -Destination $FileDestination
    }
}

function Copy-Release-Pom
{
    param(
        [string]$PomSource,
        [string]$PomDestination
    )
    $utf8 = New-Object System.Text.UTF8Encoding $false
    $content = [System.IO.File]::ReadAllText((Resolve-Path $PomSource), [System.Text.Encoding]::UTF8)
    $content = $content.Replace('${revision}', $version_name)
    [System.IO.File]::WriteAllText($PomDestination, $content, $utf8)
}

function Install-Release-Artifact
{
    param(
        [string]$ReleasePom,
        [string]$JarFile
    )
    if (Test-Path -Path $JarFile -PathType Leaf)
    {
        mvn install:install-file "-Dfile=$JarFile" "-DpomFile=$ReleasePom"
    }
    else
    {
        mvn install:install-file "-Dfile=$ReleasePom" "-DpomFile=$ReleasePom" "-Dpackaging=pom"
    }
}

foreach ($project in $project_list)
{
    $project_parts = $project -split "\\"
    $project_name = $project_parts[-1]

    $runtime_path = "$( Get-Location )\$project"
    $staging_path = "$runtime_path\target\central-staging"
    $publishing_path = "$runtime_path\target\central-publishing"
    $storage_path = "$staging_path\cn\hdfk7\$project_name\$version_name"
    $pom = "$project_name-$version_name.pom"
    $jar = "$project_name-$version_name.jar"
    $javadoc = "$project_name-$version_name-javadoc.jar"
    $sources = "$project_name-$version_name-sources.jar"

    Set-Location "$runtime_path"
    mvn clean install
    New-Item -ItemType Directory -Force -Path "$publishing_path"
    New-Item -ItemType Directory -Force -Path $storage_path
    $release_pom_path = Join-Path $storage_path $pom
    Copy-Release-Pom "pom.xml" $release_pom_path
    Install-Release-Artifact $release_pom_path "target\$jar"
    Copy-File "target\$jar" (Join-Path $storage_path $jar)
    Copy-File "target\$javadoc" (Join-Path $storage_path $javadoc)
    Copy-File "target\$sources" (Join-Path $storage_path $sources)

    Set-Location $storage_path
    Signature-File $pom
    Signature-File $jar
    Signature-File $javadoc
    Signature-File $sources

    Set-Location "$staging_path"
    Compress-Archive -Path ".\*" -DestinationPath "$publishing_path\central-bundle.zip"

    Set-Location "$init_runtime_path"
}
