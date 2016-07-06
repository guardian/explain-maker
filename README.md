# Explainer Atom Editor
###Scala: 2.11.6, Scala.js: 0.6.5, Play: 2.4.0, Slick: 3.0.0


The sbt build file contains 3 modules
- `ExplainerServer` Play application (server side)
- `ExplainerClient` Scala.js application (client side)
- `ExplainerShared` Scala code that you want to share between the server and the client.

### Run the application
```
$ sbt
> run
$ open http://localhost:9000
```
