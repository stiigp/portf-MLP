param(
    [Alias("force-recreate")]
    [switch]$ForceRecreate,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ComposeArgs
)

$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

$UpArgs = @("up", "--build")

if ($ForceRecreate -and $ComposeArgs -notcontains "--force-recreate") {
    $UpArgs += "--force-recreate"
}

docker compose -f docker-compose.yml -f docker-compose.dev.yml @UpArgs @ComposeArgs
