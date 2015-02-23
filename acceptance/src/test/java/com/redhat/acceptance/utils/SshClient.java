package com.redhat.acceptance.utils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;

import org.apache.log4j.Logger;

public class SshClient {
  private static final Logger log=Logger.getLogger(SshClient.class);
  private String server;
  private String username;
  private String password;
  private int port;
  public static final int KARAF_PORT_DEFAULT=8101;
  public static String verifier;
  public static final int timeout=3000;
  
  public SshClient(String server, String username, String password, int port) {
    super();
    this.server = server;
    this.username = username;
    this.password = password;
    this.port=port;
    
    // automatically add anyones host to the verifier list so we dont have to mess with ssh keys
    try{
      SSHClient c=new SSHClient();
      c.setConnectTimeout(timeout);
      c.setTimeout(timeout);
      c.connect(server, port);
    }catch(net.schmizz.sshj.transport.TransportException e){
      Matcher m=Pattern.compile(".+fingerprint `(.+)` for.+").matcher(e.getMessage());
      m.find();
      verifier=m.group(1);
    }catch(IOException e){
      throw new RuntimeException("Unable to determine ssh verifier - have you started fuse?", e);
    }
  }
  
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getServer() {
    return server;
  }
  public void setServer(String server) {
    this.server = server;
  }
  public String getUsername() {
    return username;
  }
  public void setUsername(String username) {
    this.username = username;
  }
  public String getPassword() {
    return password;
  }
  public void setPassword(String password) {
    this.password = password;
  }
  
  public String executeCommand(String command){
    String result=null;
    try {
      net.schmizz.sshj.SSHClient ssh = new SSHClient();
//      ssh.loadKnownHosts();
      ssh.addHostKeyVerifier(verifier);
      
      try {
        ssh.setTimeout(timeout);
        ssh.setConnectTimeout(timeout);
        ssh.connect(server, port);
        ssh.authPassword(username, password);
        // ssh.authPublickey(System.getProperty("user.name"));
        final Session session = ssh.startSession();
        try {
          log.debug(">>> Executing ["+command+"]");
          final Command cmd = session.exec(command);
          result = net.schmizz.sshj.common.IOUtils.readFully(cmd.getInputStream()).toString();
          log.debug("result >> "+result);
          cmd.join(5, TimeUnit.SECONDS);
//          System.out.println("\n** exit status: " + cmd.getExitStatus());
        } catch (Exception e){
          e.printStackTrace();
        } finally {
          session.close();
        }
      } finally {
        ssh.disconnect();
      }
      return result;
    } catch (Exception sink) {
      throw new RuntimeException(sink);
    }
  }
  
  
  
  private net.schmizz.sshj.SSHClient ssh=new SSHClient();;
  private void connect() throws IOException{
    if (!ssh.isConnected())
      ssh.connect(server, port);
    if (!ssh.isAuthenticated())
      ssh.authPassword(username, password);
    // ssh.authPublickey(System.getProperty("user.name"));
    ssh.setTimeout(240000);
    ssh.setConnectTimeout(240000);
  }
  boolean sessionStarted=false;
  Session session;
  public String executeCommand2(String command){
    String result=null;
    try {
      ssh.addHostKeyVerifier(verifier);
      
      try {
        connect();
        if (!sessionStarted)
          session = ssh.startSession();
        try {
          log.debug(">>> Executing ["+command+"]");
          final Command cmd = session.exec(command);
          result = net.schmizz.sshj.common.IOUtils.readFully(cmd.getInputStream()).toString();
          log.debug("result >> "+result);
          cmd.join(5, TimeUnit.SECONDS);
//          System.out.println("\n** exit status: " + cmd.getExitStatus());
        } finally {
//          session.close();
        }
      } finally {
//        ssh.disconnect();
      }
      return result;
    } catch (Exception sink) {
      throw new RuntimeException(sink);
    }
  }

  public void disconnect() throws IOException {
    if (null!=session && session.isOpen())
      session.close();
    if (null!=ssh && ssh.isConnected())
      ssh.disconnect();
  }
  
}
