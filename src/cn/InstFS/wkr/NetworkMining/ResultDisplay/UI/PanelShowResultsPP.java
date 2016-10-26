package cn.InstFS.wkr.NetworkMining.ResultDisplay.UI;

import cn.InstFS.wkr.NetworkMining.DataInputs.DataItem;
import cn.InstFS.wkr.NetworkMining.DataInputs.DataItems;
import cn.InstFS.wkr.NetworkMining.DataInputs.DataPretreatment;
import cn.InstFS.wkr.NetworkMining.DataInputs.MovingAverage;
import cn.InstFS.wkr.NetworkMining.Miner.Common.TaskCombination;
import cn.InstFS.wkr.NetworkMining.Miner.Factory.NetworkMinerFactory;
import cn.InstFS.wkr.NetworkMining.Miner.NetworkMiner.INetworkMiner;
import cn.InstFS.wkr.NetworkMining.Miner.Results.*;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskElement;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class PanelShowResultsPP extends JPanel implements IPanelShowResults {

    private INetworkMiner miner;
    private MiningMethod miningMethod;
    ChartPanelShowTs chart1;
    int count=0;
    TaskElement task;
    List<TaskCombination> alltaskCombination;
    TaskCombination taskCombination;
    private String obName;
    private String xName;
    private String yName;
    /**
     * Create the panel.
     */
    public PanelShowResultsPP(TaskElement task) {
        this.task = task;
        obName=task.getMiningObject();
        yName = obName;
        alltaskCombination = task.allCombinationTasks;
        setLayout(new GridLayout(0, 1, 0, 0));
        chart1 = new ChartPanelShowTs("路径挖掘", "时间", obName, null);
        add(chart1);
        miningMethod = task.getMiningMethod();
        InitMiner(task);
        ChartPanelShowAbc chartc = new ChartPanelShowAbc(task);
        ChartPanelShowAbl chartl = new ChartPanelShowAbl(task);
    }

    private void InitMiner(TaskElement task) {
        this.miner = NetworkMinerFactory.getInstance().createMiner(task);
        miner.setResultsDisplayer(this);
    }

    public void showResults() {
        // TODO
    }

    @Override
    public boolean start() {
        return miner.start();
    }

    @Override
    public boolean stop() {
        return miner.stop();
    }


    @Override
    public void setData(DataItems data) {


    }


    @Override
    public TaskElement getTask() {
        return miner.getTask();
    }

    @Override
    public INetworkMiner getMiner() {
        return miner;
    }


    @Override
    public void displayMinerResults() {
        MinerResults rets = miner.getResults();
        displayMinerResults(rets);

    }

    public void displayMinerResults(MinerResults rslt) {
        if (rslt == null || rslt.getRetPath() == null)
            return;
        else if(count==0 && miningMethod.equals(MiningMethod.MiningMethods_PeriodicityMining))
        {
            JScrollPane jsp = new JScrollPane();
            JPanel jp=new JPanel();
            JPanel label = new JPanel();
            JPanel panel = new JPanel(new BorderLayout());
            remove(chart1);
            HashMap<String, MinerResultsPM> resultPM = rslt.getRetPath().getRetPM();

            HashMap<String,Integer> period = new HashMap<>();
            HashMap<String,Integer> firstPeriod = new HashMap<>();
            HashMap<String,DataItems> pathDataItems = new HashMap<>();

            yName=obName;


            for (Map.Entry<String, MinerResultsPM> entry : resultPM.entrySet()) {
                String pathname = entry.getKey();
                MinerResultsPM result = entry.getValue();
                period.put(pathname, (int) result.getPeriod());
                firstPeriod.put(pathname, result.getFirstPossiblePeriod());
                pathDataItems.put(pathname,result.getDistributePeriod());
                System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
            }


            HashMap<Integer,ArrayList<String>> sortPeriod=new HashMap<>();
            int min=100000000;
            while (!period.isEmpty()) {
                ArrayList<String> periodName=new ArrayList<>();
                //找出最小周期值
                for (String key : period.keySet()) {
                    int temp = period.get(key);
                    if (temp <= min)
                        min = temp;
                }
                //将最小周期值所对应的节点名字储存起来
                for(String key:period.keySet())
                {

                    if(period.get(key)==min)
                    {
                        periodName.add(key);
                    }
                }
                //删除最小周期值
                for(int i=0;i<periodName.size();i++)
                {
                    period.remove(periodName.get(i));
                }
                sortPeriod.put(min,periodName);
                min=1000000000;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时");
            JLabel startTime = new JLabel("开始时间:"+sdf.format(rslt.getInputData().getTime().get(0))+"      ");
            JLabel granularity = new JLabel("时间粒度:"+task.getGranularity()+"(s)");
            label.add(startTime);
            label.add(granularity);
            jp.setLayout(new GridLayout(0,1));
            panel.add(label,BorderLayout.NORTH);
            panel.add(jp,BorderLayout.CENTER);
            jsp.setViewportView(panel);
            add(jsp);

            HashMap<String, DataItems> pathOriDataItems = rslt.getRetPath().getPathOriDataItems();
            /**帮助解读:
             * key:周期数值
             * i:周期数为key的第i个周期
             * **/
            for(Integer key:sortPeriod.keySet())
            {
                int numPeriod=key;
                int numfirstPeriod=0;// TODO: 2016/5/31 多路径下最可能子周期值
                ArrayList<String> oneSeries = sortPeriod.get(key);
                ArrayList<DataItems> pathNor=new ArrayList<>();
                ArrayList<String> pathname=new ArrayList<String>();
                for(int i=0;i<oneSeries.size();i++)
                {
                    String tempName=oneSeries.get(i);

                	/*从tempName中获取源和目的IP*/
                    String[] temp = tempName.split(",");
                    int tempLen = temp.length;
                    String sIp = temp[0];
                    String dIp = temp[tempLen-1];
                    String range = sIp + "_" + dIp;

                    //pathname.add(tempName);
                    //pathNor.add(pathDataItems.get(tempName));//每条路径一個週期内的dataitem
                    numfirstPeriod = firstPeriod.get(tempName);//周期值string

                   /* for(int j=0;j<alltaskCombination.size();j++){
                    	TaskCombination tc = alltaskCombination.get(j);
                    	if(tc.getRange().equals(range)&&tc.getTasks().contains(task)){
                    		taskCombination = tc;
                    	}
                    }

                    //解析taskCombination的DataItems格式
                    List<List<String>> seqs=PMDetect(taskCombination.getDataItems(), task);
                    List<String> seq = null;
                    for(int k=0;k<seqs.size();k++){
                    	List<String> ls = seqs.get(k);
                    	if(ls.get(0).equals(tempName)){
                    		seq = ls;
                    	}
                    }
                    DataItems newItems = readSeq(seq, taskCombination.getDataItems());*/


                    //pathname.add(tempName);
                    //pathNor.add(taskCombination.getDataItems());
                    pathname.add(tempName);
                    pathNor.add(pathOriDataItems.get(tempName));
                    DataItems nor=new DataItems();
                    DataItems abnor=new DataItems();
                    if(numPeriod>0){
                        JFreeChart jf = ChartPanelShowPP.createChart(pathNor,pathname, nor, abnor,numPeriod,numfirstPeriod,yName);
                        ChartPanel chartpanel = new ChartPanel(jf);
                        jp.add(chartpanel);
                        pathNor = new ArrayList<>();
                        pathname = new ArrayList<>();
                    }
                    else{
                        System.out.println("没有周期");
                    }
                }
            }
            repaint();
            validate();
            count++;
        }else if (count==0 && miningMethod.equals(MiningMethod.MiningMethods_OutliesMining)) {
            JScrollPane jsp = new JScrollPane();
            JPanel jp=new JPanel();
            JPanel label = new JPanel();
            JPanel panel = new JPanel(new BorderLayout());
            remove(chart1);


            HashMap<String, MinerResultsOM> resultOM = rslt.getRetPath().getRetOM();
            HashMap<String, DataItems> oriItems = rslt.getRetPath().getPathOriDataItems();
            for (Map.Entry<String, MinerResultsOM> entry: resultOM.entrySet()) {
                String pathName = entry.getKey();
                MinerResultsOM result = entry.getValue();
                DataItems outlines = result.getOutlies(); //异常点
                DataItems outDegree = result.getOutDegree(); //异常度
                List<DataItems> outlinesSet = result.getOutlinesSet(); //异常线段
                DataItems oriDataItems = oriItems.get(pathName); //原始数据
                
                DataItems outliesItems = comItems(outlines); //除以1000后的异常点
                List<DataItems> outsetItems = new ArrayList<DataItems>();//除以1000后的异常线段
                DataItems oriData = comItems(oriDataItems);  //除以1000后的原始数据           
                
                for(int i = 0;i < outlinesSet.size();i++){
                    DataItems di = comItems(outlinesSet.get(i));
                    outsetItems.add(di);
                }

                /*ChartPanelShowTs chart = new ChartPanelShowTs("路径"+pathName+"原始值", "时间", yName, null);
                *//*MovingAverage ma = new MovingAverage(oriData);
                DataItems newItems = ma.getNewItems();
                chart.displayDataItems(newItems);*//*
                chart.displayDataItems(oriData);
                
                jp.add(chart);*/
                JFreeChart jf;
                /*if (!result.isHasOutlies()) {
                    jf = ChartPanelShowAb.createChart(new DataItems(),new DataItems());
                    //jf = ChartPanelShowAb.createChart(oriData,new DataItems());
                } else if (result.isIslinkDegree()) {
                    //jf = ChartPanelShowAbd.createChart(outliesItems);
                    jf = ChartPanelShowAbd.createChart(oriData,outliesItems);
                } else {
                    jf = ChartPanelShowAb.createChart(oriData, outliesItems);
                }*/
                if (!result.isHasOutlies()) {
                   
                	jf = ChartPanelShowAbl.createChart("路径"+pathName+"异常度检测",oriData,outDegree,outsetItems,yName);
                } else if (result.isIslinkDegree()) {                    
                    jf = ChartPanelShowAbl.createChart("路径"+pathName+"异常度检测",oriData,outDegree,outsetItems,yName);
                } else if(!result.isIslinkDegree()){
                    jf = ChartPanelShowAbc.createChart("路径"+pathName+"异常度检测",oriData, outDegree,outliesItems,yName);
                } else{
                	jf = ChartPanelShowAbc.createChart("路径"+pathName+"异常度检测",oriData, outDegree,outliesItems,yName);
                }
                
                ChartPanel chartpanel = new ChartPanel(jf);
                jp.add(chartpanel);

            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时");
            JLabel startTime = new JLabel("开始时间:"+sdf.format(rslt.getInputData().getTime().get(0))+"      ");
            JLabel granularity = new JLabel("时间粒度:"+task.getGranularity()+"(s)");
            label.add(startTime);
            label.add(granularity);
            jp.setLayout(new GridLayout(0,1));
            panel.add(label,BorderLayout.NORTH);
            panel.add(jp,BorderLayout.CENTER);
            jsp.setViewportView(panel);            
            add(jsp);
            repaint();
            validate();
            count++;
        }else if (count==0 && miningMethod.equals(MiningMethod.MiningMethods_Statistics)) {
            remove(chart1);
            JScrollPane jspOut = new JScrollPane();
            JPanel jpOut = new JPanel();
            jpOut.setLayout(new BorderLayout());

            JScrollPane jsp = new JScrollPane();
            JPanel jPanel = new JPanel();

            ArrayList<Map.Entry<String, MinerResultsStatistics> >resultList = new ArrayList<>(rslt.getRetPath().getRetStatistic().entrySet());
            ArrayList<Map.Entry<String, Double>> pathProbList = new ArrayList<>(rslt.getRetPath().getPathProb().entrySet());
            Collections.sort(pathProbList,new Comparator<Map.Entry<String, Double> >()
            {
                @Override
                public int compare(Map.Entry<String,Double> o1, Map.Entry<String,Double> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            int size = resultList.size();
            String data[][]=new String[size][7];
            int i = 0;
            ArrayList<String> pathList = new ArrayList<>();

            for (Map.Entry<String, Double> entry : pathProbList) {
                String pathName = entry.getKey();
                MinerResultsStatistics retStatistic = rslt.getRetPath().getRetStatistic().get(pathName);

                pathList.add(pathName);
                data[i][0] = i+1+"";
                data[i][1] = pathName.replace(","," --> ");
                data[i][2]=String.format("%5.3f",retStatistic.getMean()/1000);
                data[i][3]=String.format("%5.3f",retStatistic.getStd()/1000);
                data[i][4]=String.format("%5.3f",retStatistic.getSampleENtropy());
                data[i][5]=String.format("%5.3f",retStatistic.getComplex());
                data[i][6]=String.format("%5.3f",entry.getValue());
                i++;
            }

            String colNames[]={"序号","路径序列","平均值","标准差","平均熵","复杂度","通信概率"};


            DefaultTableModel model=new DefaultTableModel(data,colNames){
                public   boolean   isCellEditable(int   row,   int   column)
                {
                    return   false;
                };

            };


            JTable listTable = new JTable(model);
            listTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            listTable.setAutoscrolls(true);
            listTable.getColumnModel().getColumn(1).setPreferredWidth(220);
            jsp.setViewportView(listTable);

            //以下用于绘制直方图
            HashMap<String, String> probMap = new HashMap<String, String>();
            probMap = getPpProbMap(pathProbList,rslt);
            ChartPanelShowPPStatistics chat = new ChartPanelShowPPStatistics();
            JFreeChart jfreeChart = chat.createChart(probMap);
            chat.setVisible(true);
            ChartPanel chartPanel = new ChartPanel(jfreeChart,false);
            chartPanel.setPreferredSize(new Dimension(500,270));
            jPanel.add(chartPanel);
            jsp.setPreferredSize(new Dimension(500,200));
            jPanel.setLayout(new GridLayout(0,1));
            jpOut.add(jsp, BorderLayout.NORTH);
            jpOut.add(jPanel, BorderLayout.SOUTH);

            // 绘制路径结构图
            JPanel jp = new JPanel();
            jp.setLayout(new BorderLayout());
            GraphPanelShowPath view = new GraphPanelShowPath(pathList);
            jp.add(view, BorderLayout.CENTER);
            view.init();
            view.setVisible(true);
            jp.setPreferredSize(new Dimension(500, 400));
            jpOut.add(jp, BorderLayout.CENTER);

            jspOut.setViewportView(jpOut);
//            jpOut.setVisible(true);
            add(jspOut);

            repaint();
            validate();
            count++;
        } else if (count==0 && miningMethod.equals(MiningMethod.MiningMethods_PredictionMining)) {
            JScrollPane jsp = new JScrollPane();
            JPanel jp=new JPanel();
            JPanel label = new JPanel();
            JPanel panel = new JPanel(new BorderLayout());

            HashMap<String, MinerResultsFM> resultFM = rslt.getRetPath().getRetFM();
            HashMap<String, DataItems> oriItemsMap = rslt.getRetPath().getPathOriDataItems();
            remove(chart1);
            for (Map.Entry<String, MinerResultsFM> entry: resultFM.entrySet()) {
                String pathName = entry.getKey();
                DataItems predictItems = entry.getValue().getPredictItems();
                DataItems oriItems = oriItemsMap.get(pathName);

                predictItems = comItems(predictItems);
                oriItems = comItems(oriItems);

                if (predictItems == null) {
                    chart1.displayDataItems(oriItems);
                } else {
                    DataItem lastOriItem = oriItems.getElementAt(oriItems.getLength() - 1);
                    predictItems.add1Data(lastOriItem);

                    JFreeChart jf = ChartPanelShowPre.createChart("路径"+pathName+"的序列预测",oriItems, predictItems, yName);
                    ChartPanel chartpanel = new ChartPanel(jf);
                    jp.add(chartpanel);
                    count++;

                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时");
            JLabel startTime = new JLabel("开始时间:"+sdf.format(rslt.getInputData().getTime().get(0))+"      ");
            JLabel granularity = new JLabel("时间粒度:"+task.getGranularity()+"(s)");
            label.add(startTime);
            label.add(granularity);
            jp.setLayout(new GridLayout(0,1));
            panel.add(label,BorderLayout.NORTH);
            panel.add(jp,BorderLayout.CENTER);
            jsp.setViewportView(panel);
            add(jsp);

            repaint();
            validate();
            count++;

        }
    }
    /**@author LYH
     * 解析原始数据
     * 把dataItems中的ProbMap的数据转换到data中
     * **/
    public List<List<String>> PMDetect(DataItems dataItems,TaskElement tasks){

        List datas = new ArrayList();
        if (task.getMiningObject().equals("流量")){
            datas = dataItems.getNonNumData();
        } else if (task.getMiningObject().equals("通信次数")) {
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
                        seq.add(value+"");
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

            if(row<dataItems.getLength()*0.05) //如果包含路径item的行很少，则不计入seqs
                continue;
            seqs.add(seq);
        }
        return seqs;
    }
    public DataItems readSeq(List<String> seq,DataItems dataItems){


        DataItems newItem=new DataItems();
        String name=seq.get(0);
        seq.remove(0);
        newItem.setData(seq);
        newItem.setTime(dataItems.getTime());

        return newItem;

    }
    /**
     * @author LYH
     * 整合路径的概率分布数据**/
    public HashMap<String, String> getPpProbMap(ArrayList<Map.Entry<String, Double>> pathProbList,MinerResults rslt){
        HashMap<String, String> map = new HashMap<String, String>();
        for (Map.Entry<String, Double> entry : pathProbList) {
            String pathName = entry.getKey();
            MinerResultsStatistics retStatistic = rslt.getRetPath().getRetStatistic().get(pathName);
            String value=String.format("%5.3f",entry.getValue());
            map.put(pathName, value);
        }
        return map;
    }
    /**
     * @author LYH
     * 用于数据除以1000*/
    public DataItems comItems(DataItems di){
    	DataItems items = new DataItems();
    	for(int i=0;i<di.getLength();i++){
    		double data1 = Double.parseDouble(di.getData().get(i));
            Date time = di.getTime().get(i);
            String data = String.valueOf(data1/1000);
            items.add1Data(time, data);
    	}
    	return items;
    }
}





