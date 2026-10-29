"""Inventory permissions from a merged manifest or every actual AAB module via bundletool.
This is a read-only post-build audit; it neither builds nor signs the app.
"""
import argparse
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ANDROID = '{http://schemas.android.com/apk/res/android}'
APP_ID = 'bg.liklak.sandbox'
OWN = APP_ID + '.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'


def inventory(xml, module):
    root = ET.fromstring(xml)
    definitions = {n.get(ANDROID + 'name'): n.get(ANDROID + 'protectionLevel') for n in root.findall('permission')}
    requests = []
    for node in root:
        if node.tag not in {'uses-permission', 'uses-permission-sdk-23', 'uses-permission-sdk-m'}:
            continue
        name = node.get(ANDROID + 'name', '')
        kind = ('system' if name.startswith('android.permission.') else
                'own_signature' if name == OWN and definitions.get(name) == 'signature' else 'other')
        requests.append({'name': name, 'kind': kind, 'element': node.tag,
                         'maxSdkVersion': node.get(ANDROID + 'maxSdkVersion')})
    return {'module': module, 'package': root.get('package'), 'definitions': definitions, 'requests': requests}


def main():
    parser = argparse.ArgumentParser()
    source = parser.add_mutually_exclusive_group(required=True)
    source.add_argument('--aab', type=Path)
    source.add_argument('--manifest', type=Path)
    parser.add_argument('--bundletool', type=Path)
    parser.add_argument('--java', default='java')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    if args.aab:
        if not args.bundletool or not args.bundletool.is_file():
            parser.error('--aab requires an existing --bundletool JAR')
        with zipfile.ZipFile(args.aab) as archive:
            modules = sorted(n.split('/')[0] for n in archive.namelist() if n.endswith('/manifest/AndroidManifest.xml'))
        if 'base' not in modules:
            raise SystemExit('No base module manifest in AAB')
        results = []
        for module in modules:
            xml = subprocess.check_output([args.java, '-jar', str(args.bundletool), 'dump', 'manifest',
                                           '--bundle=' + str(args.aab), '--module=' + module], text=True)
            (args.output / (module + '-manifest.xml')).write_text(xml)
            results.append(inventory(xml, module))
        mode = 'ACTUAL_AAB_BUNDLETOOL'
    else:
        results = [inventory(args.manifest.read_text(), 'base')]
        mode = 'MERGED_MANIFEST_ONLY_NOT_AAB'
    requests = [item for module in results for item in module['requests']]
    unexpected = [item for item in requests if item['kind'] != 'own_signature']
    valid_package = all(x['package'] == APP_ID for x in results)
    report = {'mode': mode, 'modules': results,
              'system_permissions': sorted({x['name'] for x in requests if x['kind'] == 'system'}),
              'own_signature_permissions': sorted({x['name'] for x in requests if x['kind'] == 'own_signature'}),
              'other_permissions': sorted({x['name'] for x in requests if x['kind'] == 'other'}),
              'result': 'PASS' if not unexpected and valid_package else 'FAIL'}
    (args.output / 'permissions.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
    if report['result'] != 'PASS':
        raise SystemExit(1)


if __name__ == '__main__':
    main()
