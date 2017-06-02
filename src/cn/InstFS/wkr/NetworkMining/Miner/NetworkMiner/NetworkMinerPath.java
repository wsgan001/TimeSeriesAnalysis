package cn.InstFS.wkr.NetworkMining.Miner.NetworkMiner;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import WaveletUtil.PointPatternDetection;
import cn.InstFS.wkr.NetworkMining.DataInputs.*;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.ForcastAlgorithm.ARIMATSA;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.ForcastAlgorithm.NeuralNetwork;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.OutlierAlgorithm.AnormalyDetection;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.OutlierAlgorithm.FastFourierOutliesDetection;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.OutlierAlgorithm.GaussianOutlierDetection;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.OutlierAlgorithm.MultidimensionalOutlineDetection;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.OutlierAlgorithm.PeriodBasedOutlierDetection;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.PeriodAlgorithm.ERPDistencePM;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.PeriodAlgorithm.averageEntropyPM;
import cn.InstFS.wkr.NetworkMining.Miner.Algorithms.SeriesStatisticsAlogorithm.SeriesStatistics;
import cn.InstFS.wkr.NetworkMining.Miner.Factory.MinerFactorySettings;
import cn.InstFS.wkr.NetworkMining.Miner.Factory.PathMinerFactory;
import cn.InstFS.wkr.NetworkMining.Miner.Factory.ProtocolAssMinerFactory;
import cn.InstFS.wkr.NetworkMining.Miner.Results.*;
import cn.InstFS.wkr.NetworkMining.Miner.Common.IsOver;
import cn.InstFS.wkr.NetworkMining.Miner.Common.TaskCombination;
import cn.InstFS.wkr.NetworkMining.Params.OMParams.OMFastFourierParams;
import cn.InstFS.wkr.NetworkMining.Params.OMParams.OMGaussianNodeParams;
import cn.InstFS.wkr.NetworkMining.Params.OMParams.OMGuassianParams;
import cn.InstFS.wkr.NetworkMining.Params.OMParams.OMMultidimensionalParams;
import cn.InstFS.wkr.NetworkMining.Params.OMParams.OMperiodBasedParams;
import cn.InstFS.wkr.NetworkMining.Params.PMParams.PMparam;
import cn.InstFS.wkr.NetworkMining.Params.ParamsAPI;
import cn.InstFS.wkr.NetworkMining.Params.ParamsOM;
import cn.InstFS.wkr.NetworkMining.Params.statistics.SeriesStatisticsParam;
import cn.InstFS.wkr.NetworkMining.ResultDisplay.UI.TaskProgress;
import cn.InstFS.wkr.NetworkMining.Results.MiningResultsFile;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningAlgo;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningObject;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskElement;
import cn.InstFS.wkr.NetworkMining.UIs.Utils.UtilsSimulation;
import cn.InstFS.wkr.NetworkMining.UIs.Utils.UtilsUI;
import common.ErrorLogger;
import common.ErrorTrace;

public class NetworkMinerPath implements INetworkMiner{
	private ScheduledExecutorService timer;
	private PathTimerTask timerTask;
	private MinerResults results;
	private IResultsDisplayer displayer;
	boolean isRunning;
	IsOver Over;
	
	private TaskCombination taskCombination;
	
	public NetworkMinerPath(TaskCombination taskCombination) {
		this.taskCombination=taskCombination;
		results = new MinerResults(this);
		Over=new IsOver();
	}
	
