/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.cmdline.chunker;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.chunker.DefaultChunkerSequenceValidator;
import opennlp.tools.cmdline.CLI;
import opennlp.tools.cmdline.CmdLineTool;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.PerformanceMonitor;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.postag.POSSample;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ParseException;
import opennlp.tools.util.PlainTextByLineStream;

public class ChunkerMETool implements CmdLineTool {

  public String getName() {
    return "ChunkerME";
  }
  
  public String getShortDescription() {
    return "learnable chunker";
  }
  
  public String getHelp() {
    return "Usage: " + CLI.CMD + " " + getName() + " model < sentences";
  }

  public void run(String[] args) {
    if (args.length != 1) {
      System.out.println(getHelp());
      throw new TerminateToolException(1);
    }
    
    ChunkerModel model = new ChunkerModelLoader().load(new File(args[0]));
    
    ChunkerME chunker = new ChunkerME(model, ChunkerME.DEFAULT_BEAM_SIZE,
        new DefaultChunkerSequenceValidator());
    
    ObjectStream<String> lineStream =
      new PlainTextByLineStream(new InputStreamReader(System.in));
    
    PerformanceMonitor perfMon = new PerformanceMonitor(System.err, "sent");
    perfMon.start();
    
    try {
      String line;
      while ((line = lineStream.read()) != null) {
        
        POSSample posSample;
        try {
          posSample = POSSample.parse(line);
        } catch (ParseException e) {
          System.err.println("Invalid format:");
          System.err.println(line);
          continue;
        }
        
        String[] chunks = chunker.chunk(posSample.getSentence(),
            posSample.getTags());
        
        // TODO: Move this formating code to ChunkSample
        
        for (int ci=0,cn=chunks.length;ci<cn;ci++) {
          if (ci > 0 && !chunks[ci].startsWith("I-") && !chunks[ci-1].equals("O")) {
            System.out.print(" ]");
          }
          if (chunks[ci].startsWith("B-")) {
            System.out.print(" ["+chunks[ci].substring(2));
          }

          System.out.print(" "+posSample.getSentence()[ci]+"_"+posSample.getTags()[ci]);
        }
        
        if (chunks.length > 0 && !chunks[chunks.length-1].equals("O")) {
          System.out.print(" ]");
        }
        
        System.out.println();
        
        perfMon.incrementCounter();
      }
    } 
    catch (IOException e) {
      CmdLineUtil.handleStdinIoError(e);
    }
    
    perfMon.stopAndPrintFinalResult();
  }
}
