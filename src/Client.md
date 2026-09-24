# jPOS Client: 1. Basic Setup

## Introduction

Setting up a jPOS client involves configuring the Spring framework and Q2, which is jPOS's deployment and runtime environment. This setup allows you to create a robust and flexible ISO-8583 client application.

## 1.1 Spring Configuration

Spring is used to manage dependencies and configure the application context. jPOS 2.1.7 has no Spring container for Q2, so the main application class in section 1.3 doubles as a small QBean that starts the Spring context inside Q2.

First, let's set up the necessary dependencies in your `pom.xml`:

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

Now, let's create a Spring configuration class:

```java
package com.example.jpos.config;

import org.jpos.iso.MUX;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JposConfig {

    @Bean
    public MUX qmux() throws NameRegistrar.NotFoundException {
        return QMUX.getMUX("mux");
    }

    // Add more beans as needed
}
```

## 1.2 Q2 Configuration

Q2 is configured using XML files. Create a file named `05_spring_client_context.xml` in the `deploy` directory:

```xml
<spring-context class="com.example.jpos.JposClientApplication">
    <property name="config" value="classpath:applicationContext.xml" />
</spring-context>
```

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

    <context:component-scan base-package="com.example.jpos"/>
    <context:annotation-config/>

    <!-- Add any additional bean definitions here -->

</beans>
```

## 1.3 Creating the Main Application

Now, let's create the main application class. Its `main` starts the Q2 container, and Q2 runs the same class as the QBean in `05_spring_client_context.xml`, which starts the Spring context:

```java
package com.example.jpos;

import org.jpos.q2.Q2;
import org.jpos.q2.QBeanSupport;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JposClientApplication extends QBeanSupport {
    private ClassPathXmlApplicationContext context;

    public static void main(String[] args) {
        Q2 q2 = new Q2();
        q2.start();
    }

    @Override
    protected void startService() {
        context = new ClassPathXmlApplicationContext(cfg.get("config"));
    }

    @Override
    protected void stopService() {
        context.close();
    }
}
```

## 1.4 Setting up the Client Channel

To communicate with the server, we need to set up a client channel. Create a file named `10_client_channel.xml` in the `deploy` directory:

```xml
<channel-adaptor name='client-channel' class="org.jpos.q2.iso.ChannelAdaptor" logger="Q2">
 <channel class="org.jpos.iso.channel.NACChannel" packager="org.jpos.iso.packager.GenericPackager">
   <property name="host" value="localhost" />
   <property name="port" value="10000" />
   <property name="packager-config" value="jar:packager/iso87ascii.xml" />
 </channel>
 <in>send</in>
 <out>receive</out>
 <reconnect-delay>10000</reconnect-delay>
</channel-adaptor>
```

## 1.5 Setting up the QMUX

QMUX is used for multiplexing ISO messages. Create a file named `20_mux.xml` in the `deploy` directory:

```xml
<mux class="org.jpos.q2.iso.QMUX" logger="Q2" name="mux">
 <in>receive</in>
 <out>send</out>
</mux>
```

## 1.6 Creating a Simple ISO-8583 Client Service

Let's create a simple service that will send ISO-8583 messages:

```java
package com.example.jpos.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IsoClientService {

    private final MUX qmux;

    @Autowired
    public IsoClientService(MUX qmux) {
        this.qmux = qmux;
    }

    public ISOMsg sendMessage(ISOMsg message) throws ISOException {
        return qmux.request(message, 30000);
    }
}
```

## 1.7 Testing the Setup

To ensure everything is set up correctly, let's create a simple test:

```java
package com.example.jpos;

import com.example.jpos.service.IsoClientService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JposClientApplicationTests {

    @Test
    public void testSendMessage() throws ISOException {
        try (ClassPathXmlApplicationContext context =
                     new ClassPathXmlApplicationContext("applicationContext.xml")) {
            IsoClientService isoClientService = context.getBean(IsoClientService.class);

            ISOMsg message = new ISOMsg();
            message.setMTI("0800");
            message.set(7, "0110122359");
            message.set(11, "000001");
            message.set(70, "301");

            ISOMsg response = isoClientService.sendMessage(message);

            assertNotNull(response);
            assertEquals("0810", response.getMTI());
        }
    }
}
```

This test assumes Q2 is running with the channel and QMUX above deployed, and a server that can respond to the 0800 message with a 0810 message.

## Conclusion

This setup provides a basic foundation for a jPOS client using Spring and Q2. It includes:

1. Spring configuration for dependency injection
2. Q2 configuration for the jPOS runtime environment
3. A client channel setup for communication
4. A QMUX configuration for message multiplexing
5. A simple service for sending ISO-8583 messages
6. A basic test to verify the setup


# 2. ISO-8583 Message Creation

ISO-8583 message creation is a crucial part of implementing a jPOS client. This process involves constructing properly formatted messages to communicate with financial institutions or payment processors. Let's dive into the details of creating ISO-8583 messages using jPOS.

## 2.1 ISOMsg

The `ISOMsg` class is the core component for creating and manipulating ISO-8583 messages in jPOS. It represents a single ISO-8583 message and provides methods to set and get field values.

Here's an example of how to create a basic `ISOMsg`:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOMessageCreator {

    public ISOMsg createBasicMessage() throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setMTI("0200");  // Set Message Type Indicator
        message.set(2, "4111111111111111");  // Primary Account Number
        message.set(3, "000000");  // Processing Code
        message.set(4, "000000010000");  // Transaction Amount
        message.set(7, "0701104321");  // Transmission Date and Time
        message.set(11, "123456");  // System Trace Audit Number
        message.set(41, "12345678");  // Card Acceptor Terminal ID
        message.set(49, "840");  // Transaction Currency Code (USD)
        return message;
    }
}
```

In this example, we create a new `ISOMsg` object and set various fields using the `set` method. The first argument is the field number, and the second is the field value.

## 2.2 Message Factory

While creating messages manually is possible, it's often more efficient and less error-prone to use a message factory of your own. jPOS 2.1.7 has no `MsgFactory` type, so the factory is a plain class that builds each `ISOMsg` with `setMTI` and `set`, and lets you create predefined message templates and reuse them.

Here's an example of how to implement such a factory:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.stereotype.Component;

@Component
public class CustomMsgFactory {

    public ISOMsg create(String mti) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(mti);
        
        // Set common fields for all messages
        msg.set(7, getCurrentDateTime());
        msg.set(11, generateSystemTraceAuditNumber());
        msg.set(41, "12345678");  // Card Acceptor Terminal ID
        
        return msg;
    }

    private String getCurrentDateTime() {
        // Implementation to get current date and time in MMDDhhmmss format
        return "0701104321";  // Example static value
    }

    private String generateSystemTraceAuditNumber() {
        // Implementation to generate a unique STAN
        return "123456";  // Example static value
    }
}
```

Now, you can use this factory to create messages:

```java
@Service
public class TransactionService {

    private final CustomMsgFactory msgFactory;

    @Autowired
    public TransactionService(CustomMsgFactory msgFactory) {
        this.msgFactory = msgFactory;
    }

