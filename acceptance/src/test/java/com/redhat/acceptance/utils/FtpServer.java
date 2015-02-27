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
  
  public FtpServer start() {
      try {
        sshd.start();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return this;
  }
  
  public FtpServer stop() {
    try {
      sshd.stop();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return this;
  }
  
}
