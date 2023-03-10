package MLNs.common;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.vocabulary.XSD;
import jp.ndca.similarity.join.PPJoin;
import jp.ndca.similarity.join.StringItem;
import jp.ndca.similarity.join.Tokenizer;
import MLNs.common.NameMapperCommon.Type;
import MLNs.model.Cache;
import MLNs.model.ComparableLiteral;
import MLNs.semantifier.Commons;
import MLNs.util.URLs;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.system.StreamRDF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

/**
 * Mandolin for syntax used by common MLN learning frameworks (e.g., Netkit,
 * ProbCog, Alchemy, Tuffy).
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class MandolinCommon {

	public static final String SRC_PATH = "datasets/DBLPL3S.nt";
	public static final String TGT_PATH = "datasets/LinkedACM.nt";
	public static final String LINKSET_PATH = "linksets/DBLPL3S-LinkedACM.nt";
	public static final String GOLD_STANDARD_PATH = "linksets/DBLPL3S-LinkedACM-GoldStandard.nt";

	public static final String BASE = "./knowledgeClean-data/MLN" ;//"eval/09_publi-tuffy";

	public static final String EVIDENCE_DB = BASE + "/evidence.db";
	public static final String QUERY_DB = BASE + "/query.db";
	public static final String PROG_MLN = BASE + "/prog.mln";

	public static final int TRAINING_SIZE = Integer.MAX_VALUE; // TODO restore:
																// (int) (47 *
																// 0.9);

	private static final int THR_MIN = 80;
	private static final int THR_MAX = 90;
	private static final int THR_STEP = 10;

	private TreeSet<String> unary = new TreeSet<>();

	private NameMapperCommon map;

	public NameMapperCommon getMap() {
		return map;
	}

	public MandolinCommon() {

		map = new NameMapperCommon();

	}

	private void run() throws FileNotFoundException {

		new File(BASE).mkdirs();

		PrintWriter pwEvid = new PrintWriter(new File(EVIDENCE_DB));
		graphEvidence(pwEvid);
		mappingEvidence(pwEvid, 0, TRAINING_SIZE);
		pwEvid.close();

		buildQueryDB(new PrintWriter(new File(QUERY_DB)));

		buildProgMLN(new PrintWriter(new File(PROG_MLN)));

	}

	public void buildProgMLN(PrintWriter pwProg) {

		String sameAs = map.getName(URLs.OWL_SAMEAS);
		for (String name : map.getNamesByType(Type.PROPERTY)) {
			// closed world assumption is false for owl:sameAs
			String cw = name.equals(sameAs) ? "" : "*";
			pwProg.write(cw + name + "(res, res)\n");
		}
		for (int thr = THR_MIN; thr <= THR_MAX; thr += THR_STEP)
			pwProg.write("*Sim" + thr + "(res, res)\n");
		for (String u : unary)
			pwProg.write("*" + u + "(res)\n");
		pwProg.write("\n");
		for (String name : map.getNamesByType(Type.PROPERTY)) {
			// symmetric property
			pwProg.write("1 !" + name + "(x, y) v " + name + "(y, x)\n");
			pwProg.write("1 !" + name + "(y, x) v " + name + "(x, y)\n");
			// transitive property
			pwProg.write("1 !" + name + "(x, y) v !" + name + "(y, z) v "
					+ name + "(x, z)\n");
		}
		pwProg.close();

	}

	public void buildQueryDB(PrintWriter pwQuery) {

		String sameAs = map.getName(URLs.OWL_SAMEAS);
		pwQuery.write(sameAs);
		pwQuery.close();

	}

	public void graphEvidence(PrintWriter pwEvid) {

		final Cache cache = new Cache();

		PPJoin ppjoin = new PPJoin();

		Tokenizer tok = ppjoin.getTokenizer();
		HashMap<Integer, ComparableLiteral> dataset = new HashMap<>();

		// use a TreeSet to deduplicate
		final TreeSet<ComparableLiteral> setOfStrings = new TreeSet<>();

		StreamRDF dataStream = new StreamRDF() {

			@Override
			public void base(String arg0) {}

			@Override
			public void finish() {}

			@Override
			public void prefix(String arg0, String arg1) {
			}

			@Override
			public void quad(Quad arg0) {}

			@Override
			public void start() {}

			@Override
			public void triple(Triple arg0) {
				String s = map.add(arg0.getSubject().getURI(), Type.RESOURCE);
				// System.out.println("Added "+s+" - "+map.getURI(s));
				String p = map.add(arg0.getPredicate().getURI(), Type.PROPERTY);
				// System.out.println("Added "+p+" - "+map.getURI(p));
				String o = map.add(arg0.getObject().toString(), Type.RESOURCE);
				// System.out.println("Added "+o+" - "+map.getURI(o));

				if (pwEvid != null) {

					if (arg0.getPredicate().getURI()
							.equals(Commons.RDF_TYPE.getURI())) {
						System.out.println(o + "(" + s + ")");
						System.out.println("NEWCLASS\t" + o + "\t"
								+ arg0.getObject().toString());
						pwEvid.write(o + "(" + s + ")\n");
						unary.add(o);
					} else {
						System.out.println(p + "(" + s + ", " + o + ")");
						pwEvid.write(p + "(" + s + ", " + o + ")\n");
					}
				}

				if (arg0.getObject().isLiteral()) {
					String dtURI = arg0.getObject().getLiteralDatatypeURI();

					boolean considerString;
					if (dtURI == null)
						considerString = true;
					else
						considerString = dtURI.equals(XSD.xstring.getURI());

					if (considerString) {
						ComparableLiteral lit = new ComparableLiteral(arg0
								.getObject().getLiteral().toString(true), arg0
								.getObject().getLiteral().getValue().toString());
						setOfStrings.add(lit);
					}
				}

			}

		};

		RDFDataMgr.parse(dataStream, SRC_PATH);
		RDFDataMgr.parse(dataStream, TGT_PATH);

		map.pretty();

		Iterator<ComparableLiteral> it = setOfStrings.iterator();
		for (int i = 0; it.hasNext(); i++) {
			ComparableLiteral lit = it.next();
			String val = lit.getVal();
			cache.stringItems.add(new StringItem(tok.tokenize(val, false), i));
			dataset.put(i, lit);
		}

		System.out.println(cache.stringItems.size());
		List<StringItem> stringItems = cache.stringItems;

		StringItem[] strDatum = stringItems.toArray(new StringItem[stringItems
				.size()]);
		Arrays.sort(strDatum);

		ppjoin.setUseSortAtExtractPairs(false);

		for (int thr = THR_MIN; thr <= THR_MAX; thr += THR_STEP) {
			System.out.println("thr = " + (thr / 100.0));
			List<Entry<StringItem, StringItem>> result = ppjoin.extractPairs(
					strDatum, thr / 100.0);
			for (Entry<StringItem, StringItem> entry : result) {
				ComparableLiteral lit1 = dataset.get(entry.getKey().getId());
				ComparableLiteral lit2 = dataset.get(entry.getValue().getId());
				pwEvid.write("Sim" + thr + "(" + map.getName(lit1.getUri())
						+ ", " + map.getName(lit2.getUri()) + ")\n");
				System.out.println(lit1.getUri() + " <=> " + lit2.getUri());
				System.out.println(lit1.getVal() + " <=> " + lit2.getVal());
			}
		}

	}

	public void mappingEvidence(PrintWriter pwEvid, final int START,
			final int END) {

		final Cache training = new Cache();

		StreamRDF mapStream = new StreamRDF() {

			@Override
			public void base(String arg0) {
			}

			@Override
			public void finish() {
			}

			@Override
			public void prefix(String arg0, String arg1) {
			}

			@Override
			public void quad(Quad arg0) {
			}

			@Override
			public void start() {
			}

			@Override
			public void triple(Triple arg0) {
				String s = map.add(arg0.getSubject().getURI(), Type.RESOURCE);
				// System.out.println("Added "+s+" - "+map.getURI(s));
				String p = map.add(arg0.getPredicate().getURI(), Type.PROPERTY);
				// System.out.println("Added "+p+" - "+map.getURI(p));
				String o = map.add(arg0.getObject().toString(), Type.RESOURCE);
				// System.out.println("Added "+o+" - "+map.getURI(o));

				if (pwEvid != null) {
					int c = ++training.count;
					if (START <= c && c <= END) {
						System.out.println(training.count + "\t" + p + "(" + s
								+ ", " + o + ")");
						pwEvid.write(p + "(" + s + ", " + o + ")\n");
					}
				}
			}

		};

		RDFDataMgr.parse(mapStream, LINKSET_PATH);

	}

	public void closureEvidence(PrintWriter pwEvid) {

		StreamRDF mapStream = new StreamRDF() {

			@Override
			public void base(String arg0) {
			}

			@Override
			public void finish() {
			}

			@Override
			public void prefix(String arg0, String arg1) {
			}

			@Override
			public void quad(Quad arg0) {
			}

			@Override
			public void start() {
			}

			@Override
			public void triple(Triple arg0) {
				String s = map.getName(arg0.getSubject().getURI());
				String p = map.getName(arg0.getPredicate().getURI());
				String o = map.getName(arg0.getObject().toString());

				if (s == null || p == null || o == null)
					System.err.println("HALT!");

				if (pwEvid != null) {
					pwEvid.write(p + "(" + s + ", " + o + ")\n");
				}
			}

		};

		RDFDataMgr.parse(mapStream, GOLD_STANDARD_PATH);

	}

	public static void main(String[] args) throws FileNotFoundException {

		// System.err.println("Launch line commented to prevent file overwrite.");
		new MandolinCommon().run();

	}

}