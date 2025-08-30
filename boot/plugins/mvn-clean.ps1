$init_runtime_path = Get-Location
$project_list = @("hdfk7-boot-starter-parent", "hdfk7-code-generator", "hdfk7-common-sdk", "hdfk7-common-sdk\hdfk7-base-proto", "hdfk7-common-sdk\hdfk7-common-proto", "hdfk7-boot-starter-common", "hdfk7-boot-starter-discovery")

foreach ($project in $project_list)
{
    $runtime_path = "$( Get-Location )\$project"

    Set-Location "$runtime_path"
    mvn clean

    Set-Location "$init_runtime_path"
}