    public ISOMsg createPurchaseRequest(String pan, String amount) throws ISOException {
        ISOMsg msg = msgFactory.create("0200");
        msg.set(2, pan);
        msg.set(3, "000000");  // Processing Code for Purchase
        msg.set(4, amount);
        msg.set(49, "840");  // Transaction Currency Code (USD)
        return msg;
    }
}
```

## 2.3 Handling Different Message Types

In a real-world scenario, you'll need to handle different types of messages (e.g., authorization, financial, reversal). Here's an example of how you might structure this:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageCreationService {

    private final CustomMsgFactory msgFactory;

    @Autowired
    public MessageCreationService(CustomMsgFactory msgFactory) {
        this.msgFactory = msgFactory;
    }

    public ISOMsg createAuthorizationRequest(String pan, String amount) throws ISOException {
        ISOMsg msg = msgFactory.create("0100");
        msg.set(2, pan);
        msg.set(3, "000000");  // Processing Code for Authorization
        msg.set(4, amount);
        msg.set(49, "840");  // Transaction Currency Code (USD)
        return msg;
    }

    public ISOMsg createFinancialRequest(String pan, String amount) throws ISOException {
        ISOMsg msg = msgFactory.create("0200");
        msg.set(2, pan);
        msg.set(3, "000000");  // Processing Code for Financial
        msg.set(4, amount);
        msg.set(49, "840");  // Transaction Currency Code (USD)
        return msg;
    }

    public ISOMsg createReversalRequest(String pan, String amount, String originalData) throws ISOException {
        ISOMsg msg = msgFactory.create("0400");
        msg.set(2, pan);
        msg.set(3, "000000");  // Processing Code for Reversal
        msg.set(4, amount);
        msg.set(49, "840");  // Transaction Currency Code (USD)
        msg.set(90, originalData);  // Original Data Elements
        return msg;
    }
}
```

## 2.4 Testing Message Creation

It's crucial to test your message creation logic to ensure correctness. Here's an example of how you might write a test using JUnit 5 and Mockito:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class MessageCreationServiceTest {

    @Mock
    private CustomMsgFactory msgFactory;

    private MessageCreationService messageCreationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        messageCreationService = new MessageCreationService(msgFactory);
    }

    @Test
    void testCreateAuthorizationRequest() throws ISOException {
        // Arrange
        ISOMsg mockMsg = new ISOMsg();
        mockMsg.setMTI("0100");
        when(msgFactory.create("0100")).thenReturn(mockMsg);

        // Act
        ISOMsg result = messageCreationService.createAuthorizationRequest("4111111111111111", "000000010000");

        // Assert
        assertEquals("0100", result.getMTI());
        assertEquals("4111111111111111", result.getString(2));
        assertEquals("000000", result.getString(3));
        assertEquals("000000010000", result.getString(4));
        assertEquals("840", result.getString(49));
    }
}
```

This test verifies that the `createAuthorizationRequest` method correctly sets the MTI and required fields for an authorization request.

In conclusion, creating ISO-8583 messages using jPOS involves using the `ISOMsg` class, potentially implementing a message factory of your own, and creating service classes to handle different types of messages. By following these practices and thoroughly testing your implementation, you can ensure reliable ISO-8583 message creation in your jPOS client application.


# 3. Channel Management for jPOS Client

Channel management is a crucial aspect of implementing a jPOS client. It involves setting up and managing the communication channels between the client and the server. In this section, we'll focus on two main components: a client channel and SSL/TLS on that channel.

## 3.1 Client Channel (org.jpos.iso.channel.ASCIIChannel)

jPOS 2.1.7 has no `ISOClient` class. A client connects to an ISO-8583 server through an `ISOChannel`, such as `ASCIIChannel`, which provides methods for connecting, sending messages, and receiving responses.

### 3.1.1 Basic Implementation

Here's a basic example of how to set up a client channel:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;

public class BasicISOClientExample {
    public static void main(String[] args) throws Exception {
        // Create a new client channel
        ASCIIChannel client = new ASCIIChannel("localhost", 7000, new ISO87APackager());
        client.setTimeout(30000); // 30 seconds timeout

        // Connect to the server
        client.connect();

        // Create a sample ISO message
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0800");
        msg.set(3, "000000");
        msg.set(11, "000001");
        msg.set(41, "12345678");
        msg.set(70, "301");

        // Send the message and receive the response
        client.send(msg);
        ISOMsg response = client.receive();

        // Process the response
        if (response != null) {
            System.out.println("Received response: " + response.getMTI());
        } else {
            System.out.println("No response received");
        }

        // Disconnect
        client.disconnect();
    }
}
```

### 3.1.2 Spring Configuration

In a Spring-based application, you can configure the client channel as a bean:

```java
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ISOClientConfig {

    @Bean
    public ASCIIChannel isoClient() {
        return new ASCIIChannel("localhost", 7000, new ISO87APackager());
    }
}
```

### 3.1.3 Advanced Usage with Connection Pool

For better performance and resource management, you can use a connection pool of client channels. Here's an example using Apache Commons Pool:

```java
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.channel.ASCIIChannel;
import org.jpos.iso.packager.ISO87APackager;

public class ISOClientPool {
    private final GenericObjectPool<ISOChannel> pool;

    public ISOClientPool(String host, int port, int maxTotal) {
        pool = new GenericObjectPool<>(new ISOClientFactory(host, port));
        pool.setMaxTotal(maxTotal);
    }

    public ISOChannel borrowClient() throws Exception {
        return pool.borrowObject();
    }

    public void returnClient(ISOChannel client) {
        pool.returnObject(client);
    }

    private static class ISOClientFactory extends BasePooledObjectFactory<ISOChannel> {
        private final String host;
        private final int port;

        public ISOClientFactory(String host, int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public ISOChannel create() throws Exception {
            ISOChannel client = new ASCIIChannel(host, port, new ISO87APackager());
            client.connect();
            return client;
        }

        @Override
        public PooledObject<ISOChannel> wrap(ISOChannel client) {
            return new DefaultPooledObject<>(client);
        }

        @Override
        public void destroyObject(PooledObject<ISOChannel> p) throws Exception {
            p.getObject().disconnect();
        }
    }
}
```

Usage of the `ISOClientPool`:

```java
ISOClientPool clientPool = new ISOClientPool("localhost", 7000, 10);

ISOChannel client = clientPool.borrowClient();
try {
    // Use the client
    client.send(msg);
    ISOMsg response = client.receive();
    // Process response
} finally {
    clientPool.returnClient(client);
}
```

## 3.2 SSL/TLS on a Channel (org.jpos.iso.channel.NACChannel)

For secure communications, a jPOS channel such as `NACChannel` runs over SSL/TLS when you give it an SSL socket factory, `SunJSSESocketFactory`. Here's how to set up and use a channel over SSL/TLS:

### 3.2.1 SSL Socket Factory Configuration

First, you need to set up your SSL certificates and keystore. Then, you can create an `NACChannel` with an SSL socket factory like this:

```java
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;

public class SSLChannelExample {
    public static void main(String[] args) throws Exception {
        NACChannel channel = new NACChannel("localhost", 7000, new ISO87APackager(), null);
        
        // Set SSL properties
        SunJSSESocketFactory sslFactory = new SunJSSESocketFactory();
        sslFactory.setKeyStore("path/to/keystore.jks");
        sslFactory.setPassword("keystorePassword");
        sslFactory.setKeyPassword("keyPassword");
        sslFactory.setServerAuthNeeded(true); // trust the server via the keystore's certificates
        channel.setSocketFactory(sslFactory);
        
        // Connect
        channel.connect();
        
        // Use the channel for communication
        // ...
        
        // Disconnect
        channel.disconnect();
    }
}
```

### 3.2.2 Using the SSL-Configured Channel as the Client

jPOS 2.1.7 has no `ISOClient`, so the SSL-configured `NACChannel` is itself the client:

```java
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;

public class SecureISOClientExample {
    public static void main(String[] args) throws Exception {
        NACChannel channel = new NACChannel("localhost", 7000, new ISO87APackager(), null);
        SunJSSESocketFactory sslFactory = new SunJSSESocketFactory();
        sslFactory.setKeyStore("path/to/keystore.jks");
        sslFactory.setPassword("keystorePassword");
        sslFactory.setKeyPassword("keyPassword");
        sslFactory.setServerAuthNeeded(true); // trust the server via the keystore's certificates
        channel.setSocketFactory(sslFactory);

        // Connect and use the channel as the client
        channel.connect();

        // Send and receive messages
        // ...

        channel.disconnect();
    }
}
```

