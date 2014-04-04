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

package opennlp.tools.ml.maxent.quasinewton;

import static opennlp.tools.ml.PrepAttachDataUtil.createTrainingStream;
import static opennlp.tools.ml.PrepAttachDataUtil.testModel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.ml.AbstractEventTrainer;
import opennlp.tools.ml.AbstractTrainer;
import opennlp.tools.ml.TrainerFactory;
import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.TwoPassDataIndexer;

import org.junit.Test;

public class QNPrepAttachTest {

  @Test
  public void testQNOnPrepAttachData() throws IOException {
    AbstractModel model = 
        new QNTrainer(true).trainModel(100, 
        new TwoPassDataIndexer(createTrainingStream(), 1));

    testModel(model, 0.8165387472146571);
  }
  
  @Test
  public void testQNOnPrepAttachDataWithParams() throws IOException {
    
    Map<String, String> trainParams = new HashMap<String, String>();
    trainParams.put(AbstractTrainer.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);
    trainParams.put(AbstractEventTrainer.DATA_INDEXER_PARAM,
        AbstractEventTrainer.DATA_INDEXER_TWO_PASS_VALUE);
    trainParams.put(AbstractTrainer.CUTOFF_PARAM, Integer.toString(1));
    // use L2-cost higher than the default
    trainParams.put(QNTrainer.L2COST_PARAM, Double.toString(2.0));
    
    MaxentModel model = TrainerFactory.getEventTrainer(trainParams, null)
                                      .train(createTrainingStream());
    
    testModel(model, 0.8202525377568705);
  }
  
  @Test
  public void testQNOnPrepAttachDataWithParamsDefault() throws IOException {
    
    Map<String, String> trainParams = new HashMap<String, String>();
    trainParams.put(AbstractTrainer.ALGORITHM_PARAM, QNTrainer.MAXENT_QN_VALUE);
    
    MaxentModel model = TrainerFactory.getEventTrainer(trainParams, null)
                                      .train(createTrainingStream());
    
    testModel(model, 0.8153008170339193);
  }
  
}

