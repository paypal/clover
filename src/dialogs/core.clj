(ns dialogs.core
  (:require
   dialogs.core-actions
   dialogs.core-messages))

(def definition [dialogs.core-actions/fsm-core dialogs.core-messages/slack-unique-id dialogs.core-messages/process-slack-message!])


