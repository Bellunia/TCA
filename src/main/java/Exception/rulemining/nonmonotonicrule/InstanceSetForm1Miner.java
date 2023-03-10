package Exception.rulemining.nonmonotonicrule;

import Exception.indexing.FactIndexer;
import Exception.rules.PositiveRule;
import Exception.rules.PositiveRuleType;
import Exception.experiment.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Hai Dang Tran
 * 
 * This class is to handle rules mined from pattern: h(X, Z) <- p(X, Y) ^ q(Y, Z).
 */
public class InstanceSetForm1Miner extends InstanceSetMiner {

	private static final Logger LOG = LoggerFactory.getLogger(InstanceSetForm1Miner.class);

	public InstanceSetForm1Miner() {
	}

	@Override
	public void loadPositiveRules(String fileName, int topRuleCount) {
		int count = 0;
		List<String> lines = Utils.readLines(fileName);
		for (String line : lines) {
			line = line.split("\t")[0];
			String[] parts = line.split("(\\(X, Z\\) :- )|(\\(X, Y\\), )|(\\(Y, Z\\))");//h(x,z)<-p(x,y)q(y,z)
			count++;
			if (count > topRuleCount) break;
			positiveRules.add(new PositiveRule(parts[0] + "\t" + parts[1] + "\t" + parts[2], PositiveRuleType.FORM2));
			                                   //    h p  q
		}
	}

	@Override
	public List<Set<String>> findInstances(PositiveRule positiveRule, FactIndexer facts) {
		String h = positiveRule.getHead();
		String[] parts = positiveRule.getBody().split("\t");
		String p = parts[0];
		String q = parts[1];
		Set<String> normalExamples = new HashSet<>();
		Set<String> abnormalExamples = new HashSet<>();
		for (String yz : facts.getXySetFromP(q)) {
			String y = yz.split("\t")[0];
			String z = yz.split("\t")[1];
			Set<String> xSet = facts.getXSetFromPy(p + "\t" + y);
			if (xSet == null) {
				continue;
			}
			for (String x : xSet) {
				if (facts.checkXpy(x + "\t" + h + "\t" + z)) {
					normalExamples.add(x + "\t" + z);
				} else {
					abnormalExamples.add(x + "\t" + z);
				}
			}
		}
		List<Set<String>> result = new ArrayList<Set<String>>();
		result.add(normalExamples);
		result.add(abnormalExamples);
		return result;
	}

}