### 3.2.3 Spring Configuration for the SSL-Configured Channel

In a Spring-based application, you can configure the channel over SSL/TLS as a bean:

```java
import org.jpos.iso.SunJSSESocketFactory;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.ISO87APackager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SSLChannelConfig {

    @Bean
    public SunJSSESocketFactory sslSocketFactory() {
        SunJSSESocketFactory sslFactory = new SunJSSESocketFactory();
        sslFactory.setKeyStore("path/to/keystore.jks");
        sslFactory.setPassword("keystorePassword");
        sslFactory.setKeyPassword("keyPassword");
        sslFactory.setServerAuthNeeded(true); // trust the server via the keystore's certificates
        return sslFactory;
    }

    @Bean
    public NACChannel nacChannel(SunJSSESocketFactory sslSocketFactory) {
        NACChannel channel = new NACChannel("localhost", 7000, new ISO87APackager(), null);
        channel.setSocketFactory(sslSocketFactory);
        return channel;
    }
}
```

This covers the essential aspects of Channel Management for a jPOS client, including the client channel that takes the place of `ISOClient` and the use of channels over SSL/TLS for secure communication. These components form the foundation for establishing reliable and secure connections between your client application and ISO-8583 servers.

# 4. Transaction Sending

Transaction sending in jPOS is primarily handled by two key components: QMUX and MUX. These components provide a high-level abstraction for sending and receiving ISO-8583 messages.

## 4.1 QMUX (org.jpos.q2.iso.QMUX)

QMUX is a Q2 adaptor that implements the MUX interface. It's designed to multiplex messages over a single connection or multiple connections.

### 4.1.1 Configuration

To set up QMUX in your jPOS client, you need to add a Q2 deploy descriptor, such as `deploy/20_mux.xml`:

```xml
<mux class="org.jpos.q2.iso.QMUX" logger="Q2" name="client-mux">
    <in>receive</in>
    <out>send</out>
    <unhandled>unhandled</unhandled>
    <ready>client-channel.ready</ready>
</mux>
```

### 4.1.2 Usage

Here's an example of how to use QMUX to send a transaction:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar;
import org.springframework.stereotype.Service;

@Service
public class TransactionSender {

    private final MUX mux;

    public TransactionSender() throws NameRegistrar.NotFoundException {
        this.mux = NameRegistrar.get("mux.client-mux");
    }

    public ISOMsg sendTransaction(ISOMsg message) throws Exception {
        long timeout = 30000; // 30 seconds timeout
        return mux.request(message, timeout);
    }
}
```

## 4.2 MUX (org.jpos.iso.MUX)

MUX is an interface that defines methods for sending ISO messages and receiving responses. QMUX is an implementation of this interface.

### 4.2.1 Key Methods

- `request(ISOMsg m, long timeout)`: Sends a message and waits for a response.
- `request(ISOMsg m, long timeout, ISOResponseListener rl, Object handBack)`: Sends a message and registers a listener for the response; `handBack` is passed back to the listener.
- `send(ISOMsg m)`: Sends a message without expecting a response.

### 4.2.2 Example: Asynchronous Transaction Sending

Here's an example of how to send a transaction asynchronously using MUX:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.util.NameRegistrar;
import org.springframework.stereotype.Service;

@Service
public class AsyncTransactionSender {

    private final MUX mux;

    public AsyncTransactionSender() throws NameRegistrar.NotFoundException {
        this.mux = NameRegistrar.get("mux.client-mux");
    }

    public void sendTransactionAsync(ISOMsg message) throws Exception {
        long timeout = 30000; // 30 seconds timeout
        mux.request(message, timeout, new ISOResponseListener() {
            @Override
            public void expired(Object id) {
                System.out.println("Request " + id + " expired");
            }

            @Override
            public void responseReceived(ISOMsg response, Object id) {
                System.out.println("Received response for " + id + ": " + response);
            }
        }, message.getString(11));
    }
}
```

## 4.3 Creating ISO-8583 Messages

Before sending transactions, you need to create ISO-8583 messages. Here's an example of creating a simple authorization request:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class MessageCreator {

    public ISOMsg createAuthorizationRequest(String pan, String amount) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setMTI("0100");
        message.set(2, pan);
        message.set(3, "000000");
        message.set(4, amount);
        message.set(7, DateTimeFormatter.ofPattern("MMddHHmmss").withZone(ZoneOffset.UTC).format(Instant.now()));
        message.set(11, String.format("%06d", (int) (Math.random() * 1000000)));
        message.set(41, "12345678");
        message.set(42, "MERCHANT_ID_123");
        return message;
    }
}
```

## 4.4 Putting It All Together

Now, let's create a service that combines message creation and sending:

```java
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    private final TransactionSender sender;
    private final MessageCreator creator;

    @Autowired
    public TransactionService(TransactionSender sender, MessageCreator creator) {
        this.sender = sender;
        this.creator = creator;
    }

    public ISOMsg performAuthorization(String pan, String amount) throws Exception {
        ISOMsg request = creator.createAuthorizationRequest(pan, amount);
        return sender.sendTransaction(request);
    }
}
```

## 4.5 Testing

Here's a JUnit test for the `TransactionService`:

```java
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionSender sender;

    @Mock
    private MessageCreator creator;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(sender, creator);
    }

    @Test
    void testPerformAuthorization() throws Exception {
        // Arrange
        ISOMsg request = new ISOMsg();
        request.setMTI("0100");
        
        ISOMsg response = new ISOMsg();
        response.setMTI("0110");
        response.set(39, "00");

        when(creator.createAuthorizationRequest("1234567890123456", "100000")).thenReturn(request);
        when(sender.sendTransaction(any(ISOMsg.class))).thenReturn(response);

        // Act
        ISOMsg result = service.performAuthorization("1234567890123456", "100000");

        // Assert
        assertEquals("0110", result.getMTI());
        assertEquals("00", result.getString(39));
    }
}
```

This covers the essential aspects of transaction sending in a jPOS client implementation. The combination of QMUX, MUX, and proper message creation allows for efficient and reliable ISO-8583 message transmission.

# 5. Visa and Mastercard Specific Requests

When implementing a jPOS client for processing Visa and Mastercard transactions, it's crucial to create custom services that handle the specific requirements of each card network. These services will encapsulate the logic for creating and sending ISO-8583 messages tailored to Visa and Mastercard specifications.

Let's create two services: one for Visa and one for Mastercard. Each service will handle common transaction types such as purchases, cash advances, refunds, and balance inquiries.

## 5.1 Visa Transaction Service

First, let's create an interface for our Visa transaction service:

```java
package com.example.jpos.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public interface VisaTransactionService {
    ISOMsg createPurchaseRequest(String pan, String amount, String merchantId) throws ISOException;
    ISOMsg createCashAdvanceRequest(String pan, String amount, String merchantId) throws ISOException;
    ISOMsg createRefundRequest(String pan, String amount, String merchantId, String originalRrn) throws ISOException;
    ISOMsg createBalanceInquiryRequest(String pan, String merchantId) throws ISOException;
}
```

Now, let's implement this interface:

```java
package com.example.jpos.service.impl;

