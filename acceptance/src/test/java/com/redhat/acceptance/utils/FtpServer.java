package com.redhat.acceptance.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;

public class FtpServer {
  private SshServer sshd;
  
  public FtpServer(int port){
    sshd = SshServer.setUpDefaultServer();
    sshd.setPort(port);
    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("target/hostkey.ser"));
    
    List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
    userAuthFactories.add(new UserAuthNone.Factory());
    sshd.setUserAuthFactories(userAuthFactories);

    sshd.setCommandFactory(new ScpCommandFactory());

    List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
    namedFactoryList.add(new SftpSubsystem.Factory());
    sshd.setSubsystemFactories(namedFactoryList);
  }
  
  public void start() {
      try {
        sshd.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
  }
  
  public void stop() {
    try {
      sshd.stop();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
  
}
