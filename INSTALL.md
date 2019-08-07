
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

# other options

There is an unresolved bug in clover where it can freeze and is not able to reestablish websocket. The "nuke" API is a work around it and allows anybody authorized to restart the bot. It works together with a following cron/ "run" script combo:

```
SHELL=/bin/bash
#due to cron limitations, PATH needs to be taken from your shell as it is
PATH=....

*/15 * * * * (date && cd CLOVER/db && git add db logs && git commit -m updates && git push) &>> $HOME/clover-db.log
*/5 * * * * /usr/bin/flock -n /tmp/main.lockfile CLOVER/run &>> run.log
```

