$InitialLocation = Get-Location
$RepositoryPath = Resolve-Path "$PSScriptRoot\..\.."
$MavenConfigPath = Join-Path $RepositoryPath ".mvn\maven.config"
$RevisionLine = Get-Content $MavenConfigPath | Where-Object { $_ -like "-Drevision=*" } | Select-Object -First 1
$VersionName = $RevisionLine -replace "^-Drevision=", ""

function New-ReleaseFileSignature
{
    param(
        [string]$FileSource
    )
    if (Test-Path -Path $FileSource -PathType Leaf)
    {
        gpg --armor --output "$FileSource.asc" --detach-sig $FileSource
        $Algorithms = @("MD5", "SHA1", "SHA256", "SHA512")
        foreach ($Algorithm in $Algorithms)
        {
            $LowerAlgorithm = $Algorithm.ToLower()
            Get-FileHash $FileSource -Algorithm $Algorithm | ForEach-Object { $_.Hash } | Set-Content "$FileSource.$LowerAlgorithm"
        }
    }
}

function Copy-ExistingFile
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

function Copy-ReleasePom
{
    param(
        [string]$PomSource,
        [string]$PomDestination
    )
    $Utf8Encoding = New-Object System.Text.UTF8Encoding $false
    $PomContent = [System.IO.File]::ReadAllText((Resolve-Path $PomSource), [System.Text.Encoding]::UTF8)
    $PomContent = $PomContent.Replace('${revision}', $VersionName)
    [System.IO.File]::WriteAllText($PomDestination, $PomContent, $Utf8Encoding)
}

function Install-ReleaseArtifact
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

function Invoke-ProjectRelease
{
    param(
        [string[]]$Projects
    )

    foreach ($Project in $Projects)
    {
        $ProjectParts = $Project -split "\\"
        $ProjectName = $ProjectParts[-1]

        $RuntimePath = Join-Path $InitialLocation $Project
        $TargetPath = Join-Path $RuntimePath "target"
        $StagingPath = Join-Path $TargetPath "central-staging"
        $PublishingPath = Join-Path $TargetPath "central-publishing"
        $StoragePath = Join-Path $StagingPath "cn\hdfk7\$ProjectName\$VersionName"
        $PomFileName = "$ProjectName-$VersionName.pom"
        $JarFileName = "$ProjectName-$VersionName.jar"
        $JavadocFileName = "$ProjectName-$VersionName-javadoc.jar"
        $SourcesFileName = "$ProjectName-$VersionName-sources.jar"

        Set-Location "$RuntimePath"
        mvn clean install
        New-Item -ItemType Directory -Force -Path "$PublishingPath"
        New-Item -ItemType Directory -Force -Path $StoragePath
        $ReleasePomPath = Join-Path $StoragePath $PomFileName
        $JarPath = Join-Path "target" $JarFileName
        $JavadocPath = Join-Path "target" $JavadocFileName
        $SourcesPath = Join-Path "target" $SourcesFileName
        Copy-ReleasePom "pom.xml" $ReleasePomPath
        Install-ReleaseArtifact $ReleasePomPath $JarPath
        Copy-ExistingFile $JarPath (Join-Path $StoragePath $JarFileName)
        Copy-ExistingFile $JavadocPath (Join-Path $StoragePath $JavadocFileName)
        Copy-ExistingFile $SourcesPath (Join-Path $StoragePath $SourcesFileName)

        Set-Location $StoragePath
        New-ReleaseFileSignature $PomFileName
        New-ReleaseFileSignature $JarFileName
        New-ReleaseFileSignature $JavadocFileName
        New-ReleaseFileSignature $SourcesFileName

        Set-Location "$StagingPath"
        Compress-Archive -Path ".\*" -DestinationPath (Join-Path $PublishingPath "central-bundle.zip")

        Set-Location "$InitialLocation"
    }
}
