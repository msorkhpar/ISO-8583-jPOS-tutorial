Operations want a command-line check for a new acquirer link: connect a channel, send one echo,
and print `UP`, `NO_ANSWER` or `LINK_DOWN`. The first version reported `UP` on a night when the
host was answering nothing, and on a busy afternoon it sat for twenty minutes before it printed
anything.

Write the probe in `src/main/java/com/example/practice/EchoProbe.java`: the two methods marked
`TODO`. `check` gets a connected channel, a STAN, and a time budget measured by the `clock` it was
given. It must report within the budget what became of that one echo:

- `UP`: the host answered this echo;
- `NO_ANSWER`: the host is connected but this echo got no answer;
- `LINK_DOWN`: the host closed the connection.

About 13 lines in total. The tests use a channel with no socket that plays a script: the host's
messages, silences and hang-ups, each at a set moment on the clock.
