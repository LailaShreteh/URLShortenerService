# URL Shortener Service:

Random (SecureRandom) Base62 + Postgres + Redis design

### steps to run :
1- run docker from the project root 
```bash
docker compose up --build
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

![Screenshot 2025-09-18 202200.png](Screenshot%202025-09-18%20202200.png)

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
and

Let’s keep Users in Postgres/MySQL (durable, constraints, backups) and use Redis for what it does best: counter + hot cache + rate limits + alias reservations. This preserves sub-100ms redirects while keeping user data safe, queryable, and compliant.

### Why

  - **Durability & constraints:** Unique email/username, foreign keys, migrations, PITR → native in Postgres/MySQL.
  - **Querying:** Ad-hoc search, pagination, auditing without rolling our own indexes.
  - **Cost & ops:** Users on disk is cheaper and simpler to back up/restore than in-RAM storage.
  - **Performance:** Redirect path remains Redis-backed (code→url) + CDN; writes stay simple via Redis INCR.

What Redis is for in our design

- Atomic ID counter (INCR) for short codes
- Hot cache code → url with TTL
- Rate limiting (per IP/user)
- Alias reservation (SETNX reserved:{alias} with TTL)
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
### Schema
```sql
(Postgres/MySQL)
-- Users = system of record
CREATE TABLE users (
id            BIGSERIAL PRIMARY KEY,     -- or BIGINT AUTO_INCREMENT
email         CITEXT UNIQUE NOT NULL,    -- use VARCHAR UNIQUE if MySQL
password_hash TEXT NOT NULL,
created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
status        SMALLINT NOT NULL DEFAULT 1 -- 1=active, 2=disabled
);

-- Short URLs
CREATE TABLE short_urls (
code         VARCHAR(10) PRIMARY KEY,
long_url     TEXT NOT NULL,
user_id      BIGINT REFERENCES users(id),
created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
expires_at   TIMESTAMPTZ,
is_custom    BOOLEAN NOT NULL DEFAULT FALSE,
status       SMALLINT NOT NULL DEFAULT 1  -- 1=active, 2=expired, 3=deleted
);

CREATE INDEX idx_short_urls_user    ON short_urls(user_id);
CREATE INDEX idx_short_urls_expires ON short_urls(expires_at);
-- idempotency:
CREATE UNIQUE INDEX IF NOT EXISTS uq_short_urls_url_hash ON short_urls(url_hash);
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

![Screenshot 2025-09-18 202118.png](Screenshot%202025-09-18%20202118.png)

### Alternative designs:
I considered two approaches to generate unique short codes:
* Counter-less (chosen): Random Base62 codes (length 7–8) + DB UNIQUE/PK + retry on duplicate.
* Central Counter (alternative): Redis INCRBY with counter batching (writers lease ranges).

I chose (1) for simplicity, reliability, and easy horizontal/multi-region scaling.


### What it does
 * Create (POST /create-short, JSON body UrlDto):
   - Validates and normalizes the input URL (adds http:// if missing).
   - If alias is provided → uses it as the code (DB enforces uniqueness).
   - Otherwise generates an 8-char Base62 code with SecureRandom.
   - Persists {code, long_url, created_at, expires_at, is_custom} in Postgres.
   - Warms Redis cache: code:{code} → long_url with TTL clamped to expires_at.
   - Returns the code as text/plain.
* Redirect (GET /{code}):
  - Reads Redis first; on hit → 302 Location to the original URL.
  - On miss → loads from Postgres, checks not expired, warms cache, returns 302.
  - 404 if not found, 410 if expired.
* Why this design
  - No global counter / no hot key → easy horizontal & multi-region scaling.
  - Fast & simple: O(1) generation w.r.t. URL length; DB UNIQUE on code guarantees no duplicates (retry on the rare collision).
  - Great cache fit: read-heavy traffic is served from Redis and CDN.


### 1- Counter-less Random Base62
 How it works
```bash
On create, generate a random Base62 code (length L=7 or 8) 
using a CSPRNG (SecureRandom).
Insert into DB where code is PRIMARY KEY/UNIQUE.
If insert fails with duplicate key, 
generate a new code and retry (rare).
 ```
  Why this is preferred
- No global bottleneck (no shared counter).
- Scales across regions with zero coordination.
- Retries are negligible when L ≥ 7 (use 8 for near-zero collision odds).
- Uniform key distribution (nice for sharding/partitions).

### 2- Central Counter with Batching (Redis)
  What it is
```bash 
- A single Redis key holds the high-water mark.
- Writers lease blocks via INCRBY counter, blockSize (e.g., 1000).
- They allocate from their local block without more Redis calls until exhausted.
```
 Strengths
- Simple logic; ordered numeric IDs.
- Minimal per-write overhead (one network call per block).

 Risks / Complexity
  - Single logical source of truth (hot key/blast radius).
 - Failover consistency (must ensure the counter is not rolled back on promotion).
 - Multi-region is tricky without prefixes or strong consistency.
 
Mitigations
- Enable AOF, set min-replicas-to-write, use WAIT before returning batches.
 - Or use a strongly consistent Redis deployment.
- For multi-region, add a short region/shard prefix to codes to avoid overlap.


### When to Choose Which

| Scenario | Counter-less (random) | Counter + batching |
| --- | --- | --- |
| Horizontal / multi-region scaling | **Excellent** (no coordination) | Needs careful design (prefixes/consistency) |
| Simplicity & ops overhead | **Simple** | More moving parts (AOF/replication/WAIT) |
| Ordered identifiers needed | Not ordered (use `created_at`/ULID) | **Yes** (monotonic) |
| Throughput / latency | **Great** (single DB insert; rare retry) | Great (amortized Redis calls) |

So Finally : Counter-less random Base62 (L=8) + DB UNIQUE + retry.
Keep Redis for cache, rate-limits, and alias reservations—not for a global counter.