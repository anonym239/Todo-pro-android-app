import urllib.request
import json

# Get latest run ID
url = "https://api.github.com/repos/anonym239/Todo-pro-android-app/actions/runs?per_page=1"
req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
data = json.loads(urllib.request.urlopen(req).read())
run_id = data["workflow_runs"][0]["id"]
print(f"Run ID: {run_id}")

# Get jobs
url2 = f"https://api.github.com/repos/anonym239/Todo-pro-android-app/actions/runs/{run_id}/jobs"
req2 = urllib.request.Request(url2, headers={"User-Agent": "Mozilla/5.0"})
jobs = json.loads(urllib.request.urlopen(req2).read())
job_id = jobs["jobs"][0]["id"]
print(f"Job ID: {job_id}")

# Get job logs
url3 = f"https://api.github.com/repos/anonym239/Todo-pro-android-app/actions/jobs/{job_id}/logs"
req3 = urllib.request.Request(url3, headers={"User-Agent": "Mozilla/5.0"})
try:
    resp = urllib.request.urlopen(req3)
    logs = resp.read().decode("utf-8", errors="replace")
    # Find error lines
    lines = logs.split("\n")
    for i, line in enumerate(lines):
        if any(x in line for x in ["FAILED", "error:", "Error:", "Exception", "What went wrong", "BUILD FAILED"]):
            start = max(0, i-2)
            end = min(len(lines), i+5)
            for l in lines[start:end]:
                print(l)
            print("---")
except Exception as e:
    print(f"Error getting logs: {e}")
    # Try redirect
    try:
        import urllib.request
        opener = urllib.request.build_opener(urllib.request.HTTPRedirectHandler())
        resp = opener.open(req3)
        logs = resp.read().decode("utf-8", errors="replace")
        lines = logs.split("\n")
        for line in lines[-100:]:
            print(line)
    except Exception as e2:
        print(f"Error2: {e2}")