	@Override
	public boolean start() {
		MinerFactorySettings settings = PathMinerFactory.getInstance();
		MiningResultsFile resultsFile = new MiningResultsFile(MiningObject.fromString(taskCombination.getMiningObject()));
		if(resultsFile.hasFile(settings, taskCombination)) { // 已有挖掘结果存储，则不重新启动miner
			Over.setIsover(true);
			MinerResultsPath resultPath = (MinerResultsPath) resultsFile.file2Result();
			results.setRetPath(resultPath);

			TaskProgress taskProgress = TaskProgress.getInstance();
			taskProgress.increaseComplete();
			return false;
		}

		if (timer != null){
			UtilsUI.appendOutput(taskCombination.getName()+" -- already started");
			return false;
		}
		if (timerTask != null && timerTask.isRunning() == true){
			UtilsUI.appendOutput(taskCombination.getName()+" -- Still running");
			return false;
		}
		timer = Executors.newScheduledThreadPool(1);
		isRunning = true;
		timerTask = new PathTimerTask(taskCombination,results, displayer,Over);
		ScheduledFuture<?> future=timer.schedule(timerTask,10, TimeUnit.MILLISECONDS);
		try {
			future.get();
		} catch (Exception e) {

			ErrorLogger.log("Error-----------------------------------------------------------");
			ErrorLogger.log("TaskCombination "+ taskCombination.getName() +"任务挖掘出错");
			ErrorLogger.log("异常信息", ErrorTrace.getTrace(e));
			ErrorLogger.log("----------------------------------------------------------------");
			isRunning=false;
			Over.setIsover(false);

			/* 记录发生异常的taskCombination至任务进度 */
			TaskProgress taskProgress = TaskProgress.getInstance();
			taskProgress.increaseComplete();
			taskProgress.addErrTaskCombination(taskCombination);
			timer.shutdownNow();
		}
		return true;
	}

	@Override
	public boolean stop() {
		timer = null;
		if (timerTask != null && !timerTask.isRunning()){
			timerTask.cancel();
			timerTask = null;
		}
		
		isRunning = false;
		for(TaskElement task:taskCombination.getTasks())
			task.setRunning(isRunning);
		UtilsUI.appendOutput(taskCombination.getName() + " -- stopped");
		return true;
	}

	@Override
	public boolean isAlive() {
		return isRunning;
	}
	@Override
	public boolean isOver() {
		return Over.isIsover();
	}
	@Override
	public TaskElement getTask() {
		return taskCombination.getTasks().get(0);
	}
	@Override
	public MinerResults getResults() {
		return results;
	}
	@Override
	public void setResultsDisplayer(IResultsDisplayer displayer) {
		this.displayer = displayer;		
	}
}

class PathTimerTask extends TimerTask{
	MinerResults results;
	IResultsDisplayer displayer;
	private boolean isRunning = false;
	private IsOver isOver;
	private TaskCombination taskCombination;
	public PathTimerTask(TaskCombination taskCombination, MinerResults results, IResultsDisplayer displayer,
			IsOver isOver) {
		this.taskCombination = taskCombination;
		this.results = results;
		this.displayer = displayer;
		this.isOver=isOver;
	}
	
	public boolean isRunning(){
		return isRunning;
	}
	@Override
	
	public void run() {
		if (isRunning){
			System.out.println(taskCombination.getName()+ " --> Still Running");
			return;
		}
		results.setDateProcess(UtilsSimulation.instance.getCurTime());
		
		isRunning = true;
		// 读取数据
		PMDetect(taskCombination.getDataItems(),taskCombination.getTasks());
	}

