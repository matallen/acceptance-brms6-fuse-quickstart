/*
* JBoss, Home of Professional Open Source
* Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
* contributors by the @authors tag. See the copyright.txt in the
* distribution for a full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.redhat.acceptance.steps;

import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.redhat.acceptance.AbstractStepsBase;
import com.redhat.acceptance.utils.ToHappen;
import com.redhat.acceptance.utils.Wait;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class BillingServiceSteps extends AbstractStepsBase{
  private static final Logger log=LoggerFactory.getLogger(BillingServiceSteps.class);
  private File testFolder=new File("target/billing-service");
  private Map<String,String> cachedContent=new HashMap<String, String>();
  
//  private static boolean initialised = false;
  @Before public void beforeAll(){
//    if(!initialised) initialised=Utils.beforeScenarios();
  }

  private static boolean deployed = false;
  @Given("^the billing service is deployed$")
  public void the_billing_service_is_deployed() throws Throwable {
    deployed=!executeCommand("list | grep billing-service").equals("");
    
    if (deployed) return;
    
    log.info(">>> Deploying Service for "+BillingServiceSteps.class.getSimpleName());
    
    // install feature
    executeCommand("features:addUrl mvn:com.redhat.quickstarts.brms6fuse/features/1.0-SNAPSHOT/xml/features");
    executeCommand("log:clear");
    executeCommand("log:set INFO");
//    executeCommand("log:set DEBUG com.redhat.services.billing");
    
    // configure properties for contained acceptance testing
    updateProperty("com.redhat.services.billing", "billing.in",  "file://../../../"+testFolder.getPath()+"?antInclude=*.txt");
    updateProperty("com.redhat.services.billing", "billing.out", "file://../../../"+testFolder.getPath()+"?fileName=${file:name.noext}.processed");

    executeCommand("features:install -v billing-service");
    uninstallBundle("Drools :: OSGI Integration"); // because this bundle is always in a resolved state
    Wait.For(10, untilNoResolvedBundlesExist, "there are still bundles in a resolved state");
    
    Assert.assertTrue(!executeCommand("list | grep billing-service").equals(""));
  }
  
  static int fileCount=0;
  
  @Given("^a billing file arrives with the following call records:$")
  public void a_billing_file_arrives_with_the_following_call_records(List<Map<String,String>> table) throws Throwable {
    writeFile(new File(testFolder, fileCount+".txt"), createBillingFile(table));
    fileCount=fileCount+1;
  }
  
  public static String createBillingFile(List<Map<String, String>> table){
    StringBuffer sb=new StringBuffer();
    // Yes, I could use JaxB but sometimes its just easier to write it yourself...
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>").append("\n");
    sb.append("<call-details>").append("\n");
    for (Map<String,String> line:table)
      sb.append("<call duration=\""+line.get("Duration")+"\" from=\""+line.get("From")+"\" fromCountry=\""+line.get("From Country")+"\" to=\""+line.get("To")+"\" toCountry=\""+line.get("To Country")+"\" type=\""+line.get("Type")+"\"/>").append("\n");
    sb.append("</call-details>").append("\n");
    return sb.toString();
  }
  
  @When("^the billing files have been processed$")
  public void the_billing_file_is_processed() throws Throwable {
    
    final FilenameFilter processedFileFilter=new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".processed");
    }};
    
    log.debug("Watching for *.processed files");
    
    boolean success=Wait.For(10, new ToHappen() {
      public boolean hasHappened() {
        return testFolder.listFiles(processedFileFilter).length==fileCount;
    }}, "Timed out waiting for "+fileCount+" output files to appear in "+testFolder.getPath());
    
    if (!success){
      String logOutput=executeCommand("log:display");
      log.error("LOG OUTPUT:\n"+logOutput);
      throw new RuntimeException("Failed waiting for "+fileCount+" *.processed files");
    }
    
    File[] files=testFolder.listFiles(processedFileFilter);
    
    log.debug("Found "+files.length+" file(s)");
    for(File file:files){
      log.debug("  "+file.getName());
      String content=readFile(file);
      cachedContent.put(file.getPath(), content);
    }
  }
  
  
  @Then("^the billing file call records should match:$")
  public void the_billing_file_call_records_should_match(List<Map<String,String>> table) throws Exception{
    
    for(Entry<String, String> e:cachedContent.entrySet()){
      String filename=e.getKey();
      String content=e.getValue();
      
      List<String> calls=new ArrayList<String>();
      
      // parse actual calls
      Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(content.getBytes()));
      XPath xpath=XPathFactory.newInstance().newXPath();
      Double count=(Double) xpath.evaluate("count(/call-details/call)",doc,NUMBER);
      for (int i=1; i <= count; i++) {
        Node node=(Node) xpath.evaluate("/call-details/call[" + i + "]",doc,NODE);
        String duration    =(String) xpath.evaluate("@duration",node,STRING);
        String from        =(String) xpath.evaluate("@from",node,STRING);
        String fromCountry =(String) xpath.evaluate("@fromCountry",node,STRING);
        String to          =(String) xpath.evaluate("@to",node,STRING);
        String toCountry   =(String) xpath.evaluate("@toCountry",node,STRING);
        String type        =(String) xpath.evaluate("@type",node,STRING);
        calls.add(comparableCall(duration,from,fromCountry,to,toCountry,type));
      }
      
      Assert.assertEquals("file "+filename+": Number of expected rows ["+table.size()+"] differ from actual ["+calls.size()+"]", calls.size(), table.size());
      
      // compare actual with expected (in an unordered fashion)
      for(Map<String,String> row:table){
        String expectedDuration=row.get("Duration");
        String expectedFrom=row.get("From");
        String expectedFromCountry=row.get("From Country");
        String expectedTo=row.get("To");
        String expectedToCountry=row.get("To Country");
        String expectedType=row.get("Type");
        String expectedCall=comparableCall(expectedDuration, expectedFrom, expectedFromCountry, expectedTo, expectedToCountry, expectedType);
        if (!calls.remove(expectedCall))
          Assert.fail("file "+filename+": Unable to find: "+expectedCall.toString());
        log.debug("assertion successful - "+expectedCall);
      }
    }
  }
  
  public static String comparableCall(String duration, String from, String fromCountry, String to, String toCountry, String type) {
    return "Call[duration="+duration+":from="+from+":fromCountry="+fromCountry+":to="+to+":toCountry="+toCountry+":type="+type+"]";
  }
    
}
