package gilp.rules;

import java.util.ArrayList;
import java.util.Arrays;

public class RulePackageFactory {

	//choose rules with top-k best quality scores
	public static ArrayList<RulePackage> chooseTopRP(ArrayList<RulePackage> listCandidates, int k){		
		RulePackage[] rules = (RulePackage[]) listCandidates.toArray(new RulePackage[0]);
		Arrays.sort(rules, new RuleQualityComparator());
		ArrayList<RulePackage> listRlts = new ArrayList<>(); 
		for (int i=rules.length-1;i>=Math.max(0, rules.length-k);i--){
			listRlts.add(rules[i].clone());
		}
		return listRlts;
	}
	
	//stupid java!!!	
	public static ArrayList<ExpRulePackage> chooseTopExpRP(ArrayList<ExpRulePackage> listCandidates, int k){
		
		ExpRulePackage[] rules = (ExpRulePackage[]) listCandidates.toArray(new ExpRulePackage[0]);
		Arrays.sort(rules, new RuleQualityComparator());
		ArrayList<ExpRulePackage> listRlts = new ArrayList<>(); 
		for (int i=rules.length-1;i>=Math.max(0, rules.length-k);i--){
			listRlts.add(rules[i].clone());
		}
		return listRlts;		 
	}
	
	// clean duplicated rules
	public static void removeDuplicatedRP(ArrayList<RulePackage> listRules) {
		ArrayList<RulePackage> listRlts = new ArrayList<>();
		for (RulePackage rp : listRules) {
			boolean existed = false;
			for (RulePackage rp1: listRlts){
				if (rp1.getRule().equals(rp.getRule())){
					existed = true;
					break;
				}
			}
			if (!existed)
				listRlts.add(rp);
		}
		listRules.clear();
		listRules.addAll(listRlts);
	} 
	
}
