package cn.InstFS.wkr.NetworkMining.Miner;

import java.io.Serializable;
import java.util.List;

import associationRules.ProtocolAssociationResult;

public class MinerResultsFP_Whole implements Serializable{

	double confidence = 0.0;
	String ip = "";
	List<ProtocolAssociationResult> protocolPairList = null;
	
	public void setIp(String p){
		ip = p;
	}
	public void setConfidence(double d){
		confidence = d;
	}
	public void setProtocolPairList(List<ProtocolAssociationResult> list){
		protocolPairList = list;
	}
	public String getIp(){
		return ip;
	}
	public double getConfidence(){
		return confidence;
	}
	public List<ProtocolAssociationResult> getProtocolPairList(){
		return protocolPairList;
	}
}
