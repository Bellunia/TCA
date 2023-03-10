package Exception.experiment;

import Exception.indexing.FactIndexer;
import Exception.rulemining.nonmonotonicrule.ExceptionRanker;
import Exception.rulemining.nonmonotonicrule.RankingType;
import Exception.rules.ExceptionType;
import Exception.rules.NegativeRule;


import java.io.*;
import java.util.*;

/**
 * 
 * @author Hai Dang Tran
 * 
 */
public class Conductor {

	static final String[] RULE_TYPES = { ".pos.", ".neg." };

	public static int topRuleCount;

	public static int withDlv;

	public static boolean withSampledRules;

	public static String idealDataFileName;

	public static String encodeFileName;

	public static String patternFileName;

	public static String selectedPatternFileName;

	public static String trainingDataFileName;

	public static String trainingDataDlvFileName;

	public static String chosenRuleFileName;

	public static String extensionPrefixFileName;

	public static String dlvBinaryFileName;

	public static Date time1, time2, time3, time4;

	public static FactIndexer idealFacts, learningFacts;

	static void evaluate() {
		idealFacts = new FactIndexer(idealDataFileName);
		for (String predicate : idealFacts.getPSet()) {
			//System.out.println(predicate);
		}
		//System.out.println("Number of predicates in ideal graph: " + idealFacts.getPSet().size());

		for (int i = 0; i < 2; ++i) {
			String extensionFileName = extensionPrefixFileName + RULE_TYPES[i] + topRuleCount;
			evaluate(extensionFileName);
		}
	}

