This picks up where the previous practice ended: `LinkPolicy` already contains its solution, and
the sign-on manager now also reports every echo to it.

Two incidents from the same month:

- Every evening during the host's batch run, link A leaves the pool for a moment and signs on
  again, though nothing is wrong with the host. The manager takes a link out after a single
  unanswered echo.
- A colleague's first fix counted unanswered echoes and took the link out at the third. On
  Friday a link that had missed one echo on Monday and one on Wednesday was taken out over a single
  slow answer.

The network's operations team settled on `echoLimit`: after that many unanswered echoes, the manager
gives up on a link whose host has stopped answering. Finish `signedOn`, `echoAnswered` and `echoFailed`
in `src/main/java/com/example/practice/LinkPolicy.java`. About five lines in total.
