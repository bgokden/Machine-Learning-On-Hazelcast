package com.hazelcast.machinelearning.MLAlgorithm.MapperImpl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.machinelearning.MLCommon.Classification;
import com.hazelcast.machinelearning.MLCommon.Feature;
import com.hazelcast.machinelearning.MLCommon.IFeatureComparator;
import com.hazelcast.machinelearning.MLCommon.UnclassifiedFeature;
import com.hazelcast.mapreduce.Context;
import com.hazelcast.mapreduce.Mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by berkgokden on 9/16/14.
 */
public class DistanceBasedClassificationAlgorithmMapper implements Mapper<Feature, Classification, Feature, Classification>, HazelcastInstanceAware {

    private transient HazelcastInstance hazelcastInstance;
    private Collection<UnclassifiedFeature> data;
    private IFeatureComparator comparator;

    public DistanceBasedClassificationAlgorithmMapper(Map<String, Object> options, Collection<UnclassifiedFeature> data) {
        this.data = data;
        this.comparator = (IFeatureComparator) options.get("comparator");
    }

    @Override
    public void map(Feature key, Classification value, Context<Feature, Classification> context) {
        for (UnclassifiedFeature unclassifiedFeature : this.data) {

            //double distance = unclassifiedFeature.getFeature().distanceTo(key);
            double distance = this.comparator.compare(unclassifiedFeature.getFeature(), key);
            double weight = Double.MAX_VALUE;
            if (distance != 0) {
                weight = value.getConfidence() * unclassifiedFeature.getConfidence() / distance;
            }
            context.emit(unclassifiedFeature.getFeature(), new Classification(value.getClassification(), weight));
            System.out.println("New map:"+value.getClassification().toString());
        }

    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

}
