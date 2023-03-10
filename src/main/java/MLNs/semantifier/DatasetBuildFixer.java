package MLNs.semantifier;

import com.opencsv.exceptions.CsvValidationException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.simmetrics.metrics.Levenshtein;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeSet;

/**
 * Removes faulty mappings from the gold standard, e.g. when the authors cannot
 * be linked because one of them is missing in one dataset.
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class DatasetBuildFixer {

	public static void main(String[] args) throws IOException, ClassNotFoundException, CsvValidationException {
		
//		System.out.println(new Levenshtein().distance("Query Execution Techniques for Caching Expensive Methods.", "2Q"));
		
		new DatasetBuildFixer().run();
		new DatasetBuildFixer().fix();
	}

	public void fix() throws IOException, CsvValidationException {
		
		TreeSet<String> ids = new TreeSet<>();
		Scanner in = new Scanner(new File(Commons.TO_BE_DELETED_ID));
		while (in.hasNextLine())
			ids.add(in.nextLine());
		in.close();
		
		System.out.println("-----------\n"+ids);
		
		CSVReader reader = new CSVReader(new FileReader(new File(Commons.DBLP_ACM_CSV)));
		CSVWriter writer = new CSVWriter(new FileWriter(new File(Commons.DBLP_ACM_FIXED_CSV)));
		CSVWriter removed = new CSVWriter(new FileWriter(new File(Commons.DBLP_ACM_REMOVED_CSV)));
		String[] nextLine = reader.readNext();
		writer.writeNext(nextLine);
		removed.writeNext(nextLine);
		while ((nextLine = reader.readNext()) != null) {
			if(ids.contains(nextLine[1])) {
				removed.writeNext(nextLine);
				System.out.println("Removed: "+nextLine[0]+" | "+nextLine[1]);
			} else
				writer.writeNext(nextLine);
		}
		removed.close();
		writer.close();
		reader.close();
		
	}

	public void run() throws FileNotFoundException {
		
		TreeSet<String> blacklist = new TreeSet<>();
		PrintWriter pw = new PrintWriter(new File(Commons.TO_BE_DELETED_ID));
		
		// get list of faulty authors
		TreeSet<String> pairs = new TreeSet<>();
		Scanner in = new Scanner(new File(Commons.TO_BE_DELETED));
		while (in.hasNextLine())
			pairs.add(in.nextLine());
		in.close();

		for (String pair : pairs) {
			String dblp = pair.split(",")[0];
			String acm = pair.split(",")[1];
			
			System.out.println(dblp+" | "+acm);

			// query for DBLP-L3S publications
			HashMap<String, String> dblpLabelToURI = new HashMap<>();
			ResultSet rs1 = Commons.sparql(
					"select ?p ?t where { ?p <"+Commons.DC_CREATOR+"> <" + dblp
							+ "> . ?p <"+Commons.RDFS_LABEL+"> ?t }",
					Commons.DBLPL3S_ENDPOINT, Commons.DBLPL3S_GRAPH);
			while(rs1.hasNext()) {
				QuerySolution qs = rs1.next();
				dblpLabelToURI.put(qs.getLiteral("t").getString(), qs.getResource("p").getURI());
			}

			// query for ACM publications
			HashMap<String, String> acmLabelToURI = new HashMap<>();
			ResultSet rs2 = Commons.sparql(
					"select ?p ?t where { ?p <"+Commons.HAS_AUTHOR+"> <" + acm
							+ "> . ?p <"+Commons.HAS_TITLE+"> ?t }",
					Commons.ACMRKB_ENDPOINT, Commons.ACMRKB_GRAPH);
			while(rs2.hasNext()) {
				QuerySolution qs = rs2.next();
				acmLabelToURI.put(qs.getLiteral("t").getString(), qs.getResource("p").getURI());
			}
			
			// Round-Robin among labels, checking also for substrings (e.g., to cut off undertitles)
			float dMin = Float.MAX_VALUE, dMinSub = Float.MAX_VALUE;
			String l1min = null, l2min = null, l1minSub = null, l2minSub = null;
			Levenshtein lev = new Levenshtein();
			for(String l1 : dblpLabelToURI.keySet()) {
				for(String l2 : acmLabelToURI.keySet()) {
					float d = lev.distance(l1.toLowerCase(), l2.toLowerCase());
					if(d < dMin) {
						dMin = d;
						l1min = l1;
						l2min = l2;
					}
					for(int i=0; i<l1.length(); i++) {
						float dd = lev.distance(l1.substring(0, i).toLowerCase(), l2.toLowerCase());
						if(dd < dMinSub) {
							dMinSub = dd;
							l1minSub = l1;
							l2minSub = l2;
						}
					}
					for(int i=0; i<l2.length(); i++) {
						float dd = lev.distance(l1.toLowerCase(), l2.substring(0, i).toLowerCase());
						if(dd < dMinSub) {
							dMinSub = dd;
							l1minSub = l1;
							l2minSub = l2;
						}
					}
				}
			}
			// give more importance to full string comparison when d is at most 2.0
			if(dMin > 2.0) {
				System.out.println("Using substring comparison (dMin = "+dMin+")");
				dMin = dMinSub;
				l1min = l1minSub;
				l2min = l2minSub;
			}
				
			
			// add publications to the blacklist
			System.out.println("DISTANCE = " + dMin + "\n" + l1min + "\n" + l2min);
			String l2URI = acmLabelToURI.get(l2min);
			System.out.println("URI: "+l2URI + "\n");
			blacklist.add(l2URI.substring(l2URI.lastIndexOf("/") + 1));
			
//			break;
		}
		
		System.out.println(blacklist);
		for(String id : blacklist)
			pw.write(id+"\n");
		pw.close();
	}

}
