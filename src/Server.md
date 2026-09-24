# jPOS Server: Basic Setup

The basic setup for a jPOS server involves configuring Spring and Q2, which is jPOS's runtime environment. This setup provides a foundation for building a robust ISO-8583 message processing server.

## 1. Spring Configuration

Spring is used to manage dependencies and configure the application context. We'll use the `SpringContainer` provided by jPOS to integrate Spring with Q2.

First, let's create a Spring configuration class:

```java
package com.example.jposserver.config;

import org.jpos.q2.iso.QMUX;
import org.jpos.transaction.TransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JposConfig {

    @Bean
    public TransactionManager transactionManager() {
        TransactionManager tm = new TransactionManager();
        tm.setName("txnmgr");
        tm.setQueue("txnmgr");
        return tm;
    }

    @Bean
    public QMUX qmux() {
        QMUX qmux = new QMUX();
        qmux.setName("mux");
        qmux.setLogger("Q2");
        qmux.setRealm("mux");
        return qmux;
    }
}
```

This configuration class sets up two important beans:
1. `TransactionManager`: Manages the transaction processing flow.
2. `QMUX`: Multiplexer for ISO message routing.

## 2. Q2 Configuration

Next, we need to configure Q2 to use our Spring context. Create a file named `05_spring_context.xml` in the `deploy` directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<spring-context class="org.jpos.q2.spring.SpringContainer">
    <property name="config" value="classpath:applicationContext.xml" />
</spring-context>
```

This XML file tells Q2 to use the `SpringContainer` and load the Spring application context from `applicationContext.xml`.

Now, create the `applicationContext.xml` file in the `src/main/resources` directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd">

    <context:component-scan base-package="com.example.jposserver"/>
    <context:annotation-config/>

</beans>
```

This XML file enables component scanning and annotation-based configuration for our application.

## 3. Main Application Class

Create a main application class to start the Q2 server:

```java
package com.example.jposserver;

import org.jpos.q2.Q2;

public class JposServerApplication {
    public static void main(String[] args) {
        Q2 q2 = new Q2();
        q2.start();
    }
}
```

This class initializes and starts the Q2 server.

## 4. Project Structure

Ensure your project structure looks like this:

```
src
├── main
│   ├── java
│   │   └── com
│   │       └── example
│   │           └── jposserver
│   │               ├── config
│   │               │   └── JposConfig.java
│   │               └── JposServerApplication.java
│   └── resources
│       └── applicationContext.xml
└── test
    └── java
deploy
└── 05_spring_context.xml
```

## 5. Dependencies

Make sure you have the following dependencies in your `pom.xml` or `build.gradle` file:

```xml
<dependencies>
    <dependency>
        <groupId>org.jpos</groupId>
        <artifactId>jpos</artifactId>
        <version>2.1.7</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>5.3.20</version>
    </dependency>
</dependencies>
```

## 6. Testing the Setup

To test this basic setup, you can create a simple JUnit test:

```java
package com.example.jposserver;

import org.jpos.q2.Q2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JposServerApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
        assertThat(applicationContext.getBean(Q2.class)).isNotNull();
    }
}
```

This test ensures that the Spring context is loaded correctly and that the Q2 instance is available in the application context.

With this basic setup, you have a foundation for building a jPOS server integrated with Spring. The next steps would involve adding ISO-8583 message handling, channel management, and transaction processing logic.


# 2. ISO-8583 Message Handling

ISO-8583 message handling is at the core of jPOS functionality. It involves creating, parsing, and manipulating ISO-8583 messages. The key components in this process are the ISO components themselves (channels, packagers, fields), message templates built from `ISOMsg`, and the Generic Packager.

## 2.1 ISO Factory

jPOS 2.1.7 has no `ISOFactory` class. ISO components such as channels, packagers, and fields are created directly with their constructors (or declared in Q2 XML configuration).

Example usage:

```java
import org.jpos.iso.ISOField;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;

public class ISOFactoryExample {
    public void demonstrateISOFactory() throws Exception {
        // Create a packager
        ISO87APackager packager = new ISO87APackager();

        // Create a channel (host, port, packager, TPDU)
        NACChannel channel = new NACChannel("localhost", 8000, packager, null);

        // Create a field
        ISOField field = new ISOField(2, "4111111111111111");
    }
}
```

## 2.2 Message Factory

jPOS 2.1.7 has no `MsgFactory` class. To create ISO messages based on predefined templates, build a template `ISOMsg` once and `clone()` it for each new message. This is particularly useful when you need to create similar messages repeatedly with slight variations.

Here's an example of a template-based message factory:

```java
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;

import java.util.Date;

public class MessageFactoryExample {
    private final ISOMsg purchaseTemplate;

    public MessageFactoryExample() throws ISOException {
        // Define a template for a purchase transaction
        purchaseTemplate = new ISOMsg();
        purchaseTemplate.setPackager(new ISO87APackager()); // so msg.pack() works on every copy
        purchaseTemplate.setMTI("0200");
        purchaseTemplate.set(3, "000000"); // Processing code for purchase
        purchaseTemplate.set(25, "00"); // POS Condition Code
        purchaseTemplate.set(41, "12345678"); // Terminal ID
    }

    public ISOMsg createPurchaseMessage(String pan, String amount) throws ISOException {
        ISOMsg msg = (ISOMsg) purchaseTemplate.clone();
        msg.set(2, pan); // Primary Account Number
        msg.set(4, amount); // Transaction Amount
        msg.set(7, ISODate.getDateTime(new Date())); // Transmission Date & Time
        msg.set(11, String.format("%06d", (int) (Math.random() * 1000000))); // Systems Trace Audit Number
        return msg;
    }
}
```

## 2.3 Generic Packager

The `GenericPackager` is a flexible packager that can be configured using an XML file. This allows you to define custom message formats without writing Java code.

Here's an example of how to use `GenericPackager`:

```java
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;

public class GenericPackagerExample {
    private GenericPackager packager;

    public GenericPackagerExample() throws ISOException {
        packager = new GenericPackager("config/iso87ascii.xml");
    }

    public byte[] packageMessage(ISOMsg msg) throws ISOException {
        return packager.pack(msg);
    }

    public ISOMsg unpackMessage(byte[] msgData) throws ISOException {
        ISOMsg msg = new ISOMsg();
        packager.unpack(msg, msgData);
        return msg;
    }
}
```

The XML configuration file (`iso87ascii.xml`) might look something like this:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE isopackager SYSTEM "genericpackager.dtd">

<isopackager>
  <isofield
      id="0"
      length="4"
      name="Message Type Indicator"
      class="org.jpos.iso.IFA_NUMERIC"/>
  <isofield
      id="1"
      length="16"
      name="Bitmap"
      class="org.jpos.iso.IFB_BITMAP"/>
  <isofield
      id="2"
      length="19"
      name="Primary Account Number"
      class="org.jpos.iso.IFA_LLNUM"/>
  <!-- More field definitions... -->
