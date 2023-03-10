package MLNs.eval;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;

import java.util.TreeSet;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class FMeasureEvaluation {
	
	
	private String psetPath, hsetPath;
	
	private int tp, fp, fn;
	private double pre, rec, f1;

	public FMeasureEvaluation(String psetPath, String hsetPath) {
		super();
		this.psetPath = psetPath;
		this.hsetPath = hsetPath;
	}
	
	public void run() {
		
		System.out.println("Running evaluation on set "+psetPath+" against set "+hsetPath);
		
		TreeSet<String> spoP = read(psetPath);
		TreeSet<String> spoH = read(hsetPath);
		
		System.out.println("Predicted");
		for(String s : spoP)
			System.out.println("\t"+s);
		System.out.println("Hidden");
		for(String s : spoH)
			System.out.println("\t"+s);
		
		TreeSet<String> fpSet = new TreeSet<>(spoP);
		fpSet.removeAll(spoH);
		fp = fpSet.size();
		
		TreeSet<String> fnSet = new TreeSet<>(spoH);
		fnSet.removeAll(spoP);
		fn = fnSet.size();
		
		tp = spoP.size() - fp;
		
		System.out.println("TP = "+tp+"; FP = "+fp+"; FN = "+fn);
		
		pre = (tp + fp) == 0 ? 0d : (double) tp / (tp + fp);
		rec = (tp + fn) == 0 ? 0d : (double) tp / (tp + fn);
		f1 = (pre + rec) == 0d ? 0d : 2 * pre * rec / (pre + rec);
		
		System.out.println("F1 = "+f1+"; Pre = "+pre+"; Rec = "+rec);
		
	}

	private TreeSet<String> read(String path) {
		
		TreeSet<String> spo = new TreeSet<>();
		
		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void start() {
			}

			@Override
			public void triple(Triple triple) {
				spo.add(triple.getSubject().getURI()+" "+
						triple.getPredicate().getURI()+" "+
						triple.getObject().toString());
			}

			@Override
			public void quad(Quad quad) {
			}

			@Override
			public void base(String base) {
			}

			@Override
			public void prefix(String prefix, String iri) {
			}

			@Override
			public void finish() {
			}
			
		};
		
		RDFDataMgr.parse(dataStream, path);
		
		return spo;
	}
	
	public String getPsetPath() {
		return psetPath;
	}

	public String getHsetPath() {
		return hsetPath;
	}

	public int getTp() {
		return tp;
	}

	public int getFp() {
		return fp;
	}

	public int getFn() {
		return fn;
	}

	public double getPre() {
		return pre;
	}

	public double getRec() {
		return rec;
	}

	public double getF1() {
		return f1;
	}

	public static void main(String[] args) {
		new FMeasureEvaluation("/Users/tom/PhD/srl/Mandolin/eval/0002/cv/run0/output_1.0.nt", 
				"/Users/tom/PhD/srl/Mandolin/eval/0002/cv/partitions/0.nt").run();
	}

}