	private void PMDetect(DataItems dataItems,List<TaskElement>tasks){
		DataItems oriDataItems=dataItems;
		
		//results.setInputData(oriDataItems);
		HashMap<String, MinerResultsPM> retPathPM=new HashMap<String, MinerResultsPM>();
		HashMap<String, MinerResultsOM> retPathOM=new HashMap<String, MinerResultsOM>();
		HashMap<String, MinerResultsFM> retPathFM = new HashMap<>();
		HashMap<String, DataItems> retPathOriDataItems = new HashMap<>();
		HashMap<String, MinerResultsStatistics> retPathStatistic = new HashMap<>();
		for(TaskElement task:tasks){
			dataItems=oriDataItems;
			
			List datas = new ArrayList();
			if (task.getMiningObject().equals(MiningObject.MiningObject_Traffic.toString())){
				datas = dataItems.getNonNumData();
				dataItems = DataPretreatment.changeDataToProb(dataItems);
				datas = dataItems.getProbMap();
			} else if (task.getMiningObject().equals(MiningObject.MiningObject_Times.toString())) {
				DataPretreatment.translateProbilityOfData(dataItems);//将跳转概率保存到文件中
				dataItems = DataPretreatment.changeDataToProb(dataItems); //计算每条路径的概率
				datas = dataItems.getProbMap();
			}
			
			Set<String>varset=dataItems.getVarSet();
			List<List<String>> seqs=new ArrayList<List<String>>();
			for(String item:varset){
				int row=0;
				List<String>seq=new ArrayList<String>();
				seq.add(item);
				
				Iterator iter = datas.iterator();
				while(iter.hasNext()){
					Map map = (Map) iter.next();
					if(map.containsKey(item)) {
						if(map.get(item) instanceof Double){	//用于区别Double路径概率与Integer流量
							int value=(int)((double)map.get(item)*1000);
//							double value = (double) map.get(item);
							seq.add(value+"");
							if(value == 0)
								continue;
							row++;
						}else if(map.get(item) instanceof Integer){
							int value = (int) map.get(item);
							seq.add(value+"");
							row++;
						}
					}else{
						seq.add("0");
					}
				}
				
				if(row<dataItems.getLength()*0.05)
					continue;
				seqs.add(seq);
			}

			for (List<String> seq: seqs){
				DataItems newItem=new DataItems();
				String name=seq.get(0);
				seq.remove(0);
				newItem.setData(seq);
				newItem.setTime(dataItems.getTime());
				//聚合
				newItem=DataPretreatment.aggregateData(newItem, task.getGranularity(), task.getAggregateMethod(),dataItems.isAllDataIsDouble());				
				retPathOriDataItems.put(name,newItem);
			
				switch (task.getMiningMethod()){					
					case MiningMethods_Statistics:
						SeriesStatisticsParam ssp = ParamsAPI.getInstance().getParamsStatistic().getSsp();
						MinerResultsStatistics retStatistics = new MinerResultsStatistics();
						SeriesStatistics seriesStatistics=new SeriesStatistics(newItem, ssp);
						seriesStatistics.statistics();
						setStatisticResults(retStatistics,seriesStatistics);
						retPathStatistic.put(name, retStatistics);
						break;
					case MiningMethods_PeriodicityMining:
						IMinerPM pmMethod = null;
						MinerResultsPM retPM = new MinerResultsPM();
						PMparam pMparam = ParamsAPI.getInstance().getParamsPeriodMiner().getPmparam();
						if (task.getMiningAlgo()!= null ) { // 不为空时，自定义算法，否则自动选择
							switch (task.getMiningAlgo()) {
								case MiningAlgo_averageEntropyPM:
									int dimension = Math.max(task.getDiscreteDimension(), newItem.getDiscretizedDimension());
									pmMethod = new averageEntropyPM(task, dimension,pMparam);//添加参数
									break;
								case MiningAlgo_ERPDistencePM:
									pmMethod=new ERPDistencePM(pMparam);
									break;
								default:
									throw new RuntimeException("方法不存在！");
							}
						} else { // 周期检测默认ERP
							pmMethod = new ERPDistencePM(pMparam);
						}
						pmMethod.setDataItems(newItem);
						pmMethod.setOriginDataItems(newItem);
						pmMethod.predictPeriod();
//						MinerResultsPM retPM = new MinerResultsPM();
						if(pmMethod.hasPeriod()){
							System.out.println("period:"+name+":"+pmMethod.getPredictPeriod()+":"+pmMethod.getFirstPossiblePeriod());
						}
						setPMResults(retPM, pmMethod);
						retPathPM.put(name, retPM);

						break;
					case MiningMethods_OutliesMining:
						IMinerOM omMethod = null;
						MinerResultsOM retOM = new MinerResultsOM();
						if (task.getMiningAlgo()!= null ) { // 不为空时，自定义算法，否则自动选择
							switch (task.getMiningAlgo()) {
								case MiningAlgo_GaussDetection:
									OMGuassianParams omGuassianParams = ParamsAPI.getInstance().getPom().getOmGuassianParams();
									omMethod = new AnormalyDetection(omGuassianParams,dataItems);
									retOM.setIslinkDegree(false);
									break;
								case MiningAlgo_FastFourier:
									OMFastFourierParams omFourierParams = ParamsAPI.getInstance().getPom().getOmFastFourierParams();
									omMethod=new FastFourierOutliesDetection(omFourierParams,dataItems);
									retOM.setIslinkDegree(false);
									break;
								case MiningAlgo_Muitidimensional:
									OMMultidimensionalParams omMultiParams = ParamsAPI.getInstance().getPom().getOmMultidimensionalParams();
									omMethod = new MultidimensionalOutlineDetection(omMultiParams,dataItems);
									retOM.setIslinkDegree(true);
									break;
								case MiningAlgo_NodeOutlierDetection:
									OMGaussianNodeParams omGaussianNodeParams = ParamsAPI.getInstance().getPom().getOmGaussianNodeParams();
									omMethod = new GaussianOutlierDetection(omGaussianNodeParams,dataItems);
									retOM.setIslinkDegree(false);
								case MiningAlgo_PeriodBasedOutlier:
									OMperiodBasedParams omPeriodParams = ParamsAPI.getInstance().getPom().getOMperiodBasedParams();
									omMethod = new PeriodBasedOutlierDetection(omPeriodParams,newItem, retPathPM.get(name));							
									retOM.setIslinkDegree(false);
									break;
								default:
									throw new RuntimeException("方法不存在！");
							}
						} else {
							if(retPathPM.get(name).getHasPeriod()){
								omMethod = new PeriodBasedOutlierDetection(newItem, retPathPM.get(name));							
								retOM.setIslinkDegree(false);
							}else{
								omMethod = new AnormalyDetection(newItem);
								retOM.setIslinkDegree(false);
							}
							
						}
						
						omMethod.TimeSeriesAnalysis();
						setOMResults(retOM, omMethod);
						retPathOM.put(name, retOM);
						break;
					case MiningMethods_PredictionMining:
						IMinerFM forecast = null;
						MinerResultsFM retFM = new MinerResultsFM();

						if (task.getMiningAlgo() != null) {
							switch (task.getMiningAlgo()) {
								case MiningAlgo_NeuralNetworkTSA:
									forecast =new NeuralNetwork(newItem, task,
											ParamsAPI.getInstance().getParamsPrediction().getNnp());
									break;
								case MiningAlgo_ARIMATSA:
									forecast =new ARIMATSA(task, newItem,
											ParamsAPI.getInstance().getParamsPrediction().getAp());
									break;
								default:
									throw new RuntimeException("方法不存在！");
							}
						} else {
							forecast=new NeuralNetwork(newItem, task,
									ParamsAPI.getInstance().getParamsPrediction().getNnp());
						}
						System.out.println(task.getTaskName()+" forecast start");
						forecast.TimeSeriesAnalysis();
						System.out.println(task.getTaskName()+" forecast over");
						retFM.setPredictItems(forecast.getPredictItems());
						retPathFM.put(name, retFM);
					default:
						break;
				}
			}		
		}
		HashMap<String, Double> pathProb = getPathProb(retPathOriDataItems);
		results.getRetPath().setPathProb(pathProb);
		results.getRetPath().setRetPM(retPathPM);
		results.getRetPath().setRetOM(retPathOM);
		results.getRetPath().setRetFM(retPathFM);
		results.getRetPath().setRetStatistic(retPathStatistic);
		results.getRetPath().setPathOriDataItems(retPathOriDataItems);
		results.getRetPath().setMaxAndMinValue();
		isRunning = false;
		isOver.setIsover(true);
		System.out.println(taskCombination.getName()+" over");
		if (displayer != null)
			displayer.displayMinerResults(results);
		/* 挖掘完成，保存结果文件 */
		MinerFactorySettings settings = PathMinerFactory.getInstance();
		MiningResultsFile newResultsFile = new MiningResultsFile(MiningObject.fromString(taskCombination.getMiningObject()));
		newResultsFile.result2File(settings, taskCombination, results.getRetPath());

		TaskProgress taskProgress = TaskProgress.getInstance();
		taskProgress.increaseComplete();
	}

