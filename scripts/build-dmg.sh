#!/usr/bin/env bash

set -euo pipefail

skip_tests=false
notarize=true
download_base_url="https://seudominio.com.br/downloads"
release_notes="Correcoes de bugs e melhorias gerais."

usage() {
    echo "Uso: $0 [--skip-tests] [--no-notarize] [--download-base-url URL] [--release-notes TEXTO]"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-tests)
            skip_tests=true
            shift
            ;;
        --no-notarize)
            notarize=false
            shift
            ;;
        --download-base-url)
            download_base_url="${2:-}"
            shift 2
            ;;
        --release-notes)
            release_notes="${2:-}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Argumento desconhecido: $1" >&2
            usage
            exit 1
            ;;
    esac
done

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Comando obrigatorio nao encontrado: $1" >&2
        exit 1
    fi
}

require_env() {
    if [[ -z "${!1:-}" ]]; then
        echo "Variavel de ambiente obrigatoria nao definida: $1" >&2
        exit 1
    fi
}

url_encode() {
    local raw="$1"
    local encoded=""
    local i char hex
    for ((i = 0; i < ${#raw}; i++)); do
        char="${raw:i:1}"
        case "$char" in
            [a-zA-Z0-9.~_-])
                encoded+="$char"
                ;;
            *)
                printf -v hex '%%%02X' "'$char"
                encoded+="$hex"
                ;;
        esac
    done
    printf '%s' "$encoded"
}

json_escape() {
    local value="$1"
    value="${value//\\/\\\\}"
    value="${value//\"/\\\"}"
    value="${value//$'\n'/\\n}"
    value="${value//$'\r'/\\r}"
    value="${value//$'\t'/\\t}"
    printf '%s' "$value"
}

if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "O DMG precisa ser gerado em macOS." >&2
    exit 1
fi

require_command java
require_command jpackage
require_command xcrun
require_command shasum
require_command codesign
require_command spctl
require_command security

if [[ ! -x "./mvnw" ]]; then
    echo "Maven Wrapper nao encontrado ou sem permissao de execucao: ./mvnw" >&2
    exit 1
fi

jpackage_version="$(jpackage --version | tr -d '\r')"
if [[ "$jpackage_version" != 21* ]]; then
    echo "JDK 21 com jpackage e obrigatorio. Versao encontrada: $jpackage_version" >&2
    exit 1
fi

require_env MAC_SIGNING_KEY_USER_NAME

if ! security find-identity -v -p codesigning | grep -F "$MAC_SIGNING_KEY_USER_NAME" >/dev/null 2>&1; then
    echo "Certificado de assinatura nao encontrado no Keychain: $MAC_SIGNING_KEY_USER_NAME" >&2
    echo "Use o nome completo, por exemplo: Developer ID Application: Sua Empresa (TEAMID)" >&2
    exit 1
fi

if [[ "$notarize" == true ]]; then
    require_env APPLE_ID
    require_env APPLE_TEAM_ID
    require_env APPLE_APP_PASSWORD
fi

echo "Build macOS DMG do SMS"
echo "Versao jpackage: $jpackage_version"
echo "Assinatura: $MAC_SIGNING_KEY_USER_NAME"
echo ""

if [[ "$skip_tests" == true ]]; then
    ./mvnw clean package -DskipTests
else
    ./mvnw clean package
fi

./mvnw jpackage:jpackage -Pmac-dmg -DskipTests

dmg_path=""
if [[ -d "$repo_root/target/dist" ]]; then
    dmg_path="$(find "$repo_root/target/dist" -maxdepth 1 -name "*.dmg" -type f \
        -exec stat -f "%m %N" {} \; \
        | sort -rn \
        | head -n 1 \
        | cut -d " " -f 2-)"
fi

if [[ -z "$dmg_path" ]]; then
    echo "DMG nao encontrado em target/dist." >&2
    exit 1
fi

if [[ "$notarize" == true ]]; then
    echo ""
    echo "Enviando para notarizacao Apple..."
    xcrun notarytool submit "$dmg_path" \
        --apple-id "$APPLE_ID" \
        --team-id "$APPLE_TEAM_ID" \
        --password "$APPLE_APP_PASSWORD" \
        --wait

    echo ""
    echo "Aplicando staple..."
    xcrun stapler staple "$dmg_path"
    xcrun stapler validate "$dmg_path"

    echo ""
    echo "Validando Gatekeeper..."
    spctl --assess --type open --verbose "$dmg_path"
else
    echo ""
    echo "Notarizacao ignorada por --no-notarize."
fi

project_version="$(./mvnw -q help:evaluate -Dexpression=project.version -DforceStdout)"
hash="$(shasum -a 256 "$dmg_path" | awk '{print tolower($1)}')"
base_url="${download_base_url%/}"
filename="$(basename "$dmg_path")"
download_url="$base_url/$(url_encode "$filename")"
published_at="$(date +%Y-%m-%d)"
update_json_path="$repo_root/target/dist/update-mac.json"

cat > "$update_json_path" <<JSON
{
  "latestVersion": "$(json_escape "$project_version")",
  "downloadUrl": "$(json_escape "$download_url")",
  "sha256": "$hash",
  "releaseNotes": "$(json_escape "$release_notes")",
  "mandatory": false,
  "publishedAt": "$published_at"
}
JSON

echo ""
echo "DMG gerado:"
ls -lh "$dmg_path"

echo ""
echo "Manifest macOS gerado:"
ls -lh "$update_json_path"