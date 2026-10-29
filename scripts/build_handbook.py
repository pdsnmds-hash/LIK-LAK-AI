"""Build the authored handbook and execute only its explicitly local Python recipes."""
import json,pathlib,subprocess,sys,tempfile
from handbook_content import BOOKS as ORIGINAL_BOOKS
from merge_enrichment import merge_books
from english_content import english_only
BOOKS=english_only(merge_books(ORIGINAL_BOOKS))
root=pathlib.Path(__file__).resolve().parents[1]
for book in BOOKS:
 for section in book['sections']:
  if section['runnable']:
   with tempfile.TemporaryDirectory(prefix='liklak-recipe-') as folder:
    script=pathlib.Path(folder)/'solution.py'
    script.write_text(section['code'],encoding='utf-8')
    result=subprocess.run([sys.executable,str(script)],capture_output=True,text=True,timeout=10,cwd=folder)
    if result.returncode:raise RuntimeError(book['id']+'\n'+result.stderr)
    actual=result.stdout.strip()
    if 'expected_reference' in section and actual!=section['expected_reference']:
     raise AssertionError(book['id']+': stdout differs from imported reference')
    section['expected']=actual
path=root/'app/src/main/assets/handbook.json'
path.write_text(json.dumps(BOOKS,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Built {len(BOOKS)} guides, {sum(len(b["sections"]) for b in BOOKS)} chapters; {sum(s["runnable"] for b in BOOKS for s in b["sections"])} local Python recipes executed.')
for b in BOOKS:
 print(b['id'],len(b['sections']),'chapters',len(b['intro_en'].split())+sum(len(s['body_en'].split())+len(s['solution_en'].split()) for s in b['sections']),'EN prose words')

# Export the same content for review on a computer; Android reads the JSON above.
for book in BOOKS:
 for lang in ['en']:
  lines=['# '+book['title_'+lang],book['intro_'+lang]]
  for index,section in enumerate(book['sections'],1):
   lines+=['## '+str(index)+'. '+section['title_'+lang],section['body_'+lang]]
   if section['code']:lines+=['```'+section['language']+'\n'+section.get('code_'+lang,section['code'])+'\n```']
   if section['expected']:lines+=['### '+('Executed output' if lang=='en' else 'Изход от изпълнението'),'```text\n'+section['expected']+'\n```']
   if section['solution_'+lang]:lines+=['### '+('Solution and rationale' if lang=='en' else 'Решение и обосновка'),section['solution_'+lang]]
  lines+=['## '+('Official references' if lang=='en' else 'Официални източници')]
  lines+=['- ['+ref['title']+']('+ref['url']+')' for ref in book['references']]
  dest=root/'docs/handbook'/lang/(book['id']+'.md')
  dest.parent.mkdir(parents=True,exist_ok=True)
  dest.write_text('\n\n'.join(lines)+'\n',encoding='utf-8')