import com.example.jpos.service.VisaTransactionService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class VisaTransactionServiceImpl implements VisaTransactionService {

    private static final String VISA_MTI_REQUEST = "0100";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");

    @Override
    public ISOMsg createPurchaseRequest(String pan, String amount, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(VISA_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "000000"); // Processing code for purchase
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "5999"); // Merchant category code (example)
        msg.set(32, "123456"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        return msg;
    }

    @Override
    public ISOMsg createCashAdvanceRequest(String pan, String amount, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(VISA_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "010000"); // Processing code for cash advance
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "6011"); // Merchant category code for ATM
        msg.set(32, "123456"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        return msg;
    }

    @Override
    public ISOMsg createRefundRequest(String pan, String amount, String merchantId, String originalRrn) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(VISA_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "200000"); // Processing code for refund
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "5999"); // Merchant category code (example)
        msg.set(32, "123456"); // Acquiring Institution ID Code
        msg.set(37, originalRrn); // Retrieval Reference Number of the original transaction
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        return msg;
    }

    @Override
    public ISOMsg createBalanceInquiryRequest(String pan, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(VISA_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "300000"); // Processing code for balance inquiry
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "6011"); // Merchant category code for ATM
        msg.set(32, "123456"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        return msg;
    }

    private String generateStan() {
        // Generate a unique 6-digit STAN (System Trace Audit Number)
        return String.format("%06d", (int) (Math.random() * 999999));
    }
}
```

## 5.2 Mastercard Transaction Service

Now, let's create a similar service for Mastercard transactions:

```java
package com.example.jpos.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

public interface MastercardTransactionService {
    ISOMsg createPurchaseRequest(String pan, String amount, String merchantId) throws ISOException;
    ISOMsg createCashAdvanceRequest(String pan, String amount, String merchantId) throws ISOException;
    ISOMsg createRefundRequest(String pan, String amount, String merchantId, String originalRrn) throws ISOException;
    ISOMsg createBalanceInquiryRequest(String pan, String merchantId) throws ISOException;
}
```

And its implementation:

```java
package com.example.jpos.service.impl;

import com.example.jpos.service.MastercardTransactionService;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MastercardTransactionServiceImpl implements MastercardTransactionService {

    private static final String MASTERCARD_MTI_REQUEST = "0100";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMddHHmmss");

    @Override
    public ISOMsg createPurchaseRequest(String pan, String amount, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(MASTERCARD_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "000000"); // Processing code for purchase
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "5999"); // Merchant category code (example)
        msg.set(32, "654321"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        msg.set(61, "0000000000000001"); // Point-of-Service Data (example)
        return msg;
    }

    @Override
    public ISOMsg createCashAdvanceRequest(String pan, String amount, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(MASTERCARD_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "010000"); // Processing code for cash advance
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "6011"); // Merchant category code for ATM
        msg.set(32, "654321"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        msg.set(61, "0000000000000002"); // Point-of-Service Data (example)
        return msg;
    }

    @Override
    public ISOMsg createRefundRequest(String pan, String amount, String merchantId, String originalRrn) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(MASTERCARD_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "200000"); // Processing code for refund
        msg.set(4, amount);
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "5999"); // Merchant category code (example)
        msg.set(32, "654321"); // Acquiring Institution ID Code
        msg.set(37, originalRrn); // Retrieval Reference Number of the original transaction
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        msg.set(61, "0000000000000003"); // Point-of-Service Data (example)
        return msg;
    }

    @Override
    public ISOMsg createBalanceInquiryRequest(String pan, String merchantId) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(MASTERCARD_MTI_REQUEST);
        msg.set(2, pan);
        msg.set(3, "300000"); // Processing code for balance inquiry
        msg.set(7, LocalDateTime.now().format(DATE_TIME_FORMATTER));
        msg.set(11, generateStan());
        msg.set(18, "6011"); // Merchant category code for ATM
        msg.set(32, "654321"); // Acquiring Institution ID Code
        msg.set(41, merchantId);
        msg.set(49, "840"); // Currency code (USD)
        msg.set(61, "0000000000000004"); // Point-of-Service Data (example)
        return msg;
    }

    private String generateStan() {
        // Generate a unique 6-digit STAN (System Trace Audit Number)
        return String.format("%06d", (int) (Math.random() * 999999));
    }
}
```

## 5.3 Using the Transaction Services

To use these services in your application, you can inject them into your controllers or other services. Here's an example of how you might use them in a transaction processing service:

```java
package com.example.jpos.service;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessingService {

    @Autowired
    private VisaTransactionService visaService;

    @Autowired
    private MastercardTransactionService mastercardService;

    @Autowired
    private MUX mux;

    public ISOMsg processPurchase(String pan, String amount, String merchantId, String cardNetwork) throws ISOException {
        ISOMsg request;
        if ("visa".equalsIgnoreCase(cardNetwork)) {
            request = visaService.createPurchaseRequest(pan, amount, merchantId);
        } else if ("mastercard".equalsIgnoreCase(cardNetwork)) {
            request = mastercardService.createPurchaseRequest(pan, amount, merchantId);
        } else {
            throw new IllegalArgumentException("Unsupported card network: " + cardNetwork);
        }

        return mux.request(request, 30000); // 30 seconds timeout
    }

    // Similar methods for cash advance, refund, and balance inquiry
}
```

This implementation provides a clean separation of concerns, making it easy to handle the specific requirements of each card network while providing a unified interface for transaction processing.

# 6. Data Elements Handling in jPOS Client

Data elements are the building blocks of ISO-8583 messages. In jPOS, we primarily use the `ISOMsg` and `ISOField` classes to handle these data elements. Let's dive into how to work with these classes in a client implementation.

## 6.1 ISOMsg

`ISOMsg` represents an ISO-8583 message. It's a container for ISO fields and provides methods to set, get, and manipulate these fields.

### 6.1.1 Creating an ISOMsg

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOMessageCreator {

    public ISOMsg createBasicMessage() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");  // Set Message Type Indicator
        msg.set(2, "4111111111111111");  // Primary Account Number
        msg.set(3, "000000");  // Processing Code
        msg.set(4, "000000010000");  // Transaction Amount
        msg.set(7, "0701104321");  // Transmission Date and Time
        msg.set(11, "123456");  // System Trace Audit Number
        msg.set(41, "12345678");  // Card Acceptor Terminal ID
        msg.set(49, "840");  // Transaction Currency Code (USD)
        return msg;
    }
}
```

### 6.1.2 Reading from an ISOMsg

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOMessageReader {

    public void readMessage(ISOMsg msg) throws ISOException {
        System.out.println("MTI: " + msg.getMTI());
        System.out.println("PAN: " + msg.getString(2));
        System.out.println("Processing Code: " + msg.getString(3));
        System.out.println("Amount: " + msg.getString(4));
        
        // Check if a field exists before reading
        if (msg.hasField(55)) {
            System.out.println("EMV Data: " + msg.getString(55));
        }
    }
}
```

### 6.1.3 Modifying an ISOMsg

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOMessageModifier {

    public void modifyMessage(ISOMsg msg) throws ISOException {
        // Change the MTI
        msg.setMTI("0210");
        
        // Update the amount
        msg.set(4, "000000020000");
        
        // Add a new field
        msg.set(39, "00");  // Response code (approved)
        
        // Remove a field
        msg.unset(55);  // Remove EMV data if present
    }
}
```

## 6.2 ISOField

`ISOField` represents a single field in an ISO-8583 message. While you often work with fields through `ISOMsg`, understanding `ISOField` can be helpful for more complex scenarios.

### 6.2.1 Creating and Using ISOField

```java
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ISOFieldHandler {

    public void demonstrateISOField() throws ISOException {
        ISOField panField = new ISOField(2, "4111111111111111");
        
        ISOMsg msg = new ISOMsg();
        msg.set(panField);
        
        // You can also create fields this way
        msg.set(new ISOField(3, "000000"));
        
        // Reading a field
        ISOField retrievedField = (ISOField) msg.getComponent(2);
        System.out.println("PAN: " + retrievedField.getValue());
    }
}
```

## 6.3 Handling Complex Data Elements

Some ISO-8583 fields contain structured data. jPOS provides ways to handle these complex fields.

### 6.3.1 Subfields

For fields with subfields (like field 48 or 62), you can use nested `ISOMsg` objects.

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;

public class ComplexFieldHandler {

    public void handleSubfields() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        
        // Create a submessage for field 48
        ISOMsg subfield48 = new ISOMsg(48);
        subfield48.set(1, "001");  // Subfield 1 of field 48
        subfield48.set(2, "12345");  // Subfield 2 of field 48
        
