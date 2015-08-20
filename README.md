#Ob1k - A modern RPC Framework

[![Build Status](https://travis-ci.org/outbrain/ob1k.svg?branch=master)](https://travis-ci.org/outbrain/ob1k)
[![Download](https://api.bintray.com/packages/harel-eran/Ob1k/com.outbrain.swinfra/images/download.svg)](https://bintray.com/harel-eran/Ob1k/com.outbrain.swinfra/_latestVersion)

##Overview and Motivation 
Ob1k is an asynchronous RPC framework for rapid development of async, high performance micro services. 
You can start an Ob1k embedded server from your code and once started it will serve requests based on the way it was configured. 
Ob1k does not require a thread per request and it is based on an async stack of Netty.     

##Anatomy 
Ob1k Consists of the following sub libraries
 * ob1k-concurrent - Introduces composable futures, an alternative implementation of futures in Java.   
 * ob1k-concurrent-scala - A scala wrapper for ob1k-concurrent 
 * ob1k-core  - RPC framework Client and server infrastructure 
 * ob1k-db    - A composable futures based asynchronous client for MySQL
 * ob1k-cache - A composable futures based asynchronous client for Memcache 
 * ob1k-cql   - A composable futures based asynchronous client for cassandra
 * ob1k-security - Authentication and authorization for Ob1k 
 * ob1k-consul   - Enables consul registration  for Ob1k services 


##Getting started 

In order to create an Ob1k service you will need to add dependency to ob1k-core in your pom:

```xml
<dependency>
  <groupId>com.outbrain.ob1k</groupId>
  <artifactId>ob1k-core</artifactId>
  <version>x.y</version>
</dependency>
```

The next step will be to create a service. For that you will need to create an interface and a class to implement it:
    public interface IHelloService extends Service {
       ComposableFuture<String> helloWorld();
    }
And a class 
  public class HelloService implements IHelloService {

      @Override
       public ComposableFuture<String> helloWorld() {
          return fromValue("Hello World!");
      }
  }
 
The most simple ob1k server looks like that 

    Server server = ServerBuilder.newBuilder().
            configurePorts(builder -> builder.setPort(8080)).
            setContextPath(CTX_PATH).
            withServices(builder -> builder.addService(new HelloService(), "/hello")).
            configureExtraParams(builder -> builder.setRequestTimeout(50, TimeUnit.MILLISECONDS)).
            build();

And read the examples :)

##Examples
[ob1k-example](https://github.com/outbrain/ob1k/tree/master/ob1k-example/src/main/java/com/outbrain/ob1k/example/)

##Links
[Ob1k Presentation Slides](http://www.slideshare.net/eranharel/ob1k-presentation-at-javail)