</isopackager>
```

## 2.4 Putting It All Together

Now, let's create a comprehensive example that uses all these components together:

```java
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ISO8583MessageHandler {
    private final Map<String, ISOMsg> templates = new HashMap<>();
    private final GenericPackager packager;
    private final Logger logger;

    public ISO8583MessageHandler() throws ISOException, IOException {
        // Initialize packager
        packager = new GenericPackager("config/iso87ascii.xml");

        // Define templates
        defineMessageTemplates();

        // Initialize logger
        logger = new Logger();
        logger.addListener(new SimpleLogListener(System.out));
    }

    private void defineMessageTemplates() throws ISOException {
        // Purchase request template
        ISOMsg purchaseTemplate = new ISOMsg();
        purchaseTemplate.setPackager(packager);
        purchaseTemplate.setMTI("0200");
        purchaseTemplate.set(3, "000000");
        purchaseTemplate.set(25, "00");
        purchaseTemplate.set(41, "12345678");
        templates.put("purchase", purchaseTemplate);

        // Balance inquiry template
        ISOMsg balanceTemplate = new ISOMsg();
        balanceTemplate.setPackager(packager);
        balanceTemplate.setMTI("0100");
        balanceTemplate.set(3, "310000");
        balanceTemplate.set(25, "00");
        balanceTemplate.set(41, "12345678");
        templates.put("balance", balanceTemplate);
    }

    public ISOMsg createPurchaseMessage(String pan, String amount) throws ISOException {
        ISOMsg msg = (ISOMsg) templates.get("purchase").clone();
        msg.set(2, pan);
        msg.set(4, amount);
        msg.set(7, ISODate.getDateTime(new Date()));
        msg.set(11, String.format("%06d", (int) (Math.random() * 1000000)));
        return msg;
    }

    public ISOMsg createBalanceInquiryMessage(String pan) throws ISOException {
        ISOMsg msg = (ISOMsg) templates.get("balance").clone();
        msg.set(2, pan);
        msg.set(7, ISODate.getDateTime(new Date()));
        msg.set(11, String.format("%06d", (int) (Math.random() * 1000000)));
        return msg;
    }

    public byte[] packageMessage(ISOMsg msg) throws ISOException {
        return packager.pack(msg);
    }

    public ISOMsg unpackMessage(byte[] msgData) throws ISOException {
        ISOMsg msg = new ISOMsg();
        packager.unpack(msg, msgData);
        return msg;
    }

    public void logMessage(ISOMsg msg, String direction) {
        LogEvent ev = new LogEvent("iso-message");
        ev.addMessage(direction);
        ev.addMessage(msg);
        logger.log(ev);
    }

    public static void main(String[] args) {
        try {
            ISO8583MessageHandler handler = new ISO8583MessageHandler();

            // Create and log a purchase message
            ISOMsg purchaseMsg = handler.createPurchaseMessage("4111111111111111", "000000010000");
            handler.logMessage(purchaseMsg, "OUTGOING");

            // Package the message
            byte[] packedMsg = handler.packageMessage(purchaseMsg);
            System.out.println("Packed message: " + ISOUtil.hexdump(packedMsg));

            // Unpack the message
            ISOMsg unpackedMsg = handler.unpackMessage(packedMsg);
            handler.logMessage(unpackedMsg, "INCOMING (unpacked)");

            // Create and log a balance inquiry message
            ISOMsg balanceMsg = handler.createBalanceInquiryMessage("4111111111111111");
            handler.logMessage(balanceMsg, "OUTGOING");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

This comprehensive example demonstrates:

1. Initializing the `GenericPackager` with an XML configuration file.
2. Keeping predefined `ISOMsg` templates for different transaction types, cloned for each new message.
3. Creating purchase and balance inquiry messages using the templates.
4. Packaging and unpackaging messages.
5. Logging messages for debugging purposes.

This implementation provides a solid foundation for handling ISO-8583 messages in a jPOS-based application. It can be easily extended to handle more message types and integrated into a larger transaction processing system.



# 3. Channel Management

Channel management in jPOS is crucial for handling ISO-8583 message communication between different systems. The two main components we'll focus on are:

1. ISO Server (`org.jpos.iso.ISOServer`)
2. NAC Channel (`org.jpos.iso.channel.NACChannel`)

## 3.1 ISO Server

The `ISOServer` is responsible for listening to incoming connections and handling ISO-8583 messages. It can be configured to use different types of channels and can handle multiple connections simultaneously.

## 3.2 NAC Channel

The `NACChannel` is a plain TCP channel in jPOS: it frames each ISO-8583 message with a 2-byte binary length header and a TPDU. It does not encrypt anything by itself; SSL/TLS is added separately by giving the channel (or the server) an SSL socket factory such as `org.jpos.iso.SunJSSESocketFactory`.

Now, let's implement a jPOS server with channel management using Spring and Java 21.

```java
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOServer;
import org.jpos.iso.channel.NACChannel;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;
import org.jpos.util.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ISOServerConfig extends QBeanSupport {

    @Autowired
    private ServerConfiguration serverConfig;

    @Bean
    public ISOServer isoServer() throws ConfigurationException, ISOException {
        org.jpos.core.Configuration cfg = createConfiguration();
        NACChannel channel = new NACChannel(
            serverConfig.getHost(),
            serverConfig.getPort(),
            serverConfig.getPackager(),
            null // no TPDU header
        );
        channel.setConfiguration(cfg);

        ISOServer server = new ISOServer(
            serverConfig.getPort(),
            channel,
            new ThreadPool(1, 100)
        );

        server.setConfiguration(cfg);
        server.addISORequestListener(new TransactionRequestListener());

        NameRegistrar.register("iso-server", server);

        return server;
    }

    private org.jpos.core.Configuration createConfiguration() throws ISOException {
        org.jpos.core.Configuration cfg = new org.jpos.core.SimpleConfiguration();
        cfg.put("timeout", "300000");
        return cfg;
    }

    @Override
    protected void startService() {
        try {
            ISOServer server = isoServer();
            new Thread(server).start();
        } catch (Exception e) {
            getLog().error("Error starting ISO server", e);
        }
    }

    @Override
    protected void stopService() {
        try {
            ISOServer server = NameRegistrar.get("iso-server");
            server.shutdown();
        } catch (NameRegistrar.NotFoundException e) {
            getLog().error("ISO server not found for shutdown", e);
        }
    }
}
```

This configuration class sets up an `ISOServer` using Spring's `@Configuration` and `@Bean` annotations. Let's break down the important parts:

1. We create a `NACChannel` with the specified host, port, and packager.
2. We instantiate an `ISOServer` with the port, channel, and a `ThreadPool` whose threads serve the connections.
3. We give the channel a read timeout and pass the same configuration to the server; the `ThreadPool`'s maximum of 100 threads is what caps concurrent connections.
4. We add a `TransactionRequestListener` to handle incoming requests.
5. We register the server with `NameRegistrar` for easy access in other parts of the application.
6. The `startService` and `stopService` methods handle the lifecycle of the server.

Now, let's create the `ServerConfiguration` class to hold our server settings:

```java
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {

    @Value("${iso.server.host}")
    private String host;

    @Value("${iso.server.port}")
    private int port;

    @Value("${iso.server.packager}")
    private String packagerPath;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Bean
    public ISOPackager getPackager() throws ISOException {
        return new GenericPackager(packagerPath);
    }
}
```

This class uses Spring's `@Value` annotation to inject configuration properties and creates a bean for the `ISOPackager`.

Lastly, let's implement a simple `TransactionRequestListener`:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

public class TransactionRequestListener implements ISORequestListener {

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            // Log the incoming message
            System.out.println("Received message: " + m.toString());

            // Create a response message
            ISOMsg response = (ISOMsg) m.clone();
            response.setResponseMTI();
            response.set(39, "00"); // Approval code

            // Send the response
            source.send(response);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
```

This listener simply logs the incoming message, creates a basic response, and sends it back.

To use this setup, you would need to add the following properties to your `application.properties` file:

```
iso.server.host=localhost
iso.server.port=8000
iso.server.packager=path/to/your/packager.xml
```

This implementation provides a solid foundation for a jPOS server with channel management. It handles multiple connections, and can be easily extended to include more complex business logic in the `TransactionRequestListener`.


# 4. Transaction Processing

Transaction processing is a crucial part of any financial system, especially when dealing with ISO-8583 messages. In jPOS, transaction processing is handled by the TransactionManager and implemented through TransactionParticipants. This approach allows for a modular and flexible way to process transactions.

## 4.1 Transaction Manager

The TransactionManager is responsible for coordinating the execution of a series of participants for each transaction. It manages the transaction lifecycle, including commit and rollback operations.

### Implementation

Let's implement a basic TransactionManager configuration:

```java
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.q2.QBeanSupport;
import org.jpos.transaction.TransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionManagerConfig extends QBeanSupport {

    @Bean
    public TransactionManager transactionManager() throws ConfigurationException {
        TransactionManager txnManager = new TransactionManager();
        txnManager.setName("txnManager");
        // A TransactionManager reads its settings from a jPOS Configuration
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("queue", "txnQueue");
        cfg.put("max-sessions", "5");
        cfg.put("max-active-sessions", "10");
        cfg.put("debug", "true");
        txnManager.setConfiguration(cfg);
        return txnManager;
    }
}
```

In this configuration, we're setting up a TransactionManager bean with the following properties:
- Name: "txnManager"
- Queue: "txnQueue" (where transactions will be queued)
- MaxSessions: 5 (maximum number of concurrent sessions)
- MaxActiveSessions: 10 (maximum number of active sessions; jPOS requires it to be at least MaxSessions)
- Debug: true (for logging purposes)

## 4.2 Transaction Participant

TransactionParticipants are the building blocks of transaction processing in jPOS. Each participant performs a specific task in the transaction flow.

Let's create a simple TransactionParticipant that validates a transaction:

```java
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;

public class TransactionValidator implements TransactionParticipant, Configurable {

    private Configuration cfg;

    @Override
    public int prepare(long id, Serializable context) {
        // Validate the transaction
        if (isValidTransaction((Context) context)) {
            return PREPARED | READONLY;
        }
        return ABORTED;
    }

    @Override
    public void commit(long id, Serializable context) {
        // Nothing to commit for this participant
    }

    @Override
    public void abort(long id, Serializable context) {
        // Handle abort scenario
        ((Context) context).put("error", "Transaction validation failed");
    }

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
    }

    private boolean isValidTransaction(Context context) {
        // Implement your validation logic here
        // For example, check if required fields are present
        return context.get("amount") != null && context.get("cardNumber") != null;
    }
}
```

This TransactionValidator checks if the transaction has the required fields (amount and cardNumber in this example).

## 4.3 Custom Participants for Visa/Mastercard Logic

Now, let's create specific participants for Visa and Mastercard transactions:

```java
public class VisaTransactionParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable serializable) {
        Context context = (Context) serializable;
        // Visa-specific validation and processing
        if (isValidVisaTransaction(context)) {
            context.put("network", "VISA");
            return PREPARED;
        }
        return PREPARED | NO_JOIN | READONLY; // not a Visa card: pass it through, never abort it
    }

    @Override
    public void commit(long id, Serializable context) {
        // Commit Visa transaction
        // For example, send to Visa network
    }

    @Override
    public void abort(long id, Serializable context) {
        // Handle Visa transaction abort
    }

    private boolean isValidVisaTransaction(Context context) {
        // Implement Visa-specific validation
        String pan = (String) context.get("pan");
        return pan != null && pan.startsWith("4");
    }
}

public class MastercardTransactionParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable serializable) {
        Context context = (Context) serializable;
        // Mastercard-specific validation and processing
        if (isValidMastercardTransaction(context)) {
            context.put("network", "MASTERCARD");
            return PREPARED;
        }
        return PREPARED | NO_JOIN | READONLY; // not a Mastercard card: pass it through, never abort it
    }

    @Override
    public void commit(long id, Serializable context) {
        // Commit Mastercard transaction
        // For example, send to Mastercard network
    }

    @Override
    public void abort(long id, Serializable context) {
        // Handle Mastercard transaction abort
    }

    private boolean isValidMastercardTransaction(Context context) {
        // Implement Mastercard-specific validation
        String pan = (String) context.get("pan");
        return pan != null && pan.startsWith("5");
    }
}
```

These participants handle Visa and Mastercard specific logic, such as identifying the card network based on the PAN (Primary Account Number) prefix.

## 4.4 Configuring the Transaction Flow

To tie everything together, we need to configure the transaction flow. This is typically done in an XML configuration file for jPOS:

```xml
<txnmgr class="org.jpos.transaction.TransactionManager" logger="Q2">
  <property name="queue" value="txnQueue"/>
  <property name="sessions" value="2"/>
  <participant class="TransactionValidator" logger="Q2"/>
  <participant class="VisaTransactionParticipant" logger="Q2"/>
  <participant class="MastercardTransactionParticipant" logger="Q2"/>
  <!-- Add more participants as needed -->
