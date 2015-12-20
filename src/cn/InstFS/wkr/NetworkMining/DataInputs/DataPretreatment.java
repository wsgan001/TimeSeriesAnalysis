package cn.InstFS.wkr.NetworkMining.DataInputs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.security.acl.Owner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;



//import org.hamcrest.Matcher;
//import weka.clusterers.SimpleKMeans;
//import weka.core.Instances;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.AggregateMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.DiscreteMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskElement;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskRange;
import cn.InstFS.wkr.NetworkMining.UIs.MainFrame;

public class DataPretreatment {
	
	//获取给定毫秒数之后的时间
	private static Date getDateAfter(Date curTime, int milliSeconds){
		Calendar cal = Calendar.getInstance();
		try{
		cal.setTime(curTime);
		}catch(Exception e){
			System.out.println("");
		}
		cal.add(Calendar.MILLISECOND, milliSeconds);
		return cal.getTime();
	}
	/**
	 * 数据聚合
	 * @param valsArrayD 数据数组
	 * @param method 聚合方法
	 * @return 聚合后的数据
	 */
	static private Double aggregateDoubleVals(Double[] valsArrayD,
			AggregateMethod method) {
		int len = valsArrayD.length;
		double []valsArray = new double[len];
		for (int i = 0; i < len; i ++)
			valsArray[i] = valsArrayD[i];
		switch (method) {
		case Aggregate_MAX:
			return StatUtils.max(valsArray);
		case Aggregate_MEAN:
			return StatUtils.mean(valsArray);
		case Aggregate_MIN:
			return StatUtils.min(valsArray);
		case Aggregate_SUM:
			return StatUtils.sum(valsArray);
		default:
			return 0.0;
		}
	}
	
	/**
	 * 判断val所代表的值处于哪个区间（0~len-1）中，并以字符串形式返回这个序号
	 * @param discreteNodes	端点值
	 * @param len	端点数（为了避免每次调用函数时都提取一下数组长度）
	 * @param val	值
	 * @return
	 */
	private static String getIndexOfData(int len, double val, Double[] discreteNodes){
		if (val < discreteNodes[0])
			return discreteNodes[0]+"";
		if (val < discreteNodes[1])
			return discreteNodes[0]+"";
		for (int i = 1; i < len - 1; i ++)
			if (val >= discreteNodes[i] && val < discreteNodes[i+1])
				return discreteNodes[i]+"";
		return discreteNodes[len-1]+"";
	}
	
	/**
	 * 将路径信息转换为路径概率信息  即每个时间段中每条路径经过的概率
	 * @param dataItems
	 * @return
	 */
	public static DataItems changeDataToProb(DataItems dataItems){
		DataItems dataOut=new DataItems();
		dataOut.setIsAllDataDouble(dataItems.getIsAllDataDouble());
		int len = dataItems.getLength();
		if (dataItems == null ||  len == 0)
			return dataOut;
		List<Date> times = dataItems.getTime();
		List<Map<String, Integer>> datas=dataItems.getNonNumData();
		Iterator<Map.Entry<String, Integer>> mapIter=null;
		for(Map<String, Integer> map:datas){
			Map<String, Double> probMap=new HashMap<String, Double>();
			mapIter=map.entrySet().iterator();
			while(mapIter.hasNext()){
				Entry<String,Integer> entry=mapIter.next();
				double pathPossi=getPathProb(map,entry.getKey());
				probMap.put(entry.getKey(), pathPossi);
			}
			dataOut.getProbMap().add(probMap);
		}
		dataOut.setTime(times);
		dataOut.setProb(dataItems.getProb());
		dataOut.setVarSet(dataItems.getVarSet());
		return dataOut;
	}
	
