package com.redhat.acceptance.utils;

public class Vt100ControlCharacters {
  
  public static String strip(String s){
    return s.replaceAll("\\e\\[[\\d;]*[^\\d;]","");
  }
}
