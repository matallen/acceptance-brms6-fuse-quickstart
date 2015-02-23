package org.camel.kie;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieScanner;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;

@Ignore
public class KieScannerTest {
  
  @Test
  public void test(){
    ReleaseId gav=KieServices.Factory.get().newReleaseId("com.redhat.fuse", "camel-kie-example-rules", "1.0-SNAPSHOT");
    KieContainer kContainer=KieServices.Factory.get().newKieContainer(gav);
    KieScanner s=KieServices.Factory.get().newKieScanner(kContainer);
    s.scanNow();
  }
  
  @Test
  public void writeTestFile() throws IOException{
    String content=MaskNumberRouteTest.generateExamplePayload();
    IOUtils.write(content, new FileOutputStream(new File("target/camel-kie-example", "in.txt")));
  }
}
