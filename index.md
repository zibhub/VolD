---
title: VolD
root: .
layout: default
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
The configuration file for the server is `server/src/main/java/META-INF/applicationContext.xml`.
It will be explained in four parts.

The first part describes setting up the backends, where the physical data will be stored.
The second part describes the volatile logic interface on top of the backends.
The third part uses these interface to build up a replication tree.
The fourth and last part describes setting up the frontend and user interface.

After that, the configuration of the user interface (i.e. the configuration of the RESTController) will be explained seperately.
Alternatively, it can be configured in the file `server/web/WEB-INF/dispatcher-servlet.xml`, but the above mentioned `applicationContext.xml` is also a good choice.

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
The following entry would create a ReplicatedVolatileDirectory with the unique id `VOLDREPID` proxying all requests to the volatile directory `VOLDID` and replicating all write requests to the Replicator `REPID`.

    <bean id="VOLDREPID" class="de.zib.vold.volatilelogic.ReplicatedVolatileDirectory">
        <property name="directory" ref="VOLDID" />
        <property name="replicator" ref="REPID" />
    </bean>

There are two different types of Replicators, namely LocalReplicator and RESTVoldReplicator.
The former replicates to a VolatileDirectory object which itselfes can be a ReplicatedVolatileDirectory object again or a VolatileDirectoryImpl object.
A RESTVoldReplicator object replicates all write requests to another VolD REST based service.

The following entry would create a LocalReplicator with the unique id `REPID` proxying all write requests to the volatile directory `VOLDID`.

    <bean id="REPID" class="de.zib.vold.replication.LocalReplicator">
        <property name="replica" ref="VOLDID" />
    </bean>

The following entry would create a RESTVoldReplicator with the unique id `REPID` proxying all write requests to the VolD service reachable at the URL `http://vold.i/slave/`

    <bean id="REPID" class="de.zib.vold.replication.RESTVoldReplicator">
        <property name="baseURL" value="http://vold.i/slave/" />
    </bean>

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

### User Interfaces

Last but not least, a user interface may be configured.
The following entry would create a RESTController with the id `MASTERCONTROLLERID` proxying all requests to the Frontend `MASTERFRONTENDID`.

    <bean id="MASTERCONTROLLERID" class="de.zib.vold.userInterface.RESTController">
        <property name="frontend" ref="MASTERFRONTENDID" />
    </bean>

The default for such a controller is to handle all requests.
To change that behaviour, a SimpleUrlHandlerMapping may be configured with the following entry dispatching all requests from `/master/*` to the RestController with the id `MASTERFRONTENDID`.

    <bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping>
        <property name="mappings">
            <props>
                <prop key="/master/*">MASTERCONTROLLERID</prop>
            </props>
        </property>
    </bean>

Of course, more than one `<prop>` entries can be configured.
A typical setup would be to dispatch all replication queries from `/slave/*` to a slave controller, which itselfes does not replicate remote again.
All other requests could be dispatched to a master controller.

Configuration Examples
----------------------

### Most Simple Example

The following example sets up a VolD REST based service without a Reaper and without replication on top of the file system directory `/var/spool/vold`.
Therefore, the directory `/var/spool/vold` has to exist and the user running the application container has to have write permissions to that directory.

{% highlight xml %}
<bean id="backend" class="de.zib.vold.backend.FileSystemDirectory">
    <property name="rootPath" value="/var/spool/vold" />
    <property name="enc" value="utf-8" />
</bean>

<bean id="timeslice" class="de.zib.vold.volatilelogic.TimeSlice">
    <property name="numberOfSlices" value="60" />
    <property name="timeSliceSize" value="1000" />
    <!-- these values are choosen arbitrarily -->
</bean>

<bean id="voldi" class="de.zib.vold.volatilelogic.VolatileDirectoryImpl">
    <property name="timeslice" ref="timeslice" />
    <property name="backend" ref="backend" />
</bean>

<bean id="frontend" class="de.zib.vold.frontend.Frontend">
    <property name="volatileDirectory" ref="voldi" />
</bean>

<bean id="controller" class="de.zib.vold.userInterface.RESTController">
    <property name="frontend" ref="frontend" />
</bean>
{% endhighlight %}

### Full Featured Example

The following example sets up a VolD REST based service storing the data three times locally and replicating to another VolD service.
A Reaper and two user interfaces (one for answering queries and one for acting as slave itselfes) will be configured additionally.
There is a configuration which results in the same behaviour with a smaller replication tree, but this example tries to show all aspects.

{% highlight xml %}
<!-- --------------- BACKEND ------------------ -->

<bean id="backendfs" class="de.zib.vold.backend.FileSystemDirectory">
    <property name="rootPath" value="/var/spool/vold/fs/" />
    <property name="enc" value="utf-8" />
</bean>
<bean id="voldifs" class="de.zib.vold.volatilelogic.VolatileDirectoryImpl">
    <property name="timeslice" ref="timeslice" />
    <property name="backend" ref="backendfs" />
</bean>

<bean id="backendbabu" class="de.zib.vold.backend.BabuDirectory">
    <property databaseName" value="vold" />
    <property name="dir" value="/var/spool/vold/babu/" />
    <property name="logDir" value="/var/spool/vold/babu/logs/" />
    <property name="sync" value="ASYNC" />
    <property name="enc" value="utf-8" />
</bean>
<bean id="voldibabu" class="de.zib.vold.volatilelogic.VolatileDirectoryImpl">
    <property name="timeslice" ref="timeslice" />
    <property name="backend" ref="backendbabu" />
</bean>

<bean id="backendlogger" class="de.zib.vold.backend.WriteLogger">
    <property name="rootPath" value="/var/spool/vold/write.logs" />
    <property name="enc" value="utf-8" />
