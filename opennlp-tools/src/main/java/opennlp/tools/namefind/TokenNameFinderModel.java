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


package opennlp.tools.namefind;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import opennlp.tools.ml.BeamSearch;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.SequenceClassificationModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.AggregatedFeatureGenerator;
import opennlp.tools.util.featuregen.FeatureGeneratorResourceProvider;
import opennlp.tools.util.featuregen.GeneratorFactory;
import opennlp.tools.util.featuregen.W2VClassesDictionary;
import opennlp.tools.util.model.ArtifactSerializer;
import opennlp.tools.util.model.BaseModel;
import opennlp.tools.util.model.ModelUtil;

/**
 * The {@link TokenNameFinderModel} is the model used
 * by a learnable {@link TokenNameFinder}.
 *
 * @see NameFinderME
 */
public class TokenNameFinderModel extends BaseModel {

  public static class FeatureGeneratorCreationError extends RuntimeException {
    FeatureGeneratorCreationError(Throwable t) {
      super(t);
    }
  }
  
  private static class ByteArraySerializer implements ArtifactSerializer<byte[]> {

    public byte[] create(InputStream in) throws IOException,
        InvalidFormatException {
      
      return ModelUtil.read(in);
    }

    public void serialize(byte[] artifact, OutputStream out) throws IOException {
      out.write(artifact);
    }
  }
  
  private static final String COMPONENT_NAME = "NameFinderME";
  private static final String MAXENT_MODEL_ENTRY_NAME = "nameFinder.model";
 
  private static final String GENERATOR_DESCRIPTOR_ENTRY_NAME = "generator.featuregen";
 
  public TokenNameFinderModel(String languageCode, SequenceClassificationModel nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries);
    
