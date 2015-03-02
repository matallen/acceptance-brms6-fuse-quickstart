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

import static net.java.quickcheck.generator.PrimitiveGenerators.longs;
import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static net.java.quickcheck.generator.PrimitiveGenerators.enumValues;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import net.java.quickcheck.collection.Pair;
import net.java.quickcheck.generator.PrimitiveGenerators;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.redhat.acceptance.AbstractStepsBase;
import com.redhat.acceptance.utils.ToHappen;
import com.redhat.acceptance.utils.Wait;

import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class BillingServicePerformanceSteps extends AbstractStepsBase{
  private static final Logger log=LoggerFactory.getLogger(BillingServicePerformanceSteps.class);
  private File testFolder=new File("target/billing-service");
  private Map<String,String> cachedFileContent=new HashMap<String, String>();
  
//  private static boolean deployed = false;
//  @Given("^the billing service is deployed$")
//  public void the_billing_service_is_deployed() throws Throwable {
//    deployed=!executeCommand("list | grep billing-service").equals("");
//    
//    if (deployed) return;
//    
//    log.info(">>> Deploying Service for "+BillingServiceSteps.class.getSimpleName());
//    
//    // install feature
//    executeCommand("features:addUrl mvn:com.redhat.quickstarts.brms6fuse/features/1.0-SNAPSHOT/xml/features");
//    executeCommand("log:clear");
//    executeCommand("log:set INFO");
////    executeCommand("log:set DEBUG com.redhat.services.billing");
//    
//    // configure properties for contained acceptance testing
////    updateProperty("com.redhat.services.billing", "billing.in",  "file://../../../"+testFolder.getPath()+"?fileName=in.txt");
////    updateProperty("com.redhat.services.billing", "billing.out", "file://../../../"+testFolder.getPath()+"?fileName=out.txt");
//    updateProperty("com.redhat.services.billing", "billing.in",  "file://../../../"+testFolder.getPath()+"?antInclude=*.txt");
//    updateProperty("com.redhat.services.billing", "billing.out", "file://../../../"+testFolder.getPath()+"?${file:name.noext}.processed");
//    
//    executeCommand("features:install -v billing-service");
//    uninstallBundle("Drools :: OSGI Integration");
//    Wait.For(10, untilNoResolvedBundlesExist, "there are still bundles in a resolved state");
//    
//    Assert.assertTrue(!executeCommand("list | grep billing-service").equals(""));
//  }
  
  enum CountryCodes{
    GBR,FRA,DEU,SWE,ITA
  }
  enum Type{
    SMS,Call
  }
  
//  private int numberOfFileToGenerate=1;
  
  @Given("^(\\d+) billing files arrive with the following call records:$")
  public void billing_files_arrive_with_the_following_call_records(int numberOfFiles, List<Map<String,String>> table) throws Throwable {
//    numberOfFileToGenerate=numberOfFiles;
    Assert.assertEquals("Generator constraints should only specify one row", 1, table.size());
    Map<String,String> row=table.get(0);
    //line count
    Pair<String, String> lineCountPair=new Pair<String, String>(row.get("Line Count").split("-")[0], row.get("Line Count").split("-")[1]);
    int lineCount=integers(Integer.parseInt(lineCountPair.getFirst()), Integer.parseInt(lineCountPair.getSecond())).next();
    
    for(int fileCount=0;fileCount<=numberOfFiles;fileCount++){
      List<Map<String,String>> lines=new ArrayList<Map<String,String>>();
      
      for(int i=0;i<=lineCount;i++){
        Map<String, String> line=new HashMap<String, String>();
        
        // duration
        Pair<String, String> durationPair=new Pair<String, String>(row.get("Duration").split("-")[0], row.get("Duration").split("-")[1]);
        int duration=integers(Integer.parseInt(durationPair.getFirst()), Integer.parseInt(durationPair.getSecond())).next();
        line.put("Duration", String.valueOf(duration));
        // from
        line.put("From", String.format("%11s", longs(1000000000l, 9999999999l).next()).replaceAll(" ", "0"));
        line.put("From Country", enumValues(CountryCodes.class).next().name());
        // to
        line.put("To", String.format("%11s", longs(1000000000l, 9999999999l).next()).replaceAll(" ", "0"));
        line.put("To Country", enumValues(CountryCodes.class).next().name());
        // type
        line.put("Type", "<generated>".equalsIgnoreCase(row.get("Type"))?enumValues(Type.class).next().name():row.get("Type"));
        
        lines.add(line);
      }
      writeFile(new File(testFolder, "file-"+fileCount+".txt"), BillingServiceSteps.createBillingFile(lines));
    }
  }
  
  @Then("^the billing files call records should match:$")
  public void the_billing_file_call_records_should_match(List<Map<String,String>> table) throws Exception{
    
    Assert.assertEquals("Comparator constraints should only specify one row", 1, table.size());
    Map<String,String> row=table.get(0);
    
    FilenameFilter filter=new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".processed");
      }
    };
    
    File[] files=testFolder.listFiles(filter);
    
    for(File file:files){
      String content=readFile(file);
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
        
        Assert.assertTrue(to.matches(row.get("To")));
      }
    }
  }
  
}
