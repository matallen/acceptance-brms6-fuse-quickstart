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

import static com.jayway.restassured.RestAssured.given;

import org.apache.commons.lang.StringUtils;

import com.jayway.restassured.specification.RequestSpecification;

public class Http {
  
  public class Response{
    private int statusCode; 
    private String response;
    public Response(com.jayway.restassured.response.Response response2) {
      statusCode=response2.getStatusCode();
      response=response2.asString();
    }
    public String asString(){
      return response;
    }
    public int getStatusCode(){
      return statusCode;
    }
  }
  
  private String authUsername;
  private String authPassword;
  
  private RequestSpecification common(){
    RequestSpecification spec=given().when();
    if (!StringUtils.isEmpty(authUsername) && StringUtils.isEmpty(authPassword)){
      spec=spec.auth().preemptive().basic(authUsername, authPassword);
    }
    return spec;
  }
  
  public Http basicAuth(String username, String password){
    this.authUsername=username;
    this.authPassword=password;
    return this;
  }
  
  public Response get(String url){
    return new Response(common().get(url));
  }
  
  public Response post(String url, String body){
    return new Response(common().body(body).post(url));
  }
  
}
