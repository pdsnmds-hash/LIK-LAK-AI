"""Exercise existing home destinations and capture 411/380/320dp on dedicated emulator."""
import subprocess,re,time,json,xml.etree.ElementTree as E
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/home-refresh'
ADB=str(Path.home()/'Library/Android/sdk/platform-tools/adb')
def call(*a):return subprocess.check_output([ADB,'-s','emulator-5572',*map(str,a)])
def sh(*a):return call('shell',*a)
def nodes():
 sh('uiautomator','dump','/sdcard/home-refresh.xml')
 return list(E.fromstring(sh('cat','/sdcard/home-refresh.xml')).iter('node'))
def text():return '\n'.join(n.get('text','') for n in nodes())
def tap(label,scroll=False):
 for _ in range(10 if scroll else 2):
  for n in nodes():
   if n.get('text')==label:
    b=list(map(int,re.findall(r'\d+',n.get('bounds',''))))
    if len(b)==4 and b[2]>b[0] and b[3]>b[1]:sh('input','tap',(b[0]+b[2])//2,(b[1]+b[3])//2);return
  if scroll:sh('input','swipe',540,1850,540,850,300)
 raise AssertionError(label+'\n'+text())
def launch():
 sh('am','force-stop','bg.liklak.sandbox');sh('am','start','-W','-n','bg.liklak.sandbox/.MainActivity')
def shot(name):nodes();(OUT/name).write_bytes(call('exec-out','screencap','-p'))
size=sh('wm','size').decode();density=sh('wm','density').decode()
s=re.search(r'Override size: (\S+)',size);d=re.search(r'Override density: (\d+)',density)
try:
 sh('wm','density',420)
 for width in [411,380,320]:
  px=round(width*420/160);sh('wm','size',f'{px}x{round(px*2400/1080)}');launch();t=text()
  for label in ['Documents','Requests','Data & tables','Code & tests','All 36 cases →']:assert label in t,(width,label)
  assert not re.search('[\u0400-\u04ff]',t)
  shot(f'home-{width}.png');print('PASS: home task controls visible, English, '+str(width)+'dp',flush=True)
 sh('wm','size','1080x2400')
 for label,selection in [('Documents','Documents'),('Requests','Customer care'),('Data & tables','Data'),('Code & tests','Development')]:
  launch();tap(label);t=text();assert selection in t,(label,t)
  assert '0 / CASES' not in t;sh('input','keyevent',4);assert 'All 36 cases →' in text()
  print('PASS: '+label+' -> existing filtered catalogue -> back home',flush=True)
 launch();tap('All 36 cases →');assert '36 / CASES' in text();sh('input','keyevent',4)
 tap('Explore case →',scroll=True);assert '01 Overview' in text();sh('input','keyevent',4)
 launch();tap('Build your plan →',scroll=True);assert 'Choose a workflow.' in text();sh('input','keyevent',4)
 launch();tap('My saved materials →',scroll=True);assert 'Keep it' in text() or 'Keep the idea.' in text() or 'AUTOMATION PLAN' in text()
 print('PASS: full catalogue, featured case, existing builder and Saved destinations',flush=True)
finally:
 sh('wm','size',s.group(1) if s else 'reset');sh('wm','density',d.group(1) if d else 'reset');launch()
