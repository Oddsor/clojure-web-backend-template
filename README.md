# A simple web server

A simple web server that serves as a launchpad for creating Clojure-based web-applications. Aims to include most of the features one might expect from a web server, such as authentication, content negotiation and validation, and so on.

## Development

While developing, use the `:dev`-alias.
`sync-deps` can be used to add new dependencies while system is running.

## Running tests

Run tests using kaocha with the :main alias:

```bash
clj -M:kaocha --watch
```

## Building

Build the application's uberjar using the `:build` alias:

```bash
clj -T:build uber
```