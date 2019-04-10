(ns dialogs.research
  (:require
   dialogs.research-actions
   dialogs.research-messages))

(def definition [dialogs.research-actions/fsm-research dialogs.research-messages/slack-unique-id dialogs.research-messages/process-slack-message!])

(def definition2 [dialogs.research-actions/fsm-research dialogs.research-messages/slack-unique-id2 dialogs.research-messages/process-slack-message!])


