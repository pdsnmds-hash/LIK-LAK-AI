# LIK LAK AI Sandbox: ключ и подписанный AAB через Codemagic

Подготовлено 2026-10-29 по `AGENT_HANDOFF.md` и прочитанным примерам NEW LOOK / Safora. Пользователь подтвердил: **приложение ещё не загружалось в Google Play, нужен новый ключ**. Upload key приложения локально не создавался. Генерация выполняется на build-машине Codemagic.

## Параметры проекта

| Параметр | Значение |
|---|---|
| Название | LIK LAK AI Sandbox |
| applicationId | `bg.liklak.sandbox` |
| Модуль / Gradle | `app` / `app/build.gradle.kts` |
| versionName / versionCode | `1.7.0` / `8` |
| min / target / compile SDK | `26` / `36` / `37` |
| Java / Gradle / AGP | `17` / `9.6.1` / `9.4.1` |
| ABI | `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` |
| Release properties | `release/upload-key.properties` |
| Keystore из identity | `release/signing/upload-keystore.jks` |
| Разрешение storeFile | От корня проекта через `rootProject.file(...)` |
| Минификация | Выключена; mapping.txt не создаётся |

До изменений release signingConfig отсутствовал. Локальных upload JKS / keystore / PEM в проекте не найдено. Debug использует штатную подпись Android и не требует приватных файлов. SDK, applicationId и versionCode для CI не менялись. Если к моменту загрузки versionCode 8 уже использован, увеличьте его перед новой сборкой.

## 1. Подготовка Git

На момент подготовки папка не была Git-репозиторием; remote не задан. Codemagic собирает commit из подключённого GitHub / GitLab / Bitbucket, а не папку Mac. Создайте пустой репозиторий в своём аккаунте и выполните из корня проекта:

```sh
git init
git branch -M main
git add .gitignore README.md build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle app/build.gradle.kts app/src app/schemas content/enrichment.json docs/codemagic codemagic.yaml
git diff --cached --name-only
git commit -m "Configure Codemagic key generation and signed AAB builds"
git remote add origin YOUR_REPOSITORY_URL
git push -u origin main
```

Замените YOUR_REPOSITORY_URL. В ignore внесены JKS, keystore, приватные properties, generated-signing, local.properties, APK/AAB, ZIP и dist. Не добавляйте всю папку командой `git add -f`. Если ключ когда-либо попал в Git, ignore не удалит его из истории.

Файл `content/enrichment.json` обязательно должен попасть в Git: пять unit-тестов сверяют с ним встроенный решебник. Он является тестовыми данными, а не credential. Проверка временного чистого checkout выявила эту зависимость; список файлов выше учитывает её.

В Codemagic: Applications → Add application → подключите репозиторий, выберите конфигурацию через `codemagic.yaml`.

## 2. Одноразовая генерация ключа

**Корневой `codemagic.yaml` сейчас содержит только генератор** `liklak-create-upload-key-once`. Его резервная копия — `docs/codemagic/generate-upload-key.yaml`.

В Codemagic Environment variables создайте группу **`liklak_key_generation`**. Добавьте два значения с включённым **Secret**:

- `NEW_KEYSTORE_PASSWORD`;
- `NEW_KEY_PASSWORD`.

Сохраните пароли в менеджере паролей. Не отправляйте их в чат, Git или YAML.

Выберите ветку с этим commit → Start new build → **LIK LAK AI Sandbox - ONE-TIME create upload key**.

Параметры генерации: Linux X2 / Java 17, JKS, RSA 4096, SHA256withRSA, 10000 дней, alias **`upload`**, subject **`CN=LIK LAK AI Sandbox`**. Сохранены обязательные проверки секретов, umask 077 и отказ перезаписи существующего JKS.

Скачайте оба артефакта:

- `generated-signing/liklak-upload.jks` — приватный keystore;
- `generated-signing/upload_certificate.pem` — публичный сертификат.

Храните резервную копию JKS и пароли отдельно. Этот workflow запускается **один раз**: повторный запуск на чистой машине создаст другой ключ даже при одинаковом имени файла и alias. Генерация JKS уже создаёт upload key; вторую генерацию делать не нужно.

## 3. Code signing identities

