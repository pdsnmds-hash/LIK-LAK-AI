"""Prove Gradle signing in an isolated checkout with a disposable TEST certificate.
No app upload key or signed distribution AAB is created by this check.
"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'docs/codemagic'
env = {k: v for k, v in os.environ.items() if not k.startswith('CM_')}
java = Path(env['JAVA_HOME']) / 'bin'
with (OUT / 'release-smoke.log').open('w') as log, tempfile.TemporaryDirectory(prefix='liklak-release-test-') as directory:
    root = Path(directory) / 'checkout'
    root.mkdir()
    for folder in ['gradle', 'app/src', 'app/schemas']:
        shutil.copytree(ROOT / folder, root / folder)
    (root / 'content').mkdir()
    shutil.copy2(ROOT / 'content/enrichment.json', root / 'content/enrichment.json')
    for file in ['build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'app/build.gradle.kts']:
        shutil.copy2(ROOT / file, root / file)
    sdk = Path.home() / 'Library/Android/sdk'
    (root / 'local.properties').write_text('sdk.dir=' + str(sdk) + '\n')
    def run(args, ok=True):
        p = subprocess.run(list(map(str, args)), cwd=root, env=env, capture_output=True, text=True)
        log.write(p.stdout + p.stderr);log.flush()
        assert (p.returncode == 0) == ok, 'See release-smoke.log'
        return p
    blocked = run([root / 'gradlew', '--no-daemon', ':app:bundleRelease'], ok=False)
    assert 'Release signing is not configured.' in blocked.stdout + blocked.stderr
    log.write('\nPASS: release without signing credentials was rejected.\n')
    key = Path(directory) / 'TEST-ONLY.jks'
    env.update(CM_BUILD_DIR=str(root), CM_KEYSTORE_PATH=str(key), CM_KEY_ALIAS='test-only',
               CM_KEYSTORE_PASSWORD='throwaway-test-password', CM_KEY_PASSWORD='throwaway-test-password')
    run([java / 'keytool', '-genkeypair', '-noprompt', '-keystore', key, '-storetype', 'JKS',
         '-alias', env['CM_KEY_ALIAS'], '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1',
         '-dname', 'CN=Temporary release wiring test only', '-storepass:env', 'CM_KEYSTORE_PASSWORD',
         '-keypass:env', 'CM_KEY_PASSWORD'])
    run([sys.executable, OUT / 'prepare_signing.py'])
    run([root / 'gradlew', '--no-daemon', ':app:testDebugUnitTest', ':app:lintDebug', ':app:bundleRelease'])
    run([sys.executable, OUT / 'verify_aab.py', root / 'app/build/outputs/bundle/release/app-release.aab'])
    log.write('\nPASS: isolated signed AAB, matching test certificate and four ABI. Temporary checkout, AAB and test key removed.\n')
result = json.loads((OUT / 'local-validation.json').read_text())
result.update(release_gradle_test='PASS: isolated checkout, disposable TEST certificate; real AAB verified; test outputs deleted',
              unsigned_release_guard='PASS', gradle_tasks=['testDebugUnitTest', 'lintDebug', 'bundleRelease'])
(OUT / 'local-validation.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
print('PASS: release signing guard, isolated Gradle tests/lint/bundle, real AAB verifier; all temporary material removed.')