	static void evaluate(String fileName) {
		try {
			Writer goodFactWriter = new BufferedWriter(new FileWriter(fileName + ".good"));
			Writer needCheckFactWriter = new BufferedWriter(new FileWriter(fileName + ".needcheck"));
			Writer conflictWriter = new BufferedWriter(new FileWriter(fileName + ".conflict"));
			int inLearningPositiveFactCount = 0;
			int inLearningNegativeFactCount = 0;
			int goodFactCount = 0;
			int needCheckFactCount = 0;
			Set<String> positiveNewFacts = new HashSet<>();
			Set<String> negativeNewFacts = new HashSet<>();
//			System.out.println("Start evaluating file: " + fileName);
			if (fileName.contains(".pos.")) {
				System.out.println("With KG extended from positive rules:");
			} else {
				System.out.println("With KG extended from revised rules:");
			}
			String line = Utils.readLines(fileName).get(2);
			String[] facts = line.split(", ");
			Map<String, Long> goodFactPerPredicateCount = new HashMap<>();
			Map<String, Long> needCheckFactPerPredicateCount = new HashMap<>();
			Map<String, Long> conflictPerPredicateCount = new HashMap<>();
			Map<String, Long> negativeFactPerPredicateCount = new HashMap<>();
			for (String fact : facts) {
				if (fact.startsWith("{")) {
					fact = fact.substring(1);
				}
				if (fact.endsWith("}")) {
					fact = fact.substring(0, fact.length() - 1);
				}
				String[] parts = fact.split("\\(|\\)|,");
				if (!parts[0].startsWith("not_")) {
					String p = Encoder.id2Entity.get(parts[0]);
					String x = Encoder.id2Entity.get(parts[1]);
					String y = Encoder.id2Entity.get(parts[2]);
					String xpy = x + "\t" + p + "\t" + y;
					if (learningFacts.checkXpy(xpy)) {
						inLearningPositiveFactCount++;
						continue;
					}
					positiveNewFacts.add(xpy);
					if (idealFacts.checkXpy(xpy)) {
						goodFactCount++;
						goodFactWriter.write("<" + x + ">\t<" +p + ">\t<" + y + ">\n");
						Utils.addKeyLong(goodFactPerPredicateCount, p, 1L);
					} else {
						needCheckFactWriter.write("<" + x + ">\t<" +p + ">\t<" + y + ">\n");
						needCheckFactCount++;
						Utils.addKeyLong(needCheckFactPerPredicateCount, p, 1L);
					}
				} else {
					String p = Encoder.id2Entity.get(parts[0].substring("not_".length()));
					String x = Encoder.id2Entity.get(parts[1]);
					String y = Encoder.id2Entity.get(parts[2]);
					String xpy = x + "\t" + p + "\t" + y;
					if (learningFacts.checkXpy(xpy)) {
						inLearningNegativeFactCount++;
						continue;
					}
					Utils.addKeyLong(negativeFactPerPredicateCount, p, 1L);
					negativeNewFacts.add(xpy);
				}
			}
			int conflictCount = 0;
			for (String fact : positiveNewFacts) {
				if (negativeNewFacts.contains(fact)) {
					String[] parts = fact.split("\t");
					conflictWriter.write("<" + parts[0] + ">\t<" + parts[1] + ">\t<" + parts[2] + ">\n");
					conflictWriter.write("<" + parts[0] + ">\t<not_" + parts[1] + ">\t<" + parts[2] + ">\n");
					conflictCount++;
					Utils.addKeyLong(conflictPerPredicateCount, parts[1], 1L);
				}
			}
//			System.out.println("Already in the learning data (positive facts): " + inLearningPositiveFactCount);
//			System.out.println("Already in the learning data (negative facts): " + inLearningNegativeFactCount);
//			System.out.println("Total new predicted facts: " + (goodFactCount + needCheckFactCount));
//			System.out.println("Good predicted facts in ideal graph: " + goodFactCount);
//			System.out.println("Facts that need to check: " + needCheckFactCount);
//			System.out.println("Positive new facts: " + positiveNewFacts.size());
//			System.out.println("Negative new facts: " + negativeNewFacts.size());
//			System.out.println("Number of conflicts: " + conflictCount);
//			System.out.println("Done with file: " + fileName);
//			System.out.println();
			Set<String> predicates = new TreeSet<>();
			predicates.addAll(goodFactPerPredicateCount.keySet());
			predicates.addAll(needCheckFactPerPredicateCount.keySet());
			predicates.addAll(conflictPerPredicateCount.keySet());
			predicates.addAll(negativeFactPerPredicateCount.keySet());
			for (String predicate : predicates) {
				Long currentGoodFactCount = goodFactPerPredicateCount.get(predicate);
				if (currentGoodFactCount == null)
					currentGoodFactCount = 0L;
				Long currentNeedCheckFactCount = needCheckFactPerPredicateCount.get(predicate);
				if (currentNeedCheckFactCount == null)
					currentNeedCheckFactCount = 0L;
				Long currentConflictCount = conflictPerPredicateCount.get(predicate);
				if (currentConflictCount == null)
					currentConflictCount = 0L;
				Long currentFactCount = currentGoodFactCount + currentNeedCheckFactCount;
				Long negativeFactCount = negativeFactPerPredicateCount.get(predicate);
				if (negativeFactCount == null)
					negativeFactCount = 0L;
				System.out.println("Predicted facts: " + predicate + "\t" + currentFactCount + "\t" + currentGoodFactCount + "\t" + currentNeedCheckFactCount);
			}
			System.out.println("-----");
			goodFactWriter.close();
			needCheckFactWriter.close();
			conflictWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	static void generateExceptions(RankingType type) {
		learningFacts = Sampler.indexLearningData();
		time1 = new Date();
		if (!withSampledRules) {
			selectedPatternFileName = null;
		}
		ExceptionRanker ranker = new ExceptionRanker(patternFileName, selectedPatternFileName, learningFacts, topRuleCount);
		ranker.rankRulesWithExceptions(type);

		Conductor.time3 = new Date();
		//System.out.println("Done Exception Ranking with " + (Conductor.time3.getTime() - Conductor.time2.getTime()));

		// This is to convert rule set to DLV format.
		try {
			for (String ruleType : Conductor.RULE_TYPES) {
				int count = 0;
				Writer ruleWriter = new BufferedWriter(
						new FileWriter(Conductor.chosenRuleFileName + ruleType + topRuleCount));
//				Writer decodedRuleWriter = new BufferedWriter(
//						new FileWriter(Conductor.chosenRuleFileName + ruleType + topRuleCount + ".decode"));
				double convictionSum = 0;
				for (NegativeRule negativeRule : ranker.getChosenNegativeRules()) {
					count++;
					if (count > topRuleCount) {
						break;
					}
					String[] parts = negativeRule.getPositiveRule().getBody().split("\t");
					String head = negativeRule.getPositiveRule().getHead();
					String positiveRule = Encoder.entity2Id.get(head) + "(X, Z) :- " + Encoder.entity2Id.get(parts[0])
							+ "(X, Y), " + Encoder.entity2Id.get(parts[1]) + "(Y, Z)";
					String decodedPositiveRule = head + "(X, Z) <- " + parts[0] + "(X, Y) ^ " + parts[1] + "(Y, Z)";
					String negation = "";
					String decodedNegation = "";
					if (negativeRule.getException().getType() == ExceptionType.FIRST) {
						negation = Encoder.entity2Id.get(negativeRule.getException().getException()) + "(X).";
						decodedNegation = negativeRule.getException().getException() + "(X).";
					} else if (negativeRule.getException().getType() == ExceptionType.SECOND) {
						negation = Encoder.entity2Id.get(negativeRule.getException().getException()) + "(Z).";
						decodedNegation = negativeRule.getException().getException() + "(Z).";
					} else {
						negation = Encoder.entity2Id.get(negativeRule.getException().getException()) + "(X, Z).";
						decodedNegation = negativeRule.getException().getException() + "(X, Z).";
					}
					if (ruleType.equals(".neg.")) {
						ruleWriter.write(positiveRule + ", not " + negation + "\n");
//						decodedRuleWriter.write(decodedPositiveRule + " ^ not " + decodedNegation + "\n");
						double conviction = negativeRule.getStandardConviction();
						convictionSum += conviction;
					} else if (ruleType.equals(".pos.")) {
						ruleWriter.write(positiveRule + ".\n");
//						decodedRuleWriter.write(decodedPositiveRule + ".\n");
						double conviction = negativeRule.getPositiveRule().getConviction();
						convictionSum += conviction;
					} else {
						ruleWriter.write(positiveRule + ", not " + negation + "\n");
//						decodedRuleWriter.write(decodedPositiveRule + " ^ not " + decodedNegation + "\n");
						ruleWriter.write("not_" + positiveRule + ", " + negation + "\n");
//						decodedRuleWriter.write("not_" + decodedPositiveRule + " ^ " + decodedNegation + "\n");
					}
				}
				ruleWriter.close();
//				decodedRuleWriter.close();
				if (ruleType.equals(".pos.")) {
					System.out.println("Average conviction of positive rules: " + (convictionSum / topRuleCount));
				} else {
					System.out.println("Average conviction of revised rules: " + (convictionSum / topRuleCount));
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		System.out.println("-----");
	}

	static void runDlv() {
		System.out.println("Start DLV");
		try {
			for (String ruleType : RULE_TYPES) {
				String ruleFileName = chosenRuleFileName + ruleType + topRuleCount;
				String extensionFileName = extensionPrefixFileName + ruleType + topRuleCount;
				String command = dlvBinaryFileName + " -nofacts " + trainingDataDlvFileName + " " + ruleFileName;
				Writer dlvWriter = new BufferedWriter(new FileWriter(extensionFileName));
				Process dlvExecutor = Runtime.getRuntime().exec(command);
				BufferedReader dlvReader = new BufferedReader(new InputStreamReader(dlvExecutor.getInputStream()));
				String line;
				while ((line = dlvReader.readLine()) != null) {
					dlvWriter.write(line + "\n");
				}
				dlvExecutor.waitFor();
				dlvExecutor.destroy();
				dlvWriter.close();
				//System.out.println("Done with " + extensionFileName + " file");
			}
			Conductor.time4 = new Date();
			//System.out.println("Done with DLV in " + (Conductor.time4.getTime() - Conductor.time3.getTime()));
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	static void findDifference() {
		try {
			String negativeNeedCheckFileName = extensionPrefixFileName + ".neg." + topRuleCount + ".needcheck";
			String positiveNeedCheckFileName = extensionPrefixFileName + ".pos." + topRuleCount + ".needcheck";
			List<String> negativeNeedCheckLines = Utils.readLines(negativeNeedCheckFileName);
			Set<String> negativeNeedCheckLineSet = new HashSet<String>(negativeNeedCheckLines);
			Writer differebceWriter = new PrintWriter(
					new File(extensionPrefixFileName + ".diff." + topRuleCount + ".needcheck"));
			List<String> positiveNeedCheckLines = Utils.readLines(positiveNeedCheckFileName);
			for (String positiveNeedCheckLine : positiveNeedCheckLines) {
				if (negativeNeedCheckLineSet.contains(positiveNeedCheckLine)) {
					continue;
				}
				differebceWriter.write(positiveNeedCheckLine + "\n");
			}
			differebceWriter.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void execute(RankingType type) {
		if (!(new File(encodeFileName).exists())) {
			Encoder.encode();
		}
		Encoder.loadEncode();
		if (!(new File(trainingDataDlvFileName).exists())) {

			Encoder.convert2DlvKnowledgeGraph();
		}
		generateExceptions(type);
		if (withDlv == 1) {
			runDlv();
			evaluate();
			Encoder.decodeDlvOutput();
		}
	}

}
