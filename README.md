# acceptance-brms6-fuse-quickstart
===============================

### What is this project?


### How to begin
 * Get the additional tools
```
cd tools
git clone https://github.com/matallen/camel-kie-example
git clone https://github.com/matallen/maven-fusecontainer-plugin
```
 * Build the whole project
```
cd <root>
mvn clean install
```
 * step 3

### To run acceptance tests from Jenkins
```
cd acceptance
mvn clean install -Pfuse,acceptance -o
```

### To develop your acceptance tests locally
1) build your rules
2) start fuse
```
cd acceptance
mvn clean fusecontainer:run
```
3) run RunAcceptanceTests class from your IDE
4) edit/change/add to the cucumber bdd scenarios and repeat from step 3


### What the profiles do
 * fuse - this uses a custom plugin (in tools) to start and stop a Fuse/Karaf container
 * acceptance - this runs cucumber BDD tests allowing you to use BDD testing framework with Fuse services/routes

### Overall design

### Transition to higher environments (ie. Prod)


