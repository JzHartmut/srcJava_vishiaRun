package org.vishia.example.fourier;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

public class ForierSyntheseHarmony
{
  float f1 = 440.0f;
  float f5 = f1 * 3/2;
  float f3 = f1 * 5/4;  //Dur-Terz
  
  
  void calcTone() throws IOException{
    float sec = 0;
    //Writer out1 = new FileWriter("tone.csv");
    PrintStream out1 = new PrintStream(new FileOutputStream("tone.csv"));
    out1.println("t; u; u1; u3; u5;");
    out1.println("1.0; 10.0; 10.0; 10.0; 10.0;");
    out1.println("0.0; -10.0; -10.0; -10.0; -10.0;");
    while(sec < 0.1f) {
      float u1 = (float)Math.cos(sec*f1 * 2*Math.PI);
      float u3 = (float)Math.cos(sec*f3 * 2*Math.PI);
      float u5 = (float)Math.cos(sec*f5 * 2*Math.PI);
      sec += 0.00005f;  // 20 kHz sampling
      float u = u1 + u3 + u5;
      out1.printf("%f; %f; %f; %f; %f;\n", sec, u, u1, u3, u5);
    }
    out1.close();
  }
  
  
  public static final void main(String[] args){
    ForierSyntheseHarmony main = new ForierSyntheseHarmony();
    try{ 
      main.calcTone();
    } catch(Exception exc){
      System.err.println(exc.getMessage());
    }
  }
}