	private void setStatisticResults(MinerResultsStatistics retStatistic,SeriesStatistics statistics){
		retStatistic.setMean(statistics.getMean());
		retStatistic.setStd(statistics.getStd());
		retStatistic.setComplex(statistics.getComplex());
		retStatistic.setSampleENtropy(statistics.getSampleEntropy());
	}
	
	private void setPMResults(MinerResults results,IMinerPM pmMethod, String path){
		results.getRetPath().getRetPM().get(path).setHasPeriod(pmMethod.hasPeriod());
		results.getRetPath().getRetPM().get(path).setPeriod(pmMethod.getPredictPeriod());
		results.getRetPath().getRetPM().get(path).setDistributePeriod(pmMethod.getItemsInPeriod());
		results.getRetPath().getRetPM().get(path).setMinDistributePeriod(pmMethod.getMinItemsInPeriod());
		results.getRetPath().getRetPM().get(path).setMaxDistributePeriod(pmMethod.getMaxItemsInPeriod());
		results.getRetPath().getRetPM().get(path).setFeatureValue(pmMethod.getMinEntropy());
		results.getRetPath().getRetPM().get(path).setFeatureValues(pmMethod.getEntropies());
		results.getRetPath().getRetPM().get(path).setFirstPossiblePeriod(pmMethod.getFirstPossiblePeriod());//找出第一个呈现周期性的周期
		results.getRetPath().getRetPM().get(path).setConfidence(pmMethod.getConfidence());
	}

