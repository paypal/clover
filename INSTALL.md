```
lein uberjar
CONFIG_FILE=config.edn java -Djava.security.manager -Djava.security.policy==java.policy -jar target/CLOVER.jar
```