Team settings → codemagic.yaml settings → Code signing identities → Android keystores:

| Поле | Значение |
|---|---|
| File | скачанный `liklak-upload.jks` |
| Keystore password | сохранённый `NEW_KEYSTORE_PASSWORD` |
| Key alias | **`upload`** |
| Key password | сохранённый `NEW_KEY_PASSWORD` |
| Reference name | **`liklak_upload`** |

Alias и Reference name — разные значения. [Официальная инструкция Codemagic](https://docs.codemagic.io/yaml-code-signing/signing-android/).

## 4. Переключение на AAB-сборку

После скачивания и резервного копирования ключа и добавления identity:

```sh
cp docs/codemagic/codemagic.yaml codemagic.yaml
git add codemagic.yaml
git commit -m "Enable signed LIK LAK AAB workflow"
git push
```

Теперь в корне обычный workflow **`liklak-aab`**, имя **LIK LAK AI Sandbox - Signed AAB**. Он ссылается на identity **`liklak_upload`**. Не запускайте генератор для обновлений приложения.

Codemagic автоматически предоставляет CM_KEYSTORE_PATH, CM_KEYSTORE_PASSWORD, CM_KEY_ALIAS, CM_KEY_PASSWORD. Скрипт prepare_signing.py переносит их в требуемые Gradle файлы: права 600, корректное Java Properties escaping, отказ перезаписи. Значения паролей не выводятся. Gradle подключает эти properties к release signingConfig; release без properties намеренно завершается ошибкой, debug работает без них.

Выберите обновлённую ветку → Start new build → **LIK LAK AI Sandbox - Signed AAB**.

Workflow:

1. Находит sdkmanager по полному пути внутри SDK, выбирая максимальную Pkg.Revision из source.properties.
2. Устанавливает **`platforms;android-37.0`** и **`build-tools;36.0.0`**. Именно эти package IDs сверены с [репозиторием Google](https://dl.google.com/android/repository/repository2-3.xml). compileSdk в Gradle остаётся 37; `platforms;android-37` здесь неверен.
3. Подготавливает подпись из identity.
4. Выполняет `./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:bundleRelease`.
5. Проверяет подпись AAB, совпадение сертификата с identity, четыре ABI и отсутствие приватных signing-файлов внутри bundle.

Не используйте printenv, set -x или вывод приватных properties при диагностике.

Артефакты обычного workflow:

- `app/build/outputs/bundle/release/app-release.aab`;
- HTML Android Lint и отчёт unit tests;
- mapping.txt — только если позднее будет включён R8 (сейчас отсутствует).

Обычный workflow не отдаёт JKS в артефакты и не публикует приложение в Google Play. App signing key Google Play отличается от upload key: для нового приложения управление первым можно предоставить Google. [Подпись Android](https://developer.android.com/studio/publish/app-signing).

## 5. Локальные проверки и границы результата

Результаты — `local-validation.json`, `helper-validation.log`, `gradle-validation.log`, `release-smoke.log`. Состав файлов для Git проверен через временный индекс: `git-preview.json` и `git-files-preview.txt`; настоящий репозиторий и remote этим действием не создавались.

Проверены YAML и bash syntax, signing bridge с Unicode и спецсимволами через настоящий Java Properties, обязательные CM_*, права 600 и отказ перезаписи; SDK на mock-каталоге с двумя ревизиями sdkmanager; verifier на временных подписанном и неподписанном архивах, отсутствующем ABI и приватном файле. В отдельной временной копии проверяется реальный Gradle bundleRelease с одноразовым **тестовым** сертификатом и все четыре ABI; тестовые ключи и AAB удаляются. Этот сертификат не является ключом приложения и не загружается в identities.

Повтор локальной проверки с JDK Android Studio:

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
python3 docs/codemagic/validate_setup.py
python3 docs/codemagic/validate_release.py
```

Первый скрипт использует установленный Ruby/Psych для разбора YAML. Эти проверки предназначены для локальной валидации, не запускают генератор приложения и не вызывают Codemagic API.

**Облачный build: NOT RUN.** Подготовленный YAML и локальные проверки не подтверждают успех облака. После запуска зафиксируйте build ID, commit, статус и скачанные артефакты реального Codemagic build.
