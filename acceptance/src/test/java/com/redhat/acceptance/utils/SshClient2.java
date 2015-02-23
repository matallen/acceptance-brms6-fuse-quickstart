package com.redhat.acceptance.utils;

import static org.apache.sshd.SshClient.setUpDefaultClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;
import org.apache.sshd.SshClient;

public class SshClient2 {
  private static final Logger log=Logger.getLogger(SshClient2.class);
  private ClientSession session;
  private SshClient client;
  private String host;
  private String username;
  private String password;
  private int port;
  
  public SshClient2(String host, String username, String password, int port) {
    this.host=host;
    this.username=username;
    this.password=password;
    this.port=port;
  }
  public void start() throws Exception{
    client = setUpDefaultClient();
    client.start();
    session = client.connect(host, port).await().getSession();
    session.authPassword(username, password);
    session.waitFor(ClientSession.CLOSED | ClientSession.AUTHED, 0);
  }

  
  public String executeCommand(String command) {
    try{
      // ensure 'execution' character (CR-LF) terminates the command if not provided
//      System.out.println("in  >> "+command.trim());
      log.trace("in  >> "+command.trim());
      
      if (!command.endsWith("\n")) command+="\n";
      
      // execute command and block/wait
      ClientChannel channel = session.createShellChannel();
      channel.setIn(new NoCloseInputStream(new ByteArrayInputStream(command.getBytes())));
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      channel.setOut(new NoCloseOutputStream(out));
      channel.setErr(new NoCloseOutputStream(out));
      channel.open();
      channel.waitFor(ClientChannel.CLOSED, 0);
      
      // strip VT100 console control codes from the Fuse/Karaf output
      String[] lines=new String(out.toByteArray()).split("\r\n");
      StringBuffer result=new StringBuffer();
      for(int i=21;i<lines.length-1;i++)
        result.append(Vt100ControlCharacters.strip(lines[i])).append("\n");
      
      // visual output of Fuse/Karaf interactions
      if (!"".equals(result.toString().trim()))
        log.trace("out >> "+result.toString().trim());
//        System.out.println("out >> "+result.toString().trim());
      return result.toString();
    }catch(Exception e){
//      System.err.println(e.getMessage());
//      return e.getMessage();
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  public void stop(){
    session.close(false);
    client.stop();
  }

}