</txnmgr>
```

This configuration sets up the TransactionManager and defines the order of participants in the transaction flow.

## 4.5 Testing the Transaction Flow

Let's create a test to verify our transaction flow:

```java
import org.jpos.core.SimpleConfiguration;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.jpos.transaction.TransactionParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class TransactionFlowTest implements TransactionConstants {

    private VisaTransactionParticipant visa;
    private MastercardTransactionParticipant mastercard;
    private List<TransactionParticipant> participants;

    @BeforeEach
    public void setup() throws Exception {
        // Configure participants, in the order the txnmgr descriptor lists them
        TransactionValidator validator = new TransactionValidator();
        validator.setConfiguration(new SimpleConfiguration());
        visa = spy(new VisaTransactionParticipant());
        mastercard = spy(new MastercardTransactionParticipant());
        participants = List.of(validator, visa, mastercard);
    }

    // Runs one transaction the way the TransactionManager does: prepare each participant
    // in order, then commit the ones that joined, or abort them if one aborted
    private int process(long id, Context context) {
        List<TransactionParticipant> joined = new ArrayList<>();
        for (TransactionParticipant p : participants) {
            int action = p.prepare(id, context);
            if ((action & NO_JOIN) == 0) {
                joined.add(p);
            }
            if ((action & PREPARED) == 0) {
                joined.forEach(j -> j.abort(id, context));
                return ABORTED;
            }
        }
        joined.forEach(j -> j.commit(id, context));
        return PREPARED;
    }

    @Test
    public void testVisaTransaction() {
        Context context = new Context();
        context.put("amount", "100.00");
        context.put("cardNumber", "4111111111111111");
        context.put("pan", "4111111111111111");

        assertEquals(PREPARED, process(1L, context));

        assertEquals("VISA", context.get("network"));
        verify(visa).commit(eq(1L), any());
        verify(mastercard, never()).commit(anyLong(), any());
    }

    @Test
    public void testMastercardTransaction() {
        Context context = new Context();
        context.put("amount", "200.00");
        context.put("cardNumber", "5555555555554444");
        context.put("pan", "5555555555554444");

        assertEquals(PREPARED, process(2L, context));

        assertEquals("MASTERCARD", context.get("network"));
        verify(mastercard).commit(eq(2L), any());
        verify(visa, never()).commit(anyLong(), any());
    }
}
```

This test class verifies that our transaction flow correctly identifies Visa and Mastercard transactions based on the PAN.

In conclusion, this implementation demonstrates how to set up transaction processing in jPOS, including the TransactionManager, custom TransactionParticipants for general validation and card network-specific processing, and how to test the transaction flow. This modular approach allows for easy extension and modification of the transaction processing logic as needed.


# Visa and Mastercard Specific Processing in jPOS

## Overview

When implementing a payment processing system using jPOS, it's essential to handle Visa and Mastercard transactions differently due to their specific requirements and data elements. We'll create custom participants for both Visa and Mastercard logic, which will be part of the transaction processing flow.

## Implementation

### 1. Create Custom Participants

First, let's create abstract base classes for Visa and Mastercard participants:

```java
import org.jpos.transaction.TransactionParticipant;
import org.jpos.transaction.Context;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;

import java.io.Serializable;

public abstract class AbstractCardParticipant implements TransactionParticipant, Configurable {
    protected Configuration cfg;

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
    }

    @Override
    public int prepare(long id, Serializable context) {
        // Common preparation logic
        return PREPARED | NO_JOIN | READONLY;
    }

    @Override
    public void commit(long id, Serializable context) {
        // Common commit logic
    }

    @Override
    public void abort(long id, Serializable context) {
        // Common abort logic
    }

    protected abstract void processSpecificFields(Context context);
}

public abstract class VisaParticipant extends AbstractCardParticipant {
    // Visa-specific methods and fields
}

public abstract class MastercardParticipant extends AbstractCardParticipant {
    // Mastercard-specific methods and fields
}
```

### 2. Implement Visa-specific Participant

Now, let's create a concrete Visa participant:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.transaction.Context;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class VisaAuthorizationParticipant extends VisaParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        int result = super.prepare(id, context);
        if ("VISA".equals(((Context) context).get("CARD_TYPE"))) { // only Visa cards; any other card passes through untouched
            processSpecificFields((Context) context);
        }
        return result;
    }

    @Override
    protected void processSpecificFields(Context context) {
        ISOMsg msg = (ISOMsg) context.get("REQUEST");
        try {
            // Process Visa-specific fields
            String cardAcceptorId = msg.getString(42);
            String cardAcceptorNameLocation = msg.getString(43);
            String additionalData = msg.getString(48);

            // Validate and process Visa-specific data
            if (isValidVisaTransaction(cardAcceptorId, cardAcceptorNameLocation, additionalData)) {
                context.put("VISA_TRANSACTION_VALID", true);
            } else {
                context.put("VISA_TRANSACTION_VALID", false);
                context.put("VISA_ERROR", "Invalid Visa transaction data");
            }

        } catch (RuntimeException e) {
            context.put("VISA_TRANSACTION_VALID", false);
            context.put("VISA_ERROR", "Error processing Visa fields: " + e.getMessage());
        }
    }

    private boolean isValidVisaTransaction(String cardAcceptorId, String cardAcceptorNameLocation, String additionalData) {
        // Implement Visa-specific validation logic
        return cardAcceptorId != null && cardAcceptorNameLocation != null && additionalData != null;
    }
}
```

### 3. Implement Mastercard-specific Participant

Similarly, let's create a concrete Mastercard participant:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.transaction.Context;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class MastercardAuthorizationParticipant extends MastercardParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        int result = super.prepare(id, context);
        if ("MASTERCARD".equals(((Context) context).get("CARD_TYPE"))) { // only Mastercard cards; any other card passes through untouched
            processSpecificFields((Context) context);
        }
        return result;
    }

    @Override
    protected void processSpecificFields(Context context) {
        ISOMsg msg = (ISOMsg) context.get("REQUEST");
        try {
            // Process Mastercard-specific fields
            String additionalData = msg.getString(48);
            String posData = msg.getString(61);
            String networkData = msg.getString(63);

            // Validate and process Mastercard-specific data
            if (isValidMastercardTransaction(additionalData, posData, networkData)) {
                context.put("MASTERCARD_TRANSACTION_VALID", true);
            } else {
                context.put("MASTERCARD_TRANSACTION_VALID", false);
                context.put("MASTERCARD_ERROR", "Invalid Mastercard transaction data");
            }

        } catch (RuntimeException e) {
            context.put("MASTERCARD_TRANSACTION_VALID", false);
            context.put("MASTERCARD_ERROR", "Error processing Mastercard fields: " + e.getMessage());
        }
    }

    private boolean isValidMastercardTransaction(String additionalData, String posData, String networkData) {
        // Implement Mastercard-specific validation logic
        return additionalData != null && posData != null && networkData != null;
    }
}
```

### 4. Configure Transaction Manager

To use these participants in the transaction flow, configure them in the TransactionManager's Q2 deploy descriptor:

```xml
<txnmgr class="org.jpos.transaction.TransactionManager" logger="Q2">
    <property name="queue" value="txnqueue"/>
    <property name="sessions" value="2"/>
    <property name="max-sessions" value="128"/>
    <participant class="CardTypeIdentifierParticipant" logger="Q2"/>
    <participant class="VisaAuthorizationParticipant" logger="Q2"/>
    <participant class="MastercardAuthorizationParticipant" logger="Q2"/>
    <!-- Other participants -->
