package com.inmobi.grill.server.ml;

import com.codahale.metrics.MetricRegistryListener;
import com.inmobi.grill.api.GrillException;
import com.inmobi.grill.api.StringList;
import com.inmobi.grill.api.ml.ModelMetadata;
import com.inmobi.grill.server.GrillServices;
import com.inmobi.grill.server.api.ml.MLModel;
import com.inmobi.grill.server.api.ml.MLService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * ML Service resrouce
 */
@Path("/ml")
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
public class MLServiceResource {
  public static final Log LOG = LogFactory.getLog(MLServiceResource.class);
  MLService mlService;


  private MLService getMlService() {
    if (mlService == null) {
      mlService = (MLService) GrillServices.get().getService(MLService.NAME);
    }
    return mlService;
  }

  /**
   *
   * @return
   */
  @GET
  @Path("trainers")
  public StringList getTrainerNames() {
    List<String> trainers = getMlService().getTrainerNames();
    StringList result = new StringList(trainers);
    return result;
  }

  @GET
  @Path("models/{algoName}")
  public StringList getModelsForAlgo(@PathParam("algoName") String algoName) throws GrillException {
    return new StringList(getMlService().getModels(algoName));
  }

  @GET
  @Path("models/{algoName}/{modelID}")
  public ModelMetadata getModelMetadata(@PathParam("algoName") String algoName,
                                         @PathParam("modelID") String modelID) throws GrillException {
    MLModel model = getMlService().getModel(algoName, modelID);
    if (model == null) {
      throw new NotFoundException("Model not found " + modelID + ", algo=" + algoName);
    }

    ModelMetadata meta = new ModelMetadata(
      model.getId(),
      model.getTable(),
      model.getTrainerName(),
      StringUtils.join(model.getParams(), ' '),
      model.getCreatedAt().toString(),
      getMlService().getModelPath(algoName, modelID)
    );
    return meta;
  }

  @POST
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Path("{algoName}/train")
  public String train(@PathParam("algoName") String algoName,
                      MultivaluedMap<String, String> form) throws GrillException {

    // Check if trainer is valid
    if (getMlService().getTrainerForName(algoName) == null) {
      throw new NotFoundException("Trainer for algo: " + algoName + " not found");
    }

    if (isBlank(form.getFirst("table"))) {
      throw new BadRequestException("table parameter is rquired");
    }

    String table = form.getFirst("table");

    if (isBlank(form.getFirst("-label"))) {
      throw new BadRequestException("label parameter is required");
    }

    // Check features
    List<String> featureNames = form.get("-feature");
    if (featureNames.size() < 1) {
      throw new BadRequestException("At least one feature is required");
    }

    List<String> trainerArgs = new ArrayList<String>();
    Set<Map.Entry<String, List<String>>> paramSet = form.entrySet();

    for  (Map.Entry<String, List<String>> e : paramSet) {
      String p = e.getKey();
      List<String> values = e.getValue();
      if ("algoName".equals(p) || "table".equals(p)) {
        continue;
      } else if ("-feature".equals(p)) {
        for (String feature : values) {
          trainerArgs.add("-feature");
          trainerArgs.add(feature);
        }
      } else if ("-label".equals(p)) {
        trainerArgs.add("-label");
        trainerArgs.add(values.get(0));
      } else {
        trainerArgs.add(p);
        trainerArgs.add(values.get(0));
      }
    }

    String modelId = getMlService().train(table, algoName, trainerArgs.toArray(new String[]{}));
    LOG.info("Trained table " + table + " with algo " + algoName
      + " params=" + trainerArgs.toString() + ", modelID=" + modelId);
    return modelId;
  }

  @DELETE @Path("clearModelCache")
  @Produces(MediaType.TEXT_PLAIN)
  public Response clearModelCache() {
    ModelLoader.clearCache();
    LOG.info("Cleared model cache");
    return Response.ok("Cleared cache", MediaType.TEXT_PLAIN_TYPE).build();
  }

}