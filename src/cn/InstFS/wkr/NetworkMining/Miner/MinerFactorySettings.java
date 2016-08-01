package cn.InstFS.wkr.NetworkMining.Miner;

import cn.InstFS.wkr.NetworkMining.TaskConfigure.MinerType;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningMethod;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.MiningObject;
import cn.InstFS.wkr.NetworkMining.TaskConfigure.TaskRange;
import org.apache.commons.math3.analysis.function.Min;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arbor vlinyq@gmail.com
 * @date 2016/6/30
 */
public abstract class MinerFactorySettings implements Serializable{
    private String dataPath;
    private String minerType;
    private List<MiningObject> miningObjectList = new ArrayList<>();
    private List<MiningObject> miningObjectsChecked = new ArrayList<>();

    private List<MiningMethod> miningMethodsList = new ArrayList<>();
    private List<MiningMethod> miningMethodsChecked = new ArrayList<>();
    private TaskRange taskRange;
    private String granularity = "3600";
    private boolean isModified = false;
    private boolean isOnlyObjectModified = false;
    private List<MiningObject> miningObjectsAdded = new ArrayList<>();
    private List<MiningObject> miningObjectsDeleted = new ArrayList<>();

    public MinerFactorySettings(String minerType) {
        this.minerType = minerType;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public TaskRange getTaskRange() {
        return taskRange;
    }

    public void setTaskRange(TaskRange taskRange) {
        this.taskRange = taskRange;
    }

    public String getMinerType() {
        return minerType;
    }

    public void setMinerType(String minerType) {
        this.minerType = minerType;
    }

    public String getGranularity() {
        return granularity;
    }

    public void setGranularity(String granularity) {
        this.granularity = granularity;
    }
    public List<MiningObject> getMiningObjectList() {
        return miningObjectList;
    }

    public void setMiningObjectList(List<MiningObject> miningObjectList) {
        this.miningObjectList = miningObjectList;
    }
    public List<MiningObject> getMiningObjectsChecked() {
        return miningObjectsChecked;
    }

    public void setMiningObjectsChecked(List<MiningObject> miningObjectsChecked) {
        this.miningObjectsChecked = miningObjectsChecked;
    }
    public boolean isModified() {
        return isModified;
    }
    public void setModified(boolean modified) {
        isModified = modified;
    }

    public List<MiningObject> getMiningObjectsAdded() {
        return miningObjectsAdded;
    }

    public void setMiningObjectsAdded(List<MiningObject> miningObjectsAdded) {
        this.miningObjectsAdded = miningObjectsAdded;
    }

    public boolean isOnlyObjectModified() {
        return isOnlyObjectModified;
    }

    public void setOnlyObjectModified(boolean onlyObjectModified) {
        isOnlyObjectModified = onlyObjectModified;
    }

    public List<MiningObject> getMiningObjectsDeleted() {
        return miningObjectsDeleted;
    }

    public void setMiningObjectsDeleted(List<MiningObject> miningObjectsDeleted) {
        this.miningObjectsDeleted = miningObjectsDeleted;
    }

    public List<MiningMethod> getMiningMethodsList() {
        return miningMethodsList;
    }

    public void setMiningMethodsList(List<MiningMethod> miningMethodsList) {
        this.miningMethodsList = miningMethodsList;
    }

    public List<MiningMethod> getMiningMethodsChecked() {
        return miningMethodsChecked;
    }

    public void setMiningMethodsChecked(List<MiningMethod> miningMethodsChecked) {
        this.miningMethodsChecked = miningMethodsChecked;
    }
}
