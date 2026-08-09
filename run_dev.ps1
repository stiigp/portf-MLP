param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ComposeArgs
)

$ErrorActionPreference = "Stop"

docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build @ComposeArgs