</txnmgr>
```

### 5. Implement Card Type Identifier

To determine which participant should process the transaction, create a `CardTypeIdentifierParticipant`:

```java
import org.jpos.transaction.TransactionParticipant;
import org.jpos.transaction.Context;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CardTypeIdentifierParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable serializable) {
        Context context = (Context) serializable;
        ISOMsg msg = (ISOMsg) context.get("REQUEST");
        try {
            String pan = msg.getString(2);
            String cardType = identifyCardType(pan);
            context.put("CARD_TYPE", cardType);
        } catch (RuntimeException e) {
            context.put("CARD_TYPE_ERROR", "Error identifying card type: " + e.getMessage());
        }
        return PREPARED | NO_JOIN | READONLY;
    }

    private String identifyCardType(String pan) {
        if (pan.startsWith("4")) {
            return "VISA";
        } else if (pan.startsWith("51") || pan.startsWith("52") || pan.startsWith("53") || 
                   pan.startsWith("54") || pan.startsWith("55")) {
            return "MASTERCARD";
        } else {
            return "UNKNOWN";
        }
    }

    @Override
    public void commit(long id, Serializable context) {}

    @Override
    public void abort(long id, Serializable context) {}
}
```

### 6. Testing

To ensure our Visa and Mastercard specific processing works correctly, let's create unit tests:

```java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.transaction.Context;
import static org.junit.jupiter.api.Assertions.*;

class VisaAuthorizationParticipantTest {

    private VisaAuthorizationParticipant participant;
    private Context context;
    private ISOMsg msg;

    @BeforeEach
    void setUp() throws ISOException {
        participant = new VisaAuthorizationParticipant();
        context = new Context();
        msg = new ISOMsg();
        msg.set(2, "4111111111111111");
        msg.set(42, "VISA_MERCHANT_ID");
        msg.set(43, "VISA_MERCHANT_NAME_LOCATION");
        msg.set(48, "ADDITIONAL_DATA");
        context.put("REQUEST", msg);
        context.put("CARD_TYPE", "VISA");
    }

    @Test
    void testValidVisaTransaction() {
        participant.prepare(1L, context);
        assertEquals(Boolean.TRUE, context.get("VISA_TRANSACTION_VALID"));
        assertNull(context.get("VISA_ERROR"));
    }

    @Test
    void testInvalidVisaTransaction() throws ISOException {
        msg.unset(42);
        participant.prepare(1L, context);
        assertEquals(Boolean.FALSE, context.get("VISA_TRANSACTION_VALID"));
        assertNotNull(context.get("VISA_ERROR"));
    }
}

class MastercardAuthorizationParticipantTest {

    private MastercardAuthorizationParticipant participant;
    private Context context;
    private ISOMsg msg;

    @BeforeEach
    void setUp() throws ISOException {
        participant = new MastercardAuthorizationParticipant();
        context = new Context();
        msg = new ISOMsg();
        msg.set(2, "5111111111111111");
        msg.set(48, "ADDITIONAL_DATA");
        msg.set(61, "POS_DATA");
        msg.set(63, "NETWORK_DATA");
        context.put("REQUEST", msg);
        context.put("CARD_TYPE", "MASTERCARD");
    }

    @Test
    void testValidMastercardTransaction() {
        participant.prepare(1L, context);
        assertEquals(Boolean.TRUE, context.get("MASTERCARD_TRANSACTION_VALID"));
        assertNull(context.get("MASTERCARD_ERROR"));
    }

    @Test
    void testInvalidMastercardTransaction() throws ISOException {
        msg.unset(61);
        participant.prepare(1L, context);
        assertEquals(Boolean.FALSE, context.get("MASTERCARD_TRANSACTION_VALID"));
        assertNotNull(context.get("MASTERCARD_ERROR"));
    }
}
```

This implementation provides a solid foundation for processing Visa and Mastercard transactions in a jPOS server. The custom participants handle the specific requirements for each card network, while the `CardTypeIdentifierParticipant` ensures that the correct participant is used for each transaction.

The modular design allows for easy extension and modification of the processing logic for each card network. The unit tests ensure that the participants behave correctly for both valid and invalid transactions.


# 6. Data Elements Handling in jPOS Server

Data elements are the building blocks of ISO-8583 messages. In jPOS, we primarily work with two main classes for handling data elements: `ISOMsg` and `ISOField`. Let's explore how to use these classes effectively in a jPOS server implementation.

## 6.1 ISOMsg

`ISOMsg` represents an ISO-8583 message. It's a container for all the fields (data elements) in the message.

### Key features of ISOMsg:

1. Creating a new message
2. Setting and getting fields
3. Cloning messages
4. Merging messages
5. Packing and unpacking messages

Let's look at some code examples:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOMessageHandler {

    public ISOMsg createSampleMessage() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");  // Set Message Type Indicator
        msg.set(2, "4111111111111111");  // Primary Account Number
        msg.set(3, "000000");  // Processing Code
        msg.set(4, "000000012345");  // Transaction Amount
        msg.set(7, "0701234567");  // Transmission Date and Time
        msg.set(11, "123456");  // System Trace Audit Number
        msg.set(41, "12345678");  // Card Acceptor Terminal ID
        msg.set(42, "MERCHANT123456789");  // Card Acceptor ID Code
        return msg;
    }

    public void printMessageFields(ISOMsg msg) throws ISOException {
        System.out.println("MTI: " + msg.getMTI());
        for (int i = 1; i <= msg.getMaxField(); i++) {
            if (msg.hasField(i)) {
                System.out.println("Field " + i + ": " + msg.getString(i));
            }
        }
    }

    public ISOMsg cloneAndModifyMessage(ISOMsg original) throws ISOException {
        ISOMsg cloned = (ISOMsg) original.clone();
        cloned.set(4, "000000054321");  // Modify the transaction amount
        return cloned;
    }
}
```

In this example, we've created methods to:
1. Create a sample ISO-8583 message
2. Print all fields in a message
3. Clone and modify a message

## 6.2 ISOField

`ISOField` represents a single field within an ISO-8583 message. While you often work with fields through the `ISOMsg` class, understanding `ISOField` is important for more advanced operations.

Here's an example of working directly with `ISOField`:

```java
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOException;

public class ISOFieldHandler {

    public ISOField createCustomField() throws ISOException {
        ISOField field = new ISOField(48);  // Field 48: Additional Data - Private Use
        field.setValue("CUSTOM DATA|MORE INFO|EXTRA");
        return field;
    }

    public void parseCustomField(ISOField field) throws ISOException {
        String fieldValue = (String) field.getValue();
        String[] parts = fieldValue.split("\\|");
        System.out.println("Field number: " + field.getFieldNumber());
        for (int i = 0; i < parts.length; i++) {
            System.out.println("Part " + (i + 1) + ": " + parts[i]);
        }
    }
}
```

In this example, we:
1. Create a custom field (Field 48) with pipe-separated values
2. Parse the custom field and print its components

## 6.3 Integrating Data Element Handling in a jPOS Server

Now, let's see how we can integrate these concepts into a jPOS server implementation. We'll create a `TransactionParticipant` that handles data elements:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;

