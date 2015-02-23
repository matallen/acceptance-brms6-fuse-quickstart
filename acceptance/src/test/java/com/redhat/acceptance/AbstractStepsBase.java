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
import com.redhat.acceptance.utils.SshClient2;
import com.redhat.acceptance.utils.ToHappen;
import com.redhat.acceptance.utils.Wait;

public abstract class AbstractStepsBase {
  private static final Logger log=Logger.getLogger(AbstractStepsBase.class);
  private SshClient2 sshClient = new SshClient2("localhost", "admin", "admin", SshClient.KARAF_PORT_DEFAULT);
  private static boolean sshClientStarted=false;
  
  public ToHappen untilNoResolvedBundlesExist=new ToHappen() {public boolean hasHappened() {
    return executeCommand("osgi:list | grep Resolved").equals("");
  }};
  
  public SshClient2 getSshClient(){
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
    final String grepCommand="config:list | grep \""+propertyName+" = "+value.replaceAll("\\?", "\\\\\\\\?").replaceAll("\\*", "\\\\*")+"\"";
    log.debug("Setting property [pid="+servicePid+", property="+propertyName+", value="+value+"]");
    boolean success=Wait.For(14, 3, new ToHappen() {public boolean hasHappened() {
      executeCommand("propset -p "+servicePid+" "+propertyName+" "+value);
      String grepResult=executeCommand(grepCommand);
      return grepResult.length() > 0;
    }}, "unable to set property [pid="+servicePid+", property="+propertyName+", value="+value+"]");
    if (success)
      log.debug("Property set successfully");
    Assert.assertTrue("Config NOT applied [pid="+servicePid+", property="+propertyName+", value="+value+"]", success);
  }
  
  public String executeCommand(String command) {
    return getSshClient().executeCommand(command);
  }
  
  public void uninstallBundle(final String name){
    String response=getSshClient().executeCommand("osgi:list | grep '"+name+"'");
    if (response.length()<=0) return; // couldnt find it
    
    boolean matches=response.matches(".*\\[([ 0-9]*)\\].*");
    System.out.println(matches);
    
    if (response.indexOf("[")>=0 && response.indexOf("]")>0){// check for invalid response strings
//    if (response.matches(".*\\[([ 0-9]*)\\].*")){ // check for invalid response strings
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
  
  public File createFile(File file, String contents) throws IOException{
    log.debug("creating file ["+file.getPath()+"] with content ["+(contents.length()>50?contents.substring(0,50)+"...":contents)+"]");
    file.getParentFile().mkdirs();
    IOUtils.write(contents.getBytes(), new FileOutputStream(file));
    return file.getCanonicalFile();
  }
  
  public String readFile(final File file) throws FileNotFoundException, IOException{
    log.debug("checking for file ["+file.getPath()+"]");
    boolean exists=Wait.For(10, new ToHappen() {
    public boolean hasHappened() {
      return file.exists();
    }}, "unable to find file ["+file.getPath()+"]");
    if (exists){
      String out=IOUtils.toString(new FileInputStream(file));
      return out;
    }else{
      String logOutput=executeCommand("log:display");
      System.out.println("LOG OUTPUT:\n"+logOutput);
      throw new FileNotFoundException("file not found ["+file.getPath()+"]");
    }
  }
  
}
