# docker-oauth2-server-template

It is a minimum build oauth2 server using spring-security-oauth2. It is designed to 
provide oauth2 service in between linked docker containers.
It comes with a unit test which demo the standard flow for 'authorization_code' grant type.

Read More
https://www.digitalocean.com/community/tutorials/an-introduction-to-oauth-2

## Usage

To boot up a dev server at localhost, run

```
gradle bootRun
```

To build the docker image, run

```
gradle build buildDocker
```

To test the 'authorization_code' grant type flow

```
gradle test
```