	private void setPMResults(MinerResultsPM retPM, IMinerPM pmMethod){
		retPM.setHasPeriod(pmMethod.hasPeriod());
		retPM.setPeriod(pmMethod.getPredictPeriod());
		retPM.setDistributePeriod(pmMethod.getItemsInPeriod());
		retPM.setMinDistributePeriod(pmMethod.getMinItemsInPeriod());
		retPM.setMaxDistributePeriod(pmMethod.getMaxItemsInPeriod());
		retPM.setFeatureValue(pmMethod.getMinEntropy());
		retPM.setFeatureValues(pmMethod.getEntropies());
		retPM.setFirstPossiblePeriod(pmMethod.getFirstPossiblePeriod());//找出第一个呈现周期性的周期
		retPM.setConfidence(pmMethod.getConfidence());
	}

	private void setOMResults(MinerResultsOM retOM, IMinerOM omMethod){
		retOM.setOutlies(omMethod.getOutlies());    //查找异常
		retOM.setOutlinesSet(omMethod.getOutlinesSet()); //异常线段
		retOM.setOutDegree(omMethod.getOutDegree()); //异常度
		if(omMethod.getOutlies()!=null){
			DataItems outlies=omMethod.getOutlies();
			int outliesLen=outlies.getLength();
			int itemLen=taskCombination.getDataItems().getLength();
			if(Math.abs(outliesLen-itemLen)<=1){
				int confidence=0;
				for(String item:outlies.getData()){
					if(Double.parseDouble(item)>=80){
						retOM.setHasOutlies(true);
						confidence++;
					}
				}
				if(confidence!=0)
					retOM.setConfidence(confidence);
			}else{
				if(outlies.getLength()>0){
					retOM.setHasOutlies(true);
					retOM.setConfidence(outlies.getLength());
				}
			}
		}
	}

	/**
	 * 获取通信的主要路径
	 * @param pathDataItems 各条路径dataItems，各小时上的通信次数/流量和
	 * @return pathProb
	 */
	private HashMap<String, Double> getPathProb(HashMap<String, DataItems> pathDataItems){
		HashMap<String, Integer> total = new HashMap<>();
		HashMap<String, Double> pathProb = new HashMap<>();
		int totalTimes = 0;

		for (Map.Entry<String, DataItems> entry: pathDataItems.entrySet()) {
			String pathName = entry.getKey();
			DataItems di = entry.getValue();
			int times = 0;
			for (String value : di.getData()) {
				double val = Double.parseDouble(value);
				times += (int)val;
			}
			total.put(pathName, times);
			totalTimes += times;
		}

		for (Map.Entry<String, Integer> entry: total.entrySet()) {
			String pathName = entry.getKey();
			double value = entry.getValue();
			pathProb.put(pathName, value/totalTimes);
		}



		/*Iterator totalKeys = total.keySet().iterator();
		while (totalKeys.hasNext()){
			String key = (String)totalKeys.next();
			float value = total.get(key);
			pathProb.put(key, (double) (value/totalTimes));
		}*/
//		System.out.println("主要路径："+ primaryPath.toString() + "次数"+ total.get(primaryPath));

		/*HashMap<String, Double> total = new HashMap<>();
		HashMap<String, Double> pathProb = new HashMap<>();
		int size = 0;

		for (Map.Entry<String, DataItems> entry: pathDataItems.entrySet()) {
			String pathName = entry.getKey();
			DataItems di = entry.getValue();
			double times = 0;
			for (String value : di.getData()) {
				times += Double.parseDouble(value);
			}
			total.put(pathName, times);
			size = di.getData().size();
		}

		for (Map.Entry<String, Double> entry: total.entrySet()) {
			String pathName = entry.getKey();
			double value = entry.getValue();
			pathProb.put(pathName, value/size);
		}
*/

		return pathProb;
	}	
}

