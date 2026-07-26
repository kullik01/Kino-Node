import urllib.request, urllib.parse, json

title = "Inception"
query = f"""
SELECT ?movie ?ratingVal ?critics
WHERE {{
  ?movie wdt:P31 wd:Q11424; rdfs:label "{title}"@en.
  OPTIONAL {{ ?movie p:P444 ?ratingStatement. 
             ?ratingStatement ps:P444 ?ratingVal. 
             OPTIONAL {{ ?ratingStatement pq:P447 ?critics. }} }}
}} LIMIT 50
"""
url = "https://query.wikidata.org/sparql?query=" + urllib.parse.quote(query)
req = urllib.request.Request(url, headers={'Accept': 'application/sparql-results+json', 'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        for row in data['results']['bindings']:
            if 'ratingVal' in row:
                print(f"RATING: {row['ratingVal']['value']}, CRITIC: {row.get('critics', {}).get('value')}")
except Exception as e:
    print(f"Error: {e}")
