param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ComposeArgs
)

$ErrorActionPreference = "Stop"

Set-Location $PSScriptRoot

docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build @ComposeArgs