	/**
	 * 根据历史路径信息，计算给定路径出现的概率
	 * @param map 路径的hashMap 历史路径
	 * @param path 要计算出现概率的路径
	 * @return 该路径的概率
	 */
	private static double getPathProb(Map<String, Integer> map,String path){
		int sum=sumMap(map);
		String[] pathNodes=path.split(","); //路径上的节点
		double possiblity=1.0;              //这条路径的概率
		double[] eachNodePossi=new double[pathNodes.length-1];   //路径每个节点出现概率  即先验概率P(X)
		double[] neighborNodePossi=new double[pathNodes.length-1];  //相邻节点出现概率  即联合概率P(X,Y)
		double[] conditionalPossi=new double[pathNodes.length-1];  //条件概率 即P(Y|X)
		for(int i=0;i<pathNodes.length-1;i++){
			int nodeNum=containsNodesPathNum(map,pathNodes[i]);
			eachNodePossi[i]=(nodeNum*1.0)/sum;
		}
		possiblity*=eachNodePossi[0];
		//路径概率计算服从一阶Markov模型 所以 P(A,B,C,D,E)=P(A)*P(B|A)*P(C|B)*P(D|C)*P(E|D)
		for(int i=0;i<pathNodes.length-1;i++){
			int nodeNum=containsNodesPathNum(map,pathNodes[i]+","+pathNodes[i+1]);
			neighborNodePossi[i]=(nodeNum*1.0)/sum;
			conditionalPossi[i]=neighborNodePossi[i]/eachNodePossi[i];
			possiblity*=conditionalPossi[i];
		}
		return possiblity;
	}
	
	/**
	 * 判断指定路径节点在所有路径中出现的次数
	 * @param map 路径 map
	 * @param node 指定的路径节点
	 * @return 节点在路径中出现
	 */
	private static int containsNodesPathNum(Map<String, Integer>map,String node){
		int pathsNum=0;
		String[] nodes=node.split(",");
		boolean isContain=false;
		Iterator<Map.Entry<String, Integer>> iterator=map.entrySet().iterator();
		while(iterator.hasNext()){
			Entry<String, Integer>entry=iterator.next();
			String key=entry.getKey();  //路径
			int value=entry.getValue();
			String[] pathNodes=key.split(",");   
			
			//测试路径key中是否包含给定的节点@node
			isContain=false;
			for(int i=0;i<pathNodes.length-nodes.length+1;i++){
				for(int j=0;j<nodes.length;j++){
					if(!pathNodes[i+j].equals(nodes[j])){
						isContain=false;
						break;
					}
					isContain=true;
				}
				if(isContain){
					break;
				}
			}
			if(isContain){
				pathsNum+=value;
			}
		}
		return pathsNum;
	}
	
	/**
	 * 判断所有路径的条数
	 * @param map 存储路径的hashMap
	 * @return 路径总条数
	 */
	private static int sumMap(Map<String, Integer> map){
		Iterator<Map.Entry<String, Integer>> iter=map.entrySet().iterator();
		int sum=0;
		while (iter.hasNext()) {
			sum+=iter.next().getValue();
		}
		return sum;
	}
	
	//datItems在相同的时间粒度上的聚合
	public static DataItems aggregateData(DataItems di,int granularity,
			AggregateMethod method,boolean isDiscreteOrNonDouble){	
		
		DataItems dataOut = new DataItems();
		dataOut.setIsAllDataDouble(di.getIsAllDataDouble());
		int len = di.getLength();
		if (di == null || di.getTime() == null || di.getData() == null || len == 0)
			return dataOut;
		
		List<Date> times = di.getTime();
		List<String> datas = di.getData();
		Date t1 = times.get(0);
		Date t2 = getDateAfter(t1, granularity * 1000);
		Map<String,Integer> valsStr = new HashMap<String, Integer>(); // 字符串的聚合结果
		Set<String> varSet=new HashSet<String>();                     //字符串的集合
		List<Double> vals = new ArrayList<Double>(); 	// 数值的聚合结果
		
//		Date t = t1;									// 聚合后的时间点
		int i=0;
		for(;!t1.after(times.get(times.size()-1));t1=t2,t2 = getDateAfter(t2, granularity * 1000))
		{
			
			while(i<times.size()&&times.get(i).before(t2))
			{
				if(i>0&&times.get(i).before(times.get(i-1)))
					JOptionPane.showMessageDialog(MainFrame.topFrame, "序列未排序");
				if (isDiscreteOrNonDouble){	// 离散值或字符串
					if(valsStr.containsKey(datas.get(i))){
						int originValue=valsStr.get(datas.get(i));
						valsStr.remove(datas.get(i));
						int newValue=originValue+1;
						valsStr.put(datas.get(i), newValue);
					}else{
						valsStr.put(datas.get(i), 1);
					}
					varSet.add(datas.get(i));
				}
				else{			// 若为连续值，则加至vals中，后续一起聚合
					try{
						double data= Double.parseDouble(datas.get(i));
						vals.add(data);
					}catch(Exception e){}					
				}
				i++;
			}
			//一个时间粒度内的值读完了，则建立新的值
			if(isDiscreteOrNonDouble){
//				StringBuilder sb = new StringBuilder();
//				for (String valStr : valsStr)
//					sb.append(valStr+" ");
//				if (sb.length() > 0)
//					dataOut.add1Data(t1, sb.toString().trim());
//				else
//					dataOut.add1Data(t1, "");
				dataOut.add1Data(t1, valsStr);
				valsStr.clear();
			}else{
				Double[] valsArray = vals.toArray(new Double[0]);
				
				if (valsArray.length > 0){
					Double val = aggregateDoubleVals(valsArray, method);
					dataOut.add1Data(t1, val.toString());
				}
				else
				{
					dataOut.add1Data(t1,String.valueOf(0.0));
				}
				vals.clear();
			}
		}			
		if(varSet.size()!=0){
			dataOut.setVarSet(varSet);
		}
		return dataOut;
	}
	
