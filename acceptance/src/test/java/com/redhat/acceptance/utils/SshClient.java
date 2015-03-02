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

package com.redhat.acceptance.utils;

import static org.apache.sshd.SshClient.setUpDefaultClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.log4j.Logger;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;
//import org.apache.sshd.SshClient;

public class SshClient {
  private static final Logger log=Logger.getLogger(org.apache.sshd.SshClient.class);
  public static final int KARAF_PORT_DEFAULT=8101;
  private ClientSession session;
  private org.apache.sshd.SshClient client;
  private String host;
  private String username;
  private String password;
  private int port;
  
  public SshClient(String host, String username, String password, int port) {
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
      if (session==null) throw new RuntimeException("ssh session is closed unexpectedly");
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
