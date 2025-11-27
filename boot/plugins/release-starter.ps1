$init_runtime_path = Get-Location
$version_name = "3.0.2"
$project_list = @("hdfk7-code-generator", "hdfk7-boot-starter-discovery", "hdfk7-boot-starter-common")

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

foreach ($project in $project_list)
{
    $project_parts = $project -split "\\"
    $project_name = $project_parts[-1]

    $runtime_path = "$( Get-Location )\$project"
    $staging_path = "$runtime_path\target\central-staging"
    $publishing_path = "$runtime_path\target\central-publishing"
    $storage_path = "$staging_path\cn\hdfk7\boot\$project_name\$version_name"
    $pom = "$project_name-$version_name.pom"
    $jar = "$project_name-$version_name.jar"
    $javadoc = "$project_name-$version_name-javadoc.jar"
    $sources = "$project_name-$version_name-sources.jar"

    Set-Location "$runtime_path"
    mvn clean install
    New-Item -ItemType Directory -Force -Path "$publishing_path"
    New-Item -ItemType Directory -Force -Path $storage_path
    Copy-File "pom.xml" (Join-Path $storage_path $pom)
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