public class DataElementHandler implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable serializable) {
        Context context = (Context) serializable;
        try {
            ISOMsg request = (ISOMsg) context.get("REQUEST");
            if (request != null) {
                // Process the incoming message
                processIncomingMessage(request);
                
                // Prepare the response
                ISOMsg response = prepareResponse(request);
                context.put("RESPONSE", response);
            }
            return PREPARED | NO_JOIN | READONLY;
        } catch (ISOException e) {
            context.put("EXCEPTION", e);
            return ABORTED | NO_JOIN | READONLY;
        }
    }

    private void processIncomingMessage(ISOMsg msg) throws ISOException {
        System.out.println("Processing incoming message:");
        System.out.println("MTI: " + msg.getMTI());
        System.out.println("PAN: " + msg.getString(2));
        System.out.println("Amount: " + msg.getString(4));
        // Process other fields as needed
    }

    private ISOMsg prepareResponse(ISOMsg request) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        response.set(39, "00");  // Approval code
        response.set(38, "123456");  // Authorization code
        return response;
    }

    @Override
    public void commit(long id, Serializable context) {
        // Nothing to commit in this example
    }

    @Override
    public void abort(long id, Serializable context) {
        // Handle abort scenario if needed
    }
}
```

In this `DataElementHandler`:

1. We implement the `TransactionParticipant` interface, which is part of jPOS's transaction framework.
2. In the `prepare` method, we extract the incoming `ISOMsg` from the context, process it, and prepare a response.
3. The `processIncomingMessage` method demonstrates how to access and log specific fields from the incoming message.
4. The `prepareResponse` method shows how to create a response message by cloning the request, setting it as a response MTI, and adding necessary fields.

To use this `DataElementHandler` in your jPOS server, you would configure it as part of your transaction manager in its Q2 deploy descriptor:

```xml
<txnmgr class="org.jpos.transaction.TransactionManager" logger="Q2">
    <property name="queue" value="txnqueue"/>
    <property name="sessions" value="2"/>
    <participant class="DataElementHandler" logger="Q2"/>
    <!-- Other participants -->
</txnmgr>
```

This configuration ensures that your `DataElementHandler` is part of the transaction processing flow in your jPOS server.

By implementing these concepts, you can effectively handle ISO-8583 data elements in your jPOS server, allowing you to process incoming messages, prepare responses, and manage the flow of financial transactions.


# 7. Security and Encryption in jPOS Server

Security is paramount when dealing with financial transactions. jPOS provides robust security features through its `org.jpos.security` package. We'll focus on two main components: the Security Module (SMAdapter) and the JCE Security Module.

## 7.1 Security Module (SMAdapter)

The `SMAdapter` interface defines methods for various security operations. Here's an overview of its key features:

```java
import org.jpos.security.BaseSMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureKeyStore;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SimpleKeyFile;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;

// BaseSMAdapter implements SMAdapter<SecureDESKey>; override the operations your HSM supports
public class CustomSMAdapter extends BaseSMAdapter<SecureDESKey> {
    private SecureKeyStore keyStore;

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        super.setConfiguration(cfg);
        // Initialize the key store
        try {
            this.keyStore = new SimpleKeyFile(cfg.get("keystore.path"));
        } catch (SecureKeyStore.SecureKeyStoreException e) {
            throw new ConfigurationException(e);
        }
    }

    @Override
    public SecureDESKey generateKey(short keyLength, String keyType) throws SMException {
        // Implementation for key generation, returning the new key encrypted under the Local Master Key (LMK)
        // This is just a placeholder, actual implementation would use secure random number generation
        return new SecureDESKey(keyLength, keyType, new byte[keyLength / 8], new byte[3]);
    }

    @Override
    public SecureDESKey importKey(short keyLength, String keyType, byte[] encryptedKey,
                                  SecureDESKey kek, boolean checkParity) throws SMException {
        // Implementation for re-encrypting a key received under a KEK so it is under the LMK
        // This is a simplified version, real implementation would use actual decryption and encryption
        return new SecureDESKey(keyLength, keyType, encryptedKey, new byte[3]);
    }

    @Override
    public byte[] exportKey(SecureDESKey key, SecureDESKey kek) throws SMException {
        // Implementation for re-encrypting a key from under the LMK to under a KEK, for sending
        // This is a simplified version, real implementation would use actual encryption
        return key.getKeyBytes();
    }

    // Other methods from SMAdapter interface...
}
```

## 7.2 JCE Security Module

The JCE (Java Cryptography Extension) Security Module provides a concrete implementation of the `SMAdapter` interface using Java's built-in cryptography features.

```java
import org.jpos.core.ConfigurationException;
import org.jpos.core.SimpleConfiguration;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public JCESecurityModule jceSecurityModule() throws ConfigurationException {
        JCESecurityModule module = new JCESecurityModule();
        org.jpos.core.Configuration cfg = new SimpleConfiguration();
        cfg.put("provider", "SunJCE");
        cfg.put("lmk", "src/main/resources/lmk.key");
        module.setConfiguration(cfg);
        return module;
    }
}
```

## 7.3 Implementing PIN Encryption

One of the most critical security operations in payment systems is PIN encryption. Here's an example of how to use the JCE Security Module to encrypt a PIN:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PINEncryptionService {

    @Autowired
    private JCESecurityModule securityModule;

    @Autowired
    private SecureDESKey zpk;  // Zone PIN Key shared with the next hop, stored under the LMK

    public ISOMsg encryptPIN(ISOMsg msg, String clearPIN) throws SMException {
        String pan = msg.getString(2);  // Primary Account Number
        byte pinBlockFormat = SMAdapter.FORMAT05;   // ISO Format 1

        EncryptedPIN pinUnderLmk = securityModule.encryptPIN(clearPIN, pan);
        EncryptedPIN encryptedPIN = securityModule.exportPIN(pinUnderLmk, zpk, pinBlockFormat);
        msg.set(52, encryptedPIN.getPINBlock());

        return msg;
    }
}
```

## 7.4 Key Management

Proper key management is crucial for maintaining the security of the system. Here's an example of how to implement key rotation:

```java
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SMException;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KeyRotationService {

    @Autowired
    private JCESecurityModule securityModule;

    @Autowired
    private KeyRepository keyRepository;

    @Scheduled(cron = "0 0 0 1 * ?")  // Run at midnight on the first day of every month
    public void rotateKeys() throws SMException {
        // Generate a new key (the HSM returns it already encrypted under the LMK, ready for storage)
        SecureDESKey newKey = securityModule.generateKey(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZPK);

        // Optionally, you might want to keep the old key for a period to handle in-flight transactions
        keyRepository.archiveKey("ZPK", keyRepository.getKey("ZPK"));

        // Store the new key
        keyRepository.storeKey("ZPK", newKey);
    }
}
```

## 7.5 Secure Communication Channel

To ensure secure communication between the client and server, we can use SSL/TLS. A jPOS channel such as `NACChannel` runs over SSL/TLS when you give it an SSL socket factory, `SunJSSESocketFactory`:

```java
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChannelConfig {

    @Bean
    public NACChannel secureChannel() throws Exception {
        NACChannel channel = new NACChannel("localhost", 7000, new ISO87APackager(), null);
        SunJSSESocketFactory sslFactory = new SunJSSESocketFactory();
        sslFactory.setKeyStore("path/to/keystore.jks");
        sslFactory.setPassword("keystorePassword");
        sslFactory.setKeyPassword("keyPassword");
        sslFactory.setServerAuthNeeded(true); // trust the server via the keystore's certificates
        channel.setSocketFactory(sslFactory);
        return channel;
    }
}
```

## 7.6 Testing Security Features

Here's an example of how to test the PIN encryption service using JUnit and Mockito:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.jpos.security.jceadapter.JCESecurityModule;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PINEncryptionServiceTest {

    @Mock
    private JCESecurityModule securityModule;

    @Mock
    private SecureDESKey zpk;

    @InjectMocks
    private PINEncryptionService pinEncryptionService;

    @Test
    void testEncryptPIN() throws SMException {
        // Arrange
        ISOMsg msg = new ISOMsg();
        msg.set(2, "1234567890123456");
        String clearPIN = "1234";
        byte[] encryptedPINBlock = {0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8};
        EncryptedPIN pinUnderLmk = new EncryptedPIN(new byte[8], SMAdapter.FORMAT01, "1234567890123456");

        when(securityModule.encryptPIN(any(), any())).thenReturn(pinUnderLmk);
        when(securityModule.exportPIN(any(), any(), anyByte()))
            .thenReturn(new EncryptedPIN(encryptedPINBlock, SMAdapter.FORMAT05, "1234567890123456"));

        // Act
        ISOMsg result = pinEncryptionService.encryptPIN(msg, clearPIN);

        // Assert
        assertArrayEquals(encryptedPINBlock, result.getBytes(52));
    }
}
```

This covers the main aspects of security and encryption in a jPOS server implementation. Remember that in a real-world scenario, you would need to implement additional security measures, such as:

1. Proper key storage and management
2. Regular security audits
3. Compliance with PCI DSS and other relevant standards
4. Implementing additional encryption for sensitive data fields
5. Secure coding practices to prevent common vulnerabilities


# 8. Logging and Debugging in jPOS Server

Logging and debugging are essential components of any robust jPOS server implementation. jPOS provides powerful logging capabilities through its built-in logging framework. Let's dive into the details and see how to implement effective logging and debugging in a jPOS server.

## 8.1 jPOS Logging Framework

jPOS uses its own logging framework, which is flexible and can be integrated with other logging systems like Log4j or SLF4J. The main classes involved in jPOS logging are:

- `org.jpos.util.Logger`
- `org.jpos.util.LogEvent`
- `org.jpos.util.LogListener`

### 8.1.1 Setting up the Logger

First, let's set up a basic logger in our jPOS server:

```java
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

