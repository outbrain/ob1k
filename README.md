#Ob1k - A modern RPC Framework

[![Build Status](https://travis-ci.org/outbrain/ob1k.svg?branch=master)](https://travis-ci.org/outbrain/ob1k)
[![Download](https://api.bintray.com/packages/harel-eran/Ob1k/com.outbrain.swinfra/images/download.svg)](https://bintray.com/harel-eran/Ob1k/com.outbrain.swinfra/_latestVersion)

##Overview and Motivation 
Ob1k is an asynchronous light-weight RPC framework for rapid development of async, high performance micro services.
You can start an Ob1k embedded server from your code and once started it will serve HTTP requests based on the endpoints you have configured. 
Unlike traditional servlet containers, Ob1k is based on [Netty](http://netty.io/) asynchronous event-driven model, and uses a fixed thread-per-code pool for serving.
The coordintation of asynchronous request is performed by using composable futures which enable
you to easily compose and combine asynchonous operations.      

##Anatomy 
Ob1k project consists of the following sub libraries:
 - **ob1k-concurrent**        - Introduces composable futures, an alternative implementation of futures in Java.
 - **ob1k-core**              - RPC framework Client and server infrastructure.
 - **ob1k-db**                - A composable futures based asynchronous MySQL client.
 - **ob1k-cache**             - A composable futures based asynchronous [Memcached client](https://code.google.com/p/spymemcached/), and guava cache wrapper.
 - **ob1k-cql**               - A composable futures based asynchronous Cassandra client.
 - **ob1k-security**          - Authentication and authorization for Ob1k.
 - **ob1k-consul**            - Ob1k based [Consul](https://consul.io/) API which simplifies registration and discovery for Ob1k services.
 - **ob1k-concurrent-scala**  - A scala wrapper for ob1k-concurrent.


##Getting started 
Micro services architecture consists of a group of different services which communicate with each other.
Ob1k supplies the infrastructure to build such microservices and means for them to communicate.
The communication between services is based on a RPC protocol, (HTTP with JSON or [MessagePack](http://msgpack.org/) payload), using a user provided strongly typed interface.
That said, the Ob1k server is actually an HTTP server, and you can use your favorite HTTP client to talk to your service, though we recommend you use ours ;)


###Ob1k Server
Let's start with creating an Ob1k server. Create a new maven project and add dependency to ob1k-core in your pom:

```xml
<dependency>
  <groupId>com.outbrain.ob1k</groupId>
  <artifactId>ob1k-core</artifactId>
  <version>x.y</version>
</dependency>
```

The next step will be to create a service endpoint. A service endpoint is an interface and an implementation.
Each method in the implementation will be mapped to a URL which clients as well as a simple web browser can invoke.
In the next example we are creating a service with on endpoint named `helloWorld` which gets no arguments and returns a string.
```java
public interface IHelloService extends Service {
   ComposableFuture<String> helloWorld();
}
```
The method implementation just returns a "Hello world" string which will be returned by the Ob1k framework to the client:
```java 
public class HelloService implements IHelloService {
   @Override
   public ComposableFuture<String> helloWorld() {
     return fromValue("Hello World!");
   }
}
```
 
Now that you have the service endpoint we can build the Ob1k server. For that we will need to set the port to use and the base URL which is called context. 
In addition we need to bind our services to a URL under the context. After setting some more properties (e.g. requestTimeout) we call the build method and this creates a server.

```java 
Server server = ServerBuilder.newBuilder().
  configurePorts(builder -> builder.setPort(8080)).
     setContextPath("/services").
     withServices(builder -> builder.addService(new HelloService(), "/hello")).
     configureExtraParams(builder -> builder.setRequestTimeout(50, TimeUnit.MILLISECONDS)).
     build();
```
To start the server:
```java
server.start(); 
```
Now you can access the service endpoint just go to 
    http://localhost:8080/services/hello/helloWorld


###Ob1k Client
Now we are going to create an Ob1k client. Most of the times Ob1k clients are going to be executed inside an Ob1k service, but for simplicity we will show just the client code for now.
We use the `ClientBuilder` to build the client by specifying a target URL, the interface of the service we're invoking, content type (which is controlled by the client) and can be either JSON or MessagePack, timeouts, etc.
```java
final String target = "http://localhost:8080/services/hello";
final IHelloService helloService = new ClientBuilder<>(IHelloService.class).
            setProtocol(ContentType.JSON).
            setRequestTimeout(-1).
            setTargetProvider(new SimpleTargetProvider(target)).
            build();
```
Now that we have `helloService` we can invoke methods on it which will be directed automatically to the server.
```java
final ComposableFuture<String> helloWorld = helloService.helloWorld();
System.out.println(helloWorld.get());
```

And thats it :) 


##Examples
More examples can be found here 
[ob1k-example](https://github.com/outbrain/ob1k/tree/master/ob1k-example/src/main/java/com/outbrain/ob1k/example/)

##Links
[Ob1k Presentation Slides](http://www.slideshare.net/eranharel/ob1k-presentation-at-javail)
