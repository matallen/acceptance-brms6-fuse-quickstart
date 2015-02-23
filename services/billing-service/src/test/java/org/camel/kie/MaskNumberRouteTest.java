package org.camel.kie;

import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.redhat.services.billing.domain.CallDetails;
import com.redhat.services.billing.domain.CallDetails.Call;

public class MaskNumberRouteTest extends CamelBlueprintTestSupport {
	public static Logger log=Logger.getLogger(MaskNumberRouteTest.class);
//	private static ResourceBundle properties=ResourceBundle.getBundle("config");
	
	@Override
	protected String getBlueprintDescriptor() {
		return  ""
//				+",OSGI-INF/blueprint/activemq.xml"
//				+",OSGI-INF/blueprint/kie.xml"
				+"OSGI-INF/blueprint/properties.xml"
				+",OSGI-INF/blueprint/blueprint.xml"
				;
	}

	@Override @SuppressWarnings({ "rawtypes", "unchecked" })
	public String useOverridePropertiesWithConfigAdmin(Dictionary props) {
		props.put("billing.in", "direct:in");
		props.put("billing.out", "mock:out");
//    props.put("kie.maven.settings.custom", "src/main/resources/client-settings.xml");
		return "com.redhat.services.billing";
	}
	
  public static String generateExamplePayload() throws IOException{
	  StringBuffer sb=new StringBuffer();
	  sb.append("\n<call-details>");
	  sb.append("\n <call fromCountry=\"GBR\" from=\"07112284718\" toCountry=\"GBR\" to=\"07991384916\" type=\"Call\" duration=\"300\"/>");
	  sb.append("\n <call fromCountry=\"GBR\" from=\"07112284718\" toCountry=\"GBR\" to=\"07857138576\" type=\"Call\" duration=\"60\"/>");
	  sb.append("\n <call fromCountry=\"GBR\" from=\"07112284718\" toCountry=\"GBR\" to=\"07854257832\" type=\"SMS\" duration=\"0\"/>");
	  sb.append("\n</call-details>");
	  return sb.toString();
	}
  
	@Test
	public void shouldExcecuteKieModuleRules() throws Exception{
	  MockEndpoint mockOut=bootstrapRoute("BillingServiceMaskNumbers", "{{billing.in}}", "{{billing.out}}");
	  
    template.sendBody("{{billing.in}}", generateExamplePayload());
    List<Exchange> exchanges = mockOut.getExchanges();
    Message messageOut = exchanges.get(0).getIn();
    System.out.println("messageOut = "+messageOut);
    
    CallDetails s=(CallDetails)messageOut.getBody();
    for(Call c:s.getCall())
      Assert.assertTrue(c.getTo().matches("\\d+XXXX$"));
//    org.camel.kie.domain.PhoneCallSummary s=(org.camel.kie.domain.PhoneCallSummary) messageOut.getBody();
//    System.out.println(s);
//    for(PhoneCall c:s.getPhoneCalls())
//      Assert.assertTrue(c.getTo().matches("\\d+XXXX$"));
    
    mockOut.expectedMessageCount(1);
    assertMockEndpointsSatisfied();
	}
	
	
  private MockEndpoint bootstrapRoute(String routeId, final String from, final String to) throws Exception{
    context.getRouteDefinition(routeId).adviceWith(context, new RouteBuilder() {
      public void configure() throws Exception {
        interceptFrom(from)
        .process(new Processor(){
          public void process(Exchange exchange) throws Exception {
            System.out.println("Body IN = "+exchange.getIn().getBody());
          }});
        
        interceptSendToEndpoint(to)
        // .skipSendToOriginalEndpoint()
            .process(new Processor() {
              public void process(Exchange exchange) throws Exception {
                System.out.println("Body OUT = "+exchange.getIn().getBody());
              }
            })
            .to("mock:result")
            ;
      }
    });
    return getMockEndpoint("mock:result");
  }
  
}