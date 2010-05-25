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

package opennlp.tools.tokenize;

import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.ObjectStreamException;

/**
 * This class is a stream filter which reads in string encoded samples and creates
 * {@link TokenSample}s out of them. The input string sample is tokenized if a
 * whitespace or the special separator chars occur.
 * <p>
 * Sample:<b>
 * "token1 token2 token3<SPLIT>token4"
 * The tokens token1 and token2 are separated by a whitespace, token3 and token3
 * are separated by the special character sequence, in this case the default
 * split sequence.
 * 
 * The sequence must be unique in the input string and is not escaped.
 */
public class TokenSampleStream implements ObjectStream<TokenSample> {
  
  public static final String DEFAULT_SEPARATOR_CHARS = "<SPLIT>";
  
  private final String separatorChars;
  
  private ObjectStream<String> sampleStrings;
  
  public TokenSampleStream(ObjectStream<String> sampleStrings, String separatorChars) {
    
    if (sampleStrings == null || separatorChars == null) {
      throw new IllegalArgumentException("parameters must not be null!");
    }
    
    this.sampleStrings = sampleStrings;
    this.separatorChars= separatorChars;
  }
  
  public TokenSampleStream(ObjectStream<String> sentences) {
    this(sentences, DEFAULT_SEPARATOR_CHARS);
  }
  
  public TokenSample read() throws ObjectStreamException {
    String sampleString = sampleStrings.read();
    
    if (sampleString != null) {
      return TokenSample.parse(sampleString, separatorChars);
    }
    else {
      return null;
    }
  }

  public void reset() throws ObjectStreamException,
      UnsupportedOperationException {
    sampleStrings.reset();
  }
  
  public void close() throws ObjectStreamException {
    sampleStrings.close();
  }
}
