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

package opennlp.tools.ml;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import opennlp.tools.ml.maxent.GIS;
import opennlp.tools.ml.maxent.quasinewton.QNTrainer;
import opennlp.tools.ml.perceptron.PerceptronTrainer;
import opennlp.tools.ml.perceptron.SimplePerceptronSequenceTrainer;

public class TrainerFactory {

  // built-in trainers
  private static final Map<String, Class> BUILTIN_TRAINERS;

  static {
    Map<String, Class> _trainers = new HashMap<String, Class>();
    _trainers.put(GIS.MAXENT_VALUE, GIS.class);
    _trainers.put(QNTrainer.MAXENT_QN_VALUE, QNTrainer.class);
    _trainers.put(PerceptronTrainer.PERCEPTRON_VALUE, PerceptronTrainer.class);
    _trainers.put(SimplePerceptronSequenceTrainer.PERCEPTRON_SEQUENCE_VALUE,
        SimplePerceptronSequenceTrainer.class);

    BUILTIN_TRAINERS = Collections.unmodifiableMap(_trainers);
  }

  private static String getPluggableTrainerType(String className) {
    try {
      Class<?> trainerClass = Class.forName(className);
      if(trainerClass != null) {
        
        if (EventTrainer.class.isAssignableFrom(trainerClass)) {
          return EventTrainer.EVENT_VALUE;
        }
        else if (SequenceTrainer.class.isAssignableFrom(trainerClass)) {
          return SequenceTrainer.SEQUENCE_VALUE;
        }
      }
    } catch (ClassNotFoundException e) {
    }
    
    return "UNKOWN";
  }
  
  public static boolean isSupportEvent(Map<String, String> trainParams) {
    
    String trainerType = trainParams.get(AbstractTrainer.TRAINER_TYPE_PARAM);
    
    if (trainerType == null) {
      String alogrithmValue = trainParams.get(AbstractTrainer.ALGORITHM_PARAM);
      if (alogrithmValue != null) {
        trainerType = getPluggableTrainerType(trainParams.get(AbstractTrainer.ALGORITHM_PARAM));
      }
    }
    
    if (trainParams.get(AbstractTrainer.TRAINER_TYPE_PARAM) != null) {
      return EventTrainer.EVENT_VALUE.equals(trainParams
          .get(AbstractTrainer.TRAINER_TYPE_PARAM));
    } 
    
    return true;
  }

  public static boolean isSupportSequence(Map<String, String> trainParams) {
    
    String trainerType = trainParams.get(AbstractTrainer.TRAINER_TYPE_PARAM);
    
    if (trainerType == null) {
      String alogrithmValue = trainParams.get(AbstractTrainer.ALGORITHM_PARAM);
      if (alogrithmValue != null) {
        trainerType = getPluggableTrainerType(trainParams.get(AbstractTrainer.ALGORITHM_PARAM));
      }
    }
    
    if (SequenceTrainer.SEQUENCE_VALUE.equals(trainerType)) {
      return true;
    }
    
    return false;
  }

  /**
   * This method is deprecated and should not be used! <br>
   * Use {@link TrainerFactory#isSupportSequence(Map)} instead.
   * 
   * @param trainParams
   * @return
   */
  @Deprecated
  public static boolean isSequenceTraining(Map<String, String> trainParams) {
    return SimplePerceptronSequenceTrainer.PERCEPTRON_SEQUENCE_VALUE
        .equals(trainParams.get(AbstractTrainer.ALGORITHM_PARAM));
  }
  
  public static SequenceTrainer getSequenceTrainer(
      Map<String, String> trainParams, Map<String, String> reportMap) {
    String trainerType = getTrainerType(trainParams);
    if (BUILTIN_TRAINERS.containsKey(trainerType)) {
      return TrainerFactory.<SequenceTrainer> create(
          BUILTIN_TRAINERS.get(trainerType), trainParams, reportMap);
    } else {
      return TrainerFactory.<SequenceTrainer> create(trainerType, trainParams,
          reportMap);
    }
  }

