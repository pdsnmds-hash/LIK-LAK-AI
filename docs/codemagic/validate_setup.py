"""Local-only CI checks. Temporary test keys are deleted; never creates an upload key.
Requires Python 3, Ruby/Psych for YAML, and JAVA_HOME with JDK 17.
"""
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
HELPERS = ROOT / 'docs/codemagic'
JAVA = Path(os.environ['JAVA_HOME']) / 'bin'
checks = []


def run(args, env=None, ok=True):
    p = subprocess.run(list(map(str, args)), env=env, capture_output=True, text=True)
    if (p.returncode == 0) != ok:
        raise AssertionError('Unexpected exit status: ' + str(args[0]) + '\n' + p.stdout + p.stderr)
    return p


def helper(name, env, *args, ok=True):
    return run([sys.executable, HELPERS / name, *args], env=env, ok=ok)


for path in [ROOT / 'codemagic.yaml', HELPERS / 'generate-upload-key.yaml', HELPERS / 'codemagic.yaml']:
    parsed = json.loads(run(['ruby', '-ryaml', '-rjson', '-e',
        'puts JSON.generate(YAML.safe_load(File.read(ARGV[0])))', path]).stdout)
    for workflow in parsed['workflows'].values():
        assert workflow['environment']['java'] == 17
        for script in workflow['scripts']:
            p = subprocess.run(['bash', '-n'], input=script['script'], text=True, capture_output=True)
            assert p.returncode == 0, p.stderr
assert (ROOT / 'codemagic.yaml').read_bytes() == (HELPERS / 'generate-upload-key.yaml').read_bytes()
checks.append('Both workflow YAML files parse; all embedded bash syntax valid; root = generator')

with tempfile.TemporaryDirectory(prefix='liklak-ci-check-') as directory:
    tmp = Path(directory)
    env = {k: v for k, v in os.environ.items() if not k.startswith('CM_')}
    env['CM_BUILD_DIR'] = str(tmp / 'checkout')
    Path(env['CM_BUILD_DIR']).mkdir()
    helper('prepare_signing.py', env, ok=False)
    source = tmp / 'dummy.jks'
    source.write_bytes(b'temporary mock signing data')
    secret = ' a:=#!\\\t\n\r\fпароль🔑 '
    env.update(CM_KEYSTORE_PATH=str(source), CM_KEYSTORE_PASSWORD=secret,
               CM_KEY_ALIAS='upload:= ключ', CM_KEY_PASSWORD=secret + 'end')
    helper('prepare_signing.py', env)
    root = Path(env['CM_BUILD_DIR'])
    key = root / 'release/signing/upload-keystore.jks'
    props = root / 'release/upload-key.properties'
    assert key.read_bytes() == source.read_bytes()
    assert key.stat().st_mode & 0o777 == props.stat().st_mode & 0o777 == 0o600
    before = props.read_bytes()
    helper('prepare_signing.py', env, ok=False)
    assert props.read_bytes() == before
    java = tmp / 'ReadProperties.java'
    java.write_text('''import java.util.*;import java.io.*;
class ReadProperties {public static void main(String[] args)throws Exception {
Properties p=new Properties();try(var i=new FileInputStream(args[0])){p.load(i);}
for(String k:List.of("storeFile","storePassword","keyAlias","keyPassword"))
System.out.println(Base64.getEncoder().encodeToString(p.getProperty(k).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
}}''')
    import base64
    values = [base64.b64decode(s).decode() for s in run([JAVA / 'java', java, props]).stdout.splitlines()]
    assert values == ['release/signing/upload-keystore.jks', secret, env['CM_KEY_ALIAS'], secret + 'end']
    checks.append('Signing bridge: missing variables, file paths, 0600, no overwrite, Java Properties Unicode/special-character round-trip')

    sdk = tmp / 'mock SDK'
    for folder, revision in [('latest', '19.0'), ('latest-2', '20.0')]:
        tool = sdk / 'cmdline-tools' / folder / 'bin/sdkmanager'
        tool.parent.mkdir(parents=True)
        (tool.parents[1] / 'source.properties').write_text('Pkg.Revision=' + revision + '\n')
        tool.write_text('#!' + sys.executable + '\nimport json,os,sys\nfrom pathlib import Path\nPath(os.environ["SDK_CALL"]).write_text(json.dumps([sys.argv[0],*sys.argv[1:]]))\n')
        tool.chmod(0o700)
    env.update(ANDROID_SDK_ROOT=str(sdk), SDK_CALL=str(tmp / 'sdk-call.json'))
    helper('configure_sdk.py', env)
    call = json.loads(Path(env['SDK_CALL']).read_text())
    assert call == [str(sdk / 'cmdline-tools/latest-2/bin/sdkmanager'), '--sdk_root=' + str(sdk),
                    'platforms;android-37.0', 'build-tools;36.0.0']
    assert (root / 'local.properties').read_text() == 'sdk.dir=' + str(sdk) + '\n'
    checks.append('SDK mock: highest Pkg.Revision selected by full executable path; exact API 37.0 / build-tools 36.0.0 arguments')

    # Throwaway test certificate: never used as the app upload key.
    env.update(CM_KEYSTORE_PATH=str(tmp / 'test-only.jks'), CM_KEYSTORE_PASSWORD='temporary-test-password',
               CM_KEY_PASSWORD='temporary-test-password', CM_KEY_ALIAS='test-only')
    run([JAVA / 'keytool', '-genkeypair', '-noprompt', '-keystore', env['CM_KEYSTORE_PATH'],
         '-storetype', 'JKS', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1',
         '-alias', env['CM_KEY_ALIAS'], '-dname', 'CN=Temporary CI verification only',
         '-storepass:env', 'CM_KEYSTORE_PASSWORD', '-keypass:env', 'CM_KEY_PASSWORD'], env)
    def archive(name, signed=False, private=False, missing=False):
        path = tmp / name
        with zipfile.ZipFile(path, 'w') as z:
            for abi in ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']:
                if missing and abi == 'x86': continue
                z.writestr('base/lib/' + abi + '/libtest.so', 'mock native payload')
            if private: z.writestr('base/assets/secret.jks', 'mock secret')
        if signed:
            run([JAVA / 'jarsigner', '-keystore', env['CM_KEYSTORE_PATH'],
                 '-storepass:env', 'CM_KEYSTORE_PASSWORD', '-keypass:env', 'CM_KEY_PASSWORD',
                 path, env['CM_KEY_ALIAS']], env)
        return path
    helper('verify_aab.py', env, archive('valid.aab', signed=True))
    helper('verify_aab.py', env, archive('unsigned.aab'), ok=False)
    helper('verify_aab.py', env, archive('missing-abi.aab', signed=True, missing=True), ok=False)
    helper('verify_aab.py', env, archive('private.aab', signed=True, private=True), ok=False)
    checks.append('AAB verifier: matching JAR signature accepted; unsigned, missing ABI and embedded private file rejected')

checks.append('All temporary fixtures and test-only keys removed')
result = {'local_checks': checks, 'status': 'PASS', 'cloud_build': 'NOT RUN',
          'registered_upload_key': 'User confirmed 2026-10-29: app not uploaded, new key required',
          'sdk_package_ids': ['platforms;android-37.0', 'build-tools;36.0.0']}
(HELPERS / 'local-validation.json').write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n')
for check in checks: print('PASS: ' + check)