public class JposServer {
    private Logger logger;

    public JposServer() {
        logger = new Logger();
        logger.addListener(new SimpleLogListener(System.out));
    }

    // Other server methods...
}
```

### 8.1.2 Creating Log Events

Now that we have a logger, we can create and log events:

```java
import org.jpos.util.LogEvent;

public class JposServer {
    // ... previous code ...

    public void processTransaction(ISOMsg msg) {
        LogEvent ev = new LogEvent(this, "transaction.process");
        try {
            ev.addMessage("Processing transaction: " + msg.getMTI());
            // Process the transaction
            // ...
            ev.addMessage("Transaction processed successfully");
        } catch (Exception e) {
            ev.addMessage(e);
        } finally {
            logger.log(ev);
        }
    }
}
```

## 8.2 Implementing Custom LogListener

While the `SimpleLogListener` is useful for basic logging, you might want to create a custom `LogListener` for more advanced logging needs:

```java
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;

public class CustomLogListener implements LogListener {
    @Override
    public LogEvent log(LogEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append(new Date()).append(" - ");
        sb.append(ev.getSource()).append(" - ");
        sb.append(ev.getTag()).append(": ");
        
        for (Object msg : ev.getPayLoad()) {
            sb.append(msg.toString()).append("\n");
        }
        
        System.out.println(sb.toString());
        return ev;
    }
}
```

Now, let's use this custom log listener in our server:

```java
public class JposServer {
    private Logger logger;

    public JposServer() {
        logger = new Logger();
        logger.addListener(new CustomLogListener());
    }

    // Other server methods...
}
```

## 8.3 Integration with Spring Framework

If you're using Spring Framework with jPOS, you can configure logging through Spring configuration:

```xml
<bean id="logger" class="org.jpos.util.Logger" init-method="addListener">
    <constructor-arg>
        <bean class="org.jpos.util.SimpleLogListener">
            <constructor-arg value="System.out"/>
        </bean>
    </constructor-arg>
</bean>
```

Then, you can inject this logger into your Spring-managed beans:

```java
@Service
public class TransactionService {
    @Autowired
    private Logger logger;

    public void processTransaction(ISOMsg msg) {
        LogEvent ev = new LogEvent(this, "transaction.process");
        // ... logging logic ...
        logger.log(ev);
    }
}
```

## 8.4 Debugging Techniques

### 8.4.1 Using LogEvent for Debugging

You can use `LogEvent` to add debug information:

```java
public void debugTransaction(ISOMsg msg) {
    LogEvent ev = new LogEvent(this, "transaction.debug");
    ev.addMessage("Transaction details:");
    ev.addMessage("MTI: " + msg.getMTI());
    ev.addMessage("PAN: " + msg.getString(2));
    ev.addMessage("Amount: " + msg.getString(4));
    logger.log(ev);
}
```

### 8.4.2 Implementing a Debug Mode

You can implement a debug mode in your server:

```java
public class JposServer {
    private boolean debugMode = false;

    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        if (debug) {
            logger.addListener(new VerboseLogListener());
        } else {
            logger.removeListener("org.jpos.util.VerboseLogListener");
        }
    }

    public void processTransaction(ISOMsg msg) {
        if (debugMode) {
            debugTransaction(msg);
        }
        // Normal processing...
    }
}
```

## 8.5 Log Rotation and Management

For production environments, it's crucial to manage log files effectively. jPOS provides a `RotateLogListener` for this purpose:

```java
import org.jpos.util.RotateLogListener;

public class JposServer {
    public JposServer() {
        logger = new Logger();
        logger.addListener(new RotateLogListener("logs/server.log"));
    }
}
```

This will create log files like `server.log`, `server.log.1`, `server.log.2`, etc., rotating them based on size or time.

## 8.6 Unit Testing with Logging

When unit testing your jPOS server components, you can use a special log listener to capture log events for assertions:

```java
import org.junit.jupiter.api.Test;
import org.jpos.util.Logger;
import org.jpos.util.LogEvent;
import static org.assertj.core.api.Assertions.assertThat;

public class JposServerTest {

    private static class TestLogListener implements LogListener {
        private List<LogEvent> events = new ArrayList<>();

        @Override
        public LogEvent log(LogEvent ev) {
            events.add(ev);
            return ev;
        }

        public List<LogEvent> getEvents() {
            return events;
        }
    }

    @Test
    public void testTransactionLogging() {
        JposServer server = new JposServer();
        TestLogListener testListener = new TestLogListener();
        server.getLogger().addListener(testListener);

        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        server.processTransaction(msg);

        assertThat(testListener.getEvents())
            .hasSize(1)
            .allSatisfy(event -> {
                assertThat(event.getTag()).isEqualTo("transaction.process");
                assertThat(event.getPayLoad().toString()).contains("Processing transaction: 0200");
            });
    }
}
```

This concludes the coverage of section 8 on Logging and Debugging in jPOS Server implementation. Effective logging and debugging are crucial for maintaining and troubleshooting jPOS applications, especially in production environments where quick issue resolution is essential.


# 9. Database Integration

Database integration is crucial for storing and retrieving transaction data, user information, and other relevant details in a payment processing system. In this section, we'll implement custom Spring Data JPA repositories to handle database operations for our jPOS server.

## 9.1 Setting up the Database Configuration

First, let's set up the database configuration using Spring Boot. We'll use PostgreSQL as our database.

Add the following dependencies to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

Now, configure the database connection in your `application.properties` file:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/jpos_db
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

## 9.2 Creating Entity Classes

Let's create some entity classes to represent our data models. We'll start with a `Transaction` entity:

```java
package com.example.jposserver.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "card_number", nullable = false)
    private String cardNumber;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and setters
    // ...

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

## 9.3 Creating Repository Interfaces

Now, let's create a repository interface for our `Transaction` entity using Spring Data JPA:

```java
package com.example.jposserver.repository;

import com.example.jposserver.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByCardNumber(String cardNumber);

    List<Transaction> findByStatus(String status);

    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
```

## 9.4 Creating a Service Layer

To encapsulate the business logic and database operations, let's create a service layer:

```java
package com.example.jposserver.service;

import com.example.jposserver.entity.Transaction;
import com.example.jposserver.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction saveTransaction(Transaction transaction) {
        return transactionRepository.save(transaction);
    }

    public Optional<Transaction> getTransactionById(String transactionId) {
        return transactionRepository.findByTransactionId(transactionId);
    }

    public List<Transaction> getTransactionsByCardNumber(String cardNumber) {
        return transactionRepository.findByCardNumber(cardNumber);
    }

    public List<Transaction> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status);
    }

    public List<Transaction> getTransactionsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return transactionRepository.findByCreatedAtBetween(start, end);
    }
}
```

## 9.5 Integrating with jPOS

Now that we have our database integration set up, let's create a custom `TransactionParticipant` that uses our `TransactionService` to store transaction data:

```java
package com.example.jposserver.participant;

import com.example.jposserver.entity.Transaction;
import com.example.jposserver.service.TransactionService;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;

@Component
public class TransactionStorageParticipant implements TransactionParticipant, Configurable {

    private final TransactionService transactionService;
    private Configuration cfg;

    @Autowired
    public TransactionStorageParticipant(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        Transaction transaction = createTransactionFromContext(ctx);
        transactionService.saveTransaction(transaction);
        return PREPARED | NO_JOIN | READONLY;
    }

    @Override
    public void commit(long id, Serializable context) {
        // No action needed, as the transaction is already saved in the prepare phase
    }

    @Override
    public void abort(long id, Serializable context) {
        // No action needed, as we're not modifying any data in this participant
    }

    private Transaction createTransactionFromContext(Context ctx) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(ctx.getString("transactionId"));
        transaction.setAmount(new BigDecimal(ctx.getString("amount")));
        transaction.setCurrency(ctx.getString("currency"));
        transaction.setCardNumber(ctx.getString("cardNumber"));
        transaction.setTransactionType(ctx.getString("transactionType"));
        transaction.setStatus(ctx.getString("status"));
        return transaction;
    }

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
    }
}
```

## 9.6 Configuring the TransactionStorageParticipant

To use the `TransactionStorageParticipant` in your jPOS transaction manager, add it to your `deploy/05_spring_context.xml` file:

```xml
<bean id="transactionStorageParticipant" class="com.example.jposserver.participant.TransactionStorageParticipant" />

<bean id="txnmgr" class="org.jpos.transaction.TransactionManager">
    <property name="space" value="tspace:default" />
    <property name="queue" value="txnmgr" />
    <property name="participants">
        <list>
            <!-- Other participants -->
            <ref bean="transactionStorageParticipant" />
        </list>
    </property>
</bean>
```

