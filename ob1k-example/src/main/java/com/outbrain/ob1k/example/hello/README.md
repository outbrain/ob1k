#Hello Server
This example attempts to show a basic usage of ob1k, including creating a new server, registering a service and using a RPC
client to talk with our endpoints.

To start the server, do one of the following:
* Go to project root and run `mvn exec:java -Dexec.mainClass="com.outbrain.ob1k.example.hello.server.HelloServer" -pl ob1k-example`
* Open the example via your favorite IDE and run HelloServer main

After that, go to: `http://localhost:8080/services/hello/helloWorld`

Go ahead and read the code

##RPC client
This example contains RPC client bounded to our hello service.
Go ahead and run: `mvn exec:java -Dexec.mainClass="com.outbrain.ob1k.example.hello.client.HelloServiceClient" -pl ob1k-example` (or via your IDE)