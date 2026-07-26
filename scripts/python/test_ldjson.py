import re
html = """
<script type="application/ld+json">
{
  "@context": "http://schema.org",
  "@type": "Movie",
  "name": "Inception",
  "aggregateRating": {
    "@type": "AggregateRating",
    "ratingCount": 2686866,
    "bestRating": "10",
    "worstRating": "1",
    "ratingValue": 8.8
  }
}
</script>
"""
ldJsonPattern = re.compile(r'<script type="application/ld\+json">(.*?)</script>', re.DOTALL)
m = ldJsonPattern.search(html)
if m:
    ldjson = m.group(1)
    ratingPattern = re.compile(r'"ratingValue"\s*:\s*"?([\d\.,]+)"?')
    rm = ratingPattern.search(ldjson)
    if rm:
        print(rm.group(1))
