#Rest - A Simple Rest Server Example
This example attempts to show a usage of ob1k with the option of binding endpoints with specific http method type.
To start the service, open the example via your favorite IDE and run "RestServer.java", then go to to: `http://localhost:8080/api/users`

##API Usage
The following example defines a resource service called **UsersService**
which exposes the following endpoints:

> [**GET**]: "/" => Fetches all users
> 
> [**PUT**]: "/" => Creates a new user (Required fields: name, address,
> profession)
> 
> [**DELETE**]: "/{id}" => Deletes an user
> 
> [**GET**]: "/{id}" => Fetches a specific user
> 
> [**POST**]: "/{id}" => Updates an user (Optional fields: name, address,
> profession)

The requests should be in either JSON format or MessagePack.

##Interface
This example doesn't contains with it some web interface which interacts with the service.
You're encouraged to use a simple nice tool called [Postman](http://www.getpostman.com/).

##Todo
Add ob1k client example