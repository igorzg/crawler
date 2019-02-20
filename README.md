# Web scraping
[Before using this software read article: Is web scraping perfectly legal ?](https://benbernardblog.com/web-scraping-and-crawling-are-perfectly-legal-right)

# Requirements

1. [JDK gte 1.8](https://openjdk.java.net/install/)
2. [Sbt gte 1.x.x](https://www.scala-sbt.org/) 
3. [Docker](https://www.docker.com/products/overview)

## Running
* Debugging mode:
```
docker-compose up -d
sbt run -jvm-debug 5005 -J-Xmx4G
```
* Prod mode:
```
docker-compose up -d
sbt runProd -J-Xmx4G
```

* Docker:
```
docker-compose up -d
sbt docker:publishLocal
docker run -d -p 9000:9000 sphere-api-crawlers:1.0-SNAPSHOT
```

# API

### Schedule job
* HOST: http://localhost:9000
* METHOD: POST
* PATH: /crawler/v2

Multiple jobs:
```json
[
  {
    "url": "https://typeix.github.io",
    "config": {
      "concurrency": 1,
      "throttle": 1000
    }
  },
  {
    "url": "https://en.wikipedia.org",
    "include": [
      "/wiki"
    ],
    "config": {
      "concurrency": 1,
      "throttle": 1000
    }
  },
  {
    "url": "https://en.wikipedia.org",
    "exclude": [
      "/wiki" 
    ],
    "config": {
      "concurrency": 1,
      "throttle": 1000
    }
  }
]
```


### Kill scheduled job
* HOST: http://localhost:9000
* METHOD: DELETE
* PATH: /crawler/v2

Multiple jobs:
```json
[
  {
    "url": "https://typeix.github.io"
  }
]
```

## CONFIG OPTIONS
| Task                  | Type         | Description  |
| --------------------- |:------------:| ------------|
| url                   | String       | link to crawl |
| include               | List[String] | crawl only paths which are in include list |
| exclude               | List[String] | crawl everything except paths in exclude list |

| Config - Key      | Type    | Description  |
| --------------------- |:-------:| ------------|
| throttle              | Integer | crawling delay in ms |
| concurrency           | Integer | number of concurrent ops |
| withIndexThrottle     | Boolean | see index throttle formula |
| withStripOtherQueries | Boolean | in combination with include |


