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
import static net.java.quickcheck.generator.PrimitiveGenerators.enumValues;
import static net.java.quickcheck.generator.PrimitiveGenerators.integers;
import static net.java.quickcheck.generator.PrimitiveGenerators.longs;
import static com.redhat.acceptance.steps.BillingServiceSteps.processedFilesFilter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import net.java.quickcheck.collection.Pair;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.redhat.acceptance.AbstractStepsBase;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;

public class BillingServicePerformanceSteps extends AbstractStepsBase{
  private static final Logger log=LoggerFactory.getLogger(BillingServicePerformanceSteps.class);
  private File testFolder=new File("target/billing-service");
  
  enum CountryCodes{
    GBR,FRA,DEU,SWE,ITA
  }
  enum Type{
    SMS,Call
  }
  
  @Given("^(\\d+) billing files are generated:$")
  public void billing_files_are_generated(int numberOfFiles, List<Map<String,String>> table) throws Throwable {
    Assert.assertEquals("Generator constraints should only specify one row", 1, table.size());
    Map<String,String> row=table.get(0);
    //line count
    Pair<String, String> lineCountPair=new Pair<String, String>(row.get("Line Count").split("-")[0], row.get("Line Count").split("-")[1]);
    int lineCount=integers(Integer.parseInt(lineCountPair.getFirst()), Integer.parseInt(lineCountPair.getSecond())).next();
    
    BillingServiceSteps.fileCount=numberOfFiles;
    
    for(int fileCount=1;fileCount<=numberOfFiles;fileCount++){
      List<Map<String,String>> lines=new ArrayList<Map<String,String>>();
      
      for(int i=1;i<=lineCount;i++){
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
  
  @Then("^all billing files should be processed within (\\d+) seconds$")
  public void all_billing_files_should_be_processed_within(int timeInSeconds) throws Throwable {
    File sourceFolder=new File(testFolder, ".camel");
    File processedFolder=testFolder;
    
    FilenameFilter sourceFilesFilter=new FilenameFilter() {public boolean accept(File dir, String name) {
        return name.endsWith(".txt");
    }};
    
    File[] sourceFiles=sourceFolder.listFiles(sourceFilesFilter);
    File[] processedFiles=processedFolder.listFiles(processedFilesFilter);
    
    Arrays.sort(sourceFiles, new Comparator<File>(){
      public int compare(File f1, File f2){
          return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()); // oldest first
      }});
    
    Arrays.sort(processedFiles, new Comparator<File>(){
      public int compare(File f1, File f2){
          return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()); // newest first
      }});
    
    File oldestSourceFile=sourceFiles[0];
    File newestProcessedFile=processedFiles[0];
    
    long durationInMs=newestProcessedFile.lastModified()-oldestSourceFile.lastModified();
    long durationInS=durationInMs/1000;
    Assert.assertTrue(durationInS<timeInSeconds);
  }
  
  @Then("^all billing file call records should match:$")
  public void all_billing_file_call_records_should_match(List<Map<String,String>> table) throws Exception{
    
    Assert.assertEquals("Comparator constraints should only specify one row", 1, table.size());
    Map<String,String> row=table.get(0);
    
    File[] files=testFolder.listFiles(processedFilesFilter);
    
    if (BillingServiceSteps.fileCount!=files.length) throw new AcceptanceException("Tried to find ["+BillingServiceSteps.fileCount+"] files, but found ["+files.length+"] instead");
    
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
        
        Assert.assertTrue(row.get("To")!=null?to.matches(row.get("To")):true);
        Assert.assertTrue(row.get("To Country")!=null?toCountry.matches(row.get("ToCountry")):true);
        Assert.assertTrue(row.get("From")!=null?from.matches(row.get("From")):true);
        Assert.assertTrue(row.get("From Country")!=null?fromCountry.matches(row.get("From Country")):true);
        Assert.assertTrue(row.get("Duration")!=null?duration.matches(row.get("Duration")):true);
        Assert.assertTrue(row.get("Type")!=null?type.matches(row.get("Type")):true);
      }
    }
    BillingServiceSteps.reset();
  }
  
}
