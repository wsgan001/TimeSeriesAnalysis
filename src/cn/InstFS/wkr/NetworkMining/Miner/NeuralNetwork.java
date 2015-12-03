package cn.InstFS.wkr.NetworkMining.Miner;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import WaveletUtil.generateFeatures;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import cn.InstFS.wkr.NetworkMining.DataInputs.DataItems;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskElement;

@SuppressWarnings("deprecation")
public class NeuralNetwork implements IMinerTSA{
	private String inputFilePath;
	private DataItems predictItems;
	private int predictPeriod;
	private Date originDataEndTime;
	private TaskElement task;
	
	public NeuralNetwork(String inputFilePath,int predictPeriod,Date originDataEndTime,TaskElement task){
		this.predictPeriod=predictPeriod;
		this.inputFilePath=inputFilePath;
		this.task=task;
		this.originDataEndTime=originDataEndTime;
	}
	
	public NeuralNetwork(String inputFilePath,Date originDataEndTime,TaskElement task){
		this.inputFilePath=inputFilePath;
		this.task=task;
		this.originDataEndTime=originDataEndTime;
		this.predictPeriod=10;
	}
	
	public NeuralNetwork(){}
	
	public void TimeSeriesAnalysis(){
		try {
			generateFeatures features=new generateFeatures();
			features.setInputFile(inputFilePath);
			features.setOutputFilePrefix(inputFilePath.split("\\.")[0]);
			features.generateItems();
			MultilayerPerceptron classifier=new MultilayerPerceptron();
			File trainFile=new File("./configs/trian"+features.getOutputFilePrefix());
			File testFile=new File("./configs/test"+features.getOutputFilePrefix());
			classifier.setAutoBuild(true);
			classifier.setGUI(false);
			CSVLoader loader=new CSVLoader();
			loader.setFile(trainFile);
			Instances trainInstances=loader.getDataSet();
			trainInstances.setClassIndex(trainInstances.numAttributes()-1);
			int attrNum=trainInstances.numAttributes();
			int trianInstancesNum=trainInstances.numInstances();
			loader.setFile(testFile);
			Instances testInstances=loader.getDataSet();
			testInstances.setClassIndex(testInstances.numAttributes()-1);
			int testInstanceNum=testInstances.numInstances();
			double momentum=0.1;
			double learnRate=0.2;
			int seed=0;
			int trianTime=500;
			
			double minDistance=Double.MAX_VALUE;
			double distance=0.0;
			for(double learnRateIndex=learnRate;learnRateIndex<=0.4;learnRateIndex+=0.1){
				for(double momentumIndex=momentum;momentumIndex<=0.3;momentumIndex+=0.1){
					for(int seedIndex=seed;seedIndex<=1;seedIndex++){
						for(int timeIndex=trianTime;timeIndex<=1000;timeIndex+=100){
							classifier.setHiddenLayers("a");
							classifier.setLearningRate(learnRateIndex);
							classifier.setMomentum(momentumIndex);
							classifier.setSeed(seedIndex);
							classifier.setTrainingTime(timeIndex);
							classifier.buildClassifier(trainInstances);
							for(int i=0;i<testInstanceNum;i++){
								double forecastValue=classifier.classifyInstance(testInstances.get(i));
								double originValue=testInstances.instance(i).classValue();
								distance+=Math.abs(forecastValue-originValue);
							}
							System.out.println(" distance "+distance);
							if(distance<minDistance){
								minDistance=distance;
								seed=seedIndex;
								momentum=momentumIndex;
								learnRate=learnRateIndex;
								trianTime=timeIndex;
							}
							distance=0;
						}
					}
				}
			}
			System.out.println("min distance "+minDistance);
			classifier.setHiddenLayers("a");
			classifier.setLearningRate(learnRate);
			classifier.setMomentum(momentum);
			classifier.setSeed(seed);
			classifier.setTrainingTime(trianTime);
			classifier.buildClassifier(trainInstances);
			
			int[] autoCorrelationIndex=features.getAutoCorrelation();
			double[] items = new double[trianInstancesNum+testInstanceNum+predictPeriod];
			List<Date> time=new ArrayList<Date>();
			Calendar calendar=Calendar.getInstance();
			calendar.setTime(originDataEndTime);
			for(int i=0;i<(trianInstancesNum+testInstanceNum);i++){
				items[i]=features.getItems()[i];
			}
			
			
			Attribute[] attributes=new Attribute[attrNum];
			Instances instances=initializeAttribute(attributes);
			for(int i=(trianInstancesNum+testInstanceNum);i<(trianInstancesNum+testInstanceNum+predictPeriod);i++){
				double[] values=new double[instances.numAttributes()];
				for(int j=0;j<attrNum-1;j++){
					double value=items[i-autoCorrelationIndex[j]];
					values[j]=value;
				}
				calendar.add(Calendar.MILLISECOND, task.getGranularity()*1000);
				time.add(calendar.getTime());
				values[instances.numAttributes()-1]=0;
				Instance inst=new DenseInstance(1.0, values);
				instances.add(inst);
				double forecastValue=classifier.classifyInstance(instances.get(0));
				System.out.print(forecastValue+",");
				items[i]=forecastValue;
				instances.delete(0);
			}
			List<String> data=new ArrayList<String>();
			for(double value:items){
				data.add(value+"");
			}
			DataItems dataItems=new DataItems();
			dataItems.setData(data);
			dataItems.setTime(time);
			setPredictItems(dataItems);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private Instances initializeAttribute(Attribute[] attributes){
		for(int i=0;i<attributes.length-1;i++){
			String name="attr"+i;
			attributes[i]=new Attribute(name);
		}
		attributes[attributes.length-1]=new Attribute("value");
		FastVector<Attribute> attributesVector=new FastVector<Attribute>();
		for(Attribute attribute:attributes){
			attributesVector.addElement(attribute);
		}
		Instances instances=new Instances("forecastDataset", attributesVector, 0);
		instances.setClassIndex(instances.numAttributes()-1);
		return instances;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	

	

	public String getInputFilePath() {
		return inputFilePath;
	}

	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	
	public DataItems getPredictItems() {
		return predictItems;
	}

	public void setPredictItems(DataItems predictItems) {
		this.predictItems = predictItems;
	}

	public int getPredictPeriod() {
		return predictPeriod;
	}

	public void setPredictPeriod(int predictPeriod) {
		this.predictPeriod = predictPeriod;
	}

	@Override
	public DataItems getOutlies() {
		return null;
	}
	

}
