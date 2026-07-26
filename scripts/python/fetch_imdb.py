import urllib.request

req = urllib.request.Request(
    'https://www.imdb.com/title/tt0111161/',
    headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
    }
)
with urllib.request.urlopen(req) as response:
    html = response.read().decode('utf-8')
    import re
    m = re.search(r'"ratingValue"\s*:\s*([\d\.]+)', html)
    if m:
        print(f"FOUND ratingValue: {m.group(1)}")
    else:
        print("ratingValue NOT FOUND")
        
    m2 = re.search(r'aggregateRating', html)
    if m2:
        print("FOUND aggregateRating")
    else:
        print("aggregateRating NOT FOUND")
