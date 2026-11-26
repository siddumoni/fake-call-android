# Release signing on Windows 11 / WSL Ubuntu

Keep the keystore outside the repository and maintain a secure backup. Never paste a keystore, password or base64 content into chat. A lost signing key prevents future updates signed with the same key.

## Generate the keystore in WSL Ubuntu

```bash
mkdir -p "$HOME/.keys"
keytool -genkeypair -v \
  -keystore "$HOME/.keys/alibicall-release.jks" \
  -alias alibicall \
  -keyalg RSA -keysize 4096 -validity 10000
```

Choose strong unique passwords when prompted. Do not put passwords in shell history.

## Encode the keystore

```bash
base64 -w 0 "$HOME/.keys/alibicall-release.jks" > "$HOME/.keys/alibicall-release.jks.base64"
```

Open the base64 file in a secure local editor and copy its contents directly into GitHub's secret form. Do not print the secret to build logs or paste it into chat.

## Add repository Actions secrets

Open **Settings -> Secrets and variables -> Actions** and create:

| Secret | Value |
|---|---|
| `ALIBICALL_KEYSTORE_BASE64` | Contents of local base64 file |
| `ALIBICALL_STORE_PASSWORD` | Keystore store password |
| `ALIBICALL_KEY_ALIAS` | `alibicall` |
| `ALIBICALL_KEY_PASSWORD` | Private key password |

Only the tag-triggered release workflow receives these secrets. PR builds do not.

## Publish and verify

```bash
git tag v1.0.0
git push origin v1.0.0
sha256sum -c SHA256SUMS.txt
apksigner verify --verbose --print-certs AlibiCall-release.apk
```

When signing secrets exist, the tag workflow publishes a signed minified release APK and runs `apksigner verify --verbose --print-certs` before publication. When they do not exist, it publishes `AlibiCall-DEBUG.apk`, a debug-variant APK signed with Gradle's temporary Android debug key, plus a SHA-256 checksum. The debug signing identity is not suitable for production updates and may differ between CI runs. Never commit the keystore or upload it as a normal release asset.
