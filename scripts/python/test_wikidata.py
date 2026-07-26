import urllib.request, urllib.parse, json

title = "Inception"
query = f"""
SELECT ?movie ?movieLabel ?duration ?directorLabel ?writerLabel ?producerLabel ?cinematographerLabel ?composerLabel ?ratingVal ?criticsLabel ?genreLabel
WHERE {{
  ?movie wdt:P31 wd:Q11424; rdfs:label "{title}"@en.
  OPTIONAL {{ ?movie wdt:P2047 ?duration. }}
  OPTIONAL {{ ?movie wdt:P57 ?director. }}
  OPTIONAL {{ ?movie wdt:P58 ?writer. }}
  OPTIONAL {{ ?movie wdt:P162 ?producer. }}
  OPTIONAL {{ ?movie wdt:P344 ?cinematographer. }}
  OPTIONAL {{ ?movie wdt:P86 ?composer. }}
  OPTIONAL {{ ?movie p:P444 ?rating.
             ?rating ps:P444 ?ratingVal.
             ?rating pq:P447 ?critics. }}
  OPTIONAL {{ ?movie wdt:P136 ?genre. }}
  SERVICE wikibase:label {{ bd:serviceParam wikibase:language "en". }}
}} LIMIT 10
"""

url = "https://query.wikidata.org/sparql?query=" + urllib.parse.quote(query)
req = urllib.request.Request(url, headers={'Accept': 'application/sparql-results+json', 'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode('utf-8'))
        for row in data['results']['bindings']:
            if 'ratingVal' in row:
                print(f"RATING: {row['ratingVal']['value']} (Critic: {row.get('criticsLabel', {}).get('value', 'Unknown')})")
except Exception as e:
    print(f"Error: {e}")
