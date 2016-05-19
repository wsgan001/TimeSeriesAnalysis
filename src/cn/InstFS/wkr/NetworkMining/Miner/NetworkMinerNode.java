package cn.InstFS.wkr.NetworkMining.Miner;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import WaveletUtil.PointPatternDetection;
import cn.InstFS.wkr.NetworkMining.DataInputs.DataItems;
import cn.InstFS.wkr.NetworkMining.DataInputs.DataPretreatment;
import cn.InstFS.wkr.NetworkMining.DataInputs.IReader;
import cn.InstFS.wkr.NetworkMining.DataInputs.PointSegment;
import cn.InstFS.wkr.NetworkMining.DataInputs.SegPattern;
import cn.InstFS.wkr.NetworkMining.DataInputs.WavCluster;
import cn.InstFS.wkr.NetworkMining.Params.ParamsPM;
import cn.InstFS.wkr.NetworkMining.Params.ParamsSM;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.AggregateMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.DiscreteMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningAlgo;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskElement;
import cn.InstFS.wkr.NetworkMining.UIs.Utils.UtilsSimulation;
import cn.InstFS.wkr.NetworkMining.UIs.Utils.UtilsUI;

public class NetworkMinerNode implements INetworkMiner{
	private Timer timer;
	private NodeTimerTask timerTask;
	private MinerResults results;
	private IResultsDisplayer displayer;
	boolean isRunning;
	Boolean isOver=false;
	
	private TaskCombination taskCombination;
	
	public NetworkMinerNode(TaskCombination taskCombination) {
		this.taskCombination=taskCombination;
		results = new MinerResults(this);
	}
	
	@Override
	public boolean start() {
		System.out.println("PanelShowResultsPM   timer starting");
		if (timer != null){
			UtilsUI.appendOutput(taskCombination.getName()+" -- already started");
			return false;
		}
		if (timerTask != null && timerTask.isRunning() == true){
			UtilsUI.appendOutput(taskCombination.getName()+" -- Still running");
			return false;
		}
		timer = new Timer();
		
		timerTask = new NodeTimerTask(taskCombination,results, displayer,timer,isOver);
		timer.scheduleAtFixedRate(timerTask, new Date(), UtilsSimulation.instance.getForcastWindowSizeInSeconds() * 1000);
		isRunning = true;
		return true;
	}

