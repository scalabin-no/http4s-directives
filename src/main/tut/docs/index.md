---
layout: docs
title: Introduction
position: 1
---

# Introduction

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

# Installation

To begin, add the following dependency to your SBT build file:

```scala
"no.scalabin.http4s" %% "http4s-directives" % http4sDirectivesVersion
```

You also need http4s as a dependency. See the table bellow to find a matching 
version of http4s.

| http4s-directives | http4s |
|---|--|
| 0.5.x | 0.18.x |
| 0.4.x | 0.18.x |
| 0.3.x | 0.18.x |
| 0.2.x | 0.18.x |
| 0.1.x | 0.18.x |



# Acknowledgements

- [Unfiltered directives](https://unfiltered.ws/07/00.html)
- [http4s](https://http4s.org)