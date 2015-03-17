# acceptance-brms6-fuse-quickstart
===============================

### Concept

To enable you to integrate BRMS 6.x (Drools) rules in a Camel route deployed on Fuse 6.1/Karaf.  Optionally, a 
BDD module (called acceptance) has also been provided to enable you to write BDD tests against Camel/Fuse routes.

You can:
 * Build and unit test your business rules (business-rules)
 * Build and test your fuse routes (contracts and services)
 * Define your features file to install services (features)
 * Define and implement your BDD test scenarios (acceptance)
 * Run your BDD test scenarios (acceptance)

### Overall design

Fuse contains your routes. Each route that requires rules will pull the rules from a specified 
rule repository (a maven repo) configured by a maven settings.xml.


### Transition to higher environments (ie. Prod)
Configure a different settings.xml for the routes to pull the rules from a "production" rule repository.



### How to begin
 * Get the additional tools
```
cd tools
git clone https://github.com/matallen/camel-kie-example
git clone https://github.com/matallen/maven-fusecontainer-plugin
cd ..
```
 * Download Fuse distribution from Red Hat Customer Support Portal
 * Install Fuse distribution into local repository:
```
./install-fuse-into-maven-locally.sh jboss-fuse-full-6.1.1.redhat-412.zip
```
 * Build the whole project
```
mvn clean install
```

### To run acceptance tests from Jenkins
```
cd acceptance
mvn clean install -Pfuse,acceptance
```

### To develop your acceptance tests locally
 * build your rules
 * start fuse
```
cd acceptance
mvn clean fusecontainer:run
```
 * wait for Fuse to start
 * run RunAcceptanceTests as JUnit test from your IDE
 * edit/change/add to the cucumber bdd scenarios and repeat from step 3
 * press Ctrl-C to shut down Fuse


### What the profiles do
 * fuse - this uses a custom maven plugin (in tools) to start and stop a Fuse/Karaf container
 * acceptance - this runs cucumber BDD tests allowing you to use BDD testing framework with Fuse services/routes