## 9.7 Testing the Database Integration

Let's create a test class to ensure our database integration is working correctly:

```java
package com.example.jposserver.service;

import com.example.jposserver.entity.Transaction;
import com.example.jposserver.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private TransactionRepository transactionRepository;

    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testTransaction = new Transaction();
        testTransaction.setTransactionId("TEST123");
        testTransaction.setAmount(new BigDecimal("100.00"));
        testTransaction.setCurrency("USD");
        testTransaction.setCardNumber("1234567890123456");
        testTransaction.setTransactionType("PURCHASE");
        testTransaction.setStatus("APPROVED");
    }

    @Test
    void testSaveTransaction() {
        Transaction savedTransaction = transactionService.saveTransaction(testTransaction);
        assertThat(savedTransaction.getId()).isNotNull();
        assertThat(savedTransaction.getTransactionId()).isEqualTo("TEST123");
    }

    @Test
    void testGetTransactionById() {
        transactionRepository.save(testTransaction);
        Optional<Transaction> foundTransaction = transactionService.getTransactionById("TEST123");
        assertThat(foundTransaction).isPresent();
        assertThat(foundTransaction.get().getTransactionId()).isEqualTo("TEST123");
    }

    @Test
    void testGetTransactionsByCardNumber() {
        transactionRepository.save(testTransaction);
        List<Transaction> transactions = transactionService.getTransactionsByCardNumber("1234567890123456");
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getCardNumber()).isEqualTo("1234567890123456");
    }

    @Test
    void testGetTransactionsByStatus() {
        transactionRepository.save(testTransaction);
        List<Transaction> transactions = transactionService.getTransactionsByStatus("APPROVED");
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void testGetTransactionsBetweenDates() {
        transactionRepository.save(testTransaction);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        List<Transaction> transactions = transactionService.getTransactionsBetweenDates(start, end);
        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getCreatedAt()).isBetween(start, end);
    }
}
```

This completes the coverage of section 9 for the jPOS server implementation, focusing on Database Integration. We've set up the database configuration, created entity classes and repositories, implemented a service layer, integrated it with jPOS using a custom `TransactionParticipant`, and provided tests to ensure everything is working correctly.


# 10. Error Handling

Error handling is a crucial aspect of any robust jPOS server implementation. Proper error handling ensures that your application can gracefully manage unexpected situations, provide meaningful feedback, and maintain system stability. In a jPOS server, errors can occur at various levels, including message parsing, transaction processing, and network communication.

## 10.1 Types of Errors

In a jPOS server, you may encounter several types of errors:

1. ISO-8583 message parsing errors
2. Transaction processing errors
3. Network communication errors
4. Database errors
5. Security-related errors

## 10.2 Implementing Error Handling

Let's implement a comprehensive error handling strategy for a jPOS server using Spring's `@ExceptionHandler` and custom exception classes.

First, let's create some custom exception classes:

```java
public class ISO8583ParseException extends RuntimeException {
    public ISO8583ParseException(String message) {
        super(message);
    }
}

public class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message) {
        super(message);
    }
}

public class NetworkCommunicationException extends RuntimeException {
    public NetworkCommunicationException(String message) {
        super(message);
    }
}
```

Now, let's create a global exception handler using Spring's `@ControllerAdvice`:

```java
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ISO8583ParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ISOMsg handleISO8583ParseException(ISO8583ParseException ex) {
        return createErrorResponse("01", ex.getMessage());
    }

    @ExceptionHandler(TransactionProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ISOMsg handleTransactionProcessingException(TransactionProcessingException ex) {
        return createErrorResponse("02", ex.getMessage());
    }

    @ExceptionHandler(NetworkCommunicationException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ISOMsg handleNetworkCommunicationException(NetworkCommunicationException ex) {
        return createErrorResponse("03", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ISOMsg handleGenericException(Exception ex) {
        return createErrorResponse("99", "An unexpected error occurred");
    }

    private ISOMsg createErrorResponse(String errorCode, String errorMessage) {
        try {
            ISOMsg response = new ISOMsg();
            response.setMTI("0810");  // Assuming 0810 is used for error responses
            response.set(39, errorCode);  // Response code field
            response.set(63, errorMessage);  // Additional data field for error message
            return response;
        } catch (ISOException e) {
            // If we can't create an ISO message, return null and let the framework handle it
            return null;
        }
    }
}
```

This global exception handler catches different types of exceptions and creates appropriate ISO-8583 error responses.

Next, let's create a custom TransactionParticipant that demonstrates how to use these exceptions:

```java
import org.jpos.transaction.TransactionParticipant;
import org.jpos.transaction.Context;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class SampleTransactionParticipant implements TransactionParticipant {

    @Override
    public int prepare(long id, Context context) {
        try {
            ISOMsg msg = (ISOMsg) context.get("REQUEST");
            if (msg == null) {
                throw new ISO8583ParseException("Invalid or missing ISO message");
            }

            // Simulate some processing that might throw exceptions
            String processingCode = msg.getString(3);
            if ("999999".equals(processingCode)) {
                throw new TransactionProcessingException("Invalid processing code");
            }

            // Simulate a network error
            if ("888888".equals(processingCode)) {
                throw new NetworkCommunicationException("Network is unavailable");
            }

            // Normal processing continues...
            return PREPARED | NO_JOIN;
        } catch (ISOException e) {
            context.put("EXCEPTION", new ISO8583ParseException("Error parsing ISO message: " + e.getMessage()));
            return ABORTED | NO_JOIN;
        }
    }

    @Override
    public void commit(long id, Context context) {
        // Commit logic here
    }

    @Override
    public void abort(long id, Context context) {
        // Abort logic here
    }
}
```

To use this participant in your TransactionManager, you would configure it in your Spring context:

```xml
<bean id="txnmgr" class="org.jpos.transaction.TransactionManager">
    <property name="space" value="tspace:default"/>
    <property name="queue" value="txnqueue"/>
    <property name="participants">
        <list>
            <bean class="com.example.SampleTransactionParticipant"/>
            <!-- Other participants... -->
        </list>
    </property>
</bean>
```

Finally, let's add some logging to our error handling. We'll use SLF4J with Logback for this example:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ISO8583ParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ISOMsg handleISO8583ParseException(ISO8583ParseException ex) {
        logger.error("ISO-8583 Parse Error: {}", ex.getMessage(), ex);
        return createErrorResponse("01", ex.getMessage());
    }

    // ... other exception handlers ...
}
```

Make sure to configure Logback in your `logback.xml` file:

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/jpos-server.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/jpos-server.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

This configuration will log errors to both the console and a rolling file.

With these implementations in place, your jPOS server will have a robust error handling mechanism that:

1. Catches and categorizes different types of exceptions
2. Creates appropriate ISO-8583 error responses
3. Logs detailed error information for troubleshooting
4. Allows for graceful degradation of the system in case of errors



# 11. Testing in jPOS Server Implementation

Testing is a crucial part of developing a robust jPOS server implementation. It ensures that your ISO-8583 message processing, transaction handling, and integration with other components work as expected. Let's dive into the various aspects of testing a jPOS server.

## 11.1 Unit Testing

Unit tests focus on testing individual components or classes in isolation. For a jPOS server, you'll want to test individual participants, message handlers, and utility classes.

### Example: Testing a TransactionParticipant

Let's create a simple TransactionParticipant and test it:

```java
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

public class AmountValidationParticipant implements TransactionParticipant {
    @Override
    public int prepare(long id, Context context) {
        String amount = (String) context.get("amount");
        if (amount == null || amount.isEmpty()) {
            context.put("error", "Amount is missing");
            return ABORTED;
        }
        try {
            double amountValue = Double.parseDouble(amount);
            if (amountValue <= 0) {
                context.put("error", "Amount must be positive");
                return ABORTED;
            }
        } catch (NumberFormatException e) {
            context.put("error", "Invalid amount format");
            return ABORTED;
        }
        return PREPARED;
    }

    @Override
    public void commit(long id, Context context) {
        // No action needed for this participant
    }

