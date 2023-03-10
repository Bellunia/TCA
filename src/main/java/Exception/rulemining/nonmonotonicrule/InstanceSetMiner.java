package Exception.rulemining.nonmonotonicrule;

import Exception.experiment.Conductor;
import Exception.indexing.FactIndexer;
import Exception.rules.PositiveRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 
 * @author Hai Dang Tran
 * 
 */
public abstract class InstanceSetMiner {

	private static final Logger LOG = LoggerFactory.getLogger(InstanceSetMiner.class);

	protected List<PositiveRule> positiveRules;

	protected Map<PositiveRule, Set<String>> positiveRule2NormalSet;

	protected Map<PositiveRule, Set<String>> positiveRule2AbnormalSet;

	protected InstanceSetMiner() {
		positiveRules = new ArrayList<>();
		positiveRule2NormalSet = new HashMap<>();
		positiveRule2AbnormalSet = new HashMap<>();
	}

	public abstract void loadPositiveRules(String fileName, int topRuleCount);

	public abstract List<Set<String>> findInstances(PositiveRule rule, FactIndexer facts);

	public void findInstances(FactIndexer facts) {
		for (PositiveRule positiveRule : positiveRules) {
			List<Set<String>> instances = findInstances(positiveRule, facts);
			positiveRule2NormalSet.put(positiveRule, instances.get(0));//normalExamples
			positiveRule2AbnormalSet.put(positiveRule, instances.get(1));//abnormalExamples
		}
		LOG.info("Done with normal and abnormal sets");
	}

	public void findPositiveNegativeExamples(FactIndexer facts) {
		for (PositiveRule positiveRule : positiveRules) {
			Set<String> normalSet = positiveRule2NormalSet.get(positiveRule);
			Set<String> abnormalSet = positiveRule2AbnormalSet.get(positiveRule);
			ExceptionMiner.findCandidates(positiveRule, abnormalSet, normalSet, facts);
		}
		LOG.info("Done with finding EWS");
		Conductor.time2 = new Date();
//		System.out.println("Done EWS with " + (Conductor.time2.getTime() - Conductor.time1.getTime()));
	}

	public List<PositiveRule> getPositiveRules() {
		return positiveRules;
	}

	public Set<String> getNormalSet(PositiveRule rule) {
		return positiveRule2NormalSet.get(rule);
	}

	public Set<String> getAbnormalSet(PositiveRule rule) {
		return positiveRule2AbnormalSet.get(rule);
	}

}
