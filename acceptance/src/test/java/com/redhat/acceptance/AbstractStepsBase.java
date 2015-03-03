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

package com.redhat.acceptance;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;

import com.google.common.base.Preconditions;
import com.redhat.acceptance.utils.SshClient;
import com.redhat.acceptance.utils.ToHappen;
import com.redhat.acceptance.utils.Wait;

public abstract class AbstractStepsBase {
  private static final Logger log=Logger.getLogger(AbstractStepsBase.class);
  private SshClient sshClient = new SshClient("localhost", "admin", "admin", SshClient.KARAF_PORT_DEFAULT);
  private static boolean sshClientStarted=false;
  
  public ToHappen untilNoResolvedBundlesExist=new ToHappen() {public boolean hasHappened() {
    return executeCommand("osgi:list | grep Resolved").equals("");
  }};
  
  public SshClient getSshClient(){
    if (!sshClientStarted){
      try{
        sshClient.start();
      }catch(Exception e){
        throw new RuntimeException(e.getMessage(), e);
      }
    }
    sshClientStarted=true;
    return sshClient;
  }
  
//  public AbstractStepsBase(){
//    try{
//      if (startSshClient())
//        sshClient.start();
//    }catch(Exception e){
//      e.printStackTrace();
//    }
//	  assertTrue("Unable to clear filecache directory", new File(System.getProperty("user.dir")+"/target/filecache").delete());
	  
//	  File cacheDir=new File(System.getProperty("user.dir")+"/target/filecache");
//	  if (cacheDir.exists()){
//		  for (File file:cacheDir.listFiles()){
//			  assertTrue("unable to delete ["+file.getName()+"]", file.delete());
//		  }
//	  }else
//		  System.err.println("Unable to find cache dir");
//  }
  
//  public String readResource(String resourceName) throws IOException{
//    String resourcePath=getResourcePath(resourceName);
//    log.debug("Loading Resource from path: "+ resourcePath);
//    URL resource = this.getClass().getClassLoader().getResource(resourcePath);
//    log.debug("Found resource: "+ resource);
//    if (resource==null){
//    	throw new RuntimeException("unable to find resource ["+resourcePath+"]");
//    }
//    InputStream is=resource.openStream();
//    if (is==null)
//    	System.out.println("the stream for resource ["+resourcePath+"] is null");
//    return IOUtils.toString(is);
//  }
//  
//  public String readRemoteFile(final String path, final String filename) throws Throwable{
//    // download file to local area for consumption multiple times
//    File[] files=new File(System.getProperty("user.dir")+"/target/filecache/").listFiles(new FilenameFilter() {
//      public boolean accept(File dir, String name) {
//        return name.equalsIgnoreCase(filename);
//      }});
//    if (files==null || files.length!=1){ // download it 'cos its not in the file cache
//      String contents=new FileIO().getFile(path+"?fileName="+filename, 10).contents();
//      assertTrue("file ["+filename+"] was not found in ["+path+"]", null!=contents);
//      File fileCache=new File(System.getProperty("user.dir")+"/target/filecache/"+filename);
//      fileCache.getParentFile().mkdirs();
//      IOUtils.write(contents, new FileOutputStream(fileCache));
//      files=new File[]{fileCache};
//    }
//    return IOUtils.toString(new FileInputStream(files[0]));
//  }
  
	public void updateProperties(String pid, Map<String,String> keyValuePairs){
		for(Map.Entry<String,String> e:keyValuePairs.entrySet())
			updateProperty(pid, e.getKey(), e.getValue());
	}
	