    @Override
    public void abort(long id, Context context) {
        // No action needed for this participant
    }
}
```

Now, let's write a unit test for this participant:

```java
import org.jpos.transaction.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AmountValidationParticipantTest {

    private AmountValidationParticipant participant;
    private Context context;

    @BeforeEach
    void setUp() {
        participant = new AmountValidationParticipant();
        context = new Context();
    }

    @Test
    void testValidAmount() {
        context.put("amount", "100.00");
        int result = participant.prepare(1L, context);
        assertThat(result).isEqualTo(TransactionParticipant.PREPARED);
        assertThat(context.get("error")).isNull();
    }

    @Test
    void testMissingAmount() {
        int result = participant.prepare(1L, context);
        assertThat(result).isEqualTo(TransactionParticipant.ABORTED);
        assertThat(context.get("error")).isEqualTo("Amount is missing");
    }

    @Test
    void testNegativeAmount() {
        context.put("amount", "-50.00");
        int result = participant.prepare(1L, context);
        assertThat(result).isEqualTo(TransactionParticipant.ABORTED);
        assertThat(context.get("error")).isEqualTo("Amount must be positive");
    }

    @Test
    void testInvalidAmountFormat() {
        context.put("amount", "not-a-number");
        int result = participant.prepare(1L, context);
        assertThat(result).isEqualTo(TransactionParticipant.ABORTED);
        assertThat(context.get("error")).isEqualTo("Invalid amount format");
    }
}
```

## 11.2 Integration Testing

Integration tests verify that different components of your jPOS server work together correctly. This includes testing the interaction between participants, channels, and other server components.

### Example: Testing a Transaction Flow

Let's create a simple integration test that simulates a transaction flow:

```java
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class TransactionFlowIntegrationTest {

    @Autowired
    private TransactionManager txnManager;

    @Autowired
    private QMUX mux;

    private Space space;

    @BeforeEach
    void setUp() throws Exception {
        space = SpaceFactory.getSpace("tspace:default");
        
        // Configure and start QMUX
        mux.setConfiguration(new SimpleConfiguration());
        mux.addChannel("test-channel", new NACChannel("localhost", 10000, new GenericPackager("packager/iso87ascii.xml")));
        mux.start();
    }

    @Test
    void testSuccessfulTransaction() throws Exception {
        // Create a sample ISO message
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(3, "000000");
        msg.set(4, "000000012345");
        msg.set(11, "000001");
        msg.set(41, "12345678");
        msg.set(42, "000000000000001");

        // Send the message through QMUX
        ISOMsg response = mux.request(msg, 30000);

        // Verify the response
        assertThat(response).isNotNull();
        assertThat(response.getMTI()).isEqualTo("0210");
        assertThat(response.getString(39)).isEqualTo("00");

        // Check the transaction manager's space for the processed transaction
        Context ctx = (Context) space.inp("$" + response.getString(11));
        assertThat(ctx).isNotNull();
        assertThat(ctx.get("txnResult")).isEqualTo("SUCCESS");
    }
}
```

This integration test sets up a QMUX, sends a transaction message, and verifies both the response and the transaction manager's processing of the message.

## 11.3 Mock Testing

Mock testing is useful when you want to isolate your tests from external dependencies or simulate specific scenarios. We'll use Mockito for mocking and Wiremock for simulating external services.

### Example: Mocking an External Service Call

Let's say we have a participant that calls an external fraud check service. We'll mock this service call:

```java
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.springframework.web.client.RestTemplate;

public class FraudCheckParticipant implements TransactionParticipant {
    private final RestTemplate restTemplate;
    private final String fraudCheckUrl;

    public FraudCheckParticipant(RestTemplate restTemplate, String fraudCheckUrl) {
        this.restTemplate = restTemplate;
        this.fraudCheckUrl = fraudCheckUrl;
    }

    @Override
    public int prepare(long id, Context context) {
        String cardNumber = (String) context.get("cardNumber");
        String amount = (String) context.get("amount");

        FraudCheckRequest request = new FraudCheckRequest(cardNumber, amount);
        FraudCheckResponse response = restTemplate.postForObject(fraudCheckUrl, request, FraudCheckResponse.class);

        if (response.isFraudulent()) {
            context.put("fraudDetected", true);
            return ABORTED;
        }

        return PREPARED;
    }

    // ... commit and abort methods
}

class FraudCheckRequest {
    private String cardNumber;
    private String amount;

    // constructor, getters, setters
}

class FraudCheckResponse {
    private boolean fraudulent;

    // constructor, getters, setters
}
```

Now, let's write a test using Mockito to mock the RestTemplate:

```java
import org.jpos.transaction.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FraudCheckParticipantTest {

    @Mock
    private RestTemplate restTemplate;

    private FraudCheckParticipant participant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        participant = new FraudCheckParticipant(restTemplate, "http://fraud-check-service.com/check");
    }

    @Test
    void testNonFraudulentTransaction() {
        Context context = new Context();
        context.put("cardNumber", "1234567890123456");
        context.put("amount", "100.00");

        FraudCheckResponse mockResponse = new FraudCheckResponse();
        mockResponse.setFraudulent(false);

        when(restTemplate.postForObject(anyString(), any(), eq(FraudCheckResponse.class)))
            .thenReturn(mockResponse);

        int result = participant.prepare(1L, context);

        assertThat(result).isEqualTo(TransactionParticipant.PREPARED);
        assertThat(context.get("fraudDetected")).isNull();
    }

    @Test
    void testFraudulentTransaction() {
        Context context = new Context();
        context.put("cardNumber", "9876543210987654");
        context.put("amount", "5000.00");

        FraudCheckResponse mockResponse = new FraudCheckResponse();
        mockResponse.setFraudulent(true);

        when(restTemplate.postForObject(anyString(), any(), eq(FraudCheckResponse.class)))
            .thenReturn(mockResponse);

        int result = participant.prepare(1L, context);

        assertThat(result).isEqualTo(TransactionParticipant.ABORTED);
        assertThat(context.get("fraudDetected")).isEqualTo(true);
    }
}
```

## 11.4 Wiremock for External Service Simulation

For more complex scenarios or when you want to test the actual HTTP calls, you can use Wiremock to simulate the external fraud check service:

```java
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jpos.transaction.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class FraudCheckParticipantWiremockTest {

    private WireMockServer wireMockServer;
    private FraudCheckParticipant participant;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        RestTemplate restTemplate = new RestTemplate();
        participant = new FraudCheckParticipant(restTemplate, "http://localhost:8089/fraud-check");
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testNonFraudulentTransaction() {
        stubFor(post(urlEqualTo("/fraud-check"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fraudulent\": false}")
            ));

        Context context = new Context();
        context.put("cardNumber", "1234567890123456");
        context.put("amount", "100.00");

        int result = participant.prepare(1L, context);

        assertThat(result).isEqualTo(TransactionParticipant.PREPARED);
        assertThat(context.get("fraudDetected")).isNull();

        verify(postRequestedFor(urlEqualTo("/fraud-check"))
            .withRequestBody(matchingJsonPath("$.cardNumber", equalTo("1234567890123456")))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("100.00")))
        );
    }

    @Test
    void testFraudulentTransaction() {
        stubFor(post(urlEqualTo("/fraud-check"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"fraudulent\": true}")
            ));

        Context context = new Context();
        context.put("cardNumber", "9876543210987654");
        context.put("amount", "5000.00");

        int result = participant.prepare(1L, context);

        assertThat(result).isEqualTo(TransactionParticipant.ABORTED);
        assertThat(context.get("fraudDetected")).isEqualTo(true);

        verify(postRequestedFor(urlEqualTo("/fraud-check"))
            .withRequestBody(matchingJsonPath("$.cardNumber", equalTo("9876543210987654")))
            .withRequestBody(matchingJsonPath("$.amount", equalTo("5000.00")))
        );
    }
}
```

This Wiremock test actually simulates the HTTP calls to the fraud check service, allowing you to test the full flow of your participant, including the network communication.

## 11.5 Performance Testing

For a jPOS server, performance testing is crucial to ensure it can handle the expected transaction load. You can use tools like JMeter or Gatling to simulate high transaction volumes and measure the server's performance.

Here's a simple JUnit test that measures the performance of processing multiple transactions:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PerformanceTest {

    @Autowired
    private QMUX mux;

    @Test
    void testConcurrentTransactions() throws InterruptedException {
        int numTransactions = 1000;
        int numThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numTransactions);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numTransactions; i++) {
            executorService.submit(() -> {
                try {
                    ISOMsg msg = createSampleISOMsg();
                    ISOMsg response = mux.request(msg, 30000);
                    assertThat(response).isNotNull();
                    assertThat(response.getString(39)).isEqualTo("00");
                } catch (Exception e) {
                    fail("Transaction failed", e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(5, TimeUnit.MINUTES);
        long endTime = System.currentTimeMillis();

        assertThat(completed).isTrue();

        long totalTime = endTime - startTime;
        double tps = (double) numTransactions / (totalTime / 1000.0);

        System.out.printf("Processed %d transactions in %d ms (%.2f TPS)%n", numTransactions, totalTime, tps);

        executorService.shutdown();
    }

    private ISOMsg createSampleISOMsg() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(3, "000000");
        msg.set(4, "000000012345");
        msg.set(11, String.format("%06d", (int) (Math.random() * 1000000)));
        msg.set(41, "12345678");
        msg.set(42, "000000000000001");
        return msg;
    }
}
```

This test simulates processing 1000 transactions concurrently using 10 threads and measures the overall throughput in transactions per second (TPS).
