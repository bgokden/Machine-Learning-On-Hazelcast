package com.hazelcast.machinelearning.MLAlgorithm.MLAlgorithmImpl;


import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.machinelearning.MLAlgorithm.MLAlgorithm;
import com.hazelcast.machinelearning.MLCommon.*;
import com.hazelcast.machinelearning.csv.IrisPlantDataReader;
import com.hazelcast.machinelearning.ToStringPrettyfier;
import com.hazelcast.machinelearning.model.IrisPlant;
import org.junit.Test;

import java.io.InputStream;
import java.util.*;

public class DistanceBasedClassificationAlgorithmTest {

    @Test
    public void shouldPassWhenSuccessIsHigh() throws Exception {
        Hazelcast.shutdownAll();
        // Prepare Hazelcast cluster
        HazelcastInstance hazelcastInstance = HelpfulMethods.buildCluster(4);

        System.out.println("Clusters ready");

        try {
            IrisPlantDataReader irisPlantDataReader = new IrisPlantDataReader();
            InputStream is = IrisPlantDataReader.class.getClassLoader().getResourceAsStream("bezdekIris.data");
            List<IrisPlant> irisPlants = irisPlantDataReader.read(is);
            Collection<ClassifiedFeature> plantsTrainData = new LinkedList<ClassifiedFeature>();
            Collection<UnclassifiedFeature> plantsPredictData = new LinkedList<UnclassifiedFeature>();
            int trainSize = (int) Math.round(0.7 * irisPlants.size());
            int predictSize = irisPlants.size() - trainSize;
            Set<Integer> predictDataIndex = new HashSet<Integer>();
            Random rd = new Random();
            for (int i = 0; i < predictSize; i++) {
                while ( predictDataIndex.add(rd.nextInt(irisPlants.size())) == false) {}
            }

            for (int i = 0; i < irisPlants.size(); i++) {
                if (!predictDataIndex.contains(i)) {
                    plantsTrainData.add(Reflections.getClassifiedFeatureFromObject(irisPlants.get(i)));
                }
            }
            MLAlgorithm algorithm = new DistanceBasedClassificationAlgorithm(hazelcastInstance);
            algorithm.train(plantsTrainData);

            int success = 0;

            for (Integer integer : predictDataIndex) {
                ClassifiedFeature classifiedFeature = Reflections.getClassifiedFeatureFromObject(irisPlants.get(integer.intValue()));
                //System.out.println("Class to predict: " + classifiedFeatureDatum.getClassification().toString());
                UnclassifiedFeature unclassifiedFeature = new UnclassifiedFeature(classifiedFeature.getFeatureMap());
                plantsPredictData.clear();
                plantsPredictData.add(unclassifiedFeature);
                Collection<Classification> classifications = algorithm.predict(plantsPredictData);
                //System.out.println("Result: " + ToStringPrettyfier.toString(classifications));
                boolean check = compareClassifications(classifiedFeature.getClassification(),classifications);
                System.out.println("Result: " + check);
                if (check == false) {
                    System.out.println("Class to predict: " + classifiedFeature.getClassification().toString());
                    System.out.println("Result: " + ToStringPrettyfier.toString(classifications));
                } else {
                    success++;
                }

                //break; //testing one value
            }
            double successRate = ((double) success) / predictDataIndex.size();
            System.out.println("Success Rate: " + successRate);

        } finally {
            // Shutdown cluster
            Hazelcast.shutdownAll();
        }
    }

    private static boolean compareClassifications(Classification classification, Collection<Classification> classifications) {
        for (Classification classification1 : classifications) {
            if (classification1.getClassification().equals(classification.getClassification())) {
                return true;
            }
            break;
        }
        return false;
    }

}