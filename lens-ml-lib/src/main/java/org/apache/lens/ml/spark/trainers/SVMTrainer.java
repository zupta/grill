package org.apache.lens.ml.spark.trainers;

import org.apache.lens.api.LensException;
import org.apache.lens.ml.spark.models.BaseSparkClassificationModel;
import org.apache.lens.ml.spark.models.SVMClassificationModel;
import org.apache.lens.ml.Algorithm;
import org.apache.lens.ml.TrainerParam;
import org.apache.spark.mllib.classification.SVMModel;
import org.apache.spark.mllib.classification.SVMWithSGD;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.rdd.RDD;

import java.util.Map;

@Algorithm(
  name = "spark_svm",
  description = "Spark SVML classifier trainer"
)
public class SVMTrainer extends BaseSparkTrainer {
  @TrainerParam(name = "minBatchFraction", help = "Fraction for batched learning",
  defaultValue = "1.0d")
  private double minBatchFraction;

  @TrainerParam(name = "regParam", help = "regularization parameter for gradient descent",
  defaultValue = "1.0d")
  private double regParam;

  @TrainerParam(name = "stepSize", help = "Iteration step size", defaultValue = "1.0d")
  private double stepSize;

  @TrainerParam(name = "iterations", help = "Number of iterations",
  defaultValue = "100")
  private int iterations;

  public SVMTrainer(String name, String description) {
    super(name, description);
  }

  @Override
  public void parseTrainerParams(Map<String, String> params) {
    minBatchFraction = getParamValue("minBatchFraction", 1.0);
    regParam = getParamValue("regParam", 1.0);
    stepSize = getParamValue("stepSize", 1.0);
    iterations = getParamValue("iterations", 100);
  }

  @Override
  protected BaseSparkClassificationModel trainInternal(String modelId, RDD<LabeledPoint> trainingRDD) throws LensException {
    SVMModel svmModel = SVMWithSGD.train(trainingRDD, iterations, stepSize, regParam, minBatchFraction);
    return new SVMClassificationModel(modelId, svmModel);
  }
}