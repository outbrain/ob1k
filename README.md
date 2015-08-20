#Ob1k - A modern RPC Framework

[![Build Status](https://travis-ci.org/outbrain/ob1k.svg?branch=master)](https://travis-ci.org/outbrain/ob1k)
[![Download](https://api.bintray.com/packages/harel-eran/Ob1k/com.outbrain.swinfra/images/download.svg)](https://bintray.com/harel-eran/Ob1k/com.outbrain.swinfra/_latestVersion)

##Overview and Motivation 
Ob1k is an asynchronous RPC framework for rapid development of async, high performance micro services. 
You can start an Ob1k embedded server from your code and once started it will serve requests based on the way it was configured. 
Ob1k does not require a thread per request and it is based on an async stack of Netty.     

##Anatomy 
Ob1k Consists of the following sub libraries
 * ob1k-concurrent - Introduces Compasble futures an alternative implementation of futures in Java  
 * ob1k-core - Client and server infrastructure 
 * ob1k-db - 

##Getting started 


##Usage
Add dependency to ob1k-core in your pom:

```xml
<dependency>
  <groupId>com.outbrain.ob1k</groupId>
  <artifactId>ob1k-core</artifactId>
  <version>x.y</version>
</dependency>
```

And read the examples :)

##Examples
[ob1k-example](https://github.com/outbrain/ob1k/tree/master/ob1k-example/src/main/java/com/outbrain/ob1k/example/)

##Links
[Ob1k Presentation Slides](http://www.slideshare.net/eranharel/ob1k-presentation-at-javail)
