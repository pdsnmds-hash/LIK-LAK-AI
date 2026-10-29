"""Source/content gate for the English-only app. Does not build Android binaries."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET
from english_content import english_only
ROOT=Path(__file__).resolve().parents[1]
checks=[]
for p in (ROOT/'app/src/main/java').rglob('*.kt'):
 assert not re.search('[\u0400-\u04ff]',p.read_text()),str(p)
checks.append('No Cyrillic text in production Kotlin')
for p in (ROOT/'app/src/main/assets').glob('*.json'):
 data=json.loads(p.read_text());assert data==english_only(data),str(p)
checks.append('Bundled JSON is English-only; compatibility fields contain identical English values')
assets=ROOT/'app/src/main/assets'
cases=json.loads((assets/'cases.json').read_text());books=json.loads((assets/'handbook.json').read_text())
assert len(cases)==12 and len(books)==110 and sum(len(b['sections']) for b in books)==503
assert len({b['id'] for b in books})==110
checks.append('12 executable cases, 110 books and 503 sections retained with stable unique IDs')
settings=(ROOT/'app/src/main/java/bg/liklak/sandbox/SettingsRepository.kt').read_text()
assert 'english: Boolean=true' in settings and 'Settings(true,' in settings and 'p[Keys.english]=true' in settings
for name in ['SettingsDialog.kt','Onboarding.kt']:
 text=(ROOT/'app/src/main/java/bg/liklak/sandbox'/name).read_text()
 assert 'onLanguage' not in text and 'ChoiceButton("English"' not in text
checks.append('Legacy language preference ignored on read; English persisted; language controls removed')
ns='{http://schemas.android.com/apk/res/android}'
locales=ET.parse(ROOT/'app/src/main/res/xml/locales_config.xml').getroot()
assert [n.get(ns+'name') for n in locales]==['en']
assert 'localeFilters += listOf("en")' in (ROOT/'app/build.gradle.kts').read_text()
checks.append('Android declared locales and library resource filters select only en')
listing=json.loads((ROOT/'store/google-play/listing.json').read_text())
assert listing['app_languages']==['en'] and list(listing['locales'])==['en-US']
assert not (ROOT/'store/google-play/bg-BG').exists()
checks.append('Store metadata and graphics locale set is English-only')
result={'result':'PASS','checks':checks,'android_build':'NOT RUN by user instruction','device_tests':'NOT RUN; require next Codemagic build'}
out=ROOT/'docs/english-only/verification.json';out.write_text(json.dumps(result,indent=2)+'\n')
for item in checks:print('PASS:',item)
