WIP

functional:


!stop clover
+1 !help
!wait 3 minutes
!start clover
!no crash
-1

!check dialogs


+1 !abc
+2 !help
-1
-2

+1 !abc
+2 !help
-2
-1

+1 !help
*1 ?lol
*1 ?clover
-1


+1 !help
*1 ?lol
*1 whatever
*1 ?clover
-1

+1 !abc
*1 !help
-1

+1 !help
+2 ?lol
+3 !abc
*3 !help
+4 !abc
-1
-2
-3
-4

+1 ?lol
*1 ?lol2
*1 ?lol
*1 abc
-1


+1 lol

+1 lol
*1 ?lol
*1 lol

+1 ?lol
*1 lol
*1 ?lol

+1 ?lol1
*1 ?lol2

+1 ?lol=one more time

+1 lol
*1 lol2


!shut down bot
+1 help
!wait
!start bot
-1

# test new/change and mod messaging
+1 ?abc1
*1 ?abc2
*1 ?abc2=abc2


variations:
 combine above with restarting clover
 reply

reliability:


performance:


logging:
