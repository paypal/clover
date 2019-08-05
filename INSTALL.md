
# build and test 
```
CONFIG_FILE=config.edn_template lein do clean, test, uberjar
```

In case of using customized or enterprise version of `saml20-clj` library, it has to be build and installed locally first.

# manual test cases in [here](TESTING.md)


# configure

`cp config.edn_template config.edn` and fill in API keys and other entries.

Important: for enterprise integrations, make sure to check "Translate User IDs/Translate global enterprise IDs to local workspace IDs" option in the bot in a bot configuration screen.


# run locally
```
lein run
```

or overwrite configuration
```
CONFIG_FILE=custom_config.edn lein run
```

# run in production
```
java -Djava.security.manager -Djava.security.policy==java.policy -jar target/CLOVER.jar
```

or with diagnostic repl and custom config
```
CONFIG_FILE=custom_config.edn java -Djava.security.manager -Djava.security.policy==java.policy -Dclojure.server.repl="{:port 5555 :accept clojure.core.server/repl}" -jar target/CLOVER.jar
```
