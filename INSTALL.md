```
#install sendmail, or "sudo apt-get install sendmail"
#"cp config.edn_template config.edn" and copy API into the config file
lein do clean, test, uberjar
CONFIG_FILE=config.edn java -Djava.security.manager -Djava.security.policy==java.policy -jar target/CLOVER.jar
CONFIG_FILE=config.edn java -Djava.security.manager -Djava.security.policy==java.policy -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar target/CLOVER.jar
```
