# Docker Container Objects JUnit Platform Extension

[![Build Status](https://travis-ci.org/rivasdiaz/dockercontainerobjects.svg?branch=master)](https://travis-ci.org/rivasdiaz/dockercontainerobjects)

This is an extension for JUnit Platform 1, a.k.a. JUnit 5

It is based on the concept of Container Objects defined in [Arquillian](http://www.arquillian.org) Cube extension

## How to use

```java
@ExtendsWith(DockerContainerObjectsExtension) //(1)
class MyContainerTest {
    
    @ContainerObject TomcatContainer tomcatContainer; //(2)
    
    @Test
    void testContainerCreated() {
        assertNotNull(tomcatContainer); //(3)
        assertNotNull(tomcatContainer.addr);
    }
    
    @Image("tomcat:jre8") //(4)
    public static class TomcatContainer {
        
        @Inject @ContainerAddress InetAddress addr; //(5)
        @Inject @ContainerId String containerId;
    }
}
```

1. Create a JUnit test annotated with `@ExtendsWith`, referencing the extension class `DockerContainerObjectsExtension`.
2. Add an attribute annotated with `@ContainerObject`, referencing your container object.
3. Your test methods can reference your container object methods and attributes.
4. Define a container object class, that can be annotated with `@Image` to define a docker image.
5. The container object class can have fields annotated with `@Inject` and different other annotations to inject information from the docker container.

## Container Objects in tests

Container Objects can be defined as class fields or instance fields.
Class fields will be created and started before any test method is executed and stopped and removed after all test methods are executed.
Instance fields will be created and started before every test method is executed and stopped and removed after the method finishes.

Container Objects can also be embedded inside other container objects.
All container objects linked from inside a container object will be started before starting the containing container object.
They will also be stopped after the containing container object is stopped.
A link will be created from the containing container object to each of the contained container objects.

## Container Objects lifecycle

Container objects can define methods annotated with any of `@BeforeCreating`, `@AfterCreated`, `@BeforeStarting`, `@AfterStarted`, `@BeforeStopping`, `@AfterStopped`, `@BeforeRemoving`, `@AfterRemoved`.
Such methods will be invoked on each stage of the container creation lifecycle.
Methods must be defined as instance methods, accepting no parameters and returning void.

## Injecting data inside the container object from docker

The container object class can define fields annotated with `@Inject` to receive information from docker.

- Fields of type `DockerClient` will receive a reference to the `DockerClient` instance used to talk to Docker.
- Fields annotated with `@ContainerId` and of type `String` will receive the container id after the container is created.
- Fields annotated with `@ContainerAddress` and of type either `String` or `Inet4Address` will receive the container internal IPv4 after the container is started.
- Fields annotated with `@ContainerAddress` and of type `Inet6Address` will receive the container global IPv6 after the container is started.
- Fields of type `NetworkSettings` will receive a reference to the `NetworkSettings` container information after starting it.
