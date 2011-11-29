---
title: VolD
root: .
layout: main
---


VolD - Volatile Directory Service
=================================

VolD is a data storage with a REST based interface written in Java.
It is a map assigning each key a set of values.
The keys can be configured to be volatile, i.e. a key will be deleted if its age has exceeded a certain time to live.
The keys are stored in a hierarchical structure (wherefore the service is called Directory Service) called scopes.
Furthermore, the keys are typed.
Inserting the same key (in the same scope with the same type) from different sources results in the merging the sets of values.

The project is shipped with a server and client.
VolD also supports a flexible but easy to configure replication multi master replication.

Compiling
---------

First you need to get the sources.
The latest sources can be downloaded at github:

    $ git clone git://github.com/zibhub/VolD

To compile the whole project, run the following command in the root of VolD:

    $ buildr vold:package

After compiling, there will be `.jar` and `.war` files in `common/target/`, `client/target/` and `server/target/`.
The file `server/target/vold-server-VERSION.war` can be used in an application container such as tomcat or jetty (where VERSION is the actual version string like `0.1.0`).


### Dependencies

The project depends on the following archives:

- JodaTime
- SLF4J
- XStream
- Javax Servlet
- Spring3
- JSON
- BabuDB

When compiling the project using buildr, these ressources should be downloaded automatically and stored in `~/.m2/repository/`

Structure
---------

The project is subdivided into three packages: 'common', 'client' and 'server'.
The server and client package depend on the common archive.
Additionally, the server package depends on the client archive.
Hence, it's possible to install the client without installing the server.

Configuration
-------------

There is no need to change the configuration of the client as long as the default marshaller will be used (which is recommended).
The configuration file for the server is `server/src/main/java/META-INF/applicationContext.xml`
It will be explained in four parts.

The first part describes setting up the backends, where the physical data will be stored.
The second part describes the volatile logic interface on top of the backends.
The third part uses these interface to build up a replication tree.
The fourth and last part describes setting up the frontend and user interface.

### Backends

An arbitrary number of backends can be configured.
A new backend will be created by inserting a bean tag with a new unique id and the parameter class telling which type of backend to crate.
Within the bean tag, the backend specific properties will be set with property tags.
Since version 0.1.0, there are three different types of backends, namely

- BabuDirectory using BabuDB as physical storage,
- FileSystemDirectory using a file system directory as physical storage and
- WriteLogger, using a file on the disk to log all write requests.

The WriteLogger backend is a write-only backend and cannot be used for read requests.

#### BabuDirectory

The following entry would create a BabuDirectory backend with the id `UNIQUEID`.
The database named `DBNAME` would be stored in the directory `/path/to/db` whereas its log files would be stored in `/path/to/db/logs`.
The data would be written asynchronously and all incoming data would be interpreted `utf-8` encoded.

    <bean id="UNIQUEID" class="de.zib.vold.backend.BabuDirectory">
        <property databaseName" value="DBNAME" />
        <property name="dir" value="/path/to/db" />
        <property name="logDir" value="/path/to/db/logs" />
        <property name="sync" value="ASYNC" />
        <property name="enc" value="utf-8" />
    </bean>

#### FileSystemDirectory

The following entry would create a FileSystemDirectory backend with the id `UNIQUEID`.
The physical data would be stored in the directory `/path/to/fsd` and the incoming data would be interpreted `utf-8` encoded.

    <bean id="UNIQUEID" class="de.zib.vold.backend.FileSystemDirectory">
        <property name="rootPath" value="/path/to/fsd" />
        <property enc="utf-8" />
    </bean>

#### WriteLogger

The following entry would create a WriteLogger backend with the id `UNIQUEID`.
All write requests would be logged in the file `/path/to/log/file`.

    <bean id="UNIQUEID" class="de.zib.vold.backend.WriteLogger">
        <property name="logfile" value="/path/to/log/file" />
    </bean>

### Volatile Logic

The backends are only able to store some data.
They do not know anything about timestamps, time to live or the whole volatile logic at all.
The timing informations are stored in a TimeSlice object.
The volatile logic is implemented in the class VolatilLogicImpl, thus each backend has to be wrapped by an object of this class.

The following entry would create a TimeSlice descriptor describing 500 time slices each of the size 200 milliseconds.

    <bean id="timesliceid" class="de.zib.vold.volatilelogic.TimeSlice">
        <property name="numberOfSlices" value="500" />
        <property name="timeSliceSize" value="200" />
    </bean>

Having a backend with the id `BACKENDID`, the following entry would create a VolatileLogicImpl object with the id `UNIQUEID` using the backend `BACKENDID` to store the keys, its birthdates and slice informations it gets from the TimeSlice `TIMESLICEID`.

    <bean id="UNIQUEID" class="de.zib.vold.volatilelogic.VolatileLogicImpl">
        <property name="backend" ref="BACKENDID" />
        <proeprty name="timeslice" ref="TIMESLICEID" />
    </bean>

Of course, an appropriate TimeSlice object and backend has to be configured somewhere else in the configuration file.

### Replication Tree

This part can be skipped if replication is not needed.
In principle, the replication tree is a binary tree where the leafs are VolatileDirectoryImpl objects and each other node is a ReplicatedVolatileDirectory object.
A ReplicatedVolatileDirectory is assigned a VolatileDirectory as backend which will serve all read and write requests and a Replicator object which replicates all write requests.
Before explaining the replicators, the definition of a ReplicatedVolatileDirectory object will be given.
The following entry would create a ReplicatedVolatileDirectory with the uniqeus proxying all requests to the

There are two different types of Replicators, namely LocalReplicator and RESTReplicator.
The former replicates to a VolatileDirectory object which itselfes can be a ReplicatedVolatileDirectory object again or a VolatileDirectoryImpl object.
A RESTReplicator object replicates all write requests to another VolD REST based service.

### Frontends

There are two different types of frontends which can be configured independently, namely the Reaper and Frontend classes.
The Reaper cares about keys exceeding their time to live and deletes them.
The following entry would create a Reaper with the id `UNIQUEREAPERID` deleting all keys older than 20000 milliseconds in the volatile directory `VOLDID` (either a replication or a concrete implementation).

    <bean id="UNIQUEREAPERID" class="de.zib.vold.frontend.Reaper">
        <property name="TTL" value="20000" />
        <property name="volatileDirectory" ref="VOLDID" />
    </bean>

The Frontend cares about all requests beeing reentrant.
A Frontend with the id `UNIQUEFRONTENDID` working on the volatile directory `VOLDID` would be created with the following entry.

    <bean id="UNIQUEFRONTENDID" class="de.zib.vold.frontend.Frontend">
        <property name="volatileDirectory" ref="VOLDID" />
    </bean>

Configuration Examples
----------------------

### Most Simple Example

### Full Featured Example

### Usual Case Example


