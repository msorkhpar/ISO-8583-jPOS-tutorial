The test lab has one proxy that every tester's client connects to. During the booked slots it
forwards to the acquirer's certification host. Outside them that link is down, and the testers
want the lab's own simulator to answer instead, so that they can keep working.

The first version did that "whenever the certification host had no answer". After the last slot,
the acquirer sent its log of the purchases it had received. For some of them the testers' logs
show an answer with `LAB001`, the simulator's approval code, instead of the acquirer's decision,
and no reversal from the client.

Finish `answerFor` in `src/main/java/com/example/practice/LabProxy.java`. It returns the answer
the tester's client gets, or `null` when the client must get none. `certHost` is the QMUX to the
certification host, and `simulator` gives the lab's answer to a request. Slots end on the minute, sometimes while a purchase is waiting, and some
clients send a purchase again while the first try still waits. The tests run a real
QMUX, with a test host on its queues that answers, declines, or says nothing. About 5 lines.
