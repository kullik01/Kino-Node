import urllib.request

req = urllib.request.Request(
    'https://www.imdb.com/title/tt0111161/', 
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    }
)

try:
    with urllib.request.urlopen(req) as response:
        html = response.read().decode('utf-8')
        print(f"Length: {len(html)}")
        if 'aggregateRating' in html:
            print("FOUND aggregateRating")
            idx = html.find('aggregateRating')
            print(html[idx:idx+200])
        else:
            print("NOT FOUND")
except Exception as e:
    print(f"Error: {e}")
