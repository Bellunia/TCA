package MLNs.rulemining;

import MLNs.Controller.NameMapper;
import amie.rules.Rule;
import com.opencsv.CSVWriter;

import MLNs.Controller.ProbKBData;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import amie.data.KB;
/**
 * Driver of rules from Amie to ProbKB.
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class RuleDriver {
	
	private final static Logger logger = LogManager.getLogger(RuleDriver.class);
	
	private NameMapper map;
	private String base;

	private static final String HEAD_LEFT = "?a";
	private static final String HEAD_RIGHT = "?b";
	
	private HashMap<String, ArrayList<String[]>>

			csvContent = new HashMap<>();
	
	public RuleDriver(NameMapper map, String base) {
		super();
		this.map = map;
		this.base = base;
		for(int i=1; i<=6; i++)
			csvContent.put(base + "/mln"+i+".csv", new ArrayList<>());
	}

	public void process(Rule rule) throws IOException {
		
		int size = rule.getBody().size();

		if (size == 1) { // call one or two

			int[] b = rule.getBody().get(0);//ByteString[] b
			// subject, predicate, object
			String pHead = rule.getHeadRelation();
			String pBody =  KB.unmap(b[1]).replace("<", "").replace(">", "");
			// b[1].toString(); // TODO check me!
			String pSubject= KB.unmap(b[0]);
			if (pSubject.equals(HEAD_LEFT))//b[0].toString()
				addTypeOne(pHead, pBody, toWeight(rule.getPcaConfidence()));
			else
				addTypeTwo(pHead, pBody, toWeight(rule.getPcaConfidence()));
		} else { // call three to six

			int[] b1 = rule.getBody().get(0);
			int[] b2 = rule.getBody().get(1);

			String pHead = rule.getHeadRelation();
			String pBody1 =  KB.unmap(b1[1]).replace("<", "").replace(">", "");
					//b1[1].toString();
			String pBody2 =KB.unmap(b2[1]).replace("<", "").replace(">", "");
					//b2[1].toString();
			String sub1=KB.unmap(b1[0]);
			String sub2=KB.unmap(b2[0]);
			String obj1=KB.unmap(b1[2]);
			String obj2=KB.unmap(b2[2]);

			if (sub1.equals(HEAD_LEFT) && sub2.equals(HEAD_RIGHT))
				addTypeThree(pHead, pBody1, pBody2,
						toWeight(rule.getPcaConfidence()));
			if (sub1.equals(HEAD_RIGHT) && sub2.equals(HEAD_LEFT))
				addTypeThree(pHead, pBody2, pBody1,
						toWeight(rule.getPcaConfidence()));
			
			if (sub1.equals(HEAD_LEFT) && obj2.equals(HEAD_RIGHT))
				addTypeFour(pHead, pBody1, pBody2,
						toWeight(rule.getPcaConfidence()));
			if (obj1.equals(HEAD_RIGHT) && sub2.equals(HEAD_LEFT))
				addTypeFour(pHead, pBody2, pBody1,
						toWeight(rule.getPcaConfidence()));
			
			if (obj1.equals(HEAD_LEFT) &&  sub2.equals(HEAD_RIGHT))
				addTypeFive(pHead, pBody1, pBody2,
						toWeight(rule.getPcaConfidence()));
			if (sub1.equals(HEAD_RIGHT) && obj2.equals(HEAD_LEFT))
				addTypeFive(pHead, pBody2, pBody1,
						toWeight(rule.getPcaConfidence()));
			
			if (obj1.equals(HEAD_LEFT) && obj2.equals(HEAD_RIGHT))
				addTypeSix(pHead, pBody1, pBody2,
						toWeight(rule.getPcaConfidence()));
			if (obj1.equals(HEAD_RIGHT) && obj2.equals(HEAD_LEFT))
				addTypeSix(pHead, pBody2, pBody1,
						toWeight(rule.getPcaConfidence()));
			
		}
	}

	/**
	 * @param pcaConfidence
	 * @return
	 */
	private double toWeight(double pcaConfidence) {
		return pcaConfidence;
	}

	/**
	 * p(x,y) <- q(x,y)
	 *
	 * @param pHead
	 * @param pBody
	 * @param weight
	 * @throws IOException 
	 */
	private void addTypeOne(String pHead, String pBody, double weight) {
		logger.trace("Adding type one: "+pHead+", "+pBody+", "+weight);
		String headName = map.getName(pHead).substring(ProbKBData.REL_LENGTH);
		String bodyName = map.getName(pBody).substring(ProbKBData.REL_LENGTH); 
		String str[] = {
				headName,
				bodyName,
				"1", // TODO class of x
				"1", // TODO class of y
				"" + weight
		};
		csvContent.get(base + "/mln1.csv").add(str);
	}

	/**
	 * p(x,y) <- q(y,x)
	 *
	 * @param pHead
	 * @param pBody
	 * @param weight
	 */
	private void addTypeTwo(String pHead, String pBody, double weight) {
		logger.trace("Adding type two: "+pHead+", "+pBody+", "+weight);

		String str[] = {

				map.getName(pHead).substring(ProbKBData.REL_LENGTH),
				map.getName(pBody).substring(ProbKBData.REL_LENGTH),
				"1", // TODO class of x
				"1", // TODO class of y
				"" + weight
		};
		csvContent.get(base + "/mln2.csv").add(str);
	}

	/**
	 * p(x,y) <- q(x,z), r(y,z)
	 * 
	 * @param pHead
	 * @param pBodyQ
	 * @param pBodyR
	 * @param weight
	 */
	private void addTypeThree(String pHead, String pBodyQ, String pBodyR,
			double weight) {
		logger.trace("Adding type three: "+pHead+", "+pBodyQ+", "+pBodyR+", "+weight);
		String str[] = {
				map.getName(pHead).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyQ).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyR).substring(ProbKBData.REL_LENGTH),
				"1", // TODO class of x
				"1", // TODO class of y
				"1", // TODO class of z
				"" + weight
		};
		csvContent.get(base + "/mln3.csv").add(str);
	}

	/**
	 * p(x,y) <- q(x,z), r(z,y)
	 * @param pHead
	 * @param pBodyQ
	 * @param pBodyR
	 * @param weight
	 */
	private void addTypeFour(String pHead, String pBodyQ, String pBodyR,
			double weight) {
		logger.trace("Adding type four: "+pHead+", "+pBodyQ+", "+pBodyR+", "+weight);
		String str[] = {
				map.getName(pHead).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyQ).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyR).substring(ProbKBData.REL_LENGTH),
				"1", // TODO class of x
				"1", // TODO class of y
				"1", // TODO class of z
				"" + weight
		};
		csvContent.get(base + "/mln4.csv").add(str);
	}

	/**
	 * p(x,y) <- q(z,x), r(y,z)
	 * @param pHead
	 * @param pBodyQ
	 * @param pBodyR
	 * @param weight
	 */
	private void addTypeFive(String pHead, String pBodyQ, String pBodyR,
			double weight) {
		logger.trace("Adding type five: "+pHead+", "+pBodyQ+", "+pBodyR+", "+weight);
		String str[] = {
				map.getName(pHead).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyQ).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyR).substring(ProbKBData.REL_LENGTH),
				"1", // TODO class of x
				"1", // TODO class of y
				"1", // TODO class of z
				"" + weight
		};
		csvContent.get(base + "/mln5.csv").add(str);
	}

	/**
	 * p(x,y) <- q(z,x), r(z,y)
	 * @param pHead
	 * @param pBodyQ
	 * @param pBodyR
	 * @param weight
	 */
	private void addTypeSix(String pHead, String pBodyQ, String pBodyR,
			double weight) {
		logger.trace("Adding type six: "+pHead+", "+pBodyQ+", "+pBodyR+", "+weight);
		String str[] = {
				map.getName(pHead).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyQ).substring(ProbKBData.REL_LENGTH),
				map.getName(pBodyR).substring(ProbKBData.REL_LENGTH),
				"1", // TODO class of x
				"1", // TODO class of y
				"1", // TODO class of z
				"" + weight
		};
		csvContent.get(base + "/mln6.csv").add(str);
	}

	public void buildCSV() {
		
		for(String key : csvContent.keySet()) {

			try {
				CSVWriter	writer = new CSVWriter(new FileWriter(key));
				for(String[] line : csvContent.get(key))
					writer.writeNext(line);
				writer.close();
			} catch (IOException e) {
				logger.error(e.getMessage());
				// XXX RuntimeException?
			}			
		}
	}

}
