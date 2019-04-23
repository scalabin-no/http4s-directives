---
layout: home
technologies:
 - first: ["http4s", "A minimal, idiomatic Scala interface for HTTP"]
 - second: ["Scala", "sbt-microsites plugin is completely written in Scala"]
---

# http4s-directives


Pattern matching is an easy and reliable way to handle requests that conform 
to expected criteria, but what about the requests that don’t? Typically, 
they fall through to a 404 handler. While this behavior is logical enough 
from the perspective of a service’s programming, clients of the service might 
reasonably assume that they’ve supplied the wrong path instead of, as in a 
previous example, an invalid parameter.

With enough fallback cases and nested match expressions, you could program 
whatever logic you want — including descriptive error handling. But that 
approach sacrifices the simplicity and readability that made pattern matching 
attractive in the first place.


**Fortunately, there is another way.** With directives you can express request 
criteria concisely, and scrupulously handle errors too.