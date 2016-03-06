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
?+ <term> <separator> <description>
```

- drop definition:
```
!drop <term> <separator> <description>
?- <term> <separator> <description>
```

- create alias:
```
!alias <term> <separator> <term>
```

- validate, vote up/down:
```
!+1 <term> [#]
!-1 <term> [#]
```
- subscription
```
!subscribe

!unsubscribe

!seen <term>
!forget <term>
```
### examples

## REPL
```
'<clojure expression> ;; be careful, variables stay (but are not persistent), could be used for scripting :-)
```



## definitions (BNF+Clojure regex)

```
<word> ::= #"[a-zA-Z0-9]+"
<white space> ::= #"\s+"
<description> ::= #".+$" ;; any ASCII till end of the string
<term> ::=   <word> { <white space> <word> }
<separator> ::= "."
```
