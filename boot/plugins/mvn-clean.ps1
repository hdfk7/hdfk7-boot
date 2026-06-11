$InitialLocation = Get-Location
$Projects = @("hdfk7-boot-parent", "hdfk7-boot-proto\hdfk7-boot-base-proto", "hdfk7-boot-starter-code-generator", "hdfk7-boot-starter-common", "hdfk7-boot-starter-discovery")

foreach ($Project in $Projects)
{
    $RuntimePath = Join-Path $InitialLocation $Project

    Set-Location "$RuntimePath"
    mvn clean

    Set-Location "$InitialLocation"
}