  public static EventTrainer getEventTrainer(Map<String, String> trainParams,
      Map<String, String> reportMap) {
    String trainerType = getTrainerType(trainParams);
    if(trainerType == null) {
      // default to MAXENT
      return new GIS(trainParams, reportMap);
    }
    
    if (BUILTIN_TRAINERS.containsKey(trainerType)) {
      return TrainerFactory.<EventTrainer> create(
          BUILTIN_TRAINERS.get(trainerType), trainParams, reportMap);
    } else {
      return TrainerFactory.<EventTrainer> create(trainerType, trainParams,
          reportMap);
    }
  }
  
  public static boolean isValid(Map<String, String> trainParams) {

    // TODO: Need to validate all parameters correctly ... error prone?!
    
    String algorithmName = trainParams.get(AbstractTrainer.ALGORITHM_PARAM);
    
    // to check the algorithm we verify if it is a built in trainer, or if we can instantiate
    // one if it is a class name
    
    if (algorithmName != null && 
        !(BUILTIN_TRAINERS.containsKey(algorithmName) || canLoadTrainer(algorithmName))) {
      return false;
    }

    try {
      String cutoffString = trainParams.get(AbstractTrainer.CUTOFF_PARAM);
      if (cutoffString != null) Integer.parseInt(cutoffString);
      
      String iterationsString = trainParams.get(AbstractTrainer.ITERATIONS_PARAM);
      if (iterationsString != null) Integer.parseInt(iterationsString);
    }
    catch (NumberFormatException e) {
      return false;
    }
    
    String dataIndexer = trainParams.get(AbstractEventTrainer.DATA_INDEXER_PARAM);
    
    if (dataIndexer != null) {
      if (!(AbstractEventTrainer.DATA_INDEXER_ONE_PASS_VALUE.equals(dataIndexer) 
          || AbstractEventTrainer.DATA_INDEXER_TWO_PASS_VALUE.equals(dataIndexer))) {
        return false;
      }
    }
    
    // TODO: Check data indexing ... 
     
    return true;
  }

  private static boolean canLoadTrainer(String className) {
    try {
      Class<?> trainerClass = Class.forName(className);
      if(trainerClass != null &&
          (EventTrainer.class.isAssignableFrom(trainerClass)
              || SequenceTrainer.class.isAssignableFrom(trainerClass))) {
        return true;
      }
    } catch (ClassNotFoundException e) {
      // fail
    }
    return false;
  }

  private static String getTrainerType(Map<String, String> trainParams) {
    return trainParams.get(AbstractTrainer.ALGORITHM_PARAM);
  }

  private static <T> T create(String className,
      Map<String, String> trainParams, Map<String, String> reportMap) {
    T theFactory = null;

    try {
      // TODO: won't work in OSGi!
      Class<T> trainerClass = (Class<T>) Class.forName(className);
      
      theFactory = create(trainerClass, trainParams, reportMap);
    } catch (Exception e) {
      String msg = "Could not instantiate the " + className
          + ". The initialization throw an exception.";
      System.err.println(msg);
      e.printStackTrace();
      throw new IllegalArgumentException(msg, e);
    }
    return theFactory;
  }

  private static <T> T create(Class<T> trainerClass,
      Map<String, String> trainParams, Map<String, String> reportMap) {
    T theTrainer = null;
    if (trainerClass != null) {
      try {
        Constructor<T> contructor = trainerClass.getConstructor(Map.class,
            Map.class);
        theTrainer = contructor.newInstance(trainParams, reportMap);
      } catch (Exception e) {
        String msg = "Could not instantiate the "
            + trainerClass.getCanonicalName()
            + ". The initialization throw an exception.";
        System.err.println(msg);
        e.printStackTrace();
        throw new IllegalArgumentException(msg, e);
      }
    }
    
    return theTrainer;
  }
}
