# URL Shortener Service:

Spring Boot service that creates short codes using a Redis atomic counter + Base62.
 ### steps to run :
1- run redis on docker
```bash
docker run --name urlshort-redis -p 6379:6379 -d redis:7-alpine
```
2- then run the spring app
```bash
mvn spring-boot:run

```

## 1- Requirements:

### Functional requirements:
- Users should be able to submit a long URL and receive a shortened version.
  - Optionally, users should be able to specify a custom alias for their shortened URL.
  - Optionally, users should be able to specify an expiration date for their shortened URL.
- Users should be able to access the original URL by using the shortened URL.
- Below the line (out of scope):
  - User authentication and account management.
  - Analytics on link clicks (e.g., click counts, geographic data).

### Non-Functional requirements:
- Ensure the uniqueness of the short code (no two long URLs can map to the same short URL)
- Low latency on redirects (~100ms)
- Scale to support 100M DAU and 1B URLS
- High availability 99.99%, eventioal consistency for URL shortening.
- Below the line (out of scope):
  - Data consistency in real-time analytics.
  - Advanced security features like spam detection and malicious URL filtering.

## NOTE: 
```text
 An important consideration in this system is the significant imbalance between read and write operations. The read-to-write ratio is heavily skewed towards reads, as users frequently access shortened URLs, while the creation of new short URLs is comparatively rare. 
 For instance, we might see 1000 clicks (reads) for every 1 new short URL created (write). 
 This asymmetry will significantly impact our system design, particularly in areas such as caching strategies, database choice, and overall architecture.
```
## 2- Entities & API:
### Entities:
- Original URL
- Short URL
- User
### API:
// shorten a url
``` 
POST  /urls -> shortUrl
{
“original_url” : “https://www.example.com/some/long/url”,
“alias” : “optional”,
“expiration_date” : “optional date”
}

->
{
“short_url” : “http://short.ly/abc12345”
}
```
// redirect to Original URL
```
GET  /{short_cide}
→ HTTP 302 Redirect to the original long URL.
```
## 3- High level design:



## 4- Deep Dives:

- Fast
- Unique
- Short (5-7)

Algorithm: there is three approaches here :
``` 
1- prefix of the long url.
2- random number generator 10^9 10 characters.
3- hash the long URL
- hash long url and then base62 [:6]
- md5(longUrl) -> hash -> base62(hash)[:6]
- Base 62 encoding , 0-9, A-Z, a-z
- 62^6 = 56B
- Birthday collisions — 880k collisions!
- We just need to check for collisions!
4- use Counter
```
I descided to use unique Counter with Base62 Encoding

### WHY THIS APPROCH!
``` 
Unique Counter with Base62 Encoding
To guarantee uniqueness and avoid collisions when generating short URLs, one effective approach is to maintain a monotonically increasing counter. For every new URL, the counter is incremented, and the resulting integer is then encoded using Base62. This ensures that the short code remains compact and human-friendly, while still mapping back to a unique numeric identifier.
Why Redis?
Redis is an ideal choice for managing this counter because of its single-threaded architecture and atomic operations. Its INCR command guarantees that each increment executes in isolation, eliminating race conditions and ensuring no duplicates or skipped values. This provides a simple yet robust mechanism for generating unique IDs at scale.
Benefits of this Approach
Collision-Free: Each counter value is unique, so no extra collision checks are required.


Efficient: Incrementing a counter and performing Base62 encoding is computationally lightweight, enabling high throughput.


Scalable: With careful counter management, the system can scale horizontally to handle massive volumes of URLs.


Reversible Mapping: Base62 codes can be decoded back to the original counter value, simplifying database lookups.


Scalability and Code Length
One natural consideration is the growth of the short code over time. However, Base62 provides excellent compression. For instance:
1 billion (10⁹) URLs in Base62 produce only a 6-character string (15ftgG).


Even at 62⁷ (~3.5 trillion URLs), the short code length increases only to 7 characters.


Thus, the system can accommodate billions of URLs while keeping short codes compact, efficient, and scalable.
Challenges
In a distributed deployment, maintaining a single global counter is more complex because all primary server instances must remain synchronized on counter values. This synchronization challenge will be addressed further when discussing system scaling strategies.
```
### Note that:
``` Pattern: Scaling Reads
URL shorteners showcase the extreme read-to-write ratio that makes scaling reads critical. With potentially millions of clicks per shortened URL, aggressive caching strategies become essential.

To solve this:
1- added index
2- implementing in-memory cache (eg Redis).
3- Leveraging Content Delivery Network(CDNs) and edge computing.
```
### Final design:

