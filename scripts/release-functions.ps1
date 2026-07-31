$InitialLocation = Get-Location
$RepositoryPath = Resolve-Path "$PSScriptRoot\.."
$BootPath = Join-Path $RepositoryPath "boot"
$MavenConfigPath = Join-Path $RepositoryPath ".mvn\maven.config"
$RevisionLine = Get-Content $MavenConfigPath | Where-Object { $_ -like "-Drevision=*" } | Select-Object -First 1
$ChangelistLine = Get-Content $MavenConfigPath | Where-Object { $_ -like "-Dchangelist=*" } | Select-Object -First 1
$Revision = $RevisionLine -replace "^-Drevision=", ""
$Changelist = $ChangelistLine -replace "^-Dchangelist=", ""

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

function Invoke-ProjectRelease
{
    param(
        [string[]]$Projects
    )

    foreach ($Project in $Projects)
    {
        $ProjectParts = $Project -split "\\"
        $ProjectName = $ProjectParts[-1]

        $RuntimePath = Join-Path $BootPath $Project
        $TargetPath = Join-Path $RuntimePath "target"

        $ProjectRevision = $Revision
        $ProjectChangelist = $Changelist
        $ProjectMavenConfigPath = Join-Path (Join-Path $BootPath $ProjectParts[0]) ".mvn\maven.config"
        if (Test-Path -Path $ProjectMavenConfigPath -PathType Leaf)
        {
            $ProjectRevisionLine = Get-Content $ProjectMavenConfigPath | Where-Object { $_ -like "-Drevision=*" } | Select-Object -First 1
            $ProjectChangelistLine = Get-Content $ProjectMavenConfigPath | Where-Object { $_ -like "-Dchangelist=*" } | Select-Object -First 1
            if ($ProjectRevisionLine)
            {
                $ProjectRevision = $ProjectRevisionLine -replace "^-Drevision=", ""
            }
            if ($ProjectChangelistLine)
            {
                $ProjectChangelist = $ProjectChangelistLine -replace "^-Dchangelist=", ""
            }
        }
        $ProjectVersionName = "$ProjectRevision$ProjectChangelist"

        $StagingPath = Join-Path $TargetPath "central-staging"
        $PublishingPath = Join-Path $TargetPath "central-publishing"
        $StoragePath = Join-Path $StagingPath "cn\hdfk7\boot\$ProjectName\$ProjectVersionName"
        $PomFileName = "$ProjectName-$ProjectVersionName.pom"
        $JarFileName = "$ProjectName-$ProjectVersionName.jar"
        $JavadocFileName = "$ProjectName-$ProjectVersionName-javadoc.jar"
        $SourcesFileName = "$ProjectName-$ProjectVersionName-sources.jar"

        Set-Location "$RuntimePath"
        mvn clean deploy

        if (-not (Get-Command gpg -ErrorAction SilentlyContinue))
        {
            Write-Host "no signature files were generated"
            Set-Location "$InitialLocation"
            continue
        }

        New-Item -ItemType Directory -Force -Path "$PublishingPath"
        New-Item -ItemType Directory -Force -Path $StoragePath
        $PomPath = Join-Path $TargetPath ".flattened-pom.xml"
        $JarPath = Join-Path $TargetPath $JarFileName
        $JavadocPath = Join-Path $TargetPath $JavadocFileName
        $SourcesPath = Join-Path $TargetPath $SourcesFileName

        Copy-ExistingFile $PomPath (Join-Path $StoragePath $PomFileName)
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