        // Set the submessage in the main message
        msg.set(subfield48);
        
        // Reading subfields
        ISOMsg retrievedSubfield = (ISOMsg) msg.getComponent(48);
        System.out.println("Subfield 48.1: " + retrievedSubfield.getString(1));
        System.out.println("Subfield 48.2: " + retrievedSubfield.getString(2));
    }
}
```

### 6.3.2 Binary Fields

For binary fields (like field 55 for EMV data), you can use byte arrays.

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOBinaryField;

public class BinaryFieldHandler {

    public void handleBinaryField() throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        
        // Sample EMV data (this would typically come from the card or terminal)
        byte[] emvData = new byte[] {(byte)0x9F, 0x26, 0x08, (byte)0xA1, (byte)0xB2, (byte)0xC3, (byte)0xD4, (byte)0xE5, (byte)0xF6, (byte)0xA7, (byte)0xB8};
        
        // Set binary field
        msg.set(new ISOBinaryField(55, emvData));
        
        // Reading binary field
        byte[] retrievedEmvData = (byte[]) msg.getValue(55);
        System.out.println("EMV Data Length: " + retrievedEmvData.length);
    }
}
```

## 6.4 Best Practices

1. **Validation**: Always validate data before setting it in an `ISOMsg` to ensure it meets the required format and length.

2. **Error Handling**: Use try-catch blocks to handle `ISOException` which can be thrown when working with ISO messages.

3. **Logging**: Implement comprehensive logging for all message creation, modification, and reading operations.

4. **Security**: Be cautious with sensitive data (like PANs). Implement proper encryption and masking techniques.

5. **Reusability**: Create utility classes for common operations on ISO messages to promote code reuse.

Here's an example of a utility class incorporating some of these best practices:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ISOMessageUtil {
    private static final Logger logger = LoggerFactory.getLogger(ISOMessageUtil.class);

    public ISOMsg createFinancialRequestMessage(String pan, String amount, String terminalId) {
        try {
            ISOMsg msg = new ISOMsg();
            msg.setMTI("0200");
            msg.set(2, maskPAN(pan));
            msg.set(3, "000000");  // Assuming purchase
            msg.set(4, padAmount(amount));
            msg.set(41, terminalId);
            
            logger.info("Created financial request message: {}", msg);
            return msg;
        } catch (ISOException e) {
            logger.error("Error creating financial request message", e);
            throw new RuntimeException("Failed to create ISO message", e);
        }
    }

    private String maskPAN(String pan) {
        if (pan.length() < 13) return pan;
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    private String padAmount(String amount) {
        return String.format("%012d", Long.parseLong(amount));
    }
}
```

This utility class demonstrates creating a financial request message with proper error handling, logging, and data masking for sensitive information.

By following these practices and understanding how to work with `ISOMsg` and `ISOField`, you can effectively handle data elements in your jPOS client implementation.

# 7. Security and Encryption in jPOS Client

Security and encryption are paramount in financial transactions to protect sensitive data such as PINs, card numbers, and other confidential information. jPOS provides robust security features through its `SMAdapter` (Security Module Adapter) interface and the `JCESecurityModule` implementation.

## 7.1 Overview of jPOS Security Features

jPOS offers several security features:
- PIN encryption and verification
- Message Authentication Code (MAC) generation and verification
- Key management (including key exchange and derivation)
- Secure key storage

## 7.2 Implementing Security in a jPOS Client

Let's implement a secure client that can encrypt PINs and generate MACs for ISO-8583 messages.

First, we'll set up the necessary dependencies in our `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.jpos</groupId>
        <artifactId>jpos</artifactId>
        <version>2.1.7</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <version>2.6.3</version>
    </dependency>
    <!-- Other dependencies... -->
</dependencies>
```

Now, let's create a `SecurityService` that will handle our security operations:

```java
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureKeyStore;
import org.jpos.security.jceadapter.JCESecurityModule;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;

@Service
public class SecurityService {

    private SMAdapter smAdapter;
    private SecureKeyStore keyStore;

    @PostConstruct
    public void init() throws ConfigurationException {
        // Initialize the JCESecurityModule
        this.smAdapter = new JCESecurityModule();
        Configuration cfg = new org.jpos.core.SimpleConfiguration();
        cfg.put("provider", "SunJCE");
        smAdapter.setConfiguration(cfg);

        // Initialize a secure key store (you'd typically load this from a secure location)
        this.keyStore = new SecureKeyStore();
        // Load keys into the keyStore...
    }

    public String encryptPIN(String pin, String pan) throws ISOException {
        return smAdapter.encryptPIN(pin, pan);
    }

    public byte[] generateMAC(ISOMsg isoMsg) throws ISOException {
        byte[] macKey = keyStore.getKey("MAC_KEY");
        return smAdapter.generateMAC(isoMsg.pack(), macKey);
    }

    public boolean verifyMAC(ISOMsg isoMsg, byte[] receivedMAC) throws ISOException {
        byte[] macKey = keyStore.getKey("MAC_KEY");
        byte[] calculatedMAC = smAdapter.generateMAC(isoMsg.pack(), macKey);
        return java.util.Arrays.equals(calculatedMAC, receivedMAC);
    }

    // Other security-related methods...
}
```

Now, let's create a `SecureISOClient` that uses this `SecurityService`:

```java
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecureISOClient {

    @Autowired
    private SecurityService securityService;

    private NACChannel channel;

    public SecureISOClient() throws ISOException {
        GenericPackager packager = new GenericPackager("packager/iso87ascii.xml");
        this.channel = new NACChannel("localhost", 8000, packager);
    }

    public ISOMsg sendSecureMessage(ISOMsg msg) throws ISOException {
        // Encrypt sensitive fields
        String pan = msg.getString(2);
        String pin = msg.getString(52);
        String encryptedPIN = securityService.encryptPIN(pin, pan);
        msg.set(52, encryptedPIN);

        // Generate and set MAC
        byte[] mac = securityService.generateMAC(msg);
        msg.set(64, mac);

        // Send the message
        channel.connect();
        channel.send(msg);
        ISOMsg response = channel.receive();
        channel.disconnect();

        // Verify MAC of the response
        byte[] responseMAC = response.getBytes(64);
        if (!securityService.verifyMAC(response, responseMAC)) {
            throw new SecurityException("Invalid MAC in response");
        }

        return response;
    }
}
```

This `SecureISOClient` encrypts the PIN, generates a MAC for outgoing messages, and verifies the MAC of incoming messages.

## 7.3 Testing the Secure Client

Let's create a test for our `SecureISOClient`:

```java
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecureISOClientTest {

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private SecureISOClient secureISOClient;

    private ISOMsg testMsg;

    @BeforeEach
    void setUp() throws Exception {
        testMsg = new ISOMsg();
        testMsg.setMTI("0200");
        testMsg.set(2, "4111111111111111");
        testMsg.set(3, "000000");
        testMsg.set(4, "000000012345");
        testMsg.set(52, "1234");
    }

    @Test
    void testSendSecureMessage() throws Exception {
        // Mock security service methods
        when(securityService.encryptPIN(anyString(), anyString())).thenReturn("ENCRYPTED_PIN");
        when(securityService.generateMAC(any(ISOMsg.class))).thenReturn("MAC".getBytes());
        when(securityService.verifyMAC(any(ISOMsg.class), any(byte[].class))).thenReturn(true);

        // Mock channel behavior (you might need to use PowerMockito for this)
        // ...

        ISOMsg response = secureISOClient.sendSecureMessage(testMsg);

        assertThat(response).isNotNull();
        verify(securityService).encryptPIN("1234", "4111111111111111");
        verify(securityService).generateMAC(any(ISOMsg.class));
        verify(securityService).verifyMAC(any(ISOMsg.class), any(byte[].class));
    }
}
```

This test verifies that our `SecureISOClient` correctly interacts with the `SecurityService` to encrypt the PIN, generate a MAC, and verify the response MAC.

## 7.4 Best Practices for Security in jPOS

1. **Key Management**: Implement a robust key management system. Regularly rotate keys and securely store them.

2. **Use Strong Encryption**: Ensure you're using strong encryption algorithms for PIN encryption and MAC generation.

3. **Secure Communication**: Always use secure channels (like SSL/TLS) for network communication.

4. **Audit Logging**: Implement comprehensive audit logging for all security-related operations.

5. **Input Validation**: Validate all input data to prevent injection attacks.

6. **Error Handling**: Implement proper error handling without revealing sensitive information in error messages.

7. **Regular Security Audits**: Conduct regular security audits and penetration testing of your jPOS implementation.

By implementing these security measures, you can significantly enhance the security of your jPOS client implementation, protecting sensitive financial data and ensuring the integrity of your transactions.

# 8. Logging and Debugging in jPOS Client

Logging and debugging are essential components of any robust jPOS client implementation. They help developers track the flow of transactions, identify issues, and maintain the system effectively. jPOS provides powerful logging capabilities through its built-in logging framework.

## 8.1 jPOS Logging Framework

jPOS uses its own logging framework, which is flexible and can be integrated with other logging systems like Log4j or java.util.logging. The main classes involved in jPOS logging are:

- `org.jpos.util.Logger`
- `org.jpos.util.LogEvent`
- `org.jpos.util.LogListener`

### 8.1.1 Logger

The `Logger` class is the central component of the jPOS logging system. It manages a collection of `LogListener` instances and dispatches `LogEvent` objects to them.

Here's an example of how to create and use a Logger:

```java
import org.jpos.util.Logger;
import org.jpos.util.LogEvent;