  // NOTE: propset commands are unreliable - they are often missed, karaf
  // cannot save the values or something. even 500ms sleeps didnt solve
  // the issue
  public void updateProperty(final String servicePid, final String propertyName, final String value){
    Preconditions.checkArgument(value!=null, "value cannot be null when setting a service property");
    log.debug("Setting property [pid="+servicePid+", property="+propertyName+", value="+value+"]");
    
    final String finalEscapedValue=value.replaceAll("\\?","\\\\?").replaceAll("\\*","\\\\*");
    final String finalEscapedValue2=value.replaceAll("\\?","\\\\?").replaceAll("\\*","\\\\*")
        // these escape sequences are for settings variables in propset configs
        .replaceAll("\\$","\\\\\\$")
        .replaceAll("\\{","\\\\\\\\\\\\{")
        .replaceAll("\\}","\\\\\\\\\\\\}")
        ;
    
    String escapedGrepValue=finalEscapedValue;
    if (finalEscapedValue.contains("$"))
      // fuse's grep command wont take that variable substitution so we cannot check to see when the setting has been set correctly if it contains a variable
      // so truncating it is the best we can do
      escapedGrepValue=finalEscapedValue.substring(0, finalEscapedValue.indexOf("$"));
    
    final String grepCommand="config:list | grep \""+propertyName+" = "+escapedGrepValue+"\"";
    
    boolean success=Wait.For(5, 1, new ToHappen() {public boolean hasHappened() {
      executeCommand("propset -p "+servicePid+" "+propertyName+" "+finalEscapedValue2);
      try {
        Thread.sleep(1000l);
      } catch (InterruptedException e) {}
      return executeCommand(grepCommand).length() > 0;
    }}, "unable to set property [pid="+servicePid+", property="+propertyName+", value="+value+"]");
    if (success)
      log.debug("Property set successfully");
    
    Assert.assertTrue("Config NOT applied [pid="+servicePid+", property="+propertyName+", value="+value+"]", success);
  }
  
  public String executeCommand(String command) {
//    System.out.println("IN  >> "+command);
    String out=getSshClient().executeCommand(command);
//    System.out.println("OUT >> "+out);
    return out;
  }
  
  public void uninstallBundle(final String name){
    String response=getSshClient().executeCommand("osgi:list | grep '"+name+"'");
    if (response.length()<=0) return; // couldn't find the bundle string
    
    if (response.indexOf("[")>=0 && response.indexOf("]")>0){// check for invalid response strings
      final String bundleId=response.substring(1,response.indexOf("]")).trim();
      getSshClient().executeCommand("osgi:uninstall "+bundleId);
      boolean success=Wait.For(10, new ToHappen() {
        public boolean hasHappened() {
          return getSshClient().executeCommand("osgi:list | grep '"+name+"'").contains("");
        }});
      log.debug("uninstallBundle ["+name+"] "+ (success?"":"un")+"successful");
    }else
      log.error("uninstallBundle not attempted - invalid response string from karaf ["+response+"]");
  }
  
  public File writeFile(File file, String contents) throws IOException{
    log.debug("creating file ["+file.getPath()+"] with content ["+(contents.length()>50?contents.substring(0,50)+"...":contents)+"]");
    file.getParentFile().mkdirs();
    IOUtils.write(contents.getBytes(), new FileOutputStream(file));
    return file.getCanonicalFile();
  }
  
  public String readFile(final File file) throws FileNotFoundException, IOException{
    log.debug("reading file content for ["+file.getPath()+"]");
    boolean exists=Wait.For(10, new ToHappen() {
    public boolean hasHappened() {
      return file.exists();
    }}, "unable to find file ["+file.getPath()+"]");
    if (exists){
      String out=IOUtils.toString(new FileInputStream(file));
      file.delete(); //cleanup read files so we dont read them multiple times
      return out;
    }else
      throw new AcceptanceException("file not found ["+file.getPath()+"]");
  }
  
  @SuppressWarnings("serial")
  public class AcceptanceException extends RuntimeException{
    public AcceptanceException(Throwable cause) {
      super(cause);
      String logOutput=executeCommand("log:display");
      log.error("LOG OUTPUT:\n"+logOutput);
    }
    public AcceptanceException(String message) {
      super(message);
      String logOutput=executeCommand("log:display");
      log.error("LOG OUTPUT:\n"+logOutput);
    }
  }
  
}
