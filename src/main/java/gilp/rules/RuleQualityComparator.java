package gilp.rules;

import java.util.Comparator;


public class RuleQualityComparator implements Comparator<RulePackage> {		 
	@Override
	public int compare(RulePackage o1, RulePackage o2) {
		if(o1.getQuality()<o2.getQuality())
			return -1;
		else if(o1.getQuality()>o2.getQuality())
			return 1;
		else
			return 0;
	}
	
}