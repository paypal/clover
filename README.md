# CLOVER

CLOVER is a smart Clojure Slack bot, currently:

1. dictionary and acronym expander
2. in Slack Clojure REPL

##

it scans all messages bot is joined to and DM explanations to subscribed users
it never repeats the explanation, unless user re-subsribes to bot

###directives:

? ! - for any channel user
will be silent if no matches or the syntax is not correct

- help -> link to GitHub:
```
!help
?
```

- explain:
```
!explain <term>
? <term>
```

- add definition:
```
!define <term> <separator> <description>
? <term> <separator> <description>
```

### examples

```
user   > ?ETA

clover > Estimated Time of Arrival

user   > ?ETA = Event tree analysis

clover > thx for defining term ETA
```

## REPL
```
'<clojure expression> ;; be careful, variables stay (but are not persistent), could be used for scripting :-)
```



## definitions (BNF+Clojure regex)

```
clover-sentence ::= eval | define | explain | help
help            ::= '!help'
define          ::= ('?' | '!define') break term break '=' break #'.+'
explain         ::= ('?' | '!explain') break term break
term            ::= word space term | word
space           ::= #'\\s+'
word            ::= #'[a-zA-Z0-9&-/]+'
eval            ::= #'[\\'\u2018]' catchall
break           ::= #'\\s*'
catchall        ::= #'.*'
```