public class ClientLogger {
    private static final Logger logger = new Logger();
    
    public static void logMessage(String message) {
        LogEvent ev = new LogEvent();
        ev.addMessage(message);
        logger.log(ev);
    }
}
```

### 8.1.2 LogEvent

`LogEvent` represents a loggable event in jPOS. It can contain multiple messages, tagged fields, and even nested events.

Here's an example of creating a more complex LogEvent:

```java
import org.jpos.util.LogEvent;

public class ComplexLogEventExample {
    public static LogEvent createComplexLogEvent() {
        LogEvent ev = new LogEvent("ClientTransaction");
        ev.addMessage("Processing transaction");
        ev.addMessage("Transaction ID: 12345");
        ev.addMessage("Amount: $100.00");
        
        // Adding a tagged field
        ev.tag("status", "approved");
        
        // Adding a nested event
        LogEvent nestedEvent = new LogEvent("CardDetails");
        nestedEvent.addMessage("Card type: Visa");
        nestedEvent.addMessage("Last 4 digits: 1234");
        ev.addEvent(nestedEvent);
        
        return ev;
    }
}
```

### 8.1.3 LogListener

`LogListener` is an interface that defines how log events are handled. jPOS provides several implementations, such as `SimpleLogListener` for console output and `RotateLogListener` for file-based logging with rotation.

Here's an example of adding a SimpleLogListener to our Logger:

```java
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

public class LoggerSetup {
    public static void configureLogger(Logger logger) {
        SimpleLogListener listener = new SimpleLogListener(System.out);
        logger.addListener(listener);
    }
}
```

## 8.2 Implementing Logging in jPOS Client

Now, let's implement logging in a jPOS client application using Spring Boot. We'll create a custom `LogListener` and configure it using Spring.

First, let's create a custom `LogListener`:

```java
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;
import org.slf4j.LoggerFactory;

public class SpringLogListener implements LogListener {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(SpringLogListener.class);

    @Override
    public LogEvent log(LogEvent ev) {
        StringBuilder sb = new StringBuilder();
        sb.append(ev.getTag()).append(": ");
        for (Object msg : ev.getPayLoad()) {
            sb.append(msg).append(" ");
        }
        log.info(sb.toString());
        return ev;
    }
}
```

Now, let's configure this listener in our Spring configuration:

```java
import org.jpos.util.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public Logger jposLogger() {
        Logger logger = new Logger();
        logger.addListener(new SpringLogListener());
        return logger;
    }
}
```

With this configuration in place, we can now inject and use the jPOS Logger in our client components:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.util.Logger;
import org.jpos.util.LogEvent;
import org.springframework.stereotype.Component;

@Component
public class TransactionLogger {
    private final Logger logger;

    public TransactionLogger(Logger logger) {
        this.logger = logger;
    }

    public void logTransaction(ISOMsg msg) {
        LogEvent ev = new LogEvent("Transaction");
        ev.addMessage("MTI: " + msg.getMTI());
        ev.addMessage("PAN: " + msg.getString(2));
        ev.addMessage("Amount: " + msg.getString(4));
        logger.log(ev);
    }
}
```

## 8.3 Debugging Techniques

When debugging jPOS applications, consider the following techniques:

1. **Use detailed logging**: Log important steps in your transaction processing, including input data, processing steps, and output data.

2. **Enable wire logging**: jPOS channels can log the raw data sent and received. This is crucial for diagnosing communication issues.

3. **Use jPOS profiler**: jPOS includes a profiler that can help identify performance bottlenecks.

4. **Implement health checks**: Create endpoints that report the status of your application components.

Here's an example of enabling wire logging for a channel:

```java
import org.jpos.iso.channel.NACChannel;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogListener;

public class ChannelConfig {
    public static NACChannel createDebugChannel(String host, int port) {
        NACChannel channel = new NACChannel(host, port, new ISO87APackager());
        
        Logger wireLogger = new Logger();
        wireLogger.addListener(new SimpleLogListener(System.out));
        
        channel.setLogger(wireLogger, "wire");
        channel.setPackager(new ISO87APackager());
        
        return channel;
    }
}
```

## 8.4 Testing Logging