	@Override
	public boolean stop() {
		if (timer != null)
			timer.cancel();
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
		return isOver;
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

class NodeTimerTask extends TimerTask{
	MinerResults results;
	Timer timer;
	IResultsDisplayer displayer;
	private boolean isRunning = false;
	private Boolean isOver;
	private TaskCombination taskCombination;
	public NodeTimerTask(TaskCombination taskCombination, MinerResults results, IResultsDisplayer displayer,
			Timer timer,Boolean isOver) {
		this.taskCombination = taskCombination;
		this.results = results;
		this.displayer = displayer;
		this.timer=timer;
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
		// ��ȡ����
		Map<String, MinerResultsPM> retPmMap=new HashMap<String, MinerResultsPM>();
		PMDetect(taskCombination.getDataItems(),retPmMap,taskCombination.getTasks());
	}
	
	private void PMDetect(DataItems dataItems,Map<String, MinerResultsPM> retPmMap,List<TaskElement>tasks){
		DataItems oriDataItems=dataItems;
		for(TaskElement task:tasks){
			if(!task.getAggregateMethod().equals(AggregateMethod.Aggregate_NONE)){
				dataItems=DataPretreatment.aggregateData(oriDataItems, task.getGranularity(), task.getAggregateMethod(),
						!dataItems.isAllDataIsDouble());
			}
			
			if(!task.getDiscreteMethod().equals(DiscreteMethod.None)){
				dataItems=DataPretreatment.toDiscreteNumbers(dataItems, task.getDiscreteMethod(), task.getDiscreteDimension(),
						task.getDiscreteEndNodes());
			}
			
			int dimension = task.getDiscreteDimension();
			dimension = Math.max(task.getDiscreteDimension(), dataItems.getDiscretizedDimension());
			IMinerPM pmMethod=null;
			IMinerOM tsaMethod=null;
			switch (task.getMiningMethod()) {
			case MiningMethods_PeriodicityMining:
				if(task.getDiscreteMethod().equals(DiscreteMethod.None))
		    		dataItems=DataPretreatment.normalization(dataItems);
				if(task.getMiningAlgo().equals(MiningAlgo.MiningAlgo_averageEntropyPM)){
					pmMethod=new averageEntropyPM(task, dimension);
				}else if(task.getMiningAlgo().equals(MiningAlgo.MiningAlgo_ERPDistencePM)){
					pmMethod=new ERPDistencePM();
				}else{
					throw new RuntimeException("���������ڣ�");
				}
				pmMethod.setOriginDataItems(oriDataItems);
				pmMethod.setDataItems(dataItems);
				pmMethod.predictPeriod();
				setPMResults(results, pmMethod);
				break;
			case MiningMethods_OutliesMining:
				if(task.getMiningAlgo().equals(MiningAlgo.MiningAlgo_FastFourier)){
					tsaMethod=new FastFourierOutliesDetection(oriDataItems);
					((FastFourierOutliesDetection)tsaMethod).setAmplitudeRatio(0.7);
					((FastFourierOutliesDetection)tsaMethod).setVarK(2.5);
				}else if(task.getMiningAlgo().equals(MiningAlgo.MiningAlgo_GaussDetection)){
					tsaMethod=new AnormalyDetection(oriDataItems);
				}else if (task.getMiningAlgo().equals(MiningAlgo.MiningAlgo_TEOTSA)) {
					//tsaMethod=new TEOPartern(dataItems, 4, 4, 7);
					tsaMethod=new PointPatternDetection(oriDataItems,2,10);
					results.getRetOM().setIslinkDegree(true);
				}else{
					throw new RuntimeException("���������ڣ�");
				}
				tsaMethod.TimeSeriesAnalysis();
				setOMResults(results, tsaMethod);
				break;
			case MiningMethods_Statistics:
				SeriesStatistics seriesStatistics=new SeriesStatistics(oriDataItems);
				seriesStatistics.statistics();
				setStatisticResults(results,seriesStatistics);
				break;
			case MiningMethods_SequenceMining:
				PointSegment segment=new PointSegment(oriDataItems, 5);
				DataItems clusterItems=null;
				List<SegPattern> segPatterns=segment.getPatterns();
				if(task.getPatternNum()==0){
					clusterItems=WavCluster.SelfCluster(segPatterns,dataItems,8,task.getTaskName());
				}else{
					clusterItems=WavCluster.SelfCluster(segPatterns,dataItems, task.getPatternNum(),task.getTaskName());
				}
				ParamsSM paramsSM=(ParamsSM)task.getMiningParams();
				SequencePatternsDontSplit sequencePattern=new SequencePatternsDontSplit();
				sequencePattern.setDataItems(clusterItems);
				sequencePattern.setTask(task);
				sequencePattern.setWinSize(paramsSM.getSizeWindow());
				sequencePattern.setThreshold(paramsSM.getMinSupport());
				sequencePattern.setStepSize(paramsSM.getStepWindow());
				sequencePattern.printClusterLabelTOLines(clusterItems, dataItems);
				sequencePattern.patternMining();
				setFrequentResults(results, sequencePattern);
				break;
			default:
				break;
			}
		}
		isRunning = false;
		isOver=true;
		
		if (displayer != null)
			displayer.displayMinerResults(results);
		timer.cancel();
	}
	
	private void setPMResults(MinerResults results,IMinerPM pmMethod){
		results.getRetNode().getRetPM().setHasPeriod(pmMethod.hasPeriod());
		results.getRetNode().getRetPM().setPeriod(pmMethod.getPredictPeriod());
		results.getRetNode().getRetPM().setDistributePeriod(pmMethod.getItemsInPeriod());
		results.getRetNode().getRetPM().setMinDistributePeriod(pmMethod.getMinItemsInPeriod());
		results.getRetNode().getRetPM().setMaxDistributePeriod(pmMethod.getMaxItemsInPeriod());
		results.getRetNode().getRetPM().setFeatureValue(pmMethod.getMinEntropy());
		results.getRetNode().getRetPM().setFeatureValues(pmMethod.getEntropies());
		results.getRetNode().getRetPM().setFirstPossiblePeriod(pmMethod.getFirstPossiblePeriod());//�ҳ���һ�����������Ե�����
		results.getRetNode().getRetPM().setConfidence(pmMethod.getConfidence());
	}
	
	private void setOMResults(MinerResults results,IMinerOM tsaMethod){
		results.getRetNode().getRetOM().setOutlies(tsaMethod.getOutlies());    //�����쳣
		if(tsaMethod.getOutlies()!=null){
			DataItems outlies=tsaMethod.getOutlies();
			if(outlies.getLength()==taskCombination.getDataItems().getLength()){
				int confidence=0;
				for(String item:outlies.getData()){
					if(Double.parseDouble(item)>=8){
						results.getRetNode().getRetOM().setHasOutlies(true);
						confidence++;
					}
				}
				if(confidence!=0)
					results.getRetNode().getRetOM().setConfidence(confidence);
			}else{
				if(outlies.getLength()>0){
					results.getRetOM().setHasOutlies(true);
					results.getRetOM().setConfidence(outlies.getLength());
				}
			}
		}
	}
	
	private void setStatisticResults(MinerResults results,SeriesStatistics statistics){
		results.getRetNode().getRetStatistics().setMean(statistics.getMean());
		results.getRetNode().getRetStatistics().setStd(statistics.getStd());
		results.getRetNode().getRetStatistics().setComplex(statistics.getComplex());
		results.getRetNode().getRetStatistics().setSampleENtropy(statistics.getSampleEntropy());
	}
	
	private void setFrequentResults(MinerResults results,SequencePatternsDontSplit sequencePattern){
		List<ArrayList<String>> patterns=sequencePattern.getPatterns();
		if(sequencePattern.isHasFreItems()){
			results.getRetSM().setPatters(patterns);
			results.getRetSM().setHasFreItems(true);
		}else{
			results.getRetSM().setHasFreItems(false);
		}
	}
}
