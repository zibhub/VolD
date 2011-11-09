# Generated by Buildr 1.4.6, change to your liking
#
require 'buildr/java'
include Java
include Commands


# Version number for this release
VERSION_NUMBER = "0.1.0"
# Group identifier for your projects
GROUP = "vold"
COPYRIGHT = ""

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << 'http://www.ibiblio.org/maven2'
repositories.remote << 'http://repo1.maven.org/maven2'
repositories.remote << 'http://repository.codehaus.org'
repositories.remote << 'http://google-gson.googlecode.com/svn/mavenrepo'
repositories.remote << 'http://guiceyfruit.googlecode.com/svn/repo/releases'
repositories.remote << 'http://download.java.net/maven/2'
repositories.remote << 'http://static.appfuse.org/repository'
repositories.remote << 'http://repository.jboss.org/maven2'
repositories.remote << 'http://google-maven-repository.googlecode.com/svn/repository'
repositories.remote << 'http://people.apache.org/repo/m2-incubating-repository'
repositories.remote << 'http://repository.jboss.org/nexus/content/groups/public'
repositories.remote << 'http://repo.marketcetera.org/maven'

JODA_TIME = transitive('joda-time:joda-time:jar:2.0')
BABUDB = 'org.xtreemfs.babudb:BabuDB:jar:0.5.5'
#SLF4J = transitive('org.slf4j:slf4j-api:jar:1.6.3')
SLF4J = transitive('org.slf4j:slf4j-log4j12:jar:1.5.8')
#SLF4J = transitive('org.slf4j:slf4j-jcl:jar:1.6.4')
#COMMONS_LOGGING = 'org.slf4j:jcl-over-slf4j:jar:1.6.3'
XSTREAM = transitive('com.thoughtworks.xstream:xstream:jar:1.3.1')
COMMONS_LOGGING = 'commons-logging:commons-logging:jar:1.1.1'
COMMONS_LANG = 'org.apache.commons:commons-lang3:jar:3.0.1'
SERVLET = 'javax.servlet:servlet-api:jar:2.5'
SPRING_VERSION = "3.0.5.RELEASE"
SPRING = [ 
           "org.springframework:spring-asm:jar:#{SPRING_VERSION}",
           "org.springframework:spring-core:jar:#{SPRING_VERSION}",
           "org.springframework:spring-beans:jar:#{SPRING_VERSION}",
           "org.springframework:spring-context:jar:#{SPRING_VERSION}",
           "org.springframework:spring-expression:jar:#{SPRING_VERSION}",
           "org.springframework:spring-oxm:jar:#{SPRING_VERSION}",
           "org.springframework:spring-orm:jar:#{SPRING_VERSION}",
           "org.springframework:spring-jdbc:jar:#{SPRING_VERSION}",
           "org.springframework:spring-web:jar:#{SPRING_VERSION}",
           "org.springframework:spring-webmvc:jar:#{SPRING_VERSION}",
           "org.springframework:spring-expression:jar:#{SPRING_VERSION}",
           "org.springframework:spring-asm:jar:#{SPRING_VERSION}",
           "org.springframework:spring-aop:jar:#{SPRING_VERSION}",
           "org.springframework:spring-aspects:jar:#{SPRING_VERSION}",
           "org.springframework:spring-instrument:jar:#{SPRING_VERSION}",
         ] 
download artifact(BABUDB) => 'http://babudb.googlecode.com/files/BabuDB-0.5.5.jar'
#download artifact(COMMONS_LANG) => 'http://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.0.1/commons-lang3-3.0.1.jar'
JSON=['org.codehaus.jackson:jackson-core-lgpl:jar:1.7.4', 
      'org.codehaus.jackson:jackson-mapper-lgpl:jar:1.7.4']

desc "VolitaryDirectoryStorage"
define "vold" do
  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT


  define "server" do

      compile.with project('common'), project('client'), JODA_TIME, BABUDB, SLF4J, COMMONS_LOGGING, COMMONS_LANG, SPRING, XSTREAM, SERVLET
      mainClass='de.zib.vold.userInterface.ABI'

      package(:jar).with :manifest=>manifest.merge('Main-Class'=>mainClass)
      package(:jar).include _('src/main/java/META-INF/*'), :path => 'META-INF/'
      package(:jar).include _('etc/*'), :path => ''

      package(:war).with :manifest=>manifest.merge('Main-Class'=>mainClass)
      package(:war).include _('web/WEB-INF/*'), :path => 'WEB-INF/'
      package(:war).include _('src/main/java/META-INF/*'), :path => 'META-INF/'
      package(:war).include _('src/main/java/META-INF/*'), :path => 'WEB-INF/classes/META-INF/'
      package(:war).include _('etc/*'), :path => 'WEB-INF/classes/'

      desc "run, voldi, run"
      task 'run' do
          jars = compile.dependencies.map(&:to_s)
          jars += [project.package(:jar).to_s]
          jars += ['etc']
          args = [] 

          Commands.java(mainClass,
                        args, { :classpath => jars, :verbose => true } )
      end
  end

  define "common" do
    compile.with COMMONS_LANG
    package(:jar).include _('src/main/java/META-INF/*'), :path => 'META-INF/'
  end

  define "client" do
    compile.with project( 'common' ), SPRING, COMMONS_LOGGING, SLF4J, XSTREAM
    package(:jar).include _('src/main/java/META-INF/*'), :path => 'META-INF/'
  end


end

# vim:ft=ruby
