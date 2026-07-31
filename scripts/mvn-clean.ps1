$InitialLocation = Get-Location
$RepositoryPath = Resolve-Path "$PSScriptRoot\.."
$BootPath = Join-Path $RepositoryPath "boot"
$Projects = @("hdfk7-boot-dependencies", "hdfk7-boot-parent", "hdfk7-boot-proto\hdfk7-boot-base-proto", "hdfk7-boot-starter-code-generator", "hdfk7-boot-starter-common", "hdfk7-boot-starter-discovery", "hdfk7-boot-starter-shardingsphere")

foreach ($Project in $Projects)
{
    $RuntimePath = Join-Path $BootPath $Project

    Set-Location "$RuntimePath"
    mvn clean

    Set-Location "$InitialLocation"
}