To ensure our logging is working correctly, we can write a test:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.util.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class TransactionLoggerTest {

    @Autowired
    private TransactionLogger transactionLogger;

    @SpyBean
    private Logger jposLogger;

    @Test
    public void testLogTransaction() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(2, "4111111111111111");
        msg.set(4, "000000010000");

        transactionLogger.logTransaction(msg);

        verify(jposLogger, times(1)).log(any());
    }
}
```

This test verifies that when we log a transaction, the jPOS Logger is called once.

## Conclusion

Proper logging and debugging are crucial for maintaining and troubleshooting jPOS client applications. By leveraging jPOS's flexible logging framework and integrating it with Spring Boot, we can create a robust logging system that helps us track transactions, identify issues, and maintain our application effectively.

# 9. Error Handling in jPOS Client

Error handling is a crucial aspect of any robust application, especially in financial transaction processing where accuracy and reliability are paramount. In a jPOS client implementation, we need to handle various types of exceptions that may occur during message creation, sending, and processing.

## 9.1 Types of Exceptions

In a jPOS client application, you may encounter several types of exceptions:

1. `ISOException`: Thrown for ISO-8583 specific errors.
2. `IOException`: For network-related issues.
3. `NullPointerException`: For unexpected null values.
4. `TimeoutException`: When a transaction takes too long to complete.
5. Custom exceptions: For application-specific error scenarios.

## 9.2 Implementing Exception Handlers

We'll use Spring's `@ExceptionHandler` annotation to create centralized exception handling for our jPOS client. This approach allows us to define how different types of exceptions should be handled across the application.

Let's create an `ErrorHandler` class:

```java
import org.jpos.iso.ISOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ISOException.class)
    public ResponseEntity<ErrorResponse> handleISOException(ISOException ex) {
        ErrorResponse error = new ErrorResponse("ISO-8583 Error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        ErrorResponse error = new ErrorResponse("Network Error", "Failed to communicate with the server");
        return new ResponseEntity<>(error, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(TimeoutException ex) {
        ErrorResponse error = new ErrorResponse("Timeout Error", "The operation timed out");
        return new ResponseEntity<>(error, HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse("Internal Server Error", "An unexpected error occurred");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

And the `ErrorResponse` class:

```java
public class ErrorResponse {
    private String errorType;
    private String errorMessage;

    public ErrorResponse(String errorType, String errorMessage) {
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }

    // Getters and setters
}
```

## 9.3 Logging Exceptions

It's important to log exceptions for debugging and monitoring purposes. We can enhance our error handler to include logging:

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);

    @ExceptionHandler(ISOException.class)
    public ResponseEntity<ErrorResponse> handleISOException(ISOException ex) {
        logger.error("ISO-8583 Error: ", ex);
        ErrorResponse error = new ErrorResponse("ISO-8583 Error", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Other exception handlers...
}
```

## 9.4 Custom Exceptions

For application-specific error scenarios, it's often useful to create custom exceptions. Here's an example:

```java
public class TransactionDeclinedException extends RuntimeException {
    private String responseCode;

    public TransactionDeclinedException(String message, String responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
```

And its corresponding exception handler:

```java
@ExceptionHandler(TransactionDeclinedException.class)
public ResponseEntity<ErrorResponse> handleTransactionDeclinedException(TransactionDeclinedException ex) {
    logger.warn("Transaction declined: {}", ex.getMessage());
    ErrorResponse error = new ErrorResponse("Transaction Declined", 
        "Transaction was declined with response code: " + ex.getResponseCode());
    return new ResponseEntity<>(error, HttpStatus.PAYMENT_REQUIRED);
}
```

## 9.5 Testing Exception Handlers

To ensure our exception handlers work as expected, we should write unit tests. Here's an example using JUnit, Mockito, and Spring's testing support:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ErrorHandlerTest {

    @InjectMocks
    private ErrorHandler errorHandler;

    @Test
    public void testHandleISOException() {
        ISOException ex = new ISOException("Invalid message format");
        ResponseEntity<ErrorResponse> response = errorHandler.handleISOException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getErrorType()).isEqualTo("ISO-8583 Error");
        assertThat(response.getBody().getErrorMessage()).isEqualTo("Invalid message format");
    }

    @Test
    public void testHandleTransactionDeclinedException() {
        TransactionDeclinedException ex = new TransactionDeclinedException("Insufficient funds", "51");
        ResponseEntity<ErrorResponse> response = errorHandler.handleTransactionDeclinedException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(response.getBody().getErrorType()).isEqualTo("Transaction Declined");
        assertThat(response.getBody().getErrorMessage()).contains("51");
    }

    // More tests for other exception handlers...
}
```

## 9.6 Integration with jPOS Client

To integrate this error handling mechanism with your jPOS client, you need to ensure that your client code throws these exceptions when appropriate. For example:

```java
@Service
public class TransactionService {

    private final MUX mux;

    public TransactionService(MUX mux) {
        this.mux = mux;
    }

    public ISOMsg sendTransaction(ISOMsg request) throws ISOException, IOException {
        try {
            ISOMsg response = mux.request(request, 30000); // 30 seconds timeout
            if (response == null) {
                throw new TimeoutException("Transaction timed out");
            }
            
            String responseCode = response.getString(39);
            if (!"00".equals(responseCode)) {
                throw new TransactionDeclinedException("Transaction declined", responseCode);
            }
            
            return response;
        } catch (ISOException | IOException e) {
            throw e;
        }
    }
}
```

This service method will now throw appropriate exceptions that our `ErrorHandler` can catch and process.

## Conclusion

Implementing robust error handling in a jPOS client application involves:

1. Creating a centralized `ErrorHandler` using Spring's `@ControllerAdvice` and `@ExceptionHandler`.
2. Defining custom exceptions for application-specific scenarios.
3. Properly logging exceptions for debugging and monitoring.
4. Writing unit tests to ensure exception handlers work correctly.
5. Integrating the error handling mechanism with the jPOS client code.


# 10. Testing in jPOS Client Implementation

Testing is a crucial part of developing a robust jPOS client. It ensures that your ISO-8583 message creation, sending, and processing work correctly. We'll cover various aspects of testing, including unit tests, integration tests, and simulating server responses.

## 10.1 Unit Testing

Unit tests are essential for testing individual components of your jPOS client. Let's start with a simple example of testing a message creation service.

```java
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MessageCreationServiceTest {

    @Mock
    private ISOFactory isoFactory;

    @InjectMocks
    private MessageCreationService messageCreationService;

    @Test
    void testCreateAuthorizationRequest() throws Exception {
        // Arrange
        ISOMsg mockMsg = new ISOMsg();
        when(isoFactory.createMsg("0100")).thenReturn(mockMsg);

        // Act
        ISOMsg result = messageCreationService.createAuthorizationRequest("1234567890123456", 100.00);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMTI()).isEqualTo("0100");
        assertThat(result.getString(2)).isEqualTo("1234567890123456");
        assertThat(result.getString(4)).isEqualTo("000000010000");
    }
}
```

In this example, we're testing the `MessageCreationService` class, which is responsible for creating ISO-8583 messages. We use Mockito to mock the `ISOFactory` and AssertJ for fluent assertions.

## 10.2 Integration Testing

Integration tests ensure that different components of your jPOS client work together correctly. Here's an example of an integration test that sends a message and verifies the response:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransactionSenderIntegrationTest {

    @Autowired
    private MUX mux;

    @Autowired
    private MessageCreationService messageCreationService;

    @Test
    void testSendAuthorizationRequest() throws Exception {
        // Arrange
        ISOMsg request = messageCreationService.createAuthorizationRequest("1234567890123456", 100.00);

        // Act
        ISOMsg response = mux.request(request, 30000);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMTI()).isEqualTo("0110");
        assertThat(response.getString(39)).isEqualTo("00"); // Assuming '00' is approval code
    }
}
```

This integration test creates an authorization request, sends it through the MUX, and verifies the response. It uses the actual Spring context, so it's closer to real-world scenarios.

## 10.3 Simulating Server Responses

To test various scenarios without relying on an actual server, we can use Wiremock to simulate server responses. Here's an example of how to set up a Wiremock server to simulate ISO-8583 responses:

```java
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.NACChannel;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

public class ISOClientWiremockTest {

    private WireMockServer wireMockServer;
    private NACChannel channel;

    @BeforeEach
    void setup() throws Exception {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8888));
        wireMockServer.start();

        GenericPackager packager = new GenericPackager("packager/iso87ascii.xml");
        channel = new NACChannel("localhost", 8888, packager);
        channel.connect();
    }

    @AfterEach
    void teardown() {
        channel.disconnect();
        wireMockServer.stop();
    }

    @Test
    void testAuthorizationRequest() throws Exception {
        // Arrange
        String responseHex = "30313130F23C46D129E09180000000000000001000000000000010000301000000301000012345678901234560000";
        wireMockServer.stubFor(any(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(hexStringToByteArray(responseHex))));

        // Act
        ISOMsg request = new ISOMsg();
        request.setMTI("0100");
        request.set(2, "1234567890123456");
        request.set(4, "000000010000");
        
        ISOMsg response = channel.send(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getMTI()).isEqualTo("0110");
        assertThat(response.getString(39)).isEqualTo("00");
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
```

This test sets up a Wiremock server to simulate an ISO-8583 server. It stubs a response for any incoming request and then sends a request using the NACChannel. The test verifies that the response is correctly parsed and contains the expected data.

## 10.4 Testing with Q2

jPOS provides a Q2 container that can be useful for testing. Here's an example of how to set up a test using Q2:

```java
import org.jpos.q2.Q2;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class Q2Test {

    private static Q2 q2;

    @BeforeAll
    static void setup() throws Exception {
        q2 = new Q2();
        q2.start();
        Thread.sleep(2000); // Wait for Q2 to start
    }

    @AfterAll
    static void teardown() {
        q2.stop();
    }

    @Test
    void testQMUX() throws Exception {
        QMUX qmux = (QMUX) NameRegistrar.get("mux.mymux");
        assertThat(qmux).isNotNull();
        assertThat(qmux.isConnected()).isTrue();

        ISOMsg msg = new ISOMsg("0800");
        msg.set(11, "000001");
        msg.set(70, "301");

        ISOMsg response = qmux.request(msg, 30000);
        assertThat(response).isNotNull();
        assertThat(response.getMTI()).isEqualTo("0810");
        assertThat(response.getString(39)).isEqualTo("00");
    }
}
```

This test starts a Q2 instance, retrieves a QMUX from the NameRegistrar, and sends a network management request. It then verifies the response.

## 10.5 Performance Testing

For performance testing, you might want to simulate multiple concurrent requests. Here's a simple example using Java's ExecutorService:

```java
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class PerformanceTest {

    @Autowired
    private MUX mux;

    @Autowired
    private MessageCreationService messageCreationService;

    @Test
    void testConcurrentRequests() throws Exception {
        int numRequests = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<ISOMsg>> futures = new ArrayList<>();

        for (int i = 0; i < numRequests; i++) {
            futures.add(executor.submit(() -> {
                ISOMsg request = messageCreationService.createAuthorizationRequest("1234567890123456", 100.00);
                return mux.request(request, 30000);
            }));
        }

        for (Future<ISOMsg> future : futures) {
            ISOMsg response = future.get();
            assertThat(response).isNotNull();
            assertThat(response.getMTI()).isEqualTo("0110");
            assertThat(response.getString(39)).isEqualTo("00");
        }

        executor.shutdown();
    }
}
```

This test simulates 100 concurrent authorization requests and verifies that all responses are successful.

# 11. Connection Management

Connection management is crucial for maintaining efficient and reliable communication between the client and server in ISO-8583 message processing. In jPOS, connection management is primarily handled through the `ISOClientSocketFactory` interface and its implementations.

## 11.1 Overview of ISOClientSocketFactory

The `ISOClientSocketFactory` is an interface in jPOS that defines methods for creating and managing socket connections. It allows for customization of socket creation, including setting up SSL/TLS connections, connection pooling, and handling reconnection attempts.

## 11.2 Key Features of Connection Management

1. Connection Pooling: Reuse existing connections to reduce overhead.
2. Reconnection Strategies: Automatically attempt to reconnect in case of network failures.
3. SSL/TLS Support: Secure communication between client and server.
4. Timeout Handling: Manage connection and read timeouts.

## 11.3 Implementing Connection Management

Let's implement a custom `ISOClientSocketFactory` that includes connection pooling and reconnection strategies. We'll use Apache Commons Pool for connection pooling.

First, add the necessary dependencies to your `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
    <version>2.11.1</version>
</dependency>
```

Now, let's create our custom `PooledISOClientSocketFactory`:

```java
package com.example.isoclient.connection;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jpos.iso.ISOClientSocketFactory;
import org.jpos.iso.ISOException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

@Component
public class PooledISOClientSocketFactory implements ISOClientSocketFactory {

    private final GenericObjectPool<Socket> socketPool;

    public PooledISOClientSocketFactory() {
        GenericObjectPoolConfig<Socket> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setTestWhileIdle(true);
        config.setMinEvictableIdleTimeMillis(60000);
        config.setTimeBetweenEvictionRunsMillis(30000);

        socketPool = new GenericObjectPool<>(new SocketFactory(), config);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        try {
            return socketPool.borrowObject();
        } catch (Exception e) {
            throw new IOException("Failed to create socket", e);
        }
    }

    @Override
    public void setLogger(org.jpos.util.Logger logger, String realm) {
        // Implementation for logging if needed
    }

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        // Implementation for configuration if needed
    }

    private class SocketFactory extends BasePooledObjectFactory<Socket> {

        @Override
        public Socket create() throws Exception {
            // Replace with actual host and port
            return new Socket("iso.example.com", 8583);
        }

        @Override
        public PooledObject<Socket> wrap(Socket socket) {
            return new DefaultPooledObject<>(socket);
        }

        @Override
        public boolean validateObject(PooledObject<Socket> p) {
            Socket socket = p.getObject();
            return socket.isConnected() && !socket.isClosed();
        }

        @Override
        public void destroyObject(PooledObject<Socket> p) throws Exception {
            Socket socket = p.getObject();
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }

    // Method to close the pool when shutting down the application
    public void closePool() {
        socketPool.close();
    }
}
```

This implementation uses Apache Commons Pool to manage a pool of Socket objects. It includes features like:

1. Connection pooling with configurable pool size and idle connections.
2. Validation of connections before borrowing and after returning to the pool.
3. Periodic eviction of idle connections.

To use this `PooledISOClientSocketFactory` in your jPOS client configuration, you can create a Spring configuration class:

```java
package com.example.isoclient.config;

import com.example.isoclient.connection.PooledISOClientSocketFactory;
import org.jpos.iso.channel.NACChannel;
import org.jpos.q2.iso.QMUX;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ISOClientConfig {

    @Bean
    public NACChannel nacChannel(PooledISOClientSocketFactory socketFactory) {
        NACChannel channel = new NACChannel();
        channel.setSocketFactory(socketFactory);
        channel.setHost("iso.example.com");
        channel.setPort(8583);
        return channel;
    }

    @Bean
    public QMUX qmux(NACChannel channel) {
        QMUX qmux = new QMUX();
        qmux.setInQueue("tx-request");
        qmux.setOutQueue("tx-response");
        qmux.addChannel(channel);
        return qmux;
    }
}
```

This configuration sets up a `NACChannel` using our custom `PooledISOClientSocketFactory` and configures a `QMUX` for handling ISO messages.

## 11.4 Testing the Connection Management

To test our connection management implementation, we can create a JUnit test:

```java
package com.example.isoclient.connection;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.NACChannel;
import org.jpos.q2.iso.QMUX;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ConnectionManagementTest {

    @Autowired
    private QMUX qmux;

    @Autowired
    private NACChannel channel;

    @Autowired
    private PooledISOClientSocketFactory socketFactory;

    @BeforeEach
    public void setup() throws Exception {
        channel.connect();
    }

    @Test
    public void testConnectionPooling() throws Exception {
        // Simulate multiple requests
        for (int i = 0; i < 5; i++) {
            ISOMsg request = new ISOMsg();
            request.setMTI("0800");
            request.set(11, String.format("%06d", i));  // Stan

            ISOMsg response = qmux.request(request, 30000);  // 30 seconds timeout

            assertThat(response).isNotNull();
            assertThat(response.getMTI()).isEqualTo("0810");
            assertThat(response.getString(39)).isEqualTo("00");  // Assuming "00" is a successful response
        }
    }

    // Add more tests as needed
}
```

This test simulates multiple requests to verify that the connection pooling is working correctly. You may need to adjust the test based on your specific ISO-8583 message format and expected responses.

## 11.5 Considerations and Best Practices

1. Error Handling: Implement robust error handling for network issues, timeouts, and other potential problems.
2. Monitoring: Add logging and metrics to monitor the health and performance of your connection pool.
3. Configuration: Make pool settings configurable through external configuration files.
4. SSL/TLS: If using secure connections, implement proper SSL/TLS handling in the socket factory.
5. Reconnection Strategy: Implement a reconnection strategy for handling network failures.

By implementing effective connection management, you can improve the reliability and efficiency of your ISO-8583 client application. The pooled connection approach helps in reusing connections, reducing the overhead of creating new connections for each request, and managing the number of open connections to the server.