	/**
	 * 根据discreteMethod,对该数据进行离散化
	 * @param discreteMethod	离散化方法
	 * @param numDims			离散后的维数
	 * @param endNodes			自定义端点，仅在自定义离散化方法条件下有效
	 * @return
	 */
	public static DataItems toDiscreteNumbers(DataItems dataItems,DiscreteMethod discreteMethod, int numDims, String endNodes){
		DataItems newDataItems = null;
		switch (discreteMethod) {
		case 各区间数值范围相同:
			newDataItems = toDiscreteNumbersAccordingToMean3Sigma(dataItems,numDims);
			break;
		case 各区间数据点数相同:
			newDataItems = toDiscreteNumbersAccordingToPercentile(dataItems,numDims);
			break;
		case 自定义端点:
			newDataItems = toDiscreteNumbersAccordingToCustomNodes(dataItems,endNodes);
			break;
		case None://不做离散化,直接返回
		default:
			newDataItems = dataItems;
		}
		return newDataItems;
		
	}
	
	/**
	 * 根据用户指定的节点进行离散化
	 * @param endNodes 用户指定的界点
	 * @return 离散化后的DataItems
	 */
	private static DataItems toDiscreteNumbersAccordingToCustomNodes(DataItems dataItems,String endNodes){
		DataItems newDataItems = new DataItems();
		newDataItems.setIsAllDataDouble(dataItems.getIsAllDataDouble());
		newDataItems.setVarSet(dataItems.getVarSet());
		if (endNodes == null || endNodes.length() == 0)
			return newDataItems;
		String []nodesStr = endNodes.split(",");
		int numDims = nodesStr.length;
		Double[] discreteNodes=new Double[numDims];
		for (int i = 0; i < numDims; i ++)
			discreteNodes[i] = Double.parseDouble(nodesStr[i]);
		newDataItems.setDiscreteNodes(discreteNodes);
		int len = dataItems.getLength();
		for (int i = 0; i < len; i ++){		
			if(i==220){
				System.out.println("here");
			}
			if(dataItems.isAllDataIsDouble()){
		    	newDataItems.add1Data(dataItems.getTime().get(i),
		        getIndexOfData(numDims,Double.parseDouble(dataItems.getData().get(i)),newDataItems.getDiscreteNodes()));
			}else{
				Map<String, Integer> map=dataItems.getNonNumData().get(i);
				Map<String, Integer> discreMap=new HashMap<String, Integer>();
				Iterator<Map.Entry<String, Integer>> iter=map.entrySet().iterator();
				while(iter.hasNext()){
					Map.Entry<String, Integer>entry=iter.next();
					discreMap.put(entry.getKey(),
					(int) Double.parseDouble(getIndexOfData(numDims,entry.getValue(),newDataItems.getDiscreteNodes())));
				}
				newDataItems.add1Data(dataItems.getTime().get(i), discreMap);
			}
		}
		return newDataItems;
	}
	/**
	 * 将区间[mean-3*sigma，mean+3*sigma]平均划分为numDims个区间，离散化得到的dataItems。
	 * @param numDims	离散后的取值数
	 * @return	已经离散化的dataItems数据
	 */
	private static DataItems toDiscreteNumbersAccordingToMean3Sigma(DataItems dataItems,int numDims){
		DataItems newDataItems=new DataItems();
		newDataItems.setIsAllDataDouble(dataItems.getIsAllDataDouble());
		Double minVal = Double.MAX_VALUE;
		Double maxVal = Double.MIN_VALUE;
		
		List<String> datas=dataItems.getData();
		int length=datas.size();
		
		// 首先，判断取值个数，如果仅为20个值以下，则直接将值作为离散值
		boolean isDiscrete = dataItems.isDiscrete();
		
		// 直接当离散值处理
		if (!isDiscrete){
			if(!dataItems.isAllDataIsDouble()){
				throw new RuntimeException("非数值型数据不能离散化");
			}
			DescriptiveStatistics statistics=new DescriptiveStatistics();
			double mean = 0.0;
			double std = 0.0;
			for(String data:datas){
				statistics.addValue(Double.parseDouble(data));
			}
			mean=statistics.getMean();
			std=statistics.getStandardDeviation();
			minVal=mean-4*std;
			maxVal=mean+4*std;
			
			Double[] discreteNodes=new Double[numDims];
			for (int i = 0; i < numDims; i ++){
				discreteNodes[i] = minVal + (maxVal - minVal) * i / numDims;
			}
			for (int i = 0; i < length; i ++){
				DataItem item = dataItems.getElementAt(i);
				Double val = null;
				val = Double.parseDouble(item.getData());
				if (val != null){
					String ind = getIndexOfData(numDims,val,discreteNodes);
					newDataItems.add1Data(item.getTime(), ind);
				}
			}
			newDataItems.setDiscreteNodes(new Double[numDims]);
			return newDataItems;
		}else{
			return dataItems;
		}
		
	}
	/**
	 * 根据分位点来进行离散化
	 * @param numDims
	 * @return
	 */
	private static DataItems toDiscreteNumbersAccordingToPercentile(DataItems dataItems,int numDims){
		
		DataItems newDataItems=new DataItems();
		List<String> datas=dataItems.getData();
		int length=datas.size();
		
		boolean isDiscrete = dataItems.isDiscrete();
		if (isDiscrete){	// 直接当离散值处理
			return dataItems;
		}else{				// 连续值，需要进行离散化
			double step = 1.0 / numDims * length; 
			int ind = 0;
			int ind_step = (int) ((ind + 1) * step - 1);
			Double[] discreteNodes = new Double[numDims];
			DataItems sortedItems = DataItems.sortByDoubleValue(dataItems);
			discreteNodes[0] = Double.parseDouble(datas.get(0));
			if(!dataItems.isAllDataIsDouble()){
				throw new RuntimeException("非数值型数据不能离散化");
			}
			datas=sortedItems.getData();
			for (int i = 0; i < length; i ++){
				Double val = Double.parseDouble(datas.get(i));
				if (i > ind_step){
					discreteNodes[ind + 1] = val;
					ind ++;
					ind_step = (int) ((ind + 1) * step - 1);
				}				
			}

			for (int i = 0; i < length; i ++){				
				newDataItems.add1Data(sortedItems.getTime().get(i),
						getIndexOfData(numDims, Double.parseDouble(datas.get(i)),discreteNodes));
			}
			newDataItems.setDiscreteNodes(discreteNodes);
			return newDataItems;
		}
	}
	public static DataItems toDiscreteNumbersAccordingToWaveform(DataItems dataItems,TaskElement task)
	{
		DataItems result = new DataItems(); 
		ArrayList<ArrayList<Double>> clustersCenter = new ArrayList<ArrayList<Double>>();
		int size =0;
		String fileName= task.getTaskRange().toString();
		Pattern p= Pattern.compile(".*protocol\\s*=(\\d{3}).*");
		Matcher match =p.matcher(task.getFilterCondition());
		match.find();
		fileName+=match.group(1)+task.getGranularity()+".txt";
		try
		{
			InputStreamReader ir = new InputStreamReader (new FileInputStream(fileName),"UTF-8");
			BufferedReader br    = new BufferedReader ( ir);
			String curLine =null;
		    size = Integer.valueOf(br.readLine());
			
			while((curLine = br.readLine())!=null)
			{
				String []num = curLine.split(" ");
				ArrayList<Double> center = new ArrayList<Double>();
				for(int i = 0 ;i<num.length;i++)
				{
					center.add(Double.valueOf(num[i]));
				}
				clustersCenter.add(center);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		System.out.println("god");
		for(int i=0;i<dataItems.getLength()&&(i+size-1)<dataItems.getLength();i+=size)
		{
			ArrayList <Double> vector = new ArrayList<Double>();
			DataItem dataItem = new DataItem();
			dataItem.setTime(dataItems.getElementAt(i).getTime());
			for(int j=i;j<i+size;j++)
			{
				vector.add(Double.valueOf(dataItems.getElementAt(j).getData()));
			}
			Double min = Double.MAX_VALUE;
			int index  =0;
			if(manhattanDistance(vector,clustersCenter.get(0))>0.1)
			{
				for(int j=0;j<clustersCenter.size();j++)
				{
					Double tmp = manhattanDistance(vector,clustersCenter.get(j));
					if(tmp<min)
					{
						min = tmp;
						index = j;
					}
				}
			}
			
//				System.out.println(i+" "+vector);
			dataItem.setData(String.valueOf(index));
			result.add1Data(dataItem);
		}
		return result;
	}
	public static void trainAll()
	{
		TaskElement task = new TaskElement();
		task.setSourcePath("E:/javaproject/NetworkMiningSystem/smtpPcap");
		task.setDataSource("Text");
		task.setMiningObject("traffic");
		for(TaskRange taskRange:TaskRange.values())
		{
//			System.out.println(taskRange);
			task.setTaskRange(taskRange);
			for(int procol=402;procol<=410;procol++)
			{
				task.setFilterCondition("protocol="+procol);
				task.setGranularity(3600);
				train(task,200,6);
			}
			
//			task.setDateStart(dateStart);
//			task.setDateEnd(dateEnd);
//			task.set
		}
	}
	public static void train(TaskElement task,double threshold,int size)
	{
		String fileName= task.getTaskRange().toString();
		Pattern p= Pattern.compile(".*protocol\\s*=(\\d{3}).*");
		Matcher match =p.matcher(task.getFilterCondition());
		match.find();
		fileName+=match.group(1)+task.getGranularity()+".txt";
		
		Calendar cal=Calendar.getInstance();
		cal.set(2014, 9, 1, 0, 0, 0);
		Date startDate=cal.getTime();
		cal.add(Calendar.DAY_OF_YEAR,100);
		Date endDate=cal.getTime();
		switch(task.getTaskRange())
		{
		case NodePairRange:
		{
			ArrayList<DataItems> list = new ArrayList<DataItems>();
//			ArrayList <String> ips = new ArrayList<String> ();
//			for(int i=1;i<=10;i++)
//				for(int j=1;j<=6;j++)
//					ips.add("10.0."+i+"."+j);
//			for(int i =0;i<ips.size();i++)
//				for(int j=i+1;j<ips.size();j++)
//				{
//					String ip[] = new String[]{ips.get(i),ips.get(j)};
//					IReader reader = new nodePairReader(task,ip);
//					DataItems tmp = reader.readInputByText();
//					DataItems dataItems=new DataItems();
//					for(int k=0;k<tmp.getLength();k++)
//					{
//						DataItem dataItem = tmp.getElementAt(k);
//						dataItem.setData(String.valueOf(Double.valueOf(dataItem.getData())/2));
//						dataItems.add1Data(dataItem);
//					}
//					
//					dataItems=DataPretreatment.aggregateData(dataItems,3600,AggregateMethod.Aggregate_MEAN,false);
//					System.out.println("i "+i+" j "+j);
//					System.out.println("list add "+dataItems.getLength());
//					list.add(dataItems);
//				}
			nodePairReader reader= new nodePairReader(task,new String[2]);
			String condition[]=new String[0];
			Map<String,DataItems> ipPairItems = new HashMap<String,DataItems>();
			
			ipPairItems=reader.readAllPairBetween(startDate,endDate);
			for(Map.Entry<String, DataItems> entry:ipPairItems.entrySet())
			{
				
				DataItems tmp = entry.getValue();
				DataItems dataItems=new DataItems();
				for(int k=0;k<tmp.getLength();k++)
				{
					DataItem dataItem = tmp.getElementAt(k);
					dataItem.setData(String.valueOf(Double.valueOf(dataItem.getData())/2));
					dataItems.add1Data(dataItem);
				}
				dataItems=DataPretreatment.aggregateData(dataItems,3600,AggregateMethod.Aggregate_MEAN,false);
				list.add(dataItems);
			}
			System.out.println("listsize "+list.size());
			ArrayList<ArrayList<Double>>instances=null;
			runTrain(list,instances,fileName,200,size);
			break;
		}
		
		case SingleNodeRange:
		{
			ArrayList<DataItems> list = new ArrayList<DataItems>();
			ArrayList <String> ips = new ArrayList<String> ();
			for(int i=1;i<=10;i++)
				for(int j=1;j<=6;j++)
					ips.add("10.0."+i+"."+j);
			for(int i =0;i<ips.size();i++)
				{
					String ip[] = new String[]{ips.get(i)};
					nodePairReader reader = new nodePairReader(task,ip);
					DataItems tmp = reader.readInputBetween(startDate,endDate);
					DataItems dataItems=new DataItems();
					for(int k=0;k<tmp.getLength();k++)
					{
						DataItem dataItem = tmp.getElementAt(k);
						
						dataItems.add1Data(dataItem);
					}
					
					dataItems=DataPretreatment.aggregateData(dataItems,3600,AggregateMethod.Aggregate_MEAN,false);
					System.out.println("i "+i);
					System.out.println("list add "+dataItems.getLength());
					list.add(dataItems);
				}
			ArrayList<ArrayList<Double>>instances=null;
			runTrain(list,instances,fileName,400,size);
		}
		case WholeNetworkRange:break;
		default: break;
		}
	}
	private static void runTrain(ArrayList <DataItems> list,ArrayList<ArrayList<Double>>instances,String fileName,double threshold,int windowSize)
	{
		int windowSizeMin = windowSize;
		int windowSizeMax =  windowSize;
		ArrayList<Double> instance;
		instances = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> clusterCenter = null;
		ArrayList<ArrayList<Integer>> clusersInstanceList=null ;
		
//		ArrayList<ArrayList<ArrayList<Double>>> cluserList = new ArrayList<ArrayList<ArrayList<Double>>>();
		/**
		 * 每个窗口训练一次得到最佳窗口
		 */
		int min= Integer.MAX_VALUE;
		int optsize = windowSizeMin;
		for(int size = windowSizeMin;size<=windowSizeMax;size++)
		{
			System.out.println("windowsize "+size);
			instances.clear();
			for(int i=0;i<list.size();i++)
			{
				for(int j=0;j<list.get(i).getLength()&&(j+size-1)<list.get(i).getLength();j++)
				{
					instance = new ArrayList<Double>();
					for(int k=j;k<j+size;k++)
						instance.add(Double.valueOf(list.get(i).getElementAt(k).getData()));
					instances.add(instance);
				}
			}
			Collections.shuffle(instances);
			System.out.println("ins "+instances.size());
			ArrayList<ArrayList<Double>> tmpclusterCenter = new ArrayList<ArrayList<Double>>();
			clusersInstanceList = new ArrayList<ArrayList<Integer>>();
			int tmp = singlePathCluster(instances,tmpclusterCenter,clusersInstanceList,threshold);
			if(tmp<min)
			{
				min = tmp;
				clusterCenter = tmpclusterCenter;
				optsize = size;
			}
//			cluserList.add(clusterCenter);
		}
		/**
		 * 将窗口大小与类中心写入文件
		 * 首行为窗口大小，以下每行一个类中心向量
		 */
		try
		{
			OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(fileName+"result"),"UTF-8");
			BufferedWriter bw     = new BufferedWriter(ow);
			bw.write(String.valueOf(optsize));
			for(int i =1 ;i<clusterCenter.size();i++)
			{
			
				for(int j = 0;j<clusersInstanceList.get(i).size();j++)
				{
					bw.newLine();
					StringBuilder sb = new StringBuilder(); 
					int index = clusersInstanceList.get(i).get(j);
					for(int k =0;k<instances.get(index).size();k++)
					{
						sb.append(String.format("%.0f ", instances.get(index).get(k)));
					}
					sb.append(i);
//					sb =sb.deleteCharAt(sb.length()-1);
					bw.write(sb.toString());
				}
//				sb.append("num "+clusersInstanceList.get(i).size());
				
				
			}
			ow.flush();
			bw.flush();
			ow.close();
			bw.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		try
		{
			OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(fileName+"avgindis"),"UTF-8");
			BufferedWriter bw     = new BufferedWriter(ow);
			bw.write(String.valueOf("size "+optsize));
			
			for(int i =1 ;i<clusterCenter.size();i++)
			{
			    double dis =0;
			    bw.newLine();
				for(int j = 0;j<clusersInstanceList.get(i).size();j++)
				{
					
					StringBuilder sb = new StringBuilder(); 
					int index = clusersInstanceList.get(i).get(j);
					for(int k =0;k<instances.get(index).size();k++)
					{
						dis+=Math.abs(instances.get(index).get(k)-clusterCenter.get(i).get(k));
					}
					
//					sb =sb.deleteCharAt(sb.length()-1);
					
				}
				dis/=clusersInstanceList.get(i).size();
				bw.write(String.format("%.0f",dis));
//				sb.append("num "+clusersInstanceList.get(i).size());
				
				
			}
			ow.flush();
			bw.flush();
			ow.close();
			bw.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		try
		{
			OutputStreamWriter ow = new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8");
			BufferedWriter bw     = new BufferedWriter(ow);
			bw.write(String.valueOf(optsize));
			for(int i =0 ;i<clusterCenter.size();i++)
			{
				bw.newLine();
				StringBuilder sb = new StringBuilder(); 
				for(int j = 0;j<clusterCenter.get(i).size();j++)
				{
					sb.append(String.format("%.0f ", clusterCenter.get(i).get(j)));
					
				}
//				sb.append("num "+clusersInstanceList.get(i).size());
				sb =sb.deleteCharAt(sb.length()-1);
				
				bw.write(sb.toString());
			}
			ow.flush();
			bw.flush();
			ow.close();
			bw.close();
		}catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
		
	}
	/**
	 * singlepath聚类
	 * @param instances       训练集
	 * @param clustersCenter 类中心列表
	 * @param threshold       阈值
	 * @return
	 */
	private static int singlePathCluster(ArrayList<ArrayList<Double>>instances,
			ArrayList<ArrayList<Double>> clustersCenter,
			ArrayList<ArrayList<Integer>> clusersInstanceList,double threshold)
	{
		
		ArrayList<ArrayList<Double>>tmpinstances = new ArrayList<ArrayList<Double>>();
		Iterator<ArrayList<Double>> it = instances.iterator();
		
		int size =instances.get(0).size();
		while(it.hasNext())
		{
			boolean allzero =true;
			ArrayList <Double > list = it.next();
			for(int i=0;i<list.size();i++)
				if(list.get(i)>0.0)
				{
					allzero =false;
					break;
				}
			if(!allzero)
			{
				tmpinstances.add(list);
			}
		}
		instances.clear();
		instances.addAll(tmpinstances);
		ArrayList <Double > tmplist = new ArrayList<Double> ();
		for(int i =0;i<size;i++)
			tmplist.add(0.0);
		clustersCenter.add(tmplist);
		
		/**由于全零类不参与后期聚类，故将0类instance列表用空队列填上
		 * 
		 */
		{
			ArrayList <Integer> list = new ArrayList<Integer>();
			clusersInstanceList.add(list);
		}
		
		if(instances.size()>0)
		{
			clustersCenter.add(instances.get(0));
			ArrayList <Integer> list = new ArrayList<Integer>();
			list.add(0);
			clusersInstanceList.add(list);
		}
		for(int i=1;i<instances.size();i++)
		{
			if(i%10000==0)
			{
				System.out.println("i "+i);
				System.out.println("clusternum "+clustersCenter.size());
			}
			double mindis =Double.MAX_VALUE ;
			double tmpdis=0.0;
			int index = 0;
			for(int j=1;j<clustersCenter.size();j++)
			{
				tmpdis = manhattanDistance(clustersCenter.get(j),instances.get(i));
				if(tmpdis<mindis)
				{
					mindis = tmpdis;
					index =j;
				}
			}
			if(mindis>threshold)
			{
				/**
				 * 加入新簇
				 */
				clustersCenter.add(instances.get(i));
				ArrayList <Integer> list = new ArrayList<Integer>();
				list.add(i);
				clusersInstanceList.add(list);
			}
			else
			{
				/**
				 * 重新计算center
				 */
				clusersInstanceList.get(index).add(i);	//将该结点加入到簇
				double n = clusersInstanceList.get(index).size();
				 
				for(int k=0;k<clustersCenter.get(index).size();k++)
				{
					double tmp = (clustersCenter.get(index).get(k)*(n-1)+instances.get(i).get(k))/n;
					clustersCenter.get(index).set(k,tmp);
				}
			}
		}
		return clustersCenter.size();
	}
	private static double 
	manhattanDistance(List<Double>list1,List<Double>list2)
	{
		double ans=0;
		for(int i=0;i<list1.size();i++)
		{
			ans+=Math.abs(list1.get(i)-list2.get(i));
		}
		return ans;
	}
	private static double erpDistance(List<Double>list1,List<Double>list2)
	{
		double dp[][] = new double[list1.size()+1][list2.size()+1];
		dp[0][0]=0;
		/*
		 * i,j对应前i,j个字符的erp距离，这里的i,j从1开始。dp[0][0]是还没有字符匹配的情况
		 */
		for(int i=0;i<list1.size()+1;i++)
			for(int j =0 ;j<list2.size()+1;j++)
			{
				if(i==0&&j==0)
					continue;
				dp[i][j]=Double.MAX_VALUE;
				double tmp;
				if(i-1>=0&&j-1>=0)
				{
					tmp = dp[i-1][j-1]+Math.abs(list1.get(i-1)-list2.get(j-1));
					dp[i][j] =tmp<dp[i][j]?tmp:dp[i][j];
//					System.out.println("i "+i+"j "+j+"dp1 "+tmp);
				}
				if(i-1>=0)
				{
					tmp = dp[i-1][j]+list1.get(i-1);
					dp[i][j] = tmp<dp[i][j]?tmp:dp[i][j];
//					System.out.println("i "+i+"j "+j+"dp2 "+tmp);
				}
			    if(j-1>=0)
				{
					tmp = dp[i][j-1]+list2.get(j-1);
					dp[i][j] = tmp<dp[i][j]?tmp:dp[i][j];
//					System.out.println("i "+i+"j "+j+"dp3 "+tmp);
				}
//				System.out.println("i "+i+"j "+j+"dp "+dp[i][j]);
			}
		
		return dp[list1.size()][list2.size()];
	}
	
	public static void main(String args[])
	{
//		TaskElement task = new TaskElement();
//		System.out.println(TaskRange.NodePairRange);
//		Pattern p= Pattern.compile(".*protocol\\s*=(\\d{3}).*");
//		Matcher match = p.matcher("protocol=402");
//		match.find();
//		System.out.println(match.group(1));
////		NodePairReader.
////		TaskElement task = new TaskElement();
//		task.setSourcePath("E:/javaproject/NetworkMiningSystem/smtpPcap");
//		task.setDataSource("Text");
//		task.setTaskRange(TaskRange.NodePairRange);
//		task.setFilterCondition("protocol="+"402");
//		task.setGranularity(3600);
//		task.setMiningObject("traffic");
////		train(task,200,6);
//		ArrayList <String> ips = new ArrayList<String> ();
//		for(int i=1;i<=1;i++)
//			for(int j=1;j<=2;j++)
//				ips.add("10.0."+i+"."+j);
//		for(int i =0;i<ips.size();i++)
//			for(int j=i+1;j<ips.size();j++)
//			{
//				String ip[] = new String[]{ips.get(i),ips.get(j)};
//				IReader reader = new nodePairReader(task,ip);
//				DataItems tmp = reader.readInputByText();
//				DataItems dataItems=new DataItems();
//				for(int k=0;k<tmp.getLength();k++)
//				{
//					DataItem dataItem = tmp.getElementAt(k);
//					dataItem.setData(String.valueOf(Double.valueOf(dataItem.getData())/2));
//					dataItems.add1Data(dataItem);
//				}
//				
//				dataItems=DataPretreatment.aggregateData(dataItems,3600,AggregateMethod.Aggregate_MEAN,false);
//				System.out.println("i "+i+" j "+j);
//				System.out.println("list add "+dataItems.getLength());
//				toDiscreteNumbersAccordingToWaveform(dataItems, task);
////				list.add(dataItems);
//			
//			}
//		System.out.println("over");
//		nodePairReader reader= new nodePairReader(task,new String[2]);
//		String condition[]=new String[0];
//		Map<String,DataItems> ipPairItems = new HashMap<String,DataItems>();
//		ipPairItems=reader.readAllByText(condition);
//		
//		
//		System.out.println("ippair "+ipPairItems.size());
		trainAll();
		System.out.println("over");
//		
//		Double array1[] = new Double[] {0.0,0.0,0.0,368.0,339.0,0.0,0.0,0.0,550.0,0.0,333.0,350.0,0.0,0.0,363.0,0.0,0.0,0.0,0.0,0.0,357.0,341.0,341.0,0.0};
//		Double array2[] = new Double[] {0.0,0.0,368.0,339.0,0.0,0.0,0.0,550.0,0.0,333.0,350.0,0.0,0.0,363.0,0.0,0.0,0.0,0.0,0.0,357.0,341.0,341.0,0.0,0.0};
//		ArrayList<Double> list1  = new ArrayList<Double>();
//		ArrayList<Double> list2  = new ArrayList<Double>();
//		list1.addAll(Arrays.asList(array1));
//		list2.addAll(Arrays.asList(array2));
//		System.out.println("erp "+erpDistance(list1,list2));
//		list1.add(e)
//		trainAll();
//		toDiscreteNumbersAccordingToWaveform
	}
}
