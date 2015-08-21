#Random Commit Message Generator
This service generates random commit message by fetching from `http://whatthecommit.com`
In this example there's a basic usage of ob1k, including creating a new server, registering a service, sending http requests
to external resources and using the RPC client to talk with our endpoints.

To start the server, do one of the following:
* Go to project root and run `mvn exec:java -Dexec.mainClass="com.outbrain.ob1k.example.randomcommitmessage.server.RandomCommitMessageServer" -pl ob1k-example`
* Open the example via your favorite IDE and run RandomCommitMessageServer main

After that, go to: `http://localhost:8080/rcm/whatthecommit/single`

Go ahead and read the code