    // TODO: Add validation for sequence models!
    //if (!isModelValid(nameFinderModel)) {
    //  throw new IllegalArgumentException("Model not compatible with name finder!");
    //}
    
    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries);
  }

  public TokenNameFinderModel(String languageCode, MaxentModel nameFinderModel, int beamSize,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    super(COMPONENT_NAME, languageCode, manifestInfoEntries);
    
    if (!isModelValid(nameFinderModel)) {
      throw new IllegalArgumentException("Model not compatible with name finder!");
    }
    
    Properties manifest = (Properties) artifactMap.get(MANIFEST_ENTRY);
    manifest.put(BeamSearch.BEAM_SIZE_PARAMETER, Integer.toString(beamSize));
    
    init(nameFinderModel, generatorDescriptor, resources, manifestInfoEntries);
  }
  
  // TODO: Extend this one with beam size!
  public TokenNameFinderModel(String languageCode, MaxentModel nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, NameFinderME.DEFAULT_BEAM_SIZE, 
        generatorDescriptor, resources, manifestInfoEntries);
  }

  public TokenNameFinderModel(String languageCode, MaxentModel nameFinderModel,
      Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    this(languageCode, nameFinderModel, null, resources, manifestInfoEntries);
  }
      
  public TokenNameFinderModel(InputStream in) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, in);
  }
  
  public TokenNameFinderModel(File modelFile) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelFile);
  }
  
  public TokenNameFinderModel(URL modelURL) throws IOException, InvalidFormatException {
    super(COMPONENT_NAME, modelURL);
  }
  
  private void init(Object nameFinderModel,
      byte[] generatorDescriptor, Map<String, Object> resources, Map<String, String> manifestInfoEntries) {
    artifactMap.put(MAXENT_MODEL_ENTRY_NAME, nameFinderModel);
    
    if (generatorDescriptor != null && generatorDescriptor.length > 0)
      artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME, generatorDescriptor);
    
    if (resources != null) {
      // The resource map must not contain key which are already taken
      // like the name finder maxent model name
      if (resources.containsKey(MAXENT_MODEL_ENTRY_NAME) ||
          resources.containsKey(GENERATOR_DESCRIPTOR_ENTRY_NAME)) {
        throw new IllegalArgumentException();
      }
      
      // TODO: Add checks to not put resources where no serializer exists,
      // make that case fail here, should be done in the BaseModel
      artifactMap.putAll(resources); 
    }
    checkArtifactMap();
  }
  
  /**
   * @deprecated use getNameFinderSequenceModel instead. This method will be removed soon.
   */
  @Deprecated
  public MaxentModel getNameFinderModel() {
    
    if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel) {
      return (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
    }
    else {
      return null;
    }
  }

  public SequenceClassificationModel<String> getNameFinderSequenceModel() {
    
    Properties manifest = (Properties) artifactMap.get(MANIFEST_ENTRY);
    
    if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel) {
      String beamSizeString = manifest.getProperty(BeamSearch.BEAM_SIZE_PARAMETER);
      
      int beamSize = NameFinderME.DEFAULT_BEAM_SIZE;
      if (beamSizeString != null) {
        beamSize = Integer.parseInt(beamSizeString);
      }
      
      return new BeamSearch<>(beamSize, (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME));
    }
    else if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      return (SequenceClassificationModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
    }
    else {
      return null;
    }
  }
  
  /**
   * Creates the {@link AdaptiveFeatureGenerator}. Usually this
   * is a set of generators contained in the {@link AggregatedFeatureGenerator}.
   *
   * Note:
   * The generators are created on every call to this method.
   *
   * @return the feature generator or null if there is no descriptor in the model
   */
  public AdaptiveFeatureGenerator createFeatureGenerators() {

    byte descriptorBytes[] = (byte[]) artifactMap.get(GENERATOR_DESCRIPTOR_ENTRY_NAME);
    
    if (descriptorBytes != null) {
      InputStream descriptorIn = new ByteArrayInputStream(descriptorBytes);
  
      AdaptiveFeatureGenerator generator = null;
      try {
        generator = GeneratorFactory.create(descriptorIn, new FeatureGeneratorResourceProvider() {
  
          public Object getResource(String key) {
            return artifactMap.get(key);
          }
        });
      } catch (InvalidFormatException e) {
        // It is assumed that the creation of the feature generation does not
        // fail after it succeeded once during model loading.
        
        // But it might still be possible that such an exception is thrown,
        // in this case the caller should not be forced to handle the exception
        // and a Runtime Exception is thrown instead.
        
        // If the re-creation of the feature generation fails it is assumed
        // that this can only be caused by a programming mistake and therefore
        // throwing a Runtime Exception is reasonable
        
        throw new FeatureGeneratorCreationError(e);
      } catch (IOException e) {
        throw new IllegalStateException("Reading from mem cannot result in an I/O error", e);
      }
  
      return generator;
    }
    else {
      return null;
    }
  }
  
  public TokenNameFinderModel updateFeatureGenerator(byte descriptor[]) {
        
    TokenNameFinderModel model;
        
        if (getNameFinderModel() != null) {
          model = new TokenNameFinderModel(getLanguage(), getNameFinderModel(),
              descriptor, Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap());
        }
        else {
          model = new TokenNameFinderModel(getLanguage(), getNameFinderSequenceModel(),
              descriptor, Collections.<String, Object>emptyMap(), Collections.<String, String>emptyMap());
        }
    
    // TODO: Not so nice!
    model.artifactMap.clear();
    model.artifactMap.putAll(artifactMap);
    model.artifactMap.put(GENERATOR_DESCRIPTOR_ENTRY_NAME, descriptor);
    
    return model;
  }
  
  @Override
  protected void createArtifactSerializers(Map<String, ArtifactSerializer> serializers) {
    super.createArtifactSerializers(serializers);
    
    serializers.put("featuregen", new ByteArraySerializer());
  }
  
  public static Map<String, ArtifactSerializer> createArtifactSerializers()  {
    
    // TODO: Not so nice, because code cannot really be reused by the other create serializer method
    //       Has to be redesigned, we need static access to default serializers
    //       and these should be able to extend during runtime ?! 
    //
    //       The XML feature generator factory should provide these mappings.
    //       Usually the feature generators should know what type of resource they expect.
    
    Map<String, ArtifactSerializer> serializers = BaseModel.createArtifactSerializers();
    
    serializers.put("featuregen", new ByteArraySerializer());
    serializers.put("w2vclasses", new W2VClassesDictionary.W2VClassesDictionarySerializer());
    
    return serializers;
  }
  
  // TODO: Write test for this method
  public static boolean isModelValid(MaxentModel model) {
    
    // We should have *optionally* one outcome named "other", some named xyz-start and sometimes 
    // they have a pair xyz-cont. We should not have any other outcome
    // To validate the model we check if we have one outcome named "other", at least
    // one outcome with suffix start. After that we check if all outcomes that ends with
    // "cont" have a pair that ends with "start".
    List<String> start = new ArrayList<String>();
    List<String> cont = new ArrayList<String>();

    for (int i = 0; i < model.getNumOutcomes(); i++) {
      String outcome = model.getOutcome(i);
      if (outcome.endsWith(NameFinderME.START)) {
        start.add(outcome.substring(0, outcome.length()
            - NameFinderME.START.length()));
      } else if (outcome.endsWith(NameFinderME.CONTINUE)) {
        cont.add(outcome.substring(0, outcome.length()
            - NameFinderME.CONTINUE.length()));
      } else if (outcome.equals(NameFinderME.OTHER)) {
        // don't fail anymore if couldn't find outcome named OTHER
      } else {
        // got unexpected outcome
        return false;
      }
    }

    if (start.size() == 0) {
      return false;
    } else {
      for (String contPreffix : cont) {
        if (!start.contains(contPreffix)) {
          return false;
        }
      }
    }

    return true;
  }
  
  @Override
  protected void validateArtifactMap() throws InvalidFormatException {
    super.validateArtifactMap();
    
    if (artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof MaxentModel ||
        artifactMap.get(MAXENT_MODEL_ENTRY_NAME) instanceof SequenceClassificationModel) {
      // TODO: Check should be performed on the possible outcomes!
//      MaxentModel model = (MaxentModel) artifactMap.get(MAXENT_MODEL_ENTRY_NAME);
//      isModelValid(model);
    }
    else {
      throw new InvalidFormatException("Token Name Finder model is incomplete!");
    }
  }
}
