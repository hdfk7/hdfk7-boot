$init_runtime_path = Get-Location
$project_list = @("hdfk7-boot-parent", "hdfk7-boot-proto\hdfk7-boot-base-proto", "hdfk7-boot-starter-code-generator", "hdfk7-boot-starter-common", "hdfk7-boot-starter-discovery")

foreach ($project in $project_list)
{
    $runtime_path = "$( Get-Location )\$project"

    Set-Location "$runtime_path"
    mvn clean

    Set-Location "$init_runtime_path"
}
