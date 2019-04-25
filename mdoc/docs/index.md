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

##Compatibility matrix

| Http4s Version | Directives version | Status       |
|----------------|--------------------|--------------|
| 0.20.0         | 0.9.1              | Current      |
| 0.20.0-RC1     | 0.8.0              | EOL          |
| 0.20.0-M7      | 0.7.0              | EOL          |
| 0.20.0-M6      | 0.20.0-M6-2        | EOL          |
| 0.20.0-M5      | 0.20.0-M5-2        | EOL          |
| 0.18.19        | 0.5.1              | EOL          |


# Acknowledgements

- [Unfiltered directives](https://unfiltered.ws/07/00.html)
- [http4s](https://http4s.org)
