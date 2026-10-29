# Задание другому агенту: keystore, upload key и AAB в Codemagic

Настрой подпись Android-приложения по процессу Safora → NEW LOOK: сначала отдельная одноразовая генерация ключа на build-машине Codemagic, затем загрузка JKS в identities и обычная подписанная AAB-сборка. Не подменяй этот процесс генерацией ключа на компьютере пользователя.

## Исходные файлы

Проверенный локально пример NEW LOOK:

`/Users/nomads_dm/Desktop/app_google/progrev/BG_NEW LOOK/docs/codemagic/`

Прочитай и адаптируй:

- `generate-upload-key.yaml` — одноразовый генератор на Codemagic;
- `codemagic.yaml` — обычная AAB-сборка, отдельная от генератора;
- `configure_sdk.py` — выбор sdkmanager и установка точных SDK packages;
- `prepare_signing.py` — соединение Codemagic identities с существующим Gradle;
- `verify_aab.py` — подпись, выбранный сертификат, четыре ABI, отсутствие signing-файлов в AAB;
- `README.md` — последовательность действий пользователя;
- `local-validation.json` — границы выполненных локальных проверок.

Второй исходный пример:
`/Users/nomads_dm/Desktop/app_google/progrev/HUN_SAFORA/`.
Бери исправленный корневой YAML Safora, а не первоначальную копию с прямым вызовом sdkmanager через PATH.

Если эти пути недоступны, запроси исходные файлы или найди их в предоставленном репозитории. Не утверждай, что скопировал пример, который не смог прочитать.

## 1. Сначала изучи целевое приложение

Прочитай применимые AGENTS.md / CLAUDE.md, Gradle и Git status. Определи:

- applicationId, Android-модуль и файл Gradle;
- compileSdk, targetSdk, Java и требования Android Gradle Plugin;
- versionCode и фактическую конфигурацию ABI;
- путь properties, способ разрешения storeFile, release signingConfig;
- наличие локального ключа, его публичного сертификата и сведения о регистрации в Play Console.

Наличие локального AAB не означает регистрацию upload key в Google Play. Если регистрация неизвестна, уточни её у пользователя до генерации нового ключа; параллельно подготовь файлы. Если upload key уже зарегистрирован, сохраняй его: замена требует процедуры сброса upload key. Не перезаписывай и не удаляй существующие ключи.

Факты NEW LOOK, не универсальные значения:

| Параметр | NEW LOOK |
|---|---|
| applicationId | bg.newlook.organizer |
| Модуль | app |
| Gradle | app/build.gradle.kts |
| Java | 17 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |
| versionCode | 2 на момент адаптации |
| properties | release/upload-key.properties |
| storeFile | разрешается относительно корня проекта |
| Минификация | отключена; mapping.txt не создаётся |

## 2. Подготовь файлы для Git

В корне выбранной ветки должен быть `codemagic.yaml`. Codemagic получает код из Git, а не из локальной папки Mac.

Добавь в ignore JKS, keystore, приватные properties, generated-signing, local.properties, готовые APK/AAB и архивы с приватными данными. Проверь staged files; .gitignore не удаляет уже отслеживаемые секреты.

Для чистого checkout проверь debug-подпись. В NEW LOOK приватный debug/newlook-debug.keystore сделали необязательным: если он отсутствует, CI использует стандартную debug-подпись Android. Не коммить приватный debug key ради testDebugUnitTest/lintDebug.

Не меняй applicationId, SDK или versionCode только ради переноса на CI. VersionCode увеличивается перед загрузкой, если прежнее значение уже использовано в Play.

## 3. Одноразовая генерация именно в Codemagic

Адаптируй `generate-upload-key.yaml`, заменив NEW LOOK значения:

- workflow ID/name;
- группу `newlook_key_generation` → `<app>_key_generation`;
- имя `newlook-upload.jks` → `<app>-upload.jks`;
- KEY_SUBJECT — фактическое имя владельца/бренда, без выдуманной организации или страны.

Alias `upload` можно оставить. Пользователь создаёт в Codemagic Environment variables группу `<app>_key_generation` и два значения с включённым Secret:

- NEW_KEYSTORE_PASSWORD;
- NEW_KEY_PASSWORD.

Пароли пользователь хранит в менеджере паролей, не передаёт в чат и не записывает в YAML.

Генератор использует Java 17 / Linux X2 и команды:

```sh
keytool -genkeypair -noprompt \
  -keystore "generated-signing/$KEYSTORE_NAME" \
  -storetype JKS -keyalg RSA -keysize 4096 \
  -validity 10000 -sigalg SHA256withRSA \
  -alias "$UPLOAD_KEY_ALIAS" -dname "$KEY_SUBJECT" \
  -storepass:env NEW_KEYSTORE_PASSWORD \
  -keypass:env NEW_KEY_PASSWORD

keytool -exportcert -rfc \
  -keystore "generated-signing/$KEYSTORE_NAME" \
  -alias "$UPLOAD_KEY_ALIAS" \
  -storepass:env NEW_KEYSTORE_PASSWORD \
  -file generated-signing/upload_certificate.pem
```

Сохрани проверки обязательных секретов, umask 077 и отказ перезаписывать файл из исходного YAML.

Только генератор возвращает приватный JKS в артефакты:

```yaml
artifacts:
  - generated-signing/*.jks
  - generated-signing/upload_certificate.pem
```

Пользователь запускает его один раз, скачивает оба файла и сохраняет резервную копию JKS. Генерация keystore одновременно создаёт upload key — отдельной второй генерации нет. Повторный запуск на чистой build-машине создаст другой ключ, даже при том же имени и alias.

## 4. Загрузка в Code signing identities

Пользователь открывает:

Team settings → codemagic.yaml settings → Code signing identities → Android keystores.

Загружает скачанный JKS и указывает:

| Поле | Значение |
|---|---|
| Keystore password | NEW_KEYSTORE_PASSWORD |
| Key alias | upload, если генератор не менялся |
| Key password | NEW_KEY_PASSWORD |
| Reference name | <app>_upload |

Alias — запись внутри JKS. Reference name — идентификатор Codemagic. Это разные сущности. У прежнего локального NEW LOOK ключа alias `newlook-upload`, у облачного генератора — `upload`; при использовании старого ключа не подставляй новый alias.

## 5. Переключение на обычную AAB-сборку

После загрузки identity замени корневой YAML адаптированной обычной конфигурацией из `docs/codemagic/codemagic.yaml`. Одноразовый генератор больше не запускается.

```yaml
environment:
  java: 17
  android_signing:
    - <app>_upload
```

Codemagic предоставляет CM_KEYSTORE_PATH, CM_KEYSTORE_PASSWORD, CM_KEY_ALIAS, CM_KEY_PASSWORD. Не печатай значения, не используй printenv / set -x / cat приватных properties.

Подключение android_signing само по себе недостаточно: Gradle release должен использовать этот ключ. Адаптируй prepare_signing.py к фактической конфигурации приложения.

Для NEW LOOK скрипт создаёт:

- release/signing/upload-keystore.jks;
- release/upload-key.properties;
- storeFile=release/signing/upload-keystore.jks.

Для Safora properties находился в release/signing и storeFile был относителен другому каталогу. Не копируй эти пути вслепую. Сохрани проверку CM_* переменных, Java Properties escaping, права 600 и отказ перезаписывать существующие файлы. Локальные существующие ключи не используй как тестовую площадку.

## 6. Два исправления SDK, которые нельзя потерять

1. На Linux X2 sdkmanager отсутствовал в PATH. Ищи executable внутри Android SDK и вызывай по полному пути.
2. Первый NEW LOOK cloud build завершился ошибкой `Failed to find package 'platforms;android-37'`. В репозитории Google пакет называется `platforms;android-37.0`. Локально также установлен platforms/android-37.0. Gradle compileSdk=37 при этом оставлен без изменений.

В NEW LOOK configure_sdk.py выбирает инструмент с максимальной Pkg.Revision из source.properties: первая папка latest может содержать старый sdkmanager, а latest-2 — более новый. Не выбирай инструмент только по имени папки.

Перед адаптацией сверяй точный package ID нужного SDK с официальным репозиторием:
https://dl.google.com/android/repository/repository2-3.xml

Пакеты NEW LOOK: platforms;android-37.0 и build-tools;36.0.0. Это пример, а не значения для любого приложения. Не понижай compileSdk ради отсутствующего package name. Предупреждения XML v3/v4 и inconsistent location — не замена конечной ошибке: прочитай конец лога.

## 7. Сборка, проверки и артефакты

Адаптируй имя модуля и выполни:

```sh
./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:bundleRelease
python3 docs/codemagic/verify_aab.py app/build/outputs/bundle/release/app-release.aab
```

Verifier подтверждает JAR-подпись, совпадение сертификата с identity, наличие требуемых ABI и отсутствие приватных signing-файлов внутри AAB. NEW LOOK требует все четыре ABI; для другого приложения сначала установи требования.

Обычные артефакты:

```yaml
artifacts:
  - app/build/outputs/bundle/release/*.aab
  - app/build/outputs/mapping/release/mapping.txt
  - app/build/reports/lint-results-debug.html
  - app/build/reports/tests/testDebugUnitTest/**
```

Если R8 выключен, mapping.txt отсутствует — не обещай его. При включённом R8 сохраняй mapping именно данного билда. Обычная сборка не возвращает JKS и не публикует приложение в Play; стадия Publishing может означать только выдачу артефактов. App signing key Google Play — отдельный ключ, для нового приложения его создание можно оставить Google.

## 8. Что проверить локально и что передать пользователю

Проверь YAML и bash syntax; signing bridge на временных данных: права 600, пути, обязательные CM_*, отказ перезаписи, Java Properties round-trip с Unicode/спецсимволами. Проверь выбор sdkmanager и точные аргументы на mock SDK. Подпись verifier проверяй на временном подписанном архиве с четырьмя ABI и неподписанном отрицательном примере. Тестовые ключи после проверки удаляй.

В результате дай:

1. Файлы обоих workflow, helper scripts и инструкцию.
2. Что сейчас находится в корневом YAML: генератор или AAB-сборка.
3. Точные названия secret group, alias, Reference name и артефактов.
4. Команды commit/push и следующий шаг в Codemagic.
5. Результаты локальных проверок отдельно от реального облачного билда.

Не объявляй облачную сборку успешной по YAML, mock-тесту или старому AAB. Нужны статус реального Codemagic build, commit и его артефакты. По зафиксированным в этом проекте данным локальные проверки прошли; после исправления SDK cloud rebuild в local-validation.json имеет статус NOT RUN. Это не утверждение о более поздних запусках, которые агент ещё не изучил.

Официальные источники для проверки актуального UI и условий:
- https://docs.codemagic.io/yaml-code-signing/signing-android/
- https://docs.codemagic.io/yaml-quick-start/building-a-native-android-app/
- https://developer.android.com/studio/publish/app-signing
