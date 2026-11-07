Your server translates PIN blocks through an HSM driver that is a QBean of its own: it registers
itself in the `NameRegistrar` under its name when it starts. `PinService`, in
`src/main/java/com/example/practice/PinService.java`, asks it for every translation. Its descriptor
is in the comment at the top of the file.

The service worked in testing, where it was copied into a server that was already running. Since
the last restart it has done nothing, and an operator can't tell that from JMX. The HSM team also
redeploys their driver now and then, while the server runs.

Finish `awaitHsm` and `hsm`, marked `TODO`. About six lines in total. When you're done, the
service works after a restart, an operator can see when it can't work and start it again once the
HSM is back, and it follows the HSM driver through a redeploy.

The tests deploy your service and a stand-in HSM driver with a real Q2 in the test's JVM: in the
same pass, in different passes, without the HSM, and with the HSM redeployed while the service
runs.