</bean>
<bean id="voldilogger" class="de.zib.vold.volatilelogic.VolatileDirectoryImpl">
    <property name="timeslice" ref="timeslice" />
    <property name="backend" ref="backendlogger" />
</bean>

<bean id="timeslice" class="de.zib.vold.volatilelogic.TimeSlice">
    <property name="numberOfSlices" value="60" />
    <property name="timeSliceSize" value="1000" />
</bean>

<!-- ---------- REPLICATION TREE -------------- -->

<!-- SLAVE TREE -->

<bean id="localreplogger" class="de.zib.vold.replication.LocalReplicator">
    <property name="replica" ref="voldilogger" />
</bean>

<bean id="repslave2" class="de.zib.vold.volatilelogic.ReplicatedVolatileLogic">
    <property name="directory" ref="voldibabu" />
    <property name="replicator" ref="localreplogger" />
</bean>

<bean id="localrepslave" class="de.zib.vold.replication.LocalReplicator">
    <property name="replica" ref="reslave2" />
</bean>

<bean id="voldislave" class="de.zib.vold.volatilelogic.ReplicatedVolatileDirectory">
    <property name="directory" ref="voldifs" />
    <property name="replicator" ref="localrepslave" />
</bean>

<!-- MASTER TREE ON TOP OF SLAVE TREE -->

<bean id="remoterep2" class="de.zib.vold.replication.RESTVoldReplicator">
    <property name="baseURL" value="http://vold.i/slave/" />
</bean>

<bean id="repmaster2" class="de.zib.vold.volatilelogic.ReplicatedVolatileDirectory">
    <property name="directory" ref="voldislave" />
    <property name="replicator" ref="remoterep2" />
</bean>

<bean id="remoterep1" class="de.zib.vold.replication.RESTVoldReplicator">
    <property name="baseURL" value="http://vold.i2/slave/" />
</bean>

<bean id="voldimaster" class="de.zib.vold.volatilelogic.ReplicatedVolatileDirectory">
    <property name="directory" ref="repmaster2" />
    <property name="replicator" ref="remoterep1" />
</bean>

<!-- ----- FRONTEND AND USER INTERFACE -------- -->

<bean id="reaper" class="de.zib.vold.frontend.Reaper">
    <property name="TTL" value="3600000" /> <!-- one hour -->
    <property name="volatileDirectory" ref="voldislave" />
</bean>

<bean id="masterfrontend" class="de.zib.vold.frontend.Frontend">
    <property name="volatileDirectory" ref="voldimaster" />
</bean>

<bean id="mastercontroller" class="de.zib.vold.userInterface.RESTController">
    <property name="frontend" ref="masterfrontend" />
</bean>

<bean id="slavefrontend" class="de.zib.vold.frontend.Frontend">
    <property name="volatileDirectory" ref="voldislave" />
</bean>

<bean id="slavecontroller" class="de.zib.vold.userInterface.RESTController">
    <property name="frontend" ref="slavefrontend" />
</bean>

<bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    <property name="mappings">
        <props>
            <prop key="/*">mastercontroller</prop>
            <prop key="/slave/*">slavecontroller</prop>
        </props>
    </property>
</bean>
{% endhighlight %}

### Usual Case Example

The following configuration example sets up a replicated VolD (to the slave `http://vold.i/slave`) and acts as slave itselfes too.
The data will be written into a file system directory on the local disk.

{% highlight xml %}
<!-- --------------- BACKEND ------------------ -->

<bean id="backend" class="de.zib.vold.backend.FileSystemDirectory">
    <property name="rootPath" value="/var/spool/vold" />
    <property name="enc" value="utf-8" />
</bean>

<bean id="timeslice" class="de.zib.vold.volatilelogic.TimeSlice">
    <property name="numberOfSlices" value="60" />
    <property name="timeSliceSize" value="1000" />
</bean>

<bean id="voldi" class="de.zib.vold.volatilelogic.VolatileDirectoryImpl">
    <property name="timeslice" ref="timeslice" />
    <property name="backend" ref="backend" />
</bean>

<!-- ---------- REPLICATION TREE -------------- -->

<bean id="slave" class="de.zib.vold.replication.RESTVoldReplicator">
    <property name="baseURL" value="http://vold.i/slave" />
</bean>

<bean id="voldimaster" class="de.zib.vold.volatilelogic.ReplicatedVolatileDirectory">
    <property name="directory" ref="voldi" />
    <property name="replicator" ref="slave" />
</bean>

<!-- ----- FRONTEND AND USER INTERFACE -------- -->

<bean id="reaper" class="de.zib.vold.frontend.Reaper">
    <property name="TTL" value="3600000" /> <!-- one hour -->
    <property name="volatileDirectory" ref="voldi" />
</bean>

<bean id="masterfrontend" class="de.zib.vold.frontend.Frontend">
    <property name="volatileDirectory" ref="voldimaster" />
</bean>

<bean id="mastercontroller" class="de.zib.vold.userInterface.RESTController">
    <property name="frontend" ref="masterfrontend" />
</bean>

<bean id="slavefrontend" class="de.zib.vold.frontend.Frontend">
    <property name="volatileDirectory" ref="voldi" />
</bean>

<bean id="slavecontroller" class="de.zib.vold.userInterface.RESTController">
    <property name="frontend" ref="slavefrontend" />
</bean>

<bean class="org.springframework.web.servlet.handler.SimpleUrlHandlerMapping">
    <property name="mappings">
        <props>
            <prop key="/*">mastercontroller</prop>
            <prop key="/slave/*">slavecontroller</prop>
        </props>
    </property>
</bean>
{% endhighlight %}

