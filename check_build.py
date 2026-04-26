import urllib.request
import json

url = "https://api.github.com/repos/anonym239/Todo-pro-android-app/actions/runs?per_page=3"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
data = json.loads(urllib.request.urlopen(req).read())

for w in data["workflow_runs"]:
    print(f"Run #{w['run_number']}: {w['status']} - {w['conclusion']} - {w['head_commit']['message'][:50]}")
