"""Strict JSON Schema + semantic validation. Install pinned development dependency first."""
from pathlib import Path
import json
from jsonschema import Draft202012Validator
root=Path(__file__).resolve().parents[1]
schema=json.loads((root/'content/level.schema.json').read_text(encoding='utf-8'))
validator=Draft202012Validator(schema)
paths=sorted((root/'app/src/main/assets/levels').glob('level-*.json'))
assert len(paths)==50
knowledge={k['id'] for k in json.loads((root/'app/src/main/assets/knowledge.json').read_text(encoding='utf-8'))}
for i,path in enumerate(paths,1):
 data=json.loads(path.read_text(encoding='utf-8'))
 validator.validate(data)
 assert data['id']==i and path.name==f'level-{i:03}.json'
 assert data['chapterId']==(i-1)//5+1
 assert all(1<=p<i for p in data['prerequisites'])
 assert set(data['knowledgeIds'])<=knowledge
 assert sum(data['score'][k] for k in ['completion','resource','operations','knowledge','noHints'])==100
 assert not data['implemented'] or all(o['type']!='planned' for o in data['objectives'])
print('PASS: 50 level schemas, IDs, prerequisites, knowledge references and scoring weights